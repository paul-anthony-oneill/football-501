# Football 501 - Web Scraper

Python web scraper for collecting football player statistics from FBRef and storing them in PostgreSQL with JSONB.

## Purpose

This scraper is **ONLY** responsible for:
1. Scraping player statistics from FBRef.com
2. Storing data in PostgreSQL database
3. Pre-computing valid answers for questions

**Game logic, answer validation, and real-time gameplay are handled by the Java/Spring Boot backend.**

## Setup

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Chrome/Chromium browser (for Selenium)

### Installation

```bash
cd football-501-scraper
pip install -r requirements.txt
```

### Database Initialization

```bash
python init_db_v3.py
```

This creates:
- Database tables (players, questions, question_valid_answers, etc.)
- PostgreSQL extensions (pg_trgm, uuid-ossp)
- Required indexes (including trigram indexes)

## Core Scripts

### 1. Scrape All Premier League History

Scrapes all Premier League seasons from 1992-1993 to 2025-2026:

```bash
python scrape_all_premier_league_history.py
```

**Duration**: ~30-40 minutes for all 34 seasons
**Output**: All players and their career stats stored in JSONB

### 2. Update Current Season

Rescrapes and updates the current season (2025-2026):

```bash
python update_current_season.py
```

**Recommended**: Run weekly to keep current season data fresh
**Duration**: ~1-2 minutes

### 3. Populate Question Answers

Pre-computes valid answers for a question by querying JSONB career stats:

```bash
python populate_question_answers.py
```

**Usage**: Run after creating new questions
**Output**: Populates `question_valid_answers` table

## Architecture

### Database Models (JSONB)

```python
Player
  - id (UUID)
  - fbref_id (unique identifier)
  - name, normalized_name
  - nationality
  - career_stats (JSONB array)  # Flexible season data
  - timestamps

Question
  - id (UUID)
  - question_text
  - stat_type (appearances, goals, combined, goalkeeper)
  - filters (team_id, competition_id, season, nationality)
  - is_active

QuestionValidAnswer (pre-computed)
  - id (UUID)
  - question_id, player_id
  - player_name, normalized_name
  - score (the answer score)
  - is_valid_darts_score (pre-computed)
  - is_bust (pre-computed)
```

### JSONB Career Stats Structure

```json
[
  {
    "season": "2023-2024",
    "team": "Manchester City",
    "team_id": "uuid",
    "competition": "Premier League",
    "competition_id": "uuid",
    "appearances": 35,
    "goals": 27,
    "assists": 5,
    "clean_sheets": 0,
    "minutes_played": 2890
  }
]
```

## Project Structure

```
football-501-scraper/
├── config.py                              # Configuration
├── init_db_v3.py                         # Database initialization
├── scrape_all_premier_league_history.py  # Scrape all seasons
├── update_current_season.py              # Update current season
├── populate_question_answers.py          # Populate valid answers
├── database/
│   ├── __init__.py
│   ├── models_v3.py                      # SQLAlchemy models
│   └── crud_v3.py                        # Database operations
├── scrapers/
│   ├── __init__.py
│   ├── league_seeder_v3.py              # Season scraping logic
│   └── player_scraper_v3.py             # Player scraping logic
└── requirements.txt                      # Python dependencies
```

## Configuration

### Environment Variables

Create `.env` file:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=football501
DB_USER=football501
DB_PASSWORD=your_password

# Scraping
SQL_ECHO=false
```

### Rate Limiting

- **Wait time between players**: 5 seconds (configurable in scraper)
- **Browser headless mode**: Enabled by default
- **Parallel workers**: 1 (sequential scraping to respect rate limits)

## Data Flow

```
FBRef.com → Selenium Scraper → Parse HTML → Transform Data → PostgreSQL (JSONB)
                                                                     ↓
                                                   Spring Boot Backend reads data
                                                                     ↓
                                                      Real-time answer validation
```

## Database Operations

### View Player Stats

```python
from database.crud_v3 import DatabaseManager

db = DatabaseManager()
with db.get_session() as session:
    player = session.query(Player).filter_by(fbref_id='1f44ac21').first()

    for season in player.career_stats:
        print(f"{season['season']}: {season['team']} - {season['appearances']} apps")
```

### Query JSONB Directly

```sql
-- Find all players with 20+ goals in 2023-2024
SELECT
    p.name,
    season->>'team' as team,
    (season->>'goals')::int as goals
FROM players p,
     jsonb_array_elements(p.career_stats) as season
WHERE season->>'season' = '2023-2024'
  AND (season->>'goals')::int >= 20
ORDER BY (season->>'goals')::int DESC;
```

## Maintenance

### Weekly Update Schedule

```bash
# Every Sunday at 3 AM UTC
0 3 * * 0 cd /path/to/football-501-scraper && python update_current_season.py
```

### Monitoring

Check database statistics:

```sql
-- Total players
SELECT COUNT(*) FROM players;

-- Total season records
SELECT COUNT(*) FROM (
    SELECT jsonb_array_elements(career_stats) FROM players
) AS seasons;

-- Questions with answers
SELECT
    q.question_text,
    COUNT(a.id) as answer_count
FROM questions q
LEFT JOIN question_valid_answers a ON q.id = a.question_id
GROUP BY q.id, q.question_text;
```

## Performance

- **Initial scrape**: 30-40 minutes for all Premier League history
- **Weekly update**: 1-2 minutes for current season
- **Answer population**: < 5 seconds per question
- **Database query performance**: < 10ms for answer validation (with indexes)

## Troubleshooting

### Selenium Issues

```bash
# Install ChromeDriver
# On Mac:
brew install chromedriver

# On Linux:
sudo apt-get install chromium-chromedriver
```

### Database Connection

```bash
# Test connection
psql -h localhost -U football501 -d football501

# Check extensions
SELECT * FROM pg_extension WHERE extname = 'pg_trgm';
```

### Rate Limiting

If you get blocked:
- Increase wait time in scraper (5s → 10s)
- Run during off-peak hours
- Use VPN/proxy if necessary

## License

This scraper is for educational/personal use only. Respect FBRef's terms of service and rate limits.

## Support

For issues with:
- **Scraping**: Check this repository
- **Game logic**: See Java/Spring Boot backend (`backend/`)
- **Answer validation**: See `backend/ANSWER_EVALUATION_FRAMEWORK.md`
