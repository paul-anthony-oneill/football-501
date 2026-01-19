# ScraperFC Integration for Football 501

**Date**: 2026-01-18
**Status**: Proposed Implementation
**Priority**: MVP Alternative Data Source

## Executive Summary

ScraperFC is a **viable alternative** to API-Football for populating Football 501's question/answer database with historical player statistics. It offers **unlimited free access** to FBref data (StatsBomb/Opta quality) at the cost of increased implementation complexity (Python microservice).

**Recommendation**: Implement ScraperFC as a **Python microservice** that runs batch jobs to populate the PostgreSQL database, keeping the Spring Boot backend unchanged.

---

## ScraperFC Overview

### What is ScraperFC?

- **Python package** for scraping soccer data from multiple sources
- **6 data sources**: FBref, Transfermarkt, Understat, Sofascore, Capology, ClubElo
- **Active maintenance**: 338 commits, 19 releases (GPL-3.0 license)
- **Installation**: `pip install ScraperFC`
- **Documentation**: https://scraperfc.readthedocs.io

### Key Features for Football 501

✅ **Free & Unlimited**: No API rate limits (respects FBref's 10 req/min guideline)
✅ **Historical Data**: Access to seasons dating back to 1888 on FBref
✅ **StatsBomb Quality**: FBref uses same Opta data as API-Football
✅ **Comprehensive Stats**: Appearances, goals, clean sheets, assists, etc.
✅ **Multiple Leagues**: EPL, La Liga, Serie A, Bundesliga, Ligue 1, Champions League, World Cup, and more
✅ **Pandas DataFrames**: Clean, structured data ready for database insertion

### Limitations & Considerations

⚠️ **Web Scraping**: Fragile to FBref HTML changes (requires maintenance)
⚠️ **Rate Limiting**: 7-second wait between requests (slower than API)
⚠️ **Legal Use**: FBref permits personal/educational use only (not commercial analytics services)
⚠️ **Language Barrier**: Python package requires microservice architecture for Java integration
⚠️ **Data Freshness**: Best for historical data; live updates require scheduled jobs

---

## ScraperFC API Reference

### Installation & Setup

```python
from ScraperFC.fbref import FBref
import pandas as pd

# Initialize scraper with 7-second wait between requests
fb_scraper = FBref(wait_time=7)
```

### Core Methods

#### 1. `scrape_stats(year, league, stat_category)`

Returns tuple of 3 DataFrames: `(squad_stats, opponent_stats, player_stats)`

**Parameters**:
- `year`: Season string (e.g., "2023-2024", "2022-2023")
- `league`: League identifier from `comps.yaml` (e.g., "EPL", "La Liga")
- `stat_category`: Stat type (see Available Stat Categories below)

**Example**:
```python
# Get EPL 2023-24 player appearances and goals
squad_df, opponent_df, player_df = fb_scraper.scrape_stats(
    "2023-2024",
    "EPL",
    "standard"
)

# player_df columns include:
# - Player (name)
# - Nation
# - Pos (position)
# - Squad (team name)
# - Age
# - Playing_Time_MP (matches played = appearances)
# - Playing_Time_Starts
# - Playing_Time_Min (minutes)
# - Performance_Gls (goals)
# - Performance_Ast (assists)
# - ... and many more
```

#### 2. `scrape_all_stats(year, league)`

Returns dictionary with ALL stat categories for a season.

**Example**:
```python
all_stats = fb_scraper.scrape_all_stats("2023-2024", "EPL")
# Returns dict: {"standard": (squad, opp, player), "shooting": (...), ...}
```

#### 3. `scrape_matches(year, league)`

Returns DataFrame of all matches in a season (for match-level data).

#### 4. `get_valid_seasons(league)`

Returns list of available seasons for a league.

```python
valid_seasons = fb_scraper.get_valid_seasons("EPL")
# Returns: ["2024-2025", "2023-2024", "2022-2023", ...]
```

### Available Stat Categories

For `scrape_stats()` and `scrape_all_stats()`:

| Stat Category | Description | Relevant for Football 501? |
|--------------|-------------|----------------------------|
| `standard` | Appearances, goals, assists, minutes | ✅ **PRIMARY** |
| `shooting` | Shots, shots on target, conversion rate | ❌ |
| `passing` | Pass completion, key passes, progressive passes | ❌ |
| `passing_types` | Through balls, crosses, corners | ❌ |
| `gca` | Goal/Shot Creating Actions | ❌ |
| `defense` | Tackles, interceptions, blocks | ❌ |
| `possession` | Touches, take-ons, carries | ❌ |
| `playing_time` | Match appearances, substitutions | ✅ **USEFUL** |
| `misc` | Yellow/red cards, fouls, offsides | ❌ |
| `keeper` | Saves, goals against, clean sheets | ✅ **GOALKEEPERS** |
| `keeper_adv` | Advanced GK metrics (PSxG, etc.) | ❌ |

**For Football 501**: Focus on `"standard"`, `"playing_time"`, and `"keeper"`.

### Supported Leagues

From `comps.yaml` (access via `list(fb_scraper.comps.keys())`):

**Confirmed Leagues**:
- English Premier League ("EPL")
- La Liga
- Serie A
- Bundesliga
- Ligue 1
- Champions League
- Europa League
- Europa Conference League
- Copa Libertadores
- World Cup

**League Coverage**: 100+ competitions available on FBref (check `comps.yaml` for complete list).

---

## Implementation Architecture

### Recommended Architecture: Python Microservice

```
┌─────────────────────────────────────────────────────────────┐
│                    Football 501 Ecosystem                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         ScraperFC Python Microservice               │   │
│  │  (Standalone Flask/FastAPI service)                 │   │
│  │                                                       │   │
│  │  • Runs scheduled batch jobs (cron/APScheduler)     │   │
│  │  • Scrapes FBref historical data                    │   │
│  │  • Transforms to Football 501 schema                │   │
│  │  • Writes directly to PostgreSQL                    │   │
│  │  • Exposes health check endpoint                    │   │
│  └────────────────┬────────────────────────────────────┘   │
│                   │                                          │
│                   │ Direct PostgreSQL Connection             │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           PostgreSQL Database                        │  │
│  │  • questions table (pre-populated)                   │  │
│  │  • answers table (player stats cache)                │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                          │
│                   │ Read-only queries (during matches)       │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        Spring Boot Backend (Java)                    │  │
│  │  • Game engine (unchanged)                           │  │
│  │  • WebSocket handlers                                │  │
│  │  • REST API                                          │  │
│  │  • NO changes to existing code                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Why Microservice Architecture?

1. **Zero Impact on Spring Boot**: No code changes to existing Java backend
2. **Language Isolation**: Python and Java run independently
3. **Scheduled Jobs**: Python handles batch scraping (daily/weekly)
4. **Simple Deployment**: Can run as Docker container or standalone script
5. **Future Flexibility**: Easy to add API-Football later without refactoring

---

## Database Schema Alignment

ScraperFC data maps cleanly to Football 501's database schema:

### `answers` Table Population

**Football 501 Schema** (from CLAUDE.md):
```sql
CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    player_name VARCHAR(255) NOT NULL,
    player_api_id INTEGER,  -- FBref doesn't provide IDs
    statistic_value INTEGER NOT NULL,  -- appearances/goals/etc.
    is_valid_darts_score BOOLEAN NOT NULL,
    is_bust BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Trigram index for fuzzy player name matching
CREATE INDEX idx_answers_player_name_trgm
ON answers USING gin(player_name gin_trgm_ops);
```

**ScraperFC DataFrame Mapping**:
```python
# From fb_scraper.scrape_stats("2023-2024", "EPL", "standard")
player_df = ...

# Map DataFrame columns to Football 501 schema
answers_data = {
    'player_name': player_df['Player'],           # Direct match
    'player_api_id': None,                         # FBref doesn't expose IDs
    'statistic_value': player_df['Playing_Time_MP'],  # For appearances
    # OR
    'statistic_value': player_df['Performance_Gls'],  # For goals
    # OR
    'statistic_value': (
        player_df['Playing_Time_MP'] + player_df['Performance_Gls']
    ),  # For combined stats
    'is_valid_darts_score': statistic_value not in [163, 166, 169, 172, 173, 175, 176, 178, 179],
    'is_bust': statistic_value > 180
}
```

### Question Type Implementations

#### 1. Team League Appearances
**Question**: "Appearances for Manchester United in Premier League 2023-24"

**ScraperFC Query**:
```python
_, _, player_df = fb_scraper.scrape_stats("2023-2024", "EPL", "standard")
man_utd_players = player_df[player_df['Squad'] == 'Manchester Utd']

# Insert into answers table
for _, player in man_utd_players.iterrows():
    insert_answer(
        player_name=player['Player'],
        statistic_value=player['Playing_Time_MP'],  # Appearances
        question_id=question_id
    )
```

#### 2. Combined Stats (Appearances + Goals)
**Question**: "Appearances + Goals for Liverpool in Premier League 2023-24"

```python
_, _, player_df = fb_scraper.scrape_stats("2023-2024", "EPL", "standard")
liverpool_players = player_df[player_df['Squad'] == 'Liverpool']

for _, player in liverpool_players.iterrows():
    combined_stat = player['Playing_Time_MP'] + player['Performance_Gls']
    insert_answer(
        player_name=player['Player'],
        statistic_value=combined_stat,
        question_id=question_id
    )
```

#### 3. Goalkeeper Stats (Appearances + Clean Sheets)
**Question**: "Appearances + Clean Sheets for Arsenal in Premier League 2023-24"

```python
# Need to use "keeper" stat category
_, _, keeper_df = fb_scraper.scrape_stats("2023-2024", "EPL", "keeper")
arsenal_keepers = keeper_df[keeper_df['Squad'] == 'Arsenal']

for _, keeper in arsenal_keepers.iterrows():
    combined_stat = keeper['Playing_Time_MP'] + keeper['Performance_CS']  # CS = Clean Sheets
    insert_answer(
        player_name=keeper['Player'],
        statistic_value=combined_stat,
        question_id=question_id
    )
```

#### 4. International Appearances
**Question**: "Appearances for England in World Cup 2022"

```python
# ScraperFC supports international competitions
_, _, player_df = fb_scraper.scrape_stats("2022", "World Cup", "standard")
england_players = player_df[player_df['Squad'] == 'England']

for _, player in england_players.iterrows():
    insert_answer(
        player_name=player['Player'],
        statistic_value=player['Playing_Time_MP'],
        question_id=question_id
    )
```

#### 5. Nationality Filter
**Question**: "Appearances in Premier League 2023-24 by Brazilian players"

```python
_, _, player_df = fb_scraper.scrape_stats("2023-2024", "EPL", "standard")
brazilian_players = player_df[player_df['Nation'].str.contains('BRA', na=False)]

for _, player in brazilian_players.iterrows():
    insert_answer(
        player_name=player['Player'],
        statistic_value=player['Playing_Time_MP'],
        question_id=question_id
    )
```

---

## Implementation Plan

### Phase 1: Proof of Concept (1-2 days)

**Goal**: Validate ScraperFC works for Football 501's use cases.

```python
# poc_scraperfc.py
from ScraperFC.fbref import FBref
import pandas as pd

def test_scraper():
    fb = FBref(wait_time=7)

    # Test 1: EPL 2023-24 standard stats
    _, _, players = fb.scrape_stats("2023-2024", "EPL", "standard")
    print(f"✅ Scraped {len(players)} EPL players")
    print(f"Columns: {list(players.columns)}")

    # Test 2: Check Man City players
    man_city = players[players['Squad'] == 'Manchester City']
    print(f"\n✅ Man City players: {len(man_city)}")
    print(man_city[['Player', 'Playing_Time_MP', 'Performance_Gls']].head())

    # Test 3: Valid darts scores check
    for stat in man_city['Playing_Time_MP']:
        if stat in [163, 166, 169, 172, 173, 175, 176, 178, 179]:
            print(f"⚠️ Invalid darts score found: {stat}")

    # Test 4: Goalkeeper stats
    _, _, keepers = fb.scrape_stats("2023-2024", "EPL", "keeper")
    print(f"\n✅ Goalkeeper stats: {len(keepers)} keepers")
    print(keepers[['Player', 'Performance_CS']].head())  # CS = Clean Sheets

    return players

if __name__ == "__main__":
    test_scraper()
```

**Success Criteria**:
- ✅ Successfully scrape EPL 2023-24 data
- ✅ Extract player names, appearances, goals
- ✅ Verify data quality (no null values for key stats)
- ✅ Confirm team names match expected format

### Phase 2: Python Microservice (3-5 days)

**Technology Stack**:
- **Framework**: FastAPI (lightweight, async-capable)
- **Database**: `psycopg2` or SQLAlchemy for PostgreSQL
- **Scheduling**: APScheduler for cron-like jobs
- **Deployment**: Docker container

**Project Structure**:
```
football-501-scraper/
├── Dockerfile
├── requirements.txt
├── config.py              # Database connection, leagues config
├── scrapers/
│   ├── __init__.py
│   ├── fbref_scraper.py   # ScraperFC wrapper
│   └── data_transformer.py # Transform to Football 501 schema
├── jobs/
│   ├── __init__.py
│   ├── populate_questions.py  # Batch job to scrape and populate DB
│   └── scheduler.py       # APScheduler setup
├── database/
│   ├── __init__.py
│   ├── models.py          # SQLAlchemy models
│   └── crud.py            # Database operations
├── api/
│   ├── __init__.py
│   └── main.py            # FastAPI health check endpoint
└── tests/
    └── test_scraper.py
```

**Core Components**:

#### 1. `fbref_scraper.py`
```python
from ScraperFC.fbref import FBref
import pandas as pd
from typing import Tuple, List

class FootballStatsScraper:
    def __init__(self, wait_time: int = 7):
        self.scraper = FBref(wait_time=wait_time)

    def get_team_appearances(
        self,
        season: str,
        league: str,
        team_name: str
    ) -> pd.DataFrame:
        """
        Get player appearances for a specific team.

        Args:
            season: "2023-2024"
            league: "EPL"
            team_name: "Manchester United"

        Returns:
            DataFrame with player stats
        """
        _, _, player_df = self.scraper.scrape_stats(season, league, "standard")
        return player_df[player_df['Squad'] == team_name]

    def get_combined_stats(
        self,
        season: str,
        league: str,
        team_name: str,
        stat_type: str = "appearances_goals"
    ) -> pd.DataFrame:
        """
        Get combined statistics (appearances + goals/clean sheets).
        """
        if "keeper" in stat_type.lower():
            _, _, df = self.scraper.scrape_stats(season, league, "keeper")
        else:
            _, _, df = self.scraper.scrape_stats(season, league, "standard")

        team_df = df[df['Squad'] == team_name].copy()

        if stat_type == "appearances_goals":
            team_df['combined_stat'] = (
                team_df['Playing_Time_MP'] + team_df['Performance_Gls']
            )
        elif stat_type == "keeper_clean_sheets":
            team_df['combined_stat'] = (
                team_df['Playing_Time_MP'] + team_df['Performance_CS']
            )

        return team_df
```

#### 2. `data_transformer.py`
```python
import pandas as pd
from typing import Dict, List

INVALID_DARTS_SCORES = {163, 166, 169, 172, 173, 175, 176, 178, 179}

def transform_to_answers(
    player_df: pd.DataFrame,
    question_id: int,
    stat_column: str = 'Playing_Time_MP'
) -> List[Dict]:
    """
    Transform ScraperFC DataFrame to Football 501 answers schema.

    Returns:
        List of dicts ready for database insertion
    """
    answers = []

    for _, row in player_df.iterrows():
        stat_value = int(row[stat_column])

        answer = {
            'question_id': question_id,
            'player_name': row['Player'],
            'player_api_id': None,  # FBref doesn't provide IDs
            'statistic_value': stat_value,
            'is_valid_darts_score': stat_value not in INVALID_DARTS_SCORES,
            'is_bust': stat_value > 180
        }
        answers.append(answer)

    return answers
```

#### 3. `populate_questions.py`
```python
from scrapers.fbref_scraper import FootballStatsScraper
from scrapers.data_transformer import transform_to_answers
from database.crud import insert_answers_batch
import logging

logger = logging.getLogger(__name__)

def populate_epl_2023_24():
    """
    Populate all EPL 2023-24 questions.
    This would be run as a scheduled job.
    """
    scraper = FootballStatsScraper()

    # Example: Manchester United appearances
    logger.info("Scraping Manchester United appearances...")
    man_utd_df = scraper.get_team_appearances("2023-2024", "EPL", "Manchester Utd")

    question_id = 1  # Get from questions table
    answers = transform_to_answers(man_utd_df, question_id, 'Playing_Time_MP')

    insert_answers_batch(answers)
    logger.info(f"✅ Inserted {len(answers)} answers for question {question_id}")

    # Repeat for other teams/questions...

def populate_all_historical_data():
    """
    Populate multiple seasons and leagues.
    Run once for initial database seeding.
    """
    scraper = FootballStatsScraper()

    seasons = ["2023-2024", "2022-2023", "2021-2022"]
    leagues = ["EPL", "La Liga", "Serie A"]

    for season in seasons:
        for league in leagues:
            logger.info(f"Scraping {league} {season}...")
            # ... scraping logic
```

#### 4. `main.py` (FastAPI)
```python
from fastapi import FastAPI
from jobs.scheduler import start_scheduler

app = FastAPI(title="Football 501 Scraper Service")

@app.on_event("startup")
async def startup_event():
    # Start background scheduler for periodic scraping
    start_scheduler()

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "scraperfc-microservice"}

@app.post("/scrape/trigger")
async def trigger_scrape():
    """Manual trigger for scraping (admin only)"""
    # Trigger immediate scraping job
    pass
```

### Phase 3: Database Integration (2-3 days)

**PostgreSQL Connection**:
```python
# config.py
import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://football501:dev_password@localhost:5432/football501"
)

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)
```

**CRUD Operations**:
```python
# database/crud.py
from sqlalchemy.orm import Session
from typing import List, Dict

def insert_answers_batch(answers: List[Dict]):
    """Batch insert answers into database"""
    db = SessionLocal()
    try:
        for answer in answers:
            db.execute(
                """
                INSERT INTO answers
                (question_id, player_name, player_api_id, statistic_value,
                 is_valid_darts_score, is_bust)
                VALUES (:question_id, :player_name, :player_api_id,
                        :statistic_value, :is_valid_darts_score, :is_bust)
                """,
                answer
            )
        db.commit()
    except Exception as e:
        db.rollback()
        raise e
    finally:
        db.close()
```

### Phase 4: Deployment (1-2 days)

**Docker Setup**:
```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Run FastAPI server
CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8001"]
```

**docker-compose.yml** (for local development):
```yaml
version: '3.8'

services:
  scraper-service:
    build: ./football-501-scraper
    ports:
      - "8001:8001"
    environment:
      - DATABASE_URL=postgresql://football501:dev_password@postgres:5432/football501
    depends_on:
      - postgres

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=football501
      - POSTGRES_USER=football501
      - POSTGRES_PASSWORD=dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## Scheduling Strategy

### Initial Population (One-Time Job)

**Goal**: Populate database with 2-3 seasons of historical data for MVP leagues.

**MVP Leagues**:
- English Premier League (EPL)
- La Liga
- Champions League

**Seasons**: 2023-24, 2022-23, 2021-22

**Estimated Time**:
- 3 leagues × 3 seasons × 20 teams = 180 scraping operations
- At 7 seconds per request = 21 minutes total
- With margin for error: ~30-45 minutes

**Script**:
```python
def initial_population():
    scraper = FootballStatsScraper()

    leagues = ["EPL", "La Liga", "Champions League"]
    seasons = ["2023-2024", "2022-2023", "2021-2022"]

    for league in leagues:
        for season in seasons:
            _, _, df = scraper.scraper.scrape_stats(season, league, "standard")

            # Get all unique teams
            teams = df['Squad'].unique()

            for team in teams:
                team_df = df[df['Squad'] == team]

                # Create question in database
                question_id = create_question(
                    text=f"Appearances for {team} in {league} {season}",
                    league=league,
                    season=season,
                    team=team
                )

                # Insert answers
                answers = transform_to_answers(team_df, question_id)
                insert_answers_batch(answers)

                logger.info(f"✅ {team} {season} complete")
```

### Periodic Updates (Weekly/Monthly)

**Weekly Update** (Sundays 3 AM UTC):
- Scrape current season data for active leagues
- Update existing answers if stats have changed
- Add new players who appeared in recent matches

**Monthly Update**:
- Add new leagues/competitions
- Backfill historical seasons as needed

```python
from apscheduler.schedulers.background import BackgroundScheduler

def start_scheduler():
    scheduler = BackgroundScheduler()

    # Weekly update every Sunday at 3 AM UTC
    scheduler.add_job(
        update_current_season,
        'cron',
        day_of_week='sun',
        hour=3,
        minute=0
    )

    scheduler.start()

def update_current_season():
    """Update stats for ongoing 2024-25 season"""
    scraper = FootballStatsScraper()
    leagues = ["EPL", "La Liga", "Serie A"]

    for league in leagues:
        _, _, df = scraper.scraper.scrape_stats("2024-2025", league, "standard")
        # Update database...
```

---

## Advantages vs API-Football

### ScraperFC Advantages

✅ **Cost**: Free (unlimited scraping)
✅ **Historical Data**: Easy access to decades of data
✅ **No Rate Limits**: Only respect 10 req/min guideline (vs 100 req/day)
✅ **Rich Stats**: Multiple stat categories beyond basic appearances/goals
✅ **League Coverage**: 100+ competitions
✅ **Data Quality**: StatsBomb/Opta (same as API-Football)

### API-Football Advantages

✅ **Reliability**: Official API (less likely to break)
✅ **Legal Clarity**: Commercial use permitted with paid plans
✅ **Player IDs**: Consistent player identifiers across seasons
✅ **Real-time Data**: Live match statistics
✅ **Direct Integration**: REST API works with Java without microservice
✅ **Support**: Official support channels

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| FBref HTML changes break scraper | High | Medium | Monitor ScraperFC updates; have fallback data |
| ScraperFC package abandoned | Medium | Low | Package is actively maintained; could fork if needed |
| Python-Java integration complexity | Low | Low | Microservice architecture isolates concerns |
| Data quality issues | Medium | Low | Validate scraped data before DB insertion |

### Legal Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| FBref ToS violation | High | Medium | Use only for personal/educational (MVP); switch to paid API for commercial launch |
| DMCA takedown | High | Low | Host data, not scraper; can claim fair use for education |

**Recommendation**: Use ScraperFC for MVP development and testing. Before commercial launch, migrate to API-Football paid tier or negotiate FBref licensing.

---

## Testing Strategy

### Unit Tests

```python
# tests/test_scraper.py
import pytest
from scrapers.fbref_scraper import FootballStatsScraper

def test_scrape_epl_returns_data():
    scraper = FootballStatsScraper()
    df = scraper.get_team_appearances("2023-2024", "EPL", "Manchester City")

    assert len(df) > 0
    assert 'Player' in df.columns
    assert 'Playing_Time_MP' in df.columns

def test_invalid_darts_scores_flagged():
    scraper = FootballStatsScraper()
    df = scraper.get_team_appearances("2023-2024", "EPL", "Arsenal")

    for stat in df['Playing_Time_MP']:
        is_valid = stat not in {163, 166, 169, 172, 173, 175, 176, 178, 179}
        # Verify our logic matches Football 501 rules
```

### Integration Tests

```python
def test_end_to_end_population():
    """Test complete flow: scrape → transform → insert → verify"""
    scraper = FootballStatsScraper()

    # Scrape
    df = scraper.get_team_appearances("2023-2024", "EPL", "Liverpool")

    # Transform
    question_id = 999  # Test question
    answers = transform_to_answers(df, question_id)

    # Insert
    insert_answers_batch(answers)

    # Verify
    db_answers = get_answers_by_question(question_id)
    assert len(db_answers) == len(answers)
```

---

## Estimated Implementation Timeline

| Phase | Duration | Description |
|-------|----------|-------------|
| Phase 1: PoC | 1-2 days | Validate ScraperFC works |
| Phase 2: Microservice | 3-5 days | Build FastAPI service |
| Phase 3: DB Integration | 2-3 days | Connect to PostgreSQL |
| Phase 4: Deployment | 1-2 days | Docker setup |
| **Total** | **7-12 days** | **Full implementation** |

**Note**: This assumes 1 developer working part-time. Can be parallelized with Spring Boot development.

---

## Recommended Next Steps

1. **✅ Approve this document** - Confirm ScraperFC is the chosen approach
2. **Run PoC** - Execute `poc_scraperfc.py` to validate data quality
3. **Set up Python environment** - Create `football-501-scraper/` directory
4. **Initial scraping** - Populate EPL 2023-24 data for testing
5. **Microservice development** - Build FastAPI service
6. **Database integration** - Connect to Football 501 PostgreSQL
7. **Testing** - Validate data in Spring Boot game engine
8. **Deployment** - Dockerize and deploy alongside Spring Boot

---

## Alternative: Hybrid Approach (ScraperFC + API-Football)

If you want the best of both worlds:

**ScraperFC**:
- Historical data (2015-2023 seasons)
- Niche leagues (lower divisions, international)
- Initial database seeding

**API-Football**:
- Current season (2024-25 live updates)
- Real-time match stats
- Production reliability

**Implementation**:
```python
def get_player_stats(season, league, team):
    if season < "2024-2025":
        # Use ScraperFC for historical
        return scrape_fbref(season, league, team)
    else:
        # Use API-Football for current season
        return api_football_client.get_stats(season, league, team)
```

---

## Conclusion

ScraperFC is a **strong alternative** to API-Football for Football 501's MVP, offering:

- ✅ **Free unlimited access** to StatsBomb-quality data
- ✅ **Easy Python implementation** with minimal learning curve
- ✅ **Rich historical data** for question variety
- ✅ **Microservice architecture** that doesn't impact Spring Boot

**Trade-offs**:
- ⚠️ Requires Python microservice (adds deployment complexity)
- ⚠️ Legal gray area for commercial use
- ⚠️ Maintenance risk if FBref changes HTML

**Recommendation**: Proceed with ScraperFC for MVP development. Monitor usage and migrate to API-Football paid tier if/when the game goes commercial.

---

## References

- [ScraperFC PyPI](https://pypi.org/project/ScraperFC/)
- [ScraperFC Documentation](https://scraperfc.readthedocs.io/)
- [ScraperFC GitHub Repository](https://github.com/oseymour/ScraperFC)
- [FBref Documentation](https://scraperfc.readthedocs.io/en/latest/fbref.html)
- [Diggy Digs Data - ScraperFC API Guide](https://diggydigsdata.github.io/posts/scraper-fc-api-doc/index.html)
- [FBref Bot Traffic Policy](https://www.sports-reference.com/bot-traffic.html)
- [Football 501 Technical Design](../TECHNICAL_DESIGN.md)
