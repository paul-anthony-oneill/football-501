

# Football 501 Data Scraper - Version 2

**Normalized database schema with comprehensive career statistics storage.**

## What's New in V2?

### Architecture Improvements

**Version 1 (Old):**
- Scrapes data per question
- Stores answers directly in `answers` table
- Re-scrapes for similar questions (wasteful)
- Hardcoded team/league strings

**Version 2 (New):**
- Scrapes ALL career data once per player
- Normalized entities (players, teams, competitions)
- Generates questions from cached data (fast!)
- Reuses data across multiple questions

### Key Benefits

1. **Scrape Once, Use Many Times**: Store all career data upfront
2. **Fast Question Creation**: No scraping needed when creating questions
3. **Easy Competition Addition**: Add Champions League without re-scraping players
4. **Clean Schema**: Normalized database design (great for portfolio!)
5. **Future-Proof**: Ready for international, historical, and cross-league questions

---

## Database Schema

```
players
├── id (PK)
├── name
├── normalized_name (for fuzzy search)
├── nationality
└── fbref_id

teams
├── id (PK)
├── name
├── team_type (club/national)
├── country
└── fbref_id

competitions
├── id (PK)
├── name
├── competition_type (domestic_league/continental/international)
├── country
└── display_name

player_career_stats (central data store!)
├── id (PK)
├── player_id (FK → players)
├── team_id (FK → teams)
├── competition_id (FK → competitions)
├── season
├── appearances
├── goals
├── assists
├── clean_sheets
└── minutes_played

questions
├── id (PK)
├── question_text
├── stat_type
├── team_id (FK → teams, nullable)
├── competition_id (FK → competitions, nullable)
├── nationality_filter
├── season_filter
├── aggregation (sum/single_season/latest_season)
└── min_score

question_valid_answers (pre-computed for gameplay)
├── id (PK)
├── question_id (FK → questions)
├── player_id (FK → players)
├── player_name (denormalized)
├── normalized_name (for fuzzy search)
├── score
├── is_valid_darts_score
└── is_bust
```

---

## Installation

### Prerequisites

- Python 3.8+
- PostgreSQL 12+ (for fuzzy search via `pg_trgm`)
- pip

### Setup

```bash
# Install dependencies
pip install -r requirements.txt

# Configure database (edit .env or config.py)
DATABASE_URL=postgresql://user:password@localhost:5432/football501

# Initialize database
python init_database_v2.py
```

---

## Quick Start

### 1. Initialize Database

```bash
# Create tables and populate sample data
python init_database_v2.py --populate

# Or drop existing tables and start fresh
python init_database_v2.py --drop --populate
```

### 2. Scrape Player Data

```bash
# Scrape Premier League 2023-2024
# This stores ALL career stats for each player
python -c "
from scrapers.player_career_scraper import PlayerCareerScraper
scraper = PlayerCareerScraper()
scraper.scrape_league_players(
    league='Premier League',
    season='2023-2024',
    min_appearances=5
)
"
```

**What this does:**
- Fetches all Premier League players for 2023-2024
- Stores their stats in `player_career_stats` table
- Data is now cached for future use!

### 3. Create Questions

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

# Get team and competition
man_city = db.get_team_by_name("Manchester City")
premier_league = db.get_competition_by_name("Premier League")

# Create question
question = db.create_question(
    question_text="Total Premier League goals for Manchester City",
    stat_type="goals",
    team_id=man_city.id,
    competition_id=premier_league.id,
    aggregation="sum",  # Sum across all seasons
    min_score=10
)
```

### 5. Multi-Competition Stats ("All Competitions")

To create questions like "Total Career Goals" or "All Competitions Goals", you need to scrape multiple competitions.

**Step 1: Scrape Competitions**

Use the provided utility to scrape major leagues:

```bash
# Scrape Premier League + Champions League (last 3 seasons)
python scrape_all_competitions.py --seasons 3 --leagues "England Premier League,UEFA Champions League"

# Or use a predefined set
python scrape_all_competitions.py --set top5 --seasons 1
```

**Step 2: Create Aggregated Question**

Create a question *without* a `competition_id` filter. The system will automatically sum stats from all scraped competitions.

```python
question = db.create_question(
    question_text="Total Goals for Erling Haaland (All Comps)",
    stat_type="goals",
    team_id=man_city.id,
    competition_id=None,  # <--- No filter triggers aggregation
    season_filter="2023-2024"
)
```

---

## Usage Examples

See `example_usage_v2.py` for complete workflow demonstrations.

```bash
# Run all examples sequentially
python example_usage_v2.py

# Run specific example
python example_usage_v2.py --example 3
```

### Example Workflows

#### Workflow 1: MVP (Premier League Only)

```bash
# 1. Scrape Premier League
python -c "from scrapers.player_career_scraper import PlayerCareerScraper; PlayerCareerScraper().scrape_league_players('Premier League', '2023-2024')"

# 2. Create questions (via Python or admin UI)
# 3. Populate answers
python -m jobs.populate_questions_v2 --all

# 4. Ready for gameplay!
```

#### Workflow 2: Add Champions League (No Re-scraping!)

```python
# If you already scraped PL players, you might have CL data!
db = DatabaseManager()

champions_league = db.get_or_create_competition(
    name="Champions League",
    competition_type="continental"
)

man_city = db.get_team_by_name("Manchester City")

question = db.create_question(
    question_text="Goals in Champions League for Manchester City",
    stat_type="goals",
    team_id=man_city.id,
    competition_id=champions_league.id
)

# Populate from cached data
populator = QuestionPopulator()
populator.populate_single_question(question.id)
```

#### Workflow 3: Weekly Stats Update

```bash
# Update current season stats
python -c "from scrapers.player_career_scraper import PlayerCareerScraper; PlayerCareerScraper().update_current_season('Premier League', '2023-2024')"

# Re-populate answers
python -m jobs.populate_questions_v2 --all
```

---

## Question Types

### Stat Types

- `appearances`: Player appearances
- `goals`: Goals scored
- `assists`: Assists provided
- `combined_apps_goals`: Appearances + Goals
- `combined_apps_assists`: Appearances + Assists
- `goalkeeper`: Appearances + Clean Sheets

### Aggregation Strategies

- `sum`: Sum stats across all seasons (default)
- `single_season`: Use specific season only
- `latest_season`: Use most recent season

### Filters

- `team_id`: Filter by specific team
- `competition_id`: Filter by specific competition
- `nationality_filter`: Filter by player nationality
- `season_filter`: Filter by specific season
- `min_score`: Only include players with score >= N

---

## API Reference

### PlayerCareerScraper

```python
from scrapers.player_career_scraper import PlayerCareerScraper

scraper = PlayerCareerScraper()

# Scrape league players
result = scraper.scrape_league_players(
    league="Premier League",
    season="2023-2024",
    min_appearances=5,
    rescrape_recent=False
)

# Update current season
result = scraper.update_current_season(
    league="Premier League",
    season="2023-2024"
)

# Scrape specific team
result = scraper.scrape_team_players(
    team_name="Manchester City",
    league="Premier League",
    season="2023-2024"
)
```

### QuestionPopulator

```python
from jobs.populate_questions_v2 import QuestionPopulator

populator = QuestionPopulator()

# Populate single question
result = populator.populate_single_question(question_id=1)

# Populate all active questions
result = populator.populate_all_active_questions()
```

### DatabaseManager

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

# Create entities
player = db.get_or_create_player(name="Erling Haaland", normalized_name="erling haaland")
team = db.get_or_create_team(name="Manchester City", team_type="club")
comp = db.get_or_create_competition(name="Premier League", competition_type="domestic_league")

# Store stats
stats = db.upsert_player_career_stats(
    player_id=player.id,
    team_id=team.id,
    competition_id=comp.id,
    season="2023-2024",
    appearances=35,
    goals=36
)

# Query stats
results = db.query_player_stats(
    team_name="Manchester City",
    competition_name="Premier League",
    season="2023-2024"
)
```

---

## Migration from V1

### Data Migration Strategy

1. **Start Fresh (Recommended for MVP)**
   - Drop old tables
   - Initialize V2 schema
   - Re-scrape data (it's available on FBref)

2. **Preserve Data (If needed)**
   - Export old `questions` and `answers` to CSV
   - Map to new schema entities
   - Import via scripts

### File Mapping

| Old File | New File | Status |
|----------|----------|--------|
| `database/models.py` | `database/models_v2.py` | ✅ Ready |
| `database/crud.py` | `database/crud_v2.py` | ✅ Ready |
| `jobs/populate_questions.py` | `jobs/populate_questions_v2.py` | ✅ Ready |
| - | `scrapers/player_career_scraper.py` | ✅ New |
| - | `init_database_v2.py` | ✅ New |
| - | `example_usage_v2.py` | ✅ New |

---

## Performance

### Scraping Times

- Premier League full season: ~10-15 minutes (600 players)
- Single team: ~30 seconds (25 players)
- Weekly update: ~10-15 minutes (updates changed players)

### Question Population Times

- Single question: < 1 second (cached data!)
- All questions (20): < 5 seconds (cached data!)

### Storage Estimates

- 1 player: ~1 KB
- 600 PL players: ~600 KB
- 20 questions × 400 answers: ~8 MB

---

## Troubleshooting

### Trigram Index Not Created

**Error:** `extension "pg_trgm" does not exist`

**Solution:**
```sql
-- Run as PostgreSQL superuser
CREATE EXTENSION pg_trgm;
```

### No Data Found for Question

**Error:** `No data found for question X`

**Cause:** No cached stats match the question filters.

**Solution:**
- Check if you've scraped the correct league/season
- Verify team/competition names match exactly
- Run scraper for missing data

### FBref Scraping Fails

**Error:** Rate limit or timeout errors

**Solution:**
- Increase `wait_time` in config (default: 3 seconds)
- Split scraping into smaller batches
- Retry failed players individually

---

## Future Enhancements

### Planned Features

1. **Full Career Scraping**: Scrape player's entire career from FBref player pages
2. **International Competitions**: World Cup, Euros, Copa America
3. **Historical Seasons**: Scrape data from previous seasons
4. **API Integration**: Replace FBref with API-Football (better rate limits)
5. **Redis Caching**: Cache frequently accessed data for gameplay

### Extending the Schema

```sql
-- Add new stat types
ALTER TABLE player_career_stats ADD COLUMN yellow_cards INTEGER DEFAULT 0;
ALTER TABLE player_career_stats ADD COLUMN red_cards INTEGER DEFAULT 0;

-- Add new question types
-- No schema changes needed! Just add new stat_type values
```

---

## Contributing

### Code Style

- Follow PEP 8
- Use type hints
- Add docstrings to all public methods
- Log important operations

### Testing

```bash
# Run tests (TODO: add test suite)
pytest tests/

# Test specific component
pytest tests/test_career_scraper.py
```

---

## License

MIT License - See LICENSE file for details.

---

## Contact

For questions or issues, please open a GitHub issue or contact the maintainer.

---

**Happy Scraping! ⚽📊**
