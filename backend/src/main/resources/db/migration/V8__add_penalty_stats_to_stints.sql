-- Migration V8: Add penalty stats to player_season_stints
-- Created: 2026-05-25
--
-- Adds penalty_goals and penalty_attempts to player_season_stints so the
-- materialiser can generate non-penalty goal questions.
--
-- Derivations available after this migration:
--   non_penalty_goals = goals - penalty_goals
--   penalty_conversion = penalty_goals / NULLIF(penalty_attempts, 0)
--
-- No data backfill needed — existing rows correctly default to 0.
-- The V8 Python backfill script has been superseded by the new multi-league
-- historical scraper; this migration replaces it.

ALTER TABLE player_season_stints
    ADD COLUMN penalty_goals    SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN penalty_attempts SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN player_season_stints.penalty_goals    IS
    'Penalty goals scored (FBref: Performance_PK). Subtract from goals to get non-penalty goals.';
COMMENT ON COLUMN player_season_stints.penalty_attempts IS
    'Penalty attempts (FBref: Performance_PKatt). Includes scored, missed, and saved.';
