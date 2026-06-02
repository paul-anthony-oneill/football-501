-- V17: Re-seed test data with enough valid answers (≥15) to satisfy viability.
-- Wipes any stale question + answers from V15 and inserts fresh.

DO $$
DECLARE
  v_category_id UUID;
  v_question_id UUID;
BEGIN
  -- Ensure the test category exists
  SELECT id INTO v_category_id FROM categories WHERE slug = 'test';
  IF v_category_id IS NULL THEN
    INSERT INTO categories (id, name, slug, description, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Test Mode', 'test',
            'Synthetic answers for frontend flow verification', NOW(), NOW())
    RETURNING id INTO v_category_id;
  END IF;

  -- Wipe old test question(s) and their answers for a clean slate
  DELETE FROM questions WHERE category_id = v_category_id;

  INSERT INTO questions (
    id, category_id, question_text, metric_key, config,
    difficulty, status,
    high_value_count, mid_range_count, checkout_count,
    total_valid_count, total_score_pool,
    single_question_viable,
    difficulty_score,
    created_at, updated_at
  )
  VALUES (
    gen_random_uuid(), v_category_id,
    'Test Question: Frontend Flow Verification',
    'test', '{"entity_type": "footballer"}'::jsonb,
    2, 'active',
    9, 3, 3, 15, 1486,
    true,
    5.0,
    NOW(), NOW()
  )
  RETURNING id INTO v_question_id;

  INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, materialized_at, created_at)
  VALUES
    (gen_random_uuid(), v_question_id, 'max 180 a',          'Max 180 A',          180, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'max 180 b',          'Max 180 B',          180, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'max 180 c',          'Max 180 C',          180, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'bust over 180',      'Bust Over 180',      181, false, true,  NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'bust invalid darts', 'Bust Invalid Darts', 179, false, true,  NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'checkout perfect',   'Checkout Perfect',   141, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'checkout range',     'Checkout Range',     150, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'bust too low',       'Bust Too Low',       160, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 140',         'Filler 140',         140, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 120',         'Filler 120',         120, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 100',         'Filler 100',         100, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 60',          'Filler 60',           60, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 40',          'Filler 40',           40, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 20',          'Filler 20',           20, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 8',           'Filler 8',             8, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 5',           'Filler 5',             5, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 2',           'Filler 2',             2, true,  false, NOW(), NOW());
END $$;
