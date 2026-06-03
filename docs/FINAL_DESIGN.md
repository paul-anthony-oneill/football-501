# Final Design - The Complete Vision

## What Trivia 501 Will Become

This document explains the **complete system** - everything we're building, not just what exists now.

---

## The Complete User Journey

### New User Experience

```
1. Visit website → Instant guest play (no login required!)
   ↓
2. Play practice games to learn
   ↓
3. Try daily challenge (compare score with others)
   ↓
4. "This is fun! I want to keep my stats"
   ↓
5. Sign up with Google/Apple/Facebook
   ↓
6. Account created → Stats preserved → Ranked play unlocked
   ↓
7. Play ranked matches → Climb the tiers
   ↓
8. Reach "Icon" tier → Become a legend!
```

---

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (PWA)                       │
│              SvelteKit + TypeScript                     │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Practice  │  │ Daily        │  │  Ranked      │ │
│  │   Mode      │  │ Challenge    │  │  Match       │ │
│  └─────────────┘  └──────────────┘  └──────────────┘ │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │         State Management (Svelte Stores)        │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
                ┌─────────┴─────────┐
                │                   │
         HTTP/REST              WebSocket (STOMP)
                │                   │
┌───────────────┴───────────────────┴───────────────────┐
│              BACKEND (Spring Boot)                    │
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │             API Layer (Controllers)              ││
│  │  ┌─────────┐ ┌───────────┐ ┌─────────────────┐ ││
│  │  │Practice │ │Matchmaking│ │Daily Challenge  │ ││
│  │  │  REST   │ │   REST    │ │     REST        │ ││
│  │  └─────────┘ └───────────┘ └─────────────────┘ ││
│  │  ┌─────────────────────────────────────────────┐││
│  │  │      WebSocket Handler (Real-time)          │││
│  │  └─────────────────────────────────────────────┘││
│  └──────────────────────────────────────────────────┘│
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │            Service Layer (Business Logic)        ││
│  │  ┌────────┐ ┌──────────┐ ┌───────────────────┐ ││
│  │  │  Game  │ │  Match   │ │ Matchmaking       │ ││
│  │  │Service │ │ Service  │ │   Service         │ ││
│  │  └────────┘ └──────────┘ └───────────────────┘ ││
│  │  ┌────────┐ ┌──────────┐ ┌───────────────────┐ ││
│  │  │Question│ │  User    │ │    Ranking        │ ││
│  │  │Service │ │ Service  │ │    Service        │ ││
│  │  └────────┘ └──────────┘ └───────────────────┘ ││
│  └──────────────────────────────────────────────────┘│
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │           Game Engine (Core Rules)               ││
│  │  ┌────────────┐ ┌────────────┐ ┌─────────────┐ ││
│  │  │  Answer    │ │  Scoring   │ │   Darts     │ ││
│  │  │ Evaluator  │ │  Service   │ │ Validator   │ ││
│  │  └────────────┘ └────────────┘ └─────────────┘ ││
│  └──────────────────────────────────────────────────┘│
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │           Integration Layer                      ││
│  │  ┌──────────────────────────────────────────┐   ││
│  │  │  API-Football Client (Batch Jobs)        │   ││
│  │  │  - Question Population                    │   ││
│  │  │  - Weekly Stats Refresh                   │   ││
│  │  └──────────────────────────────────────────┘   ││
│  └──────────────────────────────────────────────────┘│
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │           Scheduler Module                       ││
│  │  ┌──────────────┐  ┌──────────────────────────┐││
│  │  │Daily         │  │Stats Refresh (Weekly)    │││
│  │  │Challenge Gen │  │3 AM UTC Sunday           │││
│  │  │(Midnight UTC)│  │                          │││
│  │  └──────────────┘  └──────────────────────────┘││
│  └──────────────────────────────────────────────────┘│
│                                                        │
│  ┌──────────────────────────────────────────────────┐│
│  │        Auth Module (OAuth 2.0)                   ││
│  │  ┌──────┐  ┌──────┐  ┌────────┐  ┌──────────┐ ││
│  │  │Google│  │Apple │  │Facebook│  │Guest Acct│ ││
│  │  └──────┘  └──────┘  └────────┘  └──────────┘ ││
│  └──────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
   PostgreSQL                            Redis
   (Primary DB)                     (Cache + Sessions)
        │                                   │
   ┌────┴────┐                         ┌───┴────┐
   │Questions│                         │Session │
   │Answers  │                         │Cache   │
   │Users    │                         │Match   │
   │Matches  │                         │State   │
   │Rankings │                         └────────┘
   └─────────┘
```

---

## Game Modes (Complete)

### 1. Practice Mode (✅ Implemented)

**What it is:**
- Single-player mode
- No time pressure (but timer exists)
- Learn the game mechanics
- Try different strategies

**Flow:**
```
Start → Get Question → Submit Answers → Win/Lose → Play Again
```

**Example:**
```
Question: "Appearances for Man City in Premier League 2022/23"

Your score: 501
You type: "Haaland"
Result: ✓ Erling Haaland scored 52 appearances
New score: 449

You type: "De Bruyne"
Result: ✓ Kevin De Bruyne scored 32 appearances
New score: 417

... continue until you reach 0!
```

---

### 2. Daily Challenge (🔜 To Implement)

**What it is:**
- Everyone gets the SAME question each day
- Submit your best attempt
- Compete on global leaderboard
- One attempt per day (or 3 attempts for premium)

**Flow:**
```
1. Midnight UTC: New challenge generated
   ↓
2. Player visits site: "Today's Challenge: Premier League appearances"
   ↓
3. Player attempts challenge
   ↓
4. Final score submitted to leaderboard
   ↓
5. Rankings updated (turns taken + final score)
```

**Leaderboard Example:**
```
🏆 Today's Challenge: Man City PL 2022/23 Appearances

Rank  Player          Turns  Final Score  Time
────────────────────────────────────────────────
1.    FootballPro      12       -5        12:34
2.    Messi_Fan        15       -2        15:02
3.    GoalKing         18        0        18:45
4.    You              21        5        21:30
...
```

**Scoring:**
- Lower turns = better
- Closer to 0 = better
- Ties broken by timestamp (faster submission wins)

**Database Schema:**
```sql
-- Daily challenge question (one per day)
daily_challenges:
  id, question_id, date, expires_at

-- User attempts
daily_challenge_results:
  id, challenge_id, user_id, turns_taken, final_score, submitted_at
```

---

### 3. Ranked Multiplayer (🔜 To Implement)

**What it is:**
- Real-time 1v1 competition
- Both players see same question
- Alternate turns (Player 1 → Player 2 → Player 1...)
- MMR-based matchmaking
- Climb 9 tiers × 4 subtiers = 36 ranks

**Flow:**
```
1. Click "Play Ranked"
   ↓
2. Enter matchmaking queue
   ↓
3. Server finds opponent with similar MMR
   ↓
4. Match starts (best-of-1, best-of-3, or best-of-5)
   ↓
5. Players alternate turns
   ↓
6. First to reach 0 wins
   ↓
7. MMR updated based on result
   ↓
8. League points added/subtracted
   ↓
9. Rank may increase/decrease
```

**Turn Structure:**
```
Player 1 (Score: 501)
↓ Types "Haaland" → 449
↓
Player 2 (Score: 501)
↓ Types "Aguero" → 478
↓
Player 1 (Score: 449)
↓ Types "Foden" → 414
↓
... continues until someone wins
```

**Close Finish Rule:**
```
Player 1: 15 → types "De Bruyne" (25) → reaches -10 ✓
Server: "Player 1 checked out! Player 2 gets final turn"

Player 2: 30 → types "Walker" (28) → reaches 2
Server: "Player 1 wins! (-10 is closer to 0 than 2)"
```

**Matchmaking System:**

```
┌─────────────────────────────────────────────┐
│         Matchmaking Algorithm               │
└─────────────────────────────────────────────┘

1. Player clicks "Find Match"
2. Server adds to queue with metadata:
   - User ID
   - Current MMR (hidden)
   - Search range: ±50 MMR initially
   - Max wait time: 30 seconds

3. Every 2 seconds:
   - Search for opponents in range
   - Expand range: ±50 → ±100 → ±200
   - After 30s: Match anyone

4. Match found:
   - Remove both from queue
   - Create match entity
   - Notify both via WebSocket
   - Start game
```

**Database Schema:**
```sql
-- Queue entries
matchmaking_queue:
  id, user_id, mmr, joined_at, search_range

-- Match results
matches:
  id, player1_id, player2_id, winner_id,
  player1_mmr_before, player1_mmr_after,
  player2_mmr_before, player2_mmr_after,
  format (BEST_OF_1, BEST_OF_3, BEST_OF_5)
```

---

### 4. Casual Multiplayer (🔜 To Implement)

**What it is:**
- Same as ranked, but MMR not affected
- Play with friends (private lobby code)
- No matchmaking - direct invite
- Fun mode for practice

**Flow:**
```
1. Create lobby → Get code "ABCD1234"
2. Share code with friend
3. Friend joins lobby
4. Host starts game
5. Play without MMR changes
```

---

## Ranking System (Complete Design)

### Structure

**9 Tiers × 4 Subtiers = 36 Total Ranks**

```
┌──────────────────────────────────────────────┐
│               ICON TIER                      │
│  🏆 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│           WORLD CLASS TIER                   │
│  🌟 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│          CONTINENTAL TIER                    │
│  🌍 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│         INTERNATIONAL TIER                   │
│  ⚽ Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│              PRO TIER                        │
│  💼 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│          JOURNEYMAN TIER                     │
│  🚶 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│           SEMI-PRO TIER                      │
│  📈 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│            AMATEUR TIER                      │
│  🎯 Captain → Starter → Rotation → Reserve  │
├──────────────────────────────────────────────┤
│         SUNDAY LEAGUE TIER                   │
│  ⚽ Captain → Starter → Rotation → Reserve  │
│             (Starting rank)                  │
└──────────────────────────────────────────────┘
```

### How It Works

**Two Rating Systems:**

1. **Hidden MMR** (Elo-based)
   - Used for matchmaking
   - Never shown to player
   - Prevents gaming the system

2. **Visible League Points**
   - Shown to player
   - Determines displayed rank
   - Based on wins/losses

**Win/Loss Calculation:**

```
Win against higher MMR:    +30 to +50 LP
Win against equal MMR:     +20 to +30 LP
Win against lower MMR:     +10 to +20 LP

Loss against higher MMR:   -10 to -15 LP
Loss against equal MMR:    -20 to -25 LP
Loss against lower MMR:    -30 to -40 LP
```

**Rank Progression:**

```
Sunday League Reserve:      0 - 99 LP
Sunday League Rotation:     100 - 199 LP
Sunday League Starter:      200 - 299 LP
Sunday League Captain:      300 - 399 LP

Amateur Reserve:            400 - 499 LP
... and so on
```

**Database Schema:**
```sql
player_profiles:
  id, user_id,
  mmr (hidden),
  league_points (visible),
  current_tier,
  current_subtier,
  rank_history (JSON: [{ date, tier, subtier }]),
  wins, losses, draws,
  highest_tier_achieved,
  created_at, updated_at
```

---

## Real-Time Multiplayer (WebSocket)

### Why WebSocket?

**Problem with HTTP:**
- Player 1 makes move → Server updates
- Player 2 needs to know → Must poll server every second
- Inefficient! Lots of wasted requests

**Solution: WebSocket**
- Persistent connection
- Server pushes updates instantly
- Both players see changes in real-time

### WebSocket Protocol

**Connection:**
```
Frontend → wss://api.trivia501.com/ws?token={JWT_TOKEN}
```

**Authentication:**
```javascript
// On connect
const token = localStorage.getItem('auth_token');
const ws = new WebSocket(`wss://api.trivia501.com/ws?token=${token}`);
```

**Message Format (STOMP Protocol):**
```json
{
  "type": "MESSAGE_TYPE",
  "gameId": "uuid-here",
  "timestamp": "2024-01-28T12:00:00Z",
  "payload": {
    // Message-specific data
  }
}
```

### Message Types

#### Client → Server

**1. SUBMIT_ANSWER**
```json
{
  "type": "SUBMIT_ANSWER",
  "gameId": "abc-123",
  "payload": {
    "answer": "Haaland"
  }
}
```

**2. REQUEST_REFRESH**
```json
{
  "type": "REQUEST_REFRESH",
  "gameId": "abc-123",
  "payload": {
    "reason": "Too hard"
  }
}
```

**3. VOTE_REFRESH**
```json
{
  "type": "VOTE_REFRESH",
  "gameId": "abc-123",
  "payload": {
    "vote": "yes"  // or "no"
  }
}
```

**4. HEARTBEAT**
```json
{
  "type": "PING",
  "timestamp": "2024-01-28T12:00:00Z"
}
```

#### Server → Client

**1. GAME_STATE**
```json
{
  "type": "GAME_STATE",
  "gameId": "abc-123",
  "payload": {
    "player1Score": 450,
    "player2Score": 478,
    "currentTurnPlayerId": "player-1-uuid",
    "turnTimerSeconds": 45,
    "turnCount": 5,
    "status": "IN_PROGRESS"
  }
}
```

**2. ANSWER_RESULT**
```json
{
  "type": "ANSWER_RESULT",
  "gameId": "abc-123",
  "payload": {
    "playerId": "player-1-uuid",
    "result": "VALID",
    "submittedAnswer": "Haaland",
    "matchedAnswer": "Erling Haaland",
    "scoreValue": 52,
    "scoreBefore": 501,
    "scoreAfter": 449
  }
}
```

**3. TURN_TIMEOUT**
```json
{
  "type": "TURN_TIMEOUT",
  "gameId": "abc-123",
  "payload": {
    "playerId": "player-2-uuid",
    "consecutiveTimeouts": 2,
    "newTimerSeconds": 30
  }
}
```

**4. GAME_OVER**
```json
{
  "type": "GAME_OVER",
  "gameId": "abc-123",
  "payload": {
    "winnerId": "player-1-uuid",
    "finalScores": {
      "player1": -5,
      "player2": 15
    },
    "turnCount": 12,
    "mmrChanges": {
      "player1": +25,
      "player2": -20
    }
  }
}
```

**5. OPPONENT_DISCONNECTED**
```json
{
  "type": "OPPONENT_DISCONNECTED",
  "gameId": "abc-123",
  "payload": {
    "gracePeriodSeconds": 30
  }
}
```

### Reconnection Handling

**Problem:** Player loses internet → disconnected

**Solution:**
```
1. Player disconnects
   ↓
2. Server holds game state for 30 seconds (grace period)
   ↓
3. If player reconnects within 30s:
   - Full game state sync sent
   - Game continues
   ↓
4. If no reconnect after 30s:
   - Opponent wins by forfeit
   - Match ended
```

**Reconnection Flow:**
```javascript
// Frontend detects disconnect
ws.onclose = () => {
    console.log('Disconnected! Attempting reconnect...');

    // Try to reconnect
    setTimeout(() => {
        const newWs = new WebSocket(`wss://...?token=${token}`);

        // On reconnect, request full game state
        newWs.send(JSON.stringify({
            type: 'REQUEST_SYNC',
            gameId: currentGameId
        }));
    }, 1000);
};

// Server responds with full state
{
  "type": "GAME_STATE_SYNC",
  "payload": {
    // Full current game state
    "player1Score": 450,
    "player2Score": 478,
    "currentTurn": "player-1",
    "moveHistory": [ ... ]
  }
}
```

---

## Authentication System

### Guest Accounts (✅ Current Mode)

**How it works:**
- Visit site → Auto-generate UUID → Play instantly
- Stats stored in localStorage
- No login required
- Expires after 24 hours of inactivity

**Limitations:**
- No cross-device sync
- Can't play ranked matches
- Limited to practice mode

### Registered Accounts (🔜 To Implement)

**OAuth 2.0 Social Login:**
```
┌─────────────────┐
│   Choose Login  │
│                 │
│  [Google]       │
│  [Apple]        │
│  [Facebook]     │
└─────────────────┘
        ↓
┌─────────────────┐
│ OAuth Provider  │
│ (Google/etc)    │
│                 │
│ "Allow access?" │
│  [Yes] [No]     │
└─────────────────┘
        ↓
┌─────────────────┐
│ Redirect back   │
│ with auth code  │
└─────────────────┘
        ↓
┌─────────────────┐
│  Server exchanges│
│  code for token │
│                 │
│  Creates user   │
│  Returns JWT    │
└─────────────────┘
        ↓
┌─────────────────┐
│   Logged In!    │
│                 │
│  JWT stored in  │
│  HTTPOnly cookie│
└─────────────────┘
```

**Token Management:**
```
Access Token (JWT):
  - Expires: 24 hours
  - Stored in: HTTPOnly cookie (secure)
  - Contains: { userId, email, tier, mmr }

Refresh Token:
  - Expires: 30 days
  - Stored in: HTTPOnly cookie
  - Used to get new access token
```

**Database Schema:**
```sql
users:
  id, email, display_name, avatar_url,
  provider (google/apple/facebook),
  provider_id,
  account_type (guest/registered/premium),
  created_at, last_active_at

sessions:
  id, user_id, refresh_token_hash,
  expires_at, created_at
```

---

## Question Population (API-Football Integration)

### Critical Rules

⚠️ **NEVER call API during gameplay!**
- Free tier: 100 requests/day
- Match validation uses cached database
- API only for batch population

### Question Population Flow

```
┌──────────────────────────────────────────┐
│     Admin Creates Question Template      │
│  "Appearances for {team} in {league}"    │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│        Scheduled Job Triggers            │
│         (Daily at 2 AM UTC)              │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│    API-Football Client Fetches Data      │
│  GET /v3/players?team=33&league=39       │
│         &season=2023                     │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│        Response Mapping                  │
│  player.name → displayText               │
│  statistics[0].games.appearances → score │
│  DartsValidator checks score → isValidDarts│
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│     Bulk Insert to Answers Table         │
│  For each player:                        │
│    - answerKey: "erling haaland"         │
│    - displayText: "Erling Haaland"       │
│    - score: 52                           │
│    - isValidDarts: true                  │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│      Question Marked as Active           │
│   Now available for gameplay!            │
└──────────────────────────────────────────┘
```

### Rate Limiting Strategy

**Free Tier Limits:**
- 100 requests per day
- Can't exceed or we get blocked

**Solution:**
```
1. Batch requests efficiently
   - Get all players for one team in one request
   - Get all seasons at once if possible

2. Spread throughout day
   - 2 AM: Populate new questions (50 requests)
   - 8 AM: Update stats for active questions (30 requests)
   - 2 PM: Backup data refresh (20 requests)

3. Cache everything
   - Store responses in database
   - Only refresh once per week

4. Prioritize active leagues
   - Premier League, La Liga, etc. first
   - Lower leagues less frequently
```

### Weekly Stats Refresh

```
Every Sunday at 3 AM UTC:

1. Identify questions using current season data
2. Fetch updated stats from API-Football
3. Update existing answers with new scores
4. Mark outdated answers as inactive
5. Log API usage for monitoring
```

### Database Schema
```sql
questions:
  id, category_id, question_text,
  metric_key (appearances/goals/etc),
  api_config (JSON: team_id, league_id, season),
  is_active, last_refreshed_at

api_usage_log:
  id, endpoint, params, response_code,
  usage_count, timestamp
```

---

## Scheduled Jobs

### Daily Challenge Generation

**Schedule:** Every day at midnight UTC

```java
@Scheduled(cron = "0 0 0 * * *", zone = "UTC")
public void generateDailyChallenge() {
    // 1. Pick random active question
    Question question = questionService.getRandomActiveQuestion();

    // 2. Create daily challenge
    DailyChallenge challenge = DailyChallenge.builder()
        .questionId(question.getId())
        .date(LocalDate.now())
        .expiresAt(LocalDateTime.now().plusDays(1))
        .build();

    dailyChallengeRepository.save(challenge);

    // 3. Clear previous day's results (keep for 30 days)
    dailyChallengeRepository.deleteOlderThan(
        LocalDate.now().minusDays(30)
    );

    log.info("Daily challenge generated: {}", question.getQuestionText());
}
```

### Stats Refresh Job

**Schedule:** Every Sunday at 3 AM UTC

```java
@Scheduled(cron = "0 0 3 * * SUN", zone = "UTC")
public void refreshPlayerStats() {
    // 1. Get all questions needing refresh
    List<Question> questions = questionRepository
        .findByNeedsRefresh(true);

    // 2. For each question, fetch updated stats
    for (Question question : questions) {
        try {
            // Call API-Football
            List<PlayerStats> stats = apiFootballClient
                .getPlayerStats(question.getApiConfig());

            // Update answers table
            for (PlayerStats stat : stats) {
                answerRepository.updateOrCreate(
                    question.getId(),
                    stat.getPlayerName(),
                    stat.getAppearances()
                );
            }

            question.setLastRefreshedAt(LocalDateTime.now());
            questionRepository.save(question);

            // Rate limit: wait 1 second between requests
            Thread.sleep(1000);

        } catch (ApiRateLimitException e) {
            log.error("Rate limit hit! Stopping refresh.");
            break;
        }
    }
}
```

### Inactive User Cleanup

**Schedule:** Daily at 4 AM UTC

```java
@Scheduled(cron = "0 0 4 * * *", zone = "UTC")
public void cleanupInactiveUsers() {
    // Delete guest accounts inactive for 24 hours
    userRepository.deleteGuestAccountsInactiveFor(Duration.ofHours(24));

    // Mark registered accounts inactive after 90 days
    userRepository.markInactive(Duration.ofDays(90));
}
```

---

## Performance & Caching

### Redis Caching Strategy

**What to cache:**
```
1. Active questions (1 hour TTL)
   Key: "question:{id}"
   Value: Question JSON

2. User sessions (24 hour TTL)
   Key: "session:{token}"
   Value: User data

3. Daily challenge (24 hour TTL)
   Key: "daily_challenge:{date}"
   Value: Challenge JSON

4. Matchmaking queue (real-time)
   Key: "matchmaking_queue"
   Value: List of waiting players

5. Active matches (game duration)
   Key: "match:{id}"
   Value: Current game state
```

**Cache Invalidation:**
```
- Question updated → Invalidate "question:{id}"
- Stats refreshed → Invalidate all question cache
- User logs out → Invalidate "session:{token}"
- Match ends → Delete "match:{id}"
```

### Database Optimization

**Critical Indexes:**
```sql
-- Answer lookups (most frequent operation)
CREATE INDEX idx_answers_question_key
ON answers(question_id, answer_key);

-- Fuzzy search (trigram matching)
CREATE INDEX idx_answers_display_text_trgm
ON answers USING gin(display_text gin_trgm_ops);

-- Score sorting
CREATE INDEX idx_answers_question_score
ON answers(question_id, score DESC);

-- User rankings
CREATE INDEX idx_player_profiles_league_points
ON player_profiles(league_points DESC);

-- Active questions
CREATE INDEX idx_questions_active
ON questions(is_active, category_id);
```

**Query Optimization:**
```java
// ❌ BAD: N+1 query problem
for (GameMove move : moves) {
    Answer answer = answerRepository.findById(move.getAnswerId());
}

// ✅ GOOD: Batch fetch
List<UUID> answerIds = moves.stream()
    .map(GameMove::getAnswerId)
    .toList();
List<Answer> answers = answerRepository.findAllById(answerIds);
```

---

## Testing Strategy (Complete)

### Test Pyramid

```
         /\
        /  \
       / E2E\        ← Few tests, slow, full system
      /______\
     /        \
    /Integration\    ← More tests, medium speed, DB + services
   /____________\
  /              \
 /  Unit Tests    \  ← Most tests, fast, isolated
/__________________\
```

### Unit Tests (Majority)

**What:** Test individual classes in isolation
**Speed:** Milliseconds
**Coverage:** > 80% target

```java
@Test
void shouldCalculateCheckout() {
    // Arrange: Create service with no dependencies
    ScoringService service = new ScoringService();

    // Act: Test one method
    ScoreResult result = service.calculateScore(50, 50);

    // Assert: Verify behavior
    assertThat(result.isCheckout()).isTrue();
}
```

### Integration Tests (Moderate)

**What:** Test components with real database
**Speed:** Seconds
**Coverage:** Critical paths

```java
@DataJpaTest
@Test
void shouldMatchFuzzyAnswer() {
    // Uses real PostgreSQL database
    // Tests repository + database together

    answerRepository.save(createAnswer("agüero", 28));

    Optional<Answer> found = answerRepository
        .findBestMatchByFuzzyName(questionId, "aguero", null, 0.5);

    assertThat(found).isPresent();
}
```

### End-to-End Tests (Few)

**What:** Test complete user flows
**Speed:** Minutes
**Coverage:** Critical user journeys

```java
@SpringBootTest
@AutoConfigureMockMvc
@Test
void shouldCompleteFullGame() {
    // Test complete game flow:
    // 1. Start game (HTTP POST)
    // 2. Submit 10 answers
    // 3. Check win condition
    // 4. Verify database state

    // This touches: Controller → Service → Engine → Database
}
```

### Frontend Testing

**Unit Tests (Vitest):**
```typescript
// Test Svelte component logic
describe('GameState', () => {
    it('should update score when answer submitted', () => {
        const store = gameStore();
        store.submitAnswer('Messi');
        expect(store.score).toBe(466);
    });
});
```

**E2E Tests (Playwright):**
```typescript
// Test full user flow in browser
test('should win a practice game', async ({ page }) => {
    await page.goto('http://localhost:5173');
    await page.click('button:has-text("Start Game")');

    // Submit winning sequence
    await page.fill('input', 'Haaland');
    await page.press('input', 'Enter');

    // Verify win
    await expect(page.locator('.win-state')).toBeVisible();
});
```

---

## Security Considerations

### Anti-Cheat Measures

**1. Server-Side Validation (CRITICAL)**
```java
// ❌ NEVER trust client
// Client sends: { score: 0 }  ← Could be hacked!

// ✅ ALWAYS validate server-side
public GameMove processMove(String answer) {
    // Server calculates score
    // Server validates answer
    // Server checks rules
    return validatedMove;
}
```

**2. Hidden MMR**
- Client never knows true MMR
- Prevents gaming matchmaking
- Only server calculates MMR changes

**3. Turn Timing Enforcement**
- Server tracks turn start time
- Auto-timeout after 45s server-side
- Client timer is visual only

**4. Answer Validation**
- All answers pre-cached in database
- No user-submitted content
- Fuzzy matching threshold enforced server-side

### Rate Limiting

**Authenticated Users:**
- 100 requests per minute
- 1000 requests per hour

**Unauthenticated (Guest):**
- 10 requests per minute per IP
- 100 requests per hour per IP

```java
@RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
public GameStateResponse submitAnswer(SubmitAnswerRequest request) {
    // Process answer
}

private GameStateResponse rateLimitFallback(Exception e) {
    throw new RateLimitExceededException("Too many requests");
}
```

### SQL Injection Prevention

**✅ Always use parameterized queries:**
```java
// ✅ SAFE: Parameterized query
@Query("SELECT a FROM Answer a WHERE a.answerKey = :key")
Optional<Answer> findByAnswerKey(@Param("key") String key);

// ❌ DANGEROUS: String concatenation
String sql = "SELECT * FROM answers WHERE answer_key = '" + userInput + "'";
```

---

## Deployment Architecture

### Production Environment

```
┌─────────────────────────────────────────┐
│            CloudFlare CDN               │
│         (Static Assets + DDoS)          │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Load Balancer (Nginx)          │
│        (SSL Termination + Routing)      │
└─────────────────────────────────────────┘
          ↓                    ↓
┌──────────────────┐  ┌──────────────────┐
│  App Server 1    │  │  App Server 2    │
│  (Spring Boot)   │  │  (Spring Boot)   │
│  + SvelteKit SSR │  │  + SvelteKit SSR │
└──────────────────┘  └──────────────────┘
          ↓                    ↓
┌─────────────────────────────────────────┐
│         PostgreSQL (Primary)            │
│         + Read Replicas (2x)            │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│            Redis Cluster                │
│        (Cache + Session Store)          │
└─────────────────────────────────────────┘
```

### Scaling Strategy

**Horizontal Scaling:**
- Add more app servers as needed
- Load balancer distributes requests
- Stateless servers (session in Redis)

**Database Scaling:**
- Primary for writes
- Read replicas for read-heavy operations
- Connection pooling (HikariCP)

**Cache Scaling:**
- Redis cluster for high availability
- Automatic failover

---

## Summary

The final design includes:
- ✅ **Practice Mode** (implemented)
- 🔜 **Ranked Multiplayer** (real-time, WebSocket)
- 🔜 **Daily Challenges** (global leaderboard)
- 🔜 **Authentication** (OAuth + guest)
- 🔜 **Ranking System** (36 tiers, MMR-based)
- 🔜 **Question Population** (API-Football automation)
- 🔜 **Scheduled Jobs** (daily challenges, stats refresh)
- 🔜 **Caching** (Redis for performance)
- 🔜 **Security** (rate limiting, anti-cheat)
- 🔜 **Deployment** (scalable architecture)

The foundation is solid, and each feature builds on the existing architecture!
