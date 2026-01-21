# Quick Reference - Question & Answer Operations

Fast reference for common question and answer operations.

## V3 Workflow (Recommended - Matches Java Backend)

### Initial Setup (One Time)

```bash
# 1. Initialize database
python init_db_v3.py

# 2. Scrape all Premier League history (30-40 minutes)
python scrape_all_premier_league_history.py
```

### Create a Question

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Question

db = DatabaseManager()

# Get team and competition
team = db.get_or_create_team('Manchester City', 'club', 'England')
comp = db.get_or_create_competition('Premier League', 'domestic_league', 'England')

# Create question
with db.get_session() as session:
    q = Question(
        question_text='Appearances for Manchester City in Premier League 2023-2024',
        stat_type='appearances',  # Options: 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'
        team_id=team.id,          # Optional
        competition_id=comp.id,   # Optional
        season_filter='2023-2024', # Optional (None = all seasons)
        nationality_filter=None,  # Optional
        min_score=1,              # Optional
        is_active=True
    )
    session.add(q)
    session.commit()
    print(f"Question ID: {q.id}")
```

### Populate Answers for a Question

```python
from database.crud_v3 import DatabaseManager

db = DatabaseManager()

# Populate specific question
count = db.populate_question_answers('question-uuid-here')
print(f"Populated {count} answers")
```

**Or use the script:**
```bash
python populate_question_answers.py
```

### Query Answers

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import QuestionValidAnswer

db = DatabaseManager()

with db.get_session() as session:
    # Get all answers for a question
    answers = session.query(QuestionValidAnswer).filter_by(
        question_id='question-uuid'
    ).order_by(QuestionValidAnswer.score.desc()).all()

    for answer in answers:
        print(f"{answer.player_name}: {answer.score} ({'✓' if answer.is_valid_darts_score else '✗'})")
```

### Weekly Update (Current Season)

```bash
# Update current season (1-2 minutes)
python update_current_season.py
```

---

## Common Question Templates

### 1. Team Season Appearances

```python
Question(
    question_text='Appearances for Arsenal in Premier League 2023-2024',
    stat_type='appearances',
    team_id=arsenal.id,
    competition_id=premier_league.id,
    season_filter='2023-2024',
    is_active=True
)
```

**Result:** All Arsenal players with their appearance counts for that season.

### 2. Team Season Goals

```python
Question(
    question_text='Goals for Liverpool in Premier League 2023-2024',
    stat_type='goals',
    team_id=liverpool.id,
    competition_id=premier_league.id,
    season_filter='2023-2024',
    is_active=True
)
```

**Result:** All Liverpool players with their goal counts.

### 3. Career Stats (All Seasons)

```python
Question(
    question_text='Career appearances for Manchester United in Premier League',
    stat_type='appearances',
    team_id=man_united.id,
    competition_id=premier_league.id,
    season_filter=None,  # All seasons!
    is_active=True
)
```

**Result:** All Man United players with total PL appearances across all seasons.

### 4. Combined Stats

```python
Question(
    question_text='Appearances + Goals for Chelsea in Premier League 2023-2024',
    stat_type='combined_apps_goals',
    team_id=chelsea.id,
    competition_id=premier_league.id,
    season_filter='2023-2024',
    is_active=True
)
```

**Result:** Each player's score = appearances + goals.

### 5. Goalkeeper Stats

```python
Question(
    question_text='Goalkeeper stats (Apps + Clean Sheets) for Man City 2023-2024',
    stat_type='goalkeeper',
    team_id=man_city.id,
    competition_id=premier_league.id,
    season_filter='2023-2024',
    is_active=True
)
```

**Result:** Each player's score = appearances + clean_sheets.

### 6. Nationality Filter

```python
Question(
    question_text='Appearances by Argentinian players in Premier League 2023-2024',
    stat_type='appearances',
    team_id=None,
    competition_id=premier_league.id,
    season_filter='2023-2024',
    nationality_filter='Argentina',
    is_active=True
)
```

**Result:** All Argentinian players in PL 2023-2024 with appearances.

---

## Checking Data

### Check Player Data

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Player

db = DatabaseManager()

with db.get_session() as session:
    # Total players
    player_count = session.query(Player).count()
    print(f"Total players: {player_count}")

    # Players with stats
    players_with_stats = session.query(Player).filter(
        Player.career_stats.isnot(None)
    ).count()
    print(f"Players with stats: {players_with_stats}")

    # Sample player
    player = session.query(Player).filter_by(name='Erling Haaland').first()
    if player:
        print(f"\n{player.name} career stats:")
        for season in player.career_stats or []:
            print(f"  {season['season']}: {season['team']} - {season['appearances']} apps, {season['goals']} goals")
```

### Check Question Answers

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Question, QuestionValidAnswer

db = DatabaseManager()

with db.get_session() as session:
    # All active questions
    questions = session.query(Question).filter_by(is_active=True).all()
    print(f"Active questions: {len(questions)}")

    for q in questions:
        # Count answers
        answer_count = session.query(QuestionValidAnswer).filter_by(
            question_id=q.id
        ).count()

        # Count valid darts scores
        valid_count = session.query(QuestionValidAnswer).filter_by(
            question_id=q.id,
            is_valid_darts_score=True
        ).count()

        print(f"\n{q.question_text}")
        print(f"  Total answers: {answer_count}")
        print(f"  Valid darts scores: {valid_count}")
```

---

## Database Queries

### Raw SQL Queries

```python
from database.crud_v3 import DatabaseManager
from sqlalchemy import text

db = DatabaseManager()

with db.get_session() as session:
    # Query JSONB directly
    sql = text("""
        SELECT
            p.name,
            season->>'season' as season,
            season->>'team' as team,
            (season->>'goals')::int as goals
        FROM players p,
            jsonb_array_elements(p.career_stats) as season
        WHERE (season->>'goals')::int >= 20
        ORDER BY (season->>'goals')::int DESC
        LIMIT 10
    """)

    results = session.execute(sql).fetchall()

    print("Top 10 goal scorers in a season:")
    for row in results:
        print(f"{row[0]}: {row[3]} goals ({row[1]}, {row[2]})")
```

### Find Players by Name Pattern

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Player

db = DatabaseManager()

with db.get_session() as session:
    # Partial name match
    players = session.query(Player).filter(
        Player.name.ilike('%haaland%')
    ).all()

    for player in players:
        print(f"{player.name} ({player.nationality})")
```

---

## Maintenance

### Repopulate All Questions

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Question

db = DatabaseManager()

with db.get_session() as session:
    questions = session.query(Question).filter_by(is_active=True).all()

    for q in questions:
        print(f"Repopulating: {q.question_text}")
        count = db.populate_question_answers(str(q.id))
        print(f"  ✅ {count} answers")
```

### Delete Question and Answers

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Question, QuestionValidAnswer

db = DatabaseManager()

with db.get_session() as session:
    question_id = 'question-uuid-here'

    # Delete answers first (foreign key constraint)
    session.query(QuestionValidAnswer).filter_by(
        question_id=question_id
    ).delete()

    # Delete question
    session.query(Question).filter_by(id=question_id).delete()

    session.commit()
    print("Question and answers deleted")
```

### Clear All Answers for a Question

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import QuestionValidAnswer

db = DatabaseManager()

with db.get_session() as session:
    count = session.query(QuestionValidAnswer).filter_by(
        question_id='question-uuid'
    ).delete()

    session.commit()
    print(f"Deleted {count} answers")
```

---

## Troubleshooting

### "No players found"

```bash
# Check database has players
python -c "from database.crud_v3 import DatabaseManager; db = DatabaseManager(); print('Players:', db.get_session().query(__import__('database.models_v3').models_v3.Player).count())"

# If 0, run scraper
python scrape_all_premier_league_history.py
```

### "No answers populated"

```python
# Check player has stats for this team/season
from database.crud_v3 import DatabaseManager
from database.models_v3 import Player
from sqlalchemy import text

db = DatabaseManager()

with db.get_session() as session:
    sql = text("""
        SELECT COUNT(*)
        FROM players p,
             jsonb_array_elements(p.career_stats) as season
        WHERE season->>'team' = 'Manchester City'
          AND season->>'season' = '2023-2024'
    """)

    count = session.execute(sql).scalar()
    print(f"Players with Man City stats in 2023-2024: {count}")
```

### "Database connection error"

```bash
# Test connection
psql -h localhost -U football501 -d football501

# Check .env file
cat .env | grep DB_
```

---

## Environment Variables

Required in `.env`:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=football501
DB_USER=football501
DB_PASSWORD=your_password

SQL_ECHO=false  # Set to true for query debugging
```

---

## Performance Tips

1. **Use indexes:**
   ```sql
   -- Already created by init_db_v3.py
   CREATE INDEX idx_qva_question ON question_valid_answers(question_id);
   CREATE INDEX idx_players_name_trgm ON players USING gin(name gin_trgm_ops);
   ```

2. **Batch populate:**
   - Populate multiple questions at once using `populate_question_answers.py`

3. **Incremental updates:**
   - Use `update_current_season.py` for weekly updates (only updates one season)
   - Don't rescrape entire history

4. **Query optimization:**
   - Filter on indexed columns (question_id, player_id)
   - Use `is_valid_darts_score` flag instead of recalculating

---

## Next Steps

1. **After scraping data:** `python scrape_all_premier_league_history.py` ✅
2. **Create your questions:** Use templates above
3. **Populate answers:** `python populate_question_answers.py`
4. **Integrate with Java backend:** Answers are now ready for `AnswerEvaluator.java`
5. **Weekly maintenance:** `python update_current_season.py` (Sunday 3 AM UTC)

---

## Links

- Full workflow documentation: `QUESTION_ANSWER_WORKFLOW.md`
- Database models: `database/models_v3.py`
- CRUD operations: `database/crud_v3.py`
- Java backend: `backend/ANSWER_EVALUATION_FRAMEWORK.md`
