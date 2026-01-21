-- Migration V2: Refactor to Domain-Agnostic Game Schema
-- Created: 2026-01-21

-- ========================================
-- 1. Categories Table
-- ========================================
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE, -- e.g., 'Football', 'Geography'
    slug VARCHAR(255) NOT NULL UNIQUE, -- e.g., 'football', 'geography'
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Seed initial category
INSERT INTO categories (name, slug, description)
VALUES ('Football', 'football', 'Standard Football 501 Game Mode');

-- ========================================
-- 2. Refactor Questions Table
-- ========================================
-- We modify the existing table rather than dropping it to preserve IDs if possible,
-- but given the massive structural change, a recreation is cleaner.
-- However, existing foreign keys in 'question_valid_answers' would block DROP.
-- Since we are replacing 'question_valid_answers' anyway, we can drop dependencies first.

DROP TABLE IF EXISTS question_valid_answers;
DROP TABLE IF EXISTS questions;

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    
    question_text TEXT NOT NULL,
    
    -- The key metric this question asks for (e.g., 'goals', 'population')
    metric_key VARCHAR(50) NOT NULL,
    
    -- JSONB configuration for the data source (Python script uses this)
    -- Football Example: {"stat_type": "goals", "competition_id": "...", "season": "23/24"}
    -- Geo Example: {"entity_type": "city", "min_population": 1000000}
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    min_score INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_questions_category ON questions (category_id);
CREATE INDEX idx_questions_active ON questions (is_active);

COMMENT ON TABLE questions IS 'Domain-agnostic question definitions';
COMMENT ON COLUMN questions.config IS 'Dynamic configuration used by ETL scripts to determine the query';

-- ========================================
-- 3. Answers Table (The "Truth" Store)
-- ========================================
-- Replaces question_valid_answers.
-- Totally decoupled from 'players' table.

CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    
    -- Normalized key for matching (e.g., "erling haaland")
    answer_key VARCHAR(255) NOT NULL,
    
    -- Display text for UI (e.g., "Erling Haaland")
    display_text VARCHAR(255) NOT NULL,
    
    -- The generic score value (e.g., 35)
    score INTEGER NOT NULL,
    
    -- Pre-computed game rules
    is_valid_darts BOOLEAN NOT NULL, -- Is 1-180 and not invalid checkout?
    is_bust BOOLEAN NOT NULL,        -- Is > 180?
    
    -- Optional metadata for context/UI (e.g., {"team": "Man City", "player_id": "..."})
    metadata JSONB DEFAULT '{}'::jsonb,
    
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_answers_question_key ON answers (question_id, answer_key);
CREATE INDEX idx_answers_question_score ON answers (question_id, score DESC);
-- Trigram index for fuzzy matching on the answer key
CREATE INDEX idx_answers_key_trgm ON answers USING GIN (answer_key gin_trgm_ops);

COMMENT ON TABLE answers IS 'Materialized valid answers for active questions. Source of truth for the Game Engine.';

-- ========================================
-- 4. Cleanup
-- ========================================
-- 'players', 'teams', 'competitions' tables remain as the "Source Layer"
-- but are no longer referenced by Game tables.
