package com.football501.repository;

import com.football501.model.Answer;
import com.football501.model.Category;
import com.football501.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
            .isActive(true)
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
    @DisplayName("Fuzzy match finds answer with typo")
    void shouldFindBestMatchByFuzzyName() {
        // "haland" should match "erling haaland" with decent threshold
        Optional<Answer> result = answerRepository.findBestMatchByFuzzyName(
            questionId, "haland", null, 0.1
        );

        assertThat(result).isPresent();
        assertThat(result.get().getDisplayText()).isEqualTo("Erling Haaland");
    }

    @Test
    @DisplayName("Fuzzy match respects used answers")
    void shouldExcludeUsedAnswersInFuzzyMatch() {
        Optional<Answer> firstMatch = answerRepository.findBestMatchByFuzzyName(
            questionId, "haland", null, 0.1
        );
        assertThat(firstMatch).isPresent();
        UUID answerId = firstMatch.get().getId();

        // Search again with used ID
        Optional<Answer> secondMatch = answerRepository.findBestMatchByFuzzyName(
            questionId, "haland", List.of(answerId), 0.1
        );

        assertThat(secondMatch).isEmpty();
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
