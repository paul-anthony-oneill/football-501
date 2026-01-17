# Football 501 - Product Requirements Document (PRD)

**Version**: 1.0
**Last Updated**: 2026-01-17
**Status**: Draft
**Author**: Paul

---

## Executive Summary

Football 501 is a daily puzzle/trivia game that combines football knowledge with darts 501 scoring mechanics. Players compete to reach exactly 0 points (or -10 to 0 range) by naming football players that match a given question, with each player's statistics determining the score deducted from 501.

**Target Platforms**: Desktop (Web) and Mobile (Progressive Web App)
**Core Audience**: Football fans who enjoy trivia and competitive daily challenges
**Monetization**: Freemium model (free daily challenge + 1 multiplayer game/day; premium for unlimited access)

---

## Product Vision

Create an engaging, competitive football trivia game that:
- Tests football knowledge in a unique scoring format
- Provides quick, accessible gameplay sessions
- Builds a competitive community through ranked leagues
- Works seamlessly across desktop and mobile devices

---

## Core Features

### 1. Game Modes

#### 1.1 Single-Player Daily Challenge
- **Status**: ✅ Confirmed
- One unique question per day, same for all players globally
- Single attempt per day
- Leaderboard ranking based on performance
- Always free for all users

#### 1.2 Multiplayer Modes
- **Status**: ✅ Confirmed

**Quick Match (Real-time)**
- Automated matchmaking with online players
- Real-time turn-based gameplay
- Both casual (free) and ranked (premium) variants

**Friend Challenges**
- Challenge friends directly via username search
- Real-time or asynchronous gameplay options
- Can be casual format

**AI Opponent**
- **Status**: ✅ Confirmed
- Practice mode with AI difficulty levels: Easy, Medium, Hard
- Future enhancement: "Human" difficulty (mimics common player answers)
- Premium feature

#### 1.3 Match Formats
- **Status**: ✅ Confirmed
- Single game (best of 1)
- Best of 3
- Best of 5

### 2. Game Mechanics

#### 2.1 Core Gameplay Loop
- **Status**: ✅ Confirmed
- Both players start with 501 points
- Same question for entire game (e.g., "Appearances for Manchester City in Premier League")
- Players alternate turns naming different players
- Player's statistic (e.g., appearances) is subtracted from their score
- First player to reach exactly 0 or -10 to 0 range wins
- Already-used answers cannot be repeated in same game

#### 2.2 Scoring Rules
- **Status**: ✅ Confirmed

**Valid Scores**
- Must match valid darts scores (achievable with 3 darts in standard 501)
- Scores 1-180 are evaluated for darts validity
- Invalid darts scores (e.g., 179, 178, 176, 175, 173, 172, 169, 166, 163, etc.) result in no score

**Bust Rules**
- Answers over 180 score nothing (turn wasted)
- Players can finish between 0 and -10 (checkout range)
- If Player 1 checks out (e.g., at -9), Player 2 gets one final turn
- If Player 2's final answer brings them closer to 0, Player 2 wins

#### 2.3 Turn Timer
- **Status**: ✅ Confirmed
- Default: 45 seconds per turn (configurable at match start)
- Timeout consequences:
  - 1st timeout: Turn forfeited, timer remains 45s
  - 2nd consecutive timeout: Timer reduced to 30s
  - 3rd consecutive timeout: Timer reduced to 15s
  - 4th consecutive timeout: Match forfeited
- Non-consecutive timeouts reset the timer back to initial value

#### 2.4 Question Refresh
- **Status**: ✅ Confirmed
- Available only at game start (before any answers submitted)
- Either player can request refresh
- Opponent must accept via popup vote
- If accepted, new question loads; if rejected, game continues
- One refresh opportunity per game

#### 2.5 Answer Validation
- **Status**: ✅ Confirmed
- Fuzzy matching for player names (e.g., "Aguero" matches "Agüero")
- If multiple close matches exist, prefer valid answer (even if it's a bust)
- Invalid answers: instant rejection with retry (timer keeps running)
- Real-time validation against cached database

#### 2.6 Match Transparency
- **Status**: ✅ Confirmed
- Full transparency: both players see all submitted answers and scores in real-time
- Includes opponent's player names and exact scores
- Creates strategic depth (busted answers reveal scores)

### 3. Question Types

#### 3.1 Confirmed Question Formats
- **Status**: ✅ Confirmed

1. **Team League Appearances**: "Appearances for [Team] in [League]"
   - Example: "Appearances for Manchester City in Premier League"

2. **Team League Appearances + Goals**: "Appearances + Goals for [Team] in [League]"
   - Score = Appearances + Goals (summed)
   - Example: Player with 275 apps + 184 goals = 459 score (bust!)

3. **Team League Appearances + Clean Sheets** (Goalkeepers): "Appearances + Clean Sheets for [Team] in [League]"
   - Score = Appearances + Clean Sheets (summed)

4. **International Appearances**: "Appearances for [Country] in [Competition/All]"
   - Example: "Appearances for England in World Cup"

5. **Nationality/Continent Filter**: "Appearances in [League] by players from [Country/Continent]"
   - Example: "Appearances in Premier League by players from Brazil"

#### 3.2 Question Categories
- **Status**: 🔄 IN PROGRESS
- Categories for filtering/organization TBD
- Potential: Club Stats, International Stats, League-Specific, Position-Specific

### 4. Competitive Features

#### 4.1 Ranking System
- **Status**: ✅ Confirmed

**Casual Mode**
- No ranking impact
- Win/loss record tracked
- Available to free and premium users (1 game/day for free)

**Ranked Mode** (Premium Only)
- Continuous progression system (no seasonal resets)
- Hidden MMR (Matchmaking Rating) system
- Visible league tier + subtier display

**League Structure**
- 9 Tiers × 4 Subtiers = 36 total ranks
- **Tiers** (low to high):
  1. Sunday League
  2. Amateur
  3. Semi-Pro
  4. Journeyman
  5. Pro
  6. International
  7. Continental
  8. World Class
  9. Icon

- **Subtiers** (within each tier):
  1. Reserve
  2. Rotation
  3. Starter
  4. Captain

**Progression System**
- Players earn **League Points** for wins (adjusted by opponent MMR)
- Accumulate points to advance through subtiers and tiers
- Hidden MMR determines fair matchmaking
- Win against higher-rated opponent = more points
- Loss against lower-rated opponent = more points lost

#### 4.2 Daily Challenge Leaderboard
- **Status**: ✅ Confirmed
- Global leaderboard for daily challenge
- Ranking priority:
  1. Players who checked out (reached -10 to 0)
  2. Among checkouts: fewest answers wins
  3. Players who didn't check out: ranked by closest to 0
- Always free to participate

### 5. Social Features

#### 5.1 Friends System
- **Status**: ✅ Confirmed
- Add friends via username search
- View friends list
- Challenge friends directly (real-time or async)
- No spectating of matches
- No in-game chat (focus on gameplay)

#### 5.2 User Profiles
- **Status**: ✅ Confirmed
- Minimal information displayed:
  - Username
  - Current rank (tier + subtier)
  - Win/loss record
- No detailed analytics or match history (keeps it simple)

#### 5.3 Guest Accounts
- **Status**: ✅ Confirmed
- Play without creating account (guest mode)
- Guest data is temporary (lost when upgrading to full account)
- No friends, no rank, no daily challenge for guests
- Guests can play casual quick match only

### 6. Authentication & Accounts

#### 6.1 Account Types
- **Status**: ✅ Confirmed
- **Guest**: Temporary, no data persistence
- **Registered**: Social login (Google, Apple, Facebook, etc.)

#### 6.2 Guest to Registered Migration
- **Status**: ✅ Confirmed
- Guest data does NOT transfer
- Clean slate when creating registered account

### 7. Monetization

#### 7.1 Free Tier
- **Status**: ✅ Confirmed
- Daily challenge (unlimited, always free)
- 1 casual multiplayer game per day (quick match OR friend challenge)
- No AI practice
- No ranked mode
- No detailed stats

#### 7.2 Premium Subscription
- **Status**: ✅ Confirmed
- **Includes**:
  - Unlimited casual matches
  - Ranked mode access
  - Unlimited AI practice (all difficulty levels)
  - Detailed stats and analytics
  - Priority matchmaking
  - Exclusive cosmetics (future)
  - Ad-free experience

- **Pricing**: 🔄 IN PROGRESS (TBD)

### 8. Notifications

#### 8.1 Push Notifications
- **Status**: ✅ Confirmed
- Opt-in only (user preference)
- Types:
  - New daily challenge available
  - Friend challenge received
  - Async match: your turn
  - Ranked season updates (future)
  - Match result summary

### 9. Data & API Integration

#### 9.1 Football Statistics API
- **Status**: ✅ Confirmed
- **Provider**: API-Football (api-football.com)
- **Tier**: Free tier for MVP (100 requests/day)
- **Architecture**: Aggressive caching (all answers pre-fetched)

#### 9.2 Question Management
- **Status**: ✅ Confirmed
- **MVP**: 20-30 manually curated questions
- **Answer Population**: Batch API calls using free tier (spread over 2-3 weeks)
- **Match Validation**: Zero API calls (all from cached database)
- **Weekly Updates**: Batch refresh current season stats (10-20 API calls/week)

#### 9.3 Question Catalog
- **Status**: 🔄 IN PROGRESS
- Manual curation for launch
- Future: Admin panel for adding questions
- Future: Automated question generation from API data

---

## User Stories

### Epic 1: Core Gameplay
- ✅ As a player, I want to compete in a darts-style scoring game using football knowledge
- ✅ As a player, I want to see my score decrease as I name valid players
- ✅ As a player, I want to know which answers have been used so I don't repeat them
- ✅ As a player, I want clear feedback when my answer is invalid

### Epic 2: Daily Challenge
- ✅ As a player, I want a new challenge every day to test my knowledge
- ✅ As a player, I want to see how I rank against others globally
- ✅ As a player, I want this to be free forever

### Epic 3: Multiplayer
- ✅ As a player, I want to compete against real opponents in real-time
- ✅ As a player, I want to challenge my friends
- ✅ As a player, I want to practice against AI
- ✅ As a player, I want to play asynchronously with friends when we're not both online

### Epic 4: Competitive Ranking
- ✅ As a competitive player, I want to climb a ranked ladder
- ✅ As a competitive player, I want fair matchmaking based on skill
- ✅ As a competitive player, I want to see my progression over time

### Epic 5: Mobile & Desktop Experience
- ✅ As a mobile user, I want a smooth, responsive experience on my phone
- ✅ As a desktop user, I want to play in my browser without downloads
- ✅ As a user, I want to install the app on my phone like a native app (PWA)

---

## Technical Constraints

### Performance Requirements
- Match answer validation: < 200ms response time
- Matchmaking: < 10 seconds to find opponent
- PWA load time: < 3 seconds on 3G connection

### Scalability Requirements
- **Status**: 🔄 IN PROGRESS
- Initial target: 100 concurrent users
- Growth target: 10,000 DAU within 6 months

### Browser Support
- **Status**: 🔄 IN PROGRESS
- Desktop: Chrome, Firefox, Safari, Edge (latest 2 versions)
- Mobile: iOS Safari 14+, Chrome Android 90+

---

## Success Metrics

### Launch Metrics (MVP)
- **Status**: 🔄 IN PROGRESS
- Daily Active Users (DAU): Target TBD
- Daily Challenge completion rate: Target TBD
- Average session duration: Target TBD

### Engagement Metrics
- **Status**: 🔄 IN PROGRESS
- Day 7 retention: Target TBD
- Day 30 retention: Target TBD
- Premium conversion rate: Target TBD

### Quality Metrics
- **Status**: 🔄 IN PROGRESS
- Answer validation accuracy: > 95%
- Match completion rate: > 90%
- App crash rate: < 1%

---

## Out of Scope for MVP

- Spectating matches
- In-game chat or emotes
- Seasonal rank resets
- Community-generated questions
- Real-world prizes or tournaments
- Native mobile apps (iOS/Android)
- Replays or match history
- Achievements or badges
- Player-to-player trading/gifting
- Multiple language support

---

## Future Enhancements

### Post-MVP Features (Prioritized)
1. **Admin panel** for question management
2. **"Human" difficulty AI** using common answer patterns
3. **Seasonal events** with special question sets
4. **Achievements system** for engagement
5. **Cosmetic customization** (profile themes, badges)
6. **Tournament mode** for community events
7. **Statistics dashboard** with detailed analytics
8. **Match history** and replay system
9. **Social sharing** of daily challenge scores
10. **Clan/Team system** for group competition

---

## Risks & Mitigation

### API Dependency Risk
- **Risk**: API-Football free tier limits (100 req/day) could block question population
- **Mitigation**: Pre-populate question pool before launch; use batch updates; upgrade to Pro tier ($19/mo) if needed

### Question Quality Risk
- **Risk**: Manually curated questions may have errors or outdated stats
- **Mitigation**: Weekly batch refresh from API; community reporting (future); internal QA process

### Matchmaking Risk
- **Risk**: Low player count = long queue times
- **Mitigation**: AI opponents as fallback; cross-platform play; async friend matches

### Mobile Performance Risk
- **Risk**: PWA performance on older mobile devices
- **Mitigation**: Optimize bundle size; lazy loading; performance testing on low-end devices

### Cheating Risk
- **Risk**: Players could look up answers externally
- **Mitigation**: Turn timer pressure; server-side validation; hidden MMR protects ranked integrity

---

## Glossary

- **Bust**: An answer that scores over 180 or is an invalid darts score (results in no score)
- **Checkout**: Reaching exactly 0 or -10 to 0 range to win the game
- **Fuzzy Matching**: Intelligent name matching that handles spelling variations
- **MMR**: Matchmaking Rating (hidden skill rating)
- **PWA**: Progressive Web App (installable web application)
- **League Points**: Visible points earned to advance through ranks
- **Subtier**: Subdivision within a tier (Reserve, Rotation, Starter, Captain)
- **Tier**: Major rank division (Sunday League, Amateur, Semi-Pro, etc.)

---

## Appendix

### Question Examples
1. "Appearances for Manchester City in Premier League"
2. "Goals for Real Madrid in La Liga"
3. "Appearances + Goals for Liverpool in Premier League"
4. "Appearances + Clean Sheets for Chelsea in Premier League" (Goalkeepers)
5. "Appearances for Brazil in World Cup"
6. "Appearances in Premier League by players from Spain"

### Valid Darts Scores Reference
Valid scores in 501 darts (achievable with 3 darts):
- All values 1-180 EXCEPT: 163, 166, 169, 172, 173, 175, 176, 178, 179

---

**Document Status**: Draft
**Next Review**: After technical design is complete
**Approval**: Pending
