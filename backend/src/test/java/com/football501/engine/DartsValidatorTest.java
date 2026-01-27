package com.football501.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Darts Validator Tests")
class DartsValidatorTest {

    @ParameterizedTest
    @DisplayName("Invalid darts scores return false")
    @ValueSource(ints = {
        163, 166, 169, 172, 173, 175, 176, 178, 179, // Not achievable with 3 darts
        181, 200,                                    // > 180
        0, -1, -10                                   // <= 0
    })
    void shouldReturnFalseForInvalidScores(int score) {
        assertFalse(DartsValidator.isValidDartsScore(score),
            "Score " + score + " should be invalid");
    }

    @ParameterizedTest
    @DisplayName("Valid darts scores return true")
    @ValueSource(ints = {
        1, 20, 60, 100, 120, 140, 160, 164, 165, 167, 168, 170, 171, 174, 177, 180
    })
    void shouldReturnTrueForValidScores(int score) {
        assertTrue(DartsValidator.isValidDartsScore(score),
            "Score " + score + " should be valid");
    }
}
