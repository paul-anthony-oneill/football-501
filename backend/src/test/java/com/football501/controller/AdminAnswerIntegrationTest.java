package com.football501.controller;

import tools.jackson.databind.ObjectMapper;
import com.football501.BaseTest;
import com.football501.dto.admin.BulkCreateAnswersRequest;
import com.football501.dto.admin.CreateAnswerRequest;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack HTTP integration tests for the admin answer management endpoints.
 * Covers create, bulk-create (including duplicate-skipping), list, and delete.
 */
@DisplayName("Admin Answer Integration Tests")
class AdminAnswerIntegrationTest extends BaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;

    private UUID questionId;
    private UUID unknownQuestionId;

    @BeforeEach
    void setUp() {
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(Category.builder()
            .name("Integration Test Category")
            .slug("integration-test")
            .build());

        Question question = questionRepository.save(Question.builder()
            .categoryId(category.getId())
            .questionText("Goals for Test Club in Test Cup 2024/25")
            .metricKey("goals")
            .config(Map.of())
            .build());

        questionId = question.getId();
        unknownQuestionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST single answer returns 201 with saved answer data")
    void createSingleAnswer_returns201() throws Exception {
        CreateAnswerRequest req = answerRequest("Lionel Messi", 50);

        mockMvc.perform(post("/api/admin/questions/{id}/answers", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayText").value("Lionel Messi"))
            .andExpect(jsonPath("$.score").value(50))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.isValidDarts").value(true)); // 50 is a valid darts score
    }

    @Test
    @DisplayName("POST single answer for unknown question returns 400")
    void createAnswer_unknownQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/questions/{id}/answers", unknownQuestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answerRequest("Player X", 10))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bulk create with all new answers saves all and reports correct counts")
    void bulkCreate_allNew_savesAll() throws Exception {
        BulkCreateAnswersRequest req = bulkRequest("Alpha", "Beta", "Gamma", "Delta", "Epsilon");

        mockMvc.perform(post("/api/admin/questions/{id}/answers/bulk", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(5))
            .andExpect(jsonPath("$.skipped").value(0));

        // Verify persistence via the list endpoint
        mockMvc.perform(get("/api/admin/questions/{id}/answers", questionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("Bulk create with all duplicate answers skips all and saves none")
    void bulkCreate_allDuplicates_skipsAll() throws Exception {
        BulkCreateAnswersRequest req = bulkRequest("Alpha", "Beta", "Gamma");

        // First call — all new
        mockMvc.perform(post("/api/admin/questions/{id}/answers/bulk", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(jsonPath("$.created").value(3));

        // Second call — all duplicates
        mockMvc.perform(post("/api/admin/questions/{id}/answers/bulk", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(0))
            .andExpect(jsonPath("$.skipped").value(3));
    }

    @Test
    @DisplayName("Bulk create with mixed new and duplicate answers saves only new ones")
    void bulkCreate_mixedDuplicates_savesOnlyNew() throws Exception {
        // Seed "Alpha" and "Beta" first
        mockMvc.perform(post("/api/admin/questions/{id}/answers/bulk", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkRequest("Alpha", "Beta"))))
            .andExpect(jsonPath("$.created").value(2));

        // Submit 2 duplicates + 3 new
        BulkCreateAnswersRequest mixed = bulkRequest("Alpha", "Beta", "Gamma", "Delta", "Epsilon");
        mockMvc.perform(post("/api/admin/questions/{id}/answers/bulk", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mixed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(3))
            .andExpect(jsonPath("$.skipped").value(2));

        // Final list should have all 5
        mockMvc.perform(get("/api/admin/questions/{id}/answers", questionId))
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("DELETE answer removes it from the list")
    void deleteAnswer_removesFromList() throws Exception {
        // Create a single answer and capture its ID from the response
        String createResponse = mockMvc.perform(post("/api/admin/questions/{id}/answers", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answerRequest("Cristiano Ronaldo", 40))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String answerId = objectMapper.readTree(createResponse).get("id").asText();

        // List returns 1
        mockMvc.perform(get("/api/admin/questions/{id}/answers", questionId))
            .andExpect(jsonPath("$.length()").value(1));

        // Delete it
        mockMvc.perform(delete("/api/admin/answers/{id}", answerId))
            .andExpect(status().isNoContent());

        // List now returns 0
        mockMvc.perform(get("/api/admin/questions/{id}/answers", questionId))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Answer with score 179 is flagged as invalid darts score")
    void createAnswer_invalidDartsScore_isFlaggedCorrectly() throws Exception {
        mockMvc.perform(post("/api/admin/questions/{id}/answers", questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answerRequest("Bust Player", 179))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.score").value(179))
            .andExpect(jsonPath("$.isValidDarts").value(false)); // 179 is not achievable with 3 darts
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateAnswerRequest answerRequest(String name, int score) {
        CreateAnswerRequest req = new CreateAnswerRequest();
        req.setDisplayText(name);
        req.setScore(score);
        return req;
    }

    private BulkCreateAnswersRequest bulkRequest(String... names) {
        List<CreateAnswerRequest> items = Arrays.stream(names)
            .map(name -> answerRequest(name, 10))
            .toList();
        BulkCreateAnswersRequest req = new BulkCreateAnswersRequest();
        req.setAnswers(items);
        return req;
    }
}
