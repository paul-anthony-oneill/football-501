# Football 501 - Project Implementation Log

**Last Updated**: 2026-02-04
**Status**: Game Engine & Admin UI Implemented

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Implementation Phase: Game Engine & Admin UI](#implementation-phase-game-engine--admin-ui)
3. [Documents Created](#documents-created)
4. [Code Implemented](#code-implemented)
5. [Current Status](#current-status)
6. [Next Steps](#next-steps)

---

## Project Overview

**Football 501** is a competitive football trivia game that combines football knowledge with darts 501 scoring mechanics.

### Tech Stack
- **Frontend**: SvelteKit + TypeScript + Tailwind CSS
- **Backend**: Spring Boot 3.x + Java 17+ + PostgreSQL 15+
- **Data Source**: ScraperFC (Python)

---

## Implementation Phase: Game Engine & Admin UI

### Session Date: 2026-02-04

### Objective
Implement the core gameplay loop (Game Engine) in the backend and provide an administrative interface in the frontend to manage the game content (Categories and Questions).

### Achievements

#### 1. Backend: Game Engine & V3 Migration
- **Database Schema**: Applied `V3__add_game_tables.sql` to introduce `matches`, `games`, and `game_moves`.
- **Practice Mode**: Implemented `PracticeGameController` to handle single-player game flow.
- **Core Logic**: Validated scoring logic (`ScoringService`) and darts rules (`DartsValidator`).
- **State Management**: Games track turns, scores (501 countdown), and win conditions (checkout).

#### 2. Frontend: Admin Interface
- **Routes**: Created `/admin/categories` and `/admin/questions` routes.
- **Components**:
  - `DataTable`: Reusable table with pagination and actions.
  - `FormModal`: Reusable modal for create/edit forms.
  - `ConfirmDialog`: Safety check for deletions.
- **Functionality**:
  - Full CRUD for Categories.
  - List/Filter/Delete for Questions.

---

## Documents Created/Updated

### 1. `IMPLEMENTATION_SUMMARY.md` (Updated)
Reflected the completion of Phase 3, highlighting the new Game Engine capabilities and Admin UI features.

### 2. `docs/CURRENT_IMPLEMENTATION.md` (Updated)
- Added "Admin Interface" section to Frontend documentation.
- Updated Database Schema diagram to include `matches`, `games`, and `game_moves` tables.

---

## Code Implemented

### Backend
- `PracticeGameController.java`: Endpoints for `start`, `submit` answer, and `get` game state.
- `GameService.java`: Orchestrates move processing, turn validation, and state persistence.
- `ScoringService.java` & `DartsValidator.java`: Implements the 501 scoring rules.
- `V3__add_game_tables.sql`: Database migration script.

### Frontend
- `routes/admin/categories/+page.svelte`: Admin page for categories.
- `routes/admin/questions/+page.svelte`: Admin page for questions.
- `lib/api/admin.ts`: API client service.

---

## Current Status

### ✅ Completed
- **Data Source**: ScraperFC validated and integrated (Phase 2).
- **Game Engine**: Single-player practice mode working (Phase 3).
- **Admin UI**: Basic content management operational (Phase 3).

### ⏳ Pending
- **Multiplayer**: Real-time 1v1 matches (WebSocket).
- **Authentication**: User accounts and secure access.
- **Production Deployment**: Hosting and CI/CD pipelines.

---

## Next Steps

1. **Multiplayer Implementation**: Begin work on WebSocket handlers for real-time play.
2. **User Authentication**: Secure the admin routes and allow player profiles.
3. **Integration**: Fully connect the Python scraper to populate the new V3 schema tables automatically.

---

## Design Session: Game Modes & Stretch Goals

### Session Date: 2026-05-26

### Key Design Insight
The interesting difficulty in Football 501 is **strategic, not knowledge-based**. The game's depth comes from deciding *when* to play an answer and *which score to target*, not just from knowing valid answers. This means question difficulty ≠ game difficulty, and easy/medium questions can produce deeply competitive games.

### Decisions Made
- **Question pool weighting / difficulty stratification**: Parked as a stretch goal. The priority is a solid, well-known-clubs question pool for the core game.
- **Multiple game modes**: Designed and documented, but not being implemented yet. See `docs/design/GAME_MODES_STRETCH_GOALS.md`.

### Modes Documented (stretch goals)
| Mode | Summary |
|---|---|
| Daily Challenge | One question/day, global leaderboard, solo, easy/medium questions only |
| Standard H2H | Current implementation — one question, shared answer pool |
| Rapid Fire H2H | Question changes every dart; only one answer needed per question |
| Draft Mode | Pick from 3 questions each turn |
| Category Lock | Pre-agreed category for the whole match |
| Blind Mode | Question hidden until your turn |
| Tournament/League | Brackets or league tables over multiple games |

### Architectural Guardrails (act on these now)
These small decisions in the core game keep future modes open without building them:
- `game_mode` column on `matches` (default `'STANDARD'`)
- `question_id` on `game_moves` (not just on `games`) — needed for Rapid Fire
- `difficulty_score NUMERIC(4,2)` on `questions` — continuous 0–10 scale (see Difficulty Scoring session below)
- `suitable_for_daily` boolean on `questions`

---

## Design Session: Question Difficulty Scoring

### Session Date: 2026-05-26

### Problem
The existing `difficulty INTEGER` (1/2/3 scale) is a classification, not a measurement. The variance within a single tier is too wide — the hardest "Easy" question and the easiest "Medium" question may play identically.

### Key Design Insight: Knowability Correlates With Score Value
A player who scored 175 goals is a household name. A player who scored 3 goals is obscure. The *effective* answer pool a player can draw from is always skewed toward high-value answers. This means measuring the statistical distribution of all answers is misleading — what matters is the count of answers players will realistically know (high-value) and whether the critical game phases are covered.

### Decisions Made

**Replaced `difficulty_tier ENUM` with `difficulty_score NUMERIC(4,2)` (0.00–10.00)**
- Labels like "Easy" or "Hard" are derived from score ranges at render time and never stored
- Avoids the bucketing cliff-edge problem entirely

**Three score zones with relative importance**
| Zone | Range | Weight | Role |
|---|---|---|---|
| High-value | 100–180 | 50% | Velocity; most knowable answers |
| Mid-range | 20–99 | 30% | Navigation toward checkout |
| Checkout | 1–19 | 20% | Precision approach; absence is severely penalised |

**Checkout floor**: If `checkout_count = 0`, minimum difficulty is 7.0 regardless of other zones.

**Depth modifier**: Large total answer pools reduce difficulty by up to 1.5 points (saturates at 200 answers). Shifts game from knowledge to strategy.

**Raw counts stored alongside computed score**: Changing formula constants only requires a single SQL UPDATE — no re-materialisation of answers needed. Zone boundary changes are the expensive operation.

**`difficulty_locked BOOLEAN`**: Admin override flag to pin individual question scores when the formula computes incorrectly.

**`single_question_viable BOOLEAN`**: `true` when sum of valid answer scores ≥ 501. Questions failing this are excluded from standard single-question mode.

### Documents Created
- `docs/design/DIFFICULTY_SCORING.md` — full design, formula, implementation plan
- Question draw logic in a dedicated service method (not inline)