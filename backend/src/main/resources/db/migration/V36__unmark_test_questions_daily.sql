-- Revert the V20 mistake that marked test-category questions as
-- suitable_for_daily.  The test category exists for development only.
UPDATE questions
   SET suitable_for_daily = false
 WHERE category_id = (SELECT id FROM categories WHERE slug = 'test')
   AND suitable_for_daily = true;
