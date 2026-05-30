package com.football501.service;

import com.football501.engine.AnswerEvaluator;
import com.football501.engine.AnswerResult;
import com.football501.engine.GameStateMachine;
import com.football501.engine.GameTransition;
import com.football501.model.Game;
import com.football501.model.GameMove;
import com.football501.model.Match;
import com.football501.repository.GameMoveRepository;
import com.football501.repository.GameRepository;
import com.football501.repository.MatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates game-play operations: loading, validating, calling the engine, and persisting.
 *
 * <h3>Responsibility boundary</h3>
 * <ul>
 *   <li>This service owns the <em>database lifecycle</em>: load → validate → save.</li>
 *   <li>All game-state <em>transition rules</em> live in {@link GameStateMachine}.
 *       This service never encodes rule logic inline — it only calls the machine and
 *       applies the returned {@link GameTransition}.</li>
 *   <li>Controllers and WebSocket handlers are thin dispatchers that call this service
 *       and return the resulting DTOs to the client.</li>
 * </ul>
 */
@Service
@Slf4j
public class GameService {

    /**
     * Result bundle returned by {@link #processPlayerMove}.
     * Carries the already-loaded entities so callers don't need to re-fetch them.
     */
    public record MoveRecord(GameMove move, Game game, Match match, List<UUID> usedAnswerIds) {}

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final MatchRepository matchRepository;
    private final AnswerEvaluator answerEvaluator;
    private final GameStateMachine gameStateMachine;

    public GameService(
            GameRepository gameRepository,
            GameMoveRepository gameMoveRepository,
            MatchRepository matchRepository,
            AnswerEvaluator answerEvaluator,
            GameStateMachine gameStateMachine
    ) {
        this.gameRepository    = gameRepository;
        this.gameMoveRepository = gameMoveRepository;
        this.matchRepository   = matchRepository;
        this.answerEvaluator   = answerEvaluator;
        this.gameStateMachine  = gameStateMachine;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process a player's answer submission.
     *
     * <ol>
     *   <li>Load and validate the game/match state.</li>
     *   <li>Evaluate the answer via {@link AnswerEvaluator}.</li>
     *   <li>Delegate transition logic to {@link GameStateMachine}.</li>
     *   <li>Persist the resulting move and updated game state.</li>
     * </ol>
     *
     * @param gameId   the game UUID
     * @param playerId the player making the move
     * @param answer   the submitted answer text
     * @return a {@link MoveRecord} with the persisted move, updated game, match, and used answer IDs
     * @throws IllegalStateException    if the game is not in progress or it is not this player's turn
     * @throws IllegalArgumentException if the game or match does not exist
     */
    @Transactional
    public MoveRecord processPlayerMove(UUID gameId, UUID playerId, String answer) {
        log.debug("Processing move for game {} by player {}: {}", gameId, playerId, answer);

        Game game   = getGameOrThrow(gameId);
        Match match = getMatchOrThrow(game.getMatchId());

        validateGameInProgress(game);
        validatePlayerTurn(game, playerId);

        List<UUID> usedAnswerIds = gameMoveRepository.findUsedAnswerIdsByGameId(gameId);
        int currentScore = getPlayerScore(game, match, playerId);

        AnswerResult answerResult = answerEvaluator.evaluateAnswer(
                game.getQuestionId(), answer, currentScore, usedAnswerIds);

        GameTransition transition = gameStateMachine.onMoveSubmitted(game, match, playerId, answerResult);

        GameMove move = buildMove(gameId, playerId, game.getTurnCount() + 1,
                answer, answerResult, transition, currentScore);
        gameMoveRepository.save(move);

        applyTransition(game, match, playerId, transition);
        gameRepository.save(game);

        log.debug("Move processed: result={}, score {}→{}",
                transition.moveResult(), currentScore, transition.scoreAfter());

        return new MoveRecord(move, game, match, usedAnswerIds);
    }

    /**
     * Handle a player timeout.
     *
     * @param gameId   the game UUID
     * @param playerId the player who timed out
     * @throws IllegalStateException if the game is not in progress
     */
    @Transactional
    public void handleTimeout(UUID gameId, UUID playerId) {
        log.debug("Handling timeout for game {} by player {}", gameId, playerId);

        Game game   = getGameOrThrow(gameId);
        Match match = getMatchOrThrow(game.getMatchId());

        validateGameInProgress(game);

        int currentScore = getPlayerScore(game, match, playerId);
        GameTransition transition = gameStateMachine.onTimeout(game, match, playerId);

        GameMove timeoutMove = GameMove.builder()
                .gameId(gameId)
                .playerId(playerId)
                .moveNumber(game.getTurnCount() + 1)
                .submittedAnswer("")
                .result(GameMove.MoveResult.TIMEOUT)
                .scoreBefore(currentScore)
                .scoreAfter(currentScore)
                .isTimeout(true)
                .build();
        gameMoveRepository.save(timeoutMove);

        applyTransition(game, match, playerId, transition);
        gameRepository.save(game);
    }

    /**
     * Abandon an in-progress game and its parent match.
     *
     * <p>Safe to call when the game is already completed or abandoned — it
     * becomes a no-op in that case so the frontend can fire-and-forget.
     *
     * @param gameId   the game UUID
     * @param playerId the player requesting abandonment (must be part of the match)
     * @throws IllegalArgumentException if the game or match does not exist
     * @throws IllegalStateException    if the player is not part of the match
     */
    @Transactional
    public void abandonGame(UUID gameId, UUID playerId) {
        log.debug("Abandoning game {} for player {}", gameId, playerId);

        Game game   = getGameOrThrow(gameId);
        Match match = getMatchOrThrow(game.getMatchId());

        boolean isPlayer1 = playerId.equals(match.getPlayer1Id());
        boolean isPlayer2 = match.getPlayer2Id() != null && playerId.equals(match.getPlayer2Id());
        if (!isPlayer1 && !isPlayer2) {
            throw new IllegalStateException("Player " + playerId + " is not part of match " + match.getId());
        }

        if (game.getStatus() == Game.GameStatus.IN_PROGRESS) {
            game.setStatus(Game.GameStatus.ABANDONED);
            game.setCompletedAt(java.time.LocalDateTime.now());
            gameRepository.save(game);
        }

        if (match.getStatus() == Match.MatchStatus.IN_PROGRESS) {
            match.setStatus(Match.MatchStatus.ABANDONED);
            match.setCompletedAt(java.time.LocalDateTime.now());
            matchRepository.save(match);
        }

        log.info("Game abandoned: gameId={}, matchId={}", gameId, match.getId());
    }

    /**
     * Create a new game within an existing match.
     *
     * @param matchId    the match UUID
     * @param questionId the question UUID
     * @param gameNumber the ordinal position within the match (1-based)
     * @return the persisted {@link Game}
     */
    @Transactional
    public Game createGame(UUID matchId, UUID questionId, int gameNumber) {
        log.debug("Creating game {} for match {}", gameNumber, matchId);

        Match match = getMatchOrThrow(matchId);

        Game game = Game.builder()
                .matchId(matchId)
                .gameNumber(gameNumber)
                .questionId(questionId)
                .status(Game.GameStatus.IN_PROGRESS)
                .currentTurnPlayerId(match.getPlayer1Id()) // Player 1 always goes first
                .player1Score(501)
                .player2Score(501)
                .player1ConsecutiveTimeouts(0)
                .player2ConsecutiveTimeouts(0)
                .turnCount(0)
                .turnTimerSeconds(GameStateMachine.DEFAULT_TIMER)
                .build();

        return gameRepository.save(game);
    }

    /**
     * Return the set of answer UUIDs already used in a game (prevents duplicate submissions).
     *
     * @param gameId the game UUID
     * @return list of used answer UUIDs
     */
    @Transactional(readOnly = true)
    public List<UUID> getUsedAnswerIds(UUID gameId) {
        return gameMoveRepository.findUsedAnswerIdsByGameId(gameId);
    }

    /**
     * Look up a game by ID.
     *
     * @param gameId the game UUID
     * @return an optional containing the game if it exists
     */
    @Transactional(readOnly = true)
    public Optional<Game> getGameById(UUID gameId) {
        return gameRepository.findById(gameId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Apply a {@link GameTransition} to the mutable {@link Game} entity.
     *
     * <p>This is the single place in the service layer that writes game state.
     * All business rules about <em>what</em> should change have already been decided
     * by {@link GameStateMachine}; this method only applies the decision.
     */
    private void applyTransition(Game game, Match match, UUID activePlayerId, GameTransition t) {
        // Update the active player's score
        if (activePlayerId.equals(match.getPlayer1Id())) {
            game.setPlayer1Score(t.scoreAfter());
        } else if (match.getPlayer2Id() != null && activePlayerId.equals(match.getPlayer2Id())) {
            game.setPlayer2Score(t.scoreAfter());
        }

        // Update game status and winner
        game.setStatus(t.nextGameStatus());
        if (t.winnerId() != null) {
            game.setWinnerId(t.winnerId());
        }

        // Update whose turn it is
        if (t.nextTurnPlayerId() != null) {
            game.setCurrentTurnPlayerId(t.nextTurnPlayerId());
        }

        // Advance turn counter if the move consumed a turn
        if (t.turnAdvanced()) {
            game.setTurnCount(game.getTurnCount() + 1);
        }

        // Update timer and consecutive-timeout counters
        game.setTurnTimerSeconds(t.nextTimerSeconds());
        game.setPlayer1ConsecutiveTimeouts(t.player1ConsecutiveTimeouts());
        game.setPlayer2ConsecutiveTimeouts(t.player2ConsecutiveTimeouts());
    }

    /** Build a {@link GameMove} from a transition result. */
    private GameMove buildMove(
            UUID gameId,
            UUID playerId,
            int moveNumber,
            String submittedAnswer,
            AnswerResult answerResult,
            GameTransition transition,
            int scoreBefore
    ) {
        return GameMove.builder()
                .gameId(gameId)
                .playerId(playerId)
                .moveNumber(moveNumber)
                .submittedAnswer(submittedAnswer)
                .matchedAnswerId(answerResult.getAnswerId())
                .matchedDisplayText(answerResult.getDisplayText())
                .result(transition.moveResult())
                .scoreValue(answerResult.getScore())
                .scoreBefore(scoreBefore)
                .scoreAfter(transition.scoreAfter())
                .isTimeout(false)
                .build();
    }

    private Game getGameOrThrow(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
    }

    private Match getMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
    }

    private void validateGameInProgress(Game game) {
        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
    }

    private void validatePlayerTurn(Game game, UUID playerId) {
        if (!game.getCurrentTurnPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not player's turn");
        }
    }

    private int getPlayerScore(Game game, Match match, UUID playerId) {
        if (playerId.equals(match.getPlayer1Id())) return game.getPlayer1Score();
        if (match.getPlayer2Id() != null && playerId.equals(match.getPlayer2Id())) return game.getPlayer2Score();
        throw new IllegalArgumentException("Player " + playerId + " is not part of this match");
    }
}
