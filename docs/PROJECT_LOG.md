# Football 501 - Project Implementation Log

**Last Updated**: 2026-05-27  
**Status**: Audit Fixes Complete (Phase 5 of 5)

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Implementation Phase: Game Engine & Admin UI](#implementation-phase-game-engine--admin-ui)
3. [Documents Created](#documents-created)
4. [Code Implemented](#code-implemented)
5. [Current Status](#current-status)
6. [Next Steps](#next-steps)
7. [Audit Fix Sessions (2026-05-27)](#audit-fix-sessions-2026-05-27)

---

## Project Overview

**Football 501** is a competitive football trivia game that combines football knowledge with darts 501 scoring mechanics.

### Tech Stack
- **Frontend**: SvelteKit + TypeScript + Tailwind CSS
- **Backend**: Spring Boot 3.x + Java 17+ + PostgreSQL 15+
- **Data Source**: ScraperFC (Python)

---

## Implementation Phase: Game Engine & Admin UI

### Session Date: 2026-02-04

### Objective
Implement the core gameplay loop (Game Engine) in the backend and provide an administrative interface in the frontend to manage the game content (Categories and Questions).

### Achievements

#### 1. Backend: Game Engine & V3 Migration
- **Database Schema**: Applied `V3__add_game_tables.sql` to introduce `matches`, `games`, and `game_moves`.
- **Practice Mode**: Implemented `PracticeGameController` to handle single-player game flow.
- **Core Logic**: Validated scoring logic (`ScoringService`) and darts rules (`DartsValidator`).
- **State Management**: Games track turns, scores (501 countdown), and win conditions (checkout).

#### 2. Frontend: Admin Interface
- **Routes**: Created `/admin/categories` and `/admin/questions` routes.
- **Components**:
  - `DataTable`: Reusable table with pagination and actions.
  - `FormModal`: Reusable modal for create/edit forms.
  - `ConfirmDialog`: Safety check for deletions.
- **Functionality**:
  - Full CRUD for Categories.
  - List/Filter/Delete for Questions.

---

## Documents Created/Updated

### 1. `IMPLEMENTATION_SUMMARY.md` (Updated)
Reflected the completion of Phase 3, highlighting the new Game Engine capabilities and Admin UI features.

### 2. `docs/CURRENT_IMPLEMENTATION.md` (Updated)
- Added "Admin Interface" section to Frontend documentation.
- Updated Database Schema diagram to include `matches`, `games`, and `game_moves` tables.

---

## Code Implemented

### Backend
- `PracticeGameController.java`: Endpoints for `start`, `submit` answer, and `get` game state.
- `GameService.java`: Orchestrates move processing, turn validation, and state persistence.
- `ScoringService.java` & `DartsValidator.java`: Implements the 501 scoring rules.
- `V3__add_game_tables.sql`: Database migration script.

### Frontend
- `routes/admin/categories/+page.svelte`: Admin page for categories.
- `routes/admin/questions/+page.svelte`: Admin page for questions.
- `lib/api/admin.ts`: API client service.

---

## Current Status

### ✅ Completed
- **Data Source**: ScraperFC validated and integrated (Phase 2).
- **Game Engine**: Single-player practice mode working (Phase 3).
- **Admin UI**: Basic content management operational (Phase 3).

### ⏳ Pending
- **Multiplayer**: Real-time 1v1 matches (WebSocket).
- **Authentication**: User accounts and secure access.
- **Production Deployment**: Hosting and CI/CD pipelines.

---

## Next Steps

1. **Multiplayer Implementation**: Begin work on WebSocket handlers for real-time play.
2. **User Authentication**: Secure the admin routes and allow player profiles.
3. **Integration**: Fully connect the Python scraper to populate the new V3 schema tables automatically.

---

## Design Session: Game Modes & Stretch Goals

### Session Date: 2026-05-26

### Key Design Insight
The interesting difficulty in Football 501 is **strategic, not knowledge-based**. The game's depth comes from deciding *when* to play an answer and *which score to target*, not just from knowing valid answers. This means question difficulty ≠ game difficulty, and easy/medium questions can produce deeply competitive games.

### Decisions Made
- **Question pool weighting / difficulty stratification**: Parked as a stretch goal. The priority is a solid, well-known-clubs question pool for the core game.
- **Multiple game modes**: Designed and documented, but not being implemented yet. See `docs/design/GAME_MODES_STRETCH_GOALS.md`.

### Modes Documented (stretch goals)
| Mode | Summary |
|---|---|
| Daily Challenge | One question/day, global leaderboard, solo, easy/medium questions only |
| Standard H2H | Current implementation — one question, shared answer pool |
| Rapid Fire H2H | Question changes every dart; only one answer needed per question |
| Draft Mode | Pick from 3 questions each turn |
| Category Lock | Pre-agreed category for the whole match |
| Blind Mode | Question hidden until your turn |
| Tournament/League | Brackets or league tables over multiple games |

### Architectural Guardrails (act on these now)
These small decisions in the core game keep future modes open without building them:
- `game_mode` column on `matches` (default `'STANDARD'`)
- `question_id` on `game_moves` (not just on `games`) — needed for Rapid Fire
- `difficulty_score NUMERIC(4,2)` on `questions` — continuous 0–10 scale (see Difficulty Scoring session below)
- `suitable_for_daily` boolean on `questions`

---

## Design Session: Question Difficulty Scoring

### Session Date: 2026-05-26

### Problem
The existing `difficulty INTEGER` (1/2/3 scale) is a classification, not a measurement. The variance within a single tier is too wide — the hardest "Easy" question and the easiest "Medium" question may play identically.

### Key Design Insight: Knowability Correlates With Score Value
A player who scored 175 goals is a household name. A player who scored 3 goals is obscure. The *effective* answer pool a player can draw from is always skewed toward high-value answers. This means measuring the statistical distribution of all answers is misleading — what matters is the count of answers players will realistically know (high-value) and whether the critical game phases are covered.

### Decisions Made

**Replaced `difficulty_tier ENUM` with `difficulty_score NUMERIC(4,2)` (0.00–10.00)**
- Labels like "Easy" or "Hard" are derived from score ranges at render time and never stored
- Avoids the bucketing cliff-edge problem entirely

**Three score zones with relative importance**
| Zone | Range | Weight | Role |
|---|---|---|---|
| High-value | 100–180 | 50% | Velocity; most knowable answers |
| Mid-range | 20–99 | 30% | Navigation toward checkout |
| Checkout | 1–19 | 20% | Precision approach; absence is severely penalised |

**Checkout floor**: If `checkout_count = 0`, minimum difficulty is 7.0 regardless of other zones.

**Depth modifier**: Large total answer pools reduce difficulty by up to 1.5 points (saturates at 200 answers). Shifts game from knowledge to strategy.

**Raw counts stored alongside computed score**: Changing formula constants only requires a single SQL UPDATE — no re-materialisation of answers needed. Zone boundary changes are the expensive operation.

**`difficulty_locked BOOLEAN`**: Admin override flag to pin individual question scores when the formula computes incorrectly.

**`single_question_viable BOOLEAN`**: `true` when sum of valid answer scores ≥ 501. Questions failing this are excluded from standard single-question mode.

### Documents Created
- `docs/design/DIFFICULTY_SCORING.md` — full design, formula, implementation plan
- Question draw logic in a dedicated service method (not inline)

---

## Audit Fix Sessions (2026-05-27)

A comprehensive audit (`docs/AUDIT_SUMMARY.md`) was completed and all findings were fixed across 5 phases. Each phase is a single commit on the `feature/audit-fixes` branch.

---

### Audit Phase 1 — Security Architecture (commit `738c13d`)

**Date**: 2026-05-27

**Findings addressed**:
- Public admin endpoints with no authentication (CRITICAL)
- Identity spoofing via `@RequestParam UUID playerId` in game controllers (CRITICAL)
- Missing Spring Security framework entirely

**What was built**:

| File | Purpose |
|---|---|
| `security/SecurityConfig.java` | Spring Security filter chain; URL-level access rules; `@EnableMethodSecurity` |
| `security/DevModeAuthFilter.java` | Injects fixed principal (`DEV_PLAYER_ID`) on every non-prod request |

**Key decisions**:
- `@PreAuthorize("hasRole('ADMIN')")` added at class level on all 5 admin controllers. Annotations were pre-existing placeholders; `@EnableMethodSecurity` now makes them effective.
- `playerId` removed as a `@RequestParam` from `PracticeGameController`. Identity is now read from `Principal.getName()` and parsed as a UUID.
- CSRF disabled for this stateless REST + SPA architecture. Re-evaluation note left in `SecurityConfig` Javadoc for when JWT cookies are introduced.
- `DevModeAuthFilter` is `@Profile("!prod")` — it creates no bean on the `prod` profile, so the production path is: add a JWT filter, activate it on `prod`, and the dev filter disappears automatically.
- Session policy set to `STATELESS`.

**See**: `docs/SECURITY_ARCHITECTURE.md` for the full model and production migration path.

---

### Audit Phase 2 — Backfill Upsert, JPA Auditing, Normalisation Contract (commit `972dc5f`)

**Date**: 2026-05-27

**Findings addressed**:
- N+1 + race condition in `EntitySearchService.backfillFromPlayers()` — 34k+ sequential DB queries for 17k players
- Manual `LocalDateTime.now()` in `@PrePersist` across 12+ model classes (clock skew risk, no `updatedAt`)
- No test pinning the contract between Java accent-stripping and PostgreSQL `unaccent()`

**What was built / changed**:

**Backfill upsert** — `EntitySearchService.backfillFromPlayers()` replaced from a check-then-insert loop to a single native SQL:
```sql
INSERT INTO named_entities (entity_type, display_name, normalized_name, hint, created_at)
SELECT 'footballer', p.name, lower(unaccent(p.name)), p.nationality, now()
FROM players p
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
```
Race-condition-safe and restartable. The method now accepts an `entityType` parameter (was hardcoded to `"footballer"`) so future coach/city backfills can reuse it.

**JPA auditing** — `JpaConfig.java` added with `@EnableJpaAuditing`. All model classes migrated from `@PrePersist` manual timestamps to:
```java
@EntityListeners(AuditingEntityListener.class)
// ...
@CreatedDate   @Column(updatable = false) LocalDateTime createdAt;
@LastModifiedDate                          LocalDateTime updatedAt;
```

**Normalisation contract test** — `EntitySearchNormalisationIT.java` (TestContainers + real PostgreSQL):
1. Inserts `"Sergio Agüero"` into `named_entities`
2. Calls `EntitySearchService.searchEntities("footballer", "aguero", 10)` from Java
3. Asserts the result contains the inserted entity

This pins the Java `Normalizer`-based stripping against PostgreSQL `unaccent()`. The test will fail immediately if they diverge.

---

### Audit Phase 3 — GameStateMachine, useGameLoop Hook, Admin Page Decomposition (commit `4ad75ce`)

**Date**: 2026-05-27

**Findings addressed**:
- No state-machine coordinator — game transition logic was leaking into `GameService`
- Game state in `MatchView.tsx` and parent page mixed local `useState`, prop drilling, inline API calls
- `frontend-react/src/app/admin/questions/[id]/page.tsx` at 622 lines — mixing data fetching, form state, bulk-import, and answer table management

**What was built**:

**Backend — `GameStateMachine`** (`engine/GameStateMachine.java`):
- Pure function: accepts `(Game, Match, UUID playerId, AnswerResult)`, returns immutable `GameTransition`
- No repository dependencies — `GameService` loads data and persists the result; the machine only computes the transition
- Owns all turn-machine rules: INVALID (retry, timer keeps running), BUST (no score change, turn advances), VALID (deduct score, advance turn, reset timeout counter), CHECKOUT (close-finish rule), TIMEOUT (consecutive counter, timer reduction, forfeit at threshold 3)
- Close-finish rule encoded: if Player 1 checks out, Player 2 gets one final turn
- Timer constants (`DEFAULT_TIMER=45`, `REDUCED_TIMER_1=30`, `REDUCED_TIMER_2=15`, `FORFEIT_TIMEOUT_THRESHOLD=3`) defined as public fields for test assertions

**Frontend — `useGameLoop` hook** (`hooks/useGameLoop.ts`):
- Owns all game session state: `score`, `question`, `turnCount`, `gameStatus`, `moves`, `entityType`, `hints`
- Owns all API calls: `startNewGame`, `submitAnswer`, `exitGame`
- Owns WebSocket lifecycle (placeholder — will be filled in when multiplayer is implemented)
- `page.tsx` reduced to lobby state (`categories`, `selectedCategorySlug`, `playerName`, `gameMode`) + conditional render of `LobbyView` vs `MatchView`

**Frontend — admin page decomposition**:
- `questions/[id]/page.tsx`: 622 lines → 113 lines (thin coordinator only)
- Extracted: `hooks/useQuestionDetail.ts` (data fetching + mutations), `components/admin/QuestionMetaPanel.tsx`, `components/admin/AnswerTableSection.tsx`

---

### Audit Phase 4 — DifficultyCalculator, Viability Gate, V13 Migration (commit `932d9d9`)

**Date**: 2026-05-27

**Findings addressed**:
- "Broken template" risk: questions that structurally cannot produce enough valid answers were not auto-excluded
- `difficulty INTEGER` (1/2/3) replaced with continuous `difficulty_score NUMERIC(4,2)` (0.00–10.00)
- No implementation of the difficulty formula designed in `docs/design/DIFFICULTY_SCORING.md`

**What was built**:

**V13 migration** (`V13__question_difficulty_metrics.sql`) — new columns on `questions`:
```sql
high_value_count         INTEGER     NOT NULL DEFAULT 0,
mid_range_count          INTEGER     NOT NULL DEFAULT 0,
checkout_count           INTEGER     NOT NULL DEFAULT 0,
total_valid_count        INTEGER     NOT NULL DEFAULT 0,
total_score_pool         INTEGER     NOT NULL DEFAULT 0,
difficulty_score         NUMERIC(4,2) NULL,
difficulty_locked        BOOLEAN     NOT NULL DEFAULT false,
single_question_viable   BOOLEAN     NOT NULL DEFAULT false,
viability_exclusion_reason TEXT       NULL
```
The old `difficulty INTEGER` column is retained (deprecated) until all callers migrate.

**`DifficultyConstants.java`** (`engine/DifficultyConstants.java`) — all tunable parameters in one place: zone boundaries, saturation thresholds, formula weights, depth bonus max, checkout floor, viability thresholds.

**`DifficultyCalculator.java`** (`engine/DifficultyCalculator.java`) — pure static utility; no Spring dependency; testable with plain JUnit. Implements the formula from `docs/design/DIFFICULTY_SCORING.md`.

**`DifficultyRecalibrationService.java`** — re-derives `difficulty_score` and `single_question_viable` from stored counts for all non-locked questions. Called by the recalibration endpoint. Changing a constant in `DifficultyConstants` + calling this endpoint is the complete cost of a formula adjustment — no re-materialisation needed.

**Recalibration endpoint**: `POST /api/admin/questions/recalibrate-difficulty`

**`QuestionMaterializerService` updated** — auto-exclusion at materialisation time: when a question is promoted from `draft → active`, the materializer computes zone counts, evaluates viability, and sets `status = 'excluded'` + populates `viability_exclusion_reason` if either condition fails. No separate cleanup job needed.

**See**: `docs/design/DIFFICULTY_SCORING.md` for the full formula and zone boundary documentation.

---

### Audit Phase 5 — MapStruct, GlobalExceptionHandler, EntityType Constants, Delete SvelteKit (commit `93b3fe2`)

**Date**: 2026-05-27

**Findings addressed**:
- Manual `.builder()...build()` DTO mapping boilerplate across all controllers
- Local `@ExceptionHandler` definitions duplicated in `PracticeGameController` and `AdminAnswerController` (and growing)
- `"footballer"` / `"football"` string literals hardcoded in 7+ locations
- Redundant SvelteKit frontend (`frontend/`) coexisting with the active React frontend

**What was built**:

**MapStruct mappers** — `pom.xml` updated with `mapstruct` + `lombok-mapstruct-binding` dependencies:
- `mapper/AnswerMapper.java` — `Answer → AnswerResponse`
- `mapper/CategoryMapper.java` — `Category → CategoryResponse`

Manual `.builder()` chains in `AdminAnswerController` and `AdminCategoryController` replaced with injected mapper calls. Compile-time generation; zero runtime overhead.

**`GlobalExceptionHandler`** (`exception/GlobalExceptionHandler.java`) — `@RestControllerAdvice` covering:
- `IllegalArgumentException` → 400 `{ "error": "..." }`
- `MethodArgumentNotValidException` → 400 `{ "error": "Validation failed", "fieldErrors": { "field": "reason" } }`
- `EntityNotFoundException` → 404 `{ "error": "..." }`
- `RuntimeException` catch-all → 500 (logged as ERROR, generic message to client)

Local `@ExceptionHandler` methods removed from `PracticeGameController` and `AdminAnswerController`.

**`EntityType` constants class** (`model/EntityType.java`):
```java
public final class EntityType {
    public static final String FOOTBALLER = "footballer";
    public static final String CITY       = "city";
    public static final String COUNTRY    = "country";
    public static final String COACH      = "coach";
}
```
All 7 hardcoded `"footballer"` literals across `EntityController`, `EntitySearchService`, `AdminAnswerService`, `QuestionGeneratorService`, `PracticeGameController` replaced with `EntityType.FOOTBALLER`. `EntitySearchService.backfillFromPlayers()` now accepts the type as a parameter (uses `EntityType.FOOTBALLER` as the call site default).

**SvelteKit frontend deleted** — `frontend/` directory removed entirely. The active frontend is `frontend-react/` only.