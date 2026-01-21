# Question and Answer Workflow

Complete guide to creating questions and populating answers in Football 501.

## Overview

There are **two workflows** in the codebase for managing questions and answers:

1. **V3 Workflow** (Recommended) - Domain-specific schema with `question_valid_answers` table
2. **V4 Workflow** (Alternative) - Generic schema with `answers` table using JSONB config

**For new development, use the V3 workflow** as it matches the Java backend implementation.

---

## V3 Workflow (Recommended)

### Architecture

```
Player Data (JSONB) → Question Definition → Populate Answers → Java Backend Validation
```

**Files:**
- `database/models_v3.py` - Player, Question, QuestionValidAnswer models
- `database/crud_v3.py` - Database operations
- `populate_question_answers.py` - Answer population script

### Database Schema (V3)

```sql
-- Players with JSONB career stats
CREATE TABLE players (
    id UUID PRIMARY KEY,
    fbref_id VARCHAR(50) UNIQUE,
    name VARCHAR(255),
    normalized_name VARCHAR(255),
    nationality VARCHAR(100),
    career_stats JSONB,  -- Array of season objects
    ...
);

-- Questions with explicit filters
CREATE TABLE questions (
    id UUID PRIMARY KEY,
    question_text TEXT,
    stat_type VARCHAR(50),  -- 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'
    team_id UUID,           -- Optional filter
    competition_id UUID,    -- Optional filter
    season_filter VARCHAR(20),
    nationality_filter VARCHAR(100),
    min_score INTEGER,
    is_active BOOLEAN,
    ...
);

-- Pre-computed valid answers
CREATE TABLE question_valid_answers (
    id UUID PRIMARY KEY,
    question_id UUID REFERENCES questions(id),
    player_id UUID REFERENCES players(id),
    player_name VARCHAR(255),
    normalized_name VARCHAR(255),
    score INTEGER,
    is_valid_darts_score BOOLEAN,  -- Pre-computed
    is_bust BOOLEAN,                -- Pre-computed
    ...
);
```

### Workflow Steps

#### Step 1: Scrape Player Data

**Script:** `scrape_all_premier_league_history.py`

```bash
python scrape_all_premier_league_history.py
```

This populates the `players` table with JSONB career statistics:

```json
{
  "career_stats": [
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
}
```

#### Step 2: Create a Question

**Manual (Python):**

```python
from database.crud_v3 import DatabaseManager
from database.models_v3 import Question

db = DatabaseManager()

# Get team and competition
man_city = db.get_or_create_team('Manchester City', 'club', 'England')
premier_league = db.get_or_create_competition('Premier League', 'domestic_league', 'England')

# Create question
with db.get_session() as session:
    question = Question(
        question_text='Appearances for Manchester City in Premier League 2023-2024',
        stat_type='appearances',  # 'appearances', 'goals', 'combined_apps_goals', 'goalkeeper'
        team_id=man_city.id,
        competition_id=premier_league.id,
        season_filter='2023-2024',  # Optional: specific season
        nationality_filter=None,    # Optional: filter by nationality
        min_score=1,                # Optional: minimum score threshold
        is_active=True
    )
    session.add(question)
    session.commit()

    print(f"Created question: {question.id}")
```

#### Step 3: Populate Answers

**Script:** `populate_question_answers.py`

```bash
python populate_question_answers.py
```

**What it does:**

1. **Queries JSONB data** using PostgreSQL JSONB operations:
   ```sql
   SELECT
       p.id, p.name, p.normalized_name,
       (season->>'appearances')::int as score
   FROM players p,
        jsonb_array_elements(p.career_stats) as season
   WHERE season->>'team' = 'Manchester City'
     AND season->>'competition' = 'Premier League'
     AND season->>'season' = '2023-2024'
     AND (season->>'appearances')::int > 0
   ```

2. **Pre-computes validation flags:**
   - `is_valid_darts_score`: Checks if score is achievable in darts (1-180, excluding 163, 166, 169, 172, 173, 175, 176, 178, 179)
   - `is_bust`: Checks if score > 180

3. **Inserts into `question_valid_answers`** table

**Example output:**
```
Populating answers for: Appearances for Manchester City in Premier League 2023-2024
  Stat type: appearances
  Team filter: Manchester City
  Competition filter: Premier League
  Season filter: 2023-2024
✅ Populated 27 valid answers
```

#### Step 4: Gameplay (Java Backend)

The Java backend reads from `question_valid_answers` for real-time validation:

```java
// In AnswerEvaluator.java
AnswerResult result = answerEvaluator.evaluateAnswer(
    questionId,
    "Erling Haaland",  // User input
    501,               // Current score
    usedAnswerIds      // Already used answers
);

// Result contains:
// - Is valid? (found in database)
// - Score (35 appearances)
// - Is valid darts score? (true)
// - Is bust? (false)
// - New total (466)
// - Is win? (false)
```

### Stat Types

| stat_type | Calculation | Example |
|-----------|-------------|---------|
| `appearances` | Sum of appearances | 35 |
| `goals` | Sum of goals | 27 |
| `combined_apps_goals` | appearances + goals | 62 |
| `goalkeeper` | appearances + clean_sheets | 40 |

### Filters

**Team Filter:**
```python
team_id=man_city.id  # Only players for this team
```

**Competition Filter:**
```python
competition_id=premier_league.id  # Only this competition
```

**Season Filter:**
```python
season_filter='2023-2024'  # Specific season
season_filter=None          # All seasons (career stats)
```

**Nationality Filter:**
```python
nationality_filter='Argentina'  # Only Argentinian players
```

### Complete Example

```python
from database.crud_v3 import DatabaseManager

db = DatabaseManager()

# 1. Get references
arsenal = db.get_or_create_team('Arsenal', 'club', 'England')
pl = db.get_or_create_competition('Premier League', 'domestic_league', 'England')

# 2. Create question
from database.models_v3 import Question
with db.get_session() as session:
    q = Question(
        question_text='Goals for Arsenal in Premier League 2023-2024',
        stat_type='goals',
        team_id=arsenal.id,
        competition_id=pl.id,
        season_filter='2023-2024',
        is_active=True
    )
    session.add(q)
    session.commit()
    q_id = str(q.id)

# 3. Populate answers
count = db.populate_question_answers(q_id)
print(f"Populated {count} answers")

# Result: All Arsenal players who scored in PL 2023-2024
# with their goal counts as scores
```

---

## V4 Workflow (Alternative)

### Architecture

```
Generic JSONB Config → Dynamic Filtering → Generic Answer Table
```

**Files:**
- `database/models_v4.py` - Category, Question, Answer models (generic schema)
- `init_questions_v2.py` - Creates questions programmatically
- `populate_answers_v2.py` - Populates generic answers table

### Database Schema (V4)

```sql
-- Categories (grouping mechanism)
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    slug VARCHAR(100) UNIQUE,
    description TEXT
);

-- Generic questions with JSONB config
CREATE TABLE questions (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories(id),
    question_text TEXT,
    metric_key VARCHAR(50),    -- 'goals', 'appearances', etc.
    config JSONB,               -- Flexible filters {"team": "...", "competition": "..."}
    min_score INTEGER,
    is_active BOOLEAN,
    ...
);

-- Generic answers table
CREATE TABLE answers (
    id UUID PRIMARY KEY,
    question_id UUID REFERENCES questions(id),
    answer_key VARCHAR(255),    -- Normalized player name (unique per question)
    display_text VARCHAR(255),  -- Display name
    score INTEGER,
    is_valid_darts BOOLEAN,
    is_bust BOOLEAN,
    answer_metadata JSONB,      -- Flexible metadata {"player_id": "..."}
    ...
);
```

### Workflow Steps (V4)

#### Step 1: Create Category

**Script:** `init_questions_v2.py`

```python
from database.models_v4 import Category, Question

# Create category
category = Category(
    name="Premier League",
    slug="premier-league",
    description="English Premier League Stats"
)
session.add(category)
session.commit()
```

#### Step 2: Create Questions

**Script:** `init_questions_v2.py`

Creates questions for all teams automatically:

```python
for team in teams:
    # Goals question
    q_goals = Question(
        category_id=category.id,
        question_text=f"{team.name} - Premier League Goals",
        metric_key="goals",
        config={
            "team": team.name,
            "competition": "Premier League"
        },
        min_score=1,
        is_active=True
    )
    session.add(q_goals)

    # Appearances question
    q_apps = Question(
        category_id=category.id,
        question_text=f"{team.name} - Premier League Appearances",
        metric_key="appearances",
        config={
            "team": team.name,
            "competition": "Premier League"
        },
        min_score=1,
        is_active=True
    )
    session.add(q_apps)
```

#### Step 3: Populate Answers

**Script:** `populate_answers_v2.py`

```bash
python populate_answers_v2.py
```

**What it does:**

1. Iterates through all active questions
2. For each player's `career_stats` (JSONB):
   - Applies filters from `question.config`
   - Sums the `metric_key` field across matching seasons
   - Creates generic `Answer` record

```python
for player in players:
    for question in questions:
        # Apply config filters
        relevant_stats = []
        for season_stat in player.career_stats:
            match = True
            for filter_key, filter_val in question.config.items():
                if str(season_stat.get(filter_key)) != str(filter_val):
                    match = False
                    break
            if match:
                relevant_stats.append(season_stat)

        # Calculate score
        total_score = sum(stat.get(question.metric_key, 0) for stat in relevant_stats)

        # Create answer
        if total_score > 0:
            answer = Answer(
                question_id=question.id,
                answer_key=player.normalized_name,
                display_text=player.name,
                score=total_score,
                is_valid_darts=is_valid_darts_score(total_score),
                is_bust=(total_score > 180),
                answer_metadata={"player_id": str(player.id)}
            )
            session.add(answer)
```

### Key Differences: V3 vs V4

| Aspect | V3 (Recommended) | V4 (Alternative) |
|--------|-----------------|------------------|
| **Schema** | Domain-specific (QuestionValidAnswer) | Generic (Answer) |
| **Filters** | Explicit columns (team_id, competition_id) | JSONB config |
| **Flexibility** | Fixed filters, optimized queries | Dynamic filters, more flexible |
| **Java Backend** | ✅ Implemented | ❌ Not implemented |
| **Indexing** | Easier (direct columns) | Harder (JSONB indexing) |
| **Type Safety** | Better (typed columns) | Weaker (JSONB values) |
| **Use Case** | Football 501 specific | Generic trivia platform |

---

## Recommendations

### ✅ Use V3 When:
- You're building Football 501 gameplay (matches V3 Java backend)
- You want optimized queries with explicit filters
- You need type-safe database operations
- You're following the architecture in `CLAUDE.md`

### ⚠️ Use V4 When:
- You're prototyping with dynamic question types
- You need maximum flexibility in filters
- You're building a generic trivia platform
- You want to avoid schema changes for new filter types

---

## Performance Comparison

### V3 (Optimized)

```sql
-- Fast: Uses indexes on team_id, competition_id, season_filter
SELECT * FROM question_valid_answers
WHERE question_id = 'uuid'
  AND normalized_name = 'erling haaland';
```

**Query time:** < 10ms

### V4 (Generic)

```sql
-- Slower: JSONB operations + text matching in config
SELECT * FROM answers
WHERE question_id = 'uuid'
  AND answer_key = 'erling haaland';

-- Needs GIN index on config for performance:
CREATE INDEX idx_answers_config ON answers USING GIN (answer_metadata jsonb_path_ops);
```

**Query time:** 10-50ms (depending on JSONB index)

---

## Migration Path

If you started with V4 and want to move to V3:

```python
# 1. Create V3 schema
python init_db_v3.py

# 2. Scrape player data (if not already done)
python scrape_all_premier_league_history.py

# 3. Migrate questions from V4 to V3
from database.models_v4 import Question as QuestionV4
from database.models_v3 import Question as QuestionV3
from database.crud_v3 import DatabaseManager

db = DatabaseManager()

with db.get_session() as session:
    v4_questions = session.query(QuestionV4).filter_by(is_active=True).all()

    for q4 in v4_questions:
        # Extract team/competition from config
        team_name = q4.config.get('team')
        comp_name = q4.config.get('competition')

        team = db.get_or_create_team(team_name, 'club', 'England')
        comp = db.get_or_create_competition(comp_name, 'domestic_league', 'England')

        # Create V3 question
        q3 = QuestionV3(
            question_text=q4.question_text,
            stat_type=q4.metric_key,  # 'goals' -> 'goals'
            team_id=team.id,
            competition_id=comp.id,
            is_active=True
        )
        session.add(q3)

    session.commit()

# 4. Populate V3 answers
python populate_question_answers.py
```

---

## Troubleshooting

### "No answers populated"

**V3:**
- Check player data exists: `SELECT COUNT(*) FROM players WHERE career_stats IS NOT NULL`
- Verify JSONB structure matches expected format
- Check filters match actual data (team names, competition names must be exact)

**V4:**
- Check `question.config` matches player `career_stats` field names
- Verify `metric_key` exists in player stats

### "Duplicate key error"

**V3:**
- `question_valid_answers` has unique constraint on (question_id, player_id)
- Delete old answers before repopulating: `DELETE FROM question_valid_answers WHERE question_id = 'uuid'`

**V4:**
- `answers` has unique constraint on (question_id, answer_key)
- Player names must be unique per question
- Script handles this by skipping duplicates

### "Slow answer population"

**V3:**
- Ensure trigram indexes exist: `CREATE INDEX idx_players_name_trgm ON players USING gin(name gin_trgm_ops)`
- Batch operations handled automatically by `crud_v3.py`

**V4:**
- Use bulk inserts: `session.bulk_save_objects(answers_batch)`
- Commit every 10,000 answers to avoid memory issues

---

## Summary

**Current Recommended Workflow (V3):**

1. **Scrape Data:** `python scrape_all_premier_league_history.py` (one time)
2. **Create Question:** Manually define in Python with explicit filters
3. **Populate Answers:** `python populate_question_answers.py` (runs SQL query on JSONB)
4. **Gameplay:** Java backend reads from `question_valid_answers` table

**Key Advantage:** Pre-computed validation, optimized queries, matches Java backend architecture.
