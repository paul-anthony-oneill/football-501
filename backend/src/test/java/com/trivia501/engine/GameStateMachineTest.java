package com.trivia501.engine;

import com.trivia501.model.Game;
import com.trivia501.model.GameMove;
import com.trivia501.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GameStateMachine}.
 *
 * <p>These tests exercise pure transition logic in isolation — no Spring context,
 * no repositories. Input game and match objects are constructed directly.
 */
@DisplayName("GameStateMachine Tests")
class GameStateMachineTest {

    private GameStateMachine stateMachine;

    private UUID matchId;
    private UUID gameId;
    private UUID questionId;
    private UUID player1Id;
    private UUID player2Id;
    private Match multiMatch;   // two-player match
    private Match soloMatch; // solo match (player2 = null)
    private Game game;

    @BeforeEach
    void setUp() {
        stateMachine = new GameStateMachine();

        matchId    = UUID.randomUUID();
        gameId     = UUID.randomUUID();
        questionId = UUID.randomUUID();
        player1Id  = UUID.randomUUID();
        player2Id  = UUID.randomUUID();

        multiMatch = Match.builder()
                .id(matchId)
                .player1Id(player1Id)
                .player2Id(player2Id)
                .type(Match.MatchType.CASUAL)
                .format(Match.MatchFormat.BEST_OF_3)
                .status(Match.MatchStatus.IN_PROGRESS)
                .player1GamesWon(0)
                .player2GamesWon(0)
                .build();

        soloMatch = Match.builder()
                .id(UUID.randomUUID())
                .player1Id(player1Id)
                .player2Id(null) // solo = no opponent
                .type(Match.MatchType.CASUAL)
                .format(Match.MatchFormat.BEST_OF_1)
                .status(Match.MatchStatus.IN_PROGRESS)
                .player1GamesWon(0)
                .player2GamesWon(0)
                .build();

        game = Game.builder()
                .id(gameId)
                .matchId(matchId)
                .gameNumber(1)
                .questionId(questionId)
                .status(Game.GameStatus.IN_PROGRESS)
                .currentTurnPlayerId(player1Id)
                .player1Score(501)
                .player2Score(501)
                .player1ConsecutiveTimeouts(0)
                .player2ConsecutiveTimeouts(0)
                .turnCount(0)
                .turnTimerSeconds(GameStateMachine.DEFAULT_TIMER)
                .build();
    }

    // ── Move: VALID ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("VALID move — deducts score, switches turn, resets timeout counter and timer")
    void validMove_shouldDeductScoreAndSwitchTurn() {
        AnswerResult answer = AnswerResult.valid("Erling Haaland", UUID.randomUUID(),
                36, true, false, 465, false, null, 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player1Id, answer);

        assertThat(t.moveResult()).isEqualTo(GameMove.MoveResult.VALID);
        assertThat(t.scoreAfter()).isEqualTo(465);
        assertThat(t.turnAdvanced()).isTrue();
        assertThat(t.nextTurnPlayerId()).isEqualTo(player2Id);
        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.IN_PROGRESS);
        assertThat(t.winnerId()).isNull();
        assertThat(t.nextTimerSeconds()).isEqualTo(GameStateMachine.DEFAULT_TIMER);
        assertThat(t.player1ConsecutiveTimeouts()).isEqualTo(0); // reset
    }

    @Test
    @DisplayName("VALID move with prior timeouts — resets consecutive timeout counter")
    void validMove_shouldResetConsecutiveTimeoutsAndTimer() {
        game.setPlayer1ConsecutiveTimeouts(2);
        game.setTurnTimerSeconds(GameStateMachine.REDUCED_TIMER_2);

        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                25, true, false, 476, false, null, 0.9);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player1Id, answer);

        assertThat(t.player1ConsecutiveTimeouts()).isEqualTo(0);
        assertThat(t.nextTimerSeconds()).isEqualTo(GameStateMachine.DEFAULT_TIMER);
    }

    @Test
    @DisplayName("VALID move in solo mode — keeps same player (no turn switch)")
    void validMove_soloMode_keepsCurrentPlayer() {
        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                30, true, false, 471, false, null, 0.9);

        GameTransition t = stateMachine.onMoveSubmitted(game, soloMatch, player1Id, answer);

        assertThat(t.turnAdvanced()).isTrue();
        assertThat(t.nextTurnPlayerId()).isEqualTo(player1Id); // stays on player1
    }

    // ── Move: INVALID ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("INVALID move — nothing changes, same player retries")
    void invalidMove_shouldChangNothing() {
        AnswerResult answer = AnswerResult.invalid("Not found", 501);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player1Id, answer);

        assertThat(t.moveResult()).isEqualTo(GameMove.MoveResult.INVALID);
        assertThat(t.scoreAfter()).isEqualTo(501); // no change
        assertThat(t.turnAdvanced()).isFalse();
        assertThat(t.nextTurnPlayerId()).isEqualTo(player1Id); // same player
        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.IN_PROGRESS);
    }

    // ── Move: BUST ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BUST move — no score change, turn switches to opponent")
    void bustMove_shouldSwitchTurnWithoutChangingScore() {
        AnswerResult answer = AnswerResult.valid("Player with 179", UUID.randomUUID(),
                179, false, true, 501, false, "Invalid darts score", 0.9);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player1Id, answer);

        assertThat(t.moveResult()).isEqualTo(GameMove.MoveResult.BUST);
        assertThat(t.scoreAfter()).isEqualTo(501); // no change
        assertThat(t.turnAdvanced()).isTrue();
        assertThat(t.nextTurnPlayerId()).isEqualTo(player2Id);
    }

    // ── Move: CHECKOUT ────────────────────────────────────────────────────────

    @Test
    @DisplayName("CHECKOUT by P1 (multiplayer) — close-finish rule: P2 gets final turn")
    void checkoutByP1_multiplayer_applyCloseFinishRule() {
        game.setPlayer1Score(35);
        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                35, true, false, 0, true, "Win!", 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player1Id, answer);

        assertThat(t.moveResult()).isEqualTo(GameMove.MoveResult.CHECKOUT);
        assertThat(t.scoreAfter()).isEqualTo(0);
        assertThat(t.turnAdvanced()).isFalse();
        assertThat(t.nextTurnPlayerId()).isEqualTo(player2Id); // P2's final turn
        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.IN_PROGRESS); // not over yet
        assertThat(t.winnerId()).isEqualTo(player1Id); // tentative winner
    }

    @Test
    @DisplayName("CHECKOUT by P2 first (multiplayer) — immediate win, no close-finish")
    void checkoutByP2_multiplayer_immediateWin() {
        game.setPlayer2Score(40);
        game.setCurrentTurnPlayerId(player2Id);

        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                40, true, false, 0, true, "Win!", 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player2Id, answer);

        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.COMPLETED);
        assertThat(t.winnerId()).isEqualTo(player2Id);
        assertThat(t.nextTurnPlayerId()).isNull(); // game over
    }

    @Test
    @DisplayName("CHECKOUT in solo mode — immediate win")
    void checkoutSoloMode_immediateWin() {
        game.setPlayer1Score(20);
        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                20, true, false, 0, true, "Win!", 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, soloMatch, player1Id, answer);

        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.COMPLETED);
        assertThat(t.winnerId()).isEqualTo(player1Id);
        assertThat(t.nextTurnPlayerId()).isNull();
    }

    @Test
    @DisplayName("P2 responds to close-finish and beats P1 — P2 wins")
    void p2RespondsToCloseFinish_beatsP1_p2Wins() {
        // Setup: P1 checked out at -3, P2 is responding
        game.setPlayer1Score(-3);
        game.setPlayer2Score(50);
        game.setWinnerId(player1Id); // tentative
        game.setCurrentTurnPlayerId(player2Id);

        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                52, true, false, -2, true, "Win!", 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player2Id, answer);

        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.COMPLETED);
        assertThat(t.winnerId()).isEqualTo(player2Id); // P2 closer to 0 (-2 vs -3)
        assertThat(t.scoreAfter()).isEqualTo(-2);
    }

    @Test
    @DisplayName("P2 responds to close-finish but can't beat P1 — P1 wins")
    void p2RespondsToCloseFinish_cannotBeatP1_p1Wins() {
        // P1 checked out at -1, P2 is at 50 and can only get to -5
        game.setPlayer1Score(-1);
        game.setPlayer2Score(50);
        game.setWinnerId(player1Id);
        game.setCurrentTurnPlayerId(player2Id);

        AnswerResult answer = AnswerResult.valid("Player", UUID.randomUUID(),
                55, true, false, -5, true, "Win!", 0.95);

        GameTransition t = stateMachine.onMoveSubmitted(game, multiMatch, player2Id, answer);

        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.COMPLETED);
        assertThat(t.winnerId()).isEqualTo(player1Id); // P1 closer to 0 (-1 vs -5)
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("First timeout — increments counter to 1, reduces timer to 30s")
    void firstTimeout_reducesTimerTo30s() {
        GameTransition t = stateMachine.onTimeout(game, multiMatch, player1Id);

        assertThat(t.moveResult()).isEqualTo(GameMove.MoveResult.TIMEOUT);
        assertThat(t.player1ConsecutiveTimeouts()).isEqualTo(1);
        assertThat(t.nextTimerSeconds()).isEqualTo(GameStateMachine.REDUCED_TIMER_1);
        assertThat(t.nextTurnPlayerId()).isEqualTo(player2Id);
        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Second timeout — increments counter to 2, reduces timer to 15s")
    void secondTimeout_reducesTimerTo15s() {
        game.setPlayer1ConsecutiveTimeouts(1);
        game.setTurnTimerSeconds(GameStateMachine.REDUCED_TIMER_1);

        GameTransition t = stateMachine.onTimeout(game, multiMatch, player1Id);

        assertThat(t.player1ConsecutiveTimeouts()).isEqualTo(2);
        assertThat(t.nextTimerSeconds()).isEqualTo(GameStateMachine.REDUCED_TIMER_2);
    }

    @Test
    @DisplayName("Third timeout — forfeits; opponent wins")
    void thirdTimeout_forfeitsGame() {
        game.setPlayer1ConsecutiveTimeouts(2);

        GameTransition t = stateMachine.onTimeout(game, multiMatch, player1Id);

        assertThat(t.player1ConsecutiveTimeouts()).isEqualTo(3);
        assertThat(t.nextGameStatus()).isEqualTo(Game.GameStatus.COMPLETED);
        assertThat(t.winnerId()).isEqualTo(player2Id);
        assertThat(t.nextTurnPlayerId()).isNull(); // game over
    }

    @Test
    @DisplayName("Timeout in solo mode — keeps same player (no opponent to switch to)")
    void timeout_soloMode_keepsCurrentPlayer() {
        GameTransition t = stateMachine.onTimeout(game, soloMatch, player1Id);

        assertThat(t.nextTurnPlayerId()).isEqualTo(player1Id);
    }
}
