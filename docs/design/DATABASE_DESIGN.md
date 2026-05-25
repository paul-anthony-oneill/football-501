# Database Design — Source of Truth

Status: **Proposal — not yet implemented in Flyway.**
Last updated: 2026-05-25.

This document supersedes the ad-hoc evolution in V1–V5. It defines the target shape of the database before any further migrations are written. Once approved, the changes here will be expressed as Flyway migrations V6+ and a backfill script that drains the existing `players.career_stats` JSONB into the new normalized tables.

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
│  ─ question_templates    ← NEW                                     │
│  ─ questions             ← evolves                                 │
│  ─ answers               ← unchanged shape                         │
│  ─ entities              ← unchanged (autocomplete pool)           │
└────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ materialized from
                              │
┌────────────────────────────────────────────────────────────────────┐
│  Football Source Layer  (one of many possible category sources)    │
│  ─ seasons                  ← NEW                                  │
│  ─ competitions             ← evolves                              │
│  ─ teams                    ← evolves                              │
│  ─ players                  ← refactored (drop career_stats JSONB) │
│  ─ player_external_ids      ← NEW                                  │
│  ─ team_external_ids        ← NEW                                  │
│  ─ player_season_stints     ← NEW — the keystone table             │
└────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ populated by
                              │
┌────────────────────────────────────────────────────────────────────┐
│  Ingest Layer  (Python / ScraperFC microservice)                   │
│  ─ scrape_jobs                                                     │
│  ─ scrape_run_logs                                                 │
└────────────────────────────────────────────────────────────────────┘
```

**Decoupling rule:** the game engine reads `questions`, `answers`, `entities` only. It never joins to `player_season_stints` or `teams`. All football-specific logic lives in the **materializer** that translates a question template + parameters into a set of `answers` rows.

This is the key to category-agnosticism: a future "World Capitals" category adds its own `world_cities` table and its own materializer, but reuses `questions` + `answers` + `entities` unchanged.

---

## 4. Football source layer — table by table

### 4.1 `seasons`

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

One row per season per "season cycle". Cups that span the calendar year reuse the league season they fall within (e.g. FA Cup 2023-24 maps to the 2023-24 season).

### 4.2 `competitions` (evolved)

Already exists. No structural changes needed beyond making the `competition_type` enum explicit for the scope we now support:

- `domestic_league` — EPL, La Liga, Serie A, Bundesliga, Ligue 1
- `domestic_cup` — FA Cup, Copa del Rey, DFB-Pokal, Coppa Italia, Coupe de France
- `continental_club` — UCL, UEL, UECL

(`international` and `continental_national` are reserved but not seeded yet.)

Add `tier` for league pyramid future-proofing (`1` for top flight; nullable for cups/Europe).

### 4.3 `teams` (evolved)

Existing table is fine. We will:

- Drop `fbref_id` from `teams` (moved into `team_external_ids`).
- Keep `normalized_name` for matching during scraper ingest.

### 4.4 `players` (refactored)

The current `players.career_stats` JSONB column is the source of the muddle. Drop it. The player row becomes a thin identity record.

```sql
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

CREATE INDEX idx_players_norm ON players (normalized_name);
CREATE INDEX idx_players_name_trgm ON players USING GIN (display_name gin_trgm_ops);
```

External IDs move out (see 4.5). `career_stats` is replaced entirely by `player_season_stints`.

### 4.5 `player_external_ids` / `team_external_ids`

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

### 4.6 `player_season_stints` — the keystone

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

A player who played EPL for Arsenal in 2005-06 is not in the result — their Arsenal row has `team_id = arsenal_id`. A player who played for Man U → Real Madrid → Man U is summed *only* across their two Man U stints. The query is sub-50ms with the composite index.

**Sizing.** ~25 seasons × (5 leagues + 5 cups + 3 UEFA) × ~500 player-stints/comp-season ≈ **150k rows**. Trivial for Postgres.

---

## 5. Game engine layer — table by table

### 5.1 `categories` (unchanged)

Seeded with `football` today; `general_trivia`, `geography`, etc. can be added later without code changes.

### 5.2 `question_templates` (NEW)

Hybrid model: **metadata in DB, materializer logic in code.** The template row stores everything an admin sees and tweaks; the actual SQL that produces the answer set lives in a Java class keyed by `materializer_key`.

```sql
CREATE TABLE question_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,

    slug            VARCHAR(64) NOT NULL UNIQUE,        -- 'team_league_goals_since'
    display_name    VARCHAR(255) NOT NULL,              -- 'Team goals in league since year'

    -- Text template with named placeholders.
    -- Example: 'Goals for {team_name} in {competition_name} since {start_year}'
    text_template   TEXT NOT NULL,

    -- JSON schema describing required params, their types, and how to enumerate them.
    -- Example:
    -- {
    --   "params": {
    --     "team_id":        {"type": "team_ref",        "enumerate": "competitions:[epl,laliga,seriea,bundesliga,ligue1]"},
    --     "competition_id": {"type": "competition_ref", "enumerate": "slugs:[epl,laliga,seriea,bundesliga,ligue1]"},
    --     "start_year":     {"type": "int",             "enumerate": "values:[2000]"}
    --   },
    --   "constraints": [
    --     "team must have played in competition for at least one season >= start_year"
    --   ]
    -- }
    param_schema    JSONB NOT NULL,

    -- The Java materializer class registered under this key.
    -- Example: 'football.team_competition_metric_since'
    materializer_key VARCHAR(64) NOT NULL,

    -- Default metric key passed to the materializer ('goals','assists','appearances'…).
    metric_key      VARCHAR(50) NOT NULL,

    -- Defaults applied to all generated questions from this template.
    default_min_score INTEGER,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

A small Java interface — `QuestionMaterializer` — registers itself with a key. The generator job reads templates, enumerates params, instantiates draft `questions`, and the materializer for each template fills in `answers`. No template → no materializer wiring.

### 5.3 `questions` (evolved)

Add: `template_id`, `template_params`, `status`.

```sql
ALTER TABLE questions
    ADD COLUMN template_id     UUID REFERENCES question_templates(id) ON DELETE SET NULL,
    ADD COLUMN template_params JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN status          VARCHAR(20) NOT NULL DEFAULT 'draft';
                                          -- 'draft' | 'active' | 'retired'

-- Hand-curated questions: template_id NULL, status starts 'draft'.
-- Auto-generated questions: template_id set, template_params holds the concrete bindings.

CREATE INDEX idx_questions_status ON questions (status);
CREATE INDEX idx_questions_template ON questions (template_id);
```

The existing `config` JSONB stays as the raw materializer input (denormalized snapshot of `template_params` joined with template defaults, so the materializer doesn't need to re-resolve).

**Lifecycle.** Generator inserts `status='draft'`. Admin UI lists drafts; admin promotes → `active` (materializer is invoked, answers row in). Retiring a question sets `status='retired'` and leaves answers in place for historical match replay.

### 5.4 `answers` (unchanged shape)

V2's shape is correct. Two notes:

- We do **not** add `breakdown` JSONB. Breakdown is computed on demand via a `GET /api/answers/{id}/breakdown` endpoint that re-queries stints, keyed by the materializer that produced the answer.
- A new column `materialized_at` is added so we can detect stale answers when stints update.

```sql
ALTER TABLE answers
    ADD COLUMN materialized_at TIMESTAMP NOT NULL DEFAULT NOW();
```

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

## 8. Migration / phasing

Each step is its own Flyway migration so we can pause and verify between them.

| Flyway | Scope | Notes |
|---|---|---|
| **V6** | Create `seasons`, `player_external_ids`, `team_external_ids`, `player_season_stints`. | Pure additive — no destructive ops. |
| **V7** | Create `question_templates`. Add `template_id`, `template_params`, `status`, `materialized_at` columns to `questions`/`answers`. | Backfill existing rows: `status='active'`, `template_id=NULL`. |
| **V8** | Python backfill script: read `players.career_stats` JSONB → write `player_season_stints` rows. Seed `seasons` and `team_external_ids`/`player_external_ids` (FBref). | Run as a one-off, not a migration. Verify parity with a checksum query (per-player goal totals before/after). |
| **V9** | Once parity confirmed: drop `players.career_stats`, drop `players.fbref_id`, drop `teams.fbref_id`. Add `NOT NULL` constraint on the new FBref `player_external_ids` row for every player. | Destructive — only after V8 verification. |
| **V10** | Add the first batch of `question_templates` (seed data) and run the generator job for the first time. | Drafts pile up in `questions`; admin promotes incrementally. |

The Python scraper changes in lockstep with V8: a new module writes stints directly, replacing the current JSONB-write path.

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

## 10. Open items to confirm before writing V6

These are small enough that I'll just pick a default if you don't push back, but flagging them explicitly:

1. **Match scope.** Assuming a match always runs questions from a single category (no mixed football+geography matches). If you want mixed, `matches` needs a category_id removal and `games` needs one per-game.
2. **Soft-delete vs hard-delete on stints.** Default: hard delete + re-insert on rescrape. Alternative: keep history with `effective_from/to`. Probably overkill for trivia.
3. **Season alignment for cups.** Assuming each domestic cup row maps to the league season it overlaps (FA Cup 2023-24 → `seasons.label='2023-24'`). UEFA competitions ditto.
4. **Player position evolution.** `players.primary_position` is a single column, not season-aware. If we want "primary position in 2023-24" later we'd add it to the stint. For now, single column is enough.
5. **Materializer concurrency.** When the stale-detector finds 5,000 answers to rematerialize after a weekly stint refresh, do we run serially or in a worker pool? Probably worker pool with per-question advisory lock. Won't affect schema.

Once these are signed off I'll write V6/V7 as the first concrete PR.
