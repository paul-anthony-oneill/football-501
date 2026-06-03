# Current Implementation — Explained Simply

**Last Updated**: 2026-05-27  
**Status**: Audit Fixes Complete (Phase 5 of 5)

## What We've Built So Far

Trivia 501 is like a game of darts, but instead of throwing darts, you name football players. This document explains how everything works right now in simple terms.

---

## The Big Picture: How a Game Works

Imagine you're playing the game. Here's what happens step by step:

```
1. You click "Start Game" on the website
   ↓
2. The website asks the server: "Can I start a game?"
   ↓
3. The server creates a new game and gives you a question
   ↓
4. You type a player's name (like "Haaland")
   ↓
5. The website sends it to the server
   ↓
6. The server checks if it's correct and calculates your score
   ↓
7. You get feedback and your new score
   ↓
8. Repeat until you win (reach 0) or bust!
```

---

## The Parts of the System

### Frontend (Next.js / React) — What You See

**Location:** `frontend-react/src/`

This is the web application users interact with. It was migrated from SvelteKit to Next.js 16 + React 19 + Tailwind CSS 4 in May 2026. The SvelteKit `frontend/` directory has been deleted.

#### **Game Interface**

**Orchestrator:** `frontend-react/src/app/page.tsx` (97 lines — lobby state only)

The page component stays small by delegating all game logic to the `useGameLoop` hook. It only owns the lobby-vs-game display toggle.

**`useGameLoop` hook:** `frontend-react/src/hooks/useGameLoop.ts`

This custom hook owns the entire game session lifecycle:
- Tracks: score, question text, turn count, move history, entity type, hints
- Actions: `startNewGame(categorySlug)`, `submitAnswer(answer)`, `exitGame()`
- Makes all API calls to `/api/practice/start` and `/api/practice/games/{id}/submit`
- Will own WebSocket lifecycle when multiplayer is added

**`MatchView.tsx`:** `frontend-react/src/components/game/match/MatchView.tsx`

Renders the active game UI (score display, question, move history). Receives state from `useGameLoop` via props — no direct API calls.

**`EntitySearch.tsx`:** `frontend-react/src/components/game/EntitySearch.tsx`

The autocomplete input component. Fires after 4 characters; queries `GET /api/entities/search` (not `/api/answers`). Accepts an `entityType` prop read from `question.config.entity_type`.

#### **Admin Interface**

**Location:** `frontend-react/src/app/admin/`

- **Questions detail page:** `admin/questions/[id]/page.tsx` — 113 lines (reduced from 622). Now a thin coordinator that delegates to the `useQuestionDetail` hook and three focused components.
- **`useQuestionDetail` hook:** `frontend-react/src/hooks/useQuestionDetail.ts` — owns all data fetching, form state, and mutations for the question detail workflow
- **`AnswerTableSection`:** `frontend-react/src/components/admin/AnswerTableSection.tsx` — renders the answers table
- **`QuestionMetaPanel`:** `frontend-react/src/components/admin/QuestionMetaPanel.tsx` — renders question metadata form fields
- **`AnswerPreview`:** `frontend-react/src/components/admin/AnswerPreview.tsx` — renders individual answer rows
- **Categories:** `admin/categories/` — CRUD for game categories

---

### Backend (Spring Boot) — The Game Master

The backend is like the game referee. It knows all the rules and makes sure no one cheats.

---

#### Security Layer — The Bouncer

**Location:** `backend/src/main/java/com/trivia501/security/`

**`SecurityConfig.java`**  
Wires Spring Security for the whole application. Key behaviours:
- `@EnableMethodSecurity` activates `@PreAuthorize` annotations on all admin controllers
- CSRF disabled (stateless REST API)
- Session policy STATELESS
- URL rules enforce per-endpoint access (see below)
- Injects `DevModeAuthFilter` before `UsernamePasswordAuthenticationFilter` when running outside prod

**`DevModeAuthFilter.java`** (`@Profile("!prod")`)  
Injects a fixed authenticated principal (`DEV_PLAYER_ID = "00000000-0000-0000-0000-000000000001"`) with `ROLE_USER + ROLE_ADMIN` into every request. This filter is the reason all endpoints are reachable locally without real OAuth tokens. It is idempotent — if a request already has authentication, it does nothing.

**URL-level access policy:**
| Path | Access |
|------|--------|
| `/api/entities/**`, `/api/categories/**`, `/actuator/health` | Public |
| `/api/practice/**` | `ROLE_USER` or `ROLE_ADMIN` |
| `/api/admin/**` | `ROLE_ADMIN` (also `@PreAuthorize` at method level) |
| Everything else | Authenticated |

---

#### Controller Layer — The Reception Desk

**Location:** `backend/src/main/java/com/trivia501/controller/`

**`PracticeGameController.java`**  
Handles the single-player game flow. Player identity is derived from `Principal.getName()` (injected by Spring Security) — never from a request parameter. This prevents identity spoofing.

**Admin controllers** (`AdminAnswerController`, `AdminCategoryController`, `AdminEntityController`, `AdminQuestionController`, `AdminTemplateController`)  
All carry `@PreAuthorize("hasRole('ADMIN')")` at class level.

**Error handling**: All controllers use the `GlobalExceptionHandler` for error responses — no local `@ExceptionHandler` methods.

---

#### Exception Layer — Standardised Error Responses

**Location:** `backend/src/main/java/com/trivia501/exception/GlobalExceptionHandler.java`

A `@RestControllerAdvice` that handles all exceptions for every REST controller in one place. Ensures consistent JSON error format:

```json
{ "error": "human-readable message" }
```

Bean-validation failures return an additional `fieldErrors` map:

```json
{
  "error": "Validation failed",
  "fieldErrors": { "score": "must be greater than 0" }
}
```

Handled exception types:
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400 with field errors
- `IllegalStateException` → 409
- `DuplicateEntityException` → 409
- `CategoryHasQuestionsException` → 409

---

#### Service Layer — The Game Logic

**`GameService.java`**  
Now a thin database-lifecycle owner. It loads game + match state, calls `GameStateMachine.onMoveSubmitted()`, and persists the returned `GameTransition`. All transition rule logic has been moved out.

**`EntitySearchService.java`**  
Handles autocomplete search and entity registration. The `backfillFromPlayers()` method now calls a single native `INSERT … ON CONFLICT DO NOTHING` bulk upsert instead of the former O(n) check-then-insert loop.

**`DifficultyRecalibrationService.java`**  
Bulk recalculates `difficulty_score` and re-evaluates viability for all unlocked questions using stored zone counts. Does not touch the `answers` table — formula tuning costs a single endpoint call.

**`QuestionMaterializerService.java`**  
Now accumulates zone counts during answer materialisation, auto-excludes questions failing viability checks, and calls `DifficultyCalculator` to compute and store `difficulty_score`. Questions failing either viability condition (`total_score_pool < 501` or `total_valid_count < 15`) are automatically set to `status = 'excluded'`.

---

#### Game Engine Layer — The Smart Rules

**Location:** `backend/src/main/java/com/trivia501/engine/`

**`GameStateMachine.java`**  
The state-machine coordinator introduced in Phase 3. Owns all turn-transition rules:
- `INVALID` — same player retries; timer keeps running
- `BUST` — no score change; turn switches
- `VALID` — score deducted; timeout counter resets; turn switches
- `CHECKOUT` — applies the close-finish rule in multiplayer (P2 gets one final turn if P1 checks out first)
- `TIMEOUT` — increments consecutive counter; reduces timer (45s → 30s → 15s → forfeit at 3)

Has no repository dependencies — it is a pure function: takes `Game + Match + AnswerResult`, returns `GameTransition`.

**`DifficultyCalculator.java`**  
Pure static utility. Implements the three-zone ease formula with checkout floor and depth bonus. Clamped 0–10. No Spring dependencies — trivially unit-testable.

**`DifficultyConstants.java`**  
Final constants class (no Spring annotations). All zone boundaries, saturation thresholds, formula weights, and viability minimums live here. Never hardcode these values elsewhere.

**`AnswerEvaluator.java`**  
Checks if a player's name matches an answer for the current question using exact and fuzzy matching.

**`ScoringService.java`**  
Enforces the 501 subtraction rules, including the checkout range (−10 to 0).

**`DartsValidator.java`**  
Knows which scores are impossible in real darts (e.g., 179 is a bust).

---

#### Mapper Layer — DTO Conversion

**Location:** `backend/src/main/java/com/trivia501/mapper/`

MapStruct mappers introduced in Phase 5 to replace manual `.builder()...build()` DTO mapping:

- **`AnswerMapper`** — pure 1:1 interface: `Answer → AnswerResponse`
- **`CategoryMapper`** — abstract class with `@AfterMapping` for question count DB lookup: `Category → CategoryResponse`

MapStruct generates implementations at compile time. Inject via `@Autowired` or Lombok `@RequiredArgsConstructor`.

---

#### JPA Configuration

**`JpaConfig.java`** (`@EnableJpaAuditing`)  
Activates Spring Data JPA auditing. All model classes carry `@EntityListeners(AuditingEntityListener.class)` so `@CreatedDate` and `@LastModifiedDate` fields populate automatically. Manual `LocalDateTime.now()` in `@PrePersist` is the old pattern — do not use it.

**Important for tests**: `@WebMvcTest` slices do not load a real JPA context. Add `@MockitoBean JpaMetamodelMappingContext` to any `@WebMvcTest` class to satisfy the `@EnableJpaAuditing` dependency.

---

#### Data Model — Key Constants

**`EntityType.java`** (`com.trivia501.model`)  
String constants for the `entity_type` column. Always use these instead of bare literals:
- `EntityType.FOOTBALLER` = `"footballer"`
- `EntityType.CITY` = `"city"`
- `EntityType.COUNTRY` = `"country"`
- `EntityType.COACH` = `"coach"`

**`CategorySlug.java`** (`com.trivia501.model`)  
- `CategorySlug.FOOTBALL` = `"football"` — the default game category slug

---

### Data Pipeline (Python) — The Librarian

**Location:** `trivia-501-scraper/`

This part of the system fetches real-world data to make the game possible.

**The Scraper:** `trivia-501-scraper/scrape_current_season.py`  
Uses `ScraperFC` to pull live data from FBref.com. Updates stats in the `career_stats` JSONB array without touching historical data.

**Question & Answer Generator:**
- `init_questions_v2.py` — creates questions for teams
- `populate_answers_v2.py` — calculates scores for every player, pre-computes `is_valid_darts` and `is_bust` status

---

## Database Schema

```
┌─────────────────┐       ┌─────────────────────────┐
│    teams        │       │    players              │
│  - popularity   │       │  - name                 │
│    (1-10)       │       │  - career_stats (JSONB) │
└────────┬────────┘       └─────────────┬───────────┘
         │                              │
         │ Automated Generation         │
         ▼                              ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│   questions             │       │        answers          │
│  - difficulty_score     │ ◄─────┤  - question_id (FK)     │
│    (0.00–10.00)         │       │  - score                │
│  - single_question_     │       │  - is_valid_darts       │
│    viable               │       │  - is_bust              │
│  - high_value_count     │       └─────────────────────────┘
│  - mid_range_count      │
│  - checkout_count       │
│  - total_valid_count    │
│  - total_score_pool     │
│  - viability_exclusion_ │
│    reason               │
└────────┬─────┬──────────┘
         │     │
         │     │ 1:N
         ▼     ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│      matches            │ ◄──── │         games           │
│  - player1_id           │       │  - match_id (FK)        │
│  - game_mode            │ 1:N   │  - player1_score        │
│  - status               │ ─────►│  - status               │
└─────────────────────────┘       └─────────────────────────┘
```

**V13 migration adds 9 columns to `questions`**: `high_value_count`, `mid_range_count`, `checkout_count`, `total_valid_count`, `total_score_pool`, `single_question_viable`, `viability_exclusion_reason`, `difficulty_score`, `difficulty_locked`. See `docs/design/DIFFICULTY_SCORING.md` for full details.

---

## Key Feature: Difficulty Scoring System

The old integer `difficulty` field (1/2/3 scale) is deprecated. Questions now carry a continuous `difficulty_score` (0.00–10.00) computed by `DifficultyCalculator` from four zone counts.

**How difficulty is computed at materialisation time:**
1. `QuestionMaterializerService` counts answers in three zones during the answer upsert loop
2. `DifficultyCalculator.calculate(highValue, midRange, checkout, totalValid)` applies the formula
3. Both viability conditions (`total_score_pool >= 501` AND `total_valid_count >= 15`) are checked
4. Non-viable questions are automatically set to `status = 'excluded'`
5. All values are stored on the `questions` row

**Recalibration** (when formula constants change):
1. Adjust a value in `DifficultyConstants.java`
2. Call `POST /api/admin/questions/recalculate-difficulty`
3. Review results in the admin question list

---

## Audit Phases — What Was Changed

### Phase 1 — Spring Security (commit 738c13d)
- Added `spring-boot-starter-security` dependency
- New `SecurityConfig.java` with `@EnableMethodSecurity`, STATELESS session, URL access rules
- New `DevModeAuthFilter.java` (`@Profile("!prod")`) — injects fixed principal without real OAuth
- `PracticeGameController` — removed `@RequestParam UUID playerId`; now reads from `Principal`
- All five admin controllers — added `@PreAuthorize("hasRole('ADMIN')")`
- `CorsConfig` — switched from `CorsFilter` bean to `CorsConfigurationSource` bean

### Phase 2 — Backfill Upsert + JPA Auditing (commit 972dc5f)
- `NamedEntityRepository.bulkUpsertFootballersFromPlayers()` — single native `INSERT … ON CONFLICT DO NOTHING`; eliminated 34k+ sequential queries and the race condition
- `JpaConfig.java` — `@EnableJpaAuditing`
- 12+ model classes — added `@EntityListeners`, `@CreatedDate`, `@LastModifiedDate`; removed manual `LocalDateTime.now()`
- `EntitySearchNormalizationContainerTest` — pins Java `stripAccents()` vs PostgreSQL `unaccent()` contract against a real PostgreSQL 17 container

### Phase 3 — Game State Refactor (commit 4ad75ce)
- `GameStateMachine.java` + `GameTransition.java` — new engine package classes; `GameService` now delegates to them
- `useGameLoop.ts` — extracted from `page.tsx`; owns all game state and API calls
- `useQuestionDetail.ts` — extracted from the 622-line `questions/[id]/page.tsx`
- `AnswerPreview`, `QuestionMetaPanel`, `AnswerTableSection` — focused admin components
- `questions/[id]/page.tsx` reduced from 622 → 113 lines

### Phase 4 — Difficulty & Viability (commit 932d9d9)
- `V13__question_difficulty_metrics.sql` — 9 new columns, indexes, `chk_viability_reason` constraint, `'excluded'` status
- `DifficultyConstants.java`, `DifficultyCalculator.java` — pure engine classes
- `DifficultyRecalibrationService.java` — bulk recalibration from stored counts
- `QuestionMaterializerService` updated — zone accumulation, auto-exclusion
- `AdminQuestionController` — `POST /recalculate-difficulty`, `PATCH /{id}/difficulty-lock`
- `QuestionResponse` DTO — all 9 new fields exposed
- `backfill_difficulty_scores.sql` — manual backfill for existing questions

### Phase 5 — Cleanup (commit 93b3fe2)
- MapStruct: `AnswerMapper`, `CategoryMapper` — replace manual `.builder()...build()` mapping
- `GlobalExceptionHandler.java` — `@RestControllerAdvice` in `com.trivia501.exception`; removed local `@ExceptionHandler` from `AdminAnswerController` and `PracticeGameController`
- `EntityType.java`, `CategorySlug.java` — constants classes replacing hardcoded strings
- `frontend/` (SvelteKit directory) — deleted entirely

---

## What's Currently Working

- Single-player practice mode — full 501 logic, checkout range (−10 to 0), fuzzy name matching
- Game engine — `GameStateMachine` coordinator with all turn-transition rules
- Admin interface — category and question management, difficulty lock/unlock
- Difficulty scoring — continuous 0–10 score computed at materialisation, viability gate enforced
- Entity autocomplete — accent-insensitive search via PostgreSQL trigram index
- Security plumbing — Spring Security wired; dev-mode bypass; `@PreAuthorize` on admin endpoints

---

## What's NOT Yet Implemented

- Real authentication (OAuth 2.0 / JWT) — `DevModeAuthFilter` is a placeholder
- Multiplayer via WebSocket
- Player profiles and persistent ranking
- Global leaderboards
- Data population from the Python scraper (development only so far)

---

## Key Files to Know

### Backend
- `SecurityConfig.java` — URL access rules, Spring Security configuration
- `DevModeAuthFilter.java` — dev-mode identity injection
- `GlobalExceptionHandler.java` — all error responses
- `GameStateMachine.java` — turn-transition rules
- `DifficultyCalculator.java` + `DifficultyConstants.java` — difficulty formula
- `DifficultyRecalibrationService.java` — recalibration endpoint logic
- `EntityType.java` + `CategorySlug.java` — entity type and category slug constants
- `JpaConfig.java` — JPA auditing activation

### Frontend
- `frontend-react/src/hooks/useGameLoop.ts` — game session state and API calls
- `frontend-react/src/hooks/useQuestionDetail.ts` — admin question detail state
- `frontend-react/src/app/page.tsx` — lobby orchestrator (97 lines)
- `frontend-react/src/app/admin/questions/[id]/page.tsx` — question detail coordinator (113 lines)

### Migrations
- `V13__question_difficulty_metrics.sql` — difficulty and viability columns
- `backfill_difficulty_scores.sql` — manual backfill SQL for existing data (run once after V13)
