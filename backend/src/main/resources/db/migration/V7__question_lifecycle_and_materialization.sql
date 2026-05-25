-- Migration V7: Game Engine Evolution — question lifecycle + materialisation
-- Created: 2026-05-25
--
-- Changes:
--   • Adds question_templates (hybrid metadata-in-DB / logic-in-code model).
--   • Adds status lifecycle to questions: 'draft' | 'active' | 'retired'.
--   • Removes questions.is_active (replaced by status).
--   • Adds answers.materialized_at for stale-answer detection.
--   • Adds scrape_run_logs for per-message ingest audit trail.
--
-- Breaking: questions.is_active is dropped.
--   Backfill: active=TRUE  → status='active'
--             active=FALSE → status='retired'

-- ========================================
-- 1. question_templates
-- ========================================

CREATE TABLE question_templates (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id      UUID        NOT NULL REFERENCES categories(id) ON DELETE CASCADE,

    -- URL-safe identifier — used by the generator and admin UI.
    slug             VARCHAR(64) NOT NULL UNIQUE,

    -- Human-readable label for the admin UI.
    display_name     VARCHAR(255) NOT NULL,

    -- Text with named {placeholders} for each param.
    -- Example: 'Goals for {team_name} in {competition_name} since {start_year}'
    text_template    TEXT        NOT NULL,

    -- JSONB schema describing required params and enumeration strategies.
    -- Example:
    -- {
    --   "params": {
    --     "team_id":        {"type": "team_ref",        "enumerate": "competition_slugs:[epl,laliga]"},
    --     "competition_id": {"type": "competition_ref", "enumerate": "slugs:[epl,laliga]"},
    --     "start_year":     {"type": "int",             "enumerate": "values:[2000]"}
    --   }
    -- }
    param_schema     JSONB       NOT NULL,

    -- Matches a registered Java QuestionMaterializer bean.
    -- Example: 'football.team_competition_metric_since'
    materializer_key VARCHAR(64) NOT NULL,

    -- Default metric key passed to the materializer.
    -- Example: 'goals', 'assists', 'appearances'
    metric_key       VARCHAR(50) NOT NULL,

    default_min_score INTEGER,

    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,

    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  question_templates                  IS
    'Template definitions for auto-generated questions. Metadata in DB; materialiser logic in Java (QuestionMaterializer).';
COMMENT ON COLUMN question_templates.slug             IS 'URL-safe unique slug, e.g. ''team_competition_metric_since''.';
COMMENT ON COLUMN question_templates.text_template    IS
    'Question text with {placeholders} for param values, e.g. ''Goals for {team_name} in {competition_name} since {start_year}''.';
COMMENT ON COLUMN question_templates.param_schema     IS
    'JSONB schema listing required params and how to enumerate them for auto-generation.';
COMMENT ON COLUMN question_templates.materializer_key IS
    'Key of the Java QuestionMaterializer implementation that converts (template, params) → answers rows.';

-- ========================================
-- 2. Evolve questions — add columns
-- ========================================

ALTER TABLE questions
    ADD COLUMN template_id     UUID        REFERENCES question_templates(id) ON DELETE SET NULL,
    ADD COLUMN template_params JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN status          VARCHAR(20) NOT NULL DEFAULT 'active';

-- 2a. Backfill status from is_active before dropping the column.
--     Explicitly inactive questions map to 'retired' (they were disabled for a reason).
UPDATE questions SET status = CASE WHEN is_active THEN 'active' ELSE 'retired' END;

-- 2b. Drop is_active now that status is populated.
ALTER TABLE questions DROP COLUMN is_active;

-- 2c. Indexes for the new columns.
CREATE INDEX idx_questions_status   ON questions (status);
CREATE INDEX idx_questions_template ON questions (template_id);

COMMENT ON COLUMN questions.template_id     IS
    'NULL for hand-curated questions; references the generating template for auto-generated ones.';
COMMENT ON COLUMN questions.template_params IS
    'Concrete param bindings resolved from the template (denormalised snapshot for the materialiser).';
COMMENT ON COLUMN questions.status          IS
    'Lifecycle: draft (not yet materialised / not in rotation), active (in play), retired (removed from rotation).';

-- ========================================
-- 3. Evolve answers — add materialized_at
-- ========================================

ALTER TABLE answers
    ADD COLUMN materialized_at TIMESTAMP NOT NULL DEFAULT NOW();

COMMENT ON COLUMN answers.materialized_at IS
    'Timestamp of last materialisation. The stale-answer detector re-materialises answers whose stints updated after this timestamp.';

-- ========================================
-- 4. scrape_run_logs
-- ========================================
-- Per-message audit log for scrape jobs, written by the Python microservice.
-- One scrape_jobs row may produce many scrape_run_logs rows.

CREATE TABLE scrape_run_logs (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id    UUID        NOT NULL REFERENCES scrape_jobs(id) ON DELETE CASCADE,
    level     VARCHAR(10) NOT NULL,          -- 'INFO', 'WARN', 'ERROR'
    message   TEXT        NOT NULL,
    context   JSONB       NOT NULL DEFAULT '{}'::jsonb,
    logged_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_run_logs_job   ON scrape_run_logs (job_id);
CREATE INDEX idx_scrape_run_logs_level ON scrape_run_logs (level, logged_at DESC);

COMMENT ON TABLE  scrape_run_logs         IS
    'Per-message audit log for scrape jobs. Written by the Python microservice; read by the admin UI.';
COMMENT ON COLUMN scrape_run_logs.level   IS 'Log level: INFO, WARN, ERROR.';
COMMENT ON COLUMN scrape_run_logs.context IS
    'Structured context JSONB (player_id, team_id, error details, etc.)';
