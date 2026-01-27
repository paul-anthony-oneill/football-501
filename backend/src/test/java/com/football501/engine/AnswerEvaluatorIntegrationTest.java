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
import java.util.Optional;
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

    @Test
    @DisplayName("Real game flow using actual database questions and answers")
    void testRealGameFlow() {
        // 1. Find Premier League category
        Optional<Category> premierLeagueOpt = categoryRepository.findBySlug("premier-league");
        if (premierLeagueOpt.isEmpty()) {
            // Skip test if no real data available
            System.out.println("Skipping test: No Premier League category found. Run scraper to populate data.");
            return;
        }

        Category premierLeague = premierLeagueOpt.get();

        // 2. Get questions from that category
        List<Question> questions = questionRepository.findByCategoryIdAndIsActiveTrue(premierLeague.getId());
        if (questions.isEmpty()) {
            System.out.println("Skipping test: No active questions found for Premier League.");
            return;
        }

        Question randomQuestion = questions.get(0);

        // 3. Get real answers for this question (limit to valid darts scores only)
        List<Answer> availableAnswers = answerRepository.findTopNByQuestionIdOrderByScoreDesc(
            randomQuestion.getId(), 50
        ).stream()
            .filter(a -> a.getScore() >= 1 && a.getScore() <= 180 && a.getIsValidDarts())
            .toList();

        if (availableAnswers.size() < 3) {
            System.out.println("Skipping test: Not enough valid answers found.");
            return;
        }

        // 4. Play a realistic game using actual player names
        List<UUID> usedAnswers = new ArrayList<>();
        int currentScore = 501;

        // Turn 1: Submit exact player name
        Answer answer1 = availableAnswers.get(0);
        AnswerResult turn1 = evaluator.evaluateAnswer(
            randomQuestion.getId(),
            answer1.getDisplayText(),
            currentScore,
            usedAnswers
        );

        assertThat(turn1.isValid()).isTrue();
        assertThat(turn1.isBust()).isFalse();
        assertThat(turn1.getDisplayText()).isEqualTo(answer1.getDisplayText());
        assertThat(turn1.getScore()).isEqualTo(answer1.getScore());
        assertThat(turn1.getNewTotal()).isEqualTo(currentScore - answer1.getScore());

        usedAnswers.add(turn1.getAnswerId());
        currentScore = turn1.getNewTotal();

        // Turn 2: Submit another exact player name
        Answer answer2 = availableAnswers.get(1);
        AnswerResult turn2 = evaluator.evaluateAnswer(
            randomQuestion.getId(),
            answer2.getDisplayText(),
            currentScore,
            usedAnswers
        );

        assertThat(turn2.isValid()).isTrue();
        assertThat(turn2.isBust()).isFalse();
        assertThat(turn2.getScore()).isEqualTo(answer2.getScore());
        assertThat(turn2.getNewTotal()).isEqualTo(currentScore - answer2.getScore());

        usedAnswers.add(turn2.getAnswerId());
        currentScore = turn2.getNewTotal();

        // Turn 3: Test duplicate answer rejection
        AnswerResult turn3 = evaluator.evaluateAnswer(
            randomQuestion.getId(),
            answer1.getDisplayText(),
            currentScore,
            usedAnswers
        );

        assertThat(turn3.isValid()).isFalse();
        assertThat(turn3.getReason()).isEqualTo("Answer not found or already used");

        // Turn 4: Test case-insensitive matching
        Answer answer3 = availableAnswers.get(2);
        String lowercaseName = answer3.getDisplayText().toLowerCase();
        AnswerResult turn4 = evaluator.evaluateAnswer(
            randomQuestion.getId(),
            lowercaseName,
            currentScore,
            usedAnswers
        );

        assertThat(turn4.isValid()).isTrue();
        assertThat(turn4.getDisplayText()).isEqualTo(answer3.getDisplayText());
        assertThat(turn4.getNewTotal()).isEqualTo(currentScore - answer3.getScore());

        // Success! Real database integration works
        System.out.println("✅ Real game flow test passed with actual database data!");
        System.out.println("   Question: " + randomQuestion.getQuestionText());
        System.out.println("   Answers tested: " + answer1.getDisplayText() + ", " + answer2.getDisplayText() + ", " + answer3.getDisplayText());
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
