-- Migration V15: Test Flow Verification Question
-- Seeds a synthetic question + answers for frontend flow testing.

DO $$
DECLARE
  v_category_id UUID;
  v_question_id UUID;
BEGIN
  INSERT INTO categories (id, name, slug, description, created_at, updated_at)
  VALUES (gen_random_uuid(), 'Test Mode', 'test',
          'Synthetic answers for frontend flow verification', NOW(), NOW())
  RETURNING id INTO v_category_id;

  INSERT INTO questions (
    id, category_id, question_text, metric_key, config,
    difficulty, status, total_valid_count, single_question_viable,
    created_at, updated_at
  )
  VALUES (
    gen_random_uuid(), v_category_id,
    'Test Question: Frontend Flow Verification',
    'test', '{"entity_type": "footballer"}'::jsonb,
    2, 'active', 11, true,
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
    (gen_random_uuid(), v_question_id, 'filler 100',         'Filler 100',         100, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 60',          'Filler 60',           60, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 40',          'Filler 40',           40, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 20',          'Filler 20',           20, true,  false, NOW(), NOW()),
    (gen_random_uuid(), v_question_id, 'filler 5',           'Filler 5',             5, true,  false, NOW(), NOW());
END $$;
