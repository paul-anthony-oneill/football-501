package com.football501.controller;

import com.football501.dto.GameHints;
import com.football501.dto.GameStateResponse;
import com.football501.dto.MoveDto;
import com.football501.dto.StartSoloGameRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.dto.SubmitAnswerResponse;
import com.football501.model.*;
import com.football501.service.GameHintsService;
import com.football501.service.GameService;
import com.football501.service.MatchService;
import com.football501.service.QuestionService;
import com.football501.security.OptionalJwtFilter;
import jakarta.servlet.http.HttpServletRequest;
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
 * test environments {@link com.football501.security.OptionalJwtFilter}
 * provides a fixed principal automatically.  In production the same filter
 * validates Supabase JWTs.
 *
 * Endpoints:
 * <ul>
 *   <li>POST /api/solo/start              — Start a new solo game</li>
 *   <li>POST /api/solo/games/{id}/submit  — Submit an answer</li>
 *   <li>POST /api/solo/games/{id}/abandon — Abandon a game</li>
 *   <li>GET  /api/solo/games/{id}         — Get current game state</li>
 *   <li>GET  /api/solo/games/active       — Get player's active game</li>
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
    private final com.football501.service.PlayerProfileService playerProfileService;

    private static final String DEFAULT_CATEGORY_SLUG = CategorySlug.FOOTBALL;

    public SoloGameController(
        MatchService matchService,
        GameService gameService,
        QuestionService questionService,
        GameHintsService gameHintsService,
        com.football501.service.PlayerProfileService playerProfileService
    ) {
        this.matchService = matchService;
        this.gameService = gameService;
        this.questionService = questionService;
        this.gameHintsService = gameHintsService;
        this.playerProfileService = playerProfileService;
    }

    /**
     * Start a new solo game.
     *
     * <p>Player identity is read from the authenticated principal — the client
     * cannot supply or override the player ID.
     *
     * <p>Any existing in-progress games for the player are abandoned first to
     * prevent orphaned rows.
     *
     * @param request   optional category / difficulty preferences
     * @param principal injected by Spring Security from the current auth token
     * @return initial game state
     */
    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startSoloGame(
        @Valid @RequestBody StartSoloGameRequest request,
        Principal principal,
        HttpServletRequest httpRequest
    ) {
        UUID playerId = playerIdFrom(principal);
        playerProfileService.ensureProfile(playerId);

        // Rotate anonymous session cookie on game start to prevent cross-game tracking
        if (OptionalJwtFilter.AUTH_TYPE_ANON.equals(httpRequest.getAttribute(OptionalJwtFilter.AUTH_TYPE_ATTR))) {
            httpRequest.setAttribute(OptionalJwtFilter.ROTATE_ANON_ATTR, "true");
        }
        log.debug("Starting solo game for player {}", playerId);

        // Prevent orphaned-game accumulation: abandon any in-progress games
        gameService.abandonActiveGamesForPlayer(playerId);

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

        return ResponseEntity.ok(buildGameStateResponse(game, question, match, List.of(), List.of()));
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
        Principal principal,
        HttpServletRequest httpRequest
    ) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Submitting answer for game {}: '{}'", gameId, request.getAnswer());

        GameService.MoveRecord result = gameService.processPlayerMove(gameId, playerId, request.getAnswer(), request.getEntityId());

        Game game = result.game();
        Match match = result.match();

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        GameMove move = result.move();

        // Rotate anonymous session cookie on game completion to limit exfiltration window
        if (move.getResult() == GameMove.MoveResult.CHECKOUT
                && OptionalJwtFilter.AUTH_TYPE_ANON.equals(httpRequest.getAttribute(OptionalJwtFilter.AUTH_TYPE_ATTR))) {
            httpRequest.setAttribute(OptionalJwtFilter.ROTATE_ANON_ATTR, "true");
        }

        SubmitAnswerResponse response = SubmitAnswerResponse.builder()
            .result(move.getResult().name())
            .matchedAnswer(move.getMatchedDisplayText())
            .scoreValue(move.getScoreValue())
            .scoreBefore(move.getScoreBefore())
            .scoreAfter(move.getScoreAfter())
            .reason(determineReason(move))
            .isWin(move.getResult() == GameMove.MoveResult.CHECKOUT)
            .gameState(buildGameStateResponse(game, question, match, result.usedAnswerIds(), List.of()))
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
     * Get current game state including move history.
     *
     * <p>Player identity comes from the authenticated principal.
     *
     * @param gameId    the game UUID
     * @param principal injected by Spring Security
     * @return current game state with moves, or 404 if the game does not exist
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

        List<GameMove> moves = gameService.getMovesForGame(gameId);

        return ResponseEntity.ok(buildGameStateResponse(game, question, match, moves));
    }

    /**
     * Get the current player's active in-progress game (if any).
     *
     * <p>This is the primary recovery endpoint — the frontend calls it on mount
     * after a page refresh to discover any game left in progress. If no active
     * game exists, a 404 is returned and the frontend shows the lobby.
     *
     * @param principal injected by Spring Security
     * @return current game state with moves, or 404 if no active game
     */
    @GetMapping("/games/active")
    public ResponseEntity<GameStateResponse> getActiveGame(Principal principal) {
        UUID playerId = playerIdFrom(principal);
        log.debug("Looking up active game for player {}", playerId);

        Game game = gameService.findActiveGameForPlayer(playerId).orElse(null);

        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Match match = matchService.getMatchById(game.getMatchId())
            .orElseThrow(() -> new IllegalStateException("Match not found"));

        Question question = questionService.getQuestionById(game.getQuestionId())
            .orElseThrow(() -> new IllegalStateException("Question not found"));

        List<GameMove> moves = gameService.getMovesForGame(game.getId());

        log.info("Active game found for player {}: gameId={}", playerId, game.getId());

        return ResponseEntity.ok(buildGameStateResponse(game, question, match, moves));
    }

    /**
     * Returns the player's profile if they are authenticated (has a real JWT).
     * Returns 404 for anonymous users.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        UUID playerId = playerIdFrom(principal);
        return playerProfileService.findByPlayerId(playerId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses the authenticated principal name as a {@link UUID}.
     * The principal name is the player's UUID string set by the auth filter
     * (via {@link com.football501.security.OptionalJwtFilter}).
     */
    private UUID playerIdFrom(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    private GameStateResponse buildGameStateResponse(Game game, Question question, Match match,
                                                      List<GameMove> moves) {
        return buildGameStateResponse(game, question, match, null, moves);
    }

    private GameStateResponse buildGameStateResponse(Game game, Question question, Match match,
                                                      List<UUID> usedAnswerIds, List<GameMove> moves) {
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

        List<MoveDto> moveDtos = moves != null
            ? moves.stream().map(this::toMoveDto).toList()
            : null;

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
            .moves(moveDtos)
            .build();
    }

    private MoveDto toMoveDto(GameMove move) {
        return new MoveDto(
            move.getSubmittedAnswer(),
            move.getResult().name(),
            move.getScoreBefore(),
            move.getScoreAfter(),
            move.getMatchedDisplayText(),
            move.getScoreValue()
        );
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
