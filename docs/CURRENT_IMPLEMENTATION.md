# Current Implementation - Explained Simply

## What We've Built So Far

Football 501 is like a game of darts, but instead of throwing darts, you name football players. This document explains how everything works right now in simple terms.

---

## The Big Picture: How a Game Works

Imagine you're playing the game. Here's what happens step by step:

```
1. You click "Start Game" on the website
   ↓
2. The website asks the server: "Can I start a game?"
   ↓
3. The server creates a new game and gives you a question
   ↓
4. You type a player's name (like "Haaland")
   ↓
5. The website sends it to the server
   ↓
6. The server checks if it's correct and calculates your score
   ↓
7. You get feedback and your new score
   ↓
8. Repeat until you win (reach 0) or bust!
```

---

## The Parts of the System

### 🎨 Frontend (SvelteKit) - What You See

**Location:** `frontend/src/routes`

This is the web application users interact with.

#### **Game Interface**
**Location:** `frontend/src/routes/+page.svelte`
- Shows your current score (starts at 501)
- Displays the question
- Has a text box where you type player names
- Shows if your answer was correct or wrong
- Keeps track of your recent moves

#### **Admin Interface**
**Location:** `frontend/src/routes/admin/`
- **Categories:** (`/admin/categories`) Manage game categories (e.g., "Premier League", "Champions League").
- **Questions:** (`/admin/questions`) Create and manage questions, filter by category and status. Now includes **Difficulty Levels** (Easy, Medium, Hard).

---

### 🏢 Backend (Spring Boot) - The Game Master

The backend is like the game referee. It knows all the rules and makes sure no one cheats.

---

#### 📡 **Controller Layer** - The Reception Desk

**Location:** `backend/src/main/java/com/football501/controller/`

Think of this as the reception desk at a hotel. It receives requests and directs them to the right place.

**What it does:**
1. **Start Game** (`POST /api/practice/start`)
   - Creates a new match
   - Picks a random question (optionally filtered by **difficulty**)
   - Returns game state

2. **Submit Answer** (`POST /api/practice/games/{gameId}/submit`)
   - Takes your answer
   - Asks GameService to check it
   - Returns if you were right or wrong

---

#### 🎮 **Service Layer** - The Game Logic

##### **GameService** - The Turn Manager
Manages the lifecycle of a single game, including score updates and move history.

##### **MatchService** - The Match Organizer
Handles the overall match (which contains individual games). It now stores the **Match Difficulty** which is applied to every game in that match.

##### **QuestionService** - The Question Master
Picks which question you'll get asked. It now supports filtering by **Difficulty (1=Easy, 2=Medium, 3=Hard)**.

---

#### 🧠 **Engine Layer** - The Smart Rules

##### **AnswerEvaluator** - The Answer Checker
Checks if a player's name matches an answer for the current question using exact and fuzzy matching.

##### **ScoringService** - The Calculator
Enforces the 501 subtraction rules, including the checkout range (-10 to 0).

##### **DartsValidator** - The Darts Rules Expert
Knows which scores are impossible in real darts (e.g., 179 is a bust).

---

### 🐍 Data Pipeline (Python) - The Librarian

**Location:** `football-501-scraper/`

This part of the system fetches real-world data to make the game possible.

#### **The Scraper**
**Location:** `football-501-scraper/scrape_current_season.py`
- Uses `ScraperFC` to pull live data from **FBref.com**.
- Currently targeted at the **2025-2026 Premier League** season.
- **Update-Instead-of-Overwrite:** Updates current season stats in the `career_stats` JSONB array without touching historical data (2023-2024, etc.).

#### **Question & Answer Generator**
- **init_questions_v2.py**: Creates questions for teams and assigns **Difficulty** based on team popularity.
- **populate_answers_v2.py**: Calculates scores for every player for every question and pre-computes "Valid Darts" and "Bust" status for ultra-fast gameplay.

---

## Database Schema

```
┌─────────────────┐       ┌─────────────────────────┐
│    teams        │       │    players              │
│  - popularity   │       │  - name                 │
│    (1-10)       │       │  - career_stats (JSONB) │
└────────┬────────┘       └─────────────┬───────────┘
         │                              │
         │ Automated Generation         │
         ▼                              ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│   questions             │       │        answers          │
│  - difficulty (1-3)     │ ◄─────┤  - question_id (FK)     │
│  - config (JSONB)       │       │  - score                │
└────────┬─────────┬──────┘       └─────────────────────────┘
         │         │
         │         │ 1:N
         ▼         ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│      matches            │ ◄──── │         games           │
│  - player1_id           │       │  - match_id (FK)        │
│  - difficulty (1-3)     │ 1:N   │  - player1_score        │
│  - status               │ ─────►│  - status               │
└─────────────────────────┘       └─────────────────────────┘
```

---

## Key Feature: Difficulty Ranking System

We rank questions to ensure players aren't hit with impossible tasks immediately.

1.  **Team Popularity**: Teams are ranked 1 (Global Giants) to 10 (Niche/Local).
    - *Example*: Manchester City is Rank 1. Leeds United is Rank 2.
2.  **Question Difficulty**: Automatically assigned based on the team.
    - **Easy (1)**: Popular teams (Rank 1-2).
    - **Medium (2)**: Established teams (Rank 3-5).
    - **Hard (3)**: All other teams.
3.  **Gameplay Impact**: When a match starts, the requested difficulty filters the pool of available questions.

---

## What's Currently Working

✅ **Single-player practice mode**
- Full 501 logic with checkout range (-10 to 0).
- Instant feedback with fuzzy name matching.

✅ **Real-World Data (2025-2026 Season)**
- Live statistics for the current Premier League season.
- Historical data preservation for previous seasons.

✅ **Difficulty Ranking**
- Automatic difficulty assignment for questions.
- Difficulty selection when starting a practice game.

✅ **Admin Interface**
- Category and Question management with difficulty controls.

---

## What's NOT Yet Implemented

❌ **Multiplayer**
- Real-time 1v1 matches via WebSockets.

❌ **Authentication**
- Player profiles and persistence.

❌ **Global Leaderboards**
- Tracking the best "finishers" globally.

---

## Key Files to Know

### Backend
- `MatchService.java` - Match lifecycle and difficulty persistence.
- `QuestionService.java` - Difficulty-based question selection.
- `V4__add_difficulty_and_popularity.sql` - Database schema updates.

### Data Pipeline
- `scrape_current_season.py` - FBref scraper for 2025-2026 stats.
- `populate_answers_v2.py` - Score calculation engine.

### Frontend
- `QuestionForm.svelte` - Admin controls for question difficulty.
