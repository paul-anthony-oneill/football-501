-- V23: Apply unaccent() to entities.normalized_name, matching what V18 did
-- for answers.answer_key.  Without this, accented entity names (Higuaín, Di
-- María) resolve to a key that no longer matches the unaccented answer_key.
UPDATE entities
SET    normalized_name = unaccent(normalized_name)
WHERE  normalized_name != unaccent(normalized_name);
