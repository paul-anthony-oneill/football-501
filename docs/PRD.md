# Trivia 501 - Product Requirements Document (PRD)

**Version**: 2.0
**Last Updated**: 2026-06-08 (single-player pivot — multiplayer deferred, ranking dropped, Free Play added)
**Status**: Draft
**Author**: Paul

---

## Executive Summary

Trivia 501 is a daily social trivia game that combines football knowledge with darts 501 scoring mechanics. Each day, players are given a question and a target score — the goal is to reach exactly 0 by naming players whose statistics match the question. Score to zero, share your emoji-grid result with friends, and come back tomorrow for a new challenge.

**Target Platforms**: Desktop (Web) and Mobile (Progressive Web App)
**Core Audience**: Football fans who enjoy trivia, daily puzzles, and comparing results with friends
**Monetization**: None at launch — focus on growth and product quality

---

## Product Vision

Create a daily trivia game that:
- Gives players one compelling challenge per category per day, shared by everyone globally
- Makes every day feel different through varied starting scores and question pools
- Lets friends compare results via simple share links (Wordle-style emoji grids)
- Provides a Free Play mode for when players want to play on their own terms
- Works seamlessly across desktop and mobile devices

---

## Core Features

### 1. Game Modes

#### 1.1 Daily Challenge (Core Mode)
- **Status**: ✅ Built (needs polish — expanded starting scores, OG metadata, test question exclusion)
- One question per category per day, same for all players globally
- Variable starting score randomly selected from a curated pool of 20–30 scores (101–501 range)
- Single attempt per day — trust-based, no replay enforcement
- Emoji-grid result sharing (VALID→🟩, BUST→🟥, INVALID→⬜, CHECKOUT→🎯)
- Deep-link share URLs with Open Graph rich previews
- Always free, no account required

#### 1.2 Free Play (Formerly "Practice")
- **Status**: ✅ Built (needs UI reframe — rename from "practice", remove practice language)
- Pick any category, league, club, and stat type to play on your own terms
- No daily limit, no opponent, no time pressure
- Same scoring mechanics as Daily Challenge
- Players can optionally enable a turn timer for added pressure
- Real-time answer validation and autocomplete

#### 1.3 Async Friend Challenges (Planned)
- **Status**: 📋 P2 stretch goal
- Play the same daily challenge question as a friend and compare results
- Each player plays independently; results compared when both have finished
- "Can you beat my score?" share links that deep-link into the challenge
- Side-by-side emoji-grid result comparison

#### 1.4 Match Formats
- **Status**: ✅ Built (Free Play only; multiplayer formats parked)
- Single game (best of 1) for all modes
- Best of 3/5 formats (`MatchFormat` enum) exist in the data model, retained for future use

### 2. Game Mechanics

#### 2.1 Core Gameplay Loop
- **Status**: ✅ Confirmed
- Player starts with a target score (e.g., 501, 301, 167 — varies per game)
- Same question for entire game (e.g., "Appearances for Manchester City in Premier League")
- Player names players whose statistic matches the question
- Each valid player's statistic is subtracted from their score
- Goal: reach exactly 0 or land in the checkout zone (-10 to 0)
- Already-used answers cannot be repeated in the same game
- Game ends at checkout, bust-out (no valid moves remain), or forfeit

#### 2.2 Scoring Rules
- **Status**: ✅ Confirmed

**Valid Scores**
- Must match valid darts scores (achievable with 3 darts in standard 501)
- Scores 1-180 are evaluated for darts validity
- Invalid darts scores (e.g., 179, 178, 176, 175, 173, 172, 169, 166, 163) result in a bust (no score deducted, answer wasted)

**Bust Rules**
- Answers over 180 score nothing (turn wasted)
- Invalid darts scores score nothing (turn wasted)
- Player can finish by landing in the checkout zone: -10 to 0 (inclusive)
- Busts are shown in emoji-grid shares as 🟥

#### 2.3 Turn Timer (Free Play)
- **Status**: 🔄 Client display built; server enforcement pending (P1)
- Default: 45 seconds per turn (optional in Free Play, can be disabled)
- Timeout consequences:
  - 1st timeout: Turn forfeited, timer remains 45s
  - 2nd consecutive timeout: Timer reduced to 30s
  - 3rd consecutive timeout: Timer reduced to 15s
  - 4th consecutive timeout: Game forfeited
- Non-consecutive timeouts reset the timer back to initial value
- Daily Challenge: no timer — play at your own pace (single attempt per day, no pressure)

#### 2.4 Question Refresh
- **Status**: ✅ Confirmed (Free Play only)
- Available only at game start (before any answers submitted)
- Refresh gives a new random question from the same parameters
- One refresh opportunity per game
- Not applicable to Daily Challenge (question is fixed for the day)

#### 2.5 Answer Validation
- **Status**: ✅ Confirmed
- Fuzzy matching for player names (e.g., "Aguero" matches "Agüero")
- If multiple close matches exist, prefer valid answer (even if it's a bust)
- Invalid answers: instant rejection with retry
- Real-time validation against cached database only (zero external API calls during gameplay)

#### 2.6 Used Answer Tracking
- **Status**: ✅ Confirmed
- Already-used answers shown to the player so they don't repeat them
- Used answers displayed with their statistic value (shows which scores are no longer available)
- Creates strategic depth: deciding when to use a high-value answer

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
- **Status**: ✅ Active — Football, Geography, Film categories live
- **Football**: Club stats (appearances, goals, assists, clean sheets), league-scope questions, international appearances, nationality-filtered stats. Sub-categories: Premier League, La Liga, Bundesliga, Serie A, Ligue 1.
- **Geography**: Country/city population, area, and demographic statistics
- **Film**: Movie box office, runtime, release year, and cast statistics
- **Future**: Rugby, Cricket, and other sports when question pools exist

### 4. Player Profiles

#### 4.1 Personal Dashboard
- **Status**: 📋 P1 — Shortly after launch
- Player profiles serve as a personal stats dashboard, not a competitive ranking
- **Profile contents**:
  - Username / display name
  - Daily challenge history (dates played, categories, scores, emoji grids)
  - Free Play history (games played, favourite categories, personal bests)
  - Personal records: lowest score achieved, most answers used in one game, longest streak of daily plays
- No MMR, no league tiers, no competitive ranking
- Profiles are private by default; players choose what to share
- **Why deferred**: Player profiles table (V23 migration) exists but needs the auth layer and game history endpoints to be useful.

#### 4.2 Daily Challenge Streaks
- **Status**: 📋 P1 — Shortly after launch
- Track consecutive days playing the daily challenge
- Streak counter displayed on the daily challenge overview page
- Streak breaks when a day is missed (no freeze items or catch-up mechanics — keep it simple)
- Personal best streak shown on player profile

### 5. Social Features

#### 5.1 Emoji-Grid Result Sharing
- **Status**: ✅ Built (needs OG metadata polish — P1)
- Share daily challenge results as emoji grids (Wordle-style)
- Deep-link URLs that open directly to the challenge result
- Open Graph metadata for rich link previews in messaging apps
- Share format: category name, final score, emoji grid, link back to the game

#### 5.2 Async Friend Comparisons (Planned)
- **Status**: 📋 P2 stretch goal
- "Can you beat my score?" challenge links
- Side-by-side result comparison when both players have finished
- No real-time interaction required
- No friend list or social graph — share links are the social mechanic

#### 5.3 Guest Play
- **Status**: 📋 P0 — Launch blocker
- Play daily challenges and Free Play without creating an account
- Ephemeral anonymous identity via HTTPOnly cookie
- No data persistence for guests (history lost on device/browser change)
- Sign in (Google OAuth) to save history, build a profile, and send friend challenges
- No "guest" label — just seamless anonymous play with a "Sign in to save your progress" nudge after completing a game

### 6. Authentication & Accounts

#### 6.1 Account Types
- **Status**: 🔄 In Progress
- **Anonymous**: Ephemeral UUID stored in HTTPOnly cookie. Can play daily challenges and Free Play. No data persistence across devices/browsers. No setup required — just open the app and play.
- **Registered**: Google OAuth 2.0 social login via Supabase. Saves game history, player profile, and daily challenge streaks. Required for friend challenges (future).

#### 6.2 Anonymous to Registered Migration
- **Status**: 📋 P1 — Shortly after launch
- Anonymous play data does not transfer to registered account
- Clean slate when signing up — this is simple and honest
- Sign-in prompt appears as a soft nudge after game completion, never as a modal wall blocking gameplay

### 7. Monetization

#### 7.1 Current Position
- **Status**: ✅ Decided — No monetization at launch
- Daily challenges: always free
- Free Play: always free
- No premium tier, no game limits, no ads
- Focus entirely on product quality and user growth
- Monetization may be revisited once the product has traction and retention data, but there is no timeline for this

### 8. Notifications

#### 8.1 Push Notifications
- **Status**: 📋 P2 — Stretch goal
- Opt-in only (user preference)
- Types:
  - New daily challenge available
  - Friend challenge received (when async challenges ship)
  - Streak at risk reminder ("You haven't played today's challenge yet")
  - Personal best achieved

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
- ✅ As a player, I want to play a darts-style scoring game using football knowledge
- ✅ As a player, I want to see my score decrease as I name valid players
- ✅ As a player, I want to know which answers have been used so I don't repeat them
- ✅ As a player, I want clear feedback when my answer is invalid and why

### Epic 2: Daily Challenge
- ✅ As a player, I want a new challenge every day to test my knowledge
- ✅ As a player, I want to choose from multiple categories (Football, Geography, Film)
- 🔄 As a player, I want each day to feel different through varied starting scores
- ✅ As a player, I want this to be free forever with no account required

### Epic 3: Social Sharing
- ✅ As a player, I want to share my result as an emoji grid
- 🔄 As a player, I want my shared link to show a rich preview in messaging apps (OG metadata — P1)
- 📋 As a player, I want to challenge a friend to beat my score on today's challenge (P2)

### Epic 4: Free Play
- ✅ As a player, I want to pick any category, league, club, or stat type and play on my own
- 🔄 I want Free Play to feel like its own mode, not a warm-up for the daily challenge (UI reframe — P0)
- 📋 As a player, I want to optionally enable a timer in Free Play for added pressure (P1)

### Epic 5: Player Profiles
- 📋 As a player, I want to see my daily challenge history and streaks (P1)
- 📋 As a player, I want to see my personal bests across categories (P1)
- 📋 As a player, I want to sign in with Google to save my progress (P1)

### Epic 6: Mobile & Desktop Experience
- ✅ As a mobile user, I want a smooth, responsive experience on my phone
- ✅ As a desktop user, I want to play in my browser without downloads
- 📋 As a user, I want the app to work reliably on spotty connections (PWA validation — P1)

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

### PWA Requirements
- **Status**: 🔄 IN PROGRESS
- Installable on mobile devices (manifest + service worker)
- App shell, lobby, and daily challenge browse pages cached for offline access
- Game validation requires server (cannot work fully offline)
- Sub-3s load time on 3G connection

---

## Success Metrics

### Launch Metrics
- Daily Active Users (DAU): Target 100 initially
- Daily Challenge completion rate: Target > 60% of visitors who start
- Share rate: Target > 10% of completed challenges shared

### Engagement Metrics
- Day 7 retention: Target TBD
- Day 30 retention: Target TBD
- Average sessions per week: Target TBD

### Quality Metrics
- Answer validation accuracy: > 95%
- Game completion rate: > 90% (no crashes or stuck states)
- App crash rate: < 1%

---

## Out of Scope

### Permanently Out of Scope
- Real-time multiplayer matches (deferred indefinitely)
- Competitive ranking, MMR, and league tiers (dropped)
- Matchmaking queue (dropped)
- AI opponent (deferred indefinitely)
- In-game chat or emotes
- Seasonal rank resets
- Community-generated questions
- Real-world prizes or tournaments
- Native mobile apps (iOS/Android)
- Multiple language support

### Deferred to P1 (shortly after launch)
- Player profiles (personal dashboard, no ranking)
- Daily challenge streaks
- Match/game history
- PWA offline capability
- OG metadata for share links
- Content Security Policy

### Deferred to P2 (stretch goals)
- Async friend challenges
- Push notifications
- Expert Challenge mode
- Custom game rules (darts strictness, checkout zone, timer)
- Achievement/badges system

---

## Risks & Mitigation

### API Dependency Risk
- **Risk**: API-Football free tier limits (100 req/day) could block question population
- **Mitigation**: Pre-populate question pool before launch; use batch updates; upgrade to Pro tier ($19/mo) if needed

### Question Quality Risk
- **Risk**: Manually curated questions may have errors or outdated stats
- **Mitigation**: Weekly batch refresh from scraper service; community reporting (future); internal QA process

### Content Freshness Risk
- **Risk**: Without multiplayer, the daily challenge must carry the product's retention on its own. If question pools are too small or starting scores too repetitive, daily players will churn.
- **Mitigation**: Expand starting score pool from 9 to 20–30 (P0). Populate more categories via the scraper. Ensure question rotation avoids consecutive repeats. Add expert and niche categories over time for variety.

### Mobile Performance Risk
- **Risk**: PWA performance on older mobile devices
- **Mitigation**: Optimize bundle size; lazy loading; performance testing on low-end devices

### Cheating Risk
- **Risk**: Players could look up answers externally (daily challenge is trust-based)
- **Mitigation**: Daily challenge is designed for casual social play, not cash prizes. Server-side validation ensures the game itself is honest. No leaderboard eliminates the incentive to cheat for rank.

---

## Glossary

- **Bust**: An answer that scores over 180 or is an invalid darts score (turn wasted, no score deducted)
- **Checkout**: Reaching exactly 0 or landing in the -10 to 0 range to complete the game
- **Free Play**: Open-ended solo mode where players pick any category/question and play on their own terms
- **Daily Challenge**: One question per category per day, same for all players globally, with emoji-grid sharing
- **Fuzzy Matching**: Intelligent name matching that handles spelling variations and accents
- **PWA**: Progressive Web App (installable web application)
- **Emoji Grid**: Shareable result format — 🟩 valid answer, 🟥 bust, ⬜ invalid, 🎯 checkout

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
