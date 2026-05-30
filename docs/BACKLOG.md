# Football 501 — Backlog & Future Work

**Last updated**: 2026-05-29  
**Purpose**: Living document capturing all deferred work, stretch goals, and improvement ideas regardless of size or urgency. Update this whenever a decision is made to defer something, or when a backlog item is completed or abandoned.

---

## How to Read This Document

| Priority | Meaning |
|---|---|
| **P0 — Launch Blocker** | Must ship before any real users can play |
| **P1 — Shortly After Launch** | Important but doesn't block the core game loop |
| **P2 — Stretch Goal** | Designed and documented; not being built until core is stable |
| **Guardrail** | Small cheap schema/code decision to make now so a future item stays open |
| **Parked** | Explicitly deferred; revisit when the reason for deferral no longer applies |

---

## P0 — Launch Blockers

These items must be complete before real players can use the game.

### Auth: Guest accounts
- **What**: Ephemeral UUID for unauthenticated players; 24-hour inactivity timeout.
- **Why deferred**: Depends on real auth being wired first.
- **See**: `docs/SECURITY_ARCHITECTURE.md` — What Is Deferred table.

### WebSocket: STOMP multiplayer handler
- **What**: Real-time 1v1 match communication over WebSocket (STOMP protocol). `GameStateMachine` is already designed to receive WebSocket events.
- **Why deferred**: Core game engine is complete; WebSocket layer not yet wired.
- **See**: `docs/design/TECHNICAL_DESIGN.md`, `CLAUDE.md` — WebSocket Protocol section.

### Player profiles and matchmaking queue
- **What**: `player_profiles` table (MMR, league points, win/loss), matchmaking queue, rank display.
- **Why deferred**: Requires auth and multiplayer to be in place first.

---

## P1 — Shortly After Launch

These don't block the first players but should follow quickly.

### Security: Re-enable CSRF protection
- **What**: CSRF is currently disabled for stateless REST. Re-enable with SameSite cookies when JWT cookies are introduced.
- **Why deferred**: Not applicable until HTTPOnly cookie auth is in place.
- **See**: `docs/SECURITY_ARCHITECTURE.md`.

### Security: Content Security Policy
- **What**: Add a strict CSP in Next.js `middleware.ts`.
- **Why deferred**: Low risk during development; important before public launch.
- **See**: `docs/SECURITY_ARCHITECTURE.md`.

### Data: Remove legacy `difficulty INTEGER` field
- **What**: Drop the old 1/2/3 difficulty column once all callers of `findByCategoryIdAndDifficultyAndStatus` are migrated to the new range-based query (`difficulty_score NUMERIC`).
- **Why deferred**: Needs a dedicated cleanup migration; low risk to leave in place short-term.
- **See**: `docs/design/DIFFICULTY_SCORING.md` — Remaining / Future Work.

---

## UI Bugs

Small frontend issues to fix, tracked here as they come up. `EntitySearch.tsx` is the autocomplete component used during gameplay.

### Autocomplete: stale suggestions appear after submit
- **What**: When you type a name and press Enter, the answer submits, the score updates, and the input clears. But the 300ms-debounced autocomplete fetch may complete *after* submission, popping up suggestions for the previous query in an empty input box.
- **Where**: `EntitySearch.tsx:88-92` — the debounced `fetchSuggestions` timer is not cancelled on submit.
- **Fix direction**: Cancel the debounce timer in the Enter/clear path; or set a flag to suppress results that arrive after a submit.

### Autocomplete: focus lost after clicking a suggestion
- **What**: Clicking a suggestion fills the input but focus stays on the clicked button. You have to click back into the input before pressing Enter to submit.
- **Where**: `EntitySearch.tsx:122-127` (`selectEntity`) called from `onMouseDown` at line 160.
- **Fix direction**: Call `inputRef.current?.focus()` at the end of `selectEntity`.

### Autocomplete: arrow-key selection should auto-submit
- **What**: Arrow keys navigate the dropdown (lines 111-116), but pressing Enter on a highlighted suggestion only fills the input — it doesn't submit. You have to press Enter a second time. Similarly, clicking a suggestion fills but doesn't submit. Selection and submission should be a single action.
- **Where**: Both the keyboard Enter path (line 104-105) and the mouse path (line 160) call `selectEntity` which only fills the input.
- **Fix direction**: Have both paths call `onSelect(entity.name)` directly instead of (or after) filling the input, so selecting = submitting.

---

## Backend Hardening

Stability and correctness improvements that are not urgently needed but should be done before the game sees meaningful load.

### Duplicate answer protection in `game_moves`
- **What**: Two guards against a player submitting the same answer twice (e.g. via double-tap or a network retry racing with the first request):
  1. **Unique constraint** on `(game_id, matched_answer_id)` in `game_moves` — DB rejects the second insert regardless of application logic.
  2. **`@Version` optimistic locking** on the `Game` entity — a version column that increments on every save; a concurrent second transaction throws `OptimisticLockException`, which `GlobalExceptionHandler` can return as a clean error.
- **Why deferred**: Design is still evolving; don't want an unnecessary Flyway migration that needs reversing. Current `@Transactional` wrapper prevents partial saves, so this is a hardening step, not a correctness emergency.
- **When to pick up**: Once the game schema is stable and multiplayer is being wired. Both changes together close the race condition and the duplicate submission gap.
- **Files**: `GameService.java:77`, `Game.java`, `GameMove.java`, `GlobalExceptionHandler.java`.

### Toggle on/off and customise turn timer
- **What**: Allow players (or a match host) to disable the turn timer entirely, or set a custom duration, before a game starts. The default 45s→30s→15s→forfeit ladder would remain unchanged for standard ranked play.
- **Why deferred**: Core timer logic isn't fully wired yet (server-side enforcement + client display are still P0 work). Customisation only makes sense once the baseline timer is solid.
- **When to pick up**: After the turn timer P0 item is complete. Timer settings would live on the `matches` row (e.g. `timer_enabled BOOLEAN`, `turn_timer_override_seconds INTEGER`) so `GameStateMachine` can read them without changing its interface.
- **Files**: `GameStateMachine.java`, `Game.java`, `GameService.java`.

### Solo forfeit does not complete the match
- **What**: When a solo player forfeits via timeout, `GameStateMachine.onTimeout` sets `winnerId = null` (correct — no winner in solo forfeit). But `handleGameCompletion` is never called from the solo game-over path (it's only wired in tests today). Even if it were called, `isMatchComplete` would return false because neither `player1GamesWon` nor `player2GamesWon` increments — so the match stays `IN_PROGRESS` forever with a `COMPLETED` game inside it.
- **Why deferred**: Solo forfeit is a rare edge case (3 consecutive timeouts needed). The `handleGameCompletion` NPE from null `winnerId` was fixed 2026-05-28, but the broader completion flow for solo forfeits needs a dedicated design pass — should a solo forfeit end the match? Should it count as a "loss"? Today there's no two-player flow either, so this only affects solo.
- **When to pick up**: When wiring `handleGameCompletion` into the real controller flow (currently it's only called from `MatchServiceTest`). At that point, decide: (a) make `handleGameCompletion` mark the match `COMPLETED` when `winnerId` is null, or (b) give solo forfeits a different status like `ABANDONED`.
- **Files**: `MatchService.java:141`, `GameStateMachine.java:138`, `GameService.java:139`.

### Solo vs multiplayer: null-player2 design tension
- **What**: Solo (practice) games reuse the two-player `Match`/`Game` models with `player2Id = null`. This works but creates scattered `isSolo` branching in `GameStateMachine` (4+ sites) and `GameService` (2 sites). Fields like `player2Score`, `player2GamesWon`, and `MatchFormat.BEST_OF_1` are meaningless in solo context. The `opponentOf` helper was a latent footgun (now guarded with an explicit exception, 2026-05-28). The question is whether solo should be a separate type entirely or whether the current null-based approach is pragmatic enough.
- **Why deferred**: The modes share ~80% of their logic. Splitting now would duplicate code without a clear benefit. A better intermediate step is extracting the branching into a `TurnPolicy` strategy (SoloPolicy / TwoPlayerPolicy) that lives in one place, keeping the shared models. Revisit when: (a) a third game mode is added, (b) the branching spreads beyond `GameStateMachine` + `GameService`, or (c) solo-specific features (AI opponent, timer customization) start piling up.
- **Files**: `GameStateMachine.java:67,123,253`, `GameService.java:209,284`, `MatchService.java:78,148`.

### Per-turn used-answer query grows with game length
- **What**: `gameMoveRepository.findUsedAnswerIdsByGameId(gameId)` (called on every turn at `GameService.java:87`) fetches all used answer IDs from the start of the game. As a game progresses this set grows. Under load this is the query most likely to degrade first.
- **Why deferred**: Not a problem at current scale. The set is bounded by the number of valid answers for the question (~20–100 rows).
- **When to pick up**: If profiling shows this query becoming a bottleneck, consider caching the set in a `ConcurrentHashMap` keyed by `gameId` for the lifetime of the game (invalidated on game completion).

---

## Architectural Guardrails

Small, cheap decisions to make in the core game now that keep future modes open. These are not stretch goals — they are hygiene. Reference `docs/design/GAME_MODES_STRETCH_GOALS.md` when making schema or service boundary decisions.

### Database

| Guardrail | Why it matters |
|---|---|
| Add `game_mode VARCHAR` to `matches`, default `'STANDARD'` | Every non-standard mode needs this column. Adding later requires a migration everywhere matches are created. |
| Store `question_id` on `game_moves` (not just on `games`) | Rapid Fire needs per-move question tracking. Already noted in `CLAUDE.md`. |
| Add `suitable_for_daily BOOLEAN DEFAULT false` to `questions` | Explicit Daily Challenge pool flag — more reliable than inferring from difficulty alone. |
| Add `suitable_for_game_modes JSONB` to `question_templates` | Prevents unsuitable templates being re-enabled for wrong modes in future. See `DIFFICULTY_SCORING.md`. |

### Backend

| Guardrail | Why it matters |
|---|---|
| Question draw logic in a dedicated service (`QuestionDrawService`) | Rapid Fire swaps "draw once per game" for "draw once per turn". Inline logic in game start code needs extraction. |
| Pass `gameMode` through game engine context from the start | Mode-specific rules can't be added later without threading it back through the call stack. |

### Frontend

| Guardrail | Why it matters |
|---|---|
| Mode label/badge component (even if only `STANDARD` renders today) | Gives the UI a place for mode display in listings, lobbies, and history when new modes ship. |

---

## P2 — Game Modes (Stretch Goals)

Fully designed but not being implemented until the core game is stable and has real users. See `docs/design/GAME_MODES_STRETCH_GOALS.md` for full specs.

| Mode | Priority | Summary |
|---|---|---|
| **Daily Challenge** | Highest | One question/day, global leaderboard, solo, easy/medium only. Likely highest-retention feature. |
| **Rapid Fire H2H** | Medium | Question changes every dart; one answer per question. Needs `question_id` on `game_moves` guardrail. |
| **Draft Mode** | Low | Pick from 3 questions each turn. |
| **Category Lock** | Low | Pre-agreed category for the whole match. |
| **Blind Mode** | Low | Question hidden until your turn. |
| **Tournament/League** | Low | Brackets or league tables over multiple games. |
| **Expert Challenge** | Low | Questions with `difficulty_score > 8.5` excluded from standard play; surfaced here. |

---

## Admin & Data

### Difficulty range presets in admin UI
- **What**: Named bands (Accessible 0–4, Competitive 4–7, Expert 7–10) shown as UI constants at render time. Never stored in the DB.
- **Why deferred**: Needs data populated first to be meaningful.
- **See**: `docs/design/DIFFICULTY_SCORING.md` — Remaining / Future Work.

---

## Post-Launch / Scaling

Items that only become relevant once the game has real traffic.

### Read replicas
- **What**: Add PostgreSQL read replicas for scaling query load.
- **Why deferred**: Not needed until traffic justifies it.
- **See**: `docs/design/TECHNICAL_DESIGN.md`.

---

## Completed (moved here when done)

| Item | Completed | Notes |
|---|---|---|
| Spring Security plumbing + `@PreAuthorize` on admin controllers | Phase 1 (738c13d) | `DevModeAuthFilter` injects dev identity |
| Backfill upsert race-condition fix, JPA auditing, normalisation contract | Phase 2 (972dc5f) | Single `INSERT … ON CONFLICT` |
| `GameStateMachine` coordinator, `useGameLoop` hook | Phase 3 (4ad75ce) | All turn-transition rules in one place |
| `DifficultyCalculator`, viability gate, V13 migration | Phase 4 (932d9d9) | Continuous `difficulty_score NUMERIC(4,2)` |
| MapStruct mappers, `GlobalExceptionHandler`, `EntityType` constants, delete SvelteKit | Phase 5 (93b3fe2) | Audit campaign complete |
| Fix N+1 queries in question materializer | 41ef10e | 20x speedup |
| Supabase JWT validation (replaces `DevModeAuthFilter` on prod) | bf2f57a | Spring Security OAuth2 Resource Server + `SUPABASE_JWT_SECRET`; `DevModeAuthFilter` is `@Profile("!prod")` |
| Google OAuth 2.0 social login via Supabase | bf2f57a | `AuthContext.signInWithGoogle()`, `LoginButton`, callback route — end-to-end |
| Data population — seed questions/answers | bf2f57a | V10–V12 Flyway seed migrations, `seed_entities_dev.sql`, `test-data.sql`, 190K-row backup restore |
| HTTPOnly cookie token storage via Supabase SSR | bf2f57a | `@supabase/ssr` manages sessions in HTTPOnly cookies; no localStorage in any auth path |
| Rate limiting (10/100 req/min per IP) | bf2f57a | `RateLimitFilter.java` registered in `SecurityConfig` |
| Game UI reads question from game state | bf2f57a | `useGameLoop` sets question from API response; `MatchView` receives it as a prop — not hardcoded |
