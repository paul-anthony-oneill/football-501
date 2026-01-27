package com.football501.engine;

import com.football501.model.Answer;
import com.football501.model.Category;
import com.football501.model.Question;
import com.football501.repository.AnswerRepository;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AnswerEvaluator.
 * Focuses on the orchestration between Service, Repository, and Database.
 * Detailed logic tests are in AnswerEvaluatorTest and ScoringServiceTest.
 * detailed query tests are in AnswerRepositoryTest.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AnswerEvaluator.class, ScoringService.class})
@DisplayName("Answer Evaluator Integration Tests")
class AnswerEvaluatorIntegrationTest {

    @Autowired
    private AnswerEvaluator evaluator;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID questionId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
            .name("Integration Test Category")
            .slug("int-test-category")
            .build());

        Question question = questionRepository.save(Question.builder()
            .categoryId(category.getId())
            .questionText("Test Question")
            .metricKey("points")
            .config(Map.of())
            .isActive(true)
            .build());
        
        this.questionId = question.getId();
        createTestAnswers();
    }

    @Test
    @DisplayName("Full game flow: Match -> Score -> Fuzzy Match -> Bust -> Win")
    void testFullGameFlow() {
        List<UUID> usedAnswers = new ArrayList<>();
        int currentScore = 501;

        // 1. Exact Match: "Answer A" (35)
        AnswerResult turn1 = evaluator.evaluateAnswer(questionId, "Answer A", currentScore, usedAnswers);
        assertThat(turn1.isValid()).isTrue();
        assertThat(turn1.getNewTotal()).isEqualTo(466); // 501 - 35
        usedAnswers.add(turn1.getAnswerId());
        currentScore = turn1.getNewTotal();

        // 2. Fuzzy Match: "answr b" -> "Answer B" (28)
        AnswerResult turn2 = evaluator.evaluateAnswer(questionId, "answr b", currentScore, usedAnswers);
        assertThat(turn2.isValid()).isTrue();
        assertThat(turn2.getDisplayText()).isEqualTo("Answer B");
        assertThat(turn2.getNewTotal()).isEqualTo(438); // 466 - 28
        usedAnswers.add(turn2.getAnswerId());
        currentScore = turn2.getNewTotal();

        // 3. Invalid Darts Score: "Answer E" (179) -> Bust
        AnswerResult turn3 = evaluator.evaluateAnswer(questionId, "Answer E", currentScore, usedAnswers);
        assertThat(turn3.isValid()).isTrue();
        assertThat(turn3.isBust()).isTrue();
        assertThat(turn3.getReason()).isEqualTo("Invalid darts score");
        assertThat(turn3.getNewTotal()).isEqualTo(438); // Unchanged
        usedAnswers.add(turn3.getAnswerId());

        // 4. Duplicate Answer: "Answer A" -> Invalid
        AnswerResult turn4 = evaluator.evaluateAnswer(questionId, "Answer A", currentScore, usedAnswers);
        assertThat(turn4.isValid()).isFalse();
        assertThat(turn4.getReason()).isEqualTo("Answer not found or already used");
    }

    private void createTestAnswers() {
        List<Answer> answers = List.of(
            createAnswer("Answer A", 35, true),
            createAnswer("Answer B", 28, true),
            createAnswer("Answer E", 179, false) // Invalid darts score
        );
        answerRepository.saveAll(answers);
        answerRepository.flush();
    }

    private Answer createAnswer(String text, int score, boolean validDarts) {
        return Answer.builder()
            .questionId(questionId)
            .displayText(text)
            .answerKey(text.toLowerCase())
            .score(score)
            .isValidDarts(validDarts)
            .isBust(false)
            .build();
    }
}
