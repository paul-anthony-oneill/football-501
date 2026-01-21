package com.football501.engine;

import com.football501.model.QuestionValidAnswer;
import com.football501.repository.QuestionValidAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Comprehensive tests for AnswerEvaluator.
 * Tests all critical game mechanics:
 * - Answer validation and fuzzy matching
 * - Darts score validation
 * - Bust conditions
 * - Win conditions (checkout)
 * - Used answer tracking
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Answer Evaluator Tests")
class AnswerEvaluatorTest {

    @Mock
    private QuestionValidAnswerRepository answerRepository;

    @Mock
    private ScoringService scoringService;

    private AnswerEvaluator evaluator;

    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID_HAALAND = UUID.randomUUID();
    private static final UUID PLAYER_ID_DE_BRUYNE = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evaluator = new AnswerEvaluator(answerRepository, scoringService);
    }

    // ==========================================================================
    // VALID ANSWER MATCHING
    // ==========================================================================

    @Test
    @DisplayName("Exact match returns correct answer")
    void testExactMatchValidAnswer() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            PLAYER_ID_HAALAND, "Erling Haaland", 35, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(QUESTION_ID, "erling haaland"))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(501, 35))
            .thenReturn(ScoreResult.validScore(466));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Erling Haaland", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getPlayerName()).isEqualTo("Erling Haaland");
        assertThat(result.getScore()).isEqualTo(35);
        assertThat(result.getNewTotal()).isEqualTo(466);
        assertThat(result.isBust()).isFalse();
        assertThat(result.isWin()).isFalse();
    }

    @Test
    @DisplayName("Case insensitive match works")
    void testCaseInsensitiveMatch() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            PLAYER_ID_DE_BRUYNE, "Kevin De Bruyne", 28, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(QUESTION_ID, "kevin de bruyne"))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(501, 28))
            .thenReturn(ScoreResult.validScore(473));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "KEVIN DE BRUYNE", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getPlayerName()).isEqualTo("Kevin De Bruyne");
    }

    @Test
    @DisplayName("Fuzzy matching handles typos")
    void testFuzzyMatchTypo() {
        // Given - no exact match
        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.empty());

        // Fuzzy match finds Haaland
        QuestionValidAnswer answer = createAnswer(
            PLAYER_ID_HAALAND, "Erling Haaland", 35, true, false
        );

        when(answerRepository.findBestMatchByFuzzyName(
            eq(QUESTION_ID), eq("haland"), isNull(), eq(0.5)
        )).thenReturn(Optional.of(answer));

        when(scoringService.calculateScore(501, 35))
            .thenReturn(ScoreResult.validScore(466));

        // When - typo: "Haland" instead of "Haaland"
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Haland", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getPlayerName()).isEqualTo("Erling Haaland");
    }

    // ==========================================================================
    // INVALID ANSWERS
    // ==========================================================================

    @Test
    @DisplayName("Player not found returns invalid")
    void testPlayerNotFound() {
        // Given
        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.empty());
        when(answerRepository.findBestMatchByFuzzyName(any(), any(), any(), anyDouble()))
            .thenReturn(Optional.empty());

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Lionel Messi", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).isEqualTo("Player not found or already used");
    }

    @Test
    @DisplayName("Empty answer returns invalid")
    void testEmptyAnswer() {
        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).isEqualTo("Empty answer");
    }

    @Test
    @DisplayName("Already used player returns invalid")
    void testAlreadyUsedAnswer() {
        // Given
        List<UUID> usedPlayers = List.of(PLAYER_ID_HAALAND);

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.empty());
        when(answerRepository.findBestMatchByFuzzyName(any(), any(), eq(usedPlayers), anyDouble()))
            .thenReturn(Optional.empty());

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Erling Haaland", 501, usedPlayers
        );

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).isEqualTo("Player not found or already used");
    }

    // ==========================================================================
    // DARTS SCORE VALIDATION
    // ==========================================================================

    @Test
    @DisplayName("Invalid darts score 179 results in bust")
    void testInvalidDartsScore179() {
        // Given - answer with invalid darts score
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "Jack Grealish", 179, false, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));

        // ScoringService would validate, but we pre-check
        when(scoringService.calculateScore(501, 179))
            .thenReturn(ScoreResult.bust(501));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Jack Grealish", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue(); // Answer exists
        assertThat(result.isBust()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(501); // Unchanged
        assertThat(result.getReason()).isEqualTo("Invalid darts score");
    }

    @Test
    @DisplayName("Score over 180 results in bust")
    void testScoreOver180Bust() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "John Stones", 200, false, true
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(501, 200))
            .thenReturn(ScoreResult.bust(501));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "John Stones", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isBust()).isTrue();
        assertThat(result.getScore()).isEqualTo(200);
    }

    @Test
    @DisplayName("Valid max darts score 180 works")
    void testValidDartsScoreMax() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "Kyle Walker", 180, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(501, 180))
            .thenReturn(ScoreResult.validScore(321));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Kyle Walker", 501, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isBust()).isFalse();
        assertThat(result.getNewTotal()).isEqualTo(321);
        assertThat(result.isValidDartsScore()).isTrue();
    }

    // ==========================================================================
    // WIN CONDITIONS (CHECKOUT)
    // ==========================================================================

    @Test
    @DisplayName("Exact checkout at 0 triggers win")
    void testExactCheckoutZero() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "Ederson", 10, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(10, 10))
            .thenReturn(ScoreResult.checkout(0));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Ederson", 10, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isWin()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(0);
        assertThat(result.getReason()).isEqualTo("Win!");
    }

    @Test
    @DisplayName("Checkout at -5 triggers win")
    void testCheckoutNegative5() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "Phil Foden", 5, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(0, 5))
            .thenReturn(ScoreResult.checkout(-5));

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Phil Foden", 0, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isWin()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(-5);
    }

    @Test
    @DisplayName("Below checkout range results in bust")
    void testBelowCheckoutRangeBust() {
        // Given
        QuestionValidAnswer answer = createAnswer(
            UUID.randomUUID(), "Erling Haaland", 35, true, false
        );

        when(answerRepository.findByQuestionIdAndNormalizedName(any(), any()))
            .thenReturn(Optional.of(answer));
        when(scoringService.calculateScore(20, 35))
            .thenReturn(ScoreResult.bust(20)); // Would be -15

        // When
        AnswerResult result = evaluator.evaluateAnswer(
            QUESTION_ID, "Erling Haaland", 20, new ArrayList<>()
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isBust()).isTrue();
        assertThat(result.getNewTotal()).isEqualTo(20); // Unchanged
    }

    // ==========================================================================
    // UTILITY METHODS
    // ==========================================================================

    @Test
    @DisplayName("Get available answer count works")
    void testGetAvailableAnswerCount() {
        // Given
        when(answerRepository.countAvailableAnswers(QUESTION_ID, null))
            .thenReturn(10L);

        // When
        long count = evaluator.getAvailableAnswerCount(QUESTION_ID, new ArrayList<>());

        // Then
        assertThat(count).isEqualTo(10);
    }

    @Test
    @DisplayName("Get top answers returns sorted list")
    void testGetTopAnswers() {
        // Given
        List<QuestionValidAnswer> topAnswers = List.of(
            createAnswer(UUID.randomUUID(), "Player 1", 180, true, false),
            createAnswer(UUID.randomUUID(), "Player 2", 150, true, false),
            createAnswer(UUID.randomUUID(), "Player 3", 100, true, false)
        );

        when(answerRepository.findTopAnswers(QUESTION_ID, true))
            .thenReturn(topAnswers);

        // When
        List<QuestionValidAnswer> results = evaluator.getTopAnswers(QUESTION_ID, 3, true);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getScore()).isEqualTo(180);
    }

    @Test
    @DisplayName("Get answer stats returns correct counts")
    void testGetAnswerStats() {
        // Given
        when(answerRepository.countByQuestionId(QUESTION_ID)).thenReturn(10L);
        when(answerRepository.countByQuestionIdAndIsValidDartsScoreTrue(QUESTION_ID)).thenReturn(7L);

        // When
        AnswerEvaluator.AnswerStats stats = evaluator.getAnswerStats(QUESTION_ID);

        // Then
        assertThat(stats.totalAnswers()).isEqualTo(10);
        assertThat(stats.validDartsAnswers()).isEqualTo(7);
        assertThat(stats.invalidOrBustAnswers()).isEqualTo(3);
    }

    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================

    private QuestionValidAnswer createAnswer(
        UUID playerId,
        String playerName,
        int score,
        boolean validDartsScore,
        boolean isBust
    ) {
        return QuestionValidAnswer.builder()
            .id(UUID.randomUUID())
            .questionId(QUESTION_ID)
            .playerId(playerId)
            .playerName(playerName)
            .normalizedName(playerName.toLowerCase())
            .score(score)
            .isValidDartsScore(validDartsScore)
            .isBust(isBust)
            .build();
    }
}
