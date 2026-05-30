-- Backfill total_valid_count for questions where it was defaulted to 0 by V13.
-- V13 added the column with DEFAULT 0, so any question that was active before V13
-- ran will have total_valid_count = 0 even though answers exist.
--
-- After this migration, question-selection queries can safely filter on
-- total_valid_count >= N instead of running a correlated subquery against answers.

UPDATE questions q
SET total_valid_count = (
    SELECT COUNT(*) FROM answers a
    WHERE a.question_id = q.id
      AND a.is_valid_darts = true
      AND a.is_bust        = false
)
WHERE total_valid_count = 0
  AND EXISTS (SELECT 1 FROM answers a WHERE a.question_id = q.id);
