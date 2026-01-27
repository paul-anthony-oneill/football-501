package com.football501.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football501.dto.GameStateResponse;
import com.football501.dto.StartPracticeRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.model.*;
import com.football501.service.GameService;
import com.football501.service.MatchService;
import com.football501.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Tests for PracticeGameController.
 *
 * Test scenarios:
 * 1. Start practice game - returns game state with question
 * 2. Submit valid answer - score deducted, returns result
 * 3. Submit invalid answer - returns invalid result
 * 4. Submit checkout answer - returns win result
 * 5. Get game state - returns current game state
 * 6. Start practice game with invalid category - returns 400
 */
@WebMvcTest(PracticeGameController.class)
@DisplayName("PracticeGameController Tests")
class PracticeGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchService matchService;

    @MockBean
    private GameService gameService;

    @MockBean
    private QuestionService questionService;

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
        playerId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
            .id(categoryId)
            .name("Football")
            .slug("football")
            .build();

        match = Match.builder()
            .id(matchId)
            .player1Id(playerId)
            .player2Id(null) // Practice mode - no opponent
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
            .isActive(true)
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
    @DisplayName("Should start practice game and return game state")
    void shouldStartPracticeGame() throws Exception {
        // Given
        StartPracticeRequest request = StartPracticeRequest.builder()
            .playerId(playerId)
            .categorySlug("football")
            .build();

        when(questionService.getCategoryBySlug("football")).thenReturn(Optional.of(category));
        when(matchService.createMatch(eq(playerId), isNull(), eq(categoryId),
            eq(Match.MatchType.CASUAL), eq(Match.MatchFormat.BEST_OF_1)))
            .thenReturn(match);
        when(matchService.startNextGame(matchId)).thenReturn(game);
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));

        // When/Then
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
            .andExpect(jsonPath("$.isWin").value(false));
    }

    @Test
    @DisplayName("Should submit valid answer and return result with updated state")
    void shouldSubmitValidAnswer() throws Exception {
        // Given
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
            .id(gameId)
            .matchId(matchId)
            .gameNumber(1)
            .questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS)
            .currentTurnPlayerId(playerId)
            .player1Score(465)
            .player2Score(501)
            .turnCount(1)
            .turnTimerSeconds(45)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Erling Haaland")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(updatedGame));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));

        // When/Then
        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .param("playerId", playerId.toString())
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
            .andExpect(jsonPath("$.gameState.turnCount").value(1));
    }

    @Test
    @DisplayName("Should submit invalid answer and return invalid result")
    void shouldSubmitInvalidAnswer() throws Exception {
        // Given
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
            .answer("Unknown Player")
            .build();

        GameMove move = GameMove.builder()
            .id(UUID.randomUUID())
            .gameId(gameId)
            .playerId(playerId)
            .moveNumber(1)
            .submittedAnswer("Unknown Player")
            .result(GameMove.MoveResult.INVALID)
            .scoreBefore(501)
            .scoreAfter(501)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Unknown Player")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(game));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));

        // When/Then
        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .param("playerId", playerId.toString())
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
        // Given - Player at score 35
        Game gameAtLowScore = Game.builder()
            .id(gameId)
            .matchId(matchId)
            .gameNumber(1)
            .questionId(questionId)
            .status(Game.GameStatus.IN_PROGRESS)
            .currentTurnPlayerId(playerId)
            .player1Score(35)
            .player2Score(501)
            .turnCount(10)
            .turnTimerSeconds(45)
            .build();

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
            .answer("Player with 35")
            .build();

        UUID answerId = UUID.randomUUID();

        GameMove move = GameMove.builder()
            .id(UUID.randomUUID())
            .gameId(gameId)
            .playerId(playerId)
            .moveNumber(11)
            .submittedAnswer("Player with 35")
            .matchedAnswerId(answerId)
            .matchedDisplayText("Player Name")
            .result(GameMove.MoveResult.CHECKOUT)
            .scoreValue(35)
            .scoreBefore(35)
            .scoreAfter(0)
            .build();

        Game completedGame = Game.builder()
            .id(gameId)
            .matchId(matchId)
            .gameNumber(1)
            .questionId(questionId)
            .status(Game.GameStatus.COMPLETED)
            .currentTurnPlayerId(playerId)
            .player1Score(0)
            .player2Score(501)
            .winnerId(playerId)
            .turnCount(11)
            .turnTimerSeconds(45)
            .build();

        when(gameService.processPlayerMove(gameId, playerId, "Player with 35")).thenReturn(move);
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(completedGame));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));

        // When/Then
        mockMvc.perform(post("/api/practice/games/{gameId}/submit", gameId)
                .param("playerId", playerId.toString())
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
        // Given
        when(gameService.getGameById(gameId)).thenReturn(Optional.of(game));
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));
        when(matchService.getMatchById(matchId)).thenReturn(Optional.of(match));

        // When/Then
        mockMvc.perform(get("/api/practice/games/{gameId}", gameId)
                .param("playerId", playerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(gameId.toString()))
            .andExpect(jsonPath("$.questionText").value("Appearances for Manchester City in Premier League 2023/24"))
            .andExpect(jsonPath("$.currentScore").value(501))
            .andExpect(jsonPath("$.turnCount").value(0))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Should return 404 when game not found")
    void shouldReturn404WhenGameNotFound() throws Exception {
        // Given
        UUID nonExistentGameId = UUID.randomUUID();
        when(gameService.getGameById(nonExistentGameId)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/practice/games/{gameId}", nonExistentGameId)
                .param("playerId", playerId.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when category not found")
    void shouldReturn400WhenCategoryNotFound() throws Exception {
        // Given
        StartPracticeRequest request = StartPracticeRequest.builder()
            .playerId(playerId)
            .categorySlug("invalid-category")
            .build();

        when(questionService.getCategoryBySlug("invalid-category")).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should use default category when not specified")
    void shouldUseDefaultCategory() throws Exception {
        // Given
        StartPracticeRequest request = StartPracticeRequest.builder()
            .playerId(playerId)
            .build(); // No category specified

        when(questionService.getCategoryBySlug("football")).thenReturn(Optional.of(category));
        when(matchService.createMatch(eq(playerId), isNull(), eq(categoryId),
            eq(Match.MatchType.CASUAL), eq(Match.MatchFormat.BEST_OF_1)))
            .thenReturn(match);
        when(matchService.startNextGame(matchId)).thenReturn(game);
        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));

        // When/Then
        mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").exists());
    }
}
