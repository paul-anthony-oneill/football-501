-- Football 501 Database Initialization Script
-- Creates all tables and indexes needed for the scraping service

-- Enable trigram extension for fuzzy player name matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ========================================
-- Questions Table
-- ========================================
CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    league VARCHAR(100),
    season VARCHAR(20),
    team VARCHAR(100),
    stat_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW()
);

-- ========================================
-- Answers Table
-- ========================================
CREATE TABLE IF NOT EXISTS answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    player_name VARCHAR(255) NOT NULL,
    player_api_id INTEGER,
    statistic_value INTEGER NOT NULL,
    is_valid_darts_score BOOLEAN NOT NULL,
    is_bust BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_answers_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_answers_player_name_trgm ON answers USING gin(player_name gin_trgm_ops);

-- ========================================
-- Scrape Jobs Table (Audit Log)
-- ========================================
CREATE TABLE IF NOT EXISTS scrape_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    season VARCHAR(20),
    league VARCHAR(100),
    question_id BIGINT,
    status VARCHAR(20) NOT NULL,
    rows_inserted INTEGER DEFAULT 0,
    rows_updated INTEGER DEFAULT 0,
    rows_deleted INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_scrape_jobs_started_at ON scrape_jobs(started_at DESC);

-- ========================================
-- Sample Test Questions
-- ========================================
INSERT INTO questions (text, league, season, team, stat_type, status)
VALUES
    ('Appearances for Manchester City in Premier League 2023-24',
     'England Premier League',
     '2023-2024',
     'Manchester City',
     'appearances',
     'active'),

    ('Appearances + Goals for Liverpool in Premier League 2023-24',
     'England Premier League',
     '2023-2024',
     'Liverpool',
     'combined_apps_goals',
     'active'),

    ('Appearances for Arsenal in Premier League 2023-24',
     'England Premier League',
     '2023-2024',
     'Arsenal',
     'appearances',
     'active')
ON CONFLICT DO NOTHING;

-- ========================================
-- Verification Queries
-- ========================================

-- Show all tables
SELECT
    schemaname,
    tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- Show sample questions
SELECT
    id,
    text,
    league,
    season,
    team,
    stat_type,
    status
FROM questions
ORDER BY id;

-- Database ready message
SELECT 'Database initialized successfully!' as status;
