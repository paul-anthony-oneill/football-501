# Question Pipeline

*Last updated: 2026-05-26*

This document describes the full lifecycle of a question in Trivia 501 — from template definition through answer materialisation and in-game use. It covers all question types currently implemented and explains how to add new ones.

---

## Overview

Questions are not hand-authored. They are **generated automatically** from *templates* using a pipeline that has four stages:

```
Template (DB row + Java materializer)
    │
    ▼  QuestionGeneratorService.enumerateParams()
Draft Question  (one row per valid param combo)
    │
    ▼  Admin activates via PATCH /api/admin/questions/{id}/status
Active Question  (answer rows materialised automatically)
    │
    ▼  Scraper refreshes player_season_stints
Re-materialise  (POST /api/admin/questions/{id}/rematerialize)
```

---

## Stage 1 — Templates

A `question_templates` row defines:

| Column | Purpose |
|--------|---------|
| `slug` | Unique identifier, e.g. `player_competition_goals_since` |
| `display_name` | Human label in the admin UI |
| `text_template` | Question text with `{placeholders}` |
| `param_schema` | JSONB describing which params to enumerate and how |
| `materializer_key` | Matches a registered `QuestionMaterializer` Spring bean |
| `metric_key` | Which stat to aggregate (`goals`, `appearances`, etc.) |
| `is_active` | When `false`, the generator skips this template |

### Current templates (V10–V12)

| Slug | Question text | Metric | Materializer scope |
|------|--------------|--------|--------------------|
| `team_competition_goals_since` | Goals for {team} in {competition} since {year} | `goals` | Team |
| `team_competition_appearances_since` | Appearances for {team} in {competition} since {year} | `appearances` | Team |
| `team_competition_assists_since` | Assists for {team} in {competition} since {year} | `assists` | Team |
| `team_competition_sub_appearances_since` | Substitute appearances for {team} in {competition} since {year} | `sub_appearances` | Team |
| `player_competition_goals_since` | Goals in {competition} since {year} | `goals` | League |
| `player_competition_goals_assists_since` | Goals + Assists in {competition} since {year} | `goals_assists` | League |
| `player_competition_sub_appearances_since` | Substitute appearances in {competition} since {year} | `sub_appearances` | League |
| `player_career_goals_since` | Career goals in top-flight football since {year} | `goals` | Career |

---

## Stage 2 — Question Generation

`QuestionGeneratorService.generateAll()` iterates active templates and for each one:

1. Looks up the registered `QuestionMaterializer` by `materializer_key`.
2. Calls `enumerateParams(template)` to get all valid param combinations from the source layer (`player_season_stints`).
3. For each param set, renders the question text by substituting `{placeholders}`.
4. Inserts a `status = 'draft'` question if one with the same `(template_id, template_params)` doesn't already exist (idempotent).

**Triggering generation:**
```
POST /api/admin/templates/generate          # all active templates
POST /api/admin/templates/{id}/generate     # single template
```

### Number of questions generated

| Materializer | Questions per template |
|---|---|
| `team_competition_metric_since` | One per (team × competition) where stint data exists |
| `team_competition_season_metric` | One per (team × competition × season) |
| `player_competition_metric_since` | One per competition where stint data exists |
| `player_career_metric` | One question total |

---

## Stage 3 — Materialisation

When an admin promotes a question from `draft` → `active` via:
```
PATCH /api/admin/questions/{id}/status
Body: { "status": "active" }
```

`AdminQuestionService.updateStatus()` automatically calls `QuestionMaterializerService.materialize(question)`, which:

1. Resolves the `QuestionMaterializer` by the template's `materializer_key`.
2. Calls `materialize(context)` — queries `player_season_stints` and returns `List<MaterializedAnswer>`.
3. Upserts each answer into the `answers` table (setting `is_valid_darts`, `is_bust` flags via `DartsValidator`).
4. Registers each player name in the `entities` table for autocomplete.

The `answers` table is then the **sole source of truth during gameplay** — no external API calls are made.

---

## Stage 4 — Re-materialisation

After the Python scraper refreshes `player_season_stints` with updated stats, active questions can be re-materialised without cycling their status:

```
POST /api/admin/questions/{id}/rematerialize
```

This calls the same materializer again with upsert semantics — existing answers are updated in-place, new players are added, and the `entities` pool is kept in sync.

---

## Supported Metric Keys

| `metric_key` | Source column(s) | Notes |
|---|---|---|
| `goals` | `player_season_stints.goals` | |
| `appearances` | `player_season_stints.appearances` | |
| `assists` | `player_season_stints.assists` | |
| `goals_assists` | `goals + assists` (combined) | Computed in materializer |
| `clean_sheets` | `player_season_stints.clean_sheets` | GK rows only; expect small answer pools |
| `sub_appearances` | `player_season_stints.sub_appearances` | Pre-computed by scraper as `appearances - starts` |

---

## Materializer Implementations

All materializers implement `QuestionMaterializer` and are Spring `@Component` beans. They are auto-discovered by `QuestionMaterializerService` at startup.

| Class | Key | Scope |
|---|---|---|
| `FootballTeamCompetitionMetricSinceMaterializer` | `football.team_competition_metric_since` | One club across all seasons since year |
| `FootballTeamCompetitionSeasonMaterializer` | `football.team_competition_season_metric` | One club in one specific season |
| `FootballPlayerCompetitionMetricSinceMaterializer` | `football.player_competition_metric_since` | All players in a league since year |
| `FootballPlayerCareerMetricMaterializer` | `football.player_career_metric` | All players across all top-flight leagues |

### Key differences between materializers

**Team scope** (`team_competition_*`): answers are players who played for a specific club. Tighter answer pool (~5–30 answers). Harder for the guesser — they must know which players belonged to that club.

**League scope** (`player_competition_*`): answers are every player who played in the league. Much larger answer pool (~500+ answers). Easier entry for guessers — any player in the league is valid.

**Career scope** (`player_career_*`): answers span multiple leagues (all top-flight domestic). Largest answer pool. Produces a single global question per metric.

---

## Adding a New Question Type

To add a new question type (e.g. "Appearances for National Team in Competition"):

### 1. Add repository queries (if new aggregation pattern needed)

In `PlayerSeasonStintRepository`, add a `@Query` method. Ensure `StintAggregate` projection returns all six columns including `totalSubAppearances`.

### 2. Implement the materializer

```java
@Component
public class MyNewMaterializer implements QuestionMaterializer {
    public static final String KEY = "football.my_new_key";

    @Override public String getMaterializerKey() { return KEY; }

    @Override
    public List<Map<String, Object>> enumerateParams(QuestionTemplate template) {
        // return one map per valid question instance
    }

    @Override
    public List<MaterializedAnswer> materialize(MaterializationContext ctx) {
        // query source layer, return List<MaterializedAnswer>
    }
}
```

Spring auto-discovers it. No registration needed.

### 3. Add a migration seeding the template row

```sql
INSERT INTO question_templates (
    category_id, slug, display_name, text_template, param_schema,
    materializer_key, metric_key, default_min_score, is_active
) VALUES (
    <football_category_id>,
    'my_new_slug',
    'Human-readable display name',
    'Question text with {placeholder}',
    '{"params": {...}}'::jsonb,
    'football.my_new_key',
    'goals',
    1,
    TRUE
) ON CONFLICT (slug) DO NOTHING;
```

### 4. Add the new metric key to the frontend (if needed)

In `src/lib/types/admin.ts`, add to `METRIC_KEY_OPTIONS`:
```ts
{ value: "my_metric", label: "My Metric Label" },
```

---

## Admin API Reference

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/admin/templates` | List all templates with draft/active counts |
| `GET` | `/api/admin/templates/{id}` | Get a single template |
| `POST` | `/api/admin/templates/generate` | Generate drafts for all active templates |
| `POST` | `/api/admin/templates/{id}/generate` | Generate drafts for one template |
| `GET` | `/api/admin/questions` | List questions (supports `?status=draft&categoryId=...`) |
| `PATCH` | `/api/admin/questions/{id}/status` | Transition status (`draft`→`active` triggers materialisation) |
| `POST` | `/api/admin/questions/{id}/rematerialize` | Refresh answers for an active question |

---

## Data Flow Diagram

```
Python Scraper (FBref)
        │
        ▼ upsert
player_season_stints  ◄──────────────────────────┐
        │                                         │
        ├─ enumerateParams() ──► Draft Questions  │
        │                                         │ re-materialise
        └─ materialize() ──────► answers table    │
                                        │         │
                                        ▼         │
                               Game Engine        │
                          (validates answers,      │
                           deducts scores)         │
                                                   │
                          POST /rematerialize ──────┘
```
