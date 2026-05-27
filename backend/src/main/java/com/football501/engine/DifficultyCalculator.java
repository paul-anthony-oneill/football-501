package com.football501.engine;

import static com.football501.engine.DifficultyConstants.*;

/**
 * Pure-function utility for computing the continuous difficulty score (0.00–10.00)
 * of a question from its pre-counted zone answer counts.
 *
 * <h3>Formula summary</h3>
 * <pre>
 * ease_score =
 *     saturate(highValueCount, HIGH_VALUE_SATURATION) × WEIGHT_HIGH_VALUE
 *   + saturate(midRangeCount,  MID_RANGE_SATURATION)  × WEIGHT_MID_RANGE
 *   + saturate(checkoutCount,  CHECKOUT_SATURATION)   × WEIGHT_CHECKOUT
 *
 * base_difficulty = 10.0 × (1 − ease_score)
 *
 * if (checkoutCount == 0):
 *     base_difficulty = max(base_difficulty, CHECKOUT_FLOOR)
 *
 * depth_bonus = saturate(totalValidCount, TOTAL_VALID_SATURATION) × DEPTH_BONUS_MAX
 *
 * difficulty_score = clamp(base_difficulty − depth_bonus, 0.0, 10.0)
 * </pre>
 *
 * <p>All tunable constants live in {@link DifficultyConstants}. Changing a constant
 * and calling the recalibration endpoint is the complete cost of a formula adjustment —
 * no re-materialisation is required.
 *
 * <p>This class is intentionally free of Spring annotations so it can be called
 * from static contexts and tested with plain JUnit (no application context needed).
 */
public final class DifficultyCalculator {

    private DifficultyCalculator() {
        throw new UnsupportedOperationException("Utility class — do not instantiate.");
    }

    /**
     * Computes the difficulty score for a question given its pre-counted zone answer counts.
     *
     * @param highValueCount  count of valid-darts answers scoring {@value DifficultyConstants#HIGH_VALUE_SCORE_MIN}–{@value DifficultyConstants#HIGH_VALUE_SCORE_MAX}
     * @param midRangeCount   count of valid-darts answers scoring {@value DifficultyConstants#MID_RANGE_SCORE_MIN}–{@value DifficultyConstants#MID_RANGE_SCORE_MAX}
     * @param checkoutCount   count of valid-darts answers scoring {@value DifficultyConstants#CHECKOUT_SCORE_MIN}–{@value DifficultyConstants#CHECKOUT_SCORE_MAX}
     * @param totalValidCount total count of valid-darts, non-bust answers across all zones
     * @return difficulty score in the range [0.0, 10.0]; 0.0 = easiest, 10.0 = hardest
     */
    public static double calculate(int highValueCount,
                                   int midRangeCount,
                                   int checkoutCount,
                                   int totalValidCount) {
        double ease =
              saturate(highValueCount, HIGH_VALUE_SATURATION) * WEIGHT_HIGH_VALUE
            + saturate(midRangeCount,  MID_RANGE_SATURATION)  * WEIGHT_MID_RANGE
            + saturate(checkoutCount,  CHECKOUT_SATURATION)   * WEIGHT_CHECKOUT;

        double base = 10.0 * (1.0 - ease);

        // Checkout floor: absence of checkout answers is structural, not just a 20% penalty.
        if (checkoutCount == 0) {
            base = Math.max(base, CHECKOUT_FLOOR);
        }

        double depthBonus = saturate(totalValidCount, TOTAL_VALID_SATURATION) * DEPTH_BONUS_MAX;

        return Math.max(0.0, Math.min(10.0, base - depthBonus));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Maps {@code count} to the range [0.0, 1.0], saturating at {@code threshold}.
     *
     * <p>{@code saturate(0, t) = 0.0}, {@code saturate(t, t) = 1.0},
     * {@code saturate(t × 2, t) = 1.0}.
     */
    private static double saturate(int count, double threshold) {
        return Math.min(count / threshold, 1.0);
    }
}
