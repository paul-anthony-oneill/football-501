# Database Design — Source of Truth

Status: **V6 + V7 implemented and merged** (`feature/db-schema-v6-v7`). V8–V10 pending.
Last updated: 2026-05-25.

This document supersedes the ad-hoc evolution in V1–V5. It defines the target shape of the database. V6 and V7 have been expressed as Flyway migrations and companion Java/frontend changes. V8–V10 are the remaining steps before the architecture is fully live.

---

## Implementation progress

| Migration | Scope | Status |
|---|---|---|
| **V1–V5** | Initial schema → autocomplete entities | ✅ Done (pre-existing) |
| **V6** | Football source layer: `seasons`, `player_external_ids`, `team_external_ids`, `player_season_stints`; competition_type normalisation; `tier` column | ✅ Done — `V6__football_source_layer.sql` |
| **V7** | `question_templates`; question lifecycle (`status`); drop `is_active`; `materialized_at` on answers; `scrape_run_logs` | ✅ Done — `V7__question_lifecycle_and_materialization.sql` |
| **V8** | Python backfill: drain `players.career_stats` JSONB → `player_season_stints` | ⏳ Pending |
| **V9** | Drop `players.career_stats`, `players.fbref_id`, `teams.fbref_id`; add NOT NULL guarantee on FBref external IDs | ⏳ Pending (after V8 verification) |
| **V10** | Seed `question_templates`; run template generator for first draft batch | ⏳ Pending |

---

## 1. Goals

1. Answer aggregated questions correctly across many seasons and teams — e.g. "Goals for Manchester United in the Premier League since 2000" returns each player's correct *Man-United-PL-only* total, even if they left and came back.
2. Keep the **game engine (questions/answers/entities)** domain-agnostic so non-football trivia can plug in later without forking the match-runtime code.
3. Make football the first concrete category — with rich per-stint stats — but treat its tables as *one possible source layer* rather than baked into the game schema.
4. Support **template-driven** question generation (auto-enumerate "Goals for {team} in {league} since {year}" for every Top-5 team) while still allowing admins to curate, promote, and edit before activation.
5. Make external-data identity (FBref now, possibly Transfermarkt/Sofascore later) a first-class concern, so we never have to retrofit a multi-source ID story.

## 2. Non-goals

- International football (World Cup, Euros) — different shape, deferred.
- Per-match event data (shots, passes, xG by minute) — out of scope for trivia answers.
- Storing pre-computed answer breakdowns — computed on demand when the player opens an "explain" panel.

---

## 3. Architecture overview

Three logical layers, each owning a clear set of tables:

```
┌────────────────────────────────────────────────────────────────────┐
│  Game Engine Layer  (domain-agnostic, shared across categories)    │
│  ─ categories                                                      │
│  ─ question_templates    ✅ added V7                               │
│  ─ questions             ✅ evolved V7 (status lifecycle)          │
│  ─ answers               ✅ evolved V7 (materialized_at)           │
│  ─ entities              ✅ unchanged (autocomplete pool)          │
└────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ materialized from
                              │
┌────────────────────────────────────────────────────────────────────┐
│  Football Source Layer  (one of many possible category sources)    │
│  ─ seasons                  ✅ added V6                            │
│  ─ competitions             ✅ evolved V6 (type normalised + tier) │
│  ─ teams                    ⏳ fbref_id drop pending V9            │
│  ─ players                  ⏳ career_stats drop pending V9        │
│  ─ player_external_ids      ✅ added V6                            │
│  ─ team_external_ids        ✅ added V6                            │
│  ─ player_season_stints     ✅ added V6 — the keystone table       │
└────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ populated by
                              │
┌────────────────────────────────────────────────────────────────────┐
│  Ingest Layer  (Python / ScraperFC microservice)                   │
│  ─ scrape_jobs              ✅ existed pre-V6                      │
│  ─ scrape_run_logs          ✅ added V7                            │
└────────────────────────────────────────────────────────────────────┘
```

**Decoupling rule:** the game engine reads `questions`, `answers`, `entities` only. It never joins to `player_season_stints` or `teams`. All football-specific logic lives in the **materializer** that translates a question template + parameters into a set of `answers` rows.

This is the key to category-agnosticism: a future "World Capitals" category adds its own `world_cities` table and its own materializer, but reuses `questions` + `answers` + `entities` unchanged.

---

## 4. Football source layer — table by table

### 4.1 `seasons` ✅ implemented in V6

A first-class table avoids the "is it '2023-24' or '2023/2024'?" string-format problem and makes "since 2000" a clean `start_year >= 2000` filter.

```sql
CREATE TABLE seasons (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label       VARCHAR(10) NOT NULL UNIQUE,   -- '2023-24'
    start_year  SMALLINT     NOT NULL,         -- 2023
    end_year    SMALLINT     NOT NULL,         -- 2024
    start_date  DATE,                          -- approximate, for cup overlap
    end_date    DATE,
    is_current  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_seasons_start_year ON seasons (start_year);
```

One row per season cycle. Cups that span the calendar year reuse the league season they fall within (e.g. FA Cup 2023-24 maps to the 2023-24 season). **Decision locked:** hard-delete + re-insert on rescrape (no `effective_from/to` history).

### 4.2 `competitions` ✅ evolved in V6

V6 normalised the `competition_type` enum values (V1 used inconsistent slugs) and added `tier`:

```sql
-- Normalisation applied in V6
UPDATE competitions SET competition_type = 'domestic_cup'    WHERE competition_type = 'cup';
UPDATE competitions SET competition_type = 'continental_club' WHERE competition_type = 'continental';

ALTER TABLE competitions ADD COLUMN tier SMALLINT;
-- tier = 1 for top-flight domestic leagues; NULL for cups and continental competitions.
```

Canonical `competition_type` values now in use:
- `domestic_league` — EPL, La Liga, Serie A, Bundesliga, Ligue 1
- `domestic_cup` — FA Cup, Copa del Rey, DFB-Pokal, Coppa Italia, Coupe de France
- `continental_club` — UCL, UEL, UECL

(`international` and `continental_national` are reserved but not seeded yet.)

### 4.3 `teams` ⏳ `fbref_id` drop pending V9

Existing table is otherwise fine. `fbref_id` column stays until V9 (after the Python backfill in V8 has been verified and every team has a corresponding `team_external_ids` row).

### 4.4 `players` ⏳ refactor pending V8 + V9

The current `players.career_stats` JSONB column is the source of the muddle. It will be dropped in V9 after the V8 Python backfill. The target thin identity record:

```sql
-- Target shape after V9 (not yet applied):
CREATE TABLE players (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name    VARCHAR(255) NOT NULL,
    normalized_name   VARCHAR(255) NOT NULL,
    display_name      VARCHAR(255) NOT NULL,   -- 'Sergio Agüero'
    nationality       VARCHAR(100),
    date_of_birth     DATE,
    primary_position  VARCHAR(20),             -- 'FW','MF','DF','GK' (nullable)
    last_scraped_at   TIMESTAMP,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);
```

**Decision locked:** `primary_position` is a single column (not season-aware). If per-season position tracking is needed later, it will be added to `player_season_stints`.

External IDs move out (see 4.5). `career_stats` is replaced entirely by `player_season_stints`.

### 4.5 `player_external_ids` / `team_external_ids` ✅ implemented in V6

Multi-source identity from day one.

```sql
CREATE TABLE player_external_ids (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id      UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    source         VARCHAR(32) NOT NULL,        -- 'fbref','transfermarkt','sofascore','wikidata'
    external_id    VARCHAR(64) NOT NULL,
    source_url     TEXT,
    confidence     SMALLINT DEFAULT 100,        -- for fuzzy-matched cross-source links
    created_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE (source, external_id)
);

CREATE INDEX idx_player_ext_player ON player_external_ids (player_id);
```

Same shape for `team_external_ids`. FBref backfills as the only source on day one; future sources slot in without a migration.

### 4.6 `player_season_stints` ✅ implemented in V6 — the keystone

**One row per (player, season, team, competition).** A player who played for two clubs in 2018-19 has two rows for the EPL stat. A player who played EPL and UCL for the same club in 2019-20 has two rows (one per competition).

```sql
CREATE TABLE player_season_stints (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id        UUID NOT NULL REFERENCES players(id)       ON DELETE CASCADE,
    season_id        UUID NOT NULL REFERENCES seasons(id)       ON DELETE RESTRICT,
    team_id          UUID NOT NULL REFERENCES teams(id)         ON DELETE RESTRICT,
    competition_id   UUID NOT NULL REFERENCES competitions(id)  ON DELETE RESTRICT,

    appearances      SMALLINT NOT NULL DEFAULT 0,
    starts           SMALLINT NOT NULL DEFAULT 0,
    sub_appearances  SMALLINT NOT NULL DEFAULT 0,
    minutes          INTEGER  NOT NULL DEFAULT 0,

    goals            SMALLINT NOT NULL DEFAULT 0,
    assists          SMALLINT NOT NULL DEFAULT 0,

    yellow_cards     SMALLINT NOT NULL DEFAULT 0,
    red_cards        SMALLINT NOT NULL DEFAULT 0,

    clean_sheets     SMALLINT NOT NULL DEFAULT 0,    -- GK stat (0 for outfield)
    goals_conceded   SMALLINT NOT NULL DEFAULT 0,    -- GK stat
    is_goalkeeper    BOOLEAN  NOT NULL DEFAULT FALSE,

    source           VARCHAR(32) NOT NULL,            -- 'fbref'
    source_scraped_at TIMESTAMP NOT NULL,

    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),

    UNIQUE (player_id, season_id, team_id, competition_id)
);

-- Aggregation index — covers the common "stat by team x comp across seasons" pattern.
CREATE INDEX idx_stints_team_comp_season
    ON player_season_stints (team_id, competition_id, season_id);

-- For "career totals filtered by competition" patterns.
CREATE INDEX idx_stints_player_comp
    ON player_season_stints (player_id, competition_id);

-- For "all players who played in this season-comp" pattern (template enumeration).
CREATE INDEX idx_stints_comp_season
    ON player_season_stints (competition_id, season_id);
```

**Why this solves the original problem.** "Goals for Man U in the Premier League since 2000" is now:

```sql
SELECT player_id, SUM(goals) AS total
  FROM player_season_stints s
  JOIN seasons sn ON sn.id = s.season_id
 WHERE s.team_id        = :man_u_id
   AND s.competition_id = :epl_id
   AND sn.start_year   >= 2000
 GROUP BY player_id
 HAVING SUM(goals) > 0;
```

A player who played EPL for Arsenal in 2005-06 is not in the result. A player who played for Man U → Real Madrid → Man U is summed *only* across their two Man U stints. The query is sub-50ms with the composite index.

**Sizing.** ~25 seasons × (5 leagues + 5 cups + 3 UEFA) × ~500 player-stints/comp-season ≈ **150k rows**. Trivial for Postgres.

---

## 5. Game engine layer — table by table

### 5.1 `categories` (unchanged)

Seeded with `football` today; `general_trivia`, `geography`, etc. can be added later without code changes.

### 5.2 `question_templates` ✅ implemented in V7

Hybrid model: **metadata in DB, materializer logic in code.** The template row stores everything an admin sees and tweaks; the actual SQL that produces the answer set lives in a Java class keyed by `materializer_key`.

```sql
CREATE TABLE question_templates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id      UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    slug             VARCHAR(64) NOT NULL UNIQUE,
    display_name     VARCHAR(255) NOT NULL,
    text_template    TEXT NOT NULL,
    param_schema     JSONB NOT NULL,
    materializer_key VARCHAR(64) NOT NULL,
    metric_key       VARCHAR(50) NOT NULL,
    default_min_score INTEGER,
    is_active        BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);
```

The Java `QuestionMaterializer` interface registers itself under a key. The generator job reads active templates, enumerates params, inserts draft `questions`. No template → no materializer wiring.

The `QuestionTemplate` JPA model and `QuestionTemplateRepository` are implemented in `com.football501.model` and `com.football501.repository`. No template rows are seeded yet — that happens in V10.

### 5.3 `questions` ✅ evolved in V7

Added `template_id`, `template_params`, `status`. The `is_active` boolean column was removed.

```sql
ALTER TABLE questions
    ADD COLUMN template_id     UUID REFERENCES question_templates(id) ON DELETE SET NULL,
    ADD COLUMN template_params JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN status          VARCHAR(20) NOT NULL DEFAULT 'active';
                                          -- 'draft' | 'active' | 'retired'

-- Backfill: active=TRUE → 'active', active=FALSE → 'retired'
UPDATE questions SET status = CASE WHEN is_active THEN 'active' ELSE 'retired' END;
ALTER TABLE questions DROP COLUMN is_active;

CREATE INDEX idx_questions_status ON questions (status);
CREATE INDEX idx_questions_template ON questions (template_id);
```

**Implementation detail that diverged from the plan:** the plan said to backfill all rows to `status='active'`, but inactive questions (`is_active=FALSE`) were mapped to `status='retired'` instead — a more faithful representation of intent.

**Lifecycle.** New questions (hand-curated or auto-generated) start as `status='draft'`. Admin promotes via `PATCH /api/admin/questions/{id}/status` with body `{"status": "active"}`. Retiring sets `status='retired'` and leaves answers in place for historical match replay.

The `Question` Java model exposes `STATUS_DRAFT`, `STATUS_ACTIVE`, `STATUS_RETIRED` constants and `isActive()` / `isDraft()` / `isRetired()` convenience helpers.

The admin UI now has a three-state status filter (All / Draft / Active / Retired) and a context-aware lifecycle button on the question detail page:
- Draft → **Activate**
- Active → **Retire**
- Retired → **Re-activate**

### 5.4 `answers` ✅ evolved in V7

`materialized_at` added:

```sql
ALTER TABLE answers
    ADD COLUMN materialized_at TIMESTAMP NOT NULL DEFAULT NOW();
```

No `breakdown` JSONB — breakdown is computed on demand via a future `GET /api/answers/{id}/breakdown` endpoint that re-queries stints through the materializer.

### 5.5 `entities` (unchanged)

The autocomplete pool from V5 stays as-is. The football materializer is responsible for upserting any new player display name into `entities` with `entity_type = 'footballer'` whenever it writes an answer — same contract as today.

---

## 6. Materialization pipeline

```
┌──────────────────┐    nightly    ┌───────────────────────┐
│ ScraperFC (py)   │ ─────────────▶│ player_season_stints  │
└──────────────────┘               └─────────┬─────────────┘
                                             │
                          ┌──────────────────┴──────────────────┐
                          ▼                                     ▼
              ┌──────────────────────┐              ┌────────────────────────┐
              │ Template generator   │              │ Stale-answer detector  │
              │ (reads templates,    │              │ (finds answers whose   │
              │  inserts draft       │              │  stints changed since  │
              │  questions)          │              │  materialized_at)      │
              └──────────┬───────────┘              └────────────┬───────────┘
                         │                                       │
                         ▼                                       ▼
              ┌────────────────────┐                ┌────────────────────────┐
              │ Admin UI: promote  │                │ Re-materialize         │
              │ draft → active     │ ─────────────▶ │ (invoke materializer,  │
              │ (or edit/reject)   │                │  upsert answers)       │
              └────────────────────┘                └────────────────────────┘
```

Three jobs, each idempotent:

1. **Stint ingest** — Python writes to `player_season_stints` via `ON CONFLICT (player_id, season_id, team_id, competition_id) DO UPDATE`. Bumps `updated_at`.
2. **Template generator** — Java/Spring scheduled job. Reads active templates, enumerates valid param combos (e.g. "every team that has played EPL since 2000"), inserts `questions` rows with `status='draft'` skipping any combo already present.
3. **Answer materializer** — Triggered when admin promotes a draft, or by the stale-detector. Resolves `(template, params) → materializer_key`, runs the materializer, upserts `answers`, bumps `materialized_at`.

**Current state:** Jobs 2 and 3 are not yet wired — no template rows exist (V10 pending) and the `QuestionMaterializer` interface is defined but has no concrete implementations yet. Answers are still populated manually via the bulk-import admin endpoint.

---

## 7. Worked example

**Template row** (one of many):

| field | value |
|---|---|
| slug | `team_competition_metric_since` |
| display_name | "Team metric in competition since year" |
| text_template | `"{metric_label} for {team_name} in {competition_name} since {start_year}"` |
| param_schema | `{ params: { team_id, competition_id, metric_key, start_year } }` |
| materializer_key | `football.team_competition_metric_since` |
| metric_key | `goals` |

**Generator** enumerates: for each (team, competition) where the team has stints in `competition` with `start_year >= 2000`, for each metric in `[goals, assists, appearances]`, insert a draft question:

```
question_text:   "Goals for Manchester United in the Premier League since 2000"
template_params: {"team_id":"…","competition_id":"…","metric_key":"goals","start_year":2000}
status:          "draft"
```

**Admin promotes** → materializer runs:

```sql
INSERT INTO answers (question_id, answer_key, display_text, score, is_valid_darts, is_bust, materialized_at, metadata)
SELECT
    :question_id,
    p.normalized_name,
    p.display_name,
    SUM(s.goals)                                        AS score,
    is_valid_darts(SUM(s.goals))                        AS is_valid_darts,
    SUM(s.goals) > 180                                  AS is_bust,
    NOW(),
    jsonb_build_object('player_id', p.id)
  FROM player_season_stints s
  JOIN players p  ON p.id  = s.player_id
  JOIN seasons sn ON sn.id = s.season_id
 WHERE s.team_id        = :team_id
   AND s.competition_id = :competition_id
   AND sn.start_year   >= :start_year
 GROUP BY p.id, p.normalized_name, p.display_name
HAVING SUM(s.goals) > 0;
```

Cristiano Ronaldo (Man U 03-04 → 08-09, and 21-22 → 22-23) lands on one row with the correct combined total, and his Real Madrid / Juventus / Al-Nassr stints are correctly excluded because they don't satisfy `team_id = :man_u_id AND competition_id = :epl_id`.

---

## 8. Migration history and next steps

### Completed migrations

| Flyway | Scope | What was actually built |
|---|---|---|
| **V1–V5** | Initial schema → autocomplete | Pre-existing (see git history) |
| **V6** | Football source layer | `seasons`, `player_external_ids`, `team_external_ids`, `player_season_stints` created. `competition_type` normalised (cup→domestic_cup, continental→continental_club). `tier SMALLINT` added to `competitions`. All additive. |
| **V7** | Question lifecycle + materialisation | `question_templates` created. `status` (draft\|active\|retired), `template_id`, `template_params` added to `questions`. `is_active` column **dropped** (backfilled first). `materialized_at` added to `answers`. `scrape_run_logs` created. Java models, repositories, services, DTOs, and admin frontend updated in the same PR. |

### Pending migrations

#### V8 — Python backfill (not a Flyway migration)

Run as a one-off script, not checked into Flyway. The Python scraper needs a new module that:

1. Reads each player row and its `career_stats` JSONB array.
2. For each element in the array, resolves `season` → `seasons.id`, `team` → `teams.id`, `competition` → `competitions.id`.
3. Inserts a `player_season_stints` row via `ON CONFLICT DO UPDATE`.
4. Also writes `player_external_ids` rows (`source='fbref'`, `external_id = players.fbref_id`).
5. Also writes `team_external_ids` rows (`source='fbref'`, `external_id = teams.fbref_id`).

**Verification gate before V9:** run a checksum query to compare per-player goal/appearance totals between the old JSONB and the new stints table. Only proceed to V9 when the counts match.

```sql
-- Example parity check (run before V9):
WITH old_totals AS (
    SELECT id, (career_stats_elem->>'goals')::int AS goals
    FROM players, jsonb_array_elements(career_stats) AS career_stats_elem
),
new_totals AS (
    SELECT player_id, SUM(goals) AS goals
    FROM player_season_stints
    GROUP BY player_id
)
SELECT o.id, o.goals AS old_goals, n.goals AS new_goals
FROM old_totals o
JOIN new_totals n ON n.player_id = o.id
WHERE o.goals != n.goals;
-- Must return zero rows before V9.
```

**Python scraper changes in lockstep:** the existing JSONB-write path is replaced by a new path that writes directly to `player_season_stints`. Both paths can coexist during transition.

#### V9 — Destructive cleanup (after V8 verification)

**Prerequisite:** V8 checksum query returns zero rows.

```sql
-- Drop the trigger and function that validated career_stats structure
DROP TRIGGER IF EXISTS validate_career_stats_before_insert ON players;
DROP FUNCTION IF EXISTS validate_career_stats_jsonb();

-- Drop the columns that have been superseded
ALTER TABLE players DROP COLUMN career_stats;
ALTER TABLE players DROP COLUMN fbref_id;
ALTER TABLE teams   DROP COLUMN fbref_id;

-- Add NOT NULL enforcement on FBref external ID rows
-- (every player and team must now have a player_external_ids / team_external_ids row)
-- This is enforced by application logic, not a DB constraint, to allow incremental backfill.
```

**Note:** the `validate_career_stats_before_insert` trigger and `validate_career_stats_jsonb()` function must be dropped **before** dropping the `career_stats` column — otherwise Postgres will error on the column drop.

#### V10 — First template batch and generator run

1. Insert the first `question_templates` rows (seed data for the five Top-5 leagues × goals/appearances/assists).
2. Implement the first concrete `QuestionMaterializer` — `FootballTeamCompetitionMetricSinceMaterializer` — registered under key `football.team_competition_metric_since`.
3. Run the template generator job manually (or trigger via admin API) to produce draft `questions` for each valid (team, competition, start_year) combination.
4. Admin reviews drafts in the UI and promotes incrementally.

---

## 9. How a new non-football category plugs in

To prove the agnosticism, walk through "World Capitals":

1. Add a row to `categories` (`slug='geography'`).
2. Create a new source table — `world_cities (id, name, country, population, founded_year)` — entirely owned by the geography domain. Game engine knows nothing about it.
3. Add one or more `question_templates` rows with `materializer_key='geography.city_metric'`.
4. Implement `GeographyCityMetricMaterializer` in Java. It reads `world_cities`, writes `answers`.
5. Generator + admin promote loop is identical to football.
6. Add an entity-type seed: insert all city names into `entities` with `entity_type='city'`. Set `question.config.entity_type='city'` so the frontend autocomplete uses the right pool.

No changes to `questions`, `answers`, `entities`, or any game-engine code.

---

## 10. Open items — resolved

All items from the original proposal have been signed off. Decisions recorded here for reference.

| # | Item | Decision |
|---|---|---|
| 1 | **Match scope** — mixed category matches? | Single category per match for now. No schema change. |
| 2 | **Soft-delete vs hard-delete on stints** | Hard delete + re-insert on rescrape. Simpler; sufficient for trivia. |
| 3 | **Season alignment for cups** | Each domestic cup / UEFA competition row maps to the league season it overlaps (FA Cup 2023-24 → `seasons.label='2023-24'`). Documented in `seasons` table comment. |
| 4 | **Player position evolution** | Single `primary_position` column on `players`. If per-season position is needed later it goes on `player_season_stints`. |
| 5 | **Materializer concurrency** | Deferred — not a schema concern. Worker pool with per-question advisory lock when the stale-detector is built. |
| 6 | **`questions.is_active` vs `status`** | Drop `is_active`; replace with `status` string. Backfill: `TRUE→'active'`, `FALSE→'retired'`. Java model updated; frontend updated. |
| 7 | **`scrape_run_logs` — when to add?** | Added in V7 alongside the other ingest-layer work. Schema defined before the Python upgrade to avoid a later migration. |
