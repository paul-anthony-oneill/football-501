-- Enables the pg_trgm extension required for fuzzy player name matching.
-- Run automatically by the PostgreSQL Testcontainers container at startup.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
