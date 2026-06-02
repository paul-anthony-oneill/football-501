-- Football 501 — V18: Re-normalize stored names for characters that NFD does not decompose
--
-- Java's Normalizer.NFD decomposes most accented characters (ü→u, ö→o, é→e)
-- but stroke/ligature characters like ø, æ, ł, đ survive the decomposition.
-- PostgreSQL's unaccent() handles these correctly, so the stored normalized_name
-- (computed in Java) drifts from what unaccent() would produce.
--
-- This migration re-normalizes entities.normalized_name, players.normalized_name,
-- and answers.answer_key so they match what the updated Java-side stripAccents()
-- now produces (EntitySearchService.NFD_OPAQUE_REPLACEMENTS).

-- Entities: skip rows where the unaccented form would collide with an existing row
-- (same entity_type, same unaccented normalized_name, different id).
UPDATE entities
SET normalized_name = unaccent(normalized_name)
WHERE normalized_name ~ '[øØæÆłŁđĐœŒðÐþÞß]'
AND NOT EXISTS (
    SELECT 1 FROM entities e2
    WHERE e2.entity_type = entities.entity_type
    AND e2.normalized_name = unaccent(entities.normalized_name)
    AND e2.id != entities.id
);

-- Answers: same collision guard (unique index on question_id + answer_key).
UPDATE answers
SET answer_key = unaccent(answer_key)
WHERE answer_key ~ '[øØæÆłŁđĐœŒðÐþÞß]'
AND NOT EXISTS (
    SELECT 1 FROM answers a2
    WHERE a2.question_id = answers.question_id
    AND a2.answer_key = unaccent(answers.answer_key)
    AND a2.id != answers.id
);

-- Players: no unique constraint on normalized_name, so a plain UPDATE is safe.
UPDATE players
SET normalized_name = unaccent(normalized_name)
WHERE normalized_name ~ '[øØæÆłŁđĐœŒðÐþÞß]';
