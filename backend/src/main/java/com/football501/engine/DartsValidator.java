package com.football501.engine;

import java.util.Set;

/**
 * Validates if a score is achievable with 3 darts in standard 501 darts.
 *
 * Invalid scores (cannot be achieved with any combination of 3 darts):
 * 163, 166, 169, 172, 173, 175, 176, 178, 179
 *
 * All other scores from 1-180 are valid.
 */
public class DartsValidator {

    /**
     * Scores that cannot be achieved with 3 darts in standard 501.
     */
    private static final Set<Integer> INVALID_SCORES = Set.of(
        163, 166, 169, 172, 173, 175, 176, 178, 179
    );

    /**
     * Minimum valid darts score (single 1).
     */
    private static final int MIN_SCORE = 1;

    /**
     * Maximum valid darts score (3 x T20).
     */
    private static final int MAX_SCORE = 180;

    /**
     * Checks if a score is achievable with 3 darts in standard 501.
     *
     * @param score the score to validate
     * @return true if the score is valid, false otherwise
     */
    public static boolean isValidDartsScore(int score) {
        // Score must be in range 1-180
        if (score < MIN_SCORE || score > MAX_SCORE) {
            return false;
        }

        // Score must not be one of the impossible scores
        return !INVALID_SCORES.contains(score);
    }

    private DartsValidator() {
        // Utility class - prevent instantiation
    }
}
