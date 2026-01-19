package com.football501.engine;

/**
 * Result of a scoring calculation.
 */
public class ScoreResult {

    private final int newScore;
    private final boolean isBust;
    private final boolean isCheckout;

    private ScoreResult(int newScore, boolean isBust, boolean isCheckout) {
        this.newScore = newScore;
        this.isBust = isBust;
        this.isCheckout = isCheckout;
    }

    /**
     * Creates a bust result (score unchanged).
     */
    public static ScoreResult bust(int currentScore) {
        return new ScoreResult(currentScore, true, false);
    }

    /**
     * Creates a checkout result (player wins).
     */
    public static ScoreResult checkout(int newScore) {
        return new ScoreResult(newScore, false, true);
    }

    /**
     * Creates a valid score deduction result.
     */
    public static ScoreResult validScore(int newScore) {
        return new ScoreResult(newScore, false, false);
    }

    /**
     * The new score after the calculation.
     * If bust, this equals the original score (unchanged).
     */
    public int getNewScore() {
        return newScore;
    }

    /**
     * Whether the scoring attempt resulted in a bust.
     * Bust reasons:
     * - Invalid darts score (163, 166, 169, 172, 173, 175, 176, 178, 179)
     * - Score > 180 or <= 0
     * - Result would be < -10 (outside checkout range)
     * - Already in checkout range (attempting to score again)
     */
    public boolean isBust() {
        return isBust;
    }

    /**
     * Whether the score is in the checkout range (-10 to 0, inclusive).
     * A checkout means the player has won.
     */
    public boolean isCheckout() {
        return isCheckout;
    }
}
