# Football 501 - Technical Design Document

**Version**: 1.0
**Last Updated**: 2026-01-17
**Status**: Draft
**Author**: Paul

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Data Models](#data-models)
5. [API Design](#api-design)
6. [Real-Time Communication](#real-time-communication)
7. [Security](#security)
8. [Deployment](#deployment)
9. [Performance](#performance)
10. [Testing Strategy](#testing-strategy)

---

## System Overview

Football 501 is a full-stack web application with real-time multiplayer capabilities. The system follows a client-server architecture with WebSocket support for live match updates.

### High-Level Architecture

```
┌─────────────────┐
│   PWA Client    │  (Svelte + SvelteKit)
│  Desktop/Mobile │
└────────┬────────┘
         │ HTTPS + WSS
         │
┌────────▼────────┐
│   Load Balancer │  🔄 IN PROGRESS
└────────┬────────┘
         │
┌────────▼────────────────┐
│   Application Server    │  (Spring Boot)
│   - REST API            │
│   - WebSocket Handler   │
│   - Game Logic Engine   │
│   - Authentication      │
└────────┬────────────────┘
         │
    ┌────┴────┐
    │         │
┌───▼────┐ ┌─▼──────────┐
│Database│ │Redis Cache │  🔄 IN PROGRESS
│(PostgreSQL) │(Sessions)  │
└────────┘ └────────────┘
```

---

## Architecture

### 1. Client Layer (Frontend)

**Progressive Web App (PWA)**
- Built with **SvelteKit** framework
- Fully responsive design (mobile-first)
- Offline capability for game rules, profile viewing
- Service Worker for caching and push notifications
- Installable on mobile devices (iOS/Android)

**Key Features**:
- Single Page Application (SPA) architecture
- Client-side routing
- State management with Svelte stores
- WebSocket client for real-time match updates
- Optimistic UI updates

### 2. Server Layer (Backend)

**Spring Boot Application**
- RESTful API for CRUD operations
- WebSocket server for real-time match communication
- Server-side game logic validation
- Authentication & authorization
- Rate limiting and security

**Modules**:
1. **API Module**: REST endpoints
2. **WebSocket Module**: Real-time match handler
3. **Game Engine Module**: Core game logic
4. **Auth Module**: User authentication
5. **Scheduler Module**: Daily challenge generation
6. **Integration Module**: External API client (API-Football)

### 3. Data Layer

**Primary Database**: PostgreSQL
- Relational data (users, questions, matches, rankings)
- ACID compliance for critical operations (match results, rankings)
- Full-text search for player names (fuzzy matching)

**Cache Layer**: 🔄 IN PROGRESS
- Redis for session storage
- Match state caching
- Leaderboard caching
- Rate limiting counters

---

## Technology Stack

### Frontend

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Framework | **SvelteKit** | Minimal boilerplate, excellent performance, built-in PWA support, easier learning curve for backend developers |
| Language | **TypeScript** | Type safety, better IDE support, reduces runtime errors |
| Styling | **Tailwind CSS** | Utility-first, responsive design, mobile-first workflow |
| State Management | **Svelte Stores** | Built-in, reactive, simple API |
| HTTP Client | **Fetch API** | Native, lightweight, sufficient for REST calls |
| WebSocket Client | **Native WebSocket** | No additional library needed |
| PWA | **SvelteKit + Vite PWA Plugin** | Official support, easy configuration |
| Build Tool | **Vite** | Fast builds, HMR, optimized production bundles |

### Backend

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Framework | **Spring Boot 3.x** | Developer familiar with Java/Spring, robust ecosystem, excellent WebSocket support |
| Language | **Java 17+** | LTS version, modern features, excellent tooling |
| Database | **PostgreSQL 15+** | ACID compliance, full-text search, JSON support, free and open-source |
| ORM | **Spring Data JPA / Hibernate** | Integrated with Spring, reduces boilerplate |
| WebSockets | **Spring WebSocket (STOMP)** | Native Spring support, easy integration |
| Security | **Spring Security** | Industry standard, OAuth2 support, comprehensive |
| API Documentation | **Springdoc OpenAPI** | Auto-generates API docs, Swagger UI |
| Testing | **JUnit 5, Mockito, TestContainers** | Spring ecosystem standard |
| Build Tool | **Maven** or **Gradle** | 🔄 IN PROGRESS (TBD) |

### Infrastructure (🔄 IN PROGRESS)

| Component | Technology | Status |
|-----------|-----------|--------|
| Hosting | TBD | Options: AWS, GCP, DigitalOcean, Railway |
| Database Hosting | TBD | Managed PostgreSQL or self-hosted |
| CDN | TBD | For static PWA assets |
| CI/CD | TBD | GitHub Actions, GitLab CI, or similar |
| Monitoring | TBD | Application monitoring, error tracking |

### Third-Party Services

| Service | Provider | Purpose |
|---------|----------|---------|
| Football Stats API | **API-Football** | Player statistics, team data |
| Authentication | **OAuth 2.0 Providers** | Google, Apple, Facebook social login |
| Push Notifications | 🔄 IN PROGRESS | Web Push API or service like Firebase Cloud Messaging |
| Analytics | 🔄 IN PROGRESS | User behavior tracking (privacy-focused) |

---

## Data Models

### Database Schema (PostgreSQL)

#### 1. Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    oauth_provider VARCHAR(50), -- 'google', 'apple', 'facebook', NULL for guest
    oauth_id VARCHAR(255),
    account_type VARCHAR(20) NOT NULL, -- 'guest', 'registered'
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    UNIQUE(oauth_provider, oauth_id)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id);
```

#### 2. Player Profiles Table

```sql
CREATE TABLE player_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_rank_tier VARCHAR(50), -- 'Sunday League', 'Amateur', etc.
    current_rank_subtier VARCHAR(50), -- 'Reserve', 'Rotation', 'Starter', 'Captain'
    league_points INT DEFAULT 0,
    hidden_mmr INT DEFAULT 1000, -- Starting MMR
    total_wins INT DEFAULT 0,
    total_losses INT DEFAULT 0,
    casual_wins INT DEFAULT 0,
    casual_losses INT DEFAULT 0,
    ranked_wins INT DEFAULT 0,
    ranked_losses INT DEFAULT 0,
    current_win_streak INT DEFAULT 0,
    best_win_streak INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profiles_mmr ON player_profiles(hidden_mmr);
CREATE INDEX idx_profiles_rank ON player_profiles(current_rank_tier, current_rank_subtier);
```

#### 3. Friends Table

```sql
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'accepted', 'declined', 'blocked'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    UNIQUE(user_id, friend_id),
    CHECK (user_id != friend_id)
);

CREATE INDEX idx_friendships_user ON friendships(user_id, status);
CREATE INDEX idx_friendships_friend ON friendships(friend_id, status);
```

#### 4. Questions Table

```sql
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_type VARCHAR(50) NOT NULL, -- 'team_league_appearances', 'team_league_goals_apps', etc.
    question_text TEXT NOT NULL,
    question_template VARCHAR(255), -- e.g., "Appearances for {team} in {league}"
    team_name VARCHAR(255),
    team_id INT, -- External API team ID
    league_name VARCHAR(255),
    league_id INT, -- External API league ID
    season VARCHAR(20), -- e.g., '2023-2024'
    country_filter VARCHAR(100), -- For nationality-based questions
    position_filter VARCHAR(50), -- 'goalkeeper', 'defender', etc.
    stat_type VARCHAR(50), -- 'appearances', 'goals', 'apps_goals', 'apps_cleansheets'
    is_active BOOLEAN DEFAULT TRUE,
    difficulty_tier VARCHAR(20), -- 🔄 IN PROGRESS: 'easy', 'medium', 'hard'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_active ON questions(is_active);
CREATE INDEX idx_questions_type ON questions(question_type);
```

#### 5. Answers Table (Cached from API)

```sql
CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    player_name VARCHAR(255) NOT NULL,
    player_api_id INT, -- External API player ID
    score INT NOT NULL, -- The actual stat value (appearances, goals, etc.)
    is_valid_darts_score BOOLEAN NOT NULL, -- Pre-computed: is this a valid darts score?
    is_bust BOOLEAN NOT NULL, -- Pre-computed: score > 180
    usage_count INT DEFAULT 0, -- Track how often this answer is used
    last_used TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(question_id, player_api_id)
);

CREATE INDEX idx_answers_question ON answers(question_id);
CREATE INDEX idx_answers_player_name ON answers USING gin(player_name gin_trgm_ops); -- For fuzzy matching
CREATE INDEX idx_answers_usage ON answers(usage_count DESC); -- For "Human" AI difficulty
```

#### 6. Matches Table

```sql
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_type VARCHAR(20) NOT NULL, -- 'daily_challenge', 'quick_match', 'friend_challenge', 'ai_match'
    match_format VARCHAR(20) NOT NULL, -- 'ranked', 'casual'
    game_format VARCHAR(20) NOT NULL, -- 'best_of_1', 'best_of_3', 'best_of_5'
    is_realtime BOOLEAN DEFAULT TRUE,
    player1_id UUID REFERENCES users(id) ON DELETE SET NULL,
    player2_id UUID REFERENCES users(id) ON DELETE SET NULL,
    winner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'in_progress', 'completed', 'abandoned'
    player1_games_won INT DEFAULT 0,
    player2_games_won INT DEFAULT 0,
    turn_timer_seconds INT DEFAULT 45,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_matches_player1 ON matches(player1_id, status);
CREATE INDEX idx_matches_player2 ON matches(player2_id, status);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_matches_type ON matches(match_type);
```

#### 7. Games Table (Individual games within a match)

```sql
CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    game_number INT NOT NULL, -- 1, 2, 3 (for best-of-X)
    question_id UUID NOT NULL REFERENCES questions(id),
    player1_score INT DEFAULT 501,
    player2_score INT DEFAULT 501,
    current_turn VARCHAR(20), -- 'player1', 'player2'
    turn_number INT DEFAULT 1,
    winner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'in_progress', 'completed'
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(match_id, game_number)
);

CREATE INDEX idx_games_match ON games(match_id);
CREATE INDEX idx_games_question ON games(question_id);
```

#### 8. Game Moves Table (Turn-by-turn history)

```sql
CREATE TABLE game_moves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    turn_number INT NOT NULL,
    answer_id UUID REFERENCES answers(id), -- The answer they submitted
    player_name_submitted VARCHAR(255), -- Raw input from player
    score_deducted INT, -- Points deducted (0 if bust/invalid)
    is_valid BOOLEAN NOT NULL,
    is_bust BOOLEAN DEFAULT FALSE,
    is_timeout BOOLEAN DEFAULT FALSE,
    time_taken_ms INT, -- Time to submit answer
    player_score_after INT, -- Player's score after this move
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(game_id, turn_number)
);

CREATE INDEX idx_moves_game ON game_moves(game_id);
CREATE INDEX idx_moves_player ON game_moves(player_id);
```

#### 9. Daily Challenges Table

```sql
CREATE TABLE daily_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_date DATE UNIQUE NOT NULL,
    question_id UUID NOT NULL REFERENCES questions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_challenges_date ON daily_challenges(challenge_date DESC);
```

#### 10. Daily Challenge Results Table

```sql
CREATE TABLE daily_challenge_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_challenge_id UUID NOT NULL REFERENCES daily_challenges(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_id UUID REFERENCES games(id) ON DELETE SET NULL, -- Link to game record
    final_score INT, -- Final score (could be negative)
    checked_out BOOLEAN, -- TRUE if reached -10 to 0
    total_turns INT,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(daily_challenge_id, user_id)
);

CREATE INDEX idx_dc_results_challenge ON daily_challenge_results(daily_challenge_id);
CREATE INDEX idx_dc_results_user ON daily_challenge_results(user_id);
CREATE INDEX idx_dc_results_leaderboard ON daily_challenge_results(daily_challenge_id, checked_out DESC, total_turns ASC, final_score DESC);
```

#### 11. Matchmaking Queue Table

```sql
CREATE TABLE matchmaking_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    queue_type VARCHAR(20) NOT NULL, -- 'casual', 'ranked'
    game_format VARCHAR(20) NOT NULL, -- 'best_of_1', 'best_of_3', 'best_of_5'
    mmr_range_min INT,
    mmr_range_max INT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_queue_type ON matchmaking_queue(queue_type, mmr_range_min, mmr_range_max);
CREATE INDEX idx_queue_user ON matchmaking_queue(user_id);
```

---

## API Design

### REST API Endpoints

**Base URL**: `https://api.football501.com/v1` (🔄 IN PROGRESS - domain TBD)

#### Authentication Endpoints

```
POST   /auth/guest                    # Create guest account
POST   /auth/login/google             # OAuth login with Google
POST   /auth/login/apple              # OAuth login with Apple
POST   /auth/login/facebook           # OAuth login with Facebook
POST   /auth/logout                   # Logout current user
POST   /auth/upgrade                  # Upgrade guest to registered account
GET    /auth/me                       # Get current user info
```

#### User Endpoints

```
GET    /users/{username}              # Get public profile
GET    /users/me/profile              # Get own profile
PATCH  /users/me/profile              # Update own profile
GET    /users/me/stats                # Get own statistics
```

#### Friends Endpoints

```
GET    /friends                       # List friends
POST   /friends/request               # Send friend request
POST   /friends/{id}/accept           # Accept friend request
POST   /friends/{id}/decline          # Decline friend request
DELETE /friends/{id}                  # Remove friend
GET    /friends/requests              # List pending requests
```

#### Matchmaking Endpoints

```
POST   /matchmaking/queue             # Join matchmaking queue
DELETE /matchmaking/queue             # Leave matchmaking queue
GET    /matchmaking/status            # Check matchmaking status
```

#### Match Endpoints

```
POST   /matches/challenge             # Challenge a friend
POST   /matches/{id}/accept           # Accept friend challenge
POST   /matches/{id}/decline          # Decline friend challenge
GET    /matches/{id}                  # Get match details
GET    /matches/active                # List active matches
GET    /matches/history               # Match history (paginated)
```

#### Game Endpoints (within match)

```
GET    /games/{id}                    # Get game state
POST   /games/{id}/submit-answer      # Submit answer (also via WebSocket)
POST   /games/{id}/request-refresh    # Request question refresh
POST   /games/{id}/vote-refresh       # Vote on question refresh
POST   /games/{id}/forfeit            # Forfeit current game
```

#### Daily Challenge Endpoints

```
GET    /daily-challenge               # Get today's challenge
GET    /daily-challenge/leaderboard   # Global leaderboard (paginated)
POST   /daily-challenge/start         # Start daily challenge
GET    /daily-challenge/history       # User's past daily challenges
```

#### AI Match Endpoints

```
POST   /ai-match/start                # Start AI match (difficulty: easy/medium/hard)
GET    /ai-match/{id}                 # Get AI match state
POST   /ai-match/{id}/submit-answer   # Submit answer in AI match
```

#### Admin Endpoints (🔄 IN PROGRESS)

```
POST   /admin/questions               # Create new question
PATCH  /admin/questions/{id}          # Update question
DELETE /admin/questions/{id}          # Deactivate question
POST   /admin/questions/{id}/populate # Trigger API population
GET    /admin/stats                   # System statistics
```

### WebSocket Protocol

**Endpoint**: `wss://api.football501.com/ws`

#### Connection

```javascript
// Client connects with auth token
const ws = new WebSocket('wss://api.football501.com/ws?token={JWT_TOKEN}');
```

#### Message Format

All messages use JSON format:

```json
{
  "type": "MESSAGE_TYPE",
  "gameId": "uuid",
  "payload": { /* message-specific data */ }
}
```

#### Client → Server Messages

```javascript
// Submit answer
{
  "type": "SUBMIT_ANSWER",
  "gameId": "game-uuid",
  "payload": {
    "playerName": "Sergio Aguero"
  }
}

// Request question refresh
{
  "type": "REQUEST_REFRESH",
  "gameId": "game-uuid"
}

// Vote on refresh
{
  "type": "VOTE_REFRESH",
  "gameId": "game-uuid",
  "payload": {
    "accept": true
  }
}

// Heartbeat (keep-alive)
{
  "type": "PING"
}
```

#### Server → Client Messages

```javascript
// Game state update
{
  "type": "GAME_STATE",
  "gameId": "game-uuid",
  "payload": {
    "player1Score": 350,
    "player2Score": 421,
    "currentTurn": "player1",
    "turnNumber": 5,
    "usedAnswers": [/* list of used answers */],
    "turnTimeRemaining": 42
  }
}

// Answer result
{
  "type": "ANSWER_RESULT",
  "gameId": "game-uuid",
  "payload": {
    "valid": true,
    "playerName": "Sergio Aguero",
    "score": 184,
    "isBust": false,
    "newScore": 317
  }
}

// Turn timeout
{
  "type": "TURN_TIMEOUT",
  "gameId": "game-uuid",
  "payload": {
    "playerId": "user-uuid",
    "consecutiveTimeouts": 2,
    "newTimerSeconds": 30
  }
}

// Question refresh vote
{
  "type": "REFRESH_VOTE_REQUESTED",
  "gameId": "game-uuid",
  "payload": {
    "requestedBy": "player1"
  }
}

// Question refreshed
{
  "type": "QUESTION_REFRESHED",
  "gameId": "game-uuid",
  "payload": {
    "newQuestion": {/* question object */}
  }
}

// Game over
{
  "type": "GAME_OVER",
  "gameId": "game-uuid",
  "payload": {
    "winnerId": "user-uuid",
    "finalScores": {
      "player1": -5,
      "player2": 12
    }
  }
}

// Match over
{
  "type": "MATCH_OVER",
  "matchId": "match-uuid",
  "payload": {
    "winnerId": "user-uuid",
    "gamesWon": {
      "player1": 2,
      "player2": 1
    }
  }
}

// Opponent disconnected
{
  "type": "OPPONENT_DISCONNECTED",
  "gameId": "game-uuid",
  "payload": {
    "playerId": "user-uuid",
    "gracePeriodSeconds": 30
  }
}

// Error
{
  "type": "ERROR",
  "payload": {
    "code": "INVALID_ANSWER",
    "message": "Player not found for this question"
  }
}

// Pong (heartbeat response)
{
  "type": "PONG"
}
```

---

## Real-Time Communication

### WebSocket Architecture

1. **Connection Management**
   - User connects with JWT token
   - Server validates token and associates connection with user ID
   - Server subscribes user to relevant game channels
   - Heartbeat ping/pong every 30 seconds

2. **Game Channels**
   - Each active game has a channel: `/game/{gameId}`
   - Players subscribed to their active game channels
   - Server broadcasts to all subscribers on game state changes

3. **Reconnection Handling**
   - Client automatically attempts reconnection on disconnect
   - Server maintains game state for 30 seconds grace period
   - On reconnect, server sends full game state to sync client

4. **Concurrency Control**
   - Server uses optimistic locking on game state updates
   - Turn submissions queued and processed sequentially
   - Prevent double-submission with client-side disable

---

## Security

### Authentication & Authorization

#### OAuth 2.0 Social Login
- Google, Apple, Facebook providers
- JWT token issued on successful login
- Token expiration: 24 hours (refresh tokens: 30 days) 🔄 IN PROGRESS
- Token stored in HTTPOnly cookie + localStorage (🔄 decide approach)

#### Guest Accounts
- Ephemeral JWT issued without OAuth
- Limited permissions (no friends, no ranked)
- Expire after 24 hours of inactivity

#### Role-Based Access Control (RBAC)
- Roles: `GUEST`, `USER`, `PREMIUM`, `ADMIN`
- Spring Security annotations for endpoint protection

### Data Security

#### API Security
- HTTPS only in production
- CORS configuration (whitelist client domain)
- Rate limiting:
  - Authenticated: 100 req/min per user
  - Unauthenticated: 10 req/min per IP
- Input validation on all endpoints
- SQL injection prevention (parameterized queries via JPA)

#### WebSocket Security
- JWT token required for connection
- Message origin validation
- Rate limiting on message frequency
- Game state validation server-side (prevent cheating)

#### Data Privacy
- Passwords never stored (OAuth only)
- User data encrypted at rest (database encryption) 🔄 IN PROGRESS
- PII minimization (collect only necessary data)
- GDPR compliance considerations 🔄 IN PROGRESS

---

## Deployment

### Development Environment

```
Frontend: http://localhost:5173 (Vite dev server)
Backend:  http://localhost:8080 (Spring Boot)
Database: localhost:5432 (PostgreSQL Docker container)
```

### Production Environment (🔄 IN PROGRESS)

**Option 1: Traditional Hosting**
- Frontend: Static hosting (Vercel, Netlify, Cloudflare Pages)
- Backend: VPS or container (DigitalOcean, AWS EC2, GCP Compute)
- Database: Managed PostgreSQL (AWS RDS, DigitalOcean Managed DB)

**Option 2: Platform-as-a-Service**
- All-in-one: Railway, Render, Fly.io
- Simplified deployment, auto-scaling

**Option 3: Kubernetes**
- Over-engineered for MVP but good for portfolio showcase
- Docker containers, Helm charts, CI/CD pipeline

### CI/CD Pipeline (🔄 IN PROGRESS)

```
1. Code push to GitHub
2. Automated tests run (unit + integration)
3. Build Docker images (frontend + backend)
4. Deploy to staging environment
5. Run E2E tests
6. Manual approval gate
7. Deploy to production
8. Health checks
```

---

## Performance

### Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| API Response Time (p95) | < 200ms | Server-side logging |
| WebSocket Message Latency | < 100ms | Client timestamp diff |
| PWA Load Time (3G) | < 3s | Lighthouse CI |
| Database Query Time | < 50ms | Query logging |
| Matchmaking Time | < 10s | Analytics |

### Optimization Strategies

#### Frontend
- Code splitting (lazy load routes)
- Image optimization (WebP format)
- Minification and tree-shaking
- Service Worker caching
- Debounce user inputs

#### Backend
- Database indexing (see schema indexes)
- Query optimization (N+1 prevention)
- Connection pooling (HikariCP)
- Caching with Redis (🔄 IN PROGRESS)
  - Session data
  - Leaderboards
  - Active game states
  - Question/answer data
- Async processing for non-critical tasks

#### Database
- Read replicas for scaling (🔄 future)
- Partitioning for large tables (game_moves, matches)
- Regular VACUUM and ANALYZE
- Query plan monitoring

---

## Testing Strategy

### Frontend Testing

```
Unit Tests (Vitest):
- Svelte component logic
- Store behavior
- Utility functions

Integration Tests:
- API client mocks
- WebSocket client mocks
- User flows

E2E Tests (Playwright): 🔄 IN PROGRESS
- Critical user journeys
- Match gameplay flow
- Daily challenge completion
```

### Backend Testing

```
Unit Tests (JUnit 5):
- Service layer logic
- Game engine rules (scoring, validation)
- Utility functions

Integration Tests (Spring Boot Test):
- Repository layer (TestContainers for PostgreSQL)
- REST API endpoints (MockMvc)
- WebSocket handlers

E2E Tests:
- Full match flow (API + WebSocket)
- Authentication flow
```

### Testing Coverage Target

- Unit Tests: > 80% coverage
- Integration Tests: Critical paths covered
- E2E Tests: Top 5 user journeys

---

## Development Roadmap

### Phase 1: MVP Core (🔄 Current Phase)
- [ ] Project setup (frontend + backend)
- [ ] Database schema implementation
- [ ] User authentication (OAuth + guest)
- [ ] Basic REST API
- [ ] Game engine (scoring rules, validation)
- [ ] Question/answer seeding (20-30 questions)
- [ ] Single-player daily challenge
- [ ] Basic UI (responsive)

### Phase 2: Multiplayer Foundation
- [ ] WebSocket infrastructure
- [ ] Matchmaking system (quick match)
- [ ] Real-time game flow
- [ ] Friend system
- [ ] Friend challenges
- [ ] AI opponent (basic difficulty)

### Phase 3: Competitive Features
- [ ] Ranking system (MMR + League Points)
- [ ] Ranked matchmaking
- [ ] Leaderboards
- [ ] Player profiles
- [ ] Match history

### Phase 4: Polish & Launch Prep
- [ ] PWA features (installable, offline)
- [ ] Push notifications
- [ ] Premium subscription flow
- [ ] Analytics integration
- [ ] Performance optimization
- [ ] Security audit
- [ ] Load testing

### Phase 5: Post-Launch
- [ ] Admin panel for question management
- [ ] "Human" difficulty AI
- [ ] Achievements system
- [ ] Social sharing
- [ ] Mobile app optimization

---

## Open Questions & Decisions Needed

### Infrastructure
- [ ] Choose cloud provider (AWS, GCP, DigitalOcean, Railway)
- [ ] Decide on Redis caching strategy (when to implement)
- [ ] Choose CI/CD platform (GitHub Actions, GitLab CI)
- [ ] Decide on monitoring/logging solution (Sentry, Datadog, etc.)

### Features
- [ ] Define exact MMR algorithm and League Points calculation
- [ ] Define premium subscription pricing
- [ ] Define AI difficulty algorithms (Easy, Medium, Hard)
- [ ] Define question difficulty scoring system
- [ ] Define answer popularity tracking logic

### Legal & Compliance
- [ ] Terms of Service
- [ ] Privacy Policy
- [ ] GDPR compliance checklist
- [ ] Cookie consent (if using analytics cookies)

---

## Appendix

### Tech Stack Alternatives Considered

| Component | Chosen | Alternatives Considered | Reason for Choice |
|-----------|--------|------------------------|-------------------|
| Frontend Framework | **SvelteKit** | React, Vue, Angular | Simpler syntax, better performance, easier for backend dev |
| Backend Framework | **Spring Boot** | Node.js (Express), Go, Python (FastAPI) | Developer's existing expertise |
| Database | **PostgreSQL** | MySQL, MongoDB | ACID, full-text search, JSON support |
| Real-time | **WebSockets** | Server-Sent Events, Long Polling | Bi-directional, low latency |
| Styling | **Tailwind CSS** | Bootstrap, Material UI, Styled Components | Utility-first, customizable, mobile-first |

### Useful Resources

- [SvelteKit Documentation](https://kit.svelte.dev/docs)
- [Spring Boot WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [API-Football Documentation](https://www.api-football.com/documentation-v3)
- [PWA Best Practices](https://web.dev/progressive-web-apps/)
- [PostgreSQL Full-Text Search](https://www.postgresql.org/docs/current/textsearch.html)

---

**Document Status**: Draft
**Next Steps**: Review with stakeholders, finalize tech decisions, begin Phase 1 implementation
