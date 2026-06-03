# Question Materializer — A Junior Dev's Guide

This guide walks through how the question materializer works, from a question template in the database all the way to a set of pre-computed answer rows. It assumes you know Java and SQL basics but not necessarily Spring Boot or JPA internals.

## Table of Contents

1. [What problem does it solve?](#1-what-problem-does-it-solve)
2. [The big picture](#2-the-big-picture)
3. [The five stages of materialization](#3-the-five-stages-of-materialization)
4. [Stage 1: Generator — from template to draft question](#4-stage-1-generator)
5. [Stage 2: Dispatch — finding the right materializer](#5-stage-2-dispatch)
6. [Stage 3: Query — aggregating the source data](#6-stage-3-query)
7. [Stage 4: Upsert — writing answers to the database](#7-stage-4-upsert)
8. [Stage 5: Score — difficulty and viability](#8-stage-5-score)
9. [The difficulty formula explained](#9-the-difficulty-formula-explained)
10. [The viability check](#10-the-viability-check)
11. [The backfill SQL](#11-the-backfill-sql)
12. [Cheat sheet: which file does what](#12-cheat-sheet)

---

## 1. What problem does it solve?

Trivia 501 is a trivia game. A question like *"Goals for Manchester United in the Premier League since 2000"* needs a list of correct answers — every player who scored for United in the league since 2000, with their goal count as the score.

We can't call an external API during a live game (too slow, rate-limited, unreliable). So we need to **pre-compute** all the answers and store them in our database. The question materializer is the system that does this pre-computation.

The core rule is: **the game engine reads from the `answers` table only. It never touches the raw football data (`player_season_stints`).**

## 2. The big picture

Here's the end-to-end flow:

```
question_templates row          Admin triggers generation
  (e.g. "Goals for X in Y         POST /api/admin/templates/generate
   since Z, metric=goals")              │
        │                               ▼
        │              QuestionGeneratorService calls materializer.enumerateParams()
        │              → finds every valid (team, competition, year) combo with real data
        │              → inserts one "draft" Question row per combo
        │                               │
        ▼                               ▼
  draft Question row            Admin promotes to active
  (template_params =              PATCH .../status { status: "active" }
   {team_id, comp_id,                   │
    start_year})                        ▼
                              QuestionMaterializerService.materialize(question)
                                        │
                              ┌─────────┼──────────┐
                              │         │          │
                              ▼         ▼          ▼
                    Resolve template  Find       Call materializer.
                    & materializer   params     materialize(ctx)
                              │         │          │
                              └─────────┼──────────┘
                                        ▼
                              Queries player_season_stints
                              via JPQL aggregate queries
                                        │
                                        ▼
                              Returns List<MaterializedAnswer>
                              (one per player with score > 0)
                                        │
                                        ▼
                              upsertAnswers() writes to:
                              → answers table (answer rows)
                              → entities table (autocomplete pool)
                              → questions table (difficulty score, viability)
```

**Key participants:**

| Component | Role |
|---|---|
| `QuestionMaterializer` (interface) | Strategy — each implementation handles one question *shape* |
| `QuestionMaterializerService` | Orchestrator — dispatches to the right materializer, handles upsert |
| `PlayerSeasonStintRepository` | Data access — JPQL aggregate queries over raw football data |
| `DifficultyCalculator` | Pure math — converts answer-zone counts into a 0–10 difficulty score |
| `DartsValidator` | Pure math — checks if a score is achievable in darts 501 |

## 3. The five stages of materialization

When an admin promotes a draft question to "active", five things happen in sequence:

1. **Generator** (already done earlier) — the question was created from a template
2. **Dispatch** — `QuestionMaterializerService` finds the right materializer by key
3. **Query** — the materializer calls aggregate queries on `player_season_stints`
4. **Upsert** — the returned answers are written into the `answers` and `entities` tables
5. **Score** — zone counts are accumulated and fed into `DifficultyCalculator`

Let's go through each stage in detail.

## 4. Stage 1: Generator — from template to draft question

### The template

A row in `question_templates` declares the *shape* of a question. Here's what a template row looks like conceptually:

```
slug:               team_competition_goals_since
materializer_key:   "football.team_competition_metric_since"
metric_key:         "goals"
text_template:      "Goals for {team_name} in the {competition_name} since {start_year}"
param_schema:       {
  "params": {
    "team_id":        { "type": "uuid" },
    "competition_id": { "type": "uuid", "competition_types": ["domestic_league"] },
    "start_year":     { "type": "int", "values": [2000] }
  }
}
```

The `materializer_key` is the critical link — it tells the system which Java class knows how to handle this template. The `param_schema` defines what parameters need to be filled in.

### How enumeration works

When an admin hits `POST /api/admin/templates/generate`, `QuestionGeneratorService` does this for each active template:

```java
// Pseudocode of what QuestionGeneratorService does:
for (QuestionTemplate template : activeTemplates) {
    QuestionMaterializer mat = findMaterializerByKey(template.getMaterializerKey());

    // Ask the materializer: "what valid (team, competition, year) combinations exist?"
    List<Map<String, Object>> paramSets = mat.enumerateParams(template);

    for (Map<String, Object> params : paramSets) {
        // Skip if a question with these exact params already exists
        if (!existsByTemplateIdAndTemplateParams(template.getId(), params)) {
            // Create a draft question
            Question q = new Question();
            q.setTemplateId(template.getId());
            q.setTemplateParams(params);           // e.g. {team_id: "...", competition_id: "..."}
            q.setStatus("draft");
            q.setConfig(buildConfig(template));     // includes materializer_key, metric_key
            questionRepository.save(q);
        }
    }
}
```

The enumeration step in the materializer (`enumerateParams`) looks like this for the team-competition-since materializer:

```java
// FootballTeamCompetitionMetricSinceMaterializer.enumerateParams()
public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
    int startYear = extractStartYear(template);             // default 2000
    List<String> competitionTypes = extractCompetitionTypes(template);  // default ["domestic_league"]

    // Find all competitions of the right type (e.g. Premier League, La Liga)
    List<Competition> competitions = competitionRepository.findByCompetitionType("domestic_league");

    // Filter to top-flight only (tier=1)
    competitions = competitions.stream()
        .filter(c -> c.getTier() == 1)
        .toList();

    List<Map<String, Object>> results = new ArrayList<>();
    for (Competition comp : competitions) {
        // Find every team that has at least one stint row in this competition since startYear
        List<UUID> teamIds = stintRepository.findDistinctTeamIdsByCompetitionSince(comp.getId(), startYear);
        for (UUID teamId : teamIds) {
            results.add(Map.of(
                "team_id", teamId,
                "competition_id", comp.getId(),
                "start_year", startYear
            ));
        }
    }
    return results;  // ~2,600 combos for top-5 European leagues
}
```

The result: hundreds or thousands of draft `Question` rows, each with a specific (team, competition, year) triplet in its `template_params` JSONB column.

### What "draft" means

A draft question has no answers yet. It's just a placeholder saying "here's a question we *could* ask." The answers get computed later when an admin promotes it to "active."

## 5. Stage 2: Dispatch — finding the right materializer

When an admin promotes a draft question via `PATCH /api/admin/questions/{id}/status` with `{ "status": "active" }`, this chain fires:

```
AdminQuestionService.updateStatus()
  → question.setStatus("active")
  → questionMaterializerService.materialize(question)
```

Inside `QuestionMaterializerService.materialize()`:

```java
public int materialize(Question question) {
    // Step 1: Find the parent template (questions know their template_id)
    QuestionTemplate template = resolveTemplate(question);

    // Step 2: Find the materializer by key
    QuestionMaterializer materializer = resolveMaterializer(question, template);

    // Step 3: Get the concrete params
    Map<String, Object> params = question.getTemplateParams();  // from JSONB

    // Step 4: Build the context and call the materializer
    MaterializationContext ctx = new MaterializationContext(question, template, params);
    List<MaterializedAnswer> computed = materializer.materialize(ctx);

    // Step 5: Upsert
    return upsertAnswers(question, computed);
}
```

### How resolveMaterializer works

```java
private QuestionMaterializer resolveMaterializer(Question question, QuestionTemplate template) {
    String key;
    if (template != null) {
        key = template.getMaterializerKey();       // normal path — from template
    } else {
        key = question.getConfig().get("materializer_key").toString();  // hand-curated fallback
    }

    QuestionMaterializer m = materializersByKey.get(key);
    if (m == null) {
        throw new IllegalStateException("No materializer for key: " + key);
    }
    return m;
}
```

The `materializersByKey` map is built at startup. Spring automatically finds every `@Component` that implements `QuestionMaterializer`, and the service constructor builds the map:

```java
public QuestionMaterializerService(List<QuestionMaterializer> materializers, ...) {
    this.materializersByKey = materializers.stream()
        .collect(Collectors.toMap(
            QuestionMaterializer::getMaterializerKey,  // e.g. "football.team_competition_metric_since"
            Function.identity()                         // the bean itself
        ));
}
```

This is the **Strategy pattern** — the template declares a key, and Spring finds the matching strategy bean. Adding a new question type means writing a new class that implements `QuestionMaterializer` and annotating it `@Component`. No other wiring needed.

## 6. Stage 3: Query — aggregating the source data

This is where the actual football data gets turned into answer rows. Let's trace through `FootballTeamCompetitionMetricSinceMaterializer.materialize()`.

### The JPQL query

The materializer calls this repository method:

```java
// PlayerSeasonStintRepository.java
@Query("""
    SELECT s.playerId              AS playerId,
           SUM(s.goals)            AS totalGoals,
           SUM(s.appearances)      AS totalAppearances,
           SUM(s.assists)          AS totalAssists,
           SUM(s.cleanSheets)      AS totalCleanSheets,
           SUM(s.subAppearances)   AS totalSubAppearances
      FROM PlayerSeasonStint s
      JOIN Season sn ON sn.id = s.seasonId
     WHERE s.teamId        = :teamId
       AND s.competitionId = :competitionId
       AND sn.startYear   >= :startYear
     GROUP BY s.playerId
    HAVING SUM(s.appearances) > 0
""")
List<StintAggregate> aggregateByTeamCompetitionSince(
    UUID teamId, UUID competitionId, int startYear
);
```

### What this query does, in plain English

Let's say the question is *"Goals for Manchester United in the Premier League since 2010."*

The `player_season_stints` table has one row per (player, season, team, competition). If Bruno Fernandes played for United in the Premier League in 2020-21, 2021-22, 2022-23, and 2023-24, there are **four rows** for him. Each row has its own `goals` value (e.g. 18, 10, 8, 10).

The query:

1. Filters to only rows matching **Man United** AND **Premier League** AND seasons starting **2010 or later**.
2. Groups those rows by `player_id`.
3. Sums up the `goals` (and appearances, assists, etc.) across all seasons for each player.
4. Throws away any player whose total appearances sum to zero (the `HAVING` clause — a safety net for bad data).
5. Returns one row per player with all their aggregated stats.

### The HAVING clause — why it's there

Without `HAVING SUM(s.appearances) > 0`, a player who appears in the database but with zero appearances across all seasons would still show up as a "0 score" answer. That's noise. The `HAVING` clause filters them out at the database level.

### The StintAggregate projection

JPQL queries that don't return full entities need a **projection interface**. Spring Data JPA creates a proxy object that implements this interface:

```java
interface StintAggregate {
    UUID  getPlayerId();
    long  getTotalGoals();
    long  getTotalAppearances();
    long  getTotalAssists();
    long  getTotalCleanSheets();
    long  getTotalSubAppearances();
}
```

Every aggregate query in the repository returns `List<StintAggregate>`. The interface is shared across all four aggregate queries so the calling code doesn't care which query produced the results.

### How the materializer converts aggregates into answers

Back in the materializer, the aggregates get turned into `MaterializedAnswer` records:

```java
public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
    UUID teamId        = ctx.uuidParam("team_id");        // from template_params JSONB
    UUID competitionId = ctx.uuidParam("competition_id");
    int  startYear     = ctx.intParam("start_year");
    String metricKey   = ctx.template().getMetricKey();   // "goals", "appearances", etc.

    // Run the JPQL query
    List<StintAggregate> aggregates =
        stintRepository.aggregateByTeamCompetitionSince(teamId, competitionId, startYear);

    List<MaterializedAnswer> answers = new ArrayList<>();
    for (StintAggregate agg : aggregates) {
        int score = resolveMetric(agg, metricKey);  // pick the right stat column
        if (score <= 0) continue;                   // skip zero-contribution players

        Player player = playerRepository.findById(agg.getPlayerId()).orElseThrow();

        answers.add(new MaterializedAnswer(
            player.getNormalizedName(),    // answerKey:   "bruno fernandes"
            player.getName(),              // displayText: "Bruno Fernandes"
            score,                         // score: 46 (goals)
            Map.of(                        // metadata for the UI
                "player_id", player.getId(),
                "team_id", teamId,
                "metric_key", metricKey
            )
        ));
    }
    return answers;
}
```

### The resolveMetric switch

```java
private int resolveMetric(StintAggregate agg, String metricKey) {
    return switch (metricKey) {
        case "goals"           -> (int) agg.getTotalGoals();
        case "appearances"     -> (int) agg.getTotalAppearances();
        case "assists"         -> (int) agg.getTotalAssists();
        case "clean_sheets"    -> (int) agg.getTotalCleanSheets();
        case "sub_appearances" -> (int) agg.getTotalSubAppearances();
        default -> throw new IllegalArgumentException("Unknown metric: " + metricKey);
    };
}
```

The JPQL query always selects **all five** stat columns regardless of which metric the question uses. That's deliberate — it means every materializer that calls this query can pick whatever column it needs. The database does a little extra work (summing columns you might not use), but the alternative (dynamic SQL per metric) would be far more complex for negligible performance gain.

### Where the PlayerRepository call might look like N+1

```java
for (StintAggregate agg : aggregates) {
    Player player = playerRepository.findById(agg.getPlayerId()).orElseThrow();
    ...
}
```

This is a `findById` call per player inside a loop. If 200 players scored for Man United since 2010, that's 200 individual SELECT statements. This was the **N+1 problem** that was fixed in commit `41ef10e` ("perf: fix N+1 queries in question materializer, achieving 20x speedup").

The fix: batch-fetch all players in a single query before the loop, keyed by ID. (If you're reading this and the `findById` is still in the loop, that means the fix is on a different branch — check `git log` for the status.)

### The four aggregate query variants

The repository has four aggregate queries, each serving a different question shape:

| Method | Question shape | Example |
|---|---|---|
| `aggregateByTeamCompetitionSince` | Team-scoped, date-filtered | "Goals for Man United in the PL since 2010" |
| `aggregateByTeamCompetitionSeason` | Team-scoped, single-season | "Goals for Man United in the PL 2023-24" |
| `aggregateByCompetitionSince` | League-wide, date-filtered | "Goals in the PL since 2010" |
| `aggregateCareerTotalsSince` | Career, cross-competition | "Career goals in top-flight football since 2000" |

All four return `List<StintAggregate>`. The difference is the WHERE clause:

- **Team + competition + since**: filters by team AND competition AND season start year
- **Team + competition + season**: filters by team AND competition AND exact season
- **Competition + since**: filters by competition AND season start year (no team — league-wide)
- **Career + since**: filters by season start year AND a list of competition IDs

For the career query, the caller resolves which competitions qualify (e.g., all tier-1 domestic leagues) and passes their IDs as a list. This avoids a JPQL join on `competition_type`, which would be awkward with JPA's entity model.

## 7. Stage 4: Upsert — writing answers to the database

Back in `QuestionMaterializerService.upsertAnswers()`, the materializer's `List<MaterializedAnswer>` gets persisted. "Upsert" means **update or insert** — if an answer row with this `(question_id, answer_key)` already exists, update it; otherwise, create a new row.

### Why upsert instead of delete-and-replace?

If we deleted all answers and re-inserted, the primary keys would change. Any table with a foreign key to `answers.id` would break. Upsert preserves IDs for rows that already exist.

### The batch-fetch pattern

The method does two batch fetches before the loop to avoid N+1 queries:

```java
// Fetch all existing answers for this question in ONE query
Set<String> answerKeys = computed.stream().map(MaterializedAnswer::answerKey).collect(Collectors.toSet());
Map<String, Answer> existingByKey = answerRepository
    .findByQuestionIdAndAnswerKeyIn(questionId, answerKeys)
    .stream()
    .collect(Collectors.toMap(Answer::getAnswerKey, Function.identity()));

// Collect all entity entries for batch upsert in ONE query later
List<EntitySearchService.EntityEntry> entityEntries = computed.stream()
    .map(ma -> new EntitySearchService.EntityEntry(ma.displayText(), EntityType.FOOTBALLER, null))
    .toList();
```

### The main loop

```java
for (MaterializedAnswer ma : computed) {
    boolean isValidDarts = DartsValidator.isValidDartsScore(ma.score());
    boolean isBust       = ma.score() > 180;

    Answer a = existingByKey.get(ma.answerKey());
    if (a != null) {
        // EXISTS — update in place. Hibernate dirty-checks at flush time.
        a.setScore(ma.score());
        a.setDisplayText(ma.displayText());
        a.setIsValidDarts(isValidDarts);
        a.setIsBust(isBust);
        a.setMetadata(ma.metadata());
        a.setMaterializedAt(now);
    } else {
        // NEW — collect for batch INSERT later
        a = Answer.builder()
            .questionId(questionId)
            .answerKey(ma.answerKey())
            .displayText(ma.displayText())
            .score(ma.score())
            .isValidDarts(isValidDarts)
            .isBust(isBust)
            .metadata(ma.metadata())
            .materializedAt(now)
            .build();
        newAnswers.add(a);
    }

    // Accumulate zone counts for difficulty scoring
    if (isValidDarts && !isBust) {
        totalValidCount++;
        totalScorePool += ma.score();
        if      (ma.score() >= 100) highValueCount++;   // "velocity" zone
        else if (ma.score() >= 20)  midRangeCount++;    // "navigation" zone
        else                        checkoutCount++;     // "precision" zone
    }
}
```

### Darts validation at materialization time

Every answer gets two boolean flags set during materialization:

- `isValidDarts` — true if the score can be achieved with 3 darts (1–180, excluding 163, 166, 169, 172, 173, 175, 176, 178, 179)
- `isBust` — true if the score exceeds 180

These are computed once and stored. The game engine reads them directly — it never calls `DartsValidator` during gameplay. This is another example of the "pre-compute everything" principle.

### Entity registration

After persisting answers, the method registers every player name in the `entities` table:

```java
entitySearchService.batchUpsertEntities(entityEntries);
```

This is what powers the autocomplete dropdown during gameplay. The `entities` table is a separate registry from the `answers` table. A name appearing in autocomplete does NOT mean it's a valid answer to the current question — it just means the name exists in the system. This prevents players from using autocomplete to cheat.

### The `materialized_at` timestamp

Every answer row gets a `materialized_at` timestamp set to `LocalDateTime.now()`. This is a **business timestamp**, not an audit field. It answers the question: "when was this answer row last computed from the source data?"

The stale-answer detector (a future scheduled job) will compare `answers.materialized_at` against `player_season_stints.updated_at`. If the stint data was refreshed more recently than the answer, the answer is stale and needs re-materialization.

## 8. Stage 5: Score — difficulty and viability

After all answers are upserted, the service computes the question's difficulty and checks whether it's viable:

```java
if (!question.isDifficultyLocked()) {
    question.setHighValueCount(highValueCount);
    question.setMidRangeCount(midRangeCount);
    question.setCheckoutCount(checkoutCount);
    question.setTotalValidCount(totalValidCount);
    question.setTotalScorePool(totalScorePool);

    double score = DifficultyCalculator.calculate(
        highValueCount, midRangeCount, checkoutCount, totalValidCount);
    question.setDifficultyScore(score);

    boolean viable = totalScorePool  >= 501    // enough total points?
                  && totalValidCount >= 15;    // enough distinct answers?
    question.setSingleQuestionViable(viable);

    if (!viable) {
        question.setViabilityExclusionReason(buildViabilityReason(...));
        question.setStatus("excluded");
    }
}
```

### Why zone counts are stored separately from the score

The question row stores four counts (`highValueCount`, `midRangeCount`, `checkoutCount`, `totalValidCount`) AND a computed `difficultyScore`. This is deliberate:

- The **counts** are the raw data. They only change when answers are re-materialized (i.e., when the source data changes).
- The **score** is derived from the counts via a formula.

If we want to tune the formula (change a weight, adjust a saturation threshold), we can recompute scores from stored counts instantly — no need to re-query the answers table. The `POST /api/admin/questions/recalculate-difficulty` endpoint does exactly this.

If we want to change the **zone boundaries** (e.g., move the checkout ceiling from 19 to 25), that *does* require re-counting from the answers table, because the counts would mean different things under the new boundaries.

### Difficulty locked

An admin can lock a question's difficulty score via `PATCH /api/admin/questions/{id}/difficulty-lock`. When `difficultyLocked = true`, the materialization step skips difficulty computation entirely — the admin's manual score is preserved. This is for hand-curated questions where a human wants to override the formula.

## 9. The difficulty formula explained

The formula lives in `DifficultyCalculator.java` and is pure math — no database calls, no Spring annotations. You can test it with plain JUnit.

### The zones

Every valid answer falls into one of three zones based on its score:

| Zone | Score range | Strategic role | Saturation threshold |
|---|---|---|---|
| **Checkout (precision)** | 1–19 | Finishing the game — need these to hit exactly 0 | 12 answers |
| **Mid-range (navigation)** | 20–99 | Steering your score toward a good checkout position | 40 answers |
| **High-value (velocity)** | 100–180 | Rapid score reduction early in the game | 25 answers |

The thresholds are how many answers of that type you need to "fully satisfy" that zone. 12 checkout answers is enough for viable precision play; 40 mid-range answers covers the wider 20–99 range; 25 high-value answers gives plenty of velocity.

### The formula step by step

**Step 1: Compute the "ease" score (0.0 to 1.0)**

```
ease = saturate(highValueCount, 25) × 0.50
     + saturate(midRangeCount,  40) × 0.30
     + saturate(checkoutCount,  12) × 0.20
```

The `saturate(count, threshold)` function returns `min(count / threshold, 1.0)`. It can never exceed 1.0.

So if a question has 25+ high-value answers, 40+ mid-range answers, and 12+ checkout answers, `ease = 1.0` (all three components fully saturated).

If a question has 10 high-value answers, `saturate(10, 25) = 0.4`. That component contributes `0.4 × 0.50 = 0.20` to ease instead of the full 0.50.

**Why weights of 0.50, 0.30, and 0.20?**

High-value answers contribute most to ease because having lots of 100+ options makes the early game smooth. Mid-range is next most important — it's the navigation phase. Checkout is only 20% because even a handful of checkout answers is usually enough for a close finish; the formula has a separate mechanism (the checkout floor) to handle the case of zero checkout answers.

**Step 2: Invert to get base difficulty**

```
base = 10.0 × (1.0 - ease)
```

`1.0 - ease` inverts the scale: high ease → low difficulty, and vice versa. Multiplying by 10 maps it to 0–10.

Example: `ease = 0.65` → `base = 10.0 × 0.35 = 3.5`

**Step 3: Apply the checkout floor**

```
if (checkoutCount == 0):
    base = max(base, 7.0)
```

A question with zero checkout answers has a structural problem — no way to finish precisely from a low score. The checkout floor of 7.0 says "even if everything else looks easy, this question is at least a 7.0 because players can't finish cleanly." This is stronger than just the 20% checkout weight penalty, because the weight penalty assumes "few checkout answers" not "no checkout answers."

**Step 4: Apply the depth bonus**

```
depthBonus = saturate(totalValidCount, 200) × 1.5
difficulty = clamp(base - depthBonus, 0.0, 10.0)
```

Questions with large answer pools are easier to play (more options, less memorization). The depth bonus reduces difficulty by up to 1.5 points for pools of 200+ answers. For a pool of 100 answers: `saturate(100, 200) × 1.5 = 0.5 × 1.5 = 0.75` reduction.

### Concrete example

Question: *"Goals for Manchester United in the Premier League since 2010"*

```
highValueCount = 15   (15 players scored 100+ goals)
midRangeCount  = 30   (30 players scored 20–99 goals)
checkoutCount  = 8    (8 players scored 1–19 goals)
totalValidCount = 53
```

**Ease:**
```
saturate(15, 25) × 0.50 = 0.60 × 0.50 = 0.300
saturate(30, 40) × 0.30 = 0.75 × 0.30 = 0.225
saturate(8,  12) × 0.20 = 0.67 × 0.20 = 0.133
ease = 0.300 + 0.225 + 0.133 = 0.658
```

**Base difficulty:**
```
base = 10.0 × (1.0 - 0.658) = 10.0 × 0.342 = 3.42
```

**Checkout floor:** not applied (checkoutCount > 0)

**Depth bonus:**
```
saturate(53, 200) × 1.5 = 0.265 × 1.5 = 0.398
difficulty = 3.42 - 0.398 = 3.02
```

This question scores **3.0 out of 10** — relatively easy. Makes sense: 53 valid answers including 15 high-value, with a total score pool far above 501. Players should have plenty of options.

### Edge case: zero checkout answers

Question: *"Substitute appearances for Burnley in the Premier League since 2000"*

```
highValueCount = 0
midRangeCount  = 2
checkoutCount  = 0
totalValidCount = 2
```

**Ease:**
```
0.0 × 0.50 + saturate(2, 40) × 0.30 + 0.0 × 0.20
= 0.0 + 0.05 × 0.30 + 0.0
= 0.015
```

**Base:**
```
base = 10.0 × (1.0 - 0.015) = 9.85
```

**Checkout floor:**
```
max(9.85, 7.0) = 9.85  (already above the floor)
```

**Depth bonus:**
```
saturate(2, 200) × 1.5 = 0.01 × 1.5 = 0.015
difficulty = 9.85 - 0.015 = 9.84
```

This question scores **9.8 out of 10** — extremely hard. Only 2 valid answers, no checkout options. This question also fails the viability check (totalScorePool is far below 501 and validCount is below 15), so it would be auto-excluded.

## 10. The viability check

Two conditions determine whether a question can enter standard single-question play:

| Condition | Threshold | Why |
|---|---|---|
| Total score pool | ≥ 501 | You start at 501. If all possible answers combined don't sum to 501, finishing from 501 is mathematically impossible. |
| Valid answer count | ≥ 15 | Below 15 answers, the pool exhausts in a few turns and the game becomes a memory test. |

If either condition fails, the question is auto-excluded: `status = 'excluded'` with a reason like:

```
insufficient_score_pool: 312 < 501; insufficient_answer_count: 8 < 15
```

Excluded questions are still in the database — they can be re-materialized if the source data changes (e.g., a new season adds more players), and they might become viable.

Questions that pass viability have `singleQuestionViable = true` and `viabilityExclusionReason = null`.

### How viability is used at game time

When the game engine draws a question for a solo game or multiplayer match, it queries:

```java
questionRepository.findViableByDifficultyRange(minDifficulty, maxDifficulty);
```

This filters to `singleQuestionViable = true` only. Excluded questions are invisible to the game engine.

## 11. The backfill SQL

`backend/src/main/resources/db/backfill_difficulty_scores.sql` is a one-time script (NOT a Flyway migration) that populates difficulty metrics for questions that were materialized before the Phase 4 difficulty system was deployed.

It has four steps:

**Step 1: Populate zone counts from the answers table**

```sql
WITH metrics AS (
    SELECT
        question_id,
        COUNT(*) FILTER (WHERE score BETWEEN 100 AND 180 ...) AS high_value_count,
        COUNT(*) FILTER (WHERE score BETWEEN 20  AND 99  ...) AS mid_range_count,
        COUNT(*) FILTER (WHERE score BETWEEN 1   AND 19  ...) AS checkout_count,
        COUNT(*) FILTER (WHERE is_valid_darts = TRUE AND is_bust = FALSE) AS total_valid_count,
        COALESCE(SUM(score) FILTER (...), 0) AS total_score_pool
    FROM answers
    GROUP BY question_id
)
UPDATE questions q SET ... FROM metrics m WHERE q.id = m.question_id;
```

The `FILTER (WHERE ...)` clause is a PostgreSQL-specific extension to aggregate functions. It's like a `CASE WHEN` inside the aggregate, but cleaner.

**Step 2: Compute difficulty_score from stored counts**

This reimplements the Java formula in pure SQL using `LEAST` (for saturation) and `GREATEST` (for the checkout floor and the 0.0 clamp).

**Step 3: Auto-exclude non-viable questions**

This has a guard: you run a SELECT first to review what would be excluded, then uncomment the UPDATE when you're satisfied.

**Step 4: Template diagnostic**

Shows per-template stats — exclusion rate, average answer count, average score pool. Templates with high exclusion rates likely need their `is_active` flag set to false.

The backfill SQL must stay in sync with `DifficultyConstants.java`. If you change a zone boundary or saturation constant, update the SQL literals too before re-running.

## 12. Cheat sheet: which file does what

| File | What it does |
|---|---|
| `materializer/QuestionMaterializer.java` | The strategy interface — `enumerateParams()` + `materialize()` |
| `materializer/MaterializationContext.java` | Record bundling question + template + params for the materializer call |
| `materializer/MaterializedAnswer.java` | Record representing one pre-computed answer row |
| `materializer/FootballTeamCompetitionMetricSinceMaterializer.java` | "Goals for X in Y since Z" — the most common question shape |
| `materializer/FootballPlayerCompetitionMetricSinceMaterializer.java` | "Goals in Y since Z" — league-wide, no team filter |
| `materializer/FootballPlayerCareerMetricMaterializer.java` | "Career goals in top-flight football since Z" — cross-competition |
| `materializer/FootballTeamCompetitionSeasonMaterializer.java` | "Goals for X in Y 2023-24" — single-season |
| `service/QuestionMaterializerService.java` | The orchestrator — resolves materializer, calls it, upserts answers, computes difficulty |
| `repository/PlayerSeasonStintRepository.java` | JPQL aggregate queries over the raw football data |
| `engine/DifficultyCalculator.java` | Pure math — turns zone counts into a 0–10 difficulty score |
| `engine/DifficultyConstants.java` | All tunable numbers in one place |
| `engine/DartsValidator.java` | Pure math — checks if a score is achievable with 3 darts |
| `service/QuestionGeneratorService.java` | Runs enumeration to create draft questions from templates |
| `service/AdminQuestionService.java` | Admin operations — triggers materialization on status change |
| `service/DifficultyRecalibrationService.java` | Bulk recalculation of difficulty scores from stored counts |
| `db/backfill_difficulty_scores.sql` | One-time SQL to populate difficulty metrics for pre-Phase-4 questions |
| `db/migration/V7__question_lifecycle_and_materialization.sql` | Creates `question_templates`, adds `template_id`/`template_params`/`status` to questions |
| `db/migration/V13__question_difficulty_metrics.sql` | Adds all difficulty/viability columns to questions |
| `model/PlayerSeasonStint.java` | One row per (player, season, team, competition) — the raw football data |
| `model/Answer.java` | One pre-computed answer row — what the game engine reads |
| `model/Question.java` | Question definition with template params, difficulty metrics, and status |

## A final note on the design philosophy

The whole system is built around one idea: **do the expensive work once, at materialization time, so the game engine can be fast and simple.**

- Aggregate queries run during materialization, not during gameplay
- Darts validation runs during materialization, not during gameplay
- Difficulty scoring runs during materialization, not during gameplay
- Entity registration runs during materialization, not during gameplay
- Viability checks run during materialization, not during gameplay

The game engine reads from `answers` and `questions` only — two tables with pre-computed, indexed data. This is why the game can validate answers in under 200ms even with thousands of possible answers per question.
