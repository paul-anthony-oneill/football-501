-- V30: League-scope Appearances, Goals+Appearances, Assists+Appearances questions.
--
-- V12 seeded league-scope templates for goals, goals_assists, and sub_appearances.
-- This migration adds the three that were missing:
--   appearances          — Appearances in {competition} since 2000
--   goals_appearances    — Goals + Appearances in {competition} since 2000
--   assists_appearances  — Assists + Appearances in {competition} since 2000
--
-- Idempotent throughout: ON CONFLICT DO NOTHING on templates and questions;
-- ON CONFLICT DO UPDATE on answers.


-- ── 1. Seed question templates ────────────────────────────────────────────────

DO $$
DECLARE
    football_category_id UUID;
    player_comp_schema JSONB := '{
      "params": {
        "competition_id": {
          "type": "competition_ref",
          "competition_types": ["domestic_league"],
          "top_flight_only": true
        },
        "start_year": {
          "type": "int",
          "values": [2000]
        }
      }
    }'::jsonb;
BEGIN
    SELECT id INTO football_category_id
      FROM categories
     WHERE slug = 'football'
     LIMIT 1;

    IF football_category_id IS NULL THEN
        RAISE EXCEPTION 'Football category not found. Ensure V2 migration ran successfully.';
    END IF;

    -- Appearances in {competition} since {year}
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_appearances_since',
            'Appearances in competition since year (all players)',
            'Appearances in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- Goals + Appearances in {competition} since {year}
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_goals_appearances_since',
            'Goals and appearances in competition since year (all players)',
            'Goals + Appearances in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'goals_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- Assists + Appearances in {competition} since {year}
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_assists_appearances_since',
            'Assists and appearances in competition since year (all players)',
            'Assists + Appearances in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'assists_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    RAISE NOTICE 'V30: 3 league-scope question templates seeded for category %', football_category_id;
END;
$$;


-- ── 2. Create draft questions ─────────────────────────────────────────────────
--
-- One question per (template × tier-1 domestic league).
-- The partial unique index uq_questions_football_template
--   (q_scope, q_league, COALESCE(q_club, ''), q_stat) WHERE q_scope IS NOT NULL
-- makes this idempotent via ON CONFLICT DO NOTHING.

INSERT INTO questions (
    id, category_id, question_text, metric_key, config,
    min_score, difficulty, status, template_id, template_params,
    high_value_count, mid_range_count, checkout_count,
    total_valid_count, total_score_pool,
    single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily,
    q_scope, q_league, q_club, q_stat,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    qt.category_id,
    REPLACE(
        REPLACE(qt.text_template, '{competition_name}', COALESCE(c.display_name, c.name)),
        '{start_year}', '2000'
    ),
    qt.metric_key,
    jsonb_build_object(
        'entity_type',      'footballer',
        'materializer_key', qt.materializer_key,
        'metric_key',       qt.metric_key,
        'competition_id',   c.id::text,
        'start_year',       2000
    ),
    1,       -- min_score
    2,       -- difficulty
    'draft', -- status
    qt.id,
    jsonb_build_object('competition_id', c.id::text, 'start_year', 2000),
    0, 0, 0, 0, 0,   -- zone counts / score pool
    TRUE,            -- single_question_viable (updated after materialization)
    5.0, FALSE, FALSE,
    'league',
    REPLACE(c.normalized_name, ' ', '-'),
    NULL,
    qt.metric_key,
    NOW(), NOW()
FROM question_templates qt
CROSS JOIN competitions c
WHERE qt.slug IN (
    'player_competition_appearances_since',
    'player_competition_goals_appearances_since',
    'player_competition_assists_appearances_since'
)
  AND c.competition_type = 'domestic_league'
  AND c.tier = 1
ON CONFLICT DO NOTHING;


-- ── 3. Materialize answers ────────────────────────────────────────────────────
--
-- Aggregates player stats across all clubs in each competition since 2000.
-- Handles three metric_keys: appearances, goals_appearances, assists_appearances.

WITH raw AS (
    SELECT
        q.id                   AS question_id,
        q.metric_key,
        p.normalized_name      AS answer_key,
        MIN(p.name)            AS display_text,
        SUM(pss.goals)         AS g,
        SUM(pss.assists)       AS a,
        SUM(pss.appearances)   AS app
    FROM questions q
    JOIN question_templates qt
        ON  q.template_id = qt.id
        AND qt.slug IN (
            'player_competition_appearances_since',
            'player_competition_goals_appearances_since',
            'player_competition_assists_appearances_since'
        )
    JOIN player_season_stints pss
        ON  pss.competition_id = (q.template_params->>'competition_id')::uuid
    JOIN seasons s
        ON  pss.season_id = s.id
        AND s.start_year >= COALESCE((q.template_params->>'start_year')::integer, 2000)
    JOIN players p
        ON  pss.player_id = p.id
    WHERE q.status = 'draft'
    GROUP BY q.id, q.metric_key, p.normalized_name
),
computed AS (
    SELECT
        question_id,
        answer_key,
        display_text,
        CAST(
            CASE metric_key
                WHEN 'appearances'         THEN app
                WHEN 'goals_appearances'   THEN g + app
                WHEN 'assists_appearances' THEN a + app
            END
        AS INTEGER) AS score
    FROM raw
    WHERE metric_key IN ('appearances', 'goals_appearances', 'assists_appearances')
),
final_answers AS (
    SELECT
        question_id,
        answer_key,
        display_text,
        score,
        (score BETWEEN 1 AND 180
            AND score NOT IN (163, 166, 169, 172, 173, 175, 176, 178, 179)) AS is_valid_darts,
        (score > 180)                                                         AS is_bust
    FROM computed
    WHERE score > 0
)
INSERT INTO answers
    (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust,
     metadata, created_at, materialized_at)
SELECT
    gen_random_uuid(),
    question_id,
    answer_key,
    display_text,
    score,
    is_valid_darts,
    is_bust,
    '{}'::jsonb,
    NOW(),
    NOW()
FROM final_answers
ON CONFLICT (question_id, answer_key) DO UPDATE SET
    score           = EXCLUDED.score,
    is_valid_darts  = EXCLUDED.is_valid_darts,
    is_bust         = EXCLUDED.is_bust,
    materialized_at = NOW();


-- ── 4. Update difficulty metrics and activate ─────────────────────────────────

WITH zone_counts AS (
    SELECT
        a.question_id,
        COUNT(*) FILTER (WHERE a.is_valid_darts AND a.score BETWEEN 100 AND 180) AS high_value_count,
        COUNT(*) FILTER (WHERE a.is_valid_darts AND a.score BETWEEN 20  AND 99)  AS mid_range_count,
        COUNT(*) FILTER (WHERE a.is_valid_darts AND a.score BETWEEN 1   AND 19)  AS checkout_count,
        COUNT(*) FILTER (WHERE a.is_valid_darts)                                  AS total_valid_count,
        COALESCE(SUM(a.score) FILTER (WHERE a.is_valid_darts), 0)                AS total_score_pool
    FROM answers a
    JOIN questions q ON a.question_id = q.id
    JOIN question_templates qt ON q.template_id = qt.id
    WHERE qt.slug IN (
        'player_competition_appearances_since',
        'player_competition_goals_appearances_since',
        'player_competition_assists_appearances_since'
    )
      AND q.status = 'draft'
    GROUP BY a.question_id
),
with_viability AS (
    SELECT *,
        (total_score_pool >= 501 AND total_valid_count >= 15) AS viable
    FROM zone_counts
),
with_ease AS (
    SELECT *,
        LEAST(high_value_count::float / 25.0, 1.0) * 0.50
      + LEAST(mid_range_count::float  / 40.0, 1.0) * 0.30
      + LEAST(checkout_count::float   / 12.0, 1.0) * 0.20  AS ease_score
    FROM with_viability
),
with_base AS (
    SELECT *,
        CASE WHEN checkout_count = 0
             THEN GREATEST(10.0 * (1.0 - ease_score), 7.0)
             ELSE 10.0 * (1.0 - ease_score)
        END AS base_difficulty
    FROM with_ease
),
final_metrics AS (
    SELECT
        question_id,
        high_value_count,
        mid_range_count,
        checkout_count,
        total_valid_count,
        total_score_pool,
        viable,
        GREATEST(0.0, LEAST(10.0,
            base_difficulty - LEAST(total_valid_count::float / 200.0, 1.0) * 1.5
        )) AS difficulty_score
    FROM with_base
)
UPDATE questions q SET
    high_value_count           = f.high_value_count,
    mid_range_count            = f.mid_range_count,
    checkout_count             = f.checkout_count,
    total_valid_count          = f.total_valid_count,
    total_score_pool           = f.total_score_pool,
    single_question_viable     = f.viable,
    viability_exclusion_reason = CASE
        WHEN f.viable THEN NULL
        ELSE CONCAT_WS('; ',
            CASE WHEN f.total_score_pool < 501
                 THEN 'insufficient_score_pool: ' || f.total_score_pool || ' < 501'
                 ELSE NULL END,
            CASE WHEN f.total_valid_count < 15
                 THEN 'insufficient_answer_count: ' || f.total_valid_count || ' < 15'
                 ELSE NULL END
        )
    END,
    difficulty_score           = f.difficulty_score,
    status                     = CASE WHEN f.viable THEN 'active' ELSE 'excluded' END,
    updated_at                 = NOW()
FROM final_metrics f
WHERE q.id = f.question_id;


-- ── 5. Ensure footballer entities exist for autocomplete ──────────────────────

INSERT INTO entities (entity_type, display_name, normalized_name)
SELECT DISTINCT
    'footballer',
    p.name,
    p.normalized_name
FROM answers a
JOIN questions q   ON a.question_id = q.id
JOIN question_templates qt ON q.template_id = qt.id
JOIN players p     ON p.normalized_name = a.answer_key
WHERE qt.slug IN (
    'player_competition_appearances_since',
    'player_competition_goals_appearances_since',
    'player_competition_assists_appearances_since'
)
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
