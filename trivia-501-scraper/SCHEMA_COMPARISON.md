# Schema Comparison: V3 vs Current (Generic)

## What Changed

The schema was updated from **football-specific (V3)** to **generic/domain-agnostic (Current)**.

---

## Key Changes

### 1. Answer Key: Foreign Key → Text

**Before (V3):**
```sql
CREATE TABLE question_valid_answers (
    player_id UUID REFERENCES players(id),  -- Foreign key!
    player_name VARCHAR(255),
    normalized_name VARCHAR(255)
);
```

❌ Tied to players table via foreign key
❌ Can't work without players table
❌ Football-specific

**After (Current):**
```sql
CREATE TABLE answers (
    answer_key VARCHAR(255),     -- Just text!
    display_text VARCHAR(255),
    answer_metadata JSONB        -- Optional player_id in metadata
);
```

✅ No foreign key - just text
✅ Works with any domain
✅ player_id optional in metadata

### 2. Question Filters: Explicit Columns → JSONB Config

**Before (V3):**
```sql
CREATE TABLE questions (
    team_id UUID,              -- Explicit column
    competition_id UUID,       -- Explicit column
    season_filter VARCHAR(20), -- Explicit column
    nationality_filter VARCHAR(100),
    stat_type VARCHAR(50)      -- 'appearances', 'goals', etc.
);
```

❌ Schema changes for new filter types
❌ Football-specific columns
❌ Limited flexibility

**After (Current):**
```sql
CREATE TABLE questions (
    category_id UUID,          -- Generic grouping
    metric_key VARCHAR(50),    -- 'goals', 'points', 'songs', etc.
    config JSONB               -- Flexible filters!
);
```

✅ Add filters without schema changes
✅ Domain-agnostic
✅ Infinite flexibility

### 3. Java Backend: Domain Knowledge → Generic Text Matching

**Before (V3):**
```java
// Java needs to know about players, teams, competitions
@Entity
public class QuestionValidAnswer {
    private UUID playerId;  // Foreign key
    private UUID teamId;
    private UUID competitionId;
}

public Optional<QuestionValidAnswer> findByPlayerName(...) {
    // Domain-specific logic
}
```

**After (Current):**
```java
// Java just matches text
@Entity
public class Answer {
    private String answerKey;  // Just text!
    private Map<String, Object> answerMetadata;  // Flexible
}

public Optional<Answer> findByAnswerKey(String normalized) {
    // Generic text matching - no domain knowledge
}
```

---

## Comparison Table

| Aspect | V3 (Football-Specific) | Current (Generic) |
|--------|----------------------|-------------------|
| **Answer Key** | Foreign key to `player_id` | Text string (`answer_key`) |
| **Question Filters** | Explicit columns (`team_id`, `competition_id`) | JSONB `config` |
| **Stat Type** | `stat_type` enum | `metric_key` (any string) |
| **Metadata** | Separate columns | JSONB `answer_metadata` |
| **Schema Changes** | Needed for new filters | Never needed |
| **Domain** | Football only | Any domain |
| **Java Backend** | Needs domain knowledge | Domain-agnostic |
| **Foreign Keys** | To players, teams, competitions | None |
| **Flexibility** | Low | High |
| **Type Safety** | High (typed columns) | Medium (JSONB validation) |
| **Query Speed** | Faster (indexed FKs) | Slightly slower (JSONB) |

---

## Example: Same Question, Different Schemas

### Goal Scoring Question

**V3 Schema:**
```python
Question(
    question_text='Goals for Manchester City in Premier League 2023-2024',
    stat_type='goals',
    team_id=UUID('...'),           # Foreign key
    competition_id=UUID('...'),    # Foreign key
    season_filter='2023-2024'
)

QuestionValidAnswer(
    question_id=UUID('...'),
    player_id=UUID('...'),         # Foreign key
    player_name='Erling Haaland',
    normalized_name='erling haaland',
    score=27
)
```

**Current Schema:**
```python
Question(
    category_id=UUID('...'),       # Generic grouping
    question_text='Goals for Manchester City in Premier League 2023-2024',
    metric_key='goals',            # Generic
    config={                       # JSONB
        "team": "Manchester City",
        "competition": "Premier League",
        "season": "2023-2024"
    }
)

Answer(
    question_id=UUID('...'),
    answer_key='erling haaland',   # Just text!
    display_text='Erling Haaland',
    score=27,
    answer_metadata={              # JSONB
        "player_id": "uuid",       # Optional
        "team": "Manchester City"
    }
)
```

---

## Migration Path

### From V3 to Current

```python
# 1. Create generic Answer from QuestionValidAnswer
def migrate_v3_to_generic(v3_answer):
    return Answer(
        question_id=v3_answer.question_id,
        answer_key=v3_answer.normalized_name,  # Text, not FK
        display_text=v3_answer.player_name,
        score=v3_answer.score,
        is_valid_darts=v3_answer.is_valid_darts_score,
        is_bust=v3_answer.is_bust,
        answer_metadata={
            "player_id": str(v3_answer.player_id),  # Metadata
            "source": "v3_migration"
        }
    )

# 2. Convert Question filters to JSONB config
def migrate_question_v3_to_generic(v3_question, team_name, comp_name):
    config = {}
    if team_name:
        config["team"] = team_name
    if comp_name:
        config["competition"] = comp_name
    if v3_question.season_filter:
        config["season"] = v3_question.season_filter
    if v3_question.nationality_filter:
        config["nationality"] = v3_question.nationality_filter

    return Question(
        category_id=get_or_create_category("Premier League").id,
        question_text=v3_question.question_text,
        metric_key=v3_question.stat_type,  # 'goals' -> 'goals'
        config=config,
        min_score=v3_question.min_score,
        is_active=v3_question.is_active
    )
```

---

## Why the Change?

### Problems with V3:
1. **Inflexible:** Adding new filter types required schema migrations
2. **Football-specific:** Couldn't reuse for other domains
3. **Foreign key constraints:** Tightly coupled to players table
4. **Java complexity:** Backend needed domain knowledge

### Benefits of Current Schema:
1. **Flexible:** Add new filters without schema changes
2. **Reusable:** Works with any domain (sports, music, movies, etc.)
3. **Decoupled:** No foreign key dependencies
4. **Simple backend:** Java just matches text and returns scores

---

## When to Use Each

### Use V3 (Football-Specific) If:
- ❌ **Not recommended** - V3 is deprecated
- Only use if you need maximum query performance on very specific football queries
- You'll never expand beyond football domain

### Use Current (Generic) If:
- ✅ **Recommended** - This is the current implementation
- You want flexibility
- You might expand to other domains
- You want to avoid schema migrations
- You prefer simpler Java backend

---

## File Locations

### V3 (Deprecated):
- `database/models_v3.py` (football-specific)
- No longer used for new questions

### Current (Active):
- `database/models_v4.py` (generic)
- `init_questions_v2.py` (creates generic questions)
- `populate_answers_v2.py` (populates generic answers)

### Java Backend:
- Should use generic `Answer` entity with `answerKey` string
- Should use generic `Question` entity with JSONB `config`

---

## Summary

The schema evolved from **football-specific (V3)** to **generic/domain-agnostic (Current)**.

**Key Changes:**
1. `player_id` FK → `answer_key` text
2. Explicit filter columns → JSONB `config`
3. `stat_type` → `metric_key`
4. Domain-specific → Domain-agnostic

**Result:** More flexible, extensible, and simpler architecture.
