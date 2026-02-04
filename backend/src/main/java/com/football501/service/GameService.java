package com.football501.service;

import com.football501.engine.AnswerEvaluator;
import com.football501.engine.AnswerResult;
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
 * Service for managing game state and gameplay flow.
 *
 * Responsibilities:
 * - Process player moves (validate answers, update scores)
 * - Turn management (switch turns, track current player)
 * - Timeout handling (consecutive tracking, timer reduction, forfeit)
 * - Win detection (checkout achieved)
 * - Close finish rule (Player 2 gets final turn if Player 1 wins first)
 */
@Service
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final MatchRepository matchRepository;
    private final AnswerEvaluator answerEvaluator;

    // Timer constants
    private static final int DEFAULT_TIMER = 45;
    private static final int REDUCED_TIMER_1 = 30;
    private static final int REDUCED_TIMER_2 = 15;
    private static final int FORFEIT_TIMEOUT_THRESHOLD = 3;

    public GameService(
        GameRepository gameRepository,
        GameMoveRepository gameMoveRepository,
        MatchRepository matchRepository,
        AnswerEvaluator answerEvaluator
    ) {
        this.gameRepository = gameRepository;
        this.gameMoveRepository = gameMoveRepository;
        this.matchRepository = matchRepository;
        this.answerEvaluator = answerEvaluator;
    }

    /**
     * Process a player's move (answer submission).
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @param answer the player's answer
     * @return the created GameMove
     * @throws IllegalStateException if not player's turn or game not in progress
     */
    @Transactional
    public GameMove processPlayerMove(UUID gameId, UUID playerId, String answer) {
        log.debug("Processing move for game {} by player {}: {}", gameId, playerId, answer);

        // Load game and validate
        Game game = getGameOrThrow(gameId);
        Match match = getMatchOrThrow(game.getMatchId());

        validateGameInProgress(game);
        validatePlayerTurn(game, playerId);

        // Get used answers to prevent duplicates
        List<UUID> usedAnswerIds = gameMoveRepository.findUsedAnswerIdsByGameId(gameId);

        // Get current score
        int currentScore = getPlayerScore(game, playerId);

        // Evaluate answer
        AnswerResult answerResult = answerEvaluator.evaluateAnswer(
            game.getQuestionId(),
            answer,
            currentScore,
            usedAnswerIds
        );

        // Determine move result type
        GameMove.MoveResult moveResult = determineMoveResult(answerResult);

        // Create and save move
        GameMove move = createGameMove(game, playerId, answer, answerResult, moveResult, currentScore);
        gameMoveRepository.save(move);

        // Update game state based on result
        updateGameState(game, match, playerId, answerResult, moveResult);

        // Save updated game
        gameRepository.save(game);

        log.debug("Move processed: result={}, score before={}, score after={}",
            moveResult, move.getScoreBefore(), move.getScoreAfter());

        return move;
    }

    /**
     * Handle player timeout.
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     */
    @Transactional
    public void handleTimeout(UUID gameId, UUID playerId) {
        log.debug("Handling timeout for game {} by player {}", gameId, playerId);

        Game game = getGameOrThrow(gameId);
        Match match = getMatchOrThrow(game.getMatchId());

        validateGameInProgress(game);

        // Increment consecutive timeouts
        incrementConsecutiveTimeouts(game, playerId);

        // Check for forfeit (3+ consecutive timeouts)
        if (getConsecutiveTimeouts(game, playerId) >= FORFEIT_TIMEOUT_THRESHOLD) {
            log.warn("Player {} forfeited due to {} consecutive timeouts", playerId, FORFEIT_TIMEOUT_THRESHOLD);
            game.setStatus(Game.GameStatus.COMPLETED);
            game.setWinnerId(getOpponentId(match, playerId));
        } else {
            // Reduce timer based on consecutive timeouts
            updateTimerForTimeouts(game, playerId);
        }

        // Create timeout move
        int currentScore = getPlayerScore(game, playerId);
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

        // Switch turn (timeout wastes turn)
        // In practice mode (player2 = null), don't switch turns
        if (match.getPlayer2Id() != null) {
            game.setCurrentTurnPlayerId(getOpponentId(match, playerId));
        }
        game.setTurnCount(game.getTurnCount() + 1);

        gameRepository.save(game);
    }

    /**
     * Create a new game within a match.
     *
     * @param matchId the match UUID
     * @param questionId the question UUID
     * @param gameNumber the game number (1, 2, 3, etc.)
     * @return the created game
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
            .currentTurnPlayerId(match.getPlayer1Id()) // Player 1 always starts
            .player1Score(501)
            .player2Score(501)
            .player1ConsecutiveTimeouts(0)
            .player2ConsecutiveTimeouts(0)
            .turnCount(0)
            .turnTimerSeconds(DEFAULT_TIMER)
            .build();

        return gameRepository.save(game);
    }

    /**
     * Get used answer IDs for a game (to prevent duplicate answers).
     *
     * @param gameId the game UUID
     * @return list of used answer UUIDs
     */
    @Transactional(readOnly = true)
    public List<UUID> getUsedAnswerIds(UUID gameId) {
        return gameMoveRepository.findUsedAnswerIdsByGameId(gameId);
    }

    /**
     * Get game by ID.
     *
     * @param gameId the game UUID
     * @return optional game
     */
    @Transactional(readOnly = true)
    public Optional<Game> getGameById(UUID gameId) {
        return gameRepository.findById(gameId);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

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

    private int getPlayerScore(Game game, UUID playerId) {
        Match match = getMatchOrThrow(game.getMatchId());
        if (playerId.equals(match.getPlayer1Id())) {
            return game.getPlayer1Score();
        } else if (playerId.equals(match.getPlayer2Id())) {
            return game.getPlayer2Score();
        }
        throw new IllegalArgumentException("Player not in this match");
    }

    private void setPlayerScore(Game game, UUID playerId, int newScore) {
        Match match = getMatchOrThrow(game.getMatchId());
        if (playerId.equals(match.getPlayer1Id())) {
            game.setPlayer1Score(newScore);
        } else if (playerId.equals(match.getPlayer2Id())) {
            game.setPlayer2Score(newScore);
        }
    }

    private UUID getOpponentId(Match match, UUID playerId) {
        if (playerId.equals(match.getPlayer1Id())) {
            return match.getPlayer2Id();
        } else {
            return match.getPlayer1Id();
        }
    }

    private GameMove.MoveResult determineMoveResult(AnswerResult answerResult) {
        if (!answerResult.isValid()) {
            return GameMove.MoveResult.INVALID;
        }
        if (answerResult.isWin()) {
            return GameMove.MoveResult.CHECKOUT;
        }
        if (answerResult.isBust()) {
            return GameMove.MoveResult.BUST;
        }
        return GameMove.MoveResult.VALID;
    }

    private GameMove createGameMove(
        Game game,
        UUID playerId,
        String answer,
        AnswerResult answerResult,
        GameMove.MoveResult moveResult,
        int scoreBefore
    ) {
        // For invalid answers, scoreAfter should equal scoreBefore (no change)
        int scoreAfter = answerResult.isValid() ? answerResult.getNewTotal() : scoreBefore;

        return GameMove.builder()
            .gameId(game.getId())
            .playerId(playerId)
            .moveNumber(game.getTurnCount() + 1)
            .submittedAnswer(answer)
            .matchedAnswerId(answerResult.getAnswerId())
            .matchedDisplayText(answerResult.getDisplayText())
            .result(moveResult)
            .scoreValue(answerResult.getScore())
            .scoreBefore(scoreBefore)
            .scoreAfter(scoreAfter)
            .isTimeout(false)
            .build();
    }

    private void updateGameState(
        Game game,
        Match match,
        UUID playerId,
        AnswerResult answerResult,
        GameMove.MoveResult moveResult
    ) {
        // Update score if valid or checkout
        if (moveResult == GameMove.MoveResult.VALID || moveResult == GameMove.MoveResult.CHECKOUT) {
            setPlayerScore(game, playerId, answerResult.getNewTotal());
            resetConsecutiveTimeouts(game, playerId);
        }

        // Handle checkout (win)
        if (moveResult == GameMove.MoveResult.CHECKOUT) {
            handleCheckout(game, match, playerId);
            return; // Don't switch turn on checkout
        }

        // Switch turn for valid, bust (but NOT for invalid - allow retry)
        // In practice mode (player2 = null), don't switch turns - keep with player1
        if (moveResult == GameMove.MoveResult.VALID || moveResult == GameMove.MoveResult.BUST) {
            if (match.getPlayer2Id() != null) {
                // Multiplayer mode - switch turns normally
                game.setCurrentTurnPlayerId(getOpponentId(match, playerId));
            }
            // In practice mode, currentTurnPlayerId stays as player1
            game.setTurnCount(game.getTurnCount() + 1);
        }
    }

    private void handleCheckout(Game game, Match match, UUID playerId) {
        log.info("Player {} checked out in game {}", playerId, game.getId());

        // Practice mode (single player) - just complete immediately
        if (match.getPlayer2Id() == null) {
            game.setStatus(Game.GameStatus.COMPLETED);
            game.setWinnerId(playerId);
            log.info("Practice mode: Player {} wins!", playerId);
            return;
        }

        // Multiplayer mode - check if this is P2 responding to P1's checkout
        if (game.getWinnerId() != null && !game.getWinnerId().equals(playerId)) {
            // P1 already checked out, P2 is responding with their final turn
            int player1Score = game.getPlayer1Score();
            int player2Score = game.getPlayer2Score();

            // Determine winner based on distance to 0 (Standard 501 Rules).
            // Since P1 checked out, their score is 0.
            // P2 can only win if they also reach 0 (draws might favor P2 or result in draw depending on rules, 
            // but here logic implies P2 wins ties/close finishes).
            if (Math.abs(player2Score) < Math.abs(player1Score)) {
                log.info("Player 2 beat Player 1's checkout ({} vs {})", player2Score, player1Score);
                game.setWinnerId(match.getPlayer2Id());
            } else {
                log.info("Player 1 wins close finish ({} vs {})", player1Score, player2Score);
            }
            game.setStatus(Game.GameStatus.COMPLETED);
            return;
        }

        // Apply close finish rule: if Player 1 wins, Player 2 gets one final turn
        if (playerId.equals(match.getPlayer1Id())) {
            log.debug("Applying close finish rule - Player 2 gets final turn");
            game.setCurrentTurnPlayerId(match.getPlayer2Id());
            // Don't set status to COMPLETED yet - allow P2 final turn
            game.setWinnerId(playerId); // Tentative winner
        } else {
            // Player 2 won first (no close finish needed)
            game.setStatus(Game.GameStatus.COMPLETED);
            game.setWinnerId(playerId);
        }
    }

    private int getConsecutiveTimeouts(Game game, UUID playerId) {
        Match match = getMatchOrThrow(game.getMatchId());
        if (playerId.equals(match.getPlayer1Id())) {
            return game.getPlayer1ConsecutiveTimeouts();
        } else {
            return game.getPlayer2ConsecutiveTimeouts();
        }
    }

    private void incrementConsecutiveTimeouts(Game game, UUID playerId) {
        Match match = getMatchOrThrow(game.getMatchId());
        if (playerId.equals(match.getPlayer1Id())) {
            game.setPlayer1ConsecutiveTimeouts(game.getPlayer1ConsecutiveTimeouts() + 1);
        } else {
            game.setPlayer2ConsecutiveTimeouts(game.getPlayer2ConsecutiveTimeouts() + 1);
        }
    }

    private void resetConsecutiveTimeouts(Game game, UUID playerId) {
        Match match = getMatchOrThrow(game.getMatchId());
        if (playerId.equals(match.getPlayer1Id())) {
            game.setPlayer1ConsecutiveTimeouts(0);
        } else {
            game.setPlayer2ConsecutiveTimeouts(0);
        }
        // Reset timer to default on successful move
        game.setTurnTimerSeconds(DEFAULT_TIMER);
    }

    private void updateTimerForTimeouts(Game game, UUID playerId) {
        int consecutiveTimeouts = getConsecutiveTimeouts(game, playerId);

        // Timer reduction: 45s → 30s (after 2 timeouts) → 15s (after 3+ timeouts)
        if (consecutiveTimeouts == 2) {
            game.setTurnTimerSeconds(REDUCED_TIMER_1); // 30s
        } else if (consecutiveTimeouts >= 3) {
            game.setTurnTimerSeconds(REDUCED_TIMER_2); // 15s
        }
    }
}
