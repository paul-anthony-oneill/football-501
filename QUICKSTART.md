# Football 501 - Quick Start Guide

## 🚀 Get the Game Running

### Prerequisites
- Java 17+ (for backend)
- Node.js 18+ (for frontend)
- PostgreSQL 15+ (for database)

### 1. Start the Database

Make sure PostgreSQL is running with the `football501` database:

```bash
# If using Docker:
docker run -d \
  --name football501-postgres \
  -e POSTGRES_DB=football501 \
  -e POSTGRES_USER=football501 \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  postgres:15
```

The database will auto-migrate on first startup using Flyway.

### 2. Populate Database with Questions & Answers

**IMPORTANT**: The game needs questions and answers to work!

Run the Python scraper to populate data:

```bash
cd scraper
python main.py football --season 2023 --league 39
```

This will:
- Create a question for Premier League 2023/24 appearances
- Populate ~500 player answers with their statistics

### 3. Start the Backend

```bash
cd backend
./mvnw.cmd spring-boot:run
```

Backend will start on http://localhost:8080

### 4. Start the Frontend

```bash
cd frontend
npm install  # First time only
npm run dev
```

Frontend will start on http://localhost:5173

### 5. Play the Game!

1. Open http://localhost:5173 in your browser
2. Click "Start Game"
3. Enter player names (try "Erling Haaland", "Bukayo Saka", etc.)
4. Get your score from 501 to 0!

## 🎮 How to Play

**Goal**: Get your score from 501 to exactly 0 (or within -10 to 0)

**Rules**:
- Name football players who played in the Premier League 2023/24
- Your score decreases by their number of appearances
- Invalid darts scores (163, 166, 169, 172, 173, 175, 176, 178, 179) = BUST
- Bust = no score change
- Reach 0 to win!

## 🛠️ Troubleshooting

### "Error starting game. Is the backend running?"
- Check backend is running on port 8080
- Check CORS is enabled (should be automatic)

### "No question available"
- Run the scraper to populate questions: `cd scraper && python main.py football`

### Database connection errors
- Check PostgreSQL is running
- Verify connection settings in `backend/src/main/resources/application.properties`

### Frontend not connecting
- Check backend URL in `frontend/src/routes/+page.svelte` (line 5)
- Default is `http://localhost:8080/api/practice`

## 📊 Adding More Questions

Run the scraper with different leagues/seasons:

```bash
# La Liga 2023/24
python main.py football --season 2023 --league 140

# Serie A 2023/24
python main.py football --season 2023 --league 135

# Bundesliga 2023/24
python main.py football --season 2023 --league 78
```

## 🏗️ Architecture

```
Frontend (SvelteKit)
  ↓ HTTP REST
Backend (Spring Boot)
  ↓ JDBC
PostgreSQL Database
  ↑ Populated by
Python Scraper (API-Football)
```

## ✅ What's Working

- ✅ Single player practice mode
- ✅ Answer validation with fuzzy matching
- ✅ Darts scoring rules (501 to 0)
- ✅ Bust detection
- ✅ Win condition (checkout range -10 to 0)
- ✅ Real-time feedback
- ✅ Move history

## 🚧 Not Yet Implemented

- Multiplayer mode
- Matchmaking
- User accounts
- Leaderboards
- Daily challenges
- Multiple game modes

---

**Enjoy playing Football 501!** ⚽
