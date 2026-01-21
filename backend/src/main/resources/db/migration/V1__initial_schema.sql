-- Football 501 Database - Initial Schema (V3 - JSONB)
-- Flyway Migration V1
-- Created: 2026-01-20

-- ========================================
-- Extensions
-- ========================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ========================================
-- Teams Table
-- ========================================

CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    team_type VARCHAR(50) NOT NULL, -- 'club', 'national'
    country VARCHAR(100),
    fbref_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_teams_name_type ON teams (name, team_type);
CREATE INDEX idx_teams_fbref ON teams (fbref_id);
CREATE INDEX idx_teams_normalized_name ON teams (normalized_name);

COMMENT ON TABLE teams IS 'Reference table for football clubs and national teams';
COMMENT ON COLUMN teams.team_type IS 'Either ''club'' or ''national''';
COMMENT ON COLUMN teams.fbref_id IS 'FBRef.com team identifier';

-- ========================================
-- Competitions Table
-- ========================================

CREATE TABLE competitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    competition_type VARCHAR(50) NOT NULL, -- 'domestic_league', 'continental', 'international', 'cup'
    country VARCHAR(100), -- NULL for international
    fbref_id VARCHAR(100),
    display_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_competitions_name_type_country ON competitions (name, competition_type, COALESCE(country, ''));
CREATE INDEX idx_competitions_fbref ON competitions (fbref_id);
CREATE INDEX idx_competitions_normalized_name ON competitions (normalized_name);

COMMENT ON TABLE competitions IS 'Reference table for football competitions (leagues, cups, international tournaments)';
COMMENT ON COLUMN competitions.competition_type IS 'One of: domestic_league, continental, international, cup';
COMMENT ON COLUMN competitions.country IS 'NULL for international competitions';

-- ========================================
-- Players Table (JSONB Storage)
-- ========================================

CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fbref_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    nationality VARCHAR(100),
    career_stats JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_scraped_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_players_fbref ON players (fbref_id);
CREATE INDEX idx_players_name_trgm ON players USING GIN (name gin_trgm_ops);
CREATE INDEX idx_players_normalized_name ON players (normalized_name);
CREATE INDEX idx_players_career_stats ON players USING GIN (career_stats);

COMMENT ON TABLE players IS 'Players with career statistics stored in JSONB format';
COMMENT ON COLUMN players.fbref_id IS 'FBRef.com player identifier (primary external ID)';
COMMENT ON COLUMN players.normalized_name IS 'Lowercase name for fuzzy matching';
COMMENT ON COLUMN players.career_stats IS 'Array of season objects: [{season, team, competition, appearances, goals, assists}]';
COMMENT ON COLUMN players.last_scraped_at IS 'Last time player data was scraped from FBRef';

-- ========================================
-- Questions Table
-- ========================================

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_text TEXT NOT NULL,
    stat_type VARCHAR(50) NOT NULL, -- 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'

    -- Filters (NULL = no filter)
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    competition_id UUID REFERENCES competitions(id) ON DELETE SET NULL,
    season_filter VARCHAR(20), -- '2023-2024', 'career', NULL for all
    nationality_filter VARCHAR(100),

    -- Configuration
    min_score INTEGER,
    is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_questions_active ON questions (is_active);
CREATE INDEX idx_questions_filters ON questions (team_id, competition_id, season_filter);

COMMENT ON TABLE questions IS 'Question definitions with filter criteria';
COMMENT ON COLUMN questions.stat_type IS 'Type of statistic to query: appearances, goals, combined_apps_goals, goalkeeper';
COMMENT ON COLUMN questions.season_filter IS 'Filter by season (e.g., ''2023-2024''), ''career'' for all-time, or NULL for any season';
COMMENT ON COLUMN questions.min_score IS 'Minimum score threshold for valid answers';

-- ========================================
-- Question Valid Answers Table
-- ========================================

CREATE TABLE question_valid_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    player_name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    score INTEGER NOT NULL,
    is_valid_darts_score BOOLEAN NOT NULL,
    is_bust BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    last_computed TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_qva_question_player ON question_valid_answers (question_id, player_id);
CREATE INDEX idx_qva_question ON question_valid_answers (question_id);
CREATE INDEX idx_qva_normalized_name_trgm ON question_valid_answers USING GIN (normalized_name gin_trgm_ops);

COMMENT ON TABLE question_valid_answers IS 'Pre-computed valid answers for questions (populated from JSONB queries)';
COMMENT ON COLUMN question_valid_answers.score IS 'Calculated score based on stat_type (appearances, goals, sum, etc.)';
COMMENT ON COLUMN question_valid_answers.is_valid_darts_score IS 'TRUE if score is achievable in standard 501 darts (1-180 except 163,166,169,172,173,175,176,178,179)';
COMMENT ON COLUMN question_valid_answers.is_bust IS 'TRUE if score > 180';

-- ========================================
-- Scrape Jobs Table
-- ========================================

CREATE TABLE scrape_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL, -- 'initial', 'weekly_update', 'manual'
    season VARCHAR(20),
    competition_id UUID REFERENCES competitions(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL, -- 'running', 'success', 'failed', 'partial'
    players_scraped INTEGER DEFAULT 0,
    players_failed INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE INDEX idx_scrape_jobs_started ON scrape_jobs (started_at DESC);
CREATE INDEX idx_scrape_jobs_status ON scrape_jobs (status);

COMMENT ON TABLE scrape_jobs IS 'Audit log for scraping operations';
COMMENT ON COLUMN scrape_jobs.job_type IS 'Type of scraping job: initial, weekly_update, manual';
COMMENT ON COLUMN scrape_jobs.status IS 'Job status: running, success, failed, partial';

-- ========================================
-- JSONB Validation Function
-- ========================================

CREATE OR REPLACE FUNCTION validate_career_stats_jsonb()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure career_stats is an array
    IF jsonb_typeof(NEW.career_stats) != 'array' THEN
        RAISE EXCEPTION 'career_stats must be a JSON array';
    END IF;

    -- Validate each season object has required fields
    IF EXISTS (
        SELECT 1
        FROM jsonb_array_elements(NEW.career_stats) as season
        WHERE NOT (
            season ? 'season' AND
            season ? 'team' AND
            season ? 'competition' AND
            season ? 'appearances'
        )
    ) THEN
        RAISE EXCEPTION 'Each season must have: season, team, competition, appearances';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_career_stats_before_insert
    BEFORE INSERT OR UPDATE ON players
    FOR EACH ROW
    EXECUTE FUNCTION validate_career_stats_jsonb();

COMMENT ON FUNCTION validate_career_stats_jsonb() IS 'Validates JSONB structure for career_stats column';

-- ========================================
-- Seed Data: Premier League
-- ========================================

INSERT INTO competitions (name, normalized_name, competition_type, country, display_name)
VALUES
    ('Premier League', 'premier league', 'domestic_league', 'England', 'Premier League'),
    ('Champions League', 'champions league', 'continental', NULL, 'UEFA Champions League'),
    ('Europa League', 'europa league', 'continental', NULL, 'UEFA Europa League'),
    ('FA Cup', 'fa cup', 'cup', 'England', 'FA Cup'),
    ('EFL Cup', 'efl cup', 'cup', 'England', 'EFL Cup')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE competitions IS 'Seeded with major English and European competitions';
