-- Mark viable easy/medium questions as suitable for the Daily Challenge pool.
-- Criteria: single_question_viable = true, difficulty_score between 0.0 and 5.5,
-- and total_score_pool >= 101 (minimum reasonable starting score).
UPDATE questions
   SET suitable_for_daily = true
 WHERE status                = 'active'
   AND single_question_viable = true
   AND difficulty_score       BETWEEN 0.0 AND 5.5
   AND total_score_pool       >= 101
   AND suitable_for_daily     = false;

-- Also ensure the test questions are included so the frontend flow works in dev.
UPDATE questions
   SET suitable_for_daily = true
 WHERE status = 'active'
   AND (question_text ILIKE '%test question%' OR question_text ILIKE '%frontend%')
   AND suitable_for_daily = false;
