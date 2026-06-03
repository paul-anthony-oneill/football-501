# Autocomplete and Named Entity Architecture

**Last updated**: 2026-05-27  
**Status**: Implemented (Flyway V5) — bulk upsert and EntityType constants added in audit Phases 2 and 5

## Table of Contents

1. [Overview](#overview)
2. [The entities Table](#the-entities-table)
3. [How Search Works](#how-search-works)
4. [Population Paths](#population-paths)
5. [Adding a New Entity Type](#adding-a-new-entity-type)
6. [Backfill SQL](#backfill-sql)
7. [Accent-Insensitive Matching](#accent-insensitive-matching)
8. [Frontend Integration](#frontend-integration)
9. [Key Files](#key-files)

---

## Overview

During a Trivia 501 turn the player types a name into an input field. After four characters the client fetches autocomplete suggestions from the backend and displays a dropdown. The player selects a name to fill the field and then submits.

**The security constraint**: autocomplete must not reveal valid answers. If the suggestion list were built from the `answers` table for the active question, a player could enumerate all valid answers before committing. To prevent this, the system maintains a separate `entities` table that holds all known names of a given type — not just those that are valid for the current question. A name appearing in the dropdown tells the player nothing about whether it scores any points.

Answer validation remains entirely server-side in `AnswerEvaluator`, which queries the `answers` table.

---

## The entities Table

Created by Flyway migration `V5__add_player_names_autocomplete.sql`.

```
entities
├── id               UUID         PRIMARY KEY DEFAULT gen_random_uuid()
├── entity_type      VARCHAR(50)  NOT NULL DEFAULT 'footballer'
├── display_name     VARCHAR(255) NOT NULL
├── normalized_name  VARCHAR(255) NOT NULL
├── hint             VARCHAR(100)
└── created_at       TIMESTAMP    NOT NULL DEFAULT NOW()

UNIQUE (entity_type, normalized_name)
```

| Column | Purpose |
|--------|---------|
| `entity_type` | Groups rows into autocomplete pools. Examples: `"footballer"`, `"city"`, `"country"`, `"director"`. Matched against `question.config.entity_type`. |
| `display_name` | The formatted name shown in the dropdown, e.g. `"Sergio Agüero"` or `"New York City"`. |
| `normalized_name` | Lowercase, accent-stripped form used as the unique key and the search target, e.g. `"sergio aguero"` or `"new york city"`. Mirrors what PostgreSQL `unaccent()` produces. |
| `hint` | Optional short label shown next to the name in the dropdown (e.g. a country code so the frontend can render a flag emoji). Nullable. Kept as a plain string to stay domain-agnostic. |
| `created_at` | Insertion timestamp. Not updated on conflict. |

The `(entity_type, normalized_name)` unique constraint ensures idempotent inserts — the same name can be registered many times without creating duplicates.

---

## How Search Works

1. The player types into `EntitySearch.tsx`.
2. After 300 ms of idle time and at least 4 characters, the client calls:
   ```
   GET /api/entities/search?type={entityType}&query={query}
   ```
3. `EntityController` rejects queries shorter than 4 characters with an empty list.
4. `EntitySearchService.search()` delegates to `NamedEntityRepository.searchByType()`, which runs:
   ```sql
   SELECT id, entity_type, display_name, normalized_name, hint, created_at
   FROM   entities
   WHERE  entity_type = :entityType
   AND    unaccent(normalized_name) LIKE '%' || unaccent(lower(:query)) || '%'
   ORDER BY
       CASE WHEN unaccent(normalized_name) LIKE unaccent(lower(:query)) || '%'
            THEN 0 ELSE 1 END,
       normalized_name
   LIMIT  :limit
   ```
5. Results (up to 10) are mapped to `PlayerSearchResponse` and returned as JSON.
6. The client renders a dropdown; selecting a suggestion fills the input without auto-submitting.

The `ORDER BY CASE` ranks prefix matches above substring matches, so typing "mar" surfaces "Marco van Basten" before "Raheem Sterling" (which contains "mar" in the middle).

---

## Population Paths

The `entities` table is populated from two sources:

### Path 1: Admin answer import (automatic)

When an admin creates or bulk-imports answers through the admin UI, `AdminAnswerService` calls `EntitySearchService.upsertEntity(displayText, entityType, hint)` for each player name. The upsert uses a native `INSERT … ON CONFLICT (entity_type, normalized_name) DO NOTHING` — it is idempotent and race-condition-safe.

No manual step is needed when answers are added through the normal admin workflow.

**EntityType constants**: callers pass the entity type as a `String`. Always use the constants in `com.trivia501.model.EntityType` rather than bare string literals:

```java
entitySearchService.upsertEntity(displayText, EntityType.FOOTBALLER, hint);
```

Available constants: `EntityType.FOOTBALLER`, `EntityType.CITY`, `EntityType.COUNTRY`, `EntityType.COACH`.

### Path 2: Python scraper

The Python scraper service writes player names to a source table as part of its ETL process. Those names can be forwarded to `entities` via a SQL backfill (see below) or by having the scraper call the upsert endpoint directly.

### Path 3: Manual SQL backfill

Used when migrating existing answer data or seeding a brand-new entity type. See the next section.

---

## Adding a New Entity Type

This is the complete checklist for a developer introducing a new trivia category — for example, world cities.

### Step 1: Choose an entity_type slug

Pick a short, lowercase, stable slug. Examples: `"city"`, `"country"`, `"director"`, `"band"`. This string is the join key between the `entities` table and the `questions.config` JSONB; changing it later requires a data migration.

### Step 2: Create questions with the new entity_type in config

```json
{
  "entity_type": "city",
  "category": "geography",
  "region": "north_america"
}
```

The `entity_type` key in the config JSONB is the only field the autocomplete system reads. All other keys are application-specific and ignored by `EntitySearchService`.

### Step 3: Seed the entities table

There are two options:

**Option A — SQL insert for a finite list** (e.g. all capital cities):

```sql
INSERT INTO entities (entity_type, display_name, normalized_name, hint)
VALUES
    ('city', 'New York City',  'new york city',  'US'),
    ('city', 'São Paulo',      'sao paulo',       'BR'),
    ('city', 'Tōkyō',         'tokyo',           'JP')
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
```

Note that `normalized_name` must be the lowercase, accent-stripped form. Use `unaccent(lower(display_name))` in PostgreSQL to generate it, or mirror the logic in `EntitySearchService.stripAccents()` in Java.

**Option B — Bulk backfill from an answers table** (if answers already exist):

```sql
INSERT INTO entities (entity_type, display_name, normalized_name)
SELECT DISTINCT
    'city',
    display_text,
    unaccent(lower(display_text))
FROM answers
WHERE question_id IN (
    SELECT id FROM questions WHERE config->>'entity_type' = 'city'
)
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
```

### Step 4: Verify the pool before activating questions

```sql
SELECT count(*) FROM entities WHERE entity_type = 'city';
```

If the count is 0, the autocomplete will silently return empty results for every query. Seed the pool before the question type goes live.

### Step 5: Front-end — no changes required

The `EntitySearch` component reads `entityType` as a prop and passes it through to the API call. The caller (the game page) reads `question.config.entity_type` and passes it to `EntitySearch`. As long as the new slug is in the `entities` table and in the question config, everything works without any frontend code changes.

---

## Backfill SQL

Use this snippet to populate `entities` from an existing `answers` table when the footballer pool needs to be rebuilt or initially seeded:

```sql
INSERT INTO entities (entity_type, display_name, normalized_name)
SELECT DISTINCT
    'footballer',
    display_text,
    unaccent(lower(display_text))
FROM answers
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
```

The `ON CONFLICT DO NOTHING` clause makes this safe to run repeatedly. It will not update existing rows, so `display_name` is never overwritten by a differently-cased duplicate.

For other entity types, replace `'footballer'` with the target slug and change the source table or `WHERE` clause as appropriate.

---

## Accent-Insensitive Matching

The system treats accented and unaccented characters as equivalent so that players who cannot type special characters are not disadvantaged.

### How normalization works

The `normalized_name` column stores a lowercase, accent-stripped form of every name. This is computed:

- **In Java** by `EntitySearchService.stripAccents()`, which applies Unicode NFD decomposition and removes combining diacritical marks. This mirrors the behaviour of PostgreSQL `unaccent()`.
- **In SQL** by `unaccent(lower(display_name))` in backfill queries and in the search `WHERE` clause.

Because both sides use the same algorithm, a name registered in Java and then searched in SQL (or vice versa) always produces the same key.

### Normalization Contract Test

The divergence between Java's `stripAccents()` and PostgreSQL's `unaccent()` would be silent — a search would simply return no results. To prevent this, an integration test pins the contract against a real PostgreSQL 17 container:

**File**: `backend/src/test/java/com/trivia501/EntitySearchNormalizationContainerTest.java`

The test:
1. Inserts an accented name (e.g. `"Agüero"`) via `EntitySearchService.upsertEntity()`
2. Searches for it using the unaccented query `"aguero"` via `EntitySearchService.searchEntities()`
3. Asserts the result contains the expected display name

The test covers: acute diacritics (ü), umlauts (ö), accented query input, the bulk-upsert code path, and direct `stripAccents()` unit assertions. It is marked `@DisabledWithoutDocker` so it skips in environments without Docker.

### Examples

| Player types | normalized_name stored | Display name returned |
|---|---|---|
| `aguero` | `sergio aguero` | Sergio Agüero |
| `haaland` | `erling haaland` | Erling Haaland |
| `sao paulo` | `sao paulo` | São Paulo |
| `nkunku` | `christopher nkunku` (prefix match) | Christopher Nkunku |

### Index

The GIN trigram index created in V5 enables fast `LIKE '%query%'` searches without a full-table scan:

```sql
CREATE INDEX idx_entities_unaccent_trgm
    ON entities USING GIN (unaccent(normalized_name) gin_trgm_ops);
```

A separate B-tree index on `entity_type` allows the planner to filter the pool efficiently before the text search:

```sql
CREATE INDEX idx_entities_type ON entities (entity_type);
```

The query planner typically uses both indexes: `idx_entities_type` narrows the row set by type, then `idx_entities_unaccent_trgm` applies the trigram filter.

---

## Frontend Integration

The `EntitySearch` component is the single place in the codebase that knows about entity autocomplete.

**Props**:

| Prop | Type | Default | Purpose |
|------|------|---------|---------|
| `entityType` | `string` | `"footballer"` | Scopes the search to the right entity pool. Read from `question.config.entity_type` by the caller. |
| `value` | `string` | — | Controlled input value. |
| `onChange` | `(value: string) => void` | — | Called on every keystroke and on suggestion selection. |
| `onSubmit` | `() => void` | — | Called when the player presses Enter. |
| `disabled` | `boolean` | `false` | Disables the input (e.g. when it is the opponent's turn). |
| `placeholder` | `string` | `"Enter answer..."` | Input placeholder text. |

**Behaviour**:
- Characters 1–3: shows "Keep typing for suggestions…" hint, no API call made.
- Character 4+: fires `GET /api/entities/search?type={entityType}&query={query}` after a 300 ms debounce.
- Selecting a suggestion fills the input and returns focus; it does not submit.
- The `hint` field from the API response is rendered as a flag emoji via `getFlagEmoji()` when the hint is a two-letter country code.
- Pressing Enter calls `onSubmit()` and clears the suggestion list.

**Typical caller pattern** (game page):

```tsx
<EntitySearch
  entityType={question.config?.entity_type ?? "footballer"}
  value={answerInput}
  onChange={setAnswerInput}
  onSubmit={handleSubmit}
  disabled={!isMyTurn}
/>
```

---

## Key Files

| Concept | Path |
|---------|------|
| Flyway migration (table + indexes) | `backend/src/main/resources/db/migration/V5__add_player_names_autocomplete.sql` |
| JPA model | `backend/src/main/java/com/trivia501/model/NamedEntity.java` |
| Repository (search + bulk upsert) | `backend/src/main/java/com/trivia501/repository/NamedEntityRepository.java` |
| Service (search + upsert + backfill) | `backend/src/main/java/com/trivia501/service/EntitySearchService.java` |
| EntityType constants | `backend/src/main/java/com/trivia501/model/EntityType.java` |
| REST controller | `backend/src/main/java/com/trivia501/controller/EntityController.java` |
| Frontend component | `frontend-react/src/components/game/EntitySearch.tsx` |
| Normalization contract test | `backend/src/test/java/com/trivia501/EntitySearchNormalizationContainerTest.java` |
