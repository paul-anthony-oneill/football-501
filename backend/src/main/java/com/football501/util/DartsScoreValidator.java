package com.football501.util;

import java.util.Set;

public class DartsScoreValidator {
    
    private static final Set<Integer> INVALID_DARTS_SCORES = Set.of(
        163, 166, 169, 172, 173, 175, 176, 178, 179
    );

    public static boolean isValid(int score) {
        if (score < 0 || score > 180) {
            return false;
        }
        return !INVALID_DARTS_SCORES.contains(score);
    }
}
