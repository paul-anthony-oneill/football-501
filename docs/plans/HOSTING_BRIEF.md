# Football 501 — Hosting Brief

A concise overview for discussing infrastructure, deployment, and hosting options.  
**Use this as your starting context when talking to an LLM about hosting decisions.**

---

## What It Is

Football 501 is a competitive football trivia game built on 501-darts scoring mechanics. Two players compete to reduce their score from 501 to exactly 0 by naming football players whose career statistics match a given question (e.g. "Name a player with exactly 23 appearances for Arsenal in the Premier League").

Target audience: football fans who want a quick, skill-based competitive game.  
Target launch: MVP (pre-commercial, small user base initially).

---

## System Components (What Needs Hosting)

### 1. Frontend — Next.js 16 (React 19)
- **Framework**: Next.js 16 App Router, fully `"use client"` (SPA behaviour — no SSR needed)
- **Styling**: Tailwind CSS 4
- **What it does**: serves the game UI, admin interface, calls the backend API
- **Special requirement**: needs to be served as a PWA (Progressive Web App) for mobile home-screen installs
- **API calls**: all proxied to the Spring Boot backend at `/api/*`

### 2. Backend — Spring Boot 4 / Java 25
- **Framework**: Spring Boot 4.0.6, Java 25
- **What it does**: REST API, game logic, WebSocket (STOMP) for real-time multiplayer
- **Current state**: REST API complete; WebSocket multiplayer is next on the roadmap
- **Port**: 8080 in dev
- **Auth**: currently dev-mode (no real auth); OAuth 2.0 (Google login) is the next MVP item
- **Memory**: Spring Boot app is not heavy — a 1–2 vCPU / 1–2 GB RAM instance is fine at MVP scale

### 3. Database — PostgreSQL 15+
- **Extensions required**: `pg_trgm` (trigram fuzzy search), `unaccent` (accent-insensitive search)
- **Key data**: questions, pre-cached player answers, match history, user accounts, ranking data
- **Scale at launch**: small — hundreds to low thousands of questions; thousands of player records
- **Critical constraint**: must support GIN trigram indexes (`gin_trgm_ops`) for player name fuzzy matching

### 4. Python Scraper Microservice
- **Language**: Python 3.x, uses `ScraperFC` library
- **What it does**: fetches player stats from FBref.com and populates the `questions` and `answers` tables
- **Run frequency**: batch jobs, not a continuously running server — runs weekly (or on-demand by admin)
- **Hosting need**: lightweight — a cron job on a small VM, a cloud scheduler with a serverless function, or even a local machine is fine for MVP

### 5. WebSocket (STOMP)
- **Built into the Spring Boot backend** — not a separate service
- **Requirement**: hosting must support persistent WebSocket connections (rules out some serverless options for the backend)

---

## Architecture Diagram

```
Browser / PWA (Next.js)
        │
        │ HTTPS + WSS
        ▼
Spring Boot Backend (REST + WebSocket)
        │
        ▼
   PostgreSQL
        ▲
        │ (batch, not live)
Python Scraper (FBref → DB)
```

---

## Key Constraints for Hosting Decisions

| Constraint | Detail |
|---|---|
| WebSocket support required | Backend must stay on a persistent server or container — not serverless (Vercel Functions, Lambda) |
| PostgreSQL extensions | `pg_trgm` and `unaccent` must be available — standard on managed Postgres services (Supabase, RDS, etc.) |
| PWA requirement | Frontend host must support custom headers (for service worker scope, manifest) |
| Auth: OAuth redirect | When Google OAuth is added, the backend needs a stable public HTTPS URL for the OAuth callback |
| Data residency | No specific legal requirement at MVP; EU hosting preferred if possible (GDPR good practice) |
| Cost sensitivity | Pre-revenue MVP — smallest viable instance sizes are preferred |

---

## Current Implementation Status

| Layer | Status |
|---|---|
| Frontend (Next.js) | Working — lobby, game UI, admin interface |
| Backend REST API | Complete — practice mode, admin CRUD, difficulty scoring |
| Database schema | Complete through V13 migration |
| WebSocket multiplayer | Not yet built (next priority) |
| Real authentication | Not yet built (OAuth 2.0 Google login is next) |
| Python scraper | Written; not yet run against production DB |
| Data population | Empty — scraper must run before live gameplay is possible |

---

## MVP Scale Expectations

- **Concurrent users at launch**: tens, maybe low hundreds
- **Questions in DB**: hundreds (one scraper run)
- **Matches per day**: tens at MVP
- **Growth plan**: gradual; infrastructure can be right-sized up as needed

This is not a high-traffic system at launch. The main architectural complexity is the WebSocket requirement (no serverless backend) and the PostgreSQL extension requirement.

---

## What's NOT decided yet

- Hosting provider (cloud vs PaaS vs VPS)
- Domain / CDN setup
- CI/CD pipeline
- Monitoring / alerting
- Email provider (for auth emails)
- Cost budget

These are the questions you likely want to discuss with an LLM.
