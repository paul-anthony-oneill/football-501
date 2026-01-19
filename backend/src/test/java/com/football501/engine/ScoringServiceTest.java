package com.football501.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ScoringService.
 *
 * Game Rules:
 * - Players start at 501
 * - Valid scores are deducted from current score
 * - Invalid darts scores (163, 166, 169, 172, 173, 175, 176, 178, 179) = BUST (no deduction)
 * - Scores > 180 = BUST (no deduction)
 * - Scores resulting in < -10 = BUST (no deduction)
 * - Checkout range: -10 to 0 (inclusive) = WIN
 */
class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        // RED: ScoringService doesn't exist yet
        scoringService = new ScoringService();
    }

    // ========== Valid Score Deduction Tests ==========

    @Test
    void submitAnswer_validScore_deductsFromCurrentScore() {
        // RED: Should deduct 36 from 501
        int currentScore = 501;
        int answerScore = 36;

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertFalse(result.isBust(), "Valid score should not be a bust");
        assertEquals(465, result.getNewScore(), "501 - 36 should equal 465");
    }

    @Test
    void submitAnswer_multipleValidScores_deductSequentially() {
        // RED: Multiple deductions in sequence
        int score1 = scoringService.calculateScore(501, 60).getNewScore();
        assertEquals(441, score1, "First deduction: 501 - 60 = 441");

        int score2 = scoringService.calculateScore(score1, 100).getNewScore();
        assertEquals(341, score2, "Second deduction: 441 - 100 = 341");

        int score3 = scoringService.calculateScore(score2, 41).getNewScore();
        assertEquals(300, score3, "Third deduction: 341 - 41 = 300");
    }

    // ========== Bust Detection Tests ==========

    @ParameterizedTest
    @ValueSource(ints = {163, 166, 169, 172, 173, 175, 176, 178, 179})
    void submitAnswer_invalidDartsScore_isBust(int invalidScore) {
        // RED: Invalid darts scores should result in bust
        int currentScore = 501;

        ScoreResult result = scoringService.calculateScore(currentScore, invalidScore);

        assertTrue(result.isBust(),
            String.format("Score %d is invalid darts score and should be bust", invalidScore));
        assertEquals(currentScore, result.getNewScore(),
            "Bust should not change current score");
    }

    @Test
    void submitAnswer_scoreAbove180_isBust() {
        // RED: Scores > 180 are impossible and should bust
        int currentScore = 501;
        int impossibleScore = 181;

        ScoreResult result = scoringService.calculateScore(currentScore, impossibleScore);

        assertTrue(result.isBust(), "Scores above 180 should be bust");
        assertEquals(currentScore, result.getNewScore(), "Bust should not change score");
    }

    @Test
    void submitAnswer_scoreBelowMinusTen_isBust() {
        // RED: Scores resulting in < -10 are bust (outside checkout range)
        int currentScore = 15;
        int answerScore = 30; // Would result in -15

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertTrue(result.isBust(), "Score resulting in < -10 should be bust");
        assertEquals(currentScore, result.getNewScore(), "Bust should not change score");
    }

    @Test
    void submitAnswer_zeroScore_isBust() {
        // RED: Zero is not a valid darts score
        int currentScore = 501;
        int answerScore = 0;

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertTrue(result.isBust(), "Zero score should be bust");
        assertEquals(currentScore, result.getNewScore(), "Bust should not change score");
    }

    @Test
    void submitAnswer_negativeScore_isBust() {
        // RED: Negative scores are not valid
        int currentScore = 501;
        int answerScore = -10;

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertTrue(result.isBust(), "Negative score should be bust");
        assertEquals(currentScore, result.getNewScore(), "Bust should not change score");
    }

    // ========== Checkout Tests ==========

    @Test
    void submitAnswer_resultExactlyZero_isCheckout() {
        // RED: Scoring exactly 0 is a valid checkout
        int currentScore = 36;
        int answerScore = 36;

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertFalse(result.isBust(), "Exact checkout should not be bust");
        assertEquals(0, result.getNewScore(), "Should reach exactly 0");
        assertTrue(result.isCheckout(), "Result of 0 should be checkout");
    }

    @Test
    void submitAnswer_resultWithinCheckoutRange_isCheckout() {
        // RED: Scores between -10 and 0 (inclusive) are valid checkouts
        int currentScore = 36;
        int answerScore = 40; // Results in -4

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertFalse(result.isBust(), "Checkout range should not be bust");
        assertEquals(-4, result.getNewScore(), "Should reach -4");
        assertTrue(result.isCheckout(), "Score in range -10 to 0 should be checkout");
    }

    @Test
    void submitAnswer_resultExactlyMinusTen_isCheckout() {
        // RED: -10 is the minimum valid checkout
        int currentScore = 30;
        int answerScore = 40; // Results in -10

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertFalse(result.isBust(), "Checkout at -10 should not be bust");
        assertEquals(-10, result.getNewScore(), "Should reach exactly -10");
        assertTrue(result.isCheckout(), "Score of -10 should be checkout");
    }

    @Test
    void submitAnswer_resultMinusEleven_isBust() {
        // RED: -11 is just outside checkout range and should bust
        int currentScore = 30;
        int answerScore = 41; // Would result in -11

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertTrue(result.isBust(), "Score of -11 should be bust");
        assertEquals(currentScore, result.getNewScore(), "Bust should not change score");
        assertFalse(result.isCheckout(), "Bust should not be checkout");
    }

    // ========== Edge Cases ==========

    @Test
    void submitAnswer_startingScore501_maxScore180_calculatesCorrectly() {
        // RED: Maximum valid score deduction from starting position
        int currentScore = 501;
        int maxScore = 180;

        ScoreResult result = scoringService.calculateScore(currentScore, maxScore);

        assertFalse(result.isBust(), "Max score should be valid");
        assertEquals(321, result.getNewScore(), "501 - 180 = 321");
    }

    @Test
    void submitAnswer_scoreOfOne_deductsCorrectly() {
        // RED: Minimum valid score
        int currentScore = 100;
        int minScore = 1;

        ScoreResult result = scoringService.calculateScore(currentScore, minScore);

        assertFalse(result.isBust(), "Score of 1 should be valid");
        assertEquals(99, result.getNewScore(), "100 - 1 = 99");
    }

    @Test
    void submitAnswer_currentScoreAlreadyInCheckoutRange_cannotGoLower() {
        // RED: Once in checkout range, trying to score again should bust
        int currentScore = -5; // Already in checkout range
        int answerScore = 10;

        ScoreResult result = scoringService.calculateScore(currentScore, answerScore);

        assertTrue(result.isBust(), "Cannot score again once in checkout range");
        assertEquals(currentScore, result.getNewScore(), "Score should not change");
    }
}
