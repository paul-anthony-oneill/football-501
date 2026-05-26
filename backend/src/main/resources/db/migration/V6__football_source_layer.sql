-- Migration V6: Football Source Layer — normalised stats tables
-- Created: 2026-05-25
--
-- Adds the football-domain source tables that power the materialisation pipeline.
-- All changes are additive — no existing tables or data are modified destructively.
--
-- Also normalises competition_type enum values to the canonical vocabulary
-- and adds the tier column to competitions.
--
-- Destructive removal of players.career_stats and players.fbref_id happens
-- in V9, after the Python backfill (V8) has been verified.

-- ========================================
-- 0. Normalise competition_type enum values
-- ========================================
-- V1 seeded 'cup' and 'continental'; canonical slugs are now 'domestic_cup'
-- and 'continental_club'. 'domestic_league' and 'international' were correct.

UPDATE competitions SET competition_type = 'domestic_cup'    WHERE competition_type = 'cup';
UPDATE competitions SET competition_type = 'continental_club' WHERE competition_type = 'continental';

-- Add tier column for league-pyramid future-proofing.
-- 1 = top-flight domestic league; NULL for cups and continental competitions.
ALTER TABLE competitions ADD COLUMN tier SMALLINT;

COMMENT ON COLUMN competitions.tier IS '1 for top-flight domestic leagues; NULL for cups and UEFA competitions.';
COMMENT ON COLUMN competitions.competition_type IS
    'Canonical values: domestic_league, domestic_cup, continental_club, international, continental_national.';

-- ========================================
-- 1. seasons
-- ========================================
-- One row per season cycle.
-- Cups that span the calendar year are mapped to the league season they fall
-- within (e.g. FA Cup 2023-24 → label = ''2023-24'').
-- UEFA competitions follow the same convention.

CREATE TABLE seasons (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    label       VARCHAR(10) NOT NULL UNIQUE,    -- '2023-24'
    start_year  SMALLINT    NOT NULL,           -- 2023
    end_year    SMALLINT    NOT NULL,           -- 2024
    start_date  DATE,                           -- approximate; used for cup overlap resolution
    end_date    DATE,
    is_current  BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_seasons_start_year ON seasons (start_year);

COMMENT ON TABLE  seasons            IS 'One row per season cycle (e.g. 2023-24). Cups map to the overlapping league season.';
COMMENT ON COLUMN seasons.label      IS 'Human-readable season key, e.g. ''2023-24''.';
COMMENT ON COLUMN seasons.start_year IS 'Calendar year the season begins in.';
COMMENT ON COLUMN seasons.is_current IS 'TRUE for the one active season; updated nightly by the scraper.';

-- ========================================
-- 2. player_external_ids
-- ========================================
-- Moves external-source identity out of players.fbref_id into a dedicated,
-- multi-source table.  FBref is source #1; Transfermarkt, Sofascore, and
-- Wikidata can be added later without schema changes.
-- The players.fbref_id column stays until V9.

CREATE TABLE player_external_ids (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    source      VARCHAR(32) NOT NULL,       -- 'fbref', 'transfermarkt', 'sofascore', 'wikidata'
    external_id VARCHAR(64) NOT NULL,
    source_url  TEXT,
    confidence  SMALLINT    NOT NULL DEFAULT 100,  -- 100 = exact; lower = fuzzy cross-source link
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (source, external_id)
);

CREATE INDEX idx_player_ext_player ON player_external_ids (player_id);

COMMENT ON TABLE  player_external_ids            IS 'Multi-source external IDs for players (FBref, Transfermarkt, Sofascore, Wikidata).';
COMMENT ON COLUMN player_external_ids.source     IS 'Source slug: fbref, transfermarkt, sofascore, wikidata.';
COMMENT ON COLUMN player_external_ids.confidence IS '100 = exact match; lower values for fuzzy cross-source linkage.';

-- ========================================
-- 3. team_external_ids
-- ========================================
-- Same shape as player_external_ids.
-- teams.fbref_id stays until V9.

CREATE TABLE team_external_ids (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    source      VARCHAR(32) NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    source_url  TEXT,
    confidence  SMALLINT    NOT NULL DEFAULT 100,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (source, external_id)
);

CREATE INDEX idx_team_ext_team ON team_external_ids (team_id);

COMMENT ON TABLE team_external_ids IS 'Multi-source external IDs for teams. Same shape as player_external_ids.';

-- ========================================
-- 4. player_season_stints — the keystone
-- ========================================
-- One row per (player, season, team, competition).
--
-- Examples:
--   A player who played for two clubs in 2018-19 EPL → two rows (one per club).
--   A player who played EPL and UCL for the same club → two rows (one per competition).
--
-- This replaces players.career_stats JSONB once the V8 Python backfill is verified.

CREATE TABLE player_season_stints (
    id               UUID     PRIMARY KEY DEFAULT gen_random_uuid(),

    player_id        UUID     NOT NULL REFERENCES players(id)       ON DELETE CASCADE,
    season_id        UUID     NOT NULL REFERENCES seasons(id)       ON DELETE RESTRICT,
    team_id          UUID     NOT NULL REFERENCES teams(id)         ON DELETE RESTRICT,
    competition_id   UUID     NOT NULL REFERENCES competitions(id)  ON DELETE RESTRICT,

    -- Appearance breakdown
    appearances      SMALLINT NOT NULL DEFAULT 0,
    starts           SMALLINT NOT NULL DEFAULT 0,
    sub_appearances  SMALLINT NOT NULL DEFAULT 0,
    minutes          INTEGER  NOT NULL DEFAULT 0,

    -- Outfield stats
    goals            SMALLINT NOT NULL DEFAULT 0,
    assists          SMALLINT NOT NULL DEFAULT 0,

    -- Discipline
    yellow_cards     SMALLINT NOT NULL DEFAULT 0,
    red_cards        SMALLINT NOT NULL DEFAULT 0,

    -- Goalkeeper stats (always 0 for outfield players)
    clean_sheets     SMALLINT NOT NULL DEFAULT 0,
    goals_conceded   SMALLINT NOT NULL DEFAULT 0,
    is_goalkeeper    BOOLEAN  NOT NULL DEFAULT FALSE,

    -- Ingest provenance
    source           VARCHAR(32) NOT NULL,      -- 'fbref'
    source_scraped_at TIMESTAMP  NOT NULL,

    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (player_id, season_id, team_id, competition_id)
);

-- Primary aggregation pattern: stat by (team, competition) across seasons.
CREATE INDEX idx_stints_team_comp_season
    ON player_season_stints (team_id, competition_id, season_id);

-- Career totals filtered by competition.
CREATE INDEX idx_stints_player_comp
    ON player_season_stints (player_id, competition_id);

-- Template generator: enumerate all players in a (competition, season) pair.
CREATE INDEX idx_stints_comp_season
    ON player_season_stints (competition_id, season_id);

COMMENT ON TABLE  player_season_stints IS
    'One row per (player, season, team, competition). Replaces players.career_stats JSONB after V8 backfill.';
COMMENT ON COLUMN player_season_stints.is_goalkeeper    IS
    'TRUE for goalkeeper stints; enables clean_sheets/goals_conceded to be non-zero.';
COMMENT ON COLUMN player_season_stints.source           IS
    'Ingest source slug, e.g. ''fbref''.';
COMMENT ON COLUMN player_season_stints.source_scraped_at IS
    'Timestamp of the scrape run that produced this row.';
