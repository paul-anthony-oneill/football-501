-- V26: Seed four combined-stat question templates.
-- Adds goals_assists / goals_appearances / assists_appearances / goals_assists_appearances
-- variants for the 'football.team_competition_metric_since' materializer.
-- The generator (POST /api/admin/templates/generate) will create draft questions
-- once this migration has run.

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
    SELECT id INTO football_category_id
      FROM categories
     WHERE slug = 'football'
     LIMIT 1;

    IF football_category_id IS NULL THEN
        RAISE EXCEPTION 'Football category not found. Ensure V2 migration ran successfully.';
    END IF;

    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_goals_assists_since',
            'Goals + Assists for team in competition since year',
            'Goals + Assists for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'goals_assists',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_goals_appearances_since',
            'Goals + Appearances for team in competition since year',
            'Goals + Appearances for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'goals_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_assists_appearances_since',
            'Assists + Appearances for team in competition since year',
            'Assists + Appearances for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'assists_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    INSERT INTO question_templates
        (category_id, slug, display_name, text_template, param_schema, materializer_key, metric_key, default_min_score, is_active)
    VALUES
        (
            football_category_id,
            'team_competition_goals_assists_appearances_since',
            'Goals + Assists + Appearances for team in competition since year',
            'Goals + Assists + Appearances for {team_name} in {competition_name} since {start_year}',
            param_schema_json,
            'football.team_competition_metric_since',
            'goals_assists_appearances',
            1,
            TRUE
        )
    ON CONFLICT (slug) DO NOTHING;

    RAISE NOTICE 'V26: combined-stat question_templates seeded for category %', football_category_id;
END;
$$;
