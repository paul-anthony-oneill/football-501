# Trivia 501 — Scraping Service Operations Guide

**Version**: 2.0 (post-V9 schema)
**Date**: 2026-05-25
**Status**: Current — reflects V8–V11 implementation

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Script Reference](#script-reference)
4. [Operational Workflows](#operational-workflows)
5. [Java Materializer Pipeline](#java-materializer-pipeline)
6. [Data Flow](#data-flow)
7. [Configuration](#configuration)
8. [Error Handling & Troubleshooting](#error-handling--troubleshooting)

---

## Overview

The Trivia 501 scraping layer is a set of **Python scripts** that run directly against
PostgreSQL. There is no long-running FastAPI microservice — scripts are invoked on-demand
(initial backfill) or on a schedule (weekly current-season update).

Stats are written to `player_season_stints`. They are **not** written directly to the
`answers` table — that translation is done by the Java materializer pipeline after admin
promotion of a draft question.

### Key principles

| Principle | Detail |
|---|---|
| **Zero runtime impact** | Scripts run offline; no scraping occurs during live matches |
| **Two-pass per league** | Standard pass (outfield stats) → Goalkeeping pass (GK stats only) |
| **Upsert, never overwrite GK fields blindly** | `upsert_stint` uses a `None`-sentinel pattern — the standard pass never zeros clean_sheets/goals_conceded; only the GK pass sets them |
| **Real FBref IDs** | Player identity keyed through `player_external_ids(source='fbref')`. Synthetic `gen_` IDs used only when FBref provides no URL anchor |
| **Verbatim team names** | Team names are stored exactly as FBref reports them; no translation map |
| **Java owns the answers table** | Python writes stints; Java materializer aggregates stints → answers |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  FBref.com (via ScraperFC + undetected-chromedriver)        │
└───────────────────────────┬─────────────────────────────────┘
                            │  HTML → pandas DataFrame
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Python Scripts  (trivia-501-scraper/)                    │
│                                                             │
│  scrape_historical.py    — all past seasons (one-off)       │
│  scrape_current_season.py — current season (weekly)         │
│                                                             │
│  Shared functions (in scrape_historical.py):                │
│    build_fbref_client()                                     │
│    process_standard()   → upsert_player + upsert_stint      │
│    process_goalkeeping() → upsert_stint (GK fields only)    │
└───────────────────────────┬─────────────────────────────────┘
                            │  ON CONFLICT DO UPDATE
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  PostgreSQL — Football Source Layer                         │
│  ─ player_season_stints  (the keystone table)               │
│  ─ player_external_ids   (source='fbref')                   │
│  ─ team_external_ids     (source='fbref')                   │
│  ─ scrape_jobs / scrape_run_logs  (audit trail)             │
└───────────────────────────┬─────────────────────────────────┘
                            │
              POST /api/admin/templates/generate
              PATCH /api/admin/questions/{id}/status
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Java Materializer Pipeline  (Spring Boot)                  │
│  ─ QuestionGeneratorService    → draft questions            │
│  ─ QuestionMaterializerService → answers + entities         │
│    ├── FootballTeamCompetitionMetricSinceMaterializer        │
│    └── FootballTeamCompetitionSeasonMaterializer             │
└───────────────────────────┬─────────────────────────────────┘
                            │  reads answers table only
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Spring Boot Game Engine  (match validation)                │
│  NEVER queries player_season_stints during a live match     │
└─────────────────────────────────────────────────────────────┘
```

---

## Script Reference

All scripts live in `trivia-501-scraper/`.

### `scrape_historical.py`

**Purpose:** One-off backfill. Scrapes all seasons for all five Top-5 leagues (or a
specified subset). Writes standard stats first, then goalkeeping stats, for each
league × season pair.

**Usage:**
```bash
# All leagues, all seasons (default from settings.start_year)
python scrape_historical.py

# Specific leagues and/or starting year
python scrape_historical.py --leagues "EPL,La Liga" --from-year 2020

# Dry-run (parse and log; do not commit)
python scrape_historical.py --dry-run
```

**Supported leagues (`ALL_LEAGUES`):**

| `fbref_key` | DB name | Country |
|---|---|---|
| `EPL` | `English Premier League` | `England` |
| `La Liga` | `Spanish La Liga` | `Spain` |
| `Serie A` | `Italian Serie A` | `Italy` |
| `Bundesliga` | `German Bundesliga` | `Germany` |
| `Ligue 1` | `French Ligue 1` | `France` |

**Output per run:** One `ScrapeJob` row per (league, category), `ScrapeRunLog` entries
for errors, `player_season_stints` rows upserted.

---

### `scrape_current_season.py`

**Purpose:** Weekly update. Delegates all processing to `scrape_historical.py`'s shared
functions (`process_standard`, `process_goalkeeping`) to guarantee identical upsert
behaviour.

**Usage:**
```bash
# All leagues, current season (from settings.current_season)
python scrape_current_season.py

# Specific season and/or leagues
python scrape_current_season.py --season 2026-2027 --leagues "EPL,Bundesliga"

# Dry-run
python scrape_current_season.py --dry-run
```

**After running**, trigger the Java materializer to refresh any active questions whose
underlying stints have changed:
```
POST /api/admin/questions/rematerialize-stale
```

---

### `backfill_season_stints.py`

**Purpose:** One-off migration utility. Drained the old `players.career_stats` JSONB
column into `player_season_stints` rows as part of the V8 migration. **Do not re-run**
— `career_stats` was dropped in V9.

---

### `verify_2526.py`

Quick sanity check — queries `player_season_stints` for the 2025-26 season label and
prints a top-scorer sample.

```bash
python verify_2526.py
```

---

### `inspect_player.py`

Ad-hoc DB lookup. Shows all stints for a player name substring.

```bash
python inspect_player.py "Haaland"
```

---

### `inspect_fbref_season.py`

Fetches raw FBref DataFrames for a given season/league and dumps column names + row
counts to a text file. Useful for debugging column renames after FBref structure changes.

---

### `populate_answers_v2.py` (deprecated)

Raises `SystemExit` immediately. This script was the V4-era JSONB-based answer
populator. It must not be run. Use the Java materializer pipeline instead.

---

## Operational Workflows

### Workflow 1: Initial Historical Backfill (one-off, already complete)

```
1. Applied V8 Flyway migration (penalty_goals, penalty_attempts columns)
2. Ran backfill_season_stints.py  → drained career_stats JSONB
3. Ran scrape_historical.py       → scraped all 130 league × season pairs from FBref
4. Ran verify_parity.py           → confirmed zero-row diff vs old JSONB totals
5. Applied V9 Flyway migration    → dropped career_stats, fbref_id columns
```

This workflow is complete. The `player_season_stints` table is the live source of truth.

---

### Workflow 2: Weekly Current-Season Update

**Trigger:** After each matchday, or on a Sunday cron.

```bash
# 1. Scrape latest stats
python scrape_current_season.py --season 2025-2026

# 2. Tell Java to re-materialise any answers whose stints changed
POST /api/admin/questions/rematerialize-stale
```

Each `ScrapeJob` row records `players_scraped`, `players_failed`, and `status`
(`success`/`partial`/`failed`). Check `scrape_run_logs` for per-player errors.

---

### Workflow 3: New Season Kickoff

At the start of a new season (e.g. 2026-27):

```bash
# 1. Update settings.current_season in config.py
# 2. Run the scraper once after the season's first matchday
python scrape_current_season.py --season 2026-2027

# 3. Generate draft questions for the new season
POST /api/admin/templates/generate

# 4. Admin reviews drafts and promotes incrementally
PATCH /api/admin/questions/{id}/status   body: {"status":"active"}
```

The template generator creates one draft question per valid (team, competition, season)
combo. Promoting a draft triggers materialization automatically.

---

### Workflow 4: Adding a New Competition

1. Insert a `competitions` row with the correct `competition_type`:
   - `domestic_league`, `domestic_cup`, or `continental_club`
2. Add the competition to `scrape_historical.py`'s `ALL_LEAGUES` list (or scrape it
   separately via `fb.scrape_stats(season, fbref_key, "standard")`).
3. Run the scraper for historical seasons if needed.
4. Run `POST /api/admin/templates/generate` — the generator will pick up any new
   (team, competition) combos automatically.

---

### Workflow 5: Game Match Flow (no scraping involved)

```
Player submits answer "Erling Haaland"
  └─> Spring Boot AnswerEvaluator
        └─> SELECT * FROM answers WHERE question_id=? ...
            (fuzzy match via pg_trgm index)
        └─> Returns { score: 27, is_valid_darts: true, is_bust: false }
  └─> Game engine deducts from score
  └─> WebSocket broadcasts result

No scraping, no player_season_stints query, instant response.
```

---

## Java Materializer Pipeline

### Question lifecycle

```
draft  →  active  →  retired
   ▲          │
   └──────────┘  (re-activate)
```

- **draft**: created by `QuestionGeneratorService` during `POST /api/admin/templates/generate`
- **active**: admin promotes via `PATCH /api/admin/questions/{id}/status {"status":"active"}`
  → `QuestionMaterializerService.materialize()` runs immediately, writing `answers` rows
- **retired**: admin retires; answers stay for historical replay

### Materializer keys

| Key | Java class | Question shape |
|---|---|---|
| `football.team_competition_metric_since` | `FootballTeamCompetitionMetricSinceMaterializer` | "Goals for Arsenal in the Premier League since 2000" |
| `football.team_competition_season_metric` | `FootballTeamCompetitionSeasonMaterializer` | "Goals for Arsenal in the Champions League 2023-24" |

### Admin endpoints

| Endpoint | Effect |
|---|---|
| `POST /api/admin/templates/generate` | Enumerate all valid param combos across active templates; insert draft questions for new combos |
| `PATCH /api/admin/questions/{id}/status` | Transition lifecycle state; activating triggers materialization |
| `POST /api/admin/questions/rematerialize-stale` | Re-run materializer for all active questions whose `materialized_at` is older than any of their stints' `updated_at` |

---

## Data Flow

### Scraping → stints

```
FBref HTML
  │  (ScraperFC + undetected-chromedriver)
  ▼
pandas DataFrame (standard or goalkeeping)
  │  process_standard() / process_goalkeeping()
  ▼
upsert_player()         → players + player_external_ids
upsert_team()           → teams  + team_external_ids
upsert_stint()          → player_season_stints
  ON CONFLICT DO UPDATE   (GK fields only updated by goalkeeping pass)
```

### Stints → answers

```
player_season_stints
  │  QuestionMaterializerService.materialize(question)
  │  → FootballTeamCompetitionMetricSinceMaterializer  OR
  │    FootballTeamCompetitionSeasonMaterializer
  │
  │  SELECT player_id, SUM(goals|appearances|assists|clean_sheets)
  │  GROUP BY player_id
  │  HAVING SUM(...) > 0
  ▼
answers          (question_id, answer_key, display_text, score, …)
entities         (entity_type='footballer', name)
```

### Answers → game engine

```
answers  ←  AnswerEvaluator (fuzzy match, darts validation)
```

---

## Configuration

### `config.py` / `.env`

```bash
# Database
DATABASE_URL=postgresql://trivia501:password@localhost:5432/trivia501

# FBref
FBREF_WAIT_TIME=7        # seconds between requests (do not reduce below 7)
FBREF_MAX_RETRIES=3

# Season
CURRENT_SEASON=2025-2026  # used by scrape_current_season.py as default
START_YEAR=2000           # used by scrape_historical.py as default start
```

### League coverage (`ALL_LEAGUES` in `scrape_historical.py`)

```python
ALL_LEAGUES = [
    {"fbref_key": "EPL",        "db_name": "English Premier League",  "country": "England"},
    {"fbref_key": "La Liga",    "db_name": "Spanish La Liga",         "country": "Spain"},
    {"fbref_key": "Serie A",    "db_name": "Italian Serie A",         "country": "Italy"},
    {"fbref_key": "Bundesliga", "db_name": "German Bundesliga",       "country": "Germany"},
    {"fbref_key": "Ligue 1",    "db_name": "French Ligue 1",         "country": "France"},
]
```

---

## Error Handling & Troubleshooting

### Cloudflare / bot detection

ScraperFC uses `undetected-chromedriver` to bypass Cloudflare. If scraping starts
returning empty DataFrames:

1. Update `undetected-chromedriver`: `pip install --upgrade undetected-chromedriver`
2. Check the `fbref_inspect_*.txt` files produced by `inspect_fbref_season.py` — if
   column names have changed, update the column mapping in `process_standard` /
   `process_goalkeeping`.

### Empty DataFrame for a league/season

- Check `scrape_run_logs` (level=`'ERROR'`) for the failed `ScrapeJob`.
- The job status will be `'skipped'` if the DataFrame was empty, `'failed'` if
  `scrape_stats` threw an exception.
- Re-run the affected league: `python scrape_historical.py --leagues "EPL" --from-year 2024`

### Duplicate team rows

Team names are stored verbatim as FBref reports them. If two rows appear for what should
be one team (e.g. "Manchester United" vs "Manchester Utd"), do **not** add a translation
map — instead merge the rows and add a `team_external_ids` entry pointing both to the
same team. The `db_name` in `ALL_LEAGUES` must match the `competitions.name` column in
the database.

### Player deduplication

Primary lookup: `player_external_ids` JOIN `players` WHERE `source='fbref' AND external_id=<fbref_id>`.
Fallback (synthetic IDs only): `players.normalized_name`. If duplicates appear:

```sql
-- Find duplicate normalized names
SELECT normalized_name, COUNT(*) FROM players GROUP BY normalized_name HAVING COUNT(*) > 1;
```

Merge by reassigning all `player_season_stints` rows to the canonical player, then
deleting the duplicate.

### FBref rate limiting

The scraper enforces a 7-second wait between requests. Do not reduce this. If a 429
response is detected, the scraper retries with exponential backoff up to `FBREF_MAX_RETRIES`.

### Checking scrape job history

```sql
SELECT j.id, j.job_type, j.season, c.name AS competition,
       j.status, j.players_scraped, j.players_failed, j.started_at, j.completed_at
FROM scrape_jobs j
LEFT JOIN competitions c ON c.id = j.competition_id
ORDER BY j.started_at DESC
LIMIT 50;
```

---

*See also: `DATABASE_DESIGN.md` for the full schema; `trivia-501-scraper/README.md` for setup and quick-start.*
