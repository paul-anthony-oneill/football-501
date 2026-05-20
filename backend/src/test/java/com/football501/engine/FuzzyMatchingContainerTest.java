package com.football501.engine;

import tools.jackson.databind.ObjectMapper;
import com.football501.dto.StartPracticeRequest;
import com.football501.dto.SubmitAnswerRequest;
import com.football501.model.Answer;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.GameMoveRepository;
import com.football501.repository.GameRepository;
import com.football501.repository.MatchRepository;
import com.football501.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fuzzy player-name matching tests against a real PostgreSQL container.
 * These tests cannot run on H2 because they rely on the pg_trgm similarity() function.
 * Requires Docker to be running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fuzzy Matching Integration Tests (PostgreSQL + pg_trgm)")
class FuzzyMatchingContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withInitScript("db/init-test-trgm.sql");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private GameMoveRepository gameMoveRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private MatchRepository matchRepository;

    private UUID playerId;

    @BeforeEach
    void setUp() {
        gameMoveRepository.deleteAll();
        gameRepository.deleteAll();
        matchRepository.deleteAll();
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        categoryRepository.deleteAll();

        playerId = UUID.randomUUID();

        Category category = categoryRepository.save(Category.builder()
            .name("Football")
            .slug("football")
            .build());

        Question question = questionRepository.save(Question.builder()
            .categoryId(category.getId())
            .questionText("Goals in Test League 2024/25")
            .metricKey("goals")
            .config(Map.of())
            .build());

        // 12 answers — above DEFAULT_MIN_ANSWERS (10)
        // Include a player whose name we'll submit with a typo
        String[] players = {
            "Erling Haaland", "Mohamed Salah", "Harry Kane", "Son Heung-min",
            "Bruno Fernandes", "Kevin De Bruyne", "Marcus Rashford", "Bukayo Saka",
            "Phil Foden", "Rodri", "Trent Alexander-Arnold", "Virgil van Dijk"
        };
        for (int i = 0; i < players.length; i++) {
            answerRepository.save(Answer.builder()
                .questionId(question.getId())
                .displayText(players[i])
                .answerKey(players[i].toLowerCase())
                .score(10 + i)
                .isValidDarts(true)
                .isBust(false)
                .build());
        }
    }

    @Test
    @DisplayName("Exact name match returns VALID")
    void exactMatch_returnsValid() throws Exception {
        UUID gameId = startGame();

        mockMvc.perform(post("/api/practice/games/{id}/submit", gameId)
                .param("playerId", playerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitBody("Erling Haaland")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("VALID"))
            .andExpect(jsonPath("$.matchedAnswer").value("Erling Haaland"));
    }

    @Test
    @DisplayName("Typo in player name is resolved via trigram fuzzy matching")
    void typoInName_fuzzyMatchReturnsValid() throws Exception {
        UUID gameId = startGame();

        // "Erling Haland" (missing one 'a') should fuzzy-match "Erling Haaland"
        mockMvc.perform(post("/api/practice/games/{id}/submit", gameId)
                .param("playerId", playerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitBody("Erling Haland")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("VALID"))
            .andExpect(jsonPath("$.matchedAnswer").value("Erling Haaland"));
    }

    @Test
    @DisplayName("Completely unknown name returns INVALID")
    void unknownName_returnsInvalid() throws Exception {
        UUID gameId = startGame();

        mockMvc.perform(post("/api/practice/games/{id}/submit", gameId)
                .param("playerId", playerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitBody("Zxqwerty Fakename9999")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("INVALID"))
            .andExpect(jsonPath("$.scoreBefore").value(501))
            .andExpect(jsonPath("$.scoreAfter").value(501));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID startGame() throws Exception {
        StartPracticeRequest req = StartPracticeRequest.builder()
            .playerId(playerId)
            .categorySlug("football")
            .build();

        String body = mockMvc.perform(post("/api/practice/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("gameId").asText());
    }

    private String submitBody(String answer) throws Exception {
        return objectMapper.writeValueAsString(
            SubmitAnswerRequest.builder().answer(answer).build()
        );
    }
}
