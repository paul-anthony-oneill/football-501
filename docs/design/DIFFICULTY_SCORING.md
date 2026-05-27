# Question Difficulty Scoring System

**Status**: Designed, pending implementation  
**Branch**: `feature/ingame-hints`  
**Last updated**: 2026-05-26

---

## Problem Statement

Questions need a difficulty signal so the game engine can select appropriate questions for different contexts (ranked matchmaking, daily challenges, etc.). A naïve enum — `EASY / MEDIUM / HARD / EXPERT` — is inadequate because it is a **classification, not a measurement**. The variance within a single bucket can be enormous: the hardest "Easy" question and the easiest "Hard" question may play identically.

The replacement is a **continuous difficulty score** from 0.00 (easiest) to 10.00 (hardest), computed from the question's answer pool at materialisation time and stored in the database. Labels like "Easy" or "Hard" are derived from score ranges at render time and never stored.

> **Difficulty and viability are separate concerns.** Difficulty measures how hard a viable question is to play. Viability measures whether a question is playable at all. A question can be correctly scored by the formula and still be unplayable — because it has too few distinct answers, or because its stat type structurally cannot produce values in the ranges the game needs. These two checks must be treated as independent conditions and must never be conflated.

---

## Key Design Insights

### 1. Knowability correlates with score value

This is the most important insight shaping the formula. A player who scored 175 goals is a household name. A player who scored 3 goals is an obscure footnote. The *effective* answer pool a player can draw from is always skewed toward high-value answers — regardless of how many low-value answers exist in the database.

This means **measuring the statistical distribution of all answers is misleading**. A question with a perfect 1–180 spread is not meaningfully easier than one clustered at the high end, because the low-to-mid answers are the ones players are least likely to know.

### 2. The three game phases

A game of 501 has three distinct phases, each requiring different answers:

| Phase | Remaining score | Answer range needed | What it rewards |
|---|---|---|---|
| **Velocity** | ~501–100 | 100–180 | Getting the score down fast |
| **Navigation** | ~100–30 | 20–99 | Moving toward a checkable position |
| **Checkout** | ~30–0 | 1–19 | Landing precisely in the −10 to 0 window |

### 3. The checkout window is always exactly 10 points wide

To win, a player's remaining score minus their answer must land between −10 and 0 inclusive. If remaining = 15, the valid checkout range is answers scoring 15–25. This window never widens — players must approach it carefully using low-value answers. A question with no answers scoring 1–19 leaves players with no precision tools; they will repeatedly overshoot and the game stalls.

### 4. The score is computed from stored counts, not re-derived from answers

The raw zone counts (`high_value_count`, `mid_range_count`, `checkout_count`, `total_valid_count`) are stored alongside the computed `difficulty_score`. This means **changing the formula costs a single SQL UPDATE** — no re-materialisation required. Tuning constants is cheap; changing zone boundaries is not (they require recounting from the answers table).

### 5. The depth bonus rewards good questions but does not penalise scarce ones

The formula's depth bonus (`saturate(total_valid_count, 200) × 1.5`) ranges from 0 to −1.5 as answer count grows from 0 to 200. This correctly makes large pools easier, but the penalty for tiny pools is almost zero — a question with 3 answers gets nearly the same depth signal as one with 8. **Answer count must be enforced as a hard minimum, not just a soft signal in the formula.**

---

## Score Zones

| Zone | Score range | Role |
|---|---|---|
| **Checkout** | 1–19 | Precision approach and final landing |
| **Mid-range** | 20–99 | Mid-game navigation toward checkout |
| **High-value** | 100–180 | Early-game velocity |

Scores > 180 are already flagged `is_bust = true` in the answers table and are excluded from all counts. Scores 81–99 are included in mid-range (extending the user-defined 20–80 boundary to 99 closes the otherwise unaddressed gap).

---

## Formula

```
ease_score =
    saturate(high_value_count, 25)  × 0.50   ← velocity zone
  + saturate(mid_range_count,  40)  × 0.30   ← navigation zone
  + saturate(checkout_count,   12)  × 0.20   ← checkout zone

base_difficulty = 10.0 × (1 − ease_score)

if checkout_count == 0:
    base_difficulty = max(base_difficulty, 7.0)   ← checkout floor

depth_bonus = saturate(total_valid_count, 200) × 1.5

difficulty_score = clamp(base_difficulty − depth_bonus, 0.0, 10.0)
```

Where `saturate(count, threshold) = min(count / threshold, 1.0)`.

### Component rationale

| Component | Weight | Saturation | Rationale |
|---|---|---|---|
| High-value | 50% | 25 answers | Primary knowable pool; drives game velocity |
| Mid-range | 30% | 40 answers | Wider range, needs more answers; less knowable |
| Checkout | 20% | 12 answers | Smaller threshold; a handful is enough |
| Depth bonus | −1.5 max | 200 answers | Large pools shift game from knowledge to strategy |
| Checkout floor | 7.0 min | n/a | Absence of checkout answers is severe, not just a 20% penalty |

### Worked examples

**Question A: 30 × score 180, 1000 × score 1**
```
high_value = 30  → saturate(30, 25) = 1.0
mid_range  = 0   → saturate(0,  40) = 0.0
checkout   = 1000 → saturate(1000, 12) = 1.0   (all score 1, which is in 1–19)
total_valid = 1030

ease  = 1.0×0.50 + 0.0×0.30 + 1.0×0.20 = 0.70
base  = 10.0 × 0.30 = 3.0
depth = saturate(1030, 200) × 1.5 = 1.5
score = max(0, 3.0 − 1.5) = 1.5
```
Viable but strategically barren — no mid-game. Correctly scores as easy-moderate.

**Question B: 500 answers evenly spread 1–180**
```
high_value ≈ 225  → 1.0
mid_range  ≈ 200  → 1.0
checkout   ≈  50  → 1.0
total_valid = 500

ease  = 1.0
base  = 0.0
depth = 1.5
score = max(0, 0.0 − 1.5) = 0.0
```
Near-perfect question. Scores 0.0.

**Question C: Small question, 0 checkout answers**
```
high_value = 30, mid_range = 60, checkout = 0, total_valid = 90

ease  = 1.0×0.50 + 1.0×0.30 + 0.0 = 0.80
base  = 10 × 0.20 = 2.0
floor: checkout_count == 0 → base = max(2.0, 7.0) = 7.0
depth = saturate(90, 200) × 1.5 = 0.675
score = 7.0 − 0.675 = 6.325
```
Good coverage but no checkout answers — correctly penalised.

---

## Formula Constants

All tunable parameters live in a single class. Changing any value and running the recalibration endpoint is the complete cost of a formula adjustment.

```java
// backend/src/main/java/com/football501/engine/DifficultyConstants.java

public static final int    CHECKOUT_SCORE_MIN       = 1;
public static final int    CHECKOUT_SCORE_MAX        = 19;
public static final int    MID_RANGE_SCORE_MIN       = 20;
public static final int    MID_RANGE_SCORE_MAX       = 99;
public static final int    HIGH_VALUE_SCORE_MIN      = 100;
public static final int    HIGH_VALUE_SCORE_MAX      = 180;

public static final double HIGH_VALUE_SATURATION     = 25.0;
public static final double MID_RANGE_SATURATION      = 40.0;
public static final double CHECKOUT_SATURATION       = 12.0;
public static final double TOTAL_VALID_SATURATION    = 200.0;

public static final double WEIGHT_HIGH_VALUE         = 0.50;
public static final double WEIGHT_MID_RANGE          = 0.30;
public static final double WEIGHT_CHECKOUT           = 0.20;

public static final double DEPTH_BONUS_MAX           = 1.5;
public static final double CHECKOUT_FLOOR            = 7.0;

// Viability thresholds — enforced as hard minimums at materialisation time.
// These are NOT formula inputs; they gate whether the question enters standard play.
public static final int    MIN_SCORE_POOL            = 501;   // total_score_pool condition
public static final int    MIN_ANSWER_COUNT          = 15;    // total_valid_count condition
```

---

## Question Viability

Viability is a **pre-condition for gameplay**, not a difficulty measurement. A question is viable for standard single-question mode if and only if it satisfies both conditions:

```
total_score_pool  >= MIN_SCORE_POOL  (501)
total_valid_count >= MIN_ANSWER_COUNT (15)
```

Both conditions must pass. Either failure independently makes the question unplayable:

| Failure | Effect on gameplay |
|---|---|
| `total_score_pool < 501` | A player cannot theoretically reach zero — the game cannot be won |
| `total_valid_count < 15` | Both players exhaust the answer pool within a few turns; game stalls and becomes a memory test |

### Why the formula alone is insufficient

Consider a question with 5 players at 101, 102, 103, 101, 104 appearances respectively:
- `total_score_pool` = 511 (passes the 501 check)
- `total_valid_count` = 5 (fails the 15-answer minimum)
- `difficulty_score` ≈ 8.96 (the formula sees a hard question, not a broken one)
- `single_question_viable` **must be false** — both players run out of answers by turn 3

The depth bonus in the formula rewards depth but barely penalises scarcity. A question with 3 answers has nearly identical depth signal to one with 8. **Only a hard minimum enforced outside the formula prevents these questions from entering the game.**

### Question status lifecycle

```
draft
  │
  ▼  Admin promotes via PATCH /api/admin/questions/{id}/status
active  ◄─── materialisation runs, both conditions pass
  │
  ▼  materialisation finds viability failure (auto)
excluded   ← status set automatically; viability_exclusion_reason populated
  │
  ▼  Admin can also set manually
archived   ← intentionally deprecated questions (separate from auto-exclusion)
```

**Auto-exclusion happens at materialisation time, not in a separate cleanup job.** When an admin promotes `draft → active`, `QuestionMaterializerService` runs, computes zone counts, evaluates both viability conditions, and sets `status = 'excluded'` if either fails. The admin does not need to run a cleanup sweep.

### `viability_exclusion_reason` field

A `TEXT` column on `questions` that is populated whenever `single_question_viable = false`. It explains which condition(s) failed and by how much. Example values:

```
insufficient_score_pool: 234 < 501
insufficient_answer_count: 8 < 15
insufficient_score_pool: 234 < 501; insufficient_answer_count: 8 < 15
```

This is essential for admin observability. Without it, excluded questions are opaque — the admin cannot distinguish "borderline" from "completely broken" or understand which templates are structurally problematic.

---

## Structurally Incompatible Templates

Some question templates are incompatible with the darts scoring mechanic at specific materialiser scopes. The problem is not marginal data — it is structural. No combination of team or season parameters can fix it.

### `team_competition_sub_appearances_since` — disable immediately

**Why it is broken**: Substitute appearances for a single club over any time window are almost always 1–30 per player. No player accumulates 100+ substitute appearances for one club in a real dataset. This means the template **structurally cannot produce high-value answers (100–180)**. Every answer lands in the checkout (1–19) or low mid-range zone.

Consequence: the game has no velocity phase. Players chip away 3, 7, 12 points per turn. With starting score of 501 and answers averaging ~10, a game takes ~50 turns. The game stalls and is not fun to play.

**Action**: Set `is_active = false` on this template row in the database. Do not generate draft questions from it. The league-scope version (`player_competition_sub_appearances_since`) is fine — it draws from 500+ players across a whole league, including players with 30–60 career sub appearances in that competition, giving adequate mid-range coverage.

### Identifying other broken templates

When a template produces a large proportion of auto-excluded questions, treat it as a structural signal rather than a data gap. Run this diagnostic after backfilling:

```sql
SELECT
    qt.slug,
    COUNT(*)                                               AS total_questions,
    COUNT(*) FILTER (WHERE q.status = 'excluded')         AS excluded_count,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE q.status = 'excluded') / COUNT(*),
        1
    )                                                      AS exclusion_rate_pct,
    AVG(q.total_valid_count)                               AS avg_answer_count,
    AVG(q.total_score_pool)                                AS avg_score_pool
FROM questions q
JOIN question_templates qt ON q.template_id = qt.id
GROUP BY qt.slug
ORDER BY exclusion_rate_pct DESC;
```

Any template with `exclusion_rate_pct > 50` or `avg_answer_count < 20` should be reviewed and likely disabled.

---

## Difficulty Cap for Standard Play

`single_question_viable` gates whether a question is *playable at all*. A difficulty cap gates whether a question is *appropriate for standard ranked matchmaking*.

For standard ranked play, game selection queries should cap at **`difficulty_score <= 8.5`**. Questions scoring above this are playable in principle but produce such a constrained answer pool that games become frustrating rather than strategic. They can be reserved for future specialist modes (Expert Challenge, Handicap Mode).

```sql
-- Standard ranked play selection
SELECT q FROM Question q
WHERE q.categoryId           = :categoryId
  AND q.status               = 'active'
  AND q.singleQuestionViable = true
  AND q.difficultyScore      BETWEEN :minScore AND 8.5
ORDER BY FUNCTION('random')
```

The `8.5` ceiling is a starting point. Adjust after observing real question distributions — the goal is to exclude the tail where games stall, not to exclude legitimately hard questions that still play well.

---

## Database Schema

### New columns on `questions` (V13 migration)

```sql
high_value_count           INT          NOT NULL DEFAULT 0
mid_range_count            INT          NOT NULL DEFAULT 0
checkout_count             INT          NOT NULL DEFAULT 0
total_valid_count          INT          NOT NULL DEFAULT 0
total_score_pool           INT          NOT NULL DEFAULT 0
single_question_viable     BOOLEAN      NOT NULL DEFAULT TRUE
viability_exclusion_reason TEXT         NULL
difficulty_score           NUMERIC(4,2) NOT NULL DEFAULT 5.00
difficulty_locked          BOOLEAN      NOT NULL DEFAULT FALSE
```

**`single_question_viable`**: `true` when `total_score_pool >= 501` AND `total_valid_count >= 15` (constants from `DifficultyConstants.MIN_SCORE_POOL` / `MIN_ANSWER_COUNT`). Both conditions must pass. Questions that fail either check are automatically set to `status = 'excluded'` at materialisation time and are excluded from all game selection queries.

**`viability_exclusion_reason`**: `NULL` on viable questions. Populated with a human-readable string explaining which condition(s) failed whenever `single_question_viable = false`. Exposed in the admin question list view so the cause of exclusion is always visible.

**`difficulty_locked`**: Admin override flag. When `true`, the recalibration job skips this question. Allows manual pinning of scores that the formula computes incorrectly.

### Indexes

```sql
CREATE INDEX idx_questions_difficulty_score ON questions (difficulty_score) WHERE status = 'active';
CREATE INDEX idx_questions_viable           ON questions (single_question_viable) WHERE status = 'active';
CREATE INDEX idx_questions_excluded         ON questions (status) WHERE status = 'excluded';
```

### Deprecated: `difficulty INTEGER`

The `difficulty INTEGER` column (1/2/3 scale, added in V4) is superseded by `difficulty_score`. It is retained for the transition period and will be removed once all callers have been migrated to the new field.

---

## Implementation Plan

### Execution order

```
1.  V13 migration                          ← schema first, no code changes yet
2.  DifficultyConstants.java               ← no dependencies
3.  DifficultyCalculator.java              ← depends on Constants
4.  Question.java — add new fields         ← depends on migration
5.  QuestionMaterializerService — update   ← depends on Calculator + Question
6.  DifficultyRecalibrationService.java    ← depends on Calculator + Repository
7.  QuestionRepository — add new methods   ← depends on Question fields
8.  AdminQuestionController — add endpoints ← depends on RecalibrationService
9.  Run backfill SQL                       ← depends on migration + live data
10. DifficultyCalculatorTest.java          ← can be written alongside step 3
```

---

### Step 1 — Migration

**File**: `backend/src/main/resources/db/migration/V13__question_difficulty_metrics.sql`

Add all columns listed above. Include `COMMENT ON COLUMN` for each. Add the three indexes. The `viability_exclusion_reason` column is nullable — add a `CHECK` constraint enforcing that it is non-null whenever `single_question_viable = false`:

```sql
ALTER TABLE questions
    ADD CONSTRAINT chk_viability_reason
    CHECK (single_question_viable = true OR viability_exclusion_reason IS NOT NULL);
```

---

### Step 2 — `DifficultyConstants.java`

**File**: `backend/src/main/java/com/football501/engine/DifficultyConstants.java`

A `final` class with a private constructor. All constants are `public static final`. No Spring annotations — this is a pure Java constants holder. See constants listing in the Formula Constants section above, including the two new viability constants `MIN_SCORE_POOL` and `MIN_ANSWER_COUNT`.

---

### Step 3 — `DifficultyCalculator.java`

**File**: `backend/src/main/java/com/football501/engine/DifficultyCalculator.java`

A `final` utility class with a private constructor and a single `public static double calculate(int highValue, int midRange, int checkout, int totalValid)` method. No Spring annotations — pure function, trivially unit-testable.

```java
public static double calculate(int highValueCount, int midRangeCount,
                               int checkoutCount,  int totalValidCount) {
    double ease =
        saturate(highValueCount, HIGH_VALUE_SATURATION) * WEIGHT_HIGH_VALUE
      + saturate(midRangeCount,  MID_RANGE_SATURATION)  * WEIGHT_MID_RANGE
      + saturate(checkoutCount,  CHECKOUT_SATURATION)   * WEIGHT_CHECKOUT;

    double base = 10.0 * (1.0 - ease);

    if (checkoutCount == 0) {
        base = Math.max(base, CHECKOUT_FLOOR);
    }

    double depthBonus = saturate(totalValidCount, TOTAL_VALID_SATURATION) * DEPTH_BONUS_MAX;

    return Math.max(0.0, Math.min(10.0, base - depthBonus));
}

private static double saturate(int count, double threshold) {
    return Math.min(count / threshold, 1.0);
}
```

---

### Step 4 — `Question.java` updates

**File**: `backend/src/main/java/com/football501/model/Question.java`

Add nine new Lombok-mapped fields (all with `@Builder.Default` where appropriate). Do not remove the existing `difficulty` field. Add a `@Transient boolean isViable()` convenience method.

```java
@Builder.Default private int     highValueCount           = 0;
@Builder.Default private int     midRangeCount            = 0;
@Builder.Default private int     checkoutCount            = 0;
@Builder.Default private int     totalValidCount          = 0;
@Builder.Default private int     totalScorePool           = 0;
@Builder.Default private boolean singleQuestionViable     = true;
                 private String  viabilityExclusionReason;        // null when viable
@Builder.Default private double  difficultyScore          = 5.0;
@Builder.Default private boolean difficultyLocked         = false;

@Transient
public boolean isViable() { return singleQuestionViable; }
```

---

### Step 5 — `QuestionMaterializerService` update

**File**: `backend/src/main/java/com/football501/service/QuestionMaterializerService.java`

Modify `upsertAnswers()` to accumulate zone counts during the existing answer iteration loop. After all answers are processed: compute difficulty, evaluate viability, and if non-viable auto-exclude the question before saving.

Key implementation notes:
- Add `QuestionRepository` to the constructor injection (currently absent)
- Only update the question metrics when `difficulty_locked` is `false`
- Update the question with a single `questionRepository.save(question)` call after all answers are processed
- Log computed metrics at `INFO` level; log auto-exclusion at `WARN` level

Zone assignment during iteration:
```java
if (isValidDarts && !isBust) {
    totalValidCount++;
    totalScorePool += ma.score();
    int s = ma.score();
    if      (s >= HIGH_VALUE_SCORE_MIN) highValueCount++;
    else if (s >= MID_RANGE_SCORE_MIN)  midRangeCount++;
    else                                 checkoutCount++;
}
```

Viability check and auto-exclusion after the loop:
```java
// Compute and persist difficulty metrics
question.setHighValueCount(highValueCount);
question.setMidRangeCount(midRangeCount);
question.setCheckoutCount(checkoutCount);
question.setTotalValidCount(totalValidCount);
question.setTotalScorePool(totalScorePool);

double score = DifficultyCalculator.calculate(
    highValueCount, midRangeCount, checkoutCount, totalValidCount);
question.setDifficultyScore(score);

// Evaluate viability — two hard conditions, both must pass
boolean viable = totalScorePool  >= DifficultyConstants.MIN_SCORE_POOL
              && totalValidCount  >= DifficultyConstants.MIN_ANSWER_COUNT;
question.setSingleQuestionViable(viable);

if (!viable) {
    String reason = buildViabilityReason(totalScorePool, totalValidCount);
    question.setViabilityExclusionReason(reason);
    question.setStatus("excluded");
    log.warn("Question {} auto-excluded: {}", question.getId(), reason);
} else {
    question.setViabilityExclusionReason(null);
}

questionRepository.save(question);
log.info("Question {} materialised — hv={} mid={} co={} total={} pool={} score={} viable={}",
    question.getId(), highValueCount, midRangeCount, checkoutCount,
    totalValidCount, totalScorePool, score, viable);
```

Private helper:
```java
private String buildViabilityReason(int scorePool, int validCount) {
    List<String> reasons = new ArrayList<>();
    if (scorePool < DifficultyConstants.MIN_SCORE_POOL) {
        reasons.add("insufficient_score_pool: " + scorePool
                  + " < " + DifficultyConstants.MIN_SCORE_POOL);
    }
    if (validCount < DifficultyConstants.MIN_ANSWER_COUNT) {
        reasons.add("insufficient_answer_count: " + validCount
                  + " < " + DifficultyConstants.MIN_ANSWER_COUNT);
    }
    return String.join("; ", reasons);
}
```

---

### Step 6 — `DifficultyRecalibrationService.java`

**File**: `backend/src/main/java/com/football501/service/DifficultyRecalibrationService.java`

A Spring `@Service` that bulk-recalculates `difficulty_score` for all unlocked questions using stored counts. Does not touch the `answers` table. Also re-evaluates viability on each question so that threshold changes (e.g. raising `MIN_ANSWER_COUNT` from 15 to 20) propagate correctly.

```java
@Transactional
public RecalibrationResult recalculateAll() {
    List<Question> questions = questionRepository.findByDifficultyLockedFalse();
    int updated = 0;
    int reExcluded = 0;
    for (Question q : questions) {
        double newScore = DifficultyCalculator.calculate(
            q.getHighValueCount(), q.getMidRangeCount(),
            q.getCheckoutCount(),  q.getTotalValidCount()
        );
        boolean nowViable = q.getTotalScorePool()  >= DifficultyConstants.MIN_SCORE_POOL
                         && q.getTotalValidCount() >= DifficultyConstants.MIN_ANSWER_COUNT;

        boolean changed = Math.abs(newScore - q.getDifficultyScore()) > 0.005
                       || nowViable != q.isSingleQuestionViable();
        if (changed) {
            q.setDifficultyScore(newScore);
            q.setSingleQuestionViable(nowViable);
            if (!nowViable) {
                q.setViabilityExclusionReason(
                    buildViabilityReason(q.getTotalScorePool(), q.getTotalValidCount()));
                if (!"excluded".equals(q.getStatus())) {
                    q.setStatus("excluded");
                    reExcluded++;
                }
            } else {
                q.setViabilityExclusionReason(null);
            }
            questionRepository.save(q);
            updated++;
        }
    }
    return new RecalibrationResult(questions.size(), updated, reExcluded);
}

public record RecalibrationResult(int total, int updated, int reExcluded) {}
```

---

### Step 7 — `QuestionRepository` updates

**File**: `backend/src/main/java/com/football501/repository/QuestionRepository.java`

Add the following methods alongside the existing ones. Do not remove `findByCategoryIdAndDifficultyAndStatus` — it may still be referenced by callers using the old integer field.

```java
// Used by recalibration service
List<Question> findByDifficultyLockedFalse();

// Standard game selection — callers pass maxScore ≤ 8.5 for ranked standard play
@Query("""
    SELECT q FROM Question q
    WHERE q.categoryId           = :categoryId
      AND q.status               = 'active'
      AND q.singleQuestionViable = true
      AND q.difficultyScore      BETWEEN :minScore AND :maxScore
    ORDER BY FUNCTION('random')
    """)
List<Question> findViableByDifficultyRange(
    @Param("categoryId") UUID   categoryId,
    @Param("minScore")   double minScore,
    @Param("maxScore")   double maxScore
);

// Admin diagnostics
List<Question> findByCheckoutCountAndStatus(int checkoutCount, String status);
List<Question> findByStatus(String status);  // for listing excluded questions in admin UI
```

> If `FUNCTION('random')` causes a Hibernate parsing issue, convert to `nativeQuery = true`.

---

### Step 8 — Admin endpoints

**File**: `backend/src/main/java/com/football501/controller/AdminQuestionController.java`

Add to the existing controller:

```
POST  /api/admin/questions/recalculate-difficulty
      → DifficultyRecalibrationService.recalculateAll()
      → 200 { "total": 142, "updated": 38, "reExcluded": 12 }

PATCH /api/admin/questions/{id}/difficulty-lock
      → body: { "locked": true, "difficultyScore": 6.5 }
      → sets difficulty_locked; difficultyScore is optional override

GET   /api/admin/questions?status=excluded
      → lists all auto-excluded questions with viabilityExclusionReason
      → allows admin to review what was excluded and why
```

The `viabilityExclusionReason` field must be included in `QuestionResponse` DTO so it appears in the admin question list when `status=excluded` is filtered.

---

### Step 9 — Backfill existing questions

**File**: `backend/src/main/resources/db/backfill_difficulty_scores.sql`

Run once after the V13 migration. Not a Flyway migration (the formula constants may change, and the SQL must stay in sync with `DifficultyConstants.java`).

```sql
-- Step 1: Populate zone counts from answers table
WITH metrics AS (
    SELECT
        question_id,
        COUNT(*)  FILTER (WHERE score BETWEEN 100 AND 180 AND is_valid_darts AND NOT is_bust) AS high_value_count,
        COUNT(*)  FILTER (WHERE score BETWEEN 20  AND 99  AND is_valid_darts AND NOT is_bust) AS mid_range_count,
        COUNT(*)  FILTER (WHERE score BETWEEN 1   AND 19  AND is_valid_darts AND NOT is_bust) AS checkout_count,
        COUNT(*)  FILTER (WHERE is_valid_darts AND NOT is_bust)                               AS total_valid_count,
        COALESCE(SUM(score) FILTER (WHERE is_valid_darts AND NOT is_bust), 0)                 AS total_score_pool
    FROM answers
    GROUP BY question_id
)
UPDATE questions q
SET
    high_value_count       = m.high_value_count,
    mid_range_count        = m.mid_range_count,
    checkout_count         = m.checkout_count,
    total_valid_count      = m.total_valid_count,
    total_score_pool       = m.total_score_pool,
    single_question_viable = (m.total_score_pool >= 501 AND m.total_valid_count >= 15),
    viability_exclusion_reason = CASE
        WHEN m.total_score_pool >= 501 AND m.total_valid_count >= 15 THEN NULL
        WHEN m.total_score_pool < 501 AND m.total_valid_count < 15 THEN
            'insufficient_score_pool: ' || m.total_score_pool || ' < 501; '
         || 'insufficient_answer_count: ' || m.total_valid_count || ' < 15'
        WHEN m.total_score_pool < 501 THEN
            'insufficient_score_pool: ' || m.total_score_pool || ' < 501'
        ELSE
            'insufficient_answer_count: ' || m.total_valid_count || ' < 15'
    END
FROM metrics m
WHERE q.id = m.question_id;

-- Step 2: Compute difficulty_score from stored counts
-- Keep numeric literals in sync with DifficultyConstants.java
UPDATE questions
SET difficulty_score = GREATEST(0.0, LEAST(10.0,
    CASE WHEN checkout_count = 0 THEN
        GREATEST(
            (1.0 - (
                LEAST(high_value_count / 25.0, 1.0) * 0.50 +
                LEAST(mid_range_count  / 40.0, 1.0) * 0.30
            )) * 10.0 - LEAST(total_valid_count / 200.0, 1.0) * 1.5,
            7.0
        )
    ELSE
        (1.0 - (
            LEAST(high_value_count / 25.0, 1.0) * 0.50 +
            LEAST(mid_range_count  / 40.0, 1.0) * 0.30 +
            LEAST(checkout_count   / 12.0, 1.0) * 0.20
        )) * 10.0 - LEAST(total_valid_count / 200.0, 1.0) * 1.5
    END
))
WHERE difficulty_locked = FALSE;

-- Step 3: Set status = 'excluded' for non-viable questions.
-- REVIEW the list below BEFORE running this step — do not run blindly.
-- SELECT id, text, viability_exclusion_reason FROM questions WHERE single_question_viable = false;
UPDATE questions
SET status = 'excluded'
WHERE single_question_viable = false
  AND status = 'active';

-- Step 4: Run the template diagnostic to identify structurally broken templates
SELECT
    qt.slug,
    COUNT(*)                                               AS total_questions,
    COUNT(*) FILTER (WHERE q.status = 'excluded')         AS excluded_count,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE q.status = 'excluded') / NULLIF(COUNT(*), 0),
        1
    )                                                      AS exclusion_rate_pct,
    ROUND(AVG(q.total_valid_count), 1)                     AS avg_answer_count,
    ROUND(AVG(q.total_score_pool), 0)                      AS avg_score_pool
FROM questions q
JOIN question_templates qt ON q.template_id = qt.id
GROUP BY qt.slug
ORDER BY exclusion_rate_pct DESC;
-- Any template with exclusion_rate_pct > 50 or avg_answer_count < 20 should be reviewed.
```

---

### Step 10 — Tests

**File**: `backend/src/test/java/com/football501/engine/DifficultyCalculatorTest.java`

| Test case | Inputs (hv, mid, co, total) | Expected |
|---|---|---|
| All zones saturated | (30, 50, 15, 250) | 0.0 |
| No answers | (0, 0, 0, 0) | 10.0 |
| No checkout answers | (30, 50, 0, 250) | ≥ 7.0 (floor active) |
| No checkout, small pool | (5, 8, 0, 13) | ≥ 7.0 |
| Bimodal (30×180, 1000×1) | (30, 0, 1000, 1030) | ~1.5 |
| Tiny mid-range only | (0, 8, 0, 8) | ≥ 7.0 |
| Depth bonus caps at saturation | (25, 40, 12, 500) equals (25, 40, 12, 200) | identical |
| Score never exceeds 10 | (0, 0, 0, 0) | ≤ 10.0 |
| Score never below 0 | (30, 50, 15, 500) | ≥ 0.0 |

**File**: `backend/src/test/java/com/football501/service/QuestionMaterializerServiceTest.java`

Update existing tests and add:

| Test case | Setup | Expected |
|---|---|---|
| Zone counts populated correctly | 10 answers across all zones | Counts match expected zone assignments |
| `singleQuestionViable` true when both conditions pass | pool ≥ 501 and count ≥ 15 | `singleQuestionViable = true`, status remains `active` |
| Auto-excluded on insufficient score pool | pool = 234, count = 20 | `status = 'excluded'`, reason contains `insufficient_score_pool` |
| Auto-excluded on insufficient answer count | pool = 800, count = 8 | `status = 'excluded'`, reason contains `insufficient_answer_count` |
| Auto-excluded when both conditions fail | pool = 100, count = 5 | Reason contains both failure messages |
| `difficultyLocked = true` prevents metrics overwrite | locked question re-materialised | Counts and score unchanged |
| `viabilityExclusionReason` null on viable question | pool = 600, count = 20 | `viabilityExclusionReason = null` |

---

## Recalibration Guide

If the difficulty scores feel wrong after observing real questions:

1. **Adjust a formula constant** in `DifficultyConstants.java`
2. Call `POST /api/admin/questions/recalculate-difficulty`
3. Review scores in the admin question list

If viability thresholds feel wrong (too many or too few questions excluded):

1. **Adjust `MIN_SCORE_POOL` or `MIN_ANSWER_COUNT`** in `DifficultyConstants.java`
2. Call `POST /api/admin/questions/recalculate-difficulty` — it re-evaluates viability using stored counts, no re-materialisation needed
3. Review the `?status=excluded` admin list to see what changed

If a specific question scores incorrectly and needs manual fixing:
1. Call `PATCH /api/admin/questions/{id}/difficulty-lock` with `{ "locked": true, "difficultyScore": X.X }`

If zone boundaries need changing (e.g. checkout range 1–19 → 1–25):
1. Update `DifficultyConstants.java`
2. Run the backfill SQL against the live database (this re-counts from the answers table)
3. Or trigger re-materialisation of all affected questions

Zone boundary changes are the expensive operation. Formula constant changes and viability threshold changes are free.

---

## Out of Scope (Future Work)

- **Remove old `difficulty INTEGER` field** — once all callers of `findByCategoryIdAndDifficultyAndStatus` are migrated, drop the column in a dedicated cleanup migration
- **Expose `difficulty_score` and `viabilityExclusionReason` in admin UI** — add to `QuestionResponse` DTO and the question list view; show exclusion reason inline on excluded rows
- **Difficulty range presets** — named bands (e.g. Accessible 0–4, Competitive 4–7, Expert 7–10) defined as UI constants at render time, never stored
- **Expert Challenge mode** — questions with `difficulty_score > 8.5` that are excluded from standard play could be surfaced here; requires a new game mode flag on `matches`
- **Template suitability flags** — a `suitable_for_game_modes JSONB` column on `question_templates` to declare which modes a template can support; prevents future scope creep where unsuitable templates are re-enabled for the wrong modes
