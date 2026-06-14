-- V28: Remove per-season football questions and fix league-level question metadata.
--
-- Rationale: per-season questions (e.g. "Goals for Arsenal in the Premier League 2023-24")
-- are not in scope — all football questions should cover career stats since 2000.
-- League-level questions from V12 already exist but have NULL q_scope/q_league/q_stat,
-- making them invisible to the football filter query methods added in V24/V25.
--
-- Changes:
--   1. Remove dependent rows for per-season (V11) questions, then delete the questions
--   2. Deactivate V11 templates to prevent re-generation
--   3. Backfill q_scope/q_league/q_stat on league-level questions (football.player_competition_metric_since)
--   4. Set q_scope = 'career' on career questions (football.player_career_metric)

-- ── 1a. Remove daily_challenges referencing season questions ──────────────────
--        FK is ON DELETE RESTRICT so must be removed before the questions.
DELETE FROM daily_challenges
WHERE question_id IN (
    SELECT q.id
    FROM questions q
    JOIN question_templates qt ON q.template_id = qt.id
    WHERE qt.materializer_key = 'football.team_competition_season_metric'
);

-- ── 1b. Remove games referencing season questions ─────────────────────────────
--        game_moves CASCADE from games so no separate delete needed there.
DELETE FROM games
WHERE question_id IN (
    SELECT q.id
    FROM questions q
    JOIN question_templates qt ON q.template_id = qt.id
    WHERE qt.materializer_key = 'football.team_competition_season_metric'
);

-- ── 1c. Remove answers for season questions ───────────────────────────────────
DELETE FROM answers
WHERE question_id IN (
    SELECT q.id
    FROM questions q
    JOIN question_templates qt ON q.template_id = qt.id
    WHERE qt.materializer_key = 'football.team_competition_season_metric'
);

-- ── 1d. Delete the season questions themselves ────────────────────────────────
DELETE FROM questions
WHERE template_id IN (
    SELECT id FROM question_templates
    WHERE materializer_key = 'football.team_competition_season_metric'
);

-- ── 2. Deactivate V11 templates ───────────────────────────────────────────────
--       Prevents QuestionGeneratorService from ever recreating these questions.
UPDATE question_templates
SET is_active = false
WHERE materializer_key = 'football.team_competition_season_metric';

-- ── 3. Backfill q_scope/q_league/q_stat on league-level questions ─────────────
--
-- V12 seeded three league-level templates (goals, goals_assists, sub_appearances)
-- using the football.player_competition_metric_since materializer. Their questions
-- exist in the DB but were not touched by the V25 club-scope backfill.
-- Without q_scope set, findFootballLeagueQuestion() and findRandomFootballLeagueQuestion()
-- return nothing for these questions.
--
-- q_league is derived from the competition's normalized_name (spaces → hyphens),
-- matching the convention established by V25 for club-scope questions.
-- Idempotent: WHERE q_scope IS NULL guard prevents double-application.

UPDATE questions
SET
    q_scope  = 'league',
    q_league = REPLACE(c.normalized_name, ' ', '-'),
    q_club   = NULL,
    q_stat   = questions.metric_key
FROM question_templates qt,
     competitions c
WHERE questions.template_id                            = qt.id
  AND qt.materializer_key                              = 'football.player_competition_metric_since'
  AND (questions.template_params->>'competition_id')   = c.id::text
  AND questions.q_scope IS NULL;

-- ── 4. Set q_scope = 'career' on career questions ─────────────────────────────
--
-- Career questions (football.player_career_metric) span all leagues so they have
-- no competition_id in template_params and therefore no q_league. Giving them a
-- distinct scope keeps them out of the league/club filter pools while making them
-- eligible for the random_any (q_scope IS NOT NULL) query.

UPDATE questions
SET
    q_scope = 'career',
    q_club  = NULL,
    q_stat  = questions.metric_key
FROM question_templates qt
WHERE questions.template_id = qt.id
  AND qt.materializer_key   = 'football.player_career_metric'
  AND questions.q_scope IS NULL;
