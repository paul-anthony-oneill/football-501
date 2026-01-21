# Current Question & Answer Workflow

**Updated:** 2026-01-21
**Schema:** Generic, domain-agnostic (V4 architecture)

## Overview

The current implementation uses a **generic schema** that is NOT tied to football-specific concepts. This allows flexibility for different question types beyond just player statistics.

## Architecture

```
Generic JSONB Config → Dynamic Filtering → Generic Answer Table → Java Backend
```

**Key Principle:** Questions and Answers are domain-agnostic. The Java backend doesn't know about "players" or "teams" - it only knows about "answers" and "scores".

---

## Database Schema

### Categories

Groups of related questions (e.g., "Premier League", "La Liga").

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    slug VARCHAR(100) UNIQUE,
    description TEXT
);
```

### Questions (Generic)

```sql
CREATE TABLE questions (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories(id),
    question_text TEXT,
    metric_key VARCHAR(50),    -- 'goals', 'appearances', 'points', etc.
    config JSONB,               -- Flexible filters
    min_score INTEGER,
    is_active BOOLEAN
);
```

**Example config:**
```json
{
  "team": "Manchester City",
  "competition": "Premier League",
  "season": "2023-2024"
}
```

### Answers (Generic - NOT tied to players)

```sql
CREATE TABLE answers (
    id UUID PRIMARY KEY,
    question_id UUID REFERENCES questions(id),
    answer_key VARCHAR(255),    -- Normalized text (unique per question)
    display_text VARCHAR(255),  -- Display name
    score INTEGER,
    is_valid_darts BOOLEAN,     -- Pre-computed
    is_bust BOOLEAN,            -- Pre-computed
    answer_metadata JSONB,      -- Flexible metadata
    UNIQUE(question_id, answer_key)
);
```

**Example answer:**
```json
{
  "answer_key": "erling haaland",
  "display_text": "Erling Haaland",
  "score": 35,
  "is_valid_darts": true,
  "is_bust": false,
  "answer_metadata": {
    "player_id": "uuid-optional",
    "team": "Manchester City"
  }
}
```

**Key Points:**
- ✅ `answer_key` is just normalized text (not a foreign key to players table)
- ✅ `answer_metadata` is flexible JSONB (can store anything)
- ✅ No direct relationship to players, teams, or competitions tables
- ✅ Java backend only needs to match `answer_key` and return `score`

---

## Workflow

### Step 1: Initialize Database

```bash
python init_db_v3.py
```

Creates tables with PostgreSQL extensions (pg_trgm for fuzzy matching).

### Step 2: Scrape Player Data

```bash
python scrape_all_premier_league_history.py
```

Populates `players` table with JSONB career stats (this is football-specific, but the schema supports any domain).

### Step 3: Create Category

```python
from database.models_v4 import Category
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine

engine = create_engine('postgresql://...')
Session = sessionmaker(bind=engine)
session = Session()

category = Category(
    name="Premier League",
    slug="premier-league",
    description="English Premier League Questions"
)
session.add(category)
session.commit()
```

### Step 4: Create Questions

**Script:** `init_questions_v2.py`

```python
from database.models_v4 import Question

# Question with filters
question = Question(
    category_id=category.id,
    question_text="Goals for Manchester City in Premier League 2023-2024",
    metric_key="goals",
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

**Config is flexible - add any filters you want:**

```python
# Nationality filter
config = {
    "competition": "Premier League",
    "nationality": "Argentina"
}

# Career stats (no season filter)
config = {
    "team": "Arsenal",
    "competition": "Premier League"
    # No season = all seasons
}

# Global stats
config = {}  # Matches everything!
```

### Step 5: Populate Answers

**Script:** `populate_answers_v2.py`

```bash
python populate_answers_v2.py
```

**What it does:**

```python
# For each question
for question in questions:
    # For each player
    for player in players:
        # Filter player's career_stats by question.config
        relevant_stats = []
        for season_stat in player.career_stats:
            # Check if season matches ALL config filters
            match = True
            for filter_key, filter_val in question.config.items():
                if str(season_stat.get(filter_key)) != str(filter_val):
                    match = False
                    break
            if match:
                relevant_stats.append(season_stat)

        # Sum the metric_key across matching seasons
        total_score = sum(
            stat.get(question.metric_key, 0)
            for stat in relevant_stats
        )

        # Create generic Answer (NOT tied to player_id!)
        if total_score > 0:
            answer = Answer(
                question_id=question.id,
                answer_key=player.normalized_name,  # Just text!
                display_text=player.name,
                score=total_score,
                is_valid_darts=is_valid_darts_score(total_score),
                is_bust=(total_score > 180 or total_score <= 0),
                answer_metadata={
                    "player_id": str(player.id),  # Optional metadata
                    "nationality": player.nationality
                }
            )
            session.add(answer)
```

**Result:** Generic answers table populated with text-based keys.

---

## Java Backend Integration

### Java Entities

**Answer.java:**
```java
@Entity
@Table(name = "answers")
public class Answer {
    @Id
    private UUID id;

    private UUID questionId;

    @Column(name = "answer_key")
    private String answerKey;  // Normalized text (NOT player_id!)

    @Column(name = "display_text")
    private String displayText;

    private Integer score;

    @Column(name = "is_valid_darts")
    private Boolean isValidDarts;

    @Column(name = "is_bust")
    private Boolean isBust;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_metadata")
    private Map<String, Object> answerMetadata;  // Flexible!
}
```

**Question.java:**
```java
@Entity
@Table(name = "questions")
public class Question {
    @Id
    private UUID id;

    private UUID categoryId;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "metric_key")
    private String metricKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config;  // Flexible filters!

    private Integer minScore;
    private Boolean isActive;
}
```

### Java Validation

**AnswerEvaluator.java:**
```java
@Service
public class AnswerEvaluator {

    public AnswerResult evaluateAnswer(
        UUID questionId,
        String userInput,
        int currentScore,
        List<UUID> usedAnswerIds
    ) {
        // Normalize input
        String normalized = userInput.trim().toLowerCase();

        // Find answer by text matching (NOT player lookup!)
        Optional<Answer> answer = answerRepository
            .findByQuestionIdAndAnswerKey(questionId, normalized);

        // Or fuzzy match
        if (answer.isEmpty()) {
            answer = answerRepository.findBestMatchByFuzzyName(
                questionId, normalized, usedAnswerIds, 0.5
            );
        }

        // Validate and return result
        // Java doesn't need to know about "players" - just text matching!
    }
}
```

**Key point:** Java backend is **domain-agnostic**. It doesn't know about football, players, or teams. It just matches text and returns scores.

---

## Key Differences from V3

### Old V3 Schema (Football-Specific)

```sql
-- Tied to domain
CREATE TABLE question_valid_answers (
    player_id UUID REFERENCES players(id),  -- Foreign key!
    team_id UUID,
    competition_id UUID,
    season_filter VARCHAR(20)
);
```

❌ Hardcoded to football domain
❌ Schema changes needed for new question types
❌ Foreign key constraints to players table

### Current Schema (Generic)

```sql
-- Domain-agnostic
CREATE TABLE answers (
    answer_key VARCHAR(255),     -- Just text!
    answer_metadata JSONB,       -- Flexible!
    -- No foreign keys to domain tables
);

CREATE TABLE questions (
    metric_key VARCHAR(50),      -- Any metric!
    config JSONB                 -- Flexible filters!
);
```

✅ Works with any domain (football, music, movies, etc.)
✅ No schema changes for new question types
✅ No foreign key dependencies
✅ Easy to extend with new metadata

---

## Example Questions

### 1. Football: Team Season Goals

```python
Question(
    category_id=premier_league_cat.id,
    question_text="Goals for Arsenal in Premier League 2023-2024",
    metric_key="goals",
    config={
        "team": "Arsenal",
        "competition": "Premier League",
        "season": "2023-2024"
    },
    is_active=True
)
```

**Answers:**
```
answer_key: "bukayo saka", display_text: "Bukayo Saka", score: 14
answer_key: "gabriel jesus", display_text: "Gabriel Jesus", score: 11
```

### 2. Football: Nationality Filter

```python
Question(
    question_text="Appearances by Brazilian players in Premier League 2023-2024",
    metric_key="appearances",
    config={
        "competition": "Premier League",
        "season": "2023-2024",
        "nationality": "Brazil"  # Flexible filter!
    }
)
```

### 3. Non-Football Example (Future)

```python
# Movie actors
Question(
    question_text="Movies starring Tom Hanks",
    metric_key="appearances",
    config={
        "actor": "Tom Hanks",
        "media_type": "movie"
    }
)

# Answer
Answer(
    answer_key="forrest gump",
    display_text="Forrest Gump",
    score=1,  # 1 appearance
    answer_metadata={"year": 1994, "genre": "Drama"}
)
```

**The schema supports ANY domain!**

---

## Advantages of Generic Schema

### 1. Flexibility
Add new question types without schema changes:
```python
# New metric types - no migration needed!
metric_key="assists"
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
    "market_value": "150M",
    "custom_field": "anything"
}
```

### 3. Java Backend Simplicity
```java
// Java doesn't need domain knowledge!
// It just matches text and returns scores
String userInput = "Erling Haaland";
Answer answer = findByAnswerKey(questionId, normalize(userInput));
return answer.getScore();  // That's it!
```

### 4. Future-Proof
Want to add music questions? Just change the data:
```python
# No code changes needed!
config={"artist": "The Beatles", "decade": "1960s"}
metric_key="songs"
```

---

## Migration from Old V3

If you have old V3 data with `player_id` foreign keys:

```python
# Convert V3 to Generic
from database.models_v3 import QuestionValidAnswer as V3Answer
from database.models_v4 import Answer as GenericAnswer

session = Session()

v3_answers = session.query(V3Answer).all()

for v3 in v3_answers:
    # Create generic answer (no player_id FK!)
    generic = GenericAnswer(
        question_id=v3.question_id,
        answer_key=v3.normalized_name,    # Text, not FK
        display_text=v3.player_name,
        score=v3.score,
        is_valid_darts=v3.is_valid_darts_score,
        is_bust=v3.is_bust,
        answer_metadata={
            "player_id": str(v3.player_id),  # Metadata, not FK
            "original_source": "v3_migration"
        }
    )
    session.add(generic)

session.commit()
```

---

## Performance Considerations

### Indexing

```sql
-- Text matching (fast with trigram)
CREATE INDEX idx_answers_key_trgm ON answers
    USING gin(answer_key gin_trgm_ops);

-- JSONB queries
CREATE INDEX idx_answers_metadata ON answers
    USING gin(answer_metadata jsonb_path_ops);

CREATE INDEX idx_questions_config ON questions
    USING gin(config jsonb_path_ops);
```

### Query Performance

**Exact match:** < 10ms
```sql
SELECT * FROM answers
WHERE question_id = 'uuid'
  AND answer_key = 'erling haaland';
```

**Fuzzy match:** < 50ms
```sql
SELECT *, similarity(answer_key, 'haland') as sim
FROM answers
WHERE question_id = 'uuid'
  AND similarity(answer_key, 'haland') > 0.5
ORDER BY sim DESC LIMIT 1;
```

---

## Summary

### Current Implementation:
- ✅ Generic, domain-agnostic schema
- ✅ `answer_key` is text (NOT foreign key to players)
- ✅ `config` is JSONB (flexible filters)
- ✅ `answer_metadata` is JSONB (flexible data)
- ✅ Java backend doesn't need domain knowledge
- ✅ Easy to extend to new domains

### When to Use This:
- ✅ You want maximum flexibility
- ✅ You plan to support multiple domains (football, music, etc.)
- ✅ You want to avoid schema migrations for new question types
- ✅ You're okay with slightly slower JSONB queries

### Files:
- **Models:** `database/models_v4.py`
- **Create Questions:** `init_questions_v2.py`
- **Populate Answers:** `populate_answers_v2.py`
- **Java Backend:** Uses generic `Answer` entity with `answer_key` text matching

---

## Quick Start

```bash
# 1. Init database
python init_db_v3.py

# 2. Scrape data
python scrape_all_premier_league_history.py

# 3. Create questions
python init_questions_v2.py

# 4. Populate answers
python populate_answers_v2.py

# 5. Java backend reads from generic 'answers' table
# No knowledge of players, teams, or competitions needed!
```

---

This is the **current, production-ready workflow** that matches the generic schema architecture.
