-- V34: Diagnostic — surfaces current state of geography/film questions.
-- Re-runs the total_valid_count backfill unconditionally (no WHERE filter on current value)
-- so even if V32/V33's count update failed, this fixes it.
-- Also logs the result via a DO block so we can see it in the migration output.

UPDATE questions q
SET    total_valid_count = (
           SELECT COUNT(*)
           FROM   answers a
           WHERE  a.question_id = q.id
             AND  a.is_valid_darts = true
       )
WHERE  category_id IN (
           SELECT id FROM categories WHERE slug IN ('geography', 'film')
       );

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT q.id, c.slug, q.status, q.total_valid_count, q.difficulty,
               (SELECT COUNT(*) FROM answers a WHERE a.question_id = q.id) AS answer_count
        FROM   questions q
        JOIN   categories c ON c.id = q.category_id
        WHERE  c.slug IN ('geography', 'film')
    LOOP
        RAISE NOTICE 'question % (%) status=% total_valid=% difficulty=% answers=%',
            r.id, r.slug, r.status, r.total_valid_count, r.difficulty, r.answer_count;
    END LOOP;
END $$;
