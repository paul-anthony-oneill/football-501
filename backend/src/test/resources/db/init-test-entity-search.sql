-- PostgreSQL extensions required by the entity-search integration tests.
--
-- pg_trgm  — powers the GIN index used by the LIKE search in NamedEntityRepository.
-- unaccent — used by the search query: unaccent(lower(:query))
--
-- Both are run via Testcontainers @Container.withInitScript before Flyway/Hibernate
-- DDL creates the schema, so they are available when the entities table is created.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
