package com.football501.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for DartsValidator.
 *
 * Invalid darts scores (cannot be achieved with 3 darts in standard 501):
 * 163, 166, 169, 172, 173, 175, 176, 178, 179
 *
 * All other scores from 1-180 are valid.
 */
class DartsValidatorTest {

    @ParameterizedTest
    @ValueSource(ints = {163, 166, 169, 172, 173, 175, 176, 178, 179})
    void invalidDartsScores_shouldReturnFalse(int score) {
        // RED: This test should fail because DartsValidator doesn't exist yet
        boolean result = DartsValidator.isValidDartsScore(score);

        assertFalse(result,
            String.format("Score %d is not achievable with 3 darts and should be invalid", score));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 20, 60, 100, 120, 140, 160, 164, 165, 167, 168, 170, 171, 174, 177, 180})
    void validDartsScores_shouldReturnTrue(int score) {
        // RED: This test should fail because DartsValidator doesn't exist yet
        boolean result = DartsValidator.isValidDartsScore(score);

        assertTrue(result,
            String.format("Score %d is achievable with 3 darts and should be valid", score));
    }

    @Test
    void zeroScore_shouldReturnFalse() {
        // RED: Zero is not a valid darts score
        boolean result = DartsValidator.isValidDartsScore(0);

        assertFalse(result, "Score of 0 is not valid");
    }

    @Test
    void negativeScore_shouldReturnFalse() {
        // RED: Negative scores are not valid
        boolean result = DartsValidator.isValidDartsScore(-10);

        assertFalse(result, "Negative scores are not valid");
    }

    @Test
    void scoreAbove180_shouldReturnFalse() {
        // RED: Maximum possible score is 180 (3 x T20)
        boolean result = DartsValidator.isValidDartsScore(181);

        assertFalse(result, "Scores above 180 are not achievable");
    }

    @Test
    void maximumValidScore_shouldReturnTrue() {
        // RED: 180 is the maximum valid score (3 x T20)
        boolean result = DartsValidator.isValidDartsScore(180);

        assertTrue(result, "180 is the maximum valid darts score");
    }

    @Test
    void minimumValidScore_shouldReturnTrue() {
        // RED: 1 is the minimum valid score (single 1)
        boolean result = DartsValidator.isValidDartsScore(1);

        assertTrue(result, "1 is the minimum valid darts score");
    }
}
