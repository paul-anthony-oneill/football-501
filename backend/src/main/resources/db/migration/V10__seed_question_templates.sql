-- Migration V10: Seed question_templates + missing Top-5 league competitions
-- Created: 2026-05-25
--
-- Changes:
--   1. Adds missing Top-5 European league rows to competitions (if not present).
--   2. Seeds 3 question_template rows (goals, appearances, assists) for the
--      'team_competition_metric_since' template shape.
--   3. Sets tier = 1 for all five domestic_league rows.
--
-- The generator job (QuestionGeneratorService) reads these templates and
-- creates draft questions; no question rows are inserted here.

-- ========================================
-- 1. Ensure Top-5 leagues exist in competitions
-- ========================================

INSERT INTO competitions (name, normalized_name, competition_type, country, display_name, tier)
VALUES
    ('La Liga',    'la liga',    'domestic_league', 'Spain',   'La Liga',     1),
    ('Serie A',    'serie a',    'domestic_league', 'Italy',   'Serie A',     1),
    ('Bundesliga', 'bundesliga', 'domestic_league', 'Germany', 'Bundesliga',  1),
    ('Ligue 1',    'ligue 1',    'domestic_league', 'France',  'Ligue 1',     1)
ON CONFLICT (name, competition_type, COALESCE(country, '')) DO NOTHING;

-- Set tier = 1 on the Premier League row (added in V1 without a tier value).
UPDATE competitions
   SET tier = 1
 WHERE name = 'Premier League'
   AND competition_type = 'domestic_league';

-- Set tier = 1 on any other domestic_league rows that are missing it.
UPDATE competitions
   SET tier = 1
 WHERE competition_type = 'domestic_league'
   AND tier IS NULL;

-- ========================================
-- 2. Seed question_templates
-- ========================================
-- Materializer key: football.team_competition_metric_since
--
-- param_schema structure:
-- {
--   "params": {
--     "team_id":        { "type": "team_ref" },
--     "competition_id": {
--       "type": "competition_ref",
--       "competition_types": ["domestic_league"],
--       "top_flight_only": true
--     },
--     "start_year": { "type": "int", "values": [2000] }
--   }
-- }
--
-- The generator enumerates all (team, competition) pairs from player_season_stints
-- and inserts one draft question per pair.

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
    -- Resolve the football category ID (seeded in V2; slug = 'football').
    SELECT id INTO football_category_id
      FROM categories
     WHERE slug = 'football'
     LIMIT 1;

    IF football_category_id IS NULL THEN
        RAISE EXCEPTION 'Football category not found. Ensure V2 migration ran successfully.';
    END IF;

    -- Goals template
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_goals_since',
            'Goals for team in competition since year',
            'Goals for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'goals',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- Appearances template
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_appearances_since',
            'Appearances for team in competition since year',
            'Appearances for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    -- Assists template
    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_assists_since',
            'Assists for team in competition since year',
            'Assists for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'assists',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    RAISE NOTICE 'V10: question_templates seeded for category %', football_category_id;
END;
$$;

COMMENT ON TABLE question_templates IS
    'Template definitions for auto-generated questions. V10 seeds 3 football templates. See QuestionGeneratorService.';
