# Scraping Service - Quick Summary

**TL;DR**: The scraping service runs **separately** from your game. It pre-fills the database with player stats. During matches, your Spring Boot backend just reads from the database—no scraping happens.

---

## The Simple Version

### 🏗️ **Before Launch** (One-Time Setup)

```
Admin clicks "Populate Database" button
        ↓
Scraping Service runs for 30 minutes
        ↓
Database now has 100,000+ player statistics
        ↓
Game is ready to launch!
```

### 🎮 **During Gameplay** (Real-Time)

```
Player submits "Erling Haaland"
        ↓
Spring Boot queries PostgreSQL
        ↓
Database returns: 31 appearances (< 50ms)
        ↓
Game calculates score: 501 - 31 = 470
        ↓
WebSocket sends result to players

NO SCRAPING HAPPENS HERE!
```

### 🔄 **Weekly Maintenance** (Automated)

```
Every Sunday 3 AM:
    Scraping Service wakes up
    Updates current season stats (15 minutes)
    Goes back to sleep

Your game keeps running the whole time!
```

---

## How The Two Services Work Together

### Python Scraping Service (Port 8001)
**Job**: Populate database with player stats
**When**: Scheduled jobs (weekly) or manual triggers (on-demand)
**Direct Interaction with Spring Boot**: NONE

```python
# Runs independently
while True:
    if sunday_3am():
        scrape_latest_stats()
        write_to_database()
    sleep(1_hour)
```

### Spring Boot Backend (Port 8080)
**Job**: Run the game (matches, scoring, WebSocket)
**When**: 24/7 (always running)
**Direct Interaction with Scraping Service**: NONE

```java
// Just reads from database
@Override
public Answer validatePlayerAnswer(String playerName, Long questionId) {
    return answerRepository.findByPlayerNameAndQuestionId(playerName, questionId);
}
```

### PostgreSQL Database (Port 5432)
**Job**: Store everything (questions, answers, game state)
**Interaction**: Both services read/write (at different times)

```sql
-- Scraping Service WRITES
INSERT INTO answers (player_name, statistic_value, ...)
VALUES ('Erling Haaland', 31, ...);

-- Spring Boot READS
SELECT * FROM answers
WHERE question_id = 1234 AND player_name % 'Erling Haaland';
```

---

## Three Key Workflows

### 1️⃣ Initial Database Population

**When**: First deployment (before game launch)

```
┌─────────────┐
│   FBref     │  Contains: All player stats
└──────┬──────┘
       │ ScraperFC fetches HTML
       ▼
┌─────────────┐
│  Scraping   │  Transforms: HTML → Database Schema
│  Service    │
└──────┬──────┘
       │ Bulk INSERT
       ▼
┌─────────────┐
│ PostgreSQL  │  Stores: 100K+ player/stat rows
└─────────────┘
```

**Time**: ~30 minutes (180 scrape operations × 7 seconds each)

**Result**: Database ready for game launch

---

### 2️⃣ Weekly Updates

**When**: Every Sunday 3 AM (automated)

```
┌─────────────┐
│ APScheduler │  Trigger: Cron job ("0 3 * * SUN")
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Scraping   │  1. Query: Which questions need updates?
│  Service    │  2. Scrape: Latest stats from FBref
│             │  3. Compare: New vs existing data
└──────┬──────┘
       │ UPDATE answers SET statistic_value = 32 WHERE ...
       ▼
┌─────────────┐
│ PostgreSQL  │  Updated: Current season stats
└─────────────┘
```

**Time**: ~15 minutes

**Impact on Game**: ZERO (game keeps running)

---

### 3️⃣ Game Match Flow

**When**: Players actively playing (24/7)

```
┌─────────────┐
│   Player    │  Submits: "Erling Haaland"
└──────┬──────┘
       │ WebSocket message
       ▼
┌─────────────┐
│ Spring Boot │  Query: SELECT * FROM answers WHERE ...
└──────┬──────┘
       │ SQL query
       ▼
┌─────────────┐
│ PostgreSQL  │  Returns: { value: 31, is_valid: true }
└──────┬──────┘
       │ < 50ms
       ▼
┌─────────────┐
│ Spring Boot │  Calculate: 501 - 31 = 470
└──────┬──────┘
       │ WebSocket message
       ▼
┌─────────────┐
│   Player    │  Sees: "Correct! -31 points. New score: 470"
└─────────────┘
```

**Time**: < 50ms (database query only)

**External API Calls**: ZERO

---

## Example: Adding a New Question

### Scenario: Admin wants to add "World Cup 2026" questions

```
Step 1: Admin creates question
    POST /api/questions
    {
      "text": "Appearances for England in World Cup 2026",
      "league": "FIFA World Cup",
      "season": "2026"
    }
    → Returns: question_id = 9999

Step 2: Admin triggers scraping
    POST http://localhost:8001/api/admin/scrape-question
    {
      "question_id": 9999
    }
    → Scraping service runs (10 seconds)

Step 3: Database populated
    → answers table now has 26 England players
    → question_id 9999 is ready to use

Step 4: Question goes live
    → Spring Boot can immediately serve this question in matches
    → No additional scraping needed
```

---

## Technical Details You Asked About

### Q: How does Spring Boot know when new data is ready?

**A**: It doesn't need to! Spring Boot just queries the database. If new rows exist, they're returned. If not, player gets "invalid answer".

### Q: What if scraping fails during a match?

**A**: Scraping NEVER happens during matches. All data is pre-populated. If weekly update fails, game uses slightly outdated data (still works fine).

### Q: How do you prevent data conflicts?

**A**:
- Scraping service writes during off-hours (3 AM)
- Spring Boot only reads (never writes to answers table)
- PostgreSQL handles concurrent reads/writes natively

### Q: What about rate limits?

**A**:
- FBref: 10 requests/minute → We use 7-second wait (8.5 req/min)
- API-Football (if used later): 100 req/day → Only for real-time updates

### Q: Can you scrape during the day?

**A**: Yes! Admin can manually trigger scrapes anytime via API. Each scrape is independent and takes ~10 seconds.

---

## Deployment Architecture

### Development (Local)

```yaml
services:
  scraper:
    image: football501-scraper:latest
    ports:
      - "8001:8001"
    environment:
      - DATABASE_URL=postgresql://localhost:5432/football501

  postgres:
    image: postgres:15
    ports:
      - "5432:5432"

  backend:
    image: football501-backend:latest
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgresql://localhost:5432/football501
```

### Production (Cloud)

```
Load Balancer (Port 443)
    │
    ├─> Spring Boot Instances (Port 8080) ← Game traffic
    │   └─> PostgreSQL (RDS/Managed)
    │
    └─> Scraping Service (Port 8001) ← Admin only
        └─> Same PostgreSQL
```

**Note**: Scraping service has restricted access (admin JWT required).

---

## What You DON'T Need to Worry About

❌ Spring Boot calling the scraping service
❌ Real-time API calls during matches
❌ Rate limiting affecting game performance
❌ Scraping failures breaking matches
❌ Complex service coordination

## What You DO Need to Implement

✅ Python FastAPI service (3-5 days)
✅ Database connection from Python (1 day)
✅ APScheduler for weekly jobs (1 day)
✅ Admin API endpoints (1 day)
✅ Docker deployment (1 day)

**Total**: 7-12 days

---

## See Also

- **Full Operations Guide**: `docs/design/SCRAPING_SERVICE_OPERATIONS.md`
- **Integration Details**: `docs/design/SCRAPERFC_INTEGRATION.md`
- **Implementation Log**: `docs/PROJECT_LOG.md`
