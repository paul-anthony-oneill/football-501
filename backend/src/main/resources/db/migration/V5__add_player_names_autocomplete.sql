-- Football 501 Database - Named Entity Autocomplete Table
-- Flyway Migration V5
-- Created: 2026-05-25
--
-- Purpose: global registry of named things that can be entered as answers,
-- used exclusively for autocomplete search during gameplay.
--
-- DESIGN: intentionally decoupled from the `answers` table.
-- Appearing in autocomplete does NOT reveal whether a name is a valid answer
-- to the current question.  All answer validation continues to happen
-- server-side in AnswerEvaluator against the `answers` table.
--
-- Populated from two paths:
--   1. Admin answer bulk-import (via AdminAnswerService → EntitySearchService)
--   2. Python scraper (same names it writes to the `players` source table)
--
-- EXTENSIBILITY: entity_type allows different autocomplete lists per question
-- category (footballers, cities, directors, countries, etc.).
-- Questions declare their entity_type via config JSONB:
--   { "entity_type": "footballer", "stat_type": "goals", ... }
--   { "entity_type": "city",       "region": "north_america", ... }

-- ========================================
-- Accent-stripping extension
-- ========================================

CREATE EXTENSION IF NOT EXISTS unaccent;

-- ========================================
-- Entities Table
-- ========================================

CREATE TABLE entities (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    -- The entity type groups names into autocomplete pools:
    -- 'footballer', 'city', 'country', 'director', etc.
    entity_type      VARCHAR(50)  NOT NULL DEFAULT 'footballer',

    -- Display form shown in the dropdown, e.g. "Sergio Agüero" or "New York City"
    display_name     VARCHAR(255) NOT NULL,

    -- Lowercase, accent-stripped form used as the unique key and search target,
    -- e.g. "sergio aguero" or "new york city"
    normalized_name  VARCHAR(255) NOT NULL,

    -- Optional extra context shown alongside the name (nationality flag, country, etc.)
    -- Kept as a simple string to stay agnostic; structured detail belongs in metadata.
    hint             VARCHAR(100),

    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),

    UNIQUE (entity_type, normalized_name)
);

COMMENT ON TABLE  entities                 IS 'Global named-entity registry for autocomplete. One row per unique (type, name) pair. Decoupled from the per-question answers table.';
COMMENT ON COLUMN entities.entity_type     IS 'Groups entities into pools: footballer, city, country, director, etc. Matched against question config.entity_type.';
COMMENT ON COLUMN entities.display_name    IS 'Formatted name shown in the autocomplete dropdown.';
COMMENT ON COLUMN entities.normalized_name IS 'Lowercase, accent-stripped name used as the unique key and the search target.';
COMMENT ON COLUMN entities.hint            IS 'Optional short label shown next to the name (e.g. country code for a flag emoji, or a continent for a city).';

-- GIN trigram index on normalized_name.
-- normalized_name is pre-stripped of accents in Java (via Normalizer.NFD) before
-- being stored, so indexing the column directly is correct and avoids the
-- IMMUTABLE requirement that blocks functional-index use of unaccent().
-- Enables fast LIKE '%query%' substring matching without a full-table scan.
CREATE INDEX idx_entities_unaccent_trgm
    ON entities USING GIN (normalized_name gin_trgm_ops);

-- Separate index for fast filtering by type before the text search.
CREATE INDEX idx_entities_type
    ON entities (entity_type);
