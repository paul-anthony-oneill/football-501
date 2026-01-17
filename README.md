# ⚽ Football 501

> A competitive football trivia game with darts 501 scoring mechanics

[![Status](https://img.shields.io/badge/status-in%20development-yellow)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 📋 Overview

**Football 501** is a daily puzzle and multiplayer game that combines football knowledge with the scoring system of darts 501. Players compete to reduce their score from 501 to exactly 0 by naming football players whose statistics (appearances, goals, etc.) match a given question.

### 🎯 Key Features

- ⚡ **Real-time multiplayer** matches with live WebSocket updates
- 📅 **Daily challenges** with global leaderboards
- 🤖 **AI opponents** for practice (Easy, Medium, Hard)
- 🏆 **Competitive ranking system** with 36 ranks (Sunday League to Icon)
- 👥 **Friend challenges** (real-time and asynchronous)
- 📱 **Progressive Web App** - works on desktop and mobile
- 🆓 **Freemium model** - Free daily challenge + 1 match/day

---

## 🎮 How to Play

### Basic Rules

1. **Start with 501 points**
2. A question is presented (e.g., "Appearances for Manchester City in Premier League")
3. **Take turns** naming different players
4. Each player's **statistic is deducted** from your score
5. First to reach **exactly 0** (or -10 to 0 range) wins!

### Scoring Example

**Question**: "Appearances for Arsenal in Premier League"

- You name **"Thierry Henry"** (258 appearances)
  - Score: 501 - 258 = **243**
- Opponent names **"Tony Adams"** (255 appearances)
  - Opponent score: 501 - 255 = **246**

Continue until someone reaches 0!

### Special Rules

- **Bust**: Answers over 180 or invalid darts scores (163, 166, 169, etc.) score **nothing**
- **Checkout Range**: Finish between **-10 and 0**
- **No Repeats**: Can't reuse answers in the same game
- **Turn Timer**: 45 seconds (default) per turn with escalating timeouts
- **Question Refresh**: One chance to request a new question (both players must agree)

📖 **[Full Game Rules](docs/GAME_RULES.md)**

---

## 🛠️ Tech Stack

### Frontend

| Technology | Purpose |
|------------|---------|
| **SvelteKit** | Framework - Reactive, fast, minimal boilerplate |
| **TypeScript** | Type-safe development |
| **Tailwind CSS** | Utility-first styling, mobile-first responsive design |
| **Vite** | Build tool with fast HMR |
| **PWA** | Installable, offline-capable |
| **WebSocket** | Real-time game updates |

### Backend

| Technology | Purpose |
|------------|---------|
| **Java 17+** | Language (LTS version) |
| **Spring Boot 3.x** | Backend framework |
| **Spring Security** | Authentication & authorization |
| **Spring WebSocket** | Real-time communication (STOMP) |
| **Spring Data JPA** | Database ORM |
| **PostgreSQL 15+** | Primary database (ACID, full-text search) |
| **Maven/Gradle** 🔄 | Build tool (TBD) |

### Third-Party Services

| Service | Purpose |
|---------|---------|
| **API-Football** | Player statistics and football data |
| **OAuth 2.0** | Social login (Google, Apple, Facebook) |
| **🔄 Redis** | Session caching, leaderboards (future) |
| **🔄 Cloud Provider** | Hosting (AWS/GCP/DigitalOcean - TBD) |

---

## 📁 Project Structure

```
football-501/
├── docs/                           # 📚 Documentation
│   ├── PRD.md                      # Product Requirements Document
│   ├── GAME_RULES.md               # Complete game rules
│   ├── design/
│   │   └── TECHNICAL_DESIGN.md     # System architecture & tech specs
│   ├── api/
│   │   └── API_INTEGRATION.md      # API-Football integration guide
│   └── plans/                      # Implementation plans (future)
├── frontend/                       # 🔄 Svelte + SvelteKit app (TBD)
├── backend/                        # 🔄 Spring Boot application (TBD)
├── database/                       # 🔄 Schema migrations (TBD)
└── README.md                       # This file
```

---

## 📚 Documentation

Comprehensive documentation for this project (suitable for portfolio showcase):

### Core Documents

- **[Product Requirements Document (PRD)](docs/PRD.md)**
  - Product vision, features, user stories, success metrics
  - Freemium monetization model
  - Competitive ranking system

- **[Game Rules & Mechanics](docs/GAME_RULES.md)**
  - Complete gameplay rules with examples
  - Scoring system (darts 501 mechanics)
  - Match formats (Best of 1/3/5)
  - Turn timers, timeouts, and special rules

- **[Technical Design Document](docs/design/TECHNICAL_DESIGN.md)**
  - System architecture (client-server with WebSockets)
  - Database schema (PostgreSQL)
  - REST API + WebSocket protocol
  - Security, deployment, testing strategy

- **[API Integration Specification](docs/api/API_INTEGRATION.md)**
  - API-Football integration details
  - Caching strategy (zero API calls during matches)
  - Rate limiting and error handling
  - Question population workflow

---

## 🎯 Game Modes

### 1. Daily Challenge (Free)
- One unique question per day
- Single attempt
- Global leaderboard
- Always free for all users

### 2. Multiplayer (Freemium)
- **Quick Match**: Auto-matchmaking with real-time gameplay
- **Friend Challenges**: Challenge friends (real-time or async)
- **Ranked Mode** ⭐: Competitive ladder with MMR and league tiers (Premium)

### 3. AI Practice (Premium)
- Practice against AI opponents
- Difficulty levels: Easy, Medium, Hard
- Future: "Human" difficulty (mimics common player answers)

---

## 🏆 Ranking System

Football 501 features a **football-themed ranking system** with 9 tiers and 4 subtiers each (36 total ranks):

### Tiers (Low → High)

1. ⚽ **Sunday League**
2. 🏅 **Amateur**
3. 🥉 **Semi-Pro**
4. 🎒 **Journeyman**
5. ⚡ **Pro**
6. 🌍 **International**
7. 🌐 **Continental**
8. 🏆 **World Class**
9. 👑 **Icon**

### Subtiers (Within Each Tier)

1. 🪑 **Reserve**
2. 🔄 **Rotation**
3. ⭐ **Starter**
4. 🔰 **Captain**

**Example**: "Pro Captain" → "International Reserve"

### How It Works

- **Hidden MMR** determines fair matchmaking
- **League Points** earned for wins (visible progression)
- Win against higher-rated opponent = more points
- Continuous progression (no seasonal resets)
- Ranked mode exclusive to **Premium** subscribers

---

## 💎 Monetization (Freemium)

### Free Tier

- ✅ Daily challenge (unlimited)
- ✅ 1 casual multiplayer game/day
- ✅ Friend list & challenges
- ❌ No AI practice
- ❌ No ranked mode

### Premium Subscription 🔄

- ✅ **Unlimited** casual matches
- ✅ **Ranked mode** access
- ✅ **Unlimited AI practice**
- ✅ Detailed stats & analytics
- ✅ Priority matchmaking
- ✅ Exclusive cosmetics (future)
- ✅ Ad-free experience

**Pricing**: 🔄 TBD

---

## 🎮 Question Types

### Confirmed Formats

1. **Team League Appearances**
   - "Appearances for Manchester City in Premier League"

2. **Combined Stats (Appearances + Goals)**
   - "Appearances + Goals for Liverpool in Premier League"
   - Score = Sum of both stats

3. **Goalkeeper Stats (Appearances + Clean Sheets)**
   - "Appearances + Clean Sheets for Chelsea in Premier League"

4. **International Appearances**
   - "Appearances for Brazil in World Cup"

5. **Nationality/Continent Filter**
   - "Appearances in Premier League by players from Spain"

---

## 🚀 Development Status

### Current Phase: **Planning & Design** ✅

- [x] Project setup
- [x] Product Requirements Document
- [x] Technical Design Document
- [x] Game Rules specification
- [x] API Integration specification

### Next Phase: **MVP Development** 🔄

#### Phase 1: Core Foundation
- [ ] Frontend project setup (SvelteKit + TypeScript)
- [ ] Backend project setup (Spring Boot + PostgreSQL)
- [ ] Database schema implementation
- [ ] User authentication (OAuth + Guest accounts)
- [ ] Basic REST API

#### Phase 2: Game Engine
- [ ] Scoring rules implementation
- [ ] Answer validation logic
- [ ] Valid darts score checker
- [ ] Question/answer database seeding (20-30 questions)
- [ ] Daily challenge system

#### Phase 3: Multiplayer
- [ ] WebSocket infrastructure
- [ ] Real-time game state synchronization
- [ ] Matchmaking system (quick match)
- [ ] Friend system & challenges
- [ ] AI opponent (basic)

#### Phase 4: Competitive Features
- [ ] Ranking system (MMR + League Points)
- [ ] Ranked matchmaking
- [ ] Leaderboards
- [ ] Player profiles

#### Phase 5: Polish & Launch
- [ ] PWA features (installable, offline)
- [ ] Push notifications (opt-in)
- [ ] Premium subscription flow
- [ ] Performance optimization
- [ ] Security audit
- [ ] Load testing

---

## 🧪 Testing Strategy

### Frontend
- **Unit Tests**: Vitest (component logic, stores, utilities)
- **Integration Tests**: API client mocks, WebSocket mocks
- **E2E Tests**: Playwright (critical user journeys)

### Backend
- **Unit Tests**: JUnit 5 (service layer, game engine)
- **Integration Tests**: Spring Boot Test with TestContainers
- **E2E Tests**: Full match flow (API + WebSocket)

**Coverage Target**: > 80% for unit tests

---

## 🔐 Security

- ✅ HTTPS only in production
- ✅ JWT authentication with HTTPOnly cookies
- ✅ OAuth 2.0 social login (no password storage)
- ✅ Rate limiting (API & WebSocket)
- ✅ Input validation & SQL injection prevention
- ✅ Server-side game state validation (anti-cheat)
- 🔄 GDPR compliance considerations

---

## 📊 Performance Targets

| Metric | Target |
|--------|--------|
| API Response Time (p95) | < 200ms |
| WebSocket Message Latency | < 100ms |
| PWA Load Time (3G) | < 3s |
| Matchmaking Time | < 10s |

---

## 🌐 API Integration

### API-Football

**Provider**: [API-Football](https://www.api-football.com)
**Version**: v3
**Initial Tier**: Free (100 requests/day)

### Caching Strategy

- **Aggressive caching**: All match validation uses cached database
- **Zero API calls** during gameplay
- **Batch population**: Questions populated in advance
- **Weekly updates**: Automated stats refresh for current season

📖 **[Full API Integration Guide](docs/api/API_INTEGRATION.md)**

---

## 🤝 Contributing

🔄 **Contributing guidelines coming soon** (after MVP)

For now, this is a **solo portfolio project**.

---

## 📄 License

🔄 **License TBD** (likely MIT)

---

## 👤 Author

**Paul**
- Portfolio Project (2026)
- Showcasing: Full-stack development, real-time systems, game design, system architecture

---

## 🎓 Learning Goals

This project demonstrates:

1. ✅ **Product Design** - PRD, user stories, competitive analysis
2. ✅ **System Architecture** - Microservices, WebSockets, caching strategies
3. ✅ **Full-Stack Development** - Modern frontend (Svelte) + robust backend (Spring Boot)
4. 🔄 **Real-Time Systems** - WebSocket communication, concurrency handling
5. 🔄 **Database Design** - Relational modeling, indexing, query optimization
6. 🔄 **API Integration** - Third-party APIs, rate limiting, error handling
7. 🔄 **Authentication** - OAuth 2.0, JWT, session management
8. 🔄 **DevOps** - CI/CD, Docker, cloud deployment
9. 🔄 **Testing** - Unit, integration, E2E testing
10. ✅ **Documentation** - Industry-standard technical writing

---

## 📞 Contact

🔄 **Contact information TBD**

---

## 🗺️ Roadmap

### MVP (Q1 2026) 🔄
- Single-player daily challenge
- Multiplayer quick match
- Basic ranking system
- 20-30 curated questions

### Post-MVP (Q2 2026) 🔄
- AI practice mode
- Friend challenges (async)
- Premium subscription
- 100+ questions

### Future (2026+) 🔄
- Admin panel for question management
- Tournament mode
- Social sharing
- Achievements system
- Mobile app optimization
- Community features

---

## ⭐ Acknowledgments

- **API Provider**: [API-Football](https://www.api-football.com) for football statistics
- **Inspiration**: Wordle, Darts 501, football trivia games
- **Framework**: [SvelteKit](https://kit.svelte.dev) & [Spring Boot](https://spring.io/projects/spring-boot)

---

**Built with ⚽ and ☕**

**Status**: 🔄 In Development | **Last Updated**: 2026-01-17
