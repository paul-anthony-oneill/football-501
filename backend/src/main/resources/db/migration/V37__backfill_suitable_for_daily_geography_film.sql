-- V37: Back-fill suitable_for_daily for geography and film questions.
--
-- Root cause: V20 marked questions WHERE status = 'active', but geography and
-- film questions were still 'excluded' at V20 migration time. V35 later changed
-- them to 'active', leaving suitable_for_daily = false on all of them.
--
-- This migration re-runs the eligibility check against all active questions so
-- any category in the same situation is also fixed. Two deliberate differences
-- from V20:
--   1. difficulty cap is 3.5 (DAILY_MAX_DIFFICULTY in code), not V20's 5.5.
--   2. No single_question_viable = true requirement — daily challenges can start
--      at 101, so a pool of 101+ valid points is sufficient; the scheduler's
--      total_score_pool filter handles score-matching per starting score.

UPDATE questions
   SET suitable_for_daily = true
 WHERE status             = 'active'
   AND suitable_for_daily = false
   AND difficulty_score   IS NOT NULL
   AND difficulty_score   BETWEEN 0.0 AND 3.5
   AND total_score_pool   >= 101;
