# Trivia 501 - Comprehensive Project Audit

**Date:** 2026-05-26  
**Revised:** 2026-05-26 (cross-referenced against codebase)  
**Status:** Completed  
**Scope:** Backend (Java/Spring Boot), Frontend (React/Next.js), Design Docs

---

## 🛡️ Security Audit

The project currently has a **critical lack of security controls**. The following risks must be addressed before any public exposure.

### 1. Public Admin Endpoints (CRITICAL)
- **Finding:** All `/api/admin/**` endpoints are fully accessible without authentication. No `SecurityConfig.java` exists. No `@PreAuthorize` annotation appears anywhere in production code. `AdminEntityController.java:22` contains a developer comment noting the placeholder: `"(e.g. @PreAuthorize("hasRole('ADMIN')"))"`  — confirming this was always deferred, not accidentally omitted.
- **Impact:** Attackers can delete the entire database, modify question templates, and trigger expensive backfill operations.
- **Action:** Implement Spring Security plumbing. Use a permissive dev-profile locally to keep debugging easy, but ensure `@PreAuthorize("hasRole('ADMIN')")` annotations are in place for future activation.

### 2. Identity Spoofing in Game Sessions (CRITICAL)
- **Finding:** `PracticeGameController.java:105` — `playerId` is accepted as a `@RequestParam UUID`. Any caller who knows or guesses a UUID can manipulate any active game session.
- **Impact:** Users can spoof another player's identity, submit moves on their behalf, or read their game state.
- **Action:** Transition from `@RequestParam UUID playerId` to deriving identity from the `SecurityContext`. Use a mock filter for local development that injects a default identity, preventing spoofing while keeping the MVP flow simple.

### 3. Missing Infrastructure Protections
- **Finding:** No CSRF protection or Content Security Policy (CSP) is active. No `SecurityConfig` exists at all — Spring Security's auto-configuration is entirely absent.
- **Impact:** Vulnerability to Cross-Site Request Forgery and potential XSS if user-generated content (like player names) is not properly sanitised.
- **Action:** Enable standard Spring Security CSRF protection (with a dev-mode toggle) and add a strict CSP in the Next.js `middleware.ts`.

---

## 🏗️ Architectural Audit

### 1. Largest Frontend File Is Not Being Tracked
- **Finding:** `frontend-react/src/app/admin/questions/[id]/page.tsx` is **622 lines** — the largest single file in the frontend. It mixes data fetching, form state, bulk-import orchestration, and answer table management in one component.
- **Smell:** Violates Single Responsibility. Harder to test, harder to extend for new admin workflows.
- **Action:** Extract into at minimum: a `useQuestionDetail` hook (data fetching + mutations), an `AnswerTable` component, and a `QuestionMetaPanel` component. `MatchView.tsx` (223 lines) is also a candidate for splitting once the larger file is addressed.

> **Note:** An earlier version of this audit incorrectly cited `GamePage.tsx` (a file that does not exist) as the God Component. The real offender is `questions/[id]/page.tsx`.

### 2. Incomplete "Game Engine" Abstraction
- **Finding:** An `engine/` package exists and contains `AnswerEvaluator`, `ScoringService`, and `DartsValidator` — there is meaningful separation from the controller layer. However, **there is no state-machine coordinator** that owns transitions between game states (e.g. Move → Validate → UpdateScore → CheckWin → TurnEnd). That orchestration logic currently leaks into `GameService`.
- **Problem:** Adding ranked or multiplayer modes will duplicate transition logic across controllers and services.
- **Action:** Introduce a `GameStateMachine` or `GameEngine` coordinator in the `engine/` package that accepts a `GameEvent` and produces a `GameStateTransition`. Controllers and WebSocket handlers become thin dispatchers only.

### 3. N+1 Read + Write Problem in Backfills (Worse Than a Simple N+1)
- **Finding:** `EntitySearchService.backfillFromPlayers()` executes **two database round-trips per entity**:
  1. `namedEntityRepository.findByEntityTypeAndNormalizedName(...)` — existence check
  2. `namedEntityRepository.save(entity)` — insert if not found

  For 17k players this is potentially **34k+ sequential DB queries**. There is also a race condition: two concurrent backfill invocations can pass the existence check simultaneously and attempt duplicate inserts.
- **Action:** Replace the check-then-insert loop with a single native PostgreSQL upsert:
  ```sql
  INSERT INTO named_entities (entity_type, display_name, normalized_name, hint, created_at)
  SELECT 'footballer', p.name, lower(unaccent(p.name)), p.nationality, now()
  FROM players p
  ON CONFLICT (entity_type, normalized_name) DO NOTHING;
  ```
  This eliminates the N+1, removes the race condition, and is restartable. `JdbcTemplate.batchUpdate` is an improvement but does not fix the race condition.

### 4. State Management: Custom Hook, Not Zustand
- **Finding:** Game state in `MatchView.tsx` and its parent page mixes local `useState`, prop drilling, and inline API calls.
- **Action:** Extract into a `useGameLoop` custom hook that owns game state, submits answers, and manages the WebSocket lifecycle. **Do not introduce Zustand** — `CLAUDE.md` explicitly states the architecture uses `useState + React Context; no Redux/Zustand`. A custom hook is sufficient and consistent with the existing pattern.

### 5. Boilerplate Data Mapping
- **Finding:** Manual `.builder()...build()` mapping is used everywhere between entities and DTOs.
- **Action:** Implement **MapStruct** to automate DTO-to-Entity conversions and reduce the surface for mapping bugs.

---

## 🧹 Code Smells & Maintenance

### 1. `createdAt = LocalDateTime.now()` Across 12+ Model Classes
- **Severity:** Medium  
- **Finding:** Manual `LocalDateTime.now()` is set in `@PrePersist` (or equivalent) in at least: `Answer`, `Team`, `GameMove`, `Competition`, `Question`, `Game`, `Match`, `QuestionTemplate`, `PlayerSeasonStint`, `TeamExternalId`, `PlayerExternalId`, `Category`. This is not a few places — it is every model in the project.
- **Risk:** Clock skew in tests; no `updatedAt` tracking enforced by the framework; easy to forget on new models.
- **Action:** Enable Spring Data Auditing (`@EnableJpaAuditing`). Annotate fields with `@CreatedDate` / `@LastModifiedDate`. Remove all manual `LocalDateTime.now()` assignments.

### 2. Hardcoded `"footballer"` / `"football"` in 7+ Locations
- **Finding:** The string literals `"footballer"` and `"football"` are hardcoded in:
  - `EntityController.java` — `DEFAULT_ENTITY_TYPE`
  - `EntitySearchService.java` — 3 occurrences including `backfillFromPlayers()` lines 161 and 166
  - `AdminAnswerService.java` — `DEFAULT_ENTITY_TYPE`
  - `QuestionGeneratorService.java` — `config.put("entity_type", "footballer")`
  - `PracticeGameController.java` — `DEFAULT_CATEGORY_SLUG` and inline default

  The `backfillFromPlayers()` method hardcodes `"footballer"` internally, making it impossible to reuse for a future `"coach"` or `"city"` backfill without copy-pasting.
- **Action:** Define an `EntityType` enum or constants class. Move defaults to `@ConfigurationProperties`. Make `backfillFromPlayers()` accept an `entityType` parameter.

### 3. Local `@ExceptionHandler` Duplication Spreading
- **Finding:** `PracticeGameController` and `AdminAnswerController` each define their own `@ExceptionHandler(IllegalArgumentException.class)`. Two files already; this will grow.
- **Action:** Implement a single global `@RestControllerAdvice` (`GlobalExceptionHandler`) to standardise error JSON structures. Remove all local `@ExceptionHandler` definitions.

### 4. Normalization Divergence Is a Present Risk, Not a Future One
- **Finding:** `EntitySearchService.stripAccents()` explicitly documents itself as mirroring PostgreSQL's `unaccent()`. The divergence can happen today: a library upgrade, a locale change, or a difference in PostgreSQL `unaccent` dictionary version could silently break autocomplete with no test catching it.
- **Action:** Add an integration test (TestContainers + real PostgreSQL) that:
  1. Inserts an entity with an accented name (e.g. `"Agüero"`)
  2. Calls `EntitySearchService.searchEntities("footballer", "aguero", 10)` in Java
  3. Asserts the result contains the inserted entity
  This pins the contract between Java normalisation and SQL `unaccent`.

### 5. Redundant Frontends
- **Finding:** Both `frontend/` (SvelteKit) and `frontend-react/` (Next.js) exist in the repository.
- **Action:** Delete `frontend/`. The React frontend is the active one. Carrying two frontends doubles the surface for dependency vulnerabilities and confuses onboarding.

---

## 📊 Design Audit: Difficulty & Scoring

Based on the review of `docs/design/DIFFICULTY_SCORING.md`:

### 1. The "Broken Template" Risk
- **Finding:** The `team_competition_sub_appearances_since` template is structurally incapable of producing high-value answers, leading to "stalling" games.
- **Action:** Implement the **Auto-Exclusion** logic proposed in the design doc. Questions must be automatically flagged as `excluded` if they fail hard viability checks (`pool < 501` or `count < 15`).

### 2. Normalization Divergence (see Code Smells §4 above)
- **Finding:** Accents are stripped in both Java (`Normalizer`) and SQL (`unaccent`). These must stay in sync.
- **Action:** Single integration test to pin the contract — see Code Smells §4.

### 3. Difficulty vs. Viability
- **Finding:** Current logic treats difficulty as an integer (1/2/3).
- **Insight:** The new design's continuous score (0.0–10.0) is superior but requires a strict **Viability Gate** before the score is meaningful.
- **Action:** Prioritise the implementation of `V13__question_difficulty_metrics.sql` and the associated `DifficultyCalculator`. The integer `difficulty` column is deprecated but must remain until all callers migrate.

---

## 🚀 Revised Roadmap (Re-prioritised)

The original roadmap placed the React refactor (Phase 2) before the N+1 backfill fix (Phase 4). The backfill issue is a production correctness risk (connection timeouts, data corruption under concurrent runs) and should be addressed first.

1. **Phase 1: Security Architecture (Permissive).** Wire up Spring Security plumbing. Move `playerId` logic to the `Principal` context. Use a "Dev-Mode" permissive bypass to keep local debugging frictionless while future-proofing for OAuth.

2. **Phase 2: Backfill & Data Integrity.**  
   - Replace `backfillFromPlayers()` loop with a native `INSERT ... ON CONFLICT DO NOTHING` query.  
   - Add the normalization divergence integration test.  
   - Enable Spring Data Auditing; remove all manual `LocalDateTime.now()`.

3. **Phase 3: Game State Refactor.**  
   - Extract `useGameLoop` custom hook in React (not Zustand).  
   - Extract `GameStateMachine` coordinator in the `engine/` package.  
   - Break up `questions/[id]/page.tsx` (622 lines) into focused components and a data-fetching hook.

4. **Phase 4: Robust Scoring.** Implement the V13 Difficulty & Viability design.

5. **Phase 5: Optimisation & Cleanup.**  
   - Introduce MapStruct for DTO mapping.  
   - Consolidate all `@ExceptionHandler` into a global `@RestControllerAdvice`.  
   - Replace hardcoded `"footballer"` / `"football"` strings with an `EntityType` enum.  
   - Delete the `frontend/` SvelteKit directory.
