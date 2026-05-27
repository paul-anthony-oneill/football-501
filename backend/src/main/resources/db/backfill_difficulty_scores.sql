-- backfill_difficulty_scores.sql
--
-- Run ONCE after the V13 migration to populate difficulty metrics for questions
-- that were already materialised before Phase 4 was deployed.
--
-- This is NOT a Flyway migration. The formula constants may be tuned over time
-- and this SQL must stay in sync with DifficultyConstants.java. If you change
-- a zone boundary or saturation constant, update the literals here before re-running.
--
-- Execution order:
--   Step 1: Populate zone counts from answers table
--   Step 2: Compute difficulty_score from stored counts
--   Step 3: Auto-exclude non-viable questions (REVIEW before running)
--   Step 4: Run template diagnostic
--
-- IMPORTANT: Run Steps 1–2 first. Review the output of the SELECT at the start
-- of Step 3 before running the UPDATE. Never run Step 3 blindly.

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 1: Populate zone counts from the answers table
-- ─────────────────────────────────────────────────────────────────────────────
-- Zone boundaries — keep in sync with DifficultyConstants.java:
--   Checkout  : 1–19
--   Mid-range : 20–99
--   High-value: 100–180

WITH metrics AS (
    SELECT
        question_id,
        COUNT(*)   FILTER (WHERE score BETWEEN 100 AND 180 AND is_valid_darts = TRUE AND is_bust = FALSE)  AS high_value_count,
        COUNT(*)   FILTER (WHERE score BETWEEN 20  AND 99  AND is_valid_darts = TRUE AND is_bust = FALSE)  AS mid_range_count,
        COUNT(*)   FILTER (WHERE score BETWEEN 1   AND 19  AND is_valid_darts = TRUE AND is_bust = FALSE)  AS checkout_count,
        COUNT(*)   FILTER (WHERE is_valid_darts = TRUE AND is_bust = FALSE)                                AS total_valid_count,
        COALESCE(
            SUM(score) FILTER (WHERE is_valid_darts = TRUE AND is_bust = FALSE),
            0
        )                                                                                                  AS total_score_pool
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
        WHEN m.total_score_pool >= 501 AND m.total_valid_count >= 15
            THEN NULL
        WHEN m.total_score_pool < 501 AND m.total_valid_count < 15
            THEN 'insufficient_score_pool: ' || m.total_score_pool || ' < 501; '
              || 'insufficient_answer_count: ' || m.total_valid_count || ' < 15'
        WHEN m.total_score_pool < 501
            THEN 'insufficient_score_pool: ' || m.total_score_pool || ' < 501'
        ELSE
            'insufficient_answer_count: ' || m.total_valid_count || ' < 15'
    END
FROM metrics m
WHERE q.id = m.question_id;

-- Questions with no answers at all (materialised but empty) — mark non-viable.
UPDATE questions
SET
    single_question_viable     = FALSE,
    viability_exclusion_reason = 'insufficient_score_pool: 0 < 501; insufficient_answer_count: 0 < 15'
WHERE id NOT IN (SELECT DISTINCT question_id FROM answers)
  AND single_question_viable = TRUE;  -- only update defaults that haven't been touched above

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 2: Compute difficulty_score from stored counts
-- ─────────────────────────────────────────────────────────────────────────────
-- Keep numeric literals in sync with DifficultyConstants.java:
--   HIGH_VALUE_SATURATION  = 25.0   WEIGHT_HIGH_VALUE = 0.50
--   MID_RANGE_SATURATION   = 40.0   WEIGHT_MID_RANGE  = 0.30
--   CHECKOUT_SATURATION    = 12.0   WEIGHT_CHECKOUT   = 0.20
--   TOTAL_VALID_SATURATION = 200.0  DEPTH_BONUS_MAX   = 1.5
--   CHECKOUT_FLOOR         = 7.0

UPDATE questions
SET difficulty_score = GREATEST(0.0, LEAST(10.0,
    CASE
        WHEN checkout_count = 0 THEN
            GREATEST(
                (1.0 - (
                    LEAST(high_value_count / 25.0, 1.0) * 0.50 +
                    LEAST(mid_range_count  / 40.0, 1.0) * 0.30
                )) * 10.0
                - LEAST(total_valid_count / 200.0, 1.0) * 1.5,
                7.0   -- CHECKOUT_FLOOR
            )
        ELSE
            (1.0 - (
                LEAST(high_value_count / 25.0, 1.0) * 0.50 +
                LEAST(mid_range_count  / 40.0, 1.0) * 0.30 +
                LEAST(checkout_count   / 12.0, 1.0) * 0.20
            )) * 10.0
            - LEAST(total_valid_count / 200.0, 1.0) * 1.5
    END
))
WHERE difficulty_locked = FALSE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 3: Auto-exclude non-viable questions
-- ─────────────────────────────────────────────────────────────────────────────
-- REVIEW the list below BEFORE running the UPDATE.
-- This shows you what will be excluded and why. Only proceed if the list
-- looks correct (you may want to review borderline cases manually).

SELECT
    id,
    question_text,
    total_score_pool,
    total_valid_count,
    viability_exclusion_reason
FROM questions
WHERE single_question_viable = FALSE
  AND status = 'active'
ORDER BY total_valid_count ASC;

-- ↑ Review the above SELECT output, then run the UPDATE below ↓
-- (Remove the block comment delimiters when you are ready.)

/*
UPDATE questions
SET status = 'excluded'
WHERE single_question_viable = FALSE
  AND status = 'active';
*/

-- ─────────────────────────────────────────────────────────────────────────────
-- Step 4: Template diagnostic
-- ─────────────────────────────────────────────────────────────────────────────
-- Any template with exclusion_rate_pct > 50 or avg_answer_count < 20 should
-- be reviewed and likely disabled (set is_active = false on the template row).
-- The `team_competition_sub_appearances_since` template is known to be broken —
-- see docs/design/DIFFICULTY_SCORING.md §Structurally Incompatible Templates.

SELECT
    qt.slug,
    COUNT(*)                                                                        AS total_questions,
    COUNT(*) FILTER (WHERE q.status = 'excluded')                                  AS excluded_count,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE q.status = 'excluded') / NULLIF(COUNT(*), 0),
        1
    )                                                                               AS exclusion_rate_pct,
    ROUND(AVG(q.total_valid_count), 1)                                              AS avg_answer_count,
    ROUND(AVG(q.total_score_pool),  0)                                              AS avg_score_pool
FROM questions q
JOIN question_templates qt ON q.template_id = qt.id
GROUP BY qt.slug
ORDER BY exclusion_rate_pct DESC NULLS LAST;
