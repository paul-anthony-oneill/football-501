# Football 501 - Scraping Service Operations Guide

**Version**: 1.0
**Date**: 2026-01-18
**Status**: Design Document

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Service Workflows](#service-workflows)
4. [Data Flow](#data-flow)
5. [Operational Scenarios](#operational-scenarios)
6. [API Endpoints](#api-endpoints)
7. [Configuration](#configuration)
8. [Monitoring & Logging](#monitoring--logging)
9. [Error Handling](#error-handling)

---

## Overview

The **Football 501 Scraping Service** is a standalone Python microservice that runs independently from the Spring Boot backend. Its sole responsibility is to **populate and maintain** the PostgreSQL database with player statistics scraped from FBref.com.

### Key Principles

1. **Zero Runtime Impact**: Game matches NEVER call the scraping service
2. **Pre-Populated Database**: All data scraped in advance via batch jobs
3. **Scheduled Updates**: Automated weekly updates for current season
4. **Manual Triggers**: Admin can manually trigger scrapes when needed
5. **Direct Database Access**: Python service writes directly to PostgreSQL

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     FOOTBALL 501 ECOSYSTEM                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  1. ScraperFC Python Microservice (Port 8001)                   │
│     Framework: FastAPI                                          │
│     Scheduler: APScheduler (background jobs)                    │
│                                                                  │
│     Components:                                                 │
│     ├── FBrefScraper (ScraperFC wrapper)                       │
│     ├── DataTransformer (FBref → Football 501 schema)          │
│     ├── DatabaseWriter (PostgreSQL bulk insert)                │
│     ├── JobScheduler (weekly/monthly jobs)                     │
│     └── AdminAPI (manual triggers, health checks)              │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Writes player stats
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. PostgreSQL Database                                         │
│     Port: 5432                                                  │
│                                                                  │
│     Tables:                                                     │
│     ├── questions (pre-defined by admin)                       │
│     ├── answers (populated by scraping service)                │
│     ├── scrape_jobs (job history/audit log)                    │
│     └── ... (other game tables)                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ Reads answers table (query only)
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Spring Boot Backend (Port 8080)                             │
│     Framework: Spring Boot 3.x + Java 17                       │
│                                                                  │
│     Components:                                                 │
│     ├── Game Engine (validates player answers)                 │
│     ├── WebSocket Handler (real-time matches)                  │
│     ├── REST API (CRUD operations)                             │
│     └── Authentication (OAuth + JWT)                           │
│                                                                  │
│     NO CHANGES NEEDED - Uses existing answers table            │
└─────────────────────────────────────────────────────────────────┘
```

### Key Architecture Decisions

**Why Microservice?**
- Spring Boot never waits for scraping (scraping happens offline)
- Python and Java run completely independently
- No code changes to existing backend
- Easy to replace/upgrade scraping logic

**Why Direct Database Access?**
- Simpler than REST API between services
- Batch operations are faster with direct SQL
- No network overhead for large datasets
- Single source of truth (PostgreSQL)

---

## Service Workflows

### Workflow 1: Initial Database Population (One-Time)

**When**: First deployment, before launching the game.

**Process**:

```
1. Admin defines questions in database
   └─> INSERT INTO questions (text, league, season, team, stat_type)
       Example: "Appearances for Manchester City in Premier League 2023-24"

2. Admin triggers initial population job
   └─> POST /api/admin/populate-initial
       {
         "seasons": ["2023-2024", "2022-2023", "2021-2022"],
         "leagues": ["England Premier League", "Spain La Liga", "Italy Serie A"]
       }

3. Scraping Service starts batch job
   ├─> Queries questions table for all questions needing answers
   ├─> For each question:
   │   ├─> Scrapes FBref for that season/league/team
   │   ├─> Waits 7 seconds (FBref rate limit)
   │   ├─> Transforms data to Football 501 schema
   │   ├─> Bulk inserts into answers table
   │   └─> Logs job status to scrape_jobs table
   └─> Job complete

4. Database now contains all answers
   └─> questions table: 100 questions
   └─> answers table: 20,000+ player/stat combinations

5. Game is ready to launch
   └─> Spring Boot reads from answers table during matches
```

**Estimated Time**:
- 3 leagues × 3 seasons × 20 teams = 180 scraping operations
- 180 × 7 seconds = 21 minutes
- With retries/logging: ~30 minutes total

---

### Workflow 2: Weekly Scheduled Updates

**When**: Every Sunday at 3:00 AM UTC (configurable).

**Purpose**: Update current season statistics after weekend matches.

**Process**:

```
Trigger: APScheduler (cron: "0 3 * * SUN")

1. Scheduler executes weekly_update() job
   └─> Target: Current season only (e.g., "2024-2025")

2. Query database for active questions
   └─> SELECT * FROM questions
       WHERE season = '2024-2025' AND status = 'active'

3. For each active question:
   ├─> Scrape latest stats from FBref
   ├─> Compare with existing answers table
   ├─> UPDATE existing rows if stats changed
   ├─> INSERT new rows if new players appeared
   └─> DELETE rows if players left the league

4. Log update results
   └─> INSERT INTO scrape_jobs
       (job_type, season, status, rows_updated, completed_at)

5. Send notification (optional)
   └─> Email admin: "Weekly update complete - 237 players updated"
```

**Example Log Entry**:
```json
{
  "job_id": 42,
  "job_type": "weekly_update",
  "season": "2024-2025",
  "leagues": ["England Premier League", "Spain La Liga"],
  "started_at": "2026-01-19T03:00:00Z",
  "completed_at": "2026-01-19T03:15:23Z",
  "rows_inserted": 42,
  "rows_updated": 237,
  "rows_deleted": 3,
  "status": "success"
}
```

---

### Workflow 3: Manual Admin Trigger

**When**: Admin needs to populate a new question immediately.

**Use Cases**:
- Adding a new league mid-season
- Creating a special event question (World Cup, etc.)
- Fixing data quality issues

**Process**:

```
1. Admin creates question via Spring Boot admin panel
   └─> POST /api/questions
       {
         "text": "Appearances for Real Madrid in Champions League 2023-24",
         "league": "UEFA Champions League",
         "season": "2023-2024",
         "team": "Real Madrid",
         "stat_type": "appearances"
       }
   └─> Returns: question_id = 1234

2. Admin triggers scraping for specific question
   └─> POST http://localhost:8001/api/admin/scrape-question
       {
         "question_id": 1234
       }

3. Scraping service processes immediately
   ├─> Fetches question details from database
   ├─> Scrapes FBref: Real Madrid in UCL 2023-24
   ├─> Transforms and inserts answers
   └─> Returns: { "status": "success", "players_added": 27 }

4. Question is now active and usable in games
   └─> Spring Boot can serve this question in matches
```

**Response Time**: ~10-15 seconds (single scrape operation).

---

### Workflow 4: Game Match Flow (No Scraping!)

**When**: Players are actively playing a match.

**Process** (Spring Boot only - no scraping service involved):

```
1. Match starts → Question assigned
   └─> SELECT * FROM questions WHERE id = 1234

2. Player submits answer: "Erling Haaland"
   └─> Spring Boot queries answers table:
       SELECT statistic_value, is_valid_darts_score, is_bust
       FROM answers
       WHERE question_id = 1234
         AND SIMILARITY(player_name, 'Erling Haaland') > 0.8  -- Fuzzy match

3. Database returns pre-cached result
   └─> { value: 31, is_valid: true, is_bust: false }

4. Game engine calculates score
   └─> Player score: 501 - 31 = 470

5. WebSocket broadcasts result to both players
   └─> No external API calls, no scraping, instant response
```

**Performance**: < 50ms (database query only).

---

## Data Flow

### Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1: OFFLINE DATA COLLECTION (Scraping Service)            │
└─────────────────────────────────────────────────────────────────┘

FBref.com
   │ (HTTP GET via ScraperFC)
   ├─> https://fbref.com/.../Premier-League/2023-2024
   │   Response: HTML with 603 player stats
   │
   ▼
ScraperFC Library (Python)
   │ Parses HTML → pandas DataFrame
   ├─> player_stats['Player'] = "Erling Haaland"
   ├─> player_stats['Playing_Time_MP'] = 31
   └─> player_stats['Performance_Gls'] = 27
   │
   ▼
DataTransformer (Football 501)
   │ Converts to database schema
   ├─> player_name = "Erling Haaland"
   ├─> statistic_value = 31 (appearances)
   ├─> is_valid_darts_score = True (31 is valid)
   └─> is_bust = False (31 < 180)
   │
   ▼
PostgreSQL Database
   │ Bulk INSERT into answers table
   └─> Commit transaction
       ✓ 603 players inserted for "Man City 2023-24" question


┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2: RUNTIME GAME PLAY (Spring Boot Only)                  │
└─────────────────────────────────────────────────────────────────┘

Player Input: "Erling Haaland"
   │ (WebSocket message)
   ▼
Spring Boot Game Engine
   │ Fuzzy match query
   ▼
PostgreSQL Database
   │ SELECT * FROM answers
   │ WHERE question_id = 1234
   │   AND player_name % 'Erling Haaland'  -- PostgreSQL trigram
   │
   │ Returns: { statistic_value: 31, is_valid: true }
   ▼
Game Engine
   │ Apply scoring rules
   ├─> Current score: 501
   ├─> Deduct: 31
   └─> New score: 470
   │
   ▼
WebSocket Response
   └─> { "player": "Erling Haaland", "score": 31, "new_total": 470 }
```

### Database Schema

#### `questions` Table
```sql
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,  -- "Appearances for Man City in Premier League 2023-24"
    league VARCHAR(100),  -- "England Premier League"
    season VARCHAR(20),   -- "2023-2024"
    team VARCHAR(100),    -- "Manchester City"
    stat_type VARCHAR(50), -- "appearances", "goals", "combined"
    status VARCHAR(20) DEFAULT 'active',  -- 'active', 'inactive'
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### `answers` Table
```sql
CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    player_name VARCHAR(255) NOT NULL,  -- "Erling Haaland"
    statistic_value INTEGER NOT NULL,    -- 31
    is_valid_darts_score BOOLEAN NOT NULL,  -- true
    is_bust BOOLEAN NOT NULL,            -- false
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Fuzzy matching index for player names
CREATE INDEX idx_answers_player_name_trgm
ON answers USING gin(player_name gin_trgm_ops);

-- Query performance index
CREATE INDEX idx_answers_question_id
ON answers(question_id);
```

#### `scrape_jobs` Table (Audit Log)
```sql
CREATE TABLE scrape_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,  -- 'initial', 'weekly', 'manual'
    season VARCHAR(20),
    league VARCHAR(100),
    question_id BIGINT,
    status VARCHAR(20) NOT NULL,  -- 'running', 'success', 'failed'
    rows_inserted INTEGER DEFAULT 0,
    rows_updated INTEGER DEFAULT 0,
    rows_deleted INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);
```

---

## Operational Scenarios

### Scenario 1: MVP Launch Day

**Goal**: Launch game with 3 leagues, 3 seasons of data.

**Timeline**:
```
Day -7: Deploy scraping service to production
Day -6: Run initial population job (30 minutes)
Day -5 to -1: QA testing with pre-populated data
Day 0: Launch game (no scraping needed during launch)
```

**Data Volume**:
- 3 leagues × 3 seasons × 20 teams = 180 questions
- ~603 players per league/season
- Total answers: ~180 × 603 = 108,540 rows

**Database Size**: ~50-100 MB (easily fits in memory for fast queries).

---

### Scenario 2: Mid-Season Player Transfer

**Example**: Kylian Mbappé transfers from PSG to Real Madrid in January 2025.

**What Happens**:

```
Before Weekly Update:
- answers table has Mbappé with PSG stats (15 apps, 12 goals)
- Questions about Real Madrid don't include Mbappé

Weekly Update (Sunday 3 AM):
1. Scraper fetches latest La Liga data
2. Detects:
   - Mbappé now appears in Real Madrid squad
   - PSG stats unchanged (historical)
3. Database updates:
   - INSERT new row: Mbappé → Real Madrid (3 apps, 2 goals)
   - KEEP old row: Mbappé → PSG (15 apps, 12 goals)

Result:
- "Appearances for PSG 2024-25" → includes Mbappé (15)
- "Appearances for Real Madrid 2024-25" → now includes Mbappé (3)
- Both answers are correct for different questions
```

**No Manual Intervention Required**: Weekly update handles automatically.

---

### Scenario 3: New Competition (World Cup 2026)

**Goal**: Add FIFA World Cup 2026 questions during the tournament.

**Process**:

```
Week 1 (Before Tournament):
1. Admin creates questions via admin panel:
   - "Appearances for England in World Cup 2026"
   - "Appearances for Brazil in World Cup 2026"
   - ... (32 teams × 1 question = 32 questions)

2. Admin triggers manual scrape:
   POST /api/admin/scrape-competition
   {
     "season": "2026",
     "league": "FIFA World Cup"
   }

3. Scraping service populates all 32 questions (10 minutes)

4. Questions go live immediately

During Tournament (Weekly Updates):
- Every Monday: Update World Cup stats after weekend matches
- New players/stats automatically added
- Game always has latest rosters
```

---

### Scenario 4: Data Quality Issue

**Problem**: Admin notices incorrect stats for a player.

**Example**: Database shows "Phil Foden: 35 apps" but should be "36 apps".

**Resolution**:

```
Option A: Wait for weekly update (automatic fix)
- Next Sunday 3 AM: Scraper re-fetches data
- Detects change: 35 → 36
- UPDATEs database automatically

Option B: Manual re-scrape (immediate fix)
1. Admin identifies question_id = 1234
2. POST /api/admin/rescrape-question/1234
3. Scraper fetches fresh data from FBref (7 seconds)
4. Database updated immediately
5. Next match uses corrected data
```

**Prevention**: Weekly updates keep data fresh automatically.

---

## API Endpoints

### Scraping Service (FastAPI - Port 8001)

#### Health Check
```http
GET /health
Response: { "status": "healthy", "uptime": "3d 4h 22m" }
```

#### Initial Population
```http
POST /api/admin/populate-initial
Authorization: Bearer {ADMIN_JWT}

Request:
{
  "seasons": ["2023-2024", "2022-2023"],
  "leagues": ["England Premier League", "Spain La Liga"]
}

Response:
{
  "job_id": 42,
  "status": "running",
  "estimated_duration": "20 minutes",
  "check_status_url": "/api/admin/jobs/42"
}
```

#### Scrape Specific Question
```http
POST /api/admin/scrape-question
Authorization: Bearer {ADMIN_JWT}

Request:
{
  "question_id": 1234
}

Response:
{
  "status": "success",
  "question_id": 1234,
  "players_added": 27,
  "duration": "8.3s"
}
```

#### Scrape Entire Competition
```http
POST /api/admin/scrape-competition
Authorization: Bearer {ADMIN_JWT}

Request:
{
  "season": "2024-2025",
  "league": "UEFA Champions League"
}

Response:
{
  "job_id": 43,
  "status": "running",
  "questions_to_populate": 32,
  "estimated_duration": "5 minutes"
}
```

#### Job Status
```http
GET /api/admin/jobs/{job_id}

Response:
{
  "job_id": 42,
  "status": "success",  // "running", "success", "failed"
  "progress": "180/180 questions complete",
  "rows_inserted": 108540,
  "rows_updated": 0,
  "started_at": "2026-01-19T03:00:00Z",
  "completed_at": "2026-01-19T03:21:34Z"
}
```

#### Job History
```http
GET /api/admin/jobs?limit=20

Response:
{
  "jobs": [
    {
      "job_id": 42,
      "job_type": "initial_population",
      "status": "success",
      "started_at": "2026-01-19T03:00:00Z"
    },
    ...
  ]
}
```

#### Manual Trigger Weekly Update
```http
POST /api/admin/trigger-weekly-update
Authorization: Bearer {ADMIN_JWT}

Response:
{
  "job_id": 44,
  "message": "Weekly update triggered manually"
}
```

---

## Configuration

### Environment Variables

```bash
# Database Connection
DATABASE_URL=postgresql://football501:password@localhost:5432/football501

# Scraping Configuration
FBREF_WAIT_TIME=7  # Seconds between requests (FBref rate limit)
MAX_RETRIES=3      # Retry failed scrapes
TIMEOUT=30         # HTTP request timeout (seconds)

# Scheduling
WEEKLY_UPDATE_CRON="0 3 * * SUN"  # Sunday 3 AM UTC
MONTHLY_UPDATE_CRON="0 2 1 * *"   # 1st of month, 2 AM UTC

# Admin API
ADMIN_JWT_SECRET=your_secret_here
ADMIN_API_ENABLED=true

# Logging
LOG_LEVEL=INFO
LOG_FILE=/var/log/football501-scraper.log

# Monitoring
ENABLE_METRICS=true
METRICS_PORT=9090  # Prometheus metrics
```

### config.yaml (Leagues Configuration)

```yaml
leagues:
  mvp:
    - name: "England Premier League"
      priority: 1
      update_frequency: "weekly"

    - name: "Spain La Liga"
      priority: 1
      update_frequency: "weekly"

    - name: "Italy Serie A"
      priority: 1
      update_frequency: "weekly"

  expansion:
    - name: "Germany Bundesliga"
      priority: 2
      update_frequency: "weekly"

    - name: "France Ligue 1"
      priority: 2
      update_frequency: "weekly"

    - name: "UEFA Champions League"
      priority: 2
      update_frequency: "weekly"

seasons:
  current: "2024-2025"
  historical:
    - "2023-2024"
    - "2022-2023"
    - "2021-2022"
```

---

## Monitoring & Logging

### Key Metrics to Track

1. **Job Success Rate**
   - Metric: `scrape_jobs_success_rate`
   - Target: > 95%

2. **Scrape Duration**
   - Metric: `scrape_duration_seconds`
   - Target: < 10s per question

3. **Database Write Performance**
   - Metric: `answers_insert_duration_ms`
   - Target: < 100ms for batch insert

4. **FBref Errors**
   - Metric: `fbref_error_count`
   - Alert: > 5 errors/hour

5. **Data Freshness**
   - Metric: `answers_last_update_timestamp`
   - Alert: > 7 days old

### Logging Strategy

**Log Levels**:
- **INFO**: Job start/complete, rows inserted
- **WARNING**: Retries, partial failures
- **ERROR**: Job failures, database errors
- **DEBUG**: Individual scrape operations

**Example Log Output**:
```
2026-01-19 03:00:00 INFO  [JobScheduler] Starting weekly update job (job_id=42)
2026-01-19 03:00:01 INFO  [FBrefScraper] Scraping England Premier League 2024-2025
2026-01-19 03:00:08 INFO  [FBrefScraper] Retrieved 603 players
2026-01-19 03:00:09 INFO  [DatabaseWriter] Bulk insert: 237 rows updated
2026-01-19 03:00:09 INFO  [JobScheduler] Question 1234 complete (8.2s)
...
2026-01-19 03:15:23 INFO  [JobScheduler] Weekly update complete (job_id=42)
2026-01-19 03:15:23 INFO  [JobScheduler] Summary: 180 questions, 108540 rows, 0 errors
```

### Alerting Rules

```yaml
alerts:
  - name: "Scrape Job Failed"
    condition: job_status == 'failed'
    action: email_admin
    priority: high

  - name: "Data Stale"
    condition: hours_since_last_update > 168  # 7 days
    action: email_admin
    priority: medium

  - name: "High Error Rate"
    condition: error_count > 10 in last 1 hour
    action: slack_notification
    priority: high
```

---

## Error Handling

### Common Errors & Solutions

#### 1. FBref Rate Limit Exceeded

**Error**: `429 Too Many Requests`

**Cause**: Scraping too fast (> 10 requests/minute)

**Solution**:
```python
# Already implemented: 7-second wait between requests
fb = FBref(wait_time=7)
```

**Prevention**: Never reduce wait_time below 7 seconds.

---

#### 2. FBref HTML Structure Changed

**Error**: `KeyError: 'Playing Time_MP'` or empty DataFrame

**Cause**: FBref updated their HTML/CSS structure

**Solution**:
1. Check ScraperFC for updates: `pip install --upgrade ScraperFC`
2. If no update available, manually adjust column name mapping
3. Notify ScraperFC maintainers via GitHub issue

**Fallback**: Use cached data until fixed (weekly update will retry).

---

#### 3. Database Connection Lost

**Error**: `psycopg2.OperationalError: could not connect to server`

**Cause**: PostgreSQL down or network issue

**Solution**:
```python
# Retry logic with exponential backoff
@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10)
)
def connect_to_database():
    return psycopg2.connect(DATABASE_URL)
```

**Alert**: Email admin if 3 retries fail.

---

#### 4. Invalid League Name

**Error**: `ValueError: 'EPL' is not a valid league`

**Cause**: Using abbreviation instead of full name

**Solution**: Use league name mapping:
```python
LEAGUE_MAPPING = {
    "EPL": "England Premier League",
    "La Liga": "Spain La Liga",
    "Serie A": "Italy Serie A"
}
```

---

#### 5. Duplicate Player Names

**Example**: Two players named "John Smith" in different teams

**Solution**: Include team context in database:
```sql
-- Use composite matching
SELECT * FROM answers
WHERE question_id = 1234
  AND player_name % 'John Smith'
  AND squad = 'Manchester City'  -- Disambiguate
```

**Note**: This is rare (< 0.1% of cases) but handled by game engine logic.

---

## Summary

### How It All Works Together

1. **Before Game Launch** (One-Time)
   - Scraping service populates database with 3 seasons × 3 leagues
   - Takes ~30 minutes, runs offline
   - Database contains 100K+ player/stat combinations

2. **During Game Operations** (24/7)
   - Spring Boot queries pre-populated answers table
   - No scraping happens during matches
   - Sub-50ms response time

3. **Weekly Maintenance** (Automated)
   - Sunday 3 AM: Update current season stats
   - Takes ~15 minutes
   - Game continues running (no downtime)

4. **Admin Overrides** (As Needed)
   - Manual triggers for new competitions
   - Data quality fixes
   - Special events

### Key Advantages

✅ **Fast Game Performance**: No external API calls during matches
✅ **Scalable**: Can handle thousands of concurrent matches
✅ **Reliable**: Database queries never fail (unlike API calls)
✅ **Cost-Effective**: Free scraping, no API fees
✅ **Flexible**: Easy to add new leagues/seasons
✅ **Maintainable**: Scraping service isolated from game logic

---

**Next Steps**: See `SCRAPERFC_INTEGRATION.md` for implementation details.
