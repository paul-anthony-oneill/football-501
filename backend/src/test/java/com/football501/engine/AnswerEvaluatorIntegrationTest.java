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
 * Integration test for AnswerEvaluator using real database.
 * Tests the complete flow including repository queries and fuzzy matching.
 *
 * Note: Requires PostgreSQL with pg_trgm extension for fuzzy matching tests.
 * H2 database can be used for basic tests but fuzzy matching will fail.
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
        // Create Category
        Category category = Category.builder()
            .name("Test Category")
            .slug("test-category")
            .build();
        category = categoryRepository.save(category);

        // Create generic Question
        Question question = Question.builder()
            .categoryId(category.getId())
            .questionText("Test Question")
            .metricKey("points")
            .config(Map.of())
            .isActive(true)
            .build();
        question = questionRepository.save(question);
        questionRepository.flush();
        
        this.questionId = question.getId();

        // Create test data
        createTestAnswers();
    }

    @Test
    @DisplayName("Full game sequence from 501 to win")
    void testFullGameSequence() {
        List<UUID> usedAnswers = new ArrayList<>();
        int currentScore = 501;

        // Turn 1: Answer A (35) -> 466
        AnswerResult result1 = evaluator.evaluateAnswer(
            questionId, "Answer A", currentScore, usedAnswers
        );
        assertThat(result1.isValid()).isTrue();
        assertThat(result1.isBust()).isFalse();
        assertThat(result1.getNewTotal()).isEqualTo(466);
        currentScore = result1.getNewTotal();
        usedAnswers.add(result1.getAnswerId());

        // Turn 2: Answer B (28) -> 438
        AnswerResult result2 = evaluator.evaluateAnswer(
            questionId, "Answer B", currentScore, usedAnswers
        );
        assertThat(result2.isValid()).isTrue();
        assertThat(result2.getNewTotal()).isEqualTo(438);
        currentScore = result2.getNewTotal();
        usedAnswers.add(result2.getAnswerId());

        // Turn 3: Invalid score (179) -> Bust, score unchanged
        AnswerResult result3 = evaluator.evaluateAnswer(
            questionId, "Answer E", currentScore, usedAnswers
        );
        assertThat(result3.isValid()).isTrue();
        assertThat(result3.isBust()).isTrue();
        assertThat(result3.getNewTotal()).isEqualTo(438); // Unchanged
        usedAnswers.add(result3.getAnswerId());

        // Turn 4: Attempt reuse -> Invalid
        AnswerResult result4 = evaluator.evaluateAnswer(
            questionId, "Answer A", currentScore, usedAnswers
        );
        assertThat(result4.isValid()).isFalse();
        assertThat(result4.getReason()).isEqualTo("Answer not found or already used");
    }

    @Test
    @DisplayName("Win condition at exactly 0")
    void testWinConditionExactZero() {
        // Setup: score is 10
        AnswerResult result = evaluator.evaluateAnswer(
            questionId, "Answer C", 10, new ArrayList<>()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.isWin()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(0);
        assertThat(result.getReason()).isEqualTo("Win!");
    }

    @Test
    @DisplayName("Win condition at -5")
    void testWinConditionNegative() {
        // Setup: score is 5
        AnswerResult result = evaluator.evaluateAnswer(
            questionId, "Answer C", 5, new ArrayList<>()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.isWin()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(-5);
    }

    @Test
    @DisplayName("Bust when going below -10")
    void testBustBelowCheckoutRange() {
        // Setup: score is 5, answer is 35
        AnswerResult result = evaluator.evaluateAnswer(
            questionId, "Answer A", 5, new ArrayList<>()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.isBust()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(5); // Score unchanged
    }

    @Test
    @DisplayName("Get available answer count")
    void testGetAvailableAnswerCount() {
        long count = evaluator.getAvailableAnswerCount(questionId, new ArrayList<>());
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("Get top answers returns sorted results")
    void testGetTopAnswers() {
        List<Answer> topAnswers = evaluator.getTopAnswers(
            questionId, 5, true
        );

        assertThat(topAnswers).isNotEmpty();

        // Verify descending order
        for (int i = 0; i < topAnswers.size() - 1; i++) {
            assertThat(topAnswers.get(i).getScore())
                .isGreaterThanOrEqualTo(topAnswers.get(i + 1).getScore());
        }
    }

    @Test
    @DisplayName("Get answer stats returns correct counts")
    void testGetAnswerStats() {
        AnswerEvaluator.AnswerStats stats = evaluator.getAnswerStats(questionId);

        assertThat(stats.totalAnswers()).isGreaterThan(0);
        assertThat(stats.validDartsAnswers()).isGreaterThan(0);
        assertThat(stats.invalidOrBustAnswers()).isGreaterThanOrEqualTo(0);
    }

    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================

    private void createTestAnswers() {
        List<TestAnswer> answers = List.of(
            new TestAnswer("Answer A", 35, true, false),
            new TestAnswer("Answer B", 28, true, false),
            new TestAnswer("Answer C", 10, true, false),
            new TestAnswer("Answer D", 5, true, false),
            new TestAnswer("Answer E", 179, false, false),
            new TestAnswer("Answer F", 200, false, true),
            new TestAnswer("Answer G", 163, false, false),
            new TestAnswer("Answer H", 1, true, false),
            new TestAnswer("Answer I", 180, true, false)
        );

        for (TestAnswer ta : answers) {
            Answer answer = Answer.builder()
                .questionId(questionId)
                .displayText(ta.text)
                .answerKey(ta.text.toLowerCase())
                .score(ta.score)
                .isValidDarts(ta.validDartsScore)
                .isBust(ta.isBust)
                .build();

            answerRepository.save(answer);
        }

        answerRepository.flush();
    }

    private record TestAnswer(String text, int score, boolean validDartsScore, boolean isBust) {}
}
