-- Migration V9: Drop legacy players.career_stats, players.fbref_id, teams.fbref_id
-- Created: 2026-05-25
--
-- Prerequisites:
--   • V6 backfill script (backfill_season_stints.py) completed successfully.
--   • verify_parity.py exited 0 — all career_stats totals match player_season_stints.
--   • player_external_ids populated for every real FBref player ID.
--   • team_external_ids populated for every real FBref team ID.
--
-- What this removes:
--   1. Trigger + function that validated the career_stats JSONB array.
--   2. players.career_stats  — normalised replacement is player_season_stints.
--   3. players.fbref_id      — normalised replacement is player_external_ids.
--   4. teams.fbref_id        — normalised replacement is team_external_ids.
--
-- Associated indexes are dropped automatically by PostgreSQL when their
-- column is dropped:
--   idx_players_career_stats  (GIN on career_stats)
--   idx_players_fbref         (B-tree on players.fbref_id)
--   idx_teams_fbref           (B-tree on teams.fbref_id)
--
-- Note: competitions.fbref_id is NOT dropped here — no competition_external_ids
-- table has been created yet. That column is low-traffic and harmless to keep.

-- ========================================
-- 1. Drop the career_stats trigger + function
-- ========================================
-- Must come before the column drop; otherwise the trigger body referencing
-- NEW.career_stats would become invalid and PostgreSQL would error.

DROP TRIGGER IF EXISTS validate_career_stats_before_insert ON players;
DROP FUNCTION IF EXISTS validate_career_stats_jsonb();

-- ========================================
-- 2. Drop players legacy columns
-- ========================================
-- career_stats: superseded by player_season_stints (V6).
-- fbref_id:     superseded by player_external_ids   (V6).
--
-- Dropping a column with a UNIQUE or NOT NULL constraint is safe — PostgreSQL
-- removes the backing unique index and constraint automatically.

ALTER TABLE players
    DROP COLUMN career_stats,
    DROP COLUMN fbref_id;

-- ========================================
-- 3. Drop teams legacy column
-- ========================================
-- fbref_id: superseded by team_external_ids (V6).

ALTER TABLE teams
    DROP COLUMN fbref_id;

-- ========================================
-- 4. Update comments
-- ========================================

COMMENT ON TABLE players IS
    'Players reference table. External IDs live in player_external_ids; per-season stats in player_season_stints.';

COMMENT ON TABLE teams IS
    'Teams reference table (clubs and national teams). External IDs live in team_external_ids.';
