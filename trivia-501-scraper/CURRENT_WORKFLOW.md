# Trivia 501 Scraper — Current Workflow

**Updated:** 2026-05-25
**Schema:** V9 (post-migration) — `player_season_stints` is the keystone; `career_stats` and `fbref_id` columns have been dropped.

---

## Overview

The scraper layer is a set of Python scripts that write directly to PostgreSQL.
There is no FastAPI microservice; scripts are invoked on-demand or on a schedule.

The **answers table is never written by Python**. Stats go into `player_season_stints`;
the Java materializer pipeline translates stints → answers when an admin promotes a
draft question to `active`.

```
FBref → Python scripts → player_season_stints
                                │
                     Java materializer (on admin promote)
                                │
                           answers + entities
                                │
                     Spring Boot game engine (read-only)
```

---

## Database Schema (post-V9)

### Core source table: `player_season_stints`

One row per (player, season, team, competition). This is the only table Python writes stats to.

```sql
CREATE TABLE player_season_stints (
    id               UUID PRIMARY KEY,
    player_id        UUID REFERENCES players(id),
    season_id        UUID REFERENCES seasons(id),
    team_id          UUID REFERENCES teams(id),
    competition_id   UUID REFERENCES competitions(id),
    appearances      SMALLINT NOT NULL DEFAULT 0,
    starts           SMALLINT NOT NULL DEFAULT 0,
    minutes          INTEGER  NOT NULL DEFAULT 0,
    goals            SMALLINT NOT NULL DEFAULT 0,
    assists          SMALLINT NOT NULL DEFAULT 0,
    penalty_goals    SMALLINT NOT NULL DEFAULT 0,
    penalty_attempts SMALLINT NOT NULL DEFAULT 0,
    yellow_cards     SMALLINT NOT NULL DEFAULT 0,
    red_cards        SMALLINT NOT NULL DEFAULT 0,
    clean_sheets     SMALLINT NOT NULL DEFAULT 0,
    goals_conceded   SMALLINT NOT NULL DEFAULT 0,
    is_goalkeeper    BOOLEAN  NOT NULL DEFAULT FALSE,
    source           VARCHAR(32) NOT NULL,
    source_scraped_at TIMESTAMP NOT NULL,
    UNIQUE (player_id, season_id, team_id, competition_id)
);
```

### Player identity (post-V9)

`players.fbref_id` and `players.career_stats` have been **dropped**. Player identity
is keyed through `player_external_ids`:

```sql
-- Look up a player by FBref ID
SELECT p.* FROM players p
JOIN player_external_ids e ON e.player_id = p.id
WHERE e.source = 'fbref' AND e.external_id = '<fbref_player_id>';
```

### Question lifecycle tables

| Table | Purpose |
|---|---|
| `question_templates` | Template definitions with `materializer_key` and `param_schema` |
| `questions` | Concrete questions (status: draft → active → retired) |
| `answers` | Pre-computed answer rows — written by Java materializer only |
| `entities` | Autocomplete pool — written by Java materializer alongside answers |

---

## Scripts

### `scrape_historical.py` — historical backfill

Scrapes all seasons for the five Top-5 leagues. Already run against the live DB
(all 130 league × season pairs complete). Safe to re-run — all writes are upserts.

```bash
# Full backfill (all leagues, from settings.start_year)
python scrape_historical.py

# Specific subset
python scrape_historical.py --leagues "EPL,La Liga" --from-year 2020

# Dry-run (parse + log, no DB writes)
python scrape_historical.py --dry-run
```

Two passes per league × season:
1. **Standard pass** — appearances, goals, assists, cards, penalty stats (all players)
2. **Goalkeeping pass** — clean_sheets, goals_conceded, is_goalkeeper (GK rows only)

The GK pass uses a `None`-sentinel pattern in `upsert_stint`: it **only** overwrites
clean_sheets/goals_conceded/is_goalkeeper; it never zeros those fields on an outfield
player who also happened to touch the standard sheet.

---

### `scrape_current_season.py` — weekly update

Delegates to `scrape_historical.py`'s shared functions. Run after each matchday.

```bash
# Current season (from settings.current_season), all leagues
python scrape_current_season.py

# Override season/leagues
python scrape_current_season.py --season 2026-2027 --leagues "EPL"

# Dry-run
python scrape_current_season.py --dry-run
```

After running, trigger the Java materializer:
```
POST /api/admin/questions/rematerialize-stale
```

---

### Utility scripts

| Script | Purpose |
|---|---|
| `verify_2526.py` | Count 2025-26 stints; print top scorer sample |
| `inspect_player.py [name]` | Show all stints for a player name substring |
| `inspect_fbref_season.py` | Dump FBref column names for a season (debug FBref changes) |
| `populate_answers_v2.py` | **DEPRECATED** — raises SystemExit immediately |

---

## Java Materializer Pipeline

Python does not write to `answers`. The flow is:

1. Admin calls `POST /api/admin/templates/generate`
   → `QuestionGeneratorService` reads active `question_templates`, enumerates valid
   param combos (team × competition × season/start_year), inserts draft `questions`.

2. Admin promotes a draft: `PATCH /api/admin/questions/{id}/status {"status":"active"}`
   → `QuestionMaterializerService` invokes the correct materializer:

   | Materializer key | Java class | Question shape |
   |---|---|---|
   | `football.team_competition_metric_since` | `FootballTeamCompetitionMetricSinceMaterializer` | "Goals for Arsenal in the PL since 2000" |
   | `football.team_competition_season_metric` | `FootballTeamCompetitionSeasonMaterializer` | "Goals for Arsenal in the UCL 2023-24" |

   The materializer aggregates `player_season_stints`, writes `answers`, and upserts
   player display names into `entities` (entity_type=`'footballer'`).

3. Active questions are re-materialized automatically when stints change:
   `POST /api/admin/questions/rematerialize-stale`

---

## Quick Start (new environment)

```bash
cd trivia-501-scraper
pip install -r requirements.txt

# Copy and fill in your DB credentials
cp .env.example .env

# Verify the DB is reachable and stints exist
python verify_2526.py

# If stints are missing, re-run historical scraper
python scrape_historical.py

# Weekly cadence (after matchday)
python scrape_current_season.py
# then: POST /api/admin/questions/rematerialize-stale
```

---

## Deprecated files

| File | Status | Replacement |
|---|---|---|
| `database/models_v4.py` | Superseded by `models_v6.py` | — |
| `init_db_v3.py` | Superseded by Flyway migrations V1–V11 | — |
| `init_questions_v2.py` | Superseded by Java `QuestionGeneratorService` | — |
| `populate_answers_v2.py` | Superseded by Java materializer pipeline | — |
| `backfill_season_stints.py` | One-off V8 migration script (do not re-run) | — |
| `scrape_all_premier_league_history.py` | Superseded by `scrape_historical.py` | — |

---

*See also: `docs/plans/DATABASE_DESIGN.md` and `docs/design/SCRAPING_SERVICE_OPERATIONS.md`.*
