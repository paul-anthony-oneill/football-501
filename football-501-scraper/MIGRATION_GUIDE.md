# Migration Guide: V1 → V2

## Overview

This guide helps you transition from the old question-based scraping system to the new normalized career statistics architecture.

---

## Key Differences

### Data Model

**V1 (Old):**
```
questions ──┐
            └──→ answers (player_name, statistic_value)
```

**V2 (New):**
```
players ──┐
          ├──→ player_career_stats ←── teams
          └                          ├── competitions
                                     └── seasons

questions ──→ question_valid_answers (generated from player_career_stats)
```

### Workflow

**V1 Workflow:**
```
1. Create question (team, league, season)
2. Scrape FBref for that specific question
3. Store answers directly
```

**V2 Workflow:**
```
1. Scrape ALL career data once (for a league/season)
2. Create questions (just metadata)
3. Generate answers from cached data (instant!)
```

---

## Migration Steps

### Option 1: Fresh Start (Recommended)

**Best for:** MVP development, no critical data to preserve

```bash
# 1. Backup old database (optional)
pg_dump football501 > backup_v1.sql

# 2. Drop old tables
python init_database_v2.py --drop

# 3. Initialize new schema
python init_database_v2.py --populate

# 4. Scrape data with new system
python -c "from scrapers.player_career_scraper import PlayerCareerScraper; PlayerCareerScraper().scrape_league_players('Premier League', '2023-2024')"

# 5. Recreate questions
# (Manually via Python or future admin UI)

# 6. Populate answers
python -m jobs.populate_questions_v2 --all
```

**Time estimate:** 30 minutes + scraping time (10-15 mins)

---

### Option 2: Preserve & Migrate Data

**Best for:** If you have custom questions you want to keep

#### Step 1: Export Old Data

```python
# export_v1_data.py
from database import DatabaseManager as OldDB
import json

db = OldDB()
questions = db.get_questions(status='active')

export_data = []
for q in questions:
    export_data.append({
        'text': q.text,
        'league': q.league,
        'season': q.season,
        'team': q.team,
        'stat_type': q.stat_type
    })

with open('questions_v1.json', 'w') as f:
    json.dump(export_data, f, indent=2)

print(f"Exported {len(export_data)} questions")
```

#### Step 2: Initialize V2 Database

```bash
python init_database_v2.py --drop --populate
```

#### Step 3: Scrape Data

```bash
python -c "from scrapers.player_career_scraper import PlayerCareerScraper; scraper = PlayerCareerScraper(); scraper.scrape_league_players('Premier League', '2023-2024')"
```

#### Step 4: Recreate Questions

```python
# migrate_questions.py
import json
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

with open('questions_v1.json', 'r') as f:
    old_questions = json.load(f)

for q in old_questions:
    # Get or create team
    team = db.get_or_create_team(
        name=q['team'],
        team_type='club'
    )

    # Get or create competition
    comp = db.get_or_create_competition(
        name=q['league'],
        competition_type='domestic_league'
    )

    # Create question
    new_q = db.create_question(
        question_text=q['text'],
        stat_type=q['stat_type'],
        team_id=team.id,
        competition_id=comp.id,
        season_filter=q['season'],
        aggregation='sum'
    )

    print(f"Migrated: {new_q.question_text}")
```

#### Step 5: Populate Answers

```bash
python -m jobs.populate_questions_v2 --all
```

**Time estimate:** 1-2 hours (includes manual mapping)

---

## Code Changes Required

### Imports

**Old:**
```python
from database.models import Question, Answer
from database.crud import DatabaseManager
from jobs.populate_questions import QuestionPopulator
```

**New:**
```python
from database.models_v2 import Question, QuestionValidAnswer, Player, Team, Competition, PlayerCareerStats
from database.crud_v2 import DatabaseManager
from jobs.populate_questions_v2 import QuestionPopulator
from scrapers.player_career_scraper import PlayerCareerScraper
```

### Creating Questions

**Old:**
```python
question = Question(
    text="Appearances for Man City in 2023-2024",
    league="Premier League",
    season="2023-2024",
    team="Manchester City",
    stat_type="appearances"
)
db.session.add(question)
db.session.commit()

# Then scrape data
populator.populate_single_question(question.id)  # Scrapes FBref!
```

**New:**
```python
# First ensure data is scraped (do once)
# scraper.scrape_league_players("Premier League", "2023-2024")

# Then create question (no scraping!)
team = db.get_team_by_name("Manchester City")
comp = db.get_competition_by_name("Premier League")

question = db.create_question(
    question_text="Appearances for Man City in 2023-2024",
    stat_type="appearances",
    team_id=team.id,
    competition_id=comp.id,
    season_filter="2023-2024"
)

# Generate answers from cached data (instant!)
populator.populate_single_question(question.id)
```

### Querying Answers

**Old:**
```python
answers = db.session.query(Answer).filter_by(question_id=question_id).all()
```

**New:**
```python
answers = db.get_answers_for_question(question_id)
```

### Validating Player Input

**Old & New (Same):**
```python
# Fuzzy matching still works the same way
# Trigram index on normalized_name
answers = db.session.query(QuestionValidAnswer)\
    .filter(QuestionValidAnswer.question_id == question_id)\
    .filter(QuestionValidAnswer.normalized_name.op('%')(player_input))\
    .all()
```

---

## Database Schema Comparison

### Old Schema

```sql
questions (
  id BIGINT PRIMARY KEY,
  text TEXT,
  league VARCHAR(100),
  season VARCHAR(20),
  team VARCHAR(100),  -- String!
  stat_type VARCHAR(50),
  status VARCHAR(20)
)

answers (
  id BIGINT PRIMARY KEY,
  question_id BIGINT,
  player_name VARCHAR(255),  -- Stored per question
  player_api_id INTEGER,
  statistic_value INTEGER,
  is_valid_darts_score BOOLEAN,
  is_bust BOOLEAN
)
```

**Problems:**
- Team/league stored as strings (not normalized)
- Same player stored multiple times (once per question)
- Need to re-scrape for similar questions

### New Schema

```sql
players (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  normalized_name VARCHAR(255),
  nationality VARCHAR(100)
)

teams (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  team_type VARCHAR(50),
  country VARCHAR(100)
)

competitions (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  competition_type VARCHAR(50),
  country VARCHAR(100)
)

player_career_stats (
  id BIGINT PRIMARY KEY,
  player_id BIGINT REFERENCES players(id),
  team_id BIGINT REFERENCES teams(id),
  competition_id BIGINT REFERENCES competitions(id),
  season VARCHAR(20),
  appearances INTEGER,
  goals INTEGER,
  assists INTEGER,
  -- Central data store!
  UNIQUE(player_id, team_id, competition_id, season)
)

questions (
  id BIGINT PRIMARY KEY,
  question_text TEXT,
  stat_type VARCHAR(50),
  team_id BIGINT REFERENCES teams(id),  -- Foreign key!
  competition_id BIGINT REFERENCES competitions(id),
  season_filter VARCHAR(20),
  aggregation VARCHAR(50)
)

question_valid_answers (
  id BIGINT PRIMARY KEY,
  question_id BIGINT REFERENCES questions(id),
  player_id BIGINT REFERENCES players(id),
  player_name VARCHAR(255),  -- Denormalized for display
  normalized_name VARCHAR(255),  -- For fuzzy search
  score INTEGER,
  is_valid_darts_score BOOLEAN,
  is_bust BOOLEAN
)
```

**Benefits:**
- Proper normalization
- Players stored once
- Team/competition reuse
- Fast question generation

---

## Testing Migration

### Verify Data Integrity

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

# Count entities
print(f"Players: {db.get_session().query(Player).count()}")
print(f"Teams: {db.get_session().query(Team).count()}")
print(f"Competitions: {db.get_session().query(Competition).count()}")
print(f"Career Stats: {db.get_session().query(PlayerCareerStats).count()}")
print(f"Questions: {db.get_session().query(Question).count()}")
print(f"Answers: {db.get_session().query(QuestionValidAnswer).count()}")
```

### Test Question Population

```python
from jobs.populate_questions_v2 import QuestionPopulator

populator = QuestionPopulator()

# Test single question
result = populator.populate_single_question(1)
print(f"Status: {result['status']}")
print(f"Players: {result['players_added']}")

# Should be instant (< 1 second)
assert result['duration'] < 1.0, "Population taking too long!"
```

### Test Fuzzy Search

```python
# Test fuzzy matching works
db = DatabaseManager()
answers = db.get_answers_for_question(1)

# Test case-insensitive search
test_input = "haaland"
matches = [a for a in answers if test_input in a.normalized_name]

print(f"Found {len(matches)} matches for '{test_input}'")
for match in matches:
    print(f"  - {match.player_name}: {match.score}")
```

---

## Rollback Plan

If migration fails, you can rollback:

```bash
# Restore V1 database from backup
psql football501 < backup_v1.sql

# Switch code back to V1 imports
git checkout main  # or your V1 branch
```

---

## FAQ

### Q: Can I run V1 and V2 side-by-side?

**A:** Yes, if you use different database URLs:

```python
# V1
db_v1 = DatabaseManager(db_url="postgresql://user:pass@localhost/football501_v1")

# V2
db_v2 = DatabaseManager(db_url="postgresql://user:pass@localhost/football501_v2")
```

### Q: Will I lose my existing questions?

**A:** Only if you choose "Fresh Start". Use Option 2 (Preserve & Migrate) to keep them.

### Q: How long does re-scraping take?

**A:** Premier League (600 players): ~10-15 minutes with default wait times.

### Q: Can I migrate incrementally?

**A:** Not easily. V2 schema is fundamentally different. Better to migrate all at once.

### Q: What about the API integration?

**A:** V2 doesn't change how you scrape (still uses FBref scraper). You can add API-Football later without schema changes.

---

## Support

If you encounter issues during migration:

1. Check logs for error messages
2. Verify database connection settings
3. Ensure FBref is accessible
4. Review the example scripts in `example_usage_v2.py`

---

**Migration checklist:**
- [ ] Backup V1 database
- [ ] Initialize V2 schema
- [ ] Scrape player data
- [ ] Recreate questions
- [ ] Populate answers
- [ ] Test fuzzy search
- [ ] Verify answer counts
- [ ] Update application code

Good luck with the migration! 🚀
