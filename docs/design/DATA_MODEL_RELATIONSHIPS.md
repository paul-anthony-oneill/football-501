# Data Model Relationships

> **Who connects what, and why.**  
> This document explains the full chain from raw football data through to a player typing a name in-game: how `players`, `player_season_stints`, `entities`, `answers`, `questions`, and `categories` all fit together.

---

## 1. The Big Picture

There are three logically distinct layers in the system:

| Layer | Purpose | Tables |
|---|---|---|
| **Football Source Layer** | Raw scraped stats; ground truth | `players`, `player_season_stints`, `teams`, `competitions`, `seasons` |
| **Question Layer** | The game's question catalogue | `categories`, `question_templates`, `questions` |
| **Answer Layer** | Pre-materialised, game-ready answers + autocomplete | `answers`, `entities` |

The source layer feeds the answer layer via a **materialisation pipeline**. The answer layer is what the game engine touches during live play — it never reads from the source layer at runtime.

```mermaid
flowchart LR
    subgraph SOURCE["⚽ Football Source Layer"]
        P[players]
        PSS[player_season_stints]
        T[teams]
        C[competitions]
        S[seasons]
        P --> PSS
        T --> PSS
        C --> PSS
        S --> PSS
    end

    subgraph QUESTION["📋 Question Layer"]
        CAT[categories]
        QT[question_templates]
        Q[questions]
        CAT --> QT
        CAT --> Q
        QT --> Q
    end

    subgraph ANSWER["🎯 Answer Layer"]
        ANS[answers]
        ENT[entities]
    end

    SOURCE -- "Materialiser\n(Java)" --> ANSWER
    QUESTION -- "scopes\nmaterialisation" --> ANSWER
```

---

## 2. Full Entity-Relationship Diagram

```mermaid
erDiagram

    %% ── Football Source Layer ──────────────────────────────────────────────────
    players {
        UUID id PK
        string name
        string normalized_name
        string nationality
    }

    player_season_stints {
        UUID id PK
        UUID player_id FK
        UUID season_id FK
        UUID team_id FK
        UUID competition_id FK
        short appearances
        short starts
        short sub_appearances
        int   minutes
        short goals
        short assists
        short yellow_cards
        short red_cards
        short clean_sheets
        short goals_conceded
        bool  is_goalkeeper
        string source
        datetime source_scraped_at
    }

    teams {
        UUID id PK
        string name
        string normalized_name
        string team_type
        string country
        int popularity_rank
    }

    competitions {
        UUID id PK
        string name
        string normalized_name
        string competition_type
        string country
        string fbref_id
        string display_name
        short tier
    }

    seasons {
        UUID id PK
        string label
        short start_year
        short end_year
        date start_date
        date end_date
        bool is_current
    }

    %% ── Question Layer ─────────────────────────────────────────────────────────
    categories {
        UUID id PK
        string name
        string slug
        string description
    }

    question_templates {
        UUID id PK
        UUID category_id FK
        string slug
        string display_name
        text text_template
        jsonb param_schema
        string materializer_key
        string metric_key
        int default_min_score
        bool is_active
    }

    questions {
        UUID id PK
        UUID category_id FK
        UUID template_id FK
        string question_text
        string metric_key
        jsonb config
        int min_score
        int difficulty
        string status
        jsonb template_params
    }

    %% ── Answer Layer ───────────────────────────────────────────────────────────
    answers {
        UUID id PK
        UUID question_id FK
        string answer_key
        string display_text
        int score
        bool is_valid_darts
        bool is_bust
        jsonb metadata
        datetime materialized_at
    }

    entities {
        UUID id PK
        string entity_type
        string display_name
        string normalized_name
        string hint
    }

    %% ── Relationships ──────────────────────────────────────────────────────────
    players            ||--o{ player_season_stints : "has stints in"
    teams              ||--o{ player_season_stints : "hosts"
    competitions       ||--o{ player_season_stints : "scopes"
    seasons            ||--o{ player_season_stints : "belongs to"

    categories         ||--o{ question_templates   : "owns"
    categories         ||--o{ questions            : "contains"
    question_templates |o--o{ questions            : "auto-generates"

    questions          ||--o{ answers              : "has answers"
```

---

## 3. The Materialisation Pipeline

This is how a row in `player_season_stints` becomes a row in `answers` (and a row in `entities`).

```mermaid
flowchart TD
    A[Admin activates a draft Question] --> B{Hand-curated\nor template-generated?}

    B -- Template-generated --> C["QuestionTemplate\n(materializer_key, metric_key, param_schema)"]
    B -- Hand-curated --> D["Question.config JSONB\n(defines params directly)"]

    C --> E["QuestionMaterializerService\ndispatches to correct\nQuestionMaterializer by key"]
    D --> E

    E --> F["Materializer queries\nplayer_season_stints\n+ players + teams + competitions + seasons"]

    F --> G["Computes score per player\nbased on metric_key\n(goals / appearances / assists…)"]

    G --> H{score > 0?}
    H -- No --> I[Skip player]
    H -- Yes --> J["MaterializedAnswer\n(answerKey, displayText, score, metadata)"]

    J --> K["Upsert into answers\n(question_id, answer_key, score,\nis_valid_darts, is_bust)"]
    J --> L["Upsert into entities\n(entity_type, display_name, normalized_name)\n— for autocomplete only"]

    K --> M["Answer is live!\nGame engine can\nvalidate player input"]
    L --> N["Autocomplete pool\nis searchable"]
```

---

## 4. How entity_type Connects Everything

`entity_type` is the thread that ties a **question** to an **autocomplete pool** in `entities`. It is stored in `questions.config` as a JSONB key.

```mermaid
flowchart LR
    Q["Question\nconfig: {entity_type: 'footballer'}"]
    QT["QuestionTemplate\n(football question types)"]
    ANS["answers\n(answer_key: 'erling haaland'\ndisplay_text: 'Erling Haaland')"]
    ENT["entities\n(entity_type: 'footballer'\ndisplay_name: 'Erling Haaland'\nnormalized_name: 'erling haaland')"]
    FE["Frontend\nEntitySearch component"]
    BE["GET /api/entities/search\n?type=footballer&query=haala"]

    Q -- "template_id →" --> QT
    Q -- "question_id →" --> ANS
    Q -- "config.entity_type →" --> FE

    FE -- "passes entity_type\nas prop" --> BE
    BE -- "trigram search on\nnormalized_name" --> ENT

    ANS -. "populated as side-effect\nof materialisation" .-> ENT
    ENT -. "INTENTIONALLY DECOUPLED\n(appearing here ≠ valid answer)" .-> ANS
```

> **Security invariant**: `entities` is a *name pool*, not a *valid answer pool*.  
> The fact that "Erling Haaland" appears in autocomplete tells the player nothing about whether he is a valid answer to *this specific question*. All validation is done server-side against `answers` only.

---

## 5. What Connects Each Concept — Summary

| Concept | Table | Connected To | Via |
|---|---|---|---|
| **Player** | `players` | PlayerSeasonStint | `player_season_stints.player_id` |
| **Player Season Stint** | `player_season_stints` | Player, Team, Competition, Season | FK columns |
| **Entity** | `entities` | Question (indirectly) | `entity_type` slug matches `questions.config.entity_type` |
| **Answer** | `answers` | Question | `answers.question_id` |
| **Question** | `questions` | Category, Template, Answers, Entities | `category_id`, `template_id`; `config.entity_type` |
| **Category** | `categories` | Question, QuestionTemplate | `questions.category_id`, `question_templates.category_id` |

### The chain in one sentence

> A **Category** groups **Questions** (and **QuestionTemplates** that auto-generate them). A **Question** is materialised by querying **PlayerSeasonStints** (which aggregate a **Player**'s stats for a **Team** in a **Competition** during a **Season**). The materialiser writes pre-computed **Answers** (one per player, scored and darts-validated) and — as a side-effect — upserts **Entities** into the autocomplete pool, identified by the `entity_type` declared in the **Question**'s config.

---

## 6. Question Lifecycle

```mermaid
stateDiagram-v2
    [*] --> draft : Admin creates question\n(hand-curated or auto-generated\nby QuestionGeneratorService)

    draft --> active : Admin promotes\n(triggers materialisation:\nanswers + entities written)

    active --> retired : Admin retires\n(removed from rotation;\nanswers kept for replay)

    retired --> active : Admin re-activates\n(re-materialises answers)

    active --> active : Nightly stale-answer\ndetector re-materialises\nif stints updated after\nanswers.materialized_at
```

**Only `active` questions are served to players.**  
`draft` questions exist but have no answers yet.  
`retired` questions keep their `answers` rows so historical game replays remain valid.

---

## 7. Autocomplete vs. Answer Validation — Side by Side

```mermaid
sequenceDiagram
    participant Player
    participant Frontend
    participant EntityAPI as GET /api/entities/search
    participant AnswerAPI as POST /api/game/answer
    participant EntitiesDB as entities table
    participant AnswersDB as answers table

    Player->>Frontend: Types "haala"
    Frontend->>EntityAPI: ?type=footballer&query=haala
    EntityAPI->>EntitiesDB: trigram search on normalized_name\nWHERE entity_type = 'footballer'
    EntitiesDB-->>EntityAPI: ["Erling Haaland", "Martin Haaland"]
    EntityAPI-->>Frontend: autocomplete suggestions
    Frontend-->>Player: Shows dropdown

    Player->>Frontend: Selects "Erling Haaland" → submits
    Frontend->>AnswerAPI: { questionId, playerName: "Erling Haaland" }
    AnswerAPI->>AnswersDB: SELECT score FROM answers\nWHERE question_id = ? AND answer_key = 'erling haaland'
    AnswersDB-->>AnswerAPI: score = 36, is_valid_darts = true
    AnswerAPI-->>Frontend: { valid: true, score: 36 }
    Frontend-->>Player: Deducts 36 from their 501 score
```

---

*Last updated: 2026-05-26*  
*Related: [`AUTOCOMPLETE_ENTITY_DESIGN.md`](./AUTOCOMPLETE_ENTITY_DESIGN.md), [`TECHNICAL_DESIGN.md`](./TECHNICAL_DESIGN.md)*
