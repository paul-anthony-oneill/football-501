-- V24: Apply unaccent() to answers.answer_key and players.normalized_name for
-- normal (NFD-decomposable) accented characters.  V18 only handled NFD-opaque
-- characters (ø, æ, ł, đ, œ, ð, þ, ß).  V23 fixed entities.normalized_name.
-- This finishes the job so all three tables are consistent.

-- Answers: collision guard (unique index on question_id + answer_key).
UPDATE answers
SET answer_key = unaccent(answer_key)
WHERE answer_key != unaccent(answer_key)
AND NOT EXISTS (
    SELECT 1 FROM answers a2
    WHERE a2.question_id = answers.question_id
    AND a2.answer_key = unaccent(answers.answer_key)
    AND a2.id != answers.id
);

-- Players: no unique constraint, plain UPDATE is safe.
UPDATE players
SET normalized_name = unaccent(normalized_name)
WHERE normalized_name != unaccent(normalized_name);
