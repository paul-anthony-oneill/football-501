-- Football structured question lookup columns.
-- Enables exact-match queries for "Goals for Arsenal in the Premier League since 2000"
-- without JSONB parsing. Existing questions leave these columns NULL and are unaffected.

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS q_scope  VARCHAR(20),   -- 'league' | 'club'
    ADD COLUMN IF NOT EXISTS q_league VARCHAR(50),   -- e.g. 'premier-league'
    ADD COLUMN IF NOT EXISTS q_club   VARCHAR(50),   -- e.g. 'arsenal'; NULL for league-scope questions
    ADD COLUMN IF NOT EXISTS q_stat   VARCHAR(50);   -- 'goals' | 'assists' | 'appearances'
                                                     -- | 'goals_assists' | 'goals_appearances'
                                                     -- | 'assists_appearances' | 'goals_assists_appearances'

-- One canonical question per (scope, league, club, stat).
-- COALESCE on q_club ensures league-scope rows (NULL club) also satisfy the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS uq_questions_football_template
    ON questions (q_scope, q_league, COALESCE(q_club, ''), q_stat)
    WHERE q_scope IS NOT NULL;

-- Fast range scans when selecting by league (e.g. "random club in Premier League").
CREATE INDEX IF NOT EXISTS idx_questions_q_scope_league
    ON questions (q_scope, q_league)
    WHERE q_scope IS NOT NULL;
