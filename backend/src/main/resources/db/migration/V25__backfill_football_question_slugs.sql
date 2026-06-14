-- V25: Backfill structured football lookup columns (q_scope, q_league, q_club, q_stat)
-- on existing template-generated football questions.
--
-- Targets only questions produced by the 'football.team_competition_metric_since'
-- materializer that do not yet have q_scope populated — idempotent, safe to re-run.
--
-- Slug derivation: spaces in normalized_name are replaced with hyphens
-- (e.g. "premier league" → "premier-league"; "manchester city" → "manchester-city").
-- normalized_name is already lowercase so no LOWER() needed.

UPDATE questions
SET
    q_scope  = 'club',
    q_league = REPLACE(c.normalized_name, ' ', '-'),
    q_club   = REPLACE(t.normalized_name, ' ', '-'),
    q_stat   = questions.metric_key
FROM
    question_templates qt,
    teams              t,
    competitions       c
WHERE
    questions.template_id                              = qt.id
    AND qt.materializer_key                            = 'football.team_competition_metric_since'
    AND questions.template_params->>'team_id'          = t.id::text
    AND questions.template_params->>'competition_id'   = c.id::text
    AND questions.q_scope IS NULL;
