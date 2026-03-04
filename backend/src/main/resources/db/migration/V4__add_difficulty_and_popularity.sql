-- Migration V4: Add Difficulty and Popularity Rankings
-- Created: 2026-03-04

-- 1. Add popularity_rank to teams
ALTER TABLE teams ADD COLUMN popularity_rank INTEGER DEFAULT 10;
COMMENT ON COLUMN teams.popularity_rank IS 'Team popularity ranking: 1 (Very Popular/Global) to 10 (Niche/Local).';

-- 2. Add difficulty to questions
ALTER TABLE questions ADD COLUMN difficulty INTEGER DEFAULT 2;
COMMENT ON COLUMN questions.difficulty IS 'Question difficulty: 1 (Easy), 2 (Medium), 3 (Hard).';

-- 3. Add difficulty to matches
ALTER TABLE matches ADD COLUMN difficulty INTEGER DEFAULT 2;
COMMENT ON COLUMN matches.difficulty IS 'Match difficulty level: 1 (Easy), 2 (Medium), 3 (Hard).';

-- 4. Seed some popular teams with high rankings (Rank 1)
-- Big Six Premier League
UPDATE teams SET popularity_rank = 1 WHERE normalized_name IN (
    'manchester city', 'liverpool', 'arsenal', 'manchester united', 'chelsea', 'tottenham hotspur'
);

-- Established/Popular PL Clubs
UPDATE teams SET popularity_rank = 2 WHERE normalized_name IN (
    'aston villa', 'newcastle united', 'west ham united', 'everton', 'leicester city'
);

-- Note: All other teams default to 10 (Niche/Lower League/Historical)
