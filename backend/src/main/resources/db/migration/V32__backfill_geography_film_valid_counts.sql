-- V32: Backfill total_valid_count for geography and film questions.
-- V31 activated these questions but their total_valid_count was seeded as 0
-- by V21/V22, causing the question picker's minAnswers >= 10 filter to exclude
-- them. This recalculates the count from the answers table.

UPDATE questions q
SET    total_valid_count = (
           SELECT COUNT(*)
           FROM   answers a
           WHERE  a.question_id = q.id
             AND  a.is_valid_darts = true
       )
WHERE  category_id IN (
           SELECT id FROM categories WHERE slug IN ('geography', 'film')
       )
  AND  total_valid_count = 0;
