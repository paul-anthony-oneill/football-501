# Plan: Entertainment (Film) Category Implementation

Following the pattern set by the Geography category (V21 migration), we will implement an "Entertainment" (Film) category seeded with data from The Movie Database (TMDB) API.

## Objectives
- Seed a new "Entertainment" category.
- Provide questions based on Worldwide Box Office (revenue).
- Ensure high-quality autocomplete for movie titles.
- Follow the "Geography Pattern" (SQL-based seed for MVP).

## Phase 1: Data Acquisition (Scraper)
- Create `football-501-scraper/scrapers/tmdb_scraper.py`.
- Use TMDB API to fetch:
    - Top movies by revenue for specific years/decades.
    - Movie metadata (title, release year, revenue, poster path).
    - Cast information for top actors.
- **Scoring Logic:**
    - Score = Revenue in millions (e.g., $500,000,000 = 500).
    - `is_bust` = TRUE if score > 180.
    - `is_valid_darts` = TRUE if score <= 180.
- **Output:** The script will generate the SQL content for a Flyway migration.

## Phase 2: Database Migration
- Create `backend/src/main/resources/db/migration/V22__seed_entertainment_category.sql`.
- **Schema Updates:**
    - Insert `categories` row (`slug='entertainment'`, `theme='bigscreen'`).
    - Insert `questions` rows (Status: `active` to match Geography pattern).
    - Insert `entities` (All movie titles for autocomplete).
    - Insert `answers` (Linking movie titles to question IDs with scores).

## Phase 3: Frontend Integration
- Update `frontend-react/src/lib/questionHierarchy.ts` to ensure the `film` (or `entertainment`) category matches the database IDs and has the correct child nodes for navigation.
- Verify theme `bigscreen` is supported or mapped in the UI.

## Phase 4: Validation
- Run the migration.
- Verify the "Entertainment" category appears in the category picker.
- Test autocomplete for several movie titles.
- Play a quick game to verify scoring and "Bust" logic for high-revenue films.

## TMDB API Details
- **Endpoint:** `GET /discover/movie?sort_by=revenue.desc`
- **Field:** `revenue` (Worldwide Box Office).
- **Scale:** Divide by 1,000,000 to get score.
