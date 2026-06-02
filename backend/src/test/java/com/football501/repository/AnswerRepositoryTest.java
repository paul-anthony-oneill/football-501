package com.football501.repository;

import com.football501.config.JpaConfig;
import com.football501.model.Answer;
import com.football501.model.Category;
import com.football501.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("Answer Repository Tests")
class AnswerRepositoryTest {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID questionId;

    @BeforeEach
    void setUp() {
        // Create Category
        Category category = Category.builder()
            .name("Repo Test Category")
            .slug("repo-test-category")
            .build();
        entityManager.persist(category);

        // Create Question
        Question question = Question.builder()
            .categoryId(category.getId())
            .questionText("Test Question")
            .metricKey("points")
            .config(Map.of())
            .status(Question.STATUS_ACTIVE)
            .build();
        entityManager.persist(question);
        
        this.questionId = question.getId();

        // Create Answers
        createAnswer("Erling Haaland", 35, true);
        createAnswer("Kevin De Bruyne", 28, true);
        createAnswer("Ederson", 10, true);
        
        entityManager.flush();
    }

    @Test
    @DisplayName("Find by exact key")
    void shouldFindByExactKey() {
        Optional<Answer> result = answerRepository.findByQuestionIdAndAnswerKey(
            questionId, "erling haaland"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getDisplayText()).isEqualTo("Erling Haaland");
    }

    @Test
    @DisplayName("Count available answers respects exclusions")
    void shouldCountAvailableAnswers() {
        long total = answerRepository.countAvailableAnswers(questionId, null);
        assertThat(total).isEqualTo(3);

        Optional<Answer> answer = answerRepository.findByQuestionIdAndAnswerKey(
            questionId, "erling haaland"
        );
        long remaining = answerRepository.countAvailableAnswers(
            questionId, List.of(answer.get().getId())
        );
        assertThat(remaining).isEqualTo(2);
    }

    @Test
    @DisplayName("Find top answers sorts correctly")
    void shouldFindTopAnswers() {
        List<Answer> top = answerRepository.findTopAnswers(questionId, false);

        assertThat(top).hasSize(3);
        assertThat(top.get(0).getScore()).isEqualTo(35); // Haaland
        assertThat(top.get(1).getScore()).isEqualTo(28); // KDB
        assertThat(top.get(2).getScore()).isEqualTo(10); // Ederson
    }

    // ── Hint query tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("countRemainingMaxScores — no 180s → returns 0")
    void countMaxScores_noMaxScoreAnswers_returnsZero() {
        // setUp seeds scores 35, 28, 10 — none are 180
        long count = answerRepository.countRemainingMaxScores(questionId, List.of());
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("countRemainingMaxScores — with a 180 answer → returns 1")
    void countMaxScores_oneMaxScoreAnswer_returnsOne() {
        createAnswer("Max Scorer", 180, true);
        entityManager.flush();

        long count = answerRepository.countRemainingMaxScores(questionId, List.of());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countRemainingMaxScores — 180 already used → excluded from count")
    void countMaxScores_usedAnswerExcluded() {
        createAnswer("Max Scorer", 180, true);
        entityManager.flush();

        UUID maxAnswerId = answerRepository
            .findByQuestionIdAndAnswerKey(questionId, "max scorer")
            .orElseThrow()
            .getId();

        long withExclusion = answerRepository.countRemainingMaxScores(questionId, List.of(maxAnswerId));
        assertThat(withExclusion).isEqualTo(0);
    }

    @Test
    @DisplayName("countRemainingCheckouts — answer in range → returns 1")
    void countCheckouts_answerInRange_returnsOne() {
        // Current score = 35; checkout range = [35, 45]
        // Existing answer at score 35 ("Erling Haaland") qualifies
        long count = answerRepository.countRemainingCheckouts(questionId, 35, 45, List.of());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countRemainingCheckouts — answer out of range → returns 0")
    void countCheckouts_answerOutOfRange_returnsZero() {
        // Current score = 10; checkout range = [10, 20] — only Ederson (10) qualifies
        // But score 28 and 35 are outside → only 1
        long count = answerRepository.countRemainingCheckouts(questionId, 10, 20, List.of());
        assertThat(count).isEqualTo(1); // only Ederson (10)
    }

    @Test
    @DisplayName("countRemainingCheckouts — checkout answer already used → returns 0")
    void countCheckouts_usedAnswerExcluded() {
        // Ederson (score=10) would check out from score 10
        UUID edersonId = answerRepository
            .findByQuestionIdAndAnswerKey(questionId, "ederson")
            .orElseThrow()
            .getId();

        long count = answerRepository.countRemainingCheckouts(questionId, 10, 20, List.of(edersonId));
        assertThat(count).isEqualTo(0);
    }

    private void createAnswer(String text, int score, boolean validDarts) {
        Answer answer = Answer.builder()
            .questionId(questionId)
            .displayText(text)
            .answerKey(text.toLowerCase())
            .score(score)
            .isValidDarts(validDarts)
            .isBust(false)
            .build();
        entityManager.persist(answer);
    }
}
