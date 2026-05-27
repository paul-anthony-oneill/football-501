package com.football501.engine;

import com.football501.model.Game;
import com.football501.model.GameMove;
import com.football501.model.Match;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pure game-state-transition coordinator.
 *
 * <p>This class owns all Football 501 turn-machine rules and produces an immutable
 * {@link GameTransition} that describes exactly what should change. It has
 * <strong>no repository dependencies</strong> — all input is loaded by
 * {@link com.football501.service.GameService}, which is also responsible for
 * persisting the returned transition.
 *
 * <h3>State machine rules</h3>
 * <ul>
 *   <li><b>INVALID</b>  — same player retries; nothing changes (timer keeps running)</li>
 *   <li><b>BUST</b>     — no score change; turn switches to opponent</li>
 *   <li><b>VALID</b>    — score deducted; turn switches; timeout counter resets</li>
 *   <li><b>CHECKOUT</b> — triggers the close-finish rule in multiplayer</li>
 *   <li><b>TIMEOUT</b>  — increments consecutive counter; reduces timer; forfeits at threshold</li>
 * </ul>
 *
 * <h3>Close-finish rule</h3>
 * If Player 1 checks out, the game is not immediately over. Player 2 receives one
 * final turn to get closer to 0. The player nearer to 0 wins.
 */
@Component
@Slf4j
public class GameStateMachine {

    /** Default turn timer in seconds. */
    public static final int DEFAULT_TIMER = 45;

    /** Timer after the 1st consecutive timeout. */
    public static final int REDUCED_TIMER_1 = 30;

    /** Timer after the 2nd consecutive timeout. */
    public static final int REDUCED_TIMER_2 = 15;

    /** Number of consecutive timeouts before the player forfeits. */
    public static final int FORFEIT_TIMEOUT_THRESHOLD = 3;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process a submitted answer and compute the resulting {@link GameTransition}.
     *
     * @param game         current game entity (read-only in this call)
     * @param match        current match entity
     * @param playerId     the player who submitted the answer
     * @param answerResult the evaluation produced by {@link AnswerEvaluator}
     * @return an immutable transition descriptor; never {@code null}
     */
    public GameTransition onMoveSubmitted(
            Game game,
            Match match,
            UUID playerId,
            AnswerResult answerResult
    ) {
        GameMove.MoveResult moveResult = classifyMove(answerResult);
        boolean isPractice = match.getPlayer2Id() == null;
        int currentScore = getPlayerScore(game, match, playerId);

        // ── default values (for INVALID: nothing changes) ──────────────────
        int scoreAfter            = currentScore;
        boolean turnAdvanced      = false;
        UUID nextTurnPlayerId     = game.getCurrentTurnPlayerId();
        Game.GameStatus nextStatus = game.getStatus();
        UUID winnerId              = game.getWinnerId();
        int nextTimer              = game.getTurnTimerSeconds();
        int p1Timeouts             = game.getPlayer1ConsecutiveTimeouts();
        int p2Timeouts             = game.getPlayer2ConsecutiveTimeouts();

        if (moveResult == GameMove.MoveResult.INVALID) {
            // Same player retries; return immediately with unchanged defaults
            return new GameTransition(moveResult, scoreAfter, turnAdvanced, nextTurnPlayerId,
                    nextStatus, winnerId, nextTimer, p1Timeouts, p2Timeouts);
        }

        if (moveResult == GameMove.MoveResult.CHECKOUT) {
            // Reset timeout tracking before handling the checkout branch
            if (playerId.equals(match.getPlayer1Id())) p1Timeouts = 0;
            else                                       p2Timeouts = 0;
            return handleCheckout(game, match, playerId, answerResult, isPractice,
                    p1Timeouts, p2Timeouts);
        }

        // ── VALID or BUST ─────────────────────────────────────────────────
        if (moveResult == GameMove.MoveResult.VALID) {
            scoreAfter = answerResult.getNewTotal();
            // Reset consecutive timeouts and restore default timer on success
            if (playerId.equals(match.getPlayer1Id())) p1Timeouts = 0;
            else                                       p2Timeouts = 0;
            nextTimer = DEFAULT_TIMER;
        }
        // For BUST: scoreAfter stays = currentScore (no score change)

        turnAdvanced = true;
        if (!isPractice) {
            nextTurnPlayerId = opponentOf(match, playerId);
        }
        // In practice mode, nextTurnPlayerId remains the same player.

        return new GameTransition(moveResult, scoreAfter, turnAdvanced, nextTurnPlayerId,
                nextStatus, winnerId, nextTimer, p1Timeouts, p2Timeouts);
    }

    /**
     * Process a timeout event and compute the resulting {@link GameTransition}.
     *
     * @param game     current game entity
     * @param match    current match entity
     * @param playerId the player who timed out
     * @return an immutable transition descriptor; never {@code null}
     */
    public GameTransition onTimeout(Game game, Match match, UUID playerId) {
        boolean isPractice = match.getPlayer2Id() == null;
        int currentScore   = getPlayerScore(game, match, playerId);

        int p1Timeouts = game.getPlayer1ConsecutiveTimeouts();
        int p2Timeouts = game.getPlayer2ConsecutiveTimeouts();

        // Increment the timed-out player's counter
        if (playerId.equals(match.getPlayer1Id())) p1Timeouts++;
        else                                       p2Timeouts++;

        int consecutiveTimeouts = playerId.equals(match.getPlayer1Id()) ? p1Timeouts : p2Timeouts;

        // Forfeit threshold reached
        if (consecutiveTimeouts >= FORFEIT_TIMEOUT_THRESHOLD) {
            log.warn("Player {} forfeited after {} consecutive timeouts", playerId, consecutiveTimeouts);
            UUID winner = isPractice ? null : opponentOf(match, playerId);
            return new GameTransition(
                    GameMove.MoveResult.TIMEOUT, currentScore, true, null,
                    Game.GameStatus.COMPLETED, winner,
                    game.getTurnTimerSeconds(), // timer irrelevant when game ends
                    p1Timeouts, p2Timeouts
            );
        }

        // Reduce timer based on accumulated consecutive timeouts
        int nextTimer = game.getTurnTimerSeconds();
        if (consecutiveTimeouts == 1)      nextTimer = REDUCED_TIMER_1;
        else if (consecutiveTimeouts == 2) nextTimer = REDUCED_TIMER_2;

        UUID nextTurnPlayerId = isPractice ? playerId : opponentOf(match, playerId);

        return new GameTransition(
                GameMove.MoveResult.TIMEOUT, currentScore, true, nextTurnPlayerId,
                game.getStatus(), game.getWinnerId(),
                nextTimer, p1Timeouts, p2Timeouts
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Map an {@link AnswerResult} to the move classification the engine uses
     * for state-machine routing.
     */
    private GameMove.MoveResult classifyMove(AnswerResult answerResult) {
        if (!answerResult.isValid()) return GameMove.MoveResult.INVALID;
        if (answerResult.isWin())   return GameMove.MoveResult.CHECKOUT;
        if (answerResult.isBust())  return GameMove.MoveResult.BUST;
        return GameMove.MoveResult.VALID;
    }

    /**
     * Apply the checkout / close-finish logic and return the resulting transition.
     *
     * <h3>Cases</h3>
     * <ol>
     *   <li><b>Practice mode</b> — immediate win.</li>
     *   <li><b>P2 responding to P1's close-finish turn</b> — compare distances to 0.</li>
     *   <li><b>P1 checks out first</b> — grant P2 one final turn (close-finish rule).</li>
     *   <li><b>P2 checks out first</b> — immediate win (no close-finish needed).</li>
     * </ol>
     */
    private GameTransition handleCheckout(
            Game game,
            Match match,
            UUID playerId,
            AnswerResult answerResult,
            boolean isPractice,
            int p1Timeouts,
            int p2Timeouts
    ) {
        int scoreAfter = answerResult.getNewTotal();

        // ── Case 1: Practice mode ───────────────────────────────────────────
        if (isPractice) {
            log.info("Practice checkout: player {} wins", playerId);
            return new GameTransition(
                    GameMove.MoveResult.CHECKOUT, scoreAfter, false, null,
                    Game.GameStatus.COMPLETED, playerId,
                    DEFAULT_TIMER, p1Timeouts, p2Timeouts
            );
        }

        // ── Case 2: P2 responding to P1's tentative close-finish win ────────
        if (game.getWinnerId() != null && !game.getWinnerId().equals(playerId)) {
            int p1Score = game.getPlayer1Score();
            int p2Score = scoreAfter;

            UUID winner;
            if (Math.abs(p2Score) < Math.abs(p1Score)) {
                log.info("P2 beat P1's checkout ({} vs {})", p2Score, p1Score);
                winner = match.getPlayer2Id();
            } else {
                log.info("P1 wins close finish ({} vs {})", p1Score, p2Score);
                winner = game.getWinnerId(); // P1 retains tentative win
            }
            return new GameTransition(
                    GameMove.MoveResult.CHECKOUT, scoreAfter, false, null,
                    Game.GameStatus.COMPLETED, winner,
                    DEFAULT_TIMER, p1Timeouts, p2Timeouts
            );
        }

        // ── Case 3: P1 checks out first — grant P2 a close-finish turn ──────
        if (playerId.equals(match.getPlayer1Id())) {
            log.debug("Close-finish rule applied — P2 gets final turn");
            return new GameTransition(
                    GameMove.MoveResult.CHECKOUT, scoreAfter, false,
                    match.getPlayer2Id(),        // P2's final turn
                    Game.GameStatus.IN_PROGRESS, // not over yet
                    playerId,                    // P1 is tentative winner
                    DEFAULT_TIMER, p1Timeouts, p2Timeouts
            );
        }

        // ── Case 4: P2 checks out first — no close-finish needed ────────────
        log.info("P2 checks out first: player {} wins", playerId);
        return new GameTransition(
                GameMove.MoveResult.CHECKOUT, scoreAfter, false, null,
                Game.GameStatus.COMPLETED, playerId,
                DEFAULT_TIMER, p1Timeouts, p2Timeouts
        );
    }

    private int getPlayerScore(Game game, Match match, UUID playerId) {
        if (playerId.equals(match.getPlayer1Id())) return game.getPlayer1Score();
        if (match.getPlayer2Id() != null && playerId.equals(match.getPlayer2Id())) return game.getPlayer2Score();
        throw new IllegalArgumentException("Player " + playerId + " is not part of match " + match.getId());
    }

    private UUID opponentOf(Match match, UUID playerId) {
        return playerId.equals(match.getPlayer1Id()) ? match.getPlayer2Id() : match.getPlayer1Id();
    }
}
