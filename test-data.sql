-- Quick test data for Football 501
-- This adds a sample question with test answers so you can play immediately

-- Get the football category ID (should exist from migrations)
DO $$
DECLARE
    football_category_id UUID;
    test_question_id UUID;
BEGIN
    -- Get category ID
    SELECT id INTO football_category_id FROM categories WHERE slug = 'football';

    -- Insert a test question
    INSERT INTO questions (id, category_id, question_text, metric_key, config, is_active)
    VALUES (
        gen_random_uuid(),
        football_category_id,
        'Test Question: Premier League Appearances (use player names like Player1, Player2, etc.)',
        'appearances',
        '{"test": true}'::jsonb,
        true
    )
    RETURNING id INTO test_question_id;

    -- Insert test answers with various scores
    INSERT INTO answers (question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata) VALUES
    (test_question_id, 'player1', 'Player 1', 35, true, false, '{}'::jsonb),
    (test_question_id, 'player2', 'Player 2', 30, true, false, '{}'::jsonb),
    (test_question_id, 'player3', 'Player 3', 25, true, false, '{}'::jsonb),
    (test_question_id, 'player4', 'Player 4', 20, true, false, '{}'::jsonb),
    (test_question_id, 'player5', 'Player 5', 15, true, false, '{}'::jsonb),
    (test_question_id, 'player10', 'Player 10', 10, true, false, '{}'::jsonb),
    (test_question_id, 'player50', 'Player 50', 50, true, false, '{}'::jsonb),
    (test_question_id, 'player100', 'Player 100', 100, true, false, '{}'::jsonb),
    (test_question_id, 'player179', 'Player 179', 179, false, true, '{}'::jsonb), -- Invalid darts score
    (test_question_id, 'player180', 'Player 180', 180, true, false, '{}'::jsonb),
    (test_question_id, 'player5a', 'Player 5A', 5, true, false, '{}'::jsonb),
    (test_question_id, 'player1a', 'Player 1A', 1, true, false, '{}'::jsonb),
    (test_question_id, 'erling haaland', 'Erling Haaland', 36, true, false, '{}'::jsonb),
    (test_question_id, 'phil foden', 'Phil Foden', 34, true, false, '{}'::jsonb),
    (test_question_id, 'kevin de bruyne', 'Kevin De Bruyne', 18, true, false, '{}'::jsonb),
    (test_question_id, 'bernardo silva', 'Bernardo Silva', 33, true, false, '{}'::jsonb),
    (test_question_id, 'julian alvarez', 'Julian Alvarez', 31, true, false, '{}'::jsonb),
    (test_question_id, 'rodri', 'Rodri', 34, true, false, '{}'::jsonb),
    (test_question_id, 'kyle walker', 'Kyle Walker', 28, true, false, '{}'::jsonb),
    (test_question_id, 'john stones', 'John Stones', 16, true, false, '{}'::jsonb);

    RAISE NOTICE 'Test data inserted successfully! Question ID: %', test_question_id;
END $$;

-- Verify the data
SELECT
    q.question_text,
    COUNT(a.id) as answer_count
FROM questions q
LEFT JOIN answers a ON a.question_id = q.id
WHERE q.is_active = true
GROUP BY q.id, q.question_text;
