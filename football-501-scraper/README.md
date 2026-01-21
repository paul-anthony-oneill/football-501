# Football 501 - Web Scraper

Python web scraper for collecting football player statistics from FBRef and storing them in PostgreSQL with a **generic, domain-agnostic architecture**.

## Purpose

This scraper is **ONLY** responsible for:
1. Scraping player statistics from FBRef.com
2. Storing data in PostgreSQL database with JSONB
3. Creating generic questions
4. Populating generic answers table

**Game logic, answer validation, and real-time gameplay are handled by the Java/Spring Boot backend.**

## Key Architecture Principle

The database schema is **domain-agnostic** and **NOT tied to football-specific concepts**:
- ✅ Answer keys are TEXT (not foreign keys to players)
- ✅ Question filters use JSONB (flexible, no schema changes needed)
- ✅ Java backend only does text matching (no domain knowledge)
- ✅ Schema supports ANY domain (football, music, movies, etc.)

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
python init_db_v3.py  # Creates tables with PostgreSQL extensions
```

This creates:
- Database tables (players, categories, questions, answers)
- PostgreSQL extensions (pg_trgm, uuid-ossp)
- Required indexes (including trigram indexes for fuzzy matching)

## Core Scripts

### 1. Scrape Player Data

Scrapes player statistics and stores in JSONB `career_stats`:

```bash
python scrape_all_premier_league_history.py  # Script name TBD - may need updating
```

**Duration**: ~30-40 minutes for all seasons
**Output**: Players with JSONB career statistics

### 2. Create Questions

Creates generic questions programmatically:

```bash
python init_questions_v2.py
```

**What it does**:
- Creates category (e.g., "Premier League")
- Creates questions for all teams with JSONB config
- Example question types: goals, appearances, assists

### 3. Populate Answers

Populates the generic answers table:

```bash
python populate_answers_v2.py
```

**What it does**:
- Reads active questions
- Filters player career_stats by question.config (JSONB)
- Calculates scores by summing metric_key
- Creates Answer records with text-based answer_key
- Pre-computes is_valid_darts and is_bust flags

**Duration**: < 5 seconds per question

## Architecture

### Database Models (Generic Schema)

```python
Category
  - id (UUID)
  - name (e.g., "Premier League")
  - slug, description

Question (Generic)
  - id (UUID)
  - category_id
  - question_text
  - metric_key (e.g., "goals", "appearances", "points")  # Generic!
  - config (JSONB)  # Flexible filters {"team": "...", "season": "..."}
  - min_score, is_active

Answer (Generic - NOT tied to players!)
  - id (UUID)
  - question_id
  - answer_key (VARCHAR)  # Normalized text, NOT player_id FK!
  - display_text (VARCHAR)  # Display name
  - score (INTEGER)
  - is_valid_darts (BOOLEAN)  # Pre-computed
  - is_bust (BOOLEAN)  # Pre-computed
  - answer_metadata (JSONB)  # Flexible metadata (can include player_id optionally)

Player (Data source only)
  - id (UUID)
  - fbref_id, name, normalized_name
  - nationality
  - career_stats (JSONB array)  # Flexible season data
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

### Example Question Config (JSONB)

```json
{
  "team": "Manchester City",
  "competition": "Premier League",
  "season": "2023-2024"
}
```

**Flexibility**: Add any filter without schema changes!

```json
{
  "nationality": "Argentina",
  "competition": "Premier League"
}
```

### Example Answer

```json
{
  "answer_key": "erling haaland",  // Just text!
  "display_text": "Erling Haaland",
  "score": 35,
  "is_valid_darts": true,
  "is_bust": false,
  "answer_metadata": {
    "player_id": "uuid",  // Optional
    "team": "Manchester City"
  }
}
```

## Project Structure

```
football-501-scraper/
├── config.py                    # Configuration
├── init_questions_v2.py         # Create generic questions
├── populate_answers_v2.py       # Populate generic answers
├── database/
│   ├── __init__.py
│   └── models_v4.py            # Generic SQLAlchemy models
├── utils/
│   └── darts.py                # Darts score validation
├── scrapers/
│   └── __init__.py
├── requirements.txt
├── CURRENT_WORKFLOW.md         # Current workflow documentation
└── SCHEMA_COMPARISON.md        # V3 vs Current comparison
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

## Data Flow

```
FBRef.com → Scraper → Players (JSONB) → Create Questions → Populate Answers
                                                                  ↓
                                                Java Backend (domain-agnostic)
                                                                  ↓
                                                    Text matching only
```

## Workflow

### 1. Initial Setup (One Time)

```bash
# 1. Initialize database
python init_db_v3.py

# 2. Scrape player data (30-40 minutes)
python scrape_all_premier_league_history.py  # Script name TBD
```

### 2. Create Questions

```bash
# Create questions programmatically
python init_questions_v2.py
```

Or create manually:

```python
from database.models_v4 import Category, Question
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine

engine = create_engine('postgresql://...')
Session = sessionmaker(bind=engine)
session = Session()

# Create category
category = Category(
    name="Premier League",
    slug="premier-league",
    description="English Premier League Questions"
)
session.add(category)
session.commit()

# Create question
question = Question(
    category_id=category.id,
    question_text="Goals for Manchester City in Premier League 2023-2024",
    metric_key="goals",  # Generic!
    config={
        "team": "Manchester City",
        "competition": "Premier League",
        "season": "2023-2024"
    },
    min_score=1,
    is_active=True
)
session.add(question)
session.commit()
```

### 3. Populate Answers

```bash
python populate_answers_v2.py
```

### 4. Java Backend Reads Data

The Java backend reads from the generic `answers` table:

```java
// Find answer by text matching (NOT player lookup!)
Optional<Answer> answer = answerRepository
    .findByQuestionIdAndAnswerKey(questionId, "erling haaland");

// Return score
return answer.getScore();  // 35
```

**Key Point**: Java backend is domain-agnostic. It doesn't know about "players" or "teams" - just text matching!

## Database Operations

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

### View Answer Data

```sql
-- Check answers for a question
SELECT
    display_text,
    score,
    is_valid_darts,
    is_bust
FROM answers
WHERE question_id = 'uuid'
ORDER BY score DESC;
```

## Key Benefits of Generic Schema

### 1. Flexibility
Add new question types without schema changes:
```python
metric_key="assists"  # No migration needed!
metric_key="clean_sheets"
metric_key="minutes_played"
```

### 2. Extensibility
Store any metadata in JSONB:
```python
answer_metadata={
    "player_id": "uuid",
    "age": 27,
    "position": "Forward",
    "custom_field": "anything"
}
```

### 3. Domain-Agnostic
Schema supports ANY domain:
```python
# Music questions
Question(
    question_text="Songs by The Beatles",
    metric_key="songs",
    config={"artist": "The Beatles", "decade": "1960s"}
)
```

### 4. Java Backend Simplicity
```java
// No domain knowledge needed!
String userInput = "Erling Haaland";
Answer answer = findByAnswerKey(questionId, normalize(userInput));
return answer.getScore();  // That's it!
```

## Performance

- **Initial scrape**: 30-40 minutes for all Premier League history
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

## Documentation

- **CURRENT_WORKFLOW.md** - Current workflow documentation
- **SCHEMA_COMPARISON.md** - V3 vs Current architecture comparison

## License

This scraper is for educational/personal use only. Respect FBRef's terms of service and rate limits.

## Support

For issues with:
- **Scraping**: Check this repository
- **Game logic**: See Java/Spring Boot backend (`backend/`)
- **Schema questions**: See `CURRENT_WORKFLOW.md`
