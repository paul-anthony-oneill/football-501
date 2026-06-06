-- V31: Activate geography and film questions seeded as 'draft' by V21/V22.
--
-- V21 (SeedGeographyCategory) and V22 (SeedFilmCategory) insert questions with
-- status = 'draft' and total_valid_count = 0. This migration:
--   1. Backfills total_valid_count from the answers table (needed for the
--      question picker's minAnswers >= 10 filter in findRandomActiveQuestion).
--   2. Promotes status to 'active'.

UPDATE questions q
SET    total_valid_count = (
           SELECT COUNT(*)
           FROM   answers a
           WHERE  a.question_id = q.id
             AND  a.is_valid_darts = true
       )
WHERE  status = 'draft'
  AND  category_id IN (
           SELECT id FROM categories WHERE slug IN ('geography', 'film')
       );

UPDATE questions
SET    status = 'active'
WHERE  status = 'draft'
  AND  category_id IN (
           SELECT id FROM categories WHERE slug IN ('geography', 'film')
       );
