package com.football501.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link DifficultyCalculator}.
 *
 * <p>All test cases are derived from the worked examples and boundary conditions
 * described in {@code docs/design/DIFFICULTY_SCORING.md}.
 * No Spring context is required — the calculator is a pure static utility.
 */
@DisplayName("DifficultyCalculator")
class DifficultyCalculatorTest {

    // ── Boundary / invariant tests ────────────────────────────────────────────

    @Test
    @DisplayName("Score is never below 0.0")
    void scoreNeverBelowZero() {
        // Saturated on all zones, large pool — maximum ease
        double score = DifficultyCalculator.calculate(30, 50, 15, 500);
        assertThat(score).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Score is never above 10.0")
    void scoreNeverAboveTen() {
        // Zero answers on every zone
        double score = DifficultyCalculator.calculate(0, 0, 0, 0);
        assertThat(score).isLessThanOrEqualTo(10.0);
    }

    @Test
    @DisplayName("No answers → maximum difficulty (10.0)")
    void noAnswersMeansMaxDifficulty() {
        double score = DifficultyCalculator.calculate(0, 0, 0, 0);
        assertThat(score).isEqualTo(10.0, within(0.01));
    }

    @Test
    @DisplayName("All zones fully saturated → near-zero difficulty")
    void allZonesSaturated() {
        // hv ≥ 25, mid ≥ 40, co ≥ 12, total ≥ 200
        double score = DifficultyCalculator.calculate(30, 50, 15, 250);
        assertThat(score).isEqualTo(0.0, within(0.01));
    }

    // ── Checkout floor ────────────────────────────────────────────────────────

    @Test
    @DisplayName("No checkout answers, no depth → base floored to CHECKOUT_FLOOR (7.0)")
    void noCheckoutAnswersTriggersFloor() {
        // Good high-value and mid-range coverage, no checkout answers, zero pool (no depth bonus)
        // base = 10*(1 - 0.80) = 2.0; floor → 7.0; depth=0; score=7.0
        double score = DifficultyCalculator.calculate(30, 50, 0, 0);
        assertThat(score).isEqualTo(DifficultyConstants.CHECKOUT_FLOOR, within(0.001));
    }

    @Test
    @DisplayName("Checkout floor applies to base, not final score — depth bonus can reduce below 7.0")
    void checkoutFloor_appliesToBase_depthCanReduceBelow() {
        // hv=30, mid=50, co=0, total=250 → base floored to 7.0; depth=1.5; final score=5.5
        double score = DifficultyCalculator.calculate(30, 50, 0, 250);
        assertThat(score).isEqualTo(5.5, within(0.01));
        // The depth bonus reduced the final score below 7.0 — floor applied to base, not output
        assertThat(score).isLessThan(DifficultyConstants.CHECKOUT_FLOOR);
    }

    @Test
    @DisplayName("No checkout answers, small pool → score near CHECKOUT_FLOOR")
    void noCheckoutSmallPoolFloor() {
        double score = DifficultyCalculator.calculate(5, 8, 0, 13);
        assertThat(score).isGreaterThanOrEqualTo(DifficultyConstants.CHECKOUT_FLOOR);
    }

    @Test
    @DisplayName("Tiny mid-range only, no checkout → floor active")
    void tinyMidRangeNoCheckout() {
        double score = DifficultyCalculator.calculate(0, 8, 0, 8);
        assertThat(score).isGreaterThanOrEqualTo(DifficultyConstants.CHECKOUT_FLOOR);
    }

    // ── Depth bonus saturation ────────────────────────────────────────────────

    @Test
    @DisplayName("Depth bonus caps: total=500 gives same score as total=200")
    void depthBonusSaturates() {
        double scoreAt200 = DifficultyCalculator.calculate(25, 40, 12, 200);
        double scoreAt500 = DifficultyCalculator.calculate(25, 40, 12, 500);
        assertThat(scoreAt500).isEqualTo(scoreAt200, within(0.001));
    }

    // ── Worked examples from design doc ──────────────────────────────────────

    @Test
    @DisplayName("Question A (30×180, 1000×1) → ~1.5")
    void workedExampleA() {
        // hv=30, mid=0, checkout=1000 (all score 1 = checkout zone), total=1030
        double score = DifficultyCalculator.calculate(30, 0, 1000, 1030);
        assertThat(score).isEqualTo(1.5, within(0.01));
    }

    @Test
    @DisplayName("Question B (500 answers spread 1–180) → 0.0")
    void workedExampleB() {
        // All zones fully covered, large pool
        // Approximate: hv≈225, mid≈200, co≈50, total=500
        double score = DifficultyCalculator.calculate(225, 200, 50, 500);
        assertThat(score).isEqualTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("Question C (hv=30, mid=60, co=0, total=90) → ~6.3 (checkout floor applies)")
    void workedExampleC() {
        // base = 10*(1 - (1.0*0.50 + 1.0*0.30 + 0)) = 10*0.20 = 2.0
        // floor: checkout=0 → base = max(2.0, 7.0) = 7.0
        // depth = saturate(90, 200)*1.5 = 0.45*1.5 = 0.675
        // score = 7.0 - 0.675 = 6.325
        double score = DifficultyCalculator.calculate(30, 60, 0, 90);
        assertThat(score).isEqualTo(6.325, within(0.01));
    }

    // ── Zone weight & saturation parametric tests ─────────────────────────────

    @ParameterizedTest
    @DisplayName("Zone weights and saturation produce expected scores")
    @CsvSource({
        // hv, mid, co, total,   expectedScore  — comment
        // Only high-value saturated, no checkout (floor kicks in), no depth
        // ease=0.5; base=5.0; co=0 → floor → base=7.0; depth=0 → 7.0
        "25,  0,  0,    0,   7.0",
        // Only mid-range saturated, no checkout (floor kicks in), no depth
        // ease=0.3; base=7.0; co=0 → floor → base=max(7.0,7.0)=7.0; depth=0 → 7.0
        " 0, 40,  0,    0,   7.0",
        // Only checkout saturated, no high/mid, no depth
        // ease=0.2; base=8.0; co>0 (no floor); depth=0 → 8.0
        " 0,  0, 12,    0,   8.0",
        // High-value + mid-range saturated, no checkout (floor), no depth
        // ease=0.8; base=2.0; co=0 → floor → base=7.0; depth=0 → 7.0
        "25, 40,  0,    0,   7.0",
        // All zones saturated, depth at quarter saturation
        // ease=1.0; base=0; depth=saturate(50,200)*1.5=0.375; max(0, 0-0.375)=0.0 (clamped)
        "25, 40, 12,   50,   0.0",
    })
    void zoneWeightParametric(int hv, int mid, int co, int total, double expected) {
        double score = DifficultyCalculator.calculate(hv, mid, co, total);
        assertThat(score).isEqualTo(expected, within(0.005));
    }

    // ── Score is deterministic ────────────────────────────────────────────────

    @Test
    @DisplayName("Same inputs always produce the same score (pure function)")
    void isPureFunction() {
        double first  = DifficultyCalculator.calculate(10, 20, 5, 80);
        double second = DifficultyCalculator.calculate(10, 20, 5, 80);
        assertThat(first).isEqualTo(second);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Single high-value answer → above floor when no checkout")
    void singleHighValueNoCheckout() {
        // One answer in the velocity zone, nothing else
        double score = DifficultyCalculator.calculate(1, 0, 0, 1);
        assertThat(score).isGreaterThanOrEqualTo(DifficultyConstants.CHECKOUT_FLOOR);
    }

    @Test
    @DisplayName("Score with very large answer counts stays clamped to [0.0, 10.0]")
    void largeCountsStayClamped() {
        double score = DifficultyCalculator.calculate(10_000, 10_000, 10_000, 100_000);
        assertThat(score).isBetween(0.0, 10.0);
    }
}
