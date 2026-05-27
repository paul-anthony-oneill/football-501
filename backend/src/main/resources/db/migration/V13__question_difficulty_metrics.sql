-- V13: Add difficulty-metrics and viability columns to the questions table.
--
-- These columns are populated by QuestionMaterializerService (at materialisation
-- time) and updated in bulk by DifficultyRecalibrationService (after formula
-- constant changes). Stored counts decouple formula tuning from re-materialisation:
-- a formula change costs a single SQL UPDATE rather than re-running the scraper.
--
-- See docs/design/DIFFICULTY_SCORING.md for the full design rationale.

-- ── Zone-count columns ──────────────────────────────────────────────────────

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS high_value_count   INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN questions.high_value_count IS
    'Count of valid-darts, non-bust answers scoring 100–180 (high-velocity zone).';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS mid_range_count    INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN questions.mid_range_count IS
    'Count of valid-darts, non-bust answers scoring 20–99 (navigation zone).';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS checkout_count     INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN questions.checkout_count IS
    'Count of valid-darts, non-bust answers scoring 1–19 (checkout / precision zone).';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS total_valid_count  INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN questions.total_valid_count IS
    'Total count of valid-darts, non-bust answers across all zones.';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS total_score_pool   INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN questions.total_score_pool IS
    'Sum of scores for all valid-darts, non-bust answers. '
    'Must be >= 501 for the question to be viable in standard single-question mode.';

-- ── Viability columns ──────────────────────────────────────────────────────

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS single_question_viable       BOOLEAN NOT NULL DEFAULT TRUE;
COMMENT ON COLUMN questions.single_question_viable IS
    'TRUE when total_score_pool >= 501 AND total_valid_count >= 15. '
    'Questions failing either condition are auto-excluded at materialisation time '
    'and excluded from all game-selection queries.';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS viability_exclusion_reason   TEXT NULL;
COMMENT ON COLUMN questions.viability_exclusion_reason IS
    'NULL on viable questions. Populated with a human-readable string explaining '
    'which viability condition(s) failed whenever single_question_viable = false. '
    'Example: "insufficient_score_pool: 234 < 501; insufficient_answer_count: 8 < 15". '
    'Required for admin observability of auto-excluded questions.';

-- ── Difficulty-score columns ───────────────────────────────────────────────

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS difficulty_score   NUMERIC(4,2) NOT NULL DEFAULT 5.00;
COMMENT ON COLUMN questions.difficulty_score IS
    'Continuous difficulty value 0.00–10.00 computed from zone counts by DifficultyCalculator. '
    '0.00 = easiest, 10.00 = hardest. Derived labels (Easy/Hard) are computed at render time '
    'and never stored. Formula constants live in DifficultyConstants.java.';

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS difficulty_locked  BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN questions.difficulty_locked IS
    'When TRUE the recalibration job skips this question, preserving a manually '
    'overridden difficulty_score. Set via PATCH /api/admin/questions/{id}/difficulty-lock.';

-- ── Add "excluded" to the status lifecycle ────────────────────────────────
-- The existing status column is VARCHAR(20) with a CHECK constraint.
-- We need to widen the allowed values to include "excluded".
-- Drop and recreate the constraint if it exists; ignore if absent.

DO $$
BEGIN
    ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_status;
    ALTER TABLE questions ADD CONSTRAINT chk_questions_status
        CHECK (status IN ('draft', 'active', 'retired', 'excluded'));
EXCEPTION WHEN others THEN
    -- Constraint may not exist yet; ignore
    NULL;
END
$$;

-- ── Viability consistency constraint ──────────────────────────────────────

ALTER TABLE questions
    DROP CONSTRAINT IF EXISTS chk_viability_reason;

ALTER TABLE questions
    ADD CONSTRAINT chk_viability_reason
    CHECK (single_question_viable = TRUE OR viability_exclusion_reason IS NOT NULL);

-- ── Performance indexes ───────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_questions_difficulty_score
    ON questions (difficulty_score)
    WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_questions_viable
    ON questions (single_question_viable)
    WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_questions_excluded
    ON questions (status)
    WHERE status = 'excluded';
