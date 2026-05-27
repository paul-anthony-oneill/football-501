# Backend Architecture — Mermaid Diagrams

> Generated from source analysis of `backend/src/main/java/com/football501/`.  
> Spring Boot 4.x · Java 25 · PostgreSQL 15 · Flyway 12  
> Last updated: 2026-05-27 (reflects audit fix Phases 1–5)

---

## 1. High-Level System Architecture

A C4-style view of the full system. Only the backend components are currently implemented.

```mermaid
graph TB
    subgraph CLIENT["Client Layer"]
        FE["Next.js / React PWA\n(frontend-react/)"]
    end

    subgraph BACKEND["Spring Boot Backend\n(backend/)"]
        direction TB
        SEC["Security Layer\nSecurityConfig · DevModeAuthFilter"]
        CTRL["REST Controllers\n/api/practice\n/api/admin\n/api/entities\n/api/categories"]
        EXC["Exception Handling\nGlobalExceptionHandler"]
        SVC["Service Layer\nGameService · MatchService\nQuestionService · AdminAnswerService\nEntitySearchService · QuestionMaterializerService\nDifficultyRecalibrationService"]
        MAP["Mapper Layer\nAnswerMapper · CategoryMapper\n(MapStruct)"]
        ENGINE["Game Engine\nGameStateMachine · GameTransition\nAnswerEvaluator · ScoringService\nDartsValidator · DifficultyCalculator\nDifficultyConstants"]
        MAT["Materializers\nFootballTeamCompetitionSeason\nFootballTeamCompetitionMetricSince\nFootballPlayerCareerMetric\nFootballPlayerCompetitionMetricSince"]
        REPO["Repository Layer\nJPA Repositories (Spring Data)"]
    end

    subgraph DB["PostgreSQL 15"]
        QANDA["questions / answers\nentities / categories"]
        GAMEPLAY["matches / games\ngame_moves"]
        FOOTBALL["players / teams / seasons\ncompetitions / player_season_stints"]
        TEMPLATES["question_templates"]
    end

    subgraph SCRAPER["Python Microservice\n(football-501-scraper/)"]
        SC["ScraperFC\n(FBref data)"]
    end

    FE -- "HTTPS REST" --> SEC
    SEC --> CTRL
    CTRL --> EXC
    CTRL --> SVC
    SVC --> MAP
    SVC --> ENGINE
    SVC --> MAT
    SVC --> REPO
    ENGINE --> REPO
    MAT --> REPO
    REPO -- "JPA / JDBC" --> DB
    SC -- "Direct SQL\n(batch inserts)" --> FOOTBALL
```

---

## 2. Backend Package Architecture (Layers)

Shows how the main packages relate to each other, with their key responsibilities. Packages added by the audit fix campaign are marked with `(new)`.

```mermaid
graph LR
    subgraph INCOMING["HTTP In"]
        REQ(("HTTP\nRequest"))
    end

    subgraph SEC["security/ (new)"]
        SC["SecurityConfig\n@EnableMethodSecurity"]
        DMF["DevModeAuthFilter\n@Profile(!prod)"]
    end

    subgraph CTRL["controller/"]
        PGC["PracticeGameController\n(Principal identity)"]
        AAC["AdminAnswerController\n@PreAuthorize ADMIN"]
        AQC["AdminQuestionController\n@PreAuthorize ADMIN"]
        ATC["AdminTemplateController\n@PreAuthorize ADMIN"]
        ACC["AdminCategoryController\n@PreAuthorize ADMIN"]
        AEC["AdminEntityController\n@PreAuthorize ADMIN"]
        CC["CategoryController"]
        EC["EntityController"]
    end

    subgraph EXC["exception/ (new)"]
        GEH["GlobalExceptionHandler\n@RestControllerAdvice"]
    end

    subgraph SVC["service/"]
        GS["GameService\n(delegates to GSM)"]
        MS["MatchService"]
        QS["QuestionService"]
        AAS["AdminAnswerService"]
        ESS["EntitySearchService\n(bulk upsert)"]
        QMS["QuestionMaterializerService\n(zone counts + viability)"]
        QGS["QuestionGeneratorService"]
        ACS["AdminCategoryService"]
        AQS["AdminQuestionService"]
        DRS["DifficultyRecalibrationService (new)"]
    end

    subgraph MAP["mapper/ (new)"]
        AM["AnswerMapper\nMapStruct"]
        CM["CategoryMapper\nMapStruct"]
    end

    subgraph ENGINE["engine/"]
        GSM["GameStateMachine (new)\npure transition rules"]
        GT["GameTransition (new)\nimmutable result record"]
        AE["AnswerEvaluator"]
        SS["ScoringService"]
        DV["DartsValidator\n(static utility)"]
        DC["DifficultyCalculator (new)\npure static formula"]
        DCON["DifficultyConstants (new)\nall tunable parameters"]
    end

    subgraph MAT["materializer/"]
        QMI["«interface»\nQuestionMaterializer"]
        M1["FootballTeamCompetitionSeason\nMaterializer"]
        M2["FootballTeamCompetitionMetric\nSinceMaterializer"]
        M3["FootballPlayerCareer\nMetricMaterializer"]
        M4["FootballPlayerCompetition\nMetricSinceMaterializer"]
    end

    subgraph REPO["repository/"]
        AR["AnswerRepository"]
        GR["GameRepository"]
        GMR["GameMoveRepository"]
        MR["MatchRepository"]
        QR["QuestionRepository\n(difficulty/viability queries)"]
        NER["NamedEntityRepository\n(bulk upsert)"]
        PR["PlayerRepository"]
        PSR["PlayerSeasonStintRepository"]
        TR["TeamRepository"]
        SR["SeasonRepository"]
        CR["CompetitionRepository"]
        QTR["QuestionTemplateRepository"]
        CAT["CategoryRepository"]
    end

    subgraph MODEL["model/"]
        direction LR
        MOD1["Question · Answer\nNamedEntity · Category"]
        MOD2["Match · Game · GameMove"]
        MOD3["Player · Team · Season\nCompetition · PlayerSeasonStint\nQuestionTemplate"]
        MOD4["EntityType · CategorySlug (new)\nconstants classes"]
    end

    subgraph CFG["config/ (new)"]
        JC["JpaConfig\n@EnableJpaAuditing"]
    end

    REQ --> SEC
    SEC --> CTRL
    CTRL --> EXC
    CTRL --> SVC
    SVC --> MAP
    SVC --> ENGINE
    SVC --> MAT
    ENGINE --> REPO
    MAT --> REPO
    SVC --> REPO
    QMI --> M1 & M2 & M3 & M4
    REPO --> MODEL
    CFG -. "auditing" .-> MODEL
    DC -. "uses" .-> DCON
    GSM -. "returns" .-> GT
```

---

## 3. Game Engine — Answer Evaluation Flow

Sequence diagram for a single `POST /api/practice/games/{gameId}/submit` call.

Identity is derived from the Spring Security `Principal` (injected by `DevModeAuthFilter` in dev, by a JWT filter in prod). The `playerId` request parameter has been removed.

```mermaid
sequenceDiagram
    participant Client
    participant PracticeGameController
    participant GameService
    participant GameStateMachine
    participant AnswerEvaluator
    participant ScoringService
    participant DartsValidator
    participant AnswerRepository
    participant DB as PostgreSQL

    Client->>PracticeGameController: POST /api/practice/games/{id}/submit\n{answer: "Erling Haaland"}\n[Spring Security Principal: DEV_PLAYER_ID]

    Note over PracticeGameController: playerId = UUID.fromString(principal.getName())

    PracticeGameController->>GameService: processPlayerMove(gameId, playerId, answer)

    GameService->>DB: findById(gameId) → Game
    GameService->>DB: findById(matchId) → Match
    GameService->>AnswerRepository: findUsedAnswerIdsByGameId(gameId)
    DB-->>GameService: usedAnswerIds[]

    GameService->>AnswerEvaluator: evaluateAnswer(questionId, "erling haaland", 501, usedIds)

    Note over AnswerEvaluator: normalize input → "erling haaland"

    AnswerEvaluator->>AnswerRepository: findByQuestionIdAndAnswerKey(qId, "erling haaland")
    DB-->>AnswerEvaluator: Answer{score=35, isValidDarts=true}

    alt Exact match found
        AnswerEvaluator->>ScoringService: calculateScore(501, 35)
        ScoringService->>DartsValidator: isValidDartsScore(35)
        DartsValidator-->>ScoringService: true
        ScoringService-->>AnswerEvaluator: ScoreResult{newScore=466, isBust=false}
    else No exact match
        AnswerEvaluator->>AnswerRepository: findBestMatchByFuzzyName(qId, input, usedIds, 0.5)
        DB-->>AnswerEvaluator: Answer (trigram match) or empty
    end

    AnswerEvaluator-->>GameService: AnswerResult{valid, displayText, score=35, newTotal=466}

    GameService->>GameStateMachine: onMoveSubmitted(game, match, playerId, answerResult)
    Note over GameStateMachine: Classifies move (VALID/BUST/INVALID/CHECKOUT)\nApplies timer rules, close-finish rule\nNo DB dependencies — pure function
    GameStateMachine-->>GameService: GameTransition{result=VALID, scoreAfter=466, turnAdvanced=true, …}

    GameService->>DB: save(GameMove{result=VALID, scoreBefore=501, scoreAfter=466})
    GameService->>DB: save(Game{player1Score=466, currentTurnPlayerId=…})

    GameService-->>PracticeGameController: GameMove

    PracticeGameController-->>Client: 200 OK\n{result:"VALID", scoreBefore:501, scoreAfter:466, …}
```

---

## 4. Question Lifecycle — State Machine

The `excluded` status was added by the V13 migration (Phase 4). Questions that fail viability checks at materialisation time are automatically set to `excluded` by `QuestionMaterializerService`.

```mermaid
stateDiagram-v2
    [*] --> draft : Admin creates question\n(or QuestionGeneratorService)

    draft --> active : Admin promotes via\nPATCH /api/admin/questions/{id}/status\n\nTriggers QuestionMaterializerService.materialize()

    draft --> excluded : Materialisation runs;\nviability check fails\n(auto-exclusion)

    active --> excluded : Materialisation runs;\nviability check fails\n(auto-exclusion by QuestionMaterializerService)

    active --> retired : Admin retires\n(removed from game rotation;\nanswers retained for replay)

    retired --> active : Admin re-activates\n(re-materializes answers)

    excluded --> draft : Admin resets to draft\n(after fixing the question template)

    draft --> [*] : Admin deletes draft

    note right of draft
        Not visible to game engine.
        No answers yet (or stale).
    end note

    note right of active
        QuestionMaterializerService upserts
        answers + entities, computes zone
        counts, difficulty_score, and
        verifies viability before status flips.
    end note

    note right of excluded
        Auto-excluded when:
          total_score_pool < 501, OR
          total_valid_count < 15
        viability_exclusion_reason populated.
        Admin can review via
        GET /api/admin/questions?status=excluded
    end note

    note right of retired
        Historical answers preserved.
        match_history still valid.
    end note
```

---

## 5. Answer Materialisation Pipeline

How a question goes from a template to a live set of game answers.

```mermaid
flowchart TD
    A([Admin: promote draft → active]) --> B{Has templateId?}

    B -- Yes --> C[Load QuestionTemplate\nfrom question_templates]
    B -- No: hand-curated --> D[Read materializer_key\nfrom question.config JSONB]

    C --> E[Resolve QuestionMaterializer\nby template.materializer_key]
    D --> E

    E --> F[Build MaterializationContext\n question + template + params ]

    F --> G[Materializer.materialize ctx ]

    subgraph MATERIALIZERS["Materializer implementations"]
        direction LR
        G --> G1["FootballTeamCompetitionSeason\nSELECT players with stints\nfor team+competition+season"]
        G --> G2["FootballTeamCompetitionMetricSince\nSUM metric over seasons\n≥ start_year"]
        G --> G3["FootballPlayerCareerMetric\nCareer totals for specific player"]
        G --> G4["FootballPlayerCompetitionMetricSince\nPlayer stats in competition\n≥ start_year"]
    end

    G1 & G2 & G3 & G4 --> H["List&lt;MaterializedAnswer&gt;\n{answerKey, displayText, score, metadata}"]

    H --> I{score valid?}

    I -- "1 ≤ score ≤ 180\nnot in invalid set" --> J["isValidDarts = true"]
    I -- "score > 180 or\nin {163,166,169,172,173,175,176,178,179}" --> K["isValidDarts = false\nisBust = true"]

    J & K --> L[Upsert into answers table\n INSERT … ON CONFLICT UPDATE ]
    L --> M[EntitySearchService.upsertEntity\n displayText, 'footballer' \ninto entities table]
    M --> N([Question is now ACTIVE\nGame engine can select it])
```

---

## 6. Core Data Model (Entity–Relationship)

Key tables and their relationships. Audit columns (`created_at`, `updated_at`) omitted for clarity.

```mermaid
erDiagram
    categories {
        uuid id PK
        string name
        string slug
    }

    question_templates {
        uuid id PK
        string materializer_key
        jsonb param_schema
        jsonb default_params
    }

    questions {
        uuid id PK
        uuid category_id FK
        uuid template_id FK
        string question_text
        string metric_key
        jsonb config
        jsonb template_params
        string status
        int difficulty
    }

    answers {
        uuid id PK
        uuid question_id FK
        string answer_key
        string display_text
        int score
        bool is_valid_darts
        bool is_bust
        jsonb metadata
        timestamp materialized_at
    }

    entities {
        uuid id PK
        string entity_type
        string display_name
        string normalized_name
        string hint
    }

    matches {
        uuid id PK
        uuid player1_id
        uuid player2_id
        uuid category_id FK
        string type
        string format
        string status
        int difficulty
    }

    games {
        uuid id PK
        uuid match_id FK
        uuid question_id FK
        int game_number
        string status
        uuid current_turn_player_id
        int player1_score
        int player2_score
        int player1_consecutive_timeouts
        int player2_consecutive_timeouts
        uuid winner_id
        int turn_count
        int turn_timer_seconds
    }

    game_moves {
        uuid id PK
        uuid game_id FK
        uuid player_id
        int move_number
        string submitted_answer
        uuid matched_answer_id FK
        string matched_display_text
        string result
        int score_value
        int score_before
        int score_after
        bool is_timeout
    }

    players {
        uuid id PK
        string name
        string nationality
    }

    player_season_stints {
        uuid id PK
        uuid player_id FK
        uuid team_id FK
        uuid season_id FK
        uuid competition_id FK
        int appearances
        int goals
        int assists
        int clean_sheets
        timestamp updated_at
    }

    teams {
        uuid id PK
        string name
        string short_name
    }

    seasons {
        uuid id PK
        string label
        int start_year
    }

    competitions {
        uuid id PK
        string name
        string country
    }

    categories     ||--o{ questions         : "groups"
    question_templates ||--o{ questions     : "generates"
    questions      ||--o{ answers           : "has"
    matches        ||--o{ games             : "contains"
    games          ||--o{ game_moves        : "records"
    answers        ||--o{ game_moves        : "matched by"
    players        ||--o{ player_season_stints : "has"
    teams          ||--o{ player_season_stints : "in"
    seasons        ||--o{ player_season_stints : "during"
    competitions   ||--o{ player_season_stints : "in"
```

---

## 7. Practice Game — REST API Flow

End-to-end flow from "start game" to "submit answer" for the single-player practice mode.

```mermaid
sequenceDiagram
    actor Player
    participant FE as Next.js Frontend
    participant PGC as PracticeGameController
    participant MS as MatchService
    participant GS as GameService
    participant QS as QuestionService
    participant DB as PostgreSQL

    Player->>FE: Click "Start Practice"

    FE->>PGC: POST /api/practice/start\n{playerId, categorySlug, difficulty}

    PGC->>QS: getCategoryBySlug("football")
    DB-->>QS: Category
    PGC->>MS: createMatch(player1, null, categoryId, CASUAL, BEST_OF_1, difficulty)
    MS->>DB: INSERT matches\n(status=IN_PROGRESS, player2=null)
    DB-->>MS: Match

    MS->>MS: startNextGame(matchId)
    MS->>QS: getRandomQuestion(categoryId, difficulty)
    DB-->>QS: Question (status=active)
    MS->>GS: createGame(matchId, questionId, gameNumber=1)
    DB-->>MS: Game{player1Score=501, player2Score=501}

    PGC-->>FE: 200 GameStateResponse\n{gameId, questionText, currentScore:501, entityType}

    FE-->>Player: Shows question + score 501

    loop Each answer attempt
        Player->>FE: Types name, selects from autocomplete, submits

        Note over FE: Autocomplete uses\nGET /api/entities/search\nnot /api/answers (never!)

        FE->>PGC: POST /api/practice/games/{gameId}/submit\n{answer: "Erling Haaland"}\n[Spring Security Principal carries playerId]
        Note over PGC: playerId = UUID.fromString(principal.getName())\nNever from request param (spoofing prevention)
        PGC->>GS: processPlayerMove(gameId, playerId, answer)
        GS-->>PGC: GameMove{result, scoreBefore, scoreAfter}
        PGC-->>FE: 200 SubmitAnswerResponse\n{result, scoreAfter, isWin, gameState}
        FE-->>Player: Updated score / WIN screen
    end
```

---

## 8. Darts Scoring Rules — Decision Tree

How `ScoringService` and `DartsValidator` decide the outcome of an answer.

```mermaid
flowchart TD
    A([Player submits answer]) --> B{Answer found\nin answers table?}

    B -- No --> C([INVALID\nRetry allowed, timer keeps running])
    B -- Already used --> C

    B -- Yes --> D{answer.isBust?\nscore > 180}

    D -- Yes --> E([BUST\nTurn wasted, score unchanged])

    D -- No --> F{DartsValidator\n.isValidDartsScore score }

    F -- "score in {163,166,169,\n172,173,175,176,178,179}" --> E
    F -- "score < 1 or > 180" --> E

    F -- Valid --> G["newScore = currentScore − score"]

    G --> H{newScore < −10?}
    H -- Yes --> E

    H -- No --> I{−10 ≤ newScore ≤ 0?}

    I -- Yes --> J([CHECKOUT 🏆\nPlayer wins!\nClose-finish rule applies\nif multiplayer])

    I -- No --> K([VALID ✓\nScore deducted\nTurn switches to opponent])
```

---

## 9. Entity Autocomplete Architecture

How the `entities` table is kept separate from `answers` to avoid revealing valid answers during gameplay.

```mermaid
graph TB
    subgraph GAMEPLAY["During Gameplay (never reveals answers)"]
        TY["Player types\n'haal...'"] --> ES["GET /api/entities/search\n?type=footballer&query=haal"]
        ES --> ESS["EntitySearchService.search()"]
        ESS --> NER["NamedEntityRepository\ntrigram search on normalized_name\nPostgreSQL unaccent + GIN index"]
        NER --> DROP["Autocomplete dropdown\n['Erling Haaland', 'Kamaldeen Sulemana', ...]"]
        DROP --> SEL["Player selects & submits\n'Erling Haaland'"]
    end

    subgraph VALIDATION["Server-Side Validation (hidden from client)"]
        SEL --> AE["AnswerEvaluator\nevaluateAnswer(questionId, input, score, usedIds)"]
        AE --> AR["AnswerRepository\nfindByQuestionIdAndAnswerKey\nor fuzzy fallback"]
        AR --> ANS["answers table\n(only populated answers for\nthis specific question)"]
    end

    subgraph POPULATION["Answer Population (admin / materializer)"]
        ADM["Admin bulk-imports\nor MaterializerService runs"] --> AAS["AdminAnswerService\nor QuestionMaterializerService"]
        AAS --> SAVE["INSERT into answers"]
        AAS --> UPSERT["EntitySearchService.upsertEntity()\nregister name in entities"]
        UPSERT --> ENT["entities table\n(all known footballers)"]
    end

    style GAMEPLAY fill:#e8f4fd,stroke:#2196F3
    style VALIDATION fill:#fce8e8,stroke:#F44336
    style POPULATION fill:#e8fce8,stroke:#4CAF50
```

---

## 10. JPA Auditing Pattern

All model classes must follow this pattern (introduced in Phase 2). Manual `LocalDateTime.now()` in `@PrePersist` is the old pattern — do not reintroduce it.

```java
@Entity
@EntityListeners(AuditingEntityListener.class)  // required — without this, @CreatedDate/@LastModifiedDate do not fire
public class MyEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Business-purpose timestamps that are NOT audit fields stay manual:
    // @PrePersist void onPersist() { this.startedAt = LocalDateTime.now(); }
}
```

`JpaConfig.java` (`@EnableJpaAuditing`) activates this framework-wide. It is already in place — new model classes just need the `@EntityListeners` annotation.

**Test note**: `@WebMvcTest` slices do not load a real JPA context. Add `@MockitoBean JpaMetamodelMappingContext` to the test class to prevent a `NoSuchBeanDefinitionException` at test startup.

---

## 11. Error Response Convention

All REST error responses are produced by `GlobalExceptionHandler` (`com.football501.exception`). Do not add local `@ExceptionHandler` methods to individual controllers.

**Standard error shape:**
```json
{ "error": "human-readable message" }
```

**Bean-validation failure shape (400):**
```json
{
  "error": "Validation failed",
  "fieldErrors": { "fieldName": "constraint message" }
}
```

| Exception | HTTP Status |
|---|---|
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 (+ fieldErrors) |
| `IllegalStateException` | 409 |
| `DuplicateEntityException` | 409 |
| `CategoryHasQuestionsException` | 409 |

To handle a new exception type, add an `@ExceptionHandler` method to `GlobalExceptionHandler` only.
