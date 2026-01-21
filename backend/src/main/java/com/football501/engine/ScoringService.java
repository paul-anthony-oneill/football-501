package com.football501.engine;

import org.springframework.stereotype.Service;

/**
 * Service for calculating scores in Football 501.
 *
 * Game Rules:
 * - Players start at 501
 * - Valid answer scores are deducted from current score
 * - Invalid darts scores result in bust (no deduction)
 * - Scores > 180 or <= 0 result in bust
 * - Scores resulting in < -10 result in bust
 * - Checkout range: -10 to 0 (inclusive)
 * - Once in checkout range, player cannot score again
 */
@Service
public class ScoringService {

    /**
     * Minimum checkout score (inclusive).
     */
    private static final int CHECKOUT_MIN = -10;

    /**
     * Maximum checkout score (inclusive).
     */
    private static final int CHECKOUT_MAX = 0;

    /**
     * Calculates the result of submitting an answer score.
     *
     * @param currentScore the player's current score
     * @param answerScore the score from the answer (e.g., player's appearances/goals)
     * @return the score result (new score, bust status, checkout status)
     */
    public ScoreResult calculateScore(int currentScore, int answerScore) {
        // If already in checkout range, cannot score again
        if (currentScore < CHECKOUT_MAX) {
            return ScoreResult.bust(currentScore);
        }

        // Validate answer score is a valid darts score
        if (!DartsValidator.isValidDartsScore(answerScore)) {
            return ScoreResult.bust(currentScore);
        }

        // Calculate potential new score
        int newScore = currentScore - answerScore;

        // If new score would be below checkout minimum, it's a bust
        if (newScore < CHECKOUT_MIN) {
            return ScoreResult.bust(currentScore);
        }

        // If new score is in checkout range (-10 to 0), it's a checkout
        if (newScore >= CHECKOUT_MIN && newScore <= CHECKOUT_MAX) {
            return ScoreResult.checkout(newScore);
        }

        // Valid score deduction
        return ScoreResult.validScore(newScore);
    }
}
