# Trivia 501 — Scraper

Python scripts that populate the `player_season_stints` table from FBref.com, using
ScraperFC + undetected-chromedriver to bypass Cloudflare.

**Schema version:** V9 (post-migration). `players.career_stats` and `players.fbref_id`
have been dropped. Stats live exclusively in `player_season_stints`.

---

## Prerequisites

- Python 3.9+
- PostgreSQL 15+ (Flyway migrations V1–V11 applied)
- Chrome / Chromium (for undetected-chromedriver)

```bash
cd trivia-501-scraper
pip install -r requirements.txt
```

### Environment variables

Create a `.env` file (or set in your shell):

```bash
DATABASE_URL=postgresql://trivia501:password@localhost:5432/trivia501
CURRENT_SEASON=2025-2026   # default for scrape_current_season.py
START_YEAR=2000            # default for scrape_historical.py
FBREF_WAIT_TIME=7          # seconds between FBref requests — do not reduce
```

---

## Core scripts

### `scrape_historical.py` — one-off historical backfill

Scrapes all five Top-5 leagues from `settings.start_year` to present. Already run
against the live DB. Safe to re-run (all writes are upserts).

```bash
python scrape_historical.py                          # all leagues, all seasons
python scrape_historical.py --leagues "EPL,La Liga"  # subset of leagues
python scrape_historical.py --from-year 2020         # recent seasons only
python scrape_historical.py --dry-run                # parse + log, no DB writes
```

### `scrape_current_season.py` — weekly update

Delegates to `scrape_historical.py`'s shared functions for consistent upsert behaviour.
Run after each matchday.

```bash
python scrape_current_season.py                            # all leagues, current season
python scrape_current_season.py --season 2026-2027         # new season
python scrape_current_season.py --leagues "EPL,Bundesliga" # subset
python scrape_current_season.py --dry-run
```

After running, re-materialize stale answers:
```
POST /api/admin/questions/rematerialize-stale
```

### Utility scripts

| Script | Usage |
|---|---|
| `verify_2526.py` | Sanity-check 2025-26 stints exist; print top scorers |
| `inspect_player.py "Haaland"` | Show all stints for a player name substring |
| `inspect_fbref_season.py` | Dump FBref column names for a season (debug) |

---

## Architecture

```
FBref.com  →  ScraperFC + undetected-chromedriver
                         │
                         ▼
               Two passes per league × season:
               1. process_standard()    → appearances, goals, assists, cards, penalty stats
               2. process_goalkeeping() → clean_sheets, goals_conceded, is_goalkeeper

                         │  upsert_stint()  (None-sentinel for GK fields)
                         ▼
               player_season_stints        ← the keystone table
               player_external_ids         ← source='fbref'
               team_external_ids           ← source='fbref'
               scrape_jobs / scrape_run_logs

                         │  (Java pipeline, not Python)
                         ▼
               answers + entities          ← written by Java materializer only
```

### Database models

`database/models_v6.py` — SQLAlchemy ORM models reflecting the V9 schema:

| Class | Table | Notes |
|---|---|---|
| `Player` | `players` | No `fbref_id` or `career_stats` (dropped V9) |
| `Team` | `teams` | No `fbref_id` (dropped V9) |
| `Competition` | `competitions` | `fbref_id` kept (no competition_external_ids yet) |
| `Season` | `seasons` | `label`, `start_year`, `end_year`, `is_current` |
| `PlayerSeasonStint` | `player_season_stints` | Includes `penalty_goals`, `penalty_attempts` (added V8) |
| `PlayerExternalId` | `player_external_ids` | `source='fbref'`, `external_id=<fbref_id>` |
| `TeamExternalId` | `team_external_ids` | Same pattern |
| `ScrapeJob` | `scrape_jobs` | Per-scrape audit row |
| `ScrapeRunLog` | `scrape_run_logs` | Per-player error log |

---

## Java materializer pipeline

Python writes stints. Java turns stints into `answers`:

1. `POST /api/admin/templates/generate`
   — enumerates all valid (team, competition, season/start_year) combos and inserts
   draft `questions` for each.

2. Admin promotes: `PATCH /api/admin/questions/{id}/status {"status":"active"}`
   — triggers `QuestionMaterializerService`, which aggregates `player_season_stints` and
   writes `answers` + `entities`.

3. Weekly: `POST /api/admin/questions/rematerialize-stale`
   — re-runs materialization for any active question whose stints changed since last
   `materialized_at`.

---

## Project structure

```
trivia-501-scraper/
├── config.py                        # settings (DATABASE_URL, seasons, wait times)
├── scrape_historical.py             # main historical scraper + shared functions
├── scrape_current_season.py         # weekly updater (delegates to historical)
├── backfill_season_stints.py        # V8 one-off migration (do not re-run)
├── verify_2526.py                   # sanity check
├── inspect_player.py                # DB lookup utility
├── inspect_fbref_season.py          # FBref column-name dumper
├── populate_answers_v2.py           # DEPRECATED — raises SystemExit
├── database/
│   ├── __init__.py
│   └── models_v6.py                 # SQLAlchemy models (post-V9 schema)
├── requirements.txt
├── CURRENT_WORKFLOW.md              # step-by-step operational workflow
└── README.md                        # this file
```

---

## Troubleshooting

**Empty DataFrame / bot detection**
```bash
pip install --upgrade undetected-chromedriver ScraperFC
```

**FBref column names changed**
Run `inspect_fbref_season.py` to dump current column names, then update the column
mapping constants at the top of `scrape_historical.py`.

**Duplicate team rows**
Do not add a translation map. Merge the duplicate rows in the DB and add a
`team_external_ids` entry pointing both external IDs to the canonical team.

**Check scrape job history**
```sql
SELECT j.job_type, j.season, c.name, j.status, j.players_scraped, j.players_failed
FROM scrape_jobs j LEFT JOIN competitions c ON c.id = j.competition_id
ORDER BY j.started_at DESC LIMIT 20;
```

---

## License

For personal/educational use only. Respect FBref's terms of service and rate limits.
