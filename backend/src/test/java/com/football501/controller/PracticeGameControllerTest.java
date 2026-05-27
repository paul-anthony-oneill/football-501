package com.football501.controller;

import com.football501.dto.GameHints;
import com.football501.dto.StartPracticeRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.model.*;
import com.football501.security.DevModeAuthFilter;
import com.football501.service.GameHintsService;
import com.football501.service.GameService;
import com.football501.service.MatchService;
import com.football501.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link PracticeGameController}.
 *
 * <h3>Authentication</h3>
 * Class-level {@code @WithMockUser} provides a principal whose name is
 * {@link DevModeAuthFilter#DEV_PLAYER_ID}.  The controller reads player
 * identity via {@code Principal.getName()}, so the mock stubs are keyed on
 * {@code UUID.fromString(DEV_PLAYER_ID)} — the same value the real
 * {@link DevModeAuthFilter} injects during local development.
 */
@WebMvcTest(PracticeGameController.class)
@Import(JacksonAutoConfiguration.class)
@WithMockUser(username = DevModeAuthFilter.DEV_PLAYER_ID, roles = {"USER", "ADMIN"})
@DisplayName("PracticeGameController Tests")
class PracticeGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private GameHintsService gameHintsService;

    /** Stub hints returned by the service — non-zero so assertions are meaningful. */
    private static final GameHints STUB_HINTS = GameHints.builder()
        .maxScoresLeft(3)
        .checkoutsLeft(0)
        .build();

    /**
     * Player ID that matches the principal injected by {@code @WithMockUser}
     * and by {@link DevModeAuthFilter} in real dev/test runs.
     */
    private UUID playerId;
    private UUID matchId;
    private UUID gameId;
    private UUID questionId;
    private UUID categoryId;
    private Match match;
    private Game game;
    private Question question;
    private Category category;

    @BeforeEach
    void setUp() {
        // Must match the @WithMockUser username so controller-resolved principal == mock stub key
        playerId  = UUID.fromString(DevModeAuthFilter.DEV_PLAYER_ID);
        matchId   = UUID.randomUUID();
        gameId    = UUID.randomUUID();
        questionId  = UUID.randomUUID();
        categoryId  = UUID.randomUUID();

        category = Category.builder()
            .id(categoryId)
            .name("Football")
            .slug("football")
            .build();

        match = Match.builder()
            .id(matchId)
            .player1Id(playerId)
            .player2Id(null)
            .type(Match.MatchType.CASUAL)
            .format(Match.MatchFormat.BEST_OF_1)
            .status(Match.MatchStatus.IN_PROGRESS)
            .categoryId(categoryId)
            .build();

        question = Question.builder()
            .id(questionId)
            .categoryId(categoryId)
            .questionText("Appearances for Manchester City in Premier League 2023/24")
            .metricKey("appearances")
            .status(Question.STATUS_ACTIVE)
            .build();

        game = Game.builder()
            .id(gameId)
            .matchId(matchId)
            .gameNumber(1)
            .questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS)
            .currentTurnPlayerId(playerId)
            .player1Score(501)
            .player2Score(501)
            .turnCount(0)
            .turnTimerSeconds(45)
            .build();
    }

    @Test
    @DisplayName("Should start practice game and return game state with hints")
    void shouldStartPracticeGame() throws Exception {
        StartPracticeRequest request = StartPracticeRequest.builder()
            .categorySlug("football")
            .build();

        when(questionService.getCategoryBySlug("football")).thenReturn(Optional.of(category));
        when(matchService.createMatch(eq(playerId), isNull(), eq(categoryId),
            eq(Match.MatchType.CASUAL), eq(Match.MatchFormat.BEST_OF_1), isNull()))
            .thenReturn(match);
        when(matchService.startNextGame(matchId)).thenReturn(game);
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(501))).thenReturn(STUB_HINTS);

        mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(gameId.toString()))
            .andExpect(jsonPath("$.matchId").value(matchId.toString()))
            .andExpect(jsonPath("$.questionText").value("Appearances for Manchester City in Premier League 2023/24"))
            .andExpect(jsonPath("$.currentScore").value(501))
            .andExpect(jsonPath("$.turnCount").value(0))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.isWin").value(false))
            .andExpect(jsonPath("$.hints.maxScoresLeft").value(3))
            .andExpect(jsonPath("$.hints.checkoutsLeft").value(0));
    }

    @Test
    @DisplayName("Should submit valid answer and return result with updated game state")
    void shouldSubmitValidAnswer() throws Exception {
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
            .answer("Erling Haaland")
            .build();

        UUID answerId = UUID.randomUUID();

        GameMove move = GameMove.builder()
            .id(UUID.randomUUID())
            .gameId(gameId)
            .playerId(playerId)
            .moveNumber(1)
            .submittedAnswer("Erling Haaland")
            .matchedAnswerId(answerId)
            .matchedDisplayText("Erling Haaland")
            .result(GameMove.MoveResult.VALID)
            .scoreValue(36)
            .scoreBefore(501)
            .scoreAfter(465)
            .build();

        Game updatedGame = Game.builder()
            .id(gameId).matchId(matchId).gameNumber(1).questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS).currentTurnPlayerId(playerId)
            .player1Score(465).player2Score(501).turnCount(1).turnTimerSeconds(45)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Erling Haaland")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(updatedGame));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(465))).thenReturn(STUB_HINTS);

        // No @RequestParam playerId — identity comes from the authenticated principal
        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("VALID"))
            .andExpect(jsonPath("$.matchedAnswer").value("Erling Haaland"))
            .andExpect(jsonPath("$.scoreValue").value(36))
            .andExpect(jsonPath("$.scoreBefore").value(501))
            .andExpect(jsonPath("$.scoreAfter").value(465))
            .andExpect(jsonPath("$.isWin").value(false))
            .andExpect(jsonPath("$.gameState.currentScore").value(465))
            .andExpect(jsonPath("$.gameState.turnCount").value(1))
            .andExpect(jsonPath("$.gameState.hints.maxScoresLeft").value(3))
            .andExpect(jsonPath("$.gameState.hints.checkoutsLeft").value(0));
    }

    @Test
    @DisplayName("Should submit invalid answer and return invalid result")
    void shouldSubmitInvalidAnswer() throws Exception {
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
            .answer("Unknown Player")
            .build();

        GameMove move = GameMove.builder()
            .id(UUID.randomUUID()).gameId(gameId).playerId(playerId)
            .moveNumber(1).submittedAnswer("Unknown Player")
            .result(GameMove.MoveResult.INVALID).scoreBefore(501).scoreAfter(501)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Unknown Player")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(game));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(501))).thenReturn(STUB_HINTS);

        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("INVALID"))
            .andExpect(jsonPath("$.scoreBefore").value(501))
            .andExpect(jsonPath("$.scoreAfter").value(501))
            .andExpect(jsonPath("$.isWin").value(false))
            .andExpect(jsonPath("$.gameState.currentScore").value(501));
    }

    @Test
    @DisplayName("Should submit checkout answer and return win result")
    void shouldSubmitCheckoutAnswer() throws Exception {
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
            .answer("Player with 35")
            .build();

        GameMove move = GameMove.builder()
            .id(UUID.randomUUID()).gameId(gameId).playerId(playerId)
            .moveNumber(11).submittedAnswer("Player with 35")
            .matchedAnswerId(UUID.randomUUID()).matchedDisplayText("Player Name")
            .result(GameMove.MoveResult.CHECKOUT).scoreValue(35).scoreBefore(35).scoreAfter(0)
            .build();

        Game completedGame = Game.builder()
            .id(gameId).matchId(matchId).gameNumber(1).questionId(questionId)
            .status(Game.GameStatus.COMPLETED).currentTurnPlayerId(playerId)
            .player1Score(0).player2Score(501).winnerId(playerId).turnCount(11).turnTimerSeconds(45)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Player with 35")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(completedGame));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(0)))
            .thenReturn(GameHints.builder().maxScoresLeft(0).checkoutsLeft(0).build());

        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("CHECKOUT"))
            .andExpect(jsonPath("$.scoreAfter").value(0))
            .andExpect(jsonPath("$.isWin").value(true))
            .andExpect(jsonPath("$.gameState.status").value("COMPLETED"))
            .andExpect(jsonPath("$.gameState.isWin").value(true));
    }

    @Test
    @DisplayName("Should get current game state")
    void shouldGetGameState() throws Exception {
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(game));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(501))).thenReturn(STUB_HINTS);

        // No @RequestParam playerId — identity comes from the authenticated principal
        mockMvc.perform(get("/api/practice/games/{gameId}", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(gameId.toString()))
            .andExpect(jsonPath("$.questionText").value("Appearances for Manchester City in Premier League 2023/24"))
            .andExpect(jsonPath("$.currentScore").value(501))
            .andExpect(jsonPath("$.turnCount").value(0))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.hints.maxScoresLeft").value(3))
            .andExpect(jsonPath("$.hints.checkoutsLeft").value(0));
    }

    @Test
    @DisplayName("Should return 404 when game not found")
    void shouldReturn404WhenGameNotFound() throws Exception {
        UUID nonExistentGameId = UUID.randomUUID();
        when(gameService.getGameById(nonExistentGameId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/practice/games/{gameId}", nonExistentGameId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when category not found")
    void shouldReturn400WhenCategoryNotFound() throws Exception {
        StartPracticeRequest request = StartPracticeRequest.builder()
            .categorySlug("invalid-category")
            .build();

        when(questionService.getCategoryBySlug("invalid-category")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should use default category when not specified")
    void shouldUseDefaultCategory() throws Exception {
        StartPracticeRequest request = StartPracticeRequest.builder().build();

        when(questionService.getCategoryBySlug("football")).thenReturn(Optional.of(category));
        when(matchService.createMatch(eq(playerId), isNull(), eq(categoryId),
            eq(Match.MatchType.CASUAL), eq(Match.MatchFormat.BEST_OF_1), isNull()))
            .thenReturn(match);
        when(matchService.startNextGame(matchId)).thenReturn(game);
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(gameHintsService.computeHints(eq(gameId), eq(questionId), eq(501))).thenReturn(STUB_HINTS);

        mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").exists());
    }
}
