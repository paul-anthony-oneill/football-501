-- Migration V3: Add Match, Game, and GameMove tables for gameplay
-- Created: 2026-01-27

-- ========================================
-- 1. Matches Table
-- ========================================
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    player1_id UUID NOT NULL,
    player2_id UUID, -- Can be null for practice mode

    type VARCHAR(50) NOT NULL, -- CASUAL, RANKED, DAILY_CHALLENGE
    format VARCHAR(50) NOT NULL, -- BEST_OF_1, BEST_OF_3, BEST_OF_5
    status VARCHAR(50) NOT NULL, -- WAITING, IN_PROGRESS, COMPLETED, ABANDONED

    winner_id UUID,
    player1_games_won INTEGER NOT NULL DEFAULT 0,
    player2_games_won INTEGER NOT NULL DEFAULT 0,

    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,

    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matches_player1 ON matches (player1_id);
CREATE INDEX idx_matches_player2 ON matches (player2_id);
CREATE INDEX idx_matches_status ON matches (status);

-- ========================================
-- 2. Games Table
-- ========================================
CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    game_number INTEGER NOT NULL,

    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,

    status VARCHAR(50) NOT NULL, -- WAITING, IN_PROGRESS, COMPLETED, ABANDONED

    current_turn_player_id UUID,

    player1_score INTEGER NOT NULL DEFAULT 501,
    player2_score INTEGER NOT NULL DEFAULT 501,

    player1_consecutive_timeouts INTEGER NOT NULL DEFAULT 0,
    player2_consecutive_timeouts INTEGER NOT NULL DEFAULT 0,

    winner_id UUID,

    turn_count INTEGER NOT NULL DEFAULT 0,
    turn_timer_seconds INTEGER NOT NULL DEFAULT 45,

    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_games_match ON games (match_id);
CREATE INDEX idx_games_status ON games (status);

-- ========================================
-- 3. Game Moves Table
-- ========================================
CREATE TABLE game_moves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    player_id UUID NOT NULL,

    move_number INTEGER NOT NULL,

    submitted_answer VARCHAR(255) NOT NULL,
    matched_answer_id UUID REFERENCES answers(id) ON DELETE SET NULL,
    matched_display_text VARCHAR(255),

    result VARCHAR(50) NOT NULL, -- VALID, BUST, INVALID, TIMEOUT, CHECKOUT

    score_value INTEGER,
    score_before INTEGER NOT NULL,
    score_after INTEGER NOT NULL,

    is_timeout BOOLEAN NOT NULL DEFAULT FALSE,
    time_taken_seconds INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_moves_game ON game_moves (game_id);
CREATE INDEX idx_game_moves_player ON game_moves (player_id);

COMMENT ON TABLE matches IS 'Matches between players (best-of-N games)';
COMMENT ON TABLE games IS 'Individual games within a match (501 to 0)';
COMMENT ON TABLE game_moves IS 'Turn-by-turn history of player moves';
