# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Football 501 is a competitive football trivia game that combines football knowledge with darts 501 scoring mechanics. Players compete to reduce their score from 501 to exactly 0 by naming football players whose statistics match a given question.

**Current Status**: Planning & Design phase complete. Ready to begin MVP development.

**Tech Stack**:
- Frontend: SvelteKit + TypeScript + Tailwind CSS (Progressive Web App)
- Backend: Spring Boot 3.x + Java 17+ + PostgreSQL 15+
- Real-time: WebSocket (STOMP protocol)
- External API: API-Football (api-football.com)

## Architecture

### High-Level System Design

The application follows a client-server architecture with WebSocket support for real-time multiplayer:

```
PWA Client (SvelteKit) <--HTTPS + WSS--> Spring Boot Server
                                              |
                                         PostgreSQL + Redis
```

**Critical Architectural Principles**:
1. **Zero API Calls During Gameplay**: All match validation uses pre-cached player data from the database. API-Football is only called during batch question population and weekly updates.
2. **Server-Side Validation**: All game logic (scoring rules, darts validation, win conditions) is validated server-side to prevent cheating.
3. **Aggressive Caching**: Question/answer data is cached in PostgreSQL. Match state may be cached in Redis.

### Backend Module Structure

The Spring Boot application is organized into modules:
- **API Module**: REST endpoints for CRUD operations
- **WebSocket Module**: Real-time match communication handler
- **Game Engine Module**: Core game logic (scoring, validation, win conditions)
- **Auth Module**: User authentication (OAuth 2.0 + guest accounts)
- **Scheduler Module**: Automated tasks (daily challenge generation, stats refresh)
- **Integration Module**: External API client for API-Football

### Frontend Architecture

- **SPA Architecture**: Single-page application with client-side routing
- **State Management**: Svelte stores for reactive state
- **WebSocket Client**: Native WebSocket for real-time game updates
- **Optimistic UI**: Client updates UI immediately, then syncs with server

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

## API Integration (API-Football)

### Critical Rules

1. **Free Tier Limits**: 100 requests/day during MVP
2. **Batch Population Only**: Questions are populated in advance via scheduled jobs
3. **Zero Live API Calls**: Matches NEVER call the API - all validation uses cached `answers` table
4. **Weekly Updates**: Automated batch refresh of current season stats (Sunday 3 AM UTC)

### Question Population Workflow

```
1. Admin creates question (manual/admin panel)
2. Scheduled job fetches player stats from API-Football (respects rate limits)
3. Populate answers table with:
   - Player names
   - Statistics (appearances/goals/etc.)
   - Pre-computed validity flags (isValidDartsScore, isBust)
4. Mark question as active
```

### API Response Mapping

Key fields from API-Football `/players` endpoint:
- `response[].player.name` → Player display name
- `response[].player.id` → Player API ID (unique identifier)
- `response[].statistics[].games.appearences` → Appearances stat
- `response[].statistics[].goals.total` → Goals stat
- `response[].player.nationality` → For nationality filters

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

### Authentication
- OAuth 2.0 social login (Google, Apple, Facebook)
- JWT tokens (24-hour expiration)
- Guest accounts (ephemeral, 24-hour inactivity timeout)
- HTTPOnly cookies for token storage

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

### Project Setup (Not Yet Implemented)

When setting up the project for the first time:

**Frontend**:
```bash
cd frontend
npm install
npm run dev  # Dev server on http://localhost:5173
```

**Backend**:
```bash
cd backend
# If using Maven:
mvn spring-boot:run

# If using Gradle:
./gradlew bootRun

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

All other scores 1-180 are valid. Implementation should pre-compute this during answer population and store in `is_valid_darts_score` column.

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

## Key Design Decisions

### Why SvelteKit?
- Simpler syntax than React/Vue
- Better performance (compiler-based)
- Built-in PWA support
- Easier learning curve for backend-focused developer

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
- Technical Design: `docs/design/TECHNICAL_DESIGN.md`
- API Integration: `docs/api/API_INTEGRATION.md`

## Current Development Phase

**Phase**: Planning & Design (Complete) → Beginning MVP Development

**Next Steps**:
1. Frontend project setup (SvelteKit + TypeScript)
2. Backend project setup (Spring Boot + PostgreSQL)
3. Database schema implementation
4. User authentication (OAuth + guest accounts)
5. Basic REST API
6. Game engine implementation (scoring rules, darts validation)
