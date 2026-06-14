-- V35: Activate geography and film questions currently in 'excluded' status.
-- V31 targeted status='draft' but these questions were already 'excluded',
-- so the WHERE clause never matched. Answers and valid counts are correct
-- (confirmed by V34 diagnostic). This migration only updates the status.

UPDATE questions
SET    status = 'active'
WHERE  status = 'excluded'
  AND  category_id IN (
           SELECT id FROM categories WHERE slug IN ('geography', 'film')
       );
