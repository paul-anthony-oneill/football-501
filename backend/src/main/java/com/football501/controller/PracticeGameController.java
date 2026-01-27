package com.football501.controller;

import com.football501.dto.GameStateResponse;
import com.football501.dto.StartPracticeRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.dto.SubmitAnswerResponse;
import com.football501.model.*;
import com.football501.service.GameService;
import com.football501.service.MatchService;
import com.football501.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for practice (single player) game mode.
 *
 * Endpoints:
 * - POST /api/practice/start - Start a new practice game
 * - POST /api/practice/games/{gameId}/submit - Submit an answer
 * - GET /api/practice/games/{gameId} - Get current game state
 */
@RestController
@RequestMapping("/api/practice")
@Slf4j
public class PracticeGameController {

    private final MatchService matchService;
    private final GameService gameService;
    private final QuestionService questionService;

    private static final String DEFAULT_CATEGORY_SLUG = "football";

    public PracticeGameController(
        MatchService matchService,
        GameService gameService,
        QuestionService questionService
    ) {
        this.matchService = matchService;
        this.gameService = gameService;
        this.questionService = questionService;
    }

    /**
     * Start a new practice game.
     *
     * @param request the start practice request
     * @return game state response
     */
    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startPracticeGame(@RequestBody StartPracticeRequest request) {
        log.debug("Starting practice game for player {}", request.getPlayerId());

        // Get category (default to football if not specified)
        String categorySlug = request.getCategorySlug() != null
            ? request.getCategorySlug()
            : DEFAULT_CATEGORY_SLUG;

        Category category = questionService.getCategoryBySlug(categorySlug)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));

        // Create practice match (player vs self, best-of-1)
        Match match = matchService.createMatch(
            request.getPlayerId(),
            null, // No opponent in practice mode
            category.getId(),
            Match.MatchType.CASUAL,
            Match.MatchFormat.BEST_OF_1
        );

        // Start first game
        Game game = matchService.startNextGame(match.getId());

        // Get question
        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        log.info("Practice game started: gameId={}, playerId={}", game.getId(), request.getPlayerId());

        return ResponseEntity.ok(buildGameStateResponse(game, question, match));
    }

    /**
     * Submit an answer for the current game.
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @param request the submit answer request
     * @return submit answer response with result and updated game state
     */
    @PostMapping("/games/{gameId}/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
        @PathVariable UUID gameId,
        @RequestParam UUID playerId,
        @RequestBody SubmitAnswerRequest request
    ) {
        log.debug("Submitting answer for game {}: '{}'", gameId, request.getAnswer());

        // Process the move
        GameMove move = gameService.processPlayerMove(gameId, playerId, request.getAnswer());

        // Get updated game state
        Game game = gameService.getGameById(gameId)
            .orElseThrow(() -> new IllegalStateException("Game not found after move"));

        Match match = matchService.getMatchById(game.getMatchId())
            .orElseThrow(() -> new IllegalStateException("Match not found"));

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        // Build response
        SubmitAnswerResponse response = SubmitAnswerResponse.builder()
            .result(move.getResult().name())
            .matchedAnswer(move.getMatchedDisplayText())
            .scoreValue(move.getScoreValue())
            .scoreBefore(move.getScoreBefore())
            .scoreAfter(move.getScoreAfter())
            .reason(determineReason(move))
            .isWin(move.getResult() == GameMove.MoveResult.CHECKOUT)
            .gameState(buildGameStateResponse(game, question, match))
            .build();

        log.debug("Answer processed: result={}, score={}->{}", move.getResult(),
            move.getScoreBefore(), move.getScoreAfter());

        return ResponseEntity.ok(response);
    }

    /**
     * Get current game state.
     *
     * @param gameId the game UUID
     * @param playerId the player UUID
     * @return game state response
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameStateResponse> getGameState(
        @PathVariable UUID gameId,
        @RequestParam UUID playerId
    ) {
        log.debug("Getting game state for game {}", gameId);

        Game game = gameService.getGameById(gameId)
            .orElse(null);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Match match = matchService.getMatchById(game.getMatchId())
            .orElseThrow(() -> new IllegalStateException("Match not found"));

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        return ResponseEntity.ok(buildGameStateResponse(game, question, match));
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private GameStateResponse buildGameStateResponse(Game game, Question question, Match match) {
        // Determine current score (in practice mode, always player1)
        int currentScore = game.getPlayer1Score();

        // Check if win
        boolean isWin = game.getStatus() == Game.GameStatus.COMPLETED
            && game.getWinnerId() != null
            && game.getWinnerId().equals(match.getPlayer1Id());

        return GameStateResponse.builder()
            .gameId(game.getId())
            .matchId(game.getMatchId())
            .questionId(game.getQuestionId())
            .questionText(question.getQuestionText())
            .currentScore(currentScore)
            .turnCount(game.getTurnCount())
            .status(game.getStatus().name())
            .isWin(isWin)
            .turnTimerSeconds(game.getTurnTimerSeconds())
            .build();
    }

    private String determineReason(GameMove move) {
        return switch (move.getResult()) {
            case INVALID -> "Answer not found or already used";
            case BUST -> "Invalid darts score or bust";
            case CHECKOUT -> "Win!";
            default -> null;
        };
    }

    /**
     * Global exception handler for bad requests.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
