package com.trivia501.engine;

/**
 * Result of a scoring calculation.
 */
public class ScoreResult {

    private final int newScore;
    private final boolean isBust;
    private final boolean isCheckout;
    private final String reason;

    private ScoreResult(int newScore, boolean isBust, boolean isCheckout, String reason) {
        this.newScore = newScore;
        this.isBust = isBust;
        this.isCheckout = isCheckout;
        this.reason = reason;
    }

    /**
     * Creates a bust result (score unchanged).
     */
    public static ScoreResult bust(int currentScore, String reason) {
        return new ScoreResult(currentScore, true, false, reason);
    }

    /**
     * Creates a checkout result (player wins).
     */
    public static ScoreResult checkout(int newScore) {
        return new ScoreResult(newScore, false, true, "Win!");
    }

    /**
     * Creates a valid score deduction result.
     */
    public static ScoreResult validScore(int newScore) {
        return new ScoreResult(newScore, false, false, null);
    }

    public int getNewScore() { return newScore; }
    public boolean isBust() { return isBust; }
    public boolean isCheckout() { return isCheckout; }
    public String getReason() { return reason; }
}
