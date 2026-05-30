package com.football501.controller;

import com.football501.dto.GameHints;
import com.football501.dto.GameStateResponse;
import com.football501.dto.StartSoloGameRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.dto.SubmitAnswerResponse;
import com.football501.model.*;
import com.football501.service.GameHintsService;
import com.football501.service.GameService;
import com.football501.service.MatchService;
import com.football501.service.QuestionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for solo (single-player) game mode.
 *
 * <h3>Authentication</h3>
 * Player identity is derived from the authenticated {@link Principal} rather
 * than an explicit {@code playerId} request parameter.  In development and
 * test environments a {@code DevModeAuthFilter} provides a fixed principal
 * automatically.  In production a JWT filter will supply the real user.
 *
 * Endpoints:
 * <ul>
 *   <li>POST /api/solo/start            — Start a new solo game</li>
 *   <li>POST /api/solo/games/{id}/submit — Submit an answer</li>
 *   <li>GET  /api/solo/games/{id}        — Get current game state</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/solo")
@Slf4j
public class SoloGameController {

    private final MatchService matchService;
    private final GameService gameService;
    private final QuestionService questionService;
    private final GameHintsService gameHintsService;

    private static final String DEFAULT_CATEGORY_SLUG = CategorySlug.FOOTBALL;

    public SoloGameController(
        MatchService matchService,
        GameService gameService,
        QuestionService questionService,
        GameHintsService gameHintsService
    ) {
        this.matchService = matchService;
        this.gameService = gameService;
        this.questionService = questionService;
        this.gameHintsService = gameHintsService;
    }

    /**
     * Start a new solo game.
     *
     * <p>Player identity is read from the authenticated principal — the client
     * cannot supply or override the player ID.
     *
     * @param request   optional category / difficulty preferences
     * @param principal injected by Spring Security from the current auth token
     * @return initial game state
     */
    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startSoloGame(
        @Valid @RequestBody StartSoloGameRequest request,
        Principal principal
    ) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Starting solo game for player {}", playerId);

        String categorySlug = request.getCategorySlug() != null
            ? request.getCategorySlug()
            : DEFAULT_CATEGORY_SLUG;

        Category category = questionService.getCategoryBySlug(categorySlug)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));

        Match match = matchService.createMatch(
            playerId,
            null,
            category.getId(),
            Match.MatchType.CASUAL,
            Match.MatchFormat.BEST_OF_1,
            request.getDifficulty()
        );

        MatchService.GameStartRecord startRecord = matchService.startNextGame(match);
        Game game = startRecord.game();
        Question question = startRecord.question();

        gameHintsService.loadScoreCache(question.getId());

        log.info("Solo game started: gameId={}, playerId={}", game.getId(), playerId);

        return ResponseEntity.ok(buildGameStateResponse(game, question, match, List.of()));
    }

    /**
     * Submit an answer for the current game.
     *
     * <p>Player identity comes from the authenticated principal; the caller
     * cannot spoof another player's ID.
     *
     * @param gameId    the game UUID
     * @param request   the answer text
     * @param principal injected by Spring Security
     * @return move result and updated game state
     */
    @PostMapping("/games/{gameId}/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
        @PathVariable UUID gameId,
        @Valid @RequestBody SubmitAnswerRequest request,
        Principal principal
    ) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Submitting answer for game {}: '{}'", gameId, request.getAnswer());

        GameService.MoveRecord result = gameService.processPlayerMove(gameId, playerId, request.getAnswer());

        Game game = result.game();
        Match match = result.match();

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        GameMove move = result.move();

        SubmitAnswerResponse response = SubmitAnswerResponse.builder()
            .result(move.getResult().name())
            .matchedAnswer(move.getMatchedDisplayText())
            .scoreValue(move.getScoreValue())
            .scoreBefore(move.getScoreBefore())
            .scoreAfter(move.getScoreAfter())
            .reason(determineReason(move))
            .isWin(move.getResult() == GameMove.MoveResult.CHECKOUT)
            .gameState(buildGameStateResponse(game, question, match, result.usedAnswerIds()))
            .build();

        log.debug("Answer processed: result={}, score={}->{}", move.getResult(),
            move.getScoreBefore(), move.getScoreAfter());

        return ResponseEntity.ok(response);
    }

    /**
     * Abandon an in-progress game.
     *
     * <p>Idempotent: calling this on an already-completed or already-abandoned
     * game is safe — the service treats it as a no-op.
     *
     * @param gameId    the game UUID
     * @param principal injected by Spring Security
     * @return 204 No Content on success
     */
    @PostMapping("/games/{gameId}/abandon")
    public ResponseEntity<Void> abandonGame(
        @PathVariable UUID gameId,
        Principal principal
    ) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Abandoning game {} for player {}", gameId, playerId);
        gameService.abandonGame(gameId, playerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current game state.
     *
     * <p>Player identity comes from the authenticated principal.
     *
     * @param gameId    the game UUID
     * @param principal injected by Spring Security
     * @return current game state, or 404 if the game does not exist
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameStateResponse> getGameState(
        @PathVariable UUID gameId,
        Principal principal
    ) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Getting game state for game {} (requestedBy={})", gameId, playerId);

        Game game = gameService.getGameById(gameId).orElse(null);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Match match = matchService.getMatchById(game.getMatchId())
            .orElseThrow(() -> new IllegalStateException("Match not found"));

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        return ResponseEntity.ok(buildGameStateResponse(game, question, match));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses the authenticated principal name as a {@link UUID}.
     * The principal name is the player's UUID string set by the auth filter
     * (dev: {@code DevModeAuthFilter}; prod: JWT filter).
     */
    private UUID playerIdFrom(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    private GameStateResponse buildGameStateResponse(Game game, Question question, Match match) {
        return buildGameStateResponse(game, question, match, null);
    }

    private GameStateResponse buildGameStateResponse(Game game, Question question, Match match, List<UUID> usedAnswerIds) {
        int currentScore = game.getPlayer1Score();

        boolean isWin = game.getStatus() == Game.GameStatus.COMPLETED
            && game.getWinnerId() != null
            && game.getWinnerId().equals(match.getPlayer1Id());

        String entityType = EntityType.FOOTBALLER;
        if (question.getConfig() != null) {
            Object configEntityType = question.getConfig().get("entity_type");
            if (configEntityType instanceof String s && !s.isBlank()) {
                entityType = s;
            }
        }

        GameHints hints = usedAnswerIds != null
            ? gameHintsService.computeHintsFromCache(game.getQuestionId(), usedAnswerIds, currentScore)
            : gameHintsService.computeHints(game.getId(), game.getQuestionId(), currentScore);

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
            .entityType(entityType)
            .hints(hints)
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

}
