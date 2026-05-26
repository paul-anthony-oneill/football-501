-- Migration V11: Seed per-season question_templates
-- Created: 2026-05-25
--
-- Adds 4 question_template rows for the 'football.team_competition_season_metric'
-- materializer shape.  Unlike the V10 "since year" templates, these produce
-- questions scoped to a SINGLE season:
--
--   "Goals for Arsenal in the Premier League 2023-24"
--   "Appearances for Real Madrid in the Champions League 2018-19"
--
-- The param_schema allows all three main competition types so that Champions
-- League and domestic cup questions are generated automatically once scrape
-- data is loaded into player_season_stints for those competitions.
--
-- Materializer key: football.team_competition_season_metric
-- Registered in:    FootballTeamCompetitionSeasonMaterializer.java
--
-- param_schema fields:
--   team_id        — team_ref (UUID of a teams row)
--   competition_id — competition_ref; types: domestic_league, domestic_cup,
--                    continental_club (no top_flight_only restriction — data
--                    availability acts as the natural filter)
--   season_id      — season_ref (UUID of a seasons row)
--
-- The generator also stores denormalised display values in template_params:
--   team_name, competition_name, season_label — used when rendering question text.

DO $$
DECLARE
    football_category_id UUID;
    param_schema_json    JSONB := '{
      "params": {
        "team_id": {
          "type": "team_ref"
        },
        "competition_id": {
          "type": "competition_ref",
          "competition_types": ["domestic_league", "domestic_cup", "continental_club"]
        },
        "season_id": {
          "type": "season_ref"
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

    -- ── Goals ────────────────────────────────────────────────────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_season_goals',
            'Goals for team in competition (single season)',
            'Goals for {team_name} in {competition_name} {season_label}',
            param_schema_json,
            'football.team_competition_season_metric',
            'goals',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── Appearances ──────────────────────────────────────────────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_season_appearances',
            'Appearances for team in competition (single season)',
            'Appearances for {team_name} in {competition_name} {season_label}',
            param_schema_json,
            'football.team_competition_season_metric',
            'appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── Assists ──────────────────────────────────────────────────────────────
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_season_assists',
            'Assists for team in competition (single season)',
            'Assists for {team_name} in {competition_name} {season_label}',
            param_schema_json,
            'football.team_competition_season_metric',
            'assists',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- ── Clean sheets ─────────────────────────────────────────────────────────
    -- Answers are per goalkeeper; the score is the number of clean sheets kept.
    -- Questions of this shape: "Clean sheets for Chelsea in the Premier League 2004-05"
    -- Players name a goalkeeper; the GK's clean-sheet count in that season becomes
    -- their darts score.
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema,
         materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_season_clean_sheets',
            'Clean sheets for team in competition (single season)',
            'Clean sheets for {team_name} in {competition_name} {season_label}',
            param_schema_json,
            'football.team_competition_season_metric',
            'clean_sheets',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    RAISE NOTICE 'V11: per-season question_templates seeded for category %', football_category_id;
END;
$$;

COMMENT ON TABLE question_templates IS
    'Template definitions for auto-generated questions. '
    'V10 seeds 3 "since year" football templates; '
    'V11 seeds 4 per-season football templates. '
    'See QuestionGeneratorService + FootballTeamCompetitionSeasonMaterializer.';
