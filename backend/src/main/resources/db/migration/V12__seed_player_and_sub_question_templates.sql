-- Migration V12: Seed player-scoped and substitute-appearance question templates
-- Created: 2026-05-25
--
-- Adds 5 new question_template rows:
--
-- Player-in-league templates (materializer: football.player_competition_metric_since)
--   1. player_competition_goals_since         — Goals in {competition} since {year}
--   2. player_competition_goals_assists_since — Goals + Assists in {competition} since {year}
--   3. player_competition_sub_appearances_since — Substitute appearances in {competition} since {year}
--
-- Career totals templates (materializer: football.player_career_metric)
--   4. player_career_goals_since             — Career goals in top-flight football since {year}
--
-- Club substitute-appearances template (extends existing team materializer)
--   5. team_competition_sub_appearances_since — Substitute appearances for {team} in {competition} since {year}
--
-- No question rows are created here — the QuestionGeneratorService enumerates
-- param sets from these templates and inserts draft questions.

DO $$
DECLARE
    football_category_id UUID;

    -- Shared param schema for player-in-competition templates.
    -- One question is generated per competition that has stint data.
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

    -- Param schema for career-total templates.
    -- Produces exactly one question (no per-competition loop).
    career_schema JSONB := '{
      "params": {
        "competition_types": ["domestic_league"],
        "top_flight_only": true,
        "start_year": {
          "type": "int",
          "values": [2000]
        }
      }
    }'::jsonb;

    -- Param schema for club-level templates (reuses the existing team materializer).
    team_comp_schema JSONB := '{
      "params": {
        "team_id": {
          "type": "team_ref"
        },
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

    -- ── 1. Goals in {competition} since {year} ───────────────────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_goals_since',
            'Goals in competition since year (all players)',
            'Goals in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'goals',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── 2. Goals + Assists in {competition} since {year} ────────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_goals_assists_since',
            'Goals and assists in competition since year (all players)',
            'Goals + Assists in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'goals_assists',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── 3. Substitute appearances in {competition} since {year} ─────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_competition_sub_appearances_since',
            'Substitute appearances in competition since year (all players)',
            'Substitute appearances in {competition_name} since {start_year}',
            player_comp_schema,
            'football.player_competition_metric_since',
            'sub_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── 4. Career goals in top-flight football since {year} ─────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'player_career_goals_since',
            'Career goals in top-flight football since year',
            'Career goals in top-flight football since {start_year}',
            career_schema,
            'football.player_career_metric',
            'goals',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── 5. Substitute appearances for {team} in {competition} since {year} ──
    --   Uses the existing team materializer — no new Java code needed.
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_sub_appearances_since',
            'Substitute appearances for team in competition since year',
            'Substitute appearances for {team_name} in {competition_name} since {start_year}',
            team_comp_schema,
            'football.team_competition_metric_since',
            'sub_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    RAISE NOTICE 'V12: 5 question templates seeded for category %', football_category_id;
END;
$$;
