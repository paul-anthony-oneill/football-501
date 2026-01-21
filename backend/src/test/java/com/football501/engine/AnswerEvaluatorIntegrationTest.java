package com.football501.engine;

import com.football501.model.QuestionValidAnswer;
import com.football501.repository.QuestionValidAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
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
    private QuestionValidAnswerRepository answerRepository;

    @Autowired
    private com.football501.repository.QuestionRepository questionRepository;

    @Autowired
    private com.football501.repository.PlayerRepository playerRepository;

    private UUID questionId;

    @BeforeEach
    void setUp() {
        // Create and save a Question entity to satisfy foreign key constraints
        com.football501.model.Question question = com.football501.model.Question.builder()
            .questionText("Test Question")
            .statType("appearances")
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
        List<UUID> usedPlayers = new ArrayList<>();
        int currentScore = 501;

        // Turn 1: Haaland (35) -> 466
        AnswerResult result1 = evaluator.evaluateAnswer(
            questionId, "Erling Haaland", currentScore, usedPlayers
        );
        assertThat(result1.isValid()).isTrue();
        assertThat(result1.isBust()).isFalse();
        assertThat(result1.getNewTotal()).isEqualTo(466);
        currentScore = result1.getNewTotal();
        usedPlayers.add(result1.getPlayerId());

        // Turn 2: De Bruyne (28) -> 438
        AnswerResult result2 = evaluator.evaluateAnswer(
            questionId, "Kevin De Bruyne", currentScore, usedPlayers
        );
        assertThat(result2.isValid()).isTrue();
        assertThat(result2.getNewTotal()).isEqualTo(438);
        currentScore = result2.getNewTotal();
        usedPlayers.add(result2.getPlayerId());

        // Turn 3: Invalid score (179) -> Bust, score unchanged
        AnswerResult result3 = evaluator.evaluateAnswer(
            questionId, "Jack Grealish", currentScore, usedPlayers
        );
        assertThat(result3.isValid()).isTrue();
        assertThat(result3.isBust()).isTrue();
        assertThat(result3.getNewTotal()).isEqualTo(438); // Unchanged
        usedPlayers.add(result3.getPlayerId());

        // Turn 4: Attempt reuse -> Invalid
        AnswerResult result4 = evaluator.evaluateAnswer(
            questionId, "Erling Haaland", currentScore, usedPlayers
        );
        assertThat(result4.isValid()).isFalse();
        assertThat(result4.getReason()).isEqualTo("Player not found or already used");
    }

    @Test
    @DisplayName("Win condition at exactly 0")
    void testWinConditionExactZero() {
        // Setup: score is 10
        AnswerResult result = evaluator.evaluateAnswer(
            questionId, "Ederson", 10, new ArrayList<>()
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
            questionId, "Ederson", 5, new ArrayList<>()
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
            questionId, "Erling Haaland", 5, new ArrayList<>()
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
        List<QuestionValidAnswer> topAnswers = evaluator.getTopAnswers(
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
        List<TestPlayer> players = List.of(
            new TestPlayer("Erling Haaland", 35, true, false),
            new TestPlayer("Kevin De Bruyne", 28, true, false),
            new TestPlayer("Ederson", 10, true, false),
            new TestPlayer("Phil Foden", 5, true, false),
            new TestPlayer("Jack Grealish", 179, false, false),
            new TestPlayer("John Stones", 200, false, true),
            new TestPlayer("Bernardo Silva", 163, false, false),
            new TestPlayer("Manuel Akanji", 1, true, false),
            new TestPlayer("Kyle Walker", 180, true, false)
        );

        for (TestPlayer p : players) {
            // Create and save Player first to satisfy FK
            com.football501.model.Player playerEntity = com.football501.model.Player.builder()
                .name(p.name)
                .normalizedName(p.name.toLowerCase())
                .fbrefId("test-" + p.name.toLowerCase().replace(" ", "-"))
                .careerStats(new ArrayList<>())
                .build();
            playerEntity = playerRepository.save(playerEntity);

            QuestionValidAnswer answer = QuestionValidAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(questionId)
                .playerId(playerEntity.getId())
                .playerName(p.name)
                .normalizedName(p.name.toLowerCase())
                .score(p.score)
                .isValidDartsScore(p.validDartsScore)
                .isBust(p.isBust)
                .build();

            answerRepository.save(answer);
        }

        playerRepository.flush();
        answerRepository.flush();
    }

    private record TestPlayer(String name, int score, boolean validDartsScore, boolean isBust) {}
}
