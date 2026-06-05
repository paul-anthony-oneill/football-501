-- V31: Activate geography and film questions seeded as 'draft' by V21/V22.
-- V21 (SeedGeographyCategory) and V22 (SeedFilmCategory) both insert questions
-- with status = 'draft'. This migration promotes them to 'active' so they are
-- visible to the question picker during gameplay.

UPDATE questions
SET    status = 'active'
WHERE  status = 'draft'
  AND  category_id IN (
      SELECT id FROM categories WHERE slug IN ('geography', 'film')
  );
