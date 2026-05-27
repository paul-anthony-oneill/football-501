package com.football501.engine;

/**
 * Tunable parameters for the question difficulty formula and viability checks.
 *
 * <h3>Why a constants class?</h3>
 * <p>All formula inputs live here so that a difficulty re-calibration is a two-step
 * operation: edit a constant, then call
 * {@code POST /api/admin/questions/recalculate-difficulty}. No SQL backfill is
 * required unless zone <em>boundaries</em> change (see note below).
 *
 * <h3>Zone boundaries vs. formula weights — cost of change</h3>
 * <ul>
 *   <li><strong>Formula weights / saturation thresholds</strong> — cheap to change:
 *       adjust the constant, run the recalibration endpoint. The stored counts are
 *       reused; only the score is re-derived.</li>
 *   <li><strong>Zone boundaries</strong> (e.g. {@link #CHECKOUT_SCORE_MAX} 19 → 25) —
 *       expensive: changing a boundary invalidates the stored {@code *_count} columns.
 *       You must re-count from the answers table (backfill SQL or full re-materialisation)
 *       before running recalibration.</li>
 * </ul>
 *
 * <p>This class must not be instantiated; use its constants directly.
 *
 * @see DifficultyCalculator
 * @see <a href="docs/design/DIFFICULTY_SCORING.md">DIFFICULTY_SCORING.md</a>
 */
public final class DifficultyConstants {

    private DifficultyConstants() {
        throw new UnsupportedOperationException("Constants class — do not instantiate.");
    }

    // ── Score zone boundaries ─────────────────────────────────────────────────
    // Changing these requires a re-count from the answers table (expensive).

    /** Lower bound of the checkout zone (inclusive). */
    public static final int CHECKOUT_SCORE_MIN   = 1;

    /** Upper bound of the checkout zone (inclusive). */
    public static final int CHECKOUT_SCORE_MAX   = 19;

    /** Lower bound of the mid-range / navigation zone (inclusive). */
    public static final int MID_RANGE_SCORE_MIN  = 20;

    /** Upper bound of the mid-range / navigation zone (inclusive). */
    public static final int MID_RANGE_SCORE_MAX  = 99;

    /** Lower bound of the high-value / velocity zone (inclusive). */
    public static final int HIGH_VALUE_SCORE_MIN = 100;

    /** Upper bound of the high-value / velocity zone (inclusive). Equal to the max valid darts score. */
    public static final int HIGH_VALUE_SCORE_MAX = 180;

    // ── Saturation thresholds ─────────────────────────────────────────────────
    // Changing these is cheap: run the recalibration endpoint.

    /**
     * Number of high-value answers at which the high-value component is fully
     * saturated (contributes its maximum weight of {@link #WEIGHT_HIGH_VALUE}).
     */
    public static final double HIGH_VALUE_SATURATION  = 25.0;

    /**
     * Number of mid-range answers at which the mid-range component is fully
     * saturated. Wider range (20–99) requires more answers to reach saturation.
     */
    public static final double MID_RANGE_SATURATION   = 40.0;

    /**
     * Number of checkout answers at which the checkout component is fully
     * saturated. Lower threshold — a handful is enough for viable checkout play.
     */
    public static final double CHECKOUT_SATURATION    = 12.0;

    /**
     * Total-answer count at which the depth bonus reaches its maximum value
     * of {@link #DEPTH_BONUS_MAX}. Questions with 200+ answers get the full bonus.
     */
    public static final double TOTAL_VALID_SATURATION = 200.0;

    // ── Formula weights ───────────────────────────────────────────────────────
    // Must sum to 1.0.  Changing these is cheap.

    /** Weight of the high-value (velocity) component in the ease score. */
    public static final double WEIGHT_HIGH_VALUE = 0.50;

    /** Weight of the mid-range (navigation) component in the ease score. */
    public static final double WEIGHT_MID_RANGE  = 0.30;

    /** Weight of the checkout (precision) component in the ease score. */
    public static final double WEIGHT_CHECKOUT   = 0.20;

    // ── Depth bonus ───────────────────────────────────────────────────────────

    /**
     * Maximum reduction applied to {@code base_difficulty} for questions with
     * large answer pools. Ranges from 0 (pool = 0) to this value (pool ≥ 200).
     */
    public static final double DEPTH_BONUS_MAX = 1.5;

    // ── Checkout floor ────────────────────────────────────────────────────────

    /**
     * Minimum difficulty score applied when {@code checkout_count == 0}.
     * Absence of checkout answers is a structural problem, not just a 20% ease penalty:
     * players have no precision tools and games stall near the end.
     */
    public static final double CHECKOUT_FLOOR = 7.0;

    // ── Viability thresholds ──────────────────────────────────────────────────
    // These gate whether a question enters standard play. NOT formula inputs.
    // Changing these is cheap: run the recalibration endpoint (it re-evaluates
    // viability from stored counts without touching the answers table).

    /**
     * Minimum total score pool required for a question to be viable in standard
     * single-question mode. A question with a pool below 501 cannot be finished
     * from a starting score of 501.
     */
    public static final int MIN_SCORE_POOL   = 501;

    /**
     * Minimum number of valid-darts, non-bust answers required for a question
     * to be viable. Questions below this threshold exhaust the answer pool within
     * a few turns; the game stalls and becomes a memory test.
     */
    public static final int MIN_ANSWER_COUNT = 15;
}
