# Trivia 501 — Backlog & Future Work

**Last updated**: 2026-06-08 (single-player pivot — multiplayer deferred indefinitely, ranking/MMR dropped, Free Play reframe, expanded daily starting scores)  
**Purpose**: Living document capturing all deferred work, stretch goals, and improvement ideas regardless of size or urgency. Update this whenever a decision is made to defer something, or when a backlog item is completed or abandoned.

**Product direction** (2026-06-08): The game is now focused on **single-player daily challenges** as the core experience — Wordle-style social trivia where everyone plays the same question with the same parameters each day and shares results with friends. A separate **Free Play** mode (formerly "practice") lets players pick any category/question to play on their own terms. Async friend challenges (play the same question and compare) are a planned social feature. The ranking/MMR/league-tier system and real-time multiplayer are deferred indefinitely.

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

## Recently Completed

| Item | Migration/PR | Notes |
|---|---|---|
| Clean per-season football questions from DB | V28 | Deleted all `football.team_competition_season_metric` questions + answers + dependent rows; deactivated V11 templates. |
| Fix league-level question metadata | V28 | Backfilled `q_scope='league'`, `q_league`, `q_stat` on V12 `player_competition_metric_since` questions so `findRandomFootballLeagueQuestion()` can surface them. Set `q_scope='career'` on career questions. |
| Add league-scope Appearances, Goals+Appearances, Assists+Appearances questions | V30 | Seeded 3 new `player_competition_metric_since` templates; created one question per tier-1 domestic league; materialized answers and difficulty metrics inline. |
| Remove test question from daily challenge pool | V31 | V31 migration flips `suitable_for_daily = false` on test-category questions; `DailyChallengeScheduler` skips test category by slug; `DailyChallengeService.createChallenge()` guards with explicit exception. |
| Reframe practice mode as Free Play | — | Renamed `SoloGameController` → `FreePlayController`, `StartSoloGameRequest` → `StartFreePlayRequest`, `/api/solo` → `/api/freeplay`. All frontend `"solo"` game type → `"freeplay"`. Test files and docs updated.

---

## P0 — Launch Blockers

These items must be complete before real players can use the game.

### Auth: Guest play for core experience
- **What**: Ephemeral UUID for unauthenticated players; 24-hour inactivity timeout. The daily challenge and Free Play modes must work without requiring sign-in. Google OAuth sign-in is reserved for friend challenges, player profiles, and game history.
- **Why deferred**: Google OAuth is wired end-to-end but the unauthenticated path needs the guest identity flow to be seamless — no modal walls, no "sign in to continue" interruptions on the core game loop.
- **See**: `docs/SECURITY_ARCHITECTURE.md` — What Is Deferred table.

### Expand daily challenge starting score pool
- **What**: The current pool of 9 fixed scores (`{501, 401, 351, 301, 251, 201, 167, 125, 101}`) is too small — regular players quickly see repeats. Expand to a curated pool of 20–30 starting scores across the full 101–501 range, keeping enough variety that each day feels different even within the same category. The random selection should be weighted to avoid consecutive identical scores in the same category.
- **Why deferred**: Waiting until more question data is populated so score variety is backed by genuine strategic depth (different targets make different answers viable).
- **See**: `DailyChallengeScheduler.java`, `DailyChallengeService.java`.

### Frontend test suite
- **What**: Zero frontend tests exist. The backend has 21 test files; the React app has none. A shipped product without frontend tests looks unfinished. Minimum viable coverage: component tests for the answer input flow (type → autocomplete → select → submit → see result), integration tests for the daily challenge flow (browse → start → play → share), and smoke tests for auth (login/logout, guest path).
- **Why deferred**: Components are still being built out; writing tests against half-finished features creates rework. Stable components (EntitySearch, lobby cards, daily challenge pages) can be tested now.
- **See**: New `__tests__/` directory under `frontend-react/`; Vitest + React Testing Library setup.

### Error boundaries
- **What**: The entire app has one `<Suspense fallback={null}>` and no React error boundaries. If a game component throws, the whole page goes white. A shipped app needs error boundaries around the game screen, lobby, daily challenge, and admin sections. Each boundary should show a recovery UI ("Something went wrong — reload this section").
- **Why deferred**: Individual component stability is still being worked on; error boundaries are cheap to add once components settle but should land before users see the app.
- **See**: New `ErrorBoundary.tsx` component; wrap in `MatchView.tsx`, `LobbyView.tsx`, `page.tsx` (home), admin `page.tsx`.

### Data population — run the scraper against the live database
- **What**: The Python scraper service (`trivia-501-scraper/`) has never been run against the Supabase production database. Geography and Film categories were only just activated. Question difficulty scores were computed on whatever data existed at the time — the scores are only as good as the underlying answer counts. Without a full data population pass, question pools are thin and difficulty ratings are unreliable.
- **Why deferred**: The scraper is a separate Python microservice that needs its own deployment and scheduling infrastructure. Acceptable for dev; unacceptable for real players.
- **See**: `trivia-501-scraper/`, `QuestionMaterializerService.java`, `DifficultyCalculator.java`.

### Solo forfeit game completion
- **What**: When a solo player forfeits via timeout, `handleGameCompletion` is never called from the real controller path (only from tests). The match stays `IN_PROGRESS` forever with a `COMPLETED` game inside it. Even if `handleGameCompletion` were called, `isMatchComplete` returns false because neither player's game-won counter increments — so the match never terminates. Needs a dedicated design pass: should a solo forfeit mark the match `ABANDONED`? Should it count as a loss?
- **Why deferred**: Solo forfeit is a rare edge case (3 consecutive timeouts required). The `handleGameCompletion` NPE from null `winnerId` was fixed, but the broader completion flow for solo forfeits needs wiring into the real controller path.
- **See**: `MatchService.java:141`, `GameStateMachine.java:138`, `GameService.java:139`.

---

## Deferred Indefinitely — Multiplayer & Competitive Ranking

The following items were previously P0/P1 launch blockers but are now parked. The product focus has shifted to single-player daily challenges and Free Play. These may return as a future phase but have no timeline.

### Real-time multiplayer (WebSocket STOMP)
- **What**: 1v1 real-time match communication over WebSocket (STOMP protocol). `GameStateMachine` and `GameService` have hooks for it, but no WebSocket handler is wired. The two-player turn alternation, opponent tracking, and match completion logic in `GameStateMachine` are designed for this but only tested via solo play.
- **Why parked**: Focus is on single-player daily challenges and Free Play. Multiplayer adds significant complexity (matchmaking, reconnection, timeout arbitration) that doesn't serve the current product direction.
- **What stays**: `GameStateMachine` two-player logic, `MatchFormat` best-of-3/5, `player2Id` fields — these are not being removed, just not built out.
- **See**: `docs/design/TECHNICAL_DESIGN.md`, `CLAUDE.md` — WebSocket Protocol section.

### MMR, league tiers, and ranked play
- **What**: The 9-tier × 4-subtier ranking system (Sunday League → Icon), hidden MMR, Elo-based matchmaking, and ranked/casual mode split described in the PRD.
- **Why parked**: No multiplayer means no ranked play. Player profiles stay as a personal dashboard (stats, history, personal bests) without competitive ranking.
- **What stays**: `player_profiles` table (V23 migration) — repurposed as a personal stats/history store. The `league_tier` and `league_subtier` columns may be dropped or left unused.
- **See**: PRD §4.1, §4.2; `player_profiles` table.

### Matchmaking queue
- **What**: `matchmaking_queue` table, opponent finding, Elo-based pairing.
- **Why parked**: No multiplayer.
- **See**: `docs/design/TECHNICAL_DESIGN.md`.

### Multiplayer game modes (Rapid Fire, Draft, Category Lock, Blind, Tournament)
- **What**: All P2 stretch game modes that require two players.
- **Why parked**: These depend on multiplayer infrastructure that is now deferred indefinitely. The design docs stay for reference; no implementation priority.
- **See**: `docs/design/GAME_MODES_STRETCH_GOALS.md`.

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

### Match history
- **What**: Players can't see past games or results after the match ends. A personal game log showing past daily challenges (date, category, score, emoji grid), Free Play games (category, question, final score), and personal bests is expected from any game that asks people to come back daily.
- **Why deferred**: Player profiles need to land first so history has a place to live. The `matches` and `games` tables already exist; a read endpoint and simple history page are the main work.
- **See**: New endpoint + page; `matches` and `games` tables, `player_profiles` table (V23).

### Server-side turn timer
- **What**: The 45s→30s→15s→forfeit timer ladder is displayed client-side but not enforced server-side. For solo play this is a UX feature (adds pressure/drama) rather than a competitive integrity concern. The timer should eventually be enforced server-side so the client display is authoritative, and so Free Play optionally supports timed games for players who want the pressure.
- **Why deferred**: Not a launch blocker without multiplayer. The client timer display works; server enforcement is a polish pass.
- **See**: `GameStateMachine.java`, `GameService.java`.

### Loading, error, and empty state consistency
- **What**: Around 60 references to loading states exist across components but there's no consistent pattern (some use inline conditionals, some use a `LoadingOverlay`, some don't handle loading at all). Certain flows lack error handling for API failures; others don't handle the empty/zero-results case (no autocomplete matches, no leagues found, no daily challenge available, no search results on admin pages). Every user-facing component must handle all three non-happy-path states.
- **Why deferred**: Individual feature work is still adding new components; standardising patterns now would create churn. Better to audit and fix once the component landscape stabilises.
- **See**: Audit `frontend-react/src/components/` for missing `isLoading`, `error`, and empty-state branches. Reference: `LoadingOverlay.tsx`.

### Open Graph metadata for daily challenge shares
- **What**: Emoji-grid sharing exists and works end-to-end, but shared URLs render as raw links with no title, description, or image preview in messaging apps. The Wordle comparison sets a high bar — shared results should show a rich card with the category name, final score, and emoji grid. This is a small static metadata change with outsized social reach.
- **Why deferred**: Share mechanics work; OG metadata is a polish layer that can be added without touching game logic.
- **See**: `frontend-react/src/app/daily/[category]/page.tsx`, `frontend-react/src/app/layout.tsx` (add `<meta>` tags).

### PWA offline capability validation
- **What**: The manifest and service worker files exist but haven't been tested on spotty/missing connections. The PRD promises PWA installability and sub-3s load on 3G. The game can't work fully offline (server-side validation), but the app shell, lobby, and daily challenge browse pages should cache and render without network.
- **Why deferred**: Core game mechanics are still being built; PWA optimisation matters once the shell is stable.
- **See**: `frontend-react/public/manifest.json`, `frontend-react/public/sw.js`, `frontend-react/next.config.ts`.

### Difficulty score recalibration after data population
- **What**: Difficulty scores were backfilled from existing answer counts. Once the scraper populates more answers, scores shift — some "hard" questions become easier, some "easy" questions show their true range. Re-run the backfill after a full scraper population pass, then review daily challenge question quality with real scores.
- **Why deferred**: Depends on data population (P0) being done first.
- **See**: `DifficultyCalculator.java`, `backfill_difficulty_scores.sql`.

---

## Backend Hardening

Stability and correctness improvements that are not urgently needed but should be done before the game sees meaningful load.

### Duplicate answer protection in `game_moves`
- **What**: Two guards against a player submitting the same answer twice (e.g. via double-tap or a network retry racing with the first request):
  1. **Unique constraint** on `(game_id, matched_answer_id)` in `game_moves` — DB rejects the second insert regardless of application logic.
  2. **`@Version` optimistic locking** on the `Game` entity — a version column that increments on every save; a concurrent second transaction throws `OptimisticLockException`, which `GlobalExceptionHandler` can return as a clean error.
- **Why deferred**: Design is still evolving; don't want an unnecessary Flyway migration that needs reversing. Current `@Transactional` wrapper prevents partial saves, so this is a hardening step, not a correctness emergency.
- **When to pick up**: Once the game schema is stable. Both changes together close the race condition and the duplicate submission gap.
- **Files**: `GameService.java:77`, `Game.java`, `GameMove.java`, `GlobalExceptionHandler.java`.

### Toggle on/off and customise turn timer
- **What**: Allow players to disable the turn timer entirely or set a custom duration before starting a Free Play game. The default 45s→30s→15s→forfeit ladder would remain the default for timed games.
- **Why deferred**: Requires server-side timer enforcement (P1) to be complete first. Customisation only makes sense once the baseline timer is solid.
- **When to pick up**: After the server-side turn timer P1 item is complete. Timer settings would live on the `matches` row (e.g. `timer_enabled BOOLEAN`, `turn_timer_override_seconds INTEGER`) so `GameStateMachine` can read them without changing its interface.
- **Files**: `GameStateMachine.java`, `Game.java`, `GameService.java`.

### Solo forfeit does not complete the match
- **Status**: Promoted to P0 launch blocker (see above). This item remains here as the original analysis with full context.
- **Files**: `MatchService.java:141`, `GameStateMachine.java:138`, `GameService.java:139`.

### Solo vs multiplayer: null-player2 design tension
- **What**: Solo (Free Play) games reuse the two-player `Match`/`Game` models with `player2Id = null`. This works but creates scattered `isSolo` branching in `GameStateMachine` (4+ sites) and `GameService` (2 sites). Fields like `player2Score`, `player2GamesWon`, and `MatchFormat.BEST_OF_1` are meaningless in solo context. With multiplayer deferred indefinitely, the null-player2 approach carries dead weight — but ripping it out would be a large refactor for no user-facing gain.
- **Verdict**: Keep the current null-based approach. The modes share ~80% of their logic and there's no third mode on the horizon. If the branching becomes a maintenance tax during Free Play work, extract it into a `TurnPolicy` strategy (SoloPolicy / TwoPlayerPolicy) that lives in one place.
- **Files**: `GameStateMachine.java:67,123,253`, `GameService.java:209,284`, `MatchService.java:78,148`.

### Per-turn used-answer query grows with game length
- **What**: `gameMoveRepository.findUsedAnswerIdsByGameId(gameId)` (called on every turn at `GameService.java:87`) fetches all used answer IDs from the start of the game. As a game progresses this set grows. Under load this is the query most likely to degrade first.
- **Why deferred**: Not a problem at current scale. The set is bounded by the number of valid answers for the question (~20–100 rows).
- **When to pick up**: If profiling shows this query becoming a bottleneck, consider caching the set in a `ConcurrentHashMap` keyed by `gameId` for the lifetime of the game (invalidated on game completion).

---

## Architectural Guardrails

Small, cheap decisions to make in the core game now that keep future modes open. These are not stretch goals — they are hygiene. Reference `docs/design/GAME_MODES_STRETCH_GOALS.md` when making schema or service boundary decisions.

With multiplayer deferred indefinitely, guardrails that only serve multiplayer modes are parked (see Deferred Indefinitely section above). Guardrails that serve Daily Challenge and Free Play remain active.

### Database

| Guardrail | Status | Why it matters |
|---|---|---|
| ~~Add `game_mode VARCHAR` to `matches`, default `'STANDARD'`~~ | ✅ Done (V19) | Every non-standard mode needs this column. Added in V19 with Daily Challenge support. |
| Store `question_id` on `game_moves` (not just on `games`) | 🅿️ Parked | Rapid Fire needs per-move question tracking. Parked with multiplayer. |
| ~~Add `suitable_for_daily BOOLEAN DEFAULT false` to `questions`~~ | ✅ Done (V19) | Explicit Daily Challenge pool flag. Added in V19; V20 backfills viable easy/medium questions. |
| Add `suitable_for_game_modes JSONB` to `question_templates` | 🅿️ Parked | Prevents unsuitable templates being re-enabled for wrong modes in future. Parked with multiplayer. |

### Backend

| Guardrail | Status | Why it matters |
|---|---|---|
| Question draw logic in a dedicated service (`QuestionDrawService`) | 🅿️ Parked | Rapid Fire swaps "draw once per game" for "draw once per turn". Parked with multiplayer. |
| Pass `gameMode` through game engine context from the start | Active | Already in place. Mode-specific rules for Daily/Free Play benefit from this. |

### Frontend

| Guardrail | Status | Why it matters |
|---|---|---|
| Mode label/badge component | Active | Gives the UI a place to display "Daily Challenge" vs "Free Play" mode names in listings and history. |

---

## P2 — Stretch Goals

Features that are designed but not being built until the core Daily Challenge + Free Play experience is stable and has real users.

### Async friend challenges
- **What**: Play the same daily challenge question as a friend and compare results. Not real-time — each player plays independently, then sees the other's result when both have finished. The social mechanic is: "I played today's Football daily, scored 87. Can you beat that?" Friend challenges would be surfaced via share links that deep-link into the challenge. If the recipient plays within 24 hours, the results are compared side-by-side with the same emoji-grid format.
- **Why deferred**: Requires a friend/challenge invitation system, result comparison UI, and push notification infrastructure. Build the single-player daily loop first, get retention data, then layer in social comparison.
- **See**: Daily challenge share links, `DailyChallengeController.java`.

### Expert Challenge
- **What**: Questions with `difficulty_score > 8.5` excluded from the standard daily pool. A separate "Expert Challenge" mode surfaces these for players who want the hardest questions.
- **Why deferred**: The expert question pool doesn't exist yet (needs data population + difficulty recalibration). The standard daily pool must be proven to work at normal difficulty before adding a hard mode.
- **See**: `DifficultyCalculator.java`, `DailyChallengeService.java`.

### Custom game rules (per-game and per-user preferences)
- **What**: Two complementary features:
  1. **Per-game rule overrides** — players choose a rule preset from the Free Play lobby. Rules stored as JSONB on the `games` row.
  2. **Per-user default preferences** — a `player_preferences` table storing preferred rule settings, auto-populated into the lobby selector.
- **Proposed rule knobs**:
  | Setting | Default | Description |
  |---|---|---|
  | Darts validation | Strict | Strict: 169, 176, etc. are invalid 3-dart checkouts → bust. Relaxed: only incorrect answers bust; any score 1–180 is valid. |
  | Checkout zone | -10 to 0 | Widens the win window. Options: -5 ("tight"), -10 ("standard"), -20 ("forgiving"). |
  | Max darts score | 180 | Could be raised (e.g. 200) for easier games. |
  | Turn timer | 45s | Custom duration or disabled entirely. |
- **Why deferred**: The core game rules are still being validated. Adding rule configurability while the base rules are in flux creates a test-matrix explosion. Defer until the strict-rules game loop has seen real play and we have feedback on whether the strict darts rules are causing confusion.
- **Files**: `DartsValidator.java`, `ScoringService.java`, `AnswerEvaluator.java:95-106`, `ScoreResult.java`, `GameHintsService.java:35`, `GameService.java:88`, `AdminAnswerService.java:51-52,157-162`, `QuestionMaterializerService.java:223-224`, `Match.java:75`, `Game.java`.

### Starting score variety for daily challenges
- **What**: Once the expanded curated pool of 20–30 starting scores is live (P0), consider making the score selection smarter — e.g. avoiding consecutive repeats within the same category, weighting toward under-used scores, or showing a "score of the day" reveal animation. The goal is making each day feel like a genuinely different puzzle even when the question category repeats.
- **Why deferred**: Get the expanded pool working first (P0), then iterate on the selection algorithm based on play data.
- **See**: `DailyChallengeScheduler.java`, `DailyChallengeService.java`.

### Custom game rules (per-match and per-user preferences)

- **What**: Two complementary features:
  1. **Per-game rule overrides** — players choose a rule preset from the lobby before starting a game. Rules are stored as JSONB on the `games` row and threaded through `ScoringService` + `DartsValidator` at runtime.
  2. **Per-user default preferences** — a `player_preferences` table storing the player's preferred rule settings, auto-populated into the lobby selector. A `GET/PUT /api/player/preferences` endpoint and a small frontend settings panel.
- **Proposed rule knobs**:
  | Setting | Default | Description |
  |---|---|---|
  | Darts validation | Strict | Strict: 169, 176, etc. are invalid 3-dart checkouts → bust. Relaxed: only incorrect answers bust; any score 1–180 is valid. |
  | Checkout zone | -10 to 0 | Widens the win window. Options: -5 ("tight"), -10 ("standard"), -20 ("forgiving"). |
  | Max darts score | 180 | Could be raised (e.g. 200) for easier games. |
  | Turn timer | 45s | Custom duration or disabled entirely (overlaps with Backend Hardening item above; decide which owns it). |
- **Architecture notes** (from 2026-06-08 exploration):
  - `DartsValidator.isValidDartsScore()` is `private static final` — would need a `GameRules`-taking overload.
  - `ScoringService` constants `CHECKOUT_MIN = -10` / `CHECKOUT_MAX = 0` are `private static final` — would read from `GameRules` instead.
  - `answers.isValidDarts` and `answers.isBust` are pre-computed at import time under strict rules. For relaxed-rules games, `AnswerEvaluator` must **ignore those DB flags** and recompute bust purely at runtime from `ScoringService`. This is the right place — `AnswerEvaluator` already orchestrates the scoring call.
  - `ScoreResult.bust()` was given a `reason` parameter in the 2026-06-08 reason-surfacing pass — the reason text naturally adapts to the active rules.
  - `GameHintsService` has a hardcoded `CHECKOUT_WINDOW = 10` that must become dynamic.
  - `game_mode` on `matches` is already a write-only column (set by `DailyChallengeService`, never read by game logic). Adding a `rules JSONB` column on `games` is cleaner than overloading `game_mode`.
  - There is also an existing `isBust` discrepancy: `AdminAnswerService` computes `isBust = !isValidDartsScore(score)` while `QuestionMaterializerService` uses `isBust = score > 180`. The SQL migrations align with the materializer. Adding custom rules provides a natural moment to resolve this — bake bust logic into one place (`ScoringService`) and stop persisting it on answers.
- **Why deferred**: The core game rules are still being validated. Adding rule configurability while the base rules are in flux creates a test-matrix explosion. Defer until (a) the strict-rules game loop has seen real play, and (b) at least one more game mode is implemented so the configuration surface is better understood.
- **When to pick up**: After Daily Challenge has real users and we have feedback on whether the strict darts rules are causing confusion. The 2026-06-08 reason-surfacing pass (showing why a move was rejected) will give us that signal — if players consistently need the reason explained, relaxed rules become higher priority.
- **Files**: `DartsValidator.java`, `ScoringService.java`, `AnswerEvaluator.java:95-106`, `ScoreResult.java`, `GameHintsService.java:35`, `GameService.java:88`, `AdminAnswerService.java:51-52,157-162`, `QuestionMaterializerService.java:223-224`, `Match.java:75`, `Game.java`.

### Football: league-scope questions for "Random League Question" option
- **What**: All current football questions are `q_scope = 'club'` (tied to a specific team). A "league-scope" question would ask about the whole league (e.g. "Top scorers in the Premier League since 2000" — valid answers are any player from any club in that league). The lobby had a "Random League Question" option that fell back to club questions when none existed, causing confusion. The option has been replaced with a single "Random Question" entry that uses `scope: random_any` until league-scope questions are added.
- **Why deferred**: Requires a new question template (`football.competition_metric_since` without a `team_id` param), a new materializer variant that aggregates across all teams in a competition, and new DB migrations to seed these questions.
- **When to pick up**: After the core game loop is stable and the scraper service is running. The lobby entry in `FootballScreen` can be restored with `scope: "random_league_level"` once `findRandomFootballLeagueQuestion()` returns results.
- **Files**: `LobbyView.tsx` (FootballScreen), `QuestionService.java:206-209`, `QuestionRepository.java:278-285`, `FootballFilter.java`, `V24__add_football_question_template_columns.sql`.

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
| Autocomplete: cancel debounce on submit, auto-submit on selection, fix focus on click | (current) | `selectEntity` clears debounce + calls `onSelect`; Enter path cancels stale timer; `onMouseDown` uses `preventDefault` to keep focus on input |
| Daily Challenge mode (Wordle-style) | (current) | One challenge per category per day, variable starting scores (101–501), emoji-grid sharing, lazy creation + midnight cron. V19 schema + V20 data backfill. No leaderboards/replay enforcement — trust-based. |
| Lobby redesign: target score toggle + category flow | (current) | Two-column layout; 501/301/101/RND toggle drives `startingScore` POST param; Football 2-step (Random / Select League → 5 leagues); other categories 1-click start. `questionHierarchy.ts` and `CategoryPopup.tsx` are now unused — deferred cleanup. |
| Football question hierarchy + structured filter | (current) | V24 migration (q_scope/q_league/q_club/q_stat + unique index); FootballFilter DTO; 6 new repo query methods; QuestionService.selectQuestionByFilter; FootballController GET /api/football/clubs; SecurityConfig /api/football/** permitAll; LobbyView 4-screen navStack (root→football→league→club) with slide animations + DB-driven club list + stat type picker (7 stat types). V25 backfill migration for q_* columns on existing questions. V26 seeds 4 combined-stat templates. FootballTeamCompetitionMetricSinceMaterializer extended with goals_assists/goals_appearances/assists_appearances/goals_assists_appearances. QuestionGeneratorService now populates q_* columns on new draft questions. |
| Guardrails: `game_mode` on matches, `suitable_for_daily` on questions | (current) | V19 migration. `game_mode` defaults to `'STANDARD'`; `suitable_for_daily` backfilled for viable easy/medium questions. |
| Geography + Film category seed data (CSV-based Java migrations) | (current) | V21/V22: Replaced ~12K lines of hardcoded SQL INSERTs with CSV data files (`db/data/*.csv`) loaded by `BaseJavaMigration` subclasses. Pattern is now standard for new category seeding. `.gitignore` updated: `!**/db/data/*.csv`. |
