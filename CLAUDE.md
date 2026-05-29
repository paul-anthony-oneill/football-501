# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Backlog Discipline

Before starting any task, check **`docs/BACKLOG.md`**. After completing or deferring work:
- **Tick off** any backlog item your change addresses (move it to the Completed table).
- **Add a new entry** if you are consciously deferring something that came up during the task — capture what it is, why it's deferred, and which files are relevant.
- **Do not add** Flyway migrations, new abstractions, or hardening work that isn't needed right now just because it feels tidy. Defer it and document it instead.

## Project Overview

Football 501 is a competitive football trivia game that combines football knowledge with darts 501 scoring mechanics. Players compete to reduce their score from 501 to exactly 0 by naming football players whose statistics match a given question.

**Current Status**: Audit Fixes Complete (Phase 5 of 5).

**Tech Stack**:
- Frontend: Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS 4 — `frontend-react/`
- Backend: Spring Boot 4.0.6 + Java 25 + PostgreSQL 15+
- Data Source: ScraperFC (Python Microservice)
- Real-time: WebSocket (STOMP protocol) - *In Progress*

## Architecture

### High-Level System Design

The application follows a client-server architecture with a separate Python microservice for data scraping:

```
PWA Client (Next.js/React) <--HTTPS + WSS--> Spring Boot Server
                                                   |
                                              PostgreSQL
                                                   ^
                                                   |
                                       Python Scraper Service (ScraperFC)
```

**Critical Architectural Principles**:
1. **Zero API Calls During Gameplay**: All match validation uses pre-cached player data from the database.
2. **Server-Side Validation**: All game logic (scoring rules, darts validation, win conditions) is validated server-side to prevent cheating.
3. **Aggressive Caching**: Question/answer data is cached in PostgreSQL.
4. **Scraping Service**: A dedicated Python microservice uses ScraperFC to populate the database via batch jobs, keeping the main backend clean.

### Backend Module Structure

The Spring Boot application is organized into modules:
- **API Module**: REST endpoints for CRUD operations
- **WebSocket Module**: Real-time match communication handler
- **Game Engine Module**: Core game logic (scoring, validation, win conditions)
- **Auth Module**: User authentication (OAuth 2.0 + guest accounts)
- **Scheduler Module**: Automated tasks (daily challenge generation, stats refresh)
- **Integration Module**: External API client for API-Football

### Named Entity Autocomplete

The `entities` table is a global registry of named things that players can type as answers during gameplay. It powers the autocomplete dropdown that appears as the player types.

**Key design constraint**: the `entities` table is intentionally decoupled from the `answers` table. A name appearing in autocomplete tells the player nothing about whether it is a valid answer to the current question. All answer validation happens server-side in `AnswerEvaluator` against the `answers` table only.

**How it works**:
- `entity_type` column (e.g. `"footballer"`, `"city"`, `"country"`) groups names into pools
- Questions declare which pool to use via a `config` JSONB column: `{"entity_type": "footballer", ...}` or `{"entity_type": "city", ...}`
- The frontend reads `question.config.entity_type` and passes it to the `EntitySearch` component, which calls `GET /api/entities/search?type={entityType}&query={query}`
- Search uses PostgreSQL `unaccent()` + a GIN trigram index (`idx_entities_unaccent_trgm`) for accent-insensitive substring matching — typing "aguero" returns "Sergio Agüero"
- The `entities` table is populated automatically: when answers are bulk-imported via `AdminAnswerService`, it calls `EntitySearchService.upsertEntity()` for each player name. The upsert is idempotent and uses `(entity_type, normalized_name)` as the unique key.

**Naming note**: the Java model class is `NamedEntity` (not `Entity`) to avoid a name clash with the JPA `@Entity` annotation.

See `docs/design/AUTOCOMPLETE_ENTITY_DESIGN.md` for the full design document, including how to add a new entity type.

### Frontend Architecture

- **App Router**: Next.js 16 App Router; all pages are `"use client"` (no SSR needed — data comes from Spring Boot)
- **State Management**: Local `useState` + React Context API (`ToastContext`); no Redux/Zustand
- **API Proxy**: `next.config.ts` rewrites `/api/*` → `http://localhost:8080/api/*` in dev
- **WebSocket Client**: Native WebSocket for real-time game updates
- **Optimistic UI**: Client updates UI immediately, then syncs with server
- **Styling**: Tailwind v4 with `@theme inline` in `globals.css` (no `tailwind.config.ts`)
- **Entity Autocomplete**: `EntitySearch.tsx` (`frontend-react/src/components/game/EntitySearch.tsx`) is the autocomplete input component used during gameplay. It accepts an `entityType` prop — the caller reads `question.config.entity_type` and passes it through; the prop defaults to `"footballer"` for backward compatibility. The component fires a search after 4 characters are typed, shows a "Keep typing…" hint at 1–3 characters, and fills the input on selection without auto-submitting so the player can confirm.

## Core Game Mechanics

### Scoring System (Critical)

1. Both players start at 501 points
2. Players alternate naming football players matching the question
3. Player's statistic (appearances/goals/etc.) is deducted from score
4. **Valid Darts Scores**: Only scores achievable with 3 darts in standard 501 are valid
   - Invalid scores (163, 166, 169, 172, 173, 175, 176, 178, 179) result in **bust** (no score deducted)
5. **Bust Rules**:
   - Scores > 180 = bust (turn wasted)
   - Scores below -10 = bust
6. **Checkout Range**: -10 to 0 (inclusive) to win
7. **Close Finish Rule**: If Player 1 checks out, Player 2 gets one final turn to get closer to 0

### Question Types

All questions must be pre-populated with answers from API-Football:

1. **Team League Appearances**: "Appearances for [Team] in [League]"
2. **Combined Stats**: "Appearances + Goals for [Team] in [League]" (score = sum)
3. **Goalkeeper Stats**: "Appearances + Clean Sheets for [Team] in [League]"
4. **International**: "Appearances for [Country] in [Competition]"
5. **Nationality Filter**: "Appearances in [League] by players from [Country]"

### Turn Rules

- Default turn timer: 45 seconds
- Consecutive timeouts reduce timer (45s → 30s → 15s → forfeit)
- Invalid answers allow instant retry (timer keeps running)
- Fuzzy matching for player names (e.g., "Aguero" matches "Agüero")

## Database Schema

Key tables to understand:

### Core Tables
- `users`: User accounts (guest, registered, premium)
- `player_profiles`: Ranking data (MMR, league points, win/loss records)
- `questions`: Question definitions with API-Football metadata
- `answers`: Pre-cached player statistics (NEVER queried from API during matches)

### Match Tables
- `matches`: Match metadata (type, format, players, status)
- `games`: Individual games within a match (best-of-3/5)
- `game_moves`: Turn-by-turn history (answer, score, validity)

### Special Tables
- `daily_challenges`: Daily challenge questions
- `daily_challenge_results`: Leaderboard data
- `matchmaking_queue`: Active matchmaking queue

**Critical Indexes**:
- `answers` table uses `gin_trgm_ops` index for fuzzy player name matching
- Full-text search enabled on player names

## Data Source Integration (ScraperFC)

### Critical Rules

1. **Python Microservice**: All scraping logic resides in `football-501-scraper/`.
2. **Batch Population**: Questions are populated in advance via scheduled jobs in the Python service.
3. **Zero Live Calls**: The main Spring Boot backend NEVER calls external APIs. It only reads from the `answers` table.
4. **Weekly Updates**: Automated batch refresh of current season stats.

### Data Flow

```
1. Admin creates question (or auto-generated)
2. Python Service (ScraperFC) fetches player stats from FBref
3. Service transforms data and populates `answers` table:
   - Player names (normalized)
   - Statistics (value)
   - Validity flags (isValidDartsScore, isBust)
4. Spring Boot Backend reads from `answers` table during gameplay
```

### Schema Mapping

ScraperFC data maps to the `answers` table:
- Player Name → `player_name`
- Statistic (e.g., Appearances) → `statistic_value`
- Validation (1-180, no invalid darts) → `is_valid_darts`
- Bust Check (>180) → `is_bust`

## WebSocket Protocol

### Connection
- Endpoint: `wss://api.football501.com/ws?token={JWT_TOKEN}`
- Protocol: STOMP over WebSocket
- Heartbeat: 30-second ping/pong

### Message Format
```json
{
  "type": "MESSAGE_TYPE",
  "gameId": "uuid",
  "payload": { /* message-specific data */ }
}
```

### Critical Messages

**Client → Server**:
- `SUBMIT_ANSWER`: Submit player name for validation
- `REQUEST_REFRESH`: Request new question (only at game start)
- `VOTE_REFRESH`: Vote on opponent's refresh request

**Server → Client**:
- `GAME_STATE`: Full game state update (scores, turn, timer)
- `ANSWER_RESULT`: Validation result (valid/bust/invalid)
- `TURN_TIMEOUT`: Timeout event (timer reduction)
- `GAME_OVER`: Game complete (winner, final scores)

### Reconnection Handling
- 30-second grace period on disconnect
- Server maintains game state during grace period
- Full game state sync on reconnect

## Security

### Current Security Architecture (Dev-Mode Permissive)

Spring Security is fully wired (`SecurityConfig.java`, `DevModeAuthFilter.java`) but operates in a permissive dev-mode posture. See `docs/SECURITY_ARCHITECTURE.md` for the complete model.

**How identity works today**:
- `DevModeAuthFilter` (`@Profile("!prod")`) runs on every non-production profile and injects a fixed authenticated principal (`DEV_PLAYER_ID = "00000000-0000-0000-0000-000000000001"`) with `ROLE_USER` and `ROLE_ADMIN`.
- `PracticeGameController` reads player identity from `Principal.getName()` — never from a request parameter. Identity spoofing via `@RequestParam playerId` has been removed.
- All five admin controllers carry `@PreAuthorize("hasRole('ADMIN')")` at class level; `@EnableMethodSecurity` activates these annotations.

**URL-level access policy** (enforced by `SecurityConfig`):
- `/api/entities/**` and `/api/categories/**` — `permitAll` (public autocomplete and listing)
- `/actuator/health` — `permitAll` (liveness probe)
- `/api/practice/**` — `ROLE_USER` or `ROLE_ADMIN`
- `/api/admin/**` — `ROLE_ADMIN`
- everything else — `authenticated`

**What is deferred to production**:
- Real OAuth 2.0 / JWT validation filter (replaces `DevModeAuthFilter`)
- CSRF protection (currently disabled for stateless REST; re-evaluate when JWT cookies are introduced)
- Content Security Policy (add in Next.js `middleware.ts`)

### Anti-Cheat Measures
- All game logic validated server-side
- Hidden MMR for fair matchmaking
- Server-side turn timing enforcement
- Answer validation against cached database only

### Rate Limiting
- Authenticated users: 100 req/min
- Unauthenticated: 10 req/min per IP
- WebSocket message rate limiting

## Ranking System

### Structure
- 9 Tiers × 4 Subtiers = 36 total ranks
- Tiers: Sunday League → Amateur → Semi-Pro → Journeyman → Pro → International → Continental → World Class → Icon
- Subtiers: Reserve → Rotation → Starter → Captain

### Progression
- Hidden MMR for matchmaking (Elo-based)
- Visible League Points for rank display
- Win vs higher MMR = more points
- Continuous progression (no seasonal resets)

## Development Workflow

### Project Setup

**Frontend (React — active)**:
```bash
cd frontend-react
npm install
npm run dev  # Dev server on http://localhost:3000
# API calls proxied to http://localhost:8080 via next.config.ts
```

**Backend**:
```bash
cd backend
# Requires Java 25 (set via JAVA_HOME or .mvn/jvm.config)
mvn spring-boot:run
# Server runs on http://localhost:8080
```

**Database**:
```bash
# Using Docker for local PostgreSQL
docker run -d \
  --name football501-postgres \
  -e POSTGRES_DB=football501 \
  -e POSTGRES_USER=football501 \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  postgres:15
```

### Testing Strategy

**Frontend**:
- Unit tests: Vitest (component logic, stores, utilities)
- E2E tests: Playwright (critical user flows)

**Backend**:
- Unit tests: JUnit 5 (service layer, game engine rules)
- Integration tests: Spring Boot Test + TestContainers (PostgreSQL)
- E2E tests: Full match flow (REST + WebSocket)

**Coverage Target**: > 80% for unit tests

## Darts Score Validation

Critical utility for game engine - validates if a score is achievable in standard 501 darts:

**Invalid Scores**: 163, 166, 169, 172, 173, 175, 176, 178, 179

All other scores 1-180 are valid. Implementation should pre-compute this during answer population and store in `is_valid_darts` column.

## Environment Variables

Required environment variables:

```bash
# API-Football
FOOTBALL_API_KEY=your_api_key_here

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=football501
DB_USER=football501
DB_PASSWORD=your_password_here

# JWT
JWT_SECRET=your_secret_key_here
JWT_EXPIRATION=86400000  # 24 hours in ms

# OAuth (when implemented)
OAUTH_GOOGLE_CLIENT_ID=
OAUTH_GOOGLE_CLIENT_SECRET=
OAUTH_APPLE_CLIENT_ID=
OAUTH_APPLE_CLIENT_SECRET=
OAUTH_FACEBOOK_CLIENT_ID=
OAUTH_FACEBOOK_CLIENT_SECRET=
```

## Performance Targets

| Metric | Target |
|--------|--------|
| API Response Time (p95) | < 200ms |
| WebSocket Message Latency | < 100ms |
| PWA Load Time (3G) | < 3s |
| Database Query Time | < 50ms |
| Matchmaking Time | < 10s |

## Common Pitfalls to Avoid

1. **Never call API-Football during match validation** - Always use cached `answers` table
2. **Always validate darts scores** - Scores like 179 are invalid and must be treated as bust
3. **Server-side validation required** - Client optimistic updates must be confirmed by server
4. **Respect API rate limits** - Free tier is 100 req/day, plan batch operations carefully
5. **Fuzzy matching complexity** - Use PostgreSQL trigram indexes (`gin_trgm_ops`) for player name matching
6. **Close finish rule** - Player 2 always gets final turn if Player 1 checks out first
7. **Consecutive timeout tracking** - Non-consecutive timeouts reset the timer back to default
8. **Never use `answers` as the autocomplete source** - It would reveal valid answers for the current question. Autocomplete must query the `entities` table only.
9. **Always populate `entities` when adding answers** - `AdminAnswerService` does this automatically via `EntitySearchService.upsertEntity()`. If running a manual SQL backfill, see `docs/design/AUTOCOMPLETE_ENTITY_DESIGN.md`.
10. **Match `entity_type` slug in question config to the `entities` pool** - If `config` says `"entity_type": "city"` but no city rows exist in `entities`, the autocomplete will silently return nothing. Seed the pool before activating the question type.
11. **Use `EntityType` constants, never bare strings** - Use `EntityType.FOOTBALLER`, `EntityType.CITY`, etc. (in `com.football501.model.EntityType`). Bare `"footballer"` literals were a Phase 5 audit finding; compile-time constants prevent silent slug drift.
12. **Do not add local `@ExceptionHandler` to controllers** - `GlobalExceptionHandler` (`com.football501.exception`) owns all error formatting. Local handlers produce inconsistent JSON shapes and were eliminated in Phase 5.
13. **Do not read `playerId` from request parameters in game controllers** - Identity comes from `Principal.getName()` parsed as a UUID. Adding a `@RequestParam UUID playerId` re-opens the identity spoofing vulnerability fixed in Phase 1.
14. **New model classes must use `@EntityListeners(AuditingEntityListener.class)`** - `@CreatedDate` and `@LastModifiedDate` only populate when this listener is registered. Manual `LocalDateTime.now()` in `@PrePersist` is the old pattern; do not reintroduce it.

## Key Design Decisions

### Why Next.js + React?
- Migrated from SvelteKit (May 2026) for broader ecosystem and hiring pool
- App Router with `"use client"` pages matches the SPA mental model from SvelteKit
- Tailwind v4 `@theme` tokens map cleanly onto the existing CSS variable system
- React Context replaces per-page Svelte store patterns with minimal overhead

### Why PostgreSQL?
- ACID compliance for critical operations (rankings, match results)
- Full-text search with trigram matching for fuzzy player names
- JSON support for flexible data storage
- Free and open-source

### Why Aggressive Caching?
- API-Football free tier limits (100 req/day)
- Match validation must be instant (< 200ms)
- Offline capability for cached questions
- Cost optimization for MVP

### Why Server-Side Game Logic?
- Prevent cheating (client-side validation can be bypassed)
- Consistent rule enforcement across all clients
- Single source of truth for game state
- Easier to update rules without client updates

## Documentation References

- Product Requirements: `docs/PRD.md`
- Game Rules: `docs/GAME_RULES.md`
- **Backlog & Future Work: `docs/BACKLOG.md`** ← check this before starting any task; update it when you defer something or complete a backlog item
- Technical Design: `docs/design/TECHNICAL_DESIGN.md`
- API Integration: `docs/api/API_INTEGRATION.md`
- Autocomplete & Entity Architecture: `docs/design/AUTOCOMPLETE_ENTITY_DESIGN.md`
- Game Modes & Stretch Goals: `docs/design/GAME_MODES_STRETCH_GOALS.md`
- Question Difficulty Scoring: `docs/design/DIFFICULTY_SCORING.md`

## Question Difficulty Scoring

Questions use a **continuous `difficulty_score` (NUMERIC 0.00–10.00)** rather than a discrete enum. See `docs/design/DIFFICULTY_SCORING.md` for the full design.

**Key points for development**:
- Score is computed at materialisation time from four stored counts: `high_value_count`, `mid_range_count`, `checkout_count`, `total_valid_count`
- Storing the counts (not just the score) means formula tuning is a single SQL UPDATE — no re-materialisation needed
- `difficulty_locked BOOLEAN` allows admin override of computed scores
- `single_question_viable BOOLEAN` derived from `total_score_pool >= 501` — questions failing this are excluded from standard single-question mode
- The old `difficulty INTEGER` (1/2/3 scale, V4 migration) is deprecated but retained until all callers migrate

**Formula constants all live in `DifficultyConstants.java`** — never hardcode zone boundaries or weights elsewhere.

## Game Modes — What's Parked & Why It Matters

Multiple game modes (Daily Challenge, Rapid Fire, Draft, Category Lock) are **designed but not being implemented yet**. See `docs/design/GAME_MODES_STRETCH_GOALS.md` for full details.

**Core principle to carry through all development**: The game's difficulty is strategic, not just knowledge-based. Knowing *when* to play an answer and *which score to target* is the depth — not just knowing valid names. Easy/medium questions can produce deeply competitive games.

**Architectural guardrails to apply now** (cheap decisions that keep future modes open):
- `game_mode VARCHAR` on `matches`, default `'STANDARD'`
- `question_id` on `game_moves` (not just on `games`) — needed for Rapid Fire's per-turn question tracking
- `difficulty_score NUMERIC(4,2)` on `questions` (continuous 0–10 scale, not an enum — see Difficulty Scoring section above)
- `suitable_for_daily BOOLEAN` on `questions`
- Question draw logic must live in a dedicated service method — never inline in game start code

## Current Development Phase

**Phase**: Audit Fixes Complete (Phase 5 of 5) — Moving to MVP Features

The five-phase audit campaign is complete. All critical security, data integrity, architecture, scoring, and cleanup findings have been addressed. The codebase is now in a clean state for building the remaining MVP features.

**What was completed in the audit campaign**:
- Phase 1: Spring Security plumbing, identity spoofing fix, `@PreAuthorize` on admin controllers
- Phase 2: Backfill upsert (single SQL `INSERT … ON CONFLICT`), JPA auditing, normalization contract test
- Phase 3: `GameStateMachine` coordinator, `useGameLoop` hook, admin page decomposition
- Phase 4: `DifficultyCalculator`, viability gate, V13 migration, recalibration endpoint
- Phase 5: MapStruct mappers, `GlobalExceptionHandler`, `EntityType` constants, SvelteKit frontend deleted

**Next Steps** (remaining MVP work):
1. Real authentication — replace `DevModeAuthFilter` with a JWT validation filter; wire OAuth 2.0 (Google social login first)
2. WebSocket multiplayer — STOMP handler for real-time 1v1 matches; `GameStateMachine` is already ready to receive WebSocket events
3. Player profiles and matchmaking queue
4. Data population — run the Python scraper service against the live database to seed `questions` and `answers`
5. Run `backfill_difficulty_scores.sql` after data population to compute initial difficulty scores
