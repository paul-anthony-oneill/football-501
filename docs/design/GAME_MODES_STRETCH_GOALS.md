# Game Modes — Stretch Goals & Architectural Guardrails

**Status**: Parked — document only. No implementation until core game is stable.  
**Last updated**: 2026-05-26

---

## Why This Document Exists

These features are not being built yet. They are documented now so that:

1. **We don't forget the ideas** — the design thinking is captured while it's fresh.
2. **We build with awareness** — small, cheap decisions in the core game (column names, service boundaries) can keep these options open or close them off. This doc flags the ones that matter.
3. **Onboarding is easier** — anyone joining the project can see where the roadmap is heading without it being in a backlog tool.

---

## Core Design Principle (Important)

> The interesting difficulty in Football 501 is **strategic**, not just knowledge-based.

Knowing that Thierry Henry scored 30 goals in a season is trivia. Knowing *when* to play him — holding him back for the exact score you need, or burning him early to deny your opponent — is the game.

This means:
- **Question difficulty ≠ game difficulty.** The same question can be trivial or brutal depending on score state and what answers remain.
- **Easy/medium questions can still produce hard games.** A well-known club with many valid answers is actually a *better* question for competitive play because both players have more genuine decisions to make.
- **Niche/obscure questions are not automatically "fun hard"** — they often just mean fewer valid answers, which reduces the strategic space rather than increasing it.

Keep this principle in mind when building question selection logic.

---

## Planned Game Modes

### 1. Daily Challenge *(highest priority stretch goal)*

**Concept**: One question per day, shared globally. Solo play — no live opponent required.

**Rules**:
- Question is drawn at midnight UTC and fixed for 24 hours.
- Each player gets one attempt. No retries.
- You play through the full 501 game solo (vs. no opponent, or a simulated ghost).
- Leaderboard ranks players by final score (closest to 0 wins; negative scores from the checkout range are possible).
- Bust-heavy games score poorly.

**Question pool**: Easy/medium only — well-known clubs, top leagues, questions with large answer pools. The fun is in the strategy, not in obscure knowledge.

**Ghost opponent (optional enhancement)**: Rather than playing solo against silence, the game could simulate a ghost opponent using the median move from all community attempts so far that day. This preserves the head-to-head feel without requiring a live opponent.

**Why it matters**: Daily Challenge is likely the highest-retention feature. Even players without a live opponent to face have a reason to open the app every day.

---

### 2. Head to Head — Standard *(current implementation)*

The existing game. One question per game. Both players draw from the same answer pool. Deep strategic play: answer management, reading the opponent, holding back high-value players for the right moment.

Nothing to add here — this is the core game being built now.

---

### 3. Head to Head — Rapid Fire *(stretch goal)*

**Concept**: The question changes every dart. Each turn, a brand new question is drawn.

**Key differences from Standard**:
- You only need **one good answer** per question — not a full squad's worth of knowledge.
- Obscure/niche questions become **fun rather than frustrating** — "Name one player who made 7 appearances for Barnsley in the Championship" is a solvable puzzle, not a blocker.
- **No answer pool depletion** between players — each player faces a fresh question on their turn.
- The strategic question shifts from "who do I play?" to "what score do I *want* to hit this turn?" — do you reach for a big stat (risky bust) or play safe?

**Open design question — shared or independent questions?**
- **Option A (Shared)**: Both players answer the same question in a round (Player 1 answers Question A, then Player 2 answers Question A, then Question B for Round 2, etc.). More head-to-head feel.
- **Option B (Independent)**: Each dart draws its own question regardless of whose turn it is. More chaotic, faster, less competitive.

Decision not made — to be resolved when this mode is actually being built.

**Question pool**: Any difficulty is acceptable here since you only need one answer. Niche questions work well precisely because their low answer count doesn't matter.

---

### 4. Draft Mode *(stretch goal)*

**Concept**: Before each turn, 3 questions are shown. The active player picks one.

Adds mind-games: pick the question your opponent is weakest on. Creates a metagame of question awareness on top of the answer game.

---

### 5. Category Lock *(stretch goal)*

**Concept**: Players agree on a category at match start (e.g. "La Liga goals only"). All questions draw exclusively from that pool.

Great for themed nights, rivalry matches, or players who want to specialise. Could also work well for tournament brackets (e.g. "Champions League round — all questions are UCL stats").

---

### 6. Blind Mode *(stretch goal)*

**Concept**: The question is hidden until it's your turn. No pre-thinking between turns.

Tests instinct over preparation. Could combine with Rapid Fire for maximum chaos.

---

### 7. Tournament / League *(longer-term)*

Structured brackets or league tables built on top of the existing head-to-head system. Potentially mixes modes within a series (e.g. Game 1 = Standard, Game 2 = Rapid Fire, Game 3 = Standard).

---

## Architectural Guardrails

These are **small, cheap decisions** to make now in the core game that keep the above options open. None of them require building the stretch features — they just avoid unnecessary future migration pain.

### Database

| What to do now | Why it matters later |
|---|---|
| Add `game_mode VARCHAR` (or enum) to `matches`, default `'STANDARD'` | Every other mode needs this column. Adding it later requires a migration and code change everywhere matches are created. |
| Store `question_id` on `game_moves` (not just on `games`) | Rapid Fire needs per-move question tracking. If we only store question at the game level, Rapid Fire requires a schema change. |
| Add `difficulty_tier ENUM('EASY','MEDIUM','HARD','EXPERT')` to `questions` | Daily Challenge needs to filter to easy/medium. Costs almost nothing to add now; avoids a full-table backfill later. |
| Add `suitable_for_daily BOOLEAN DEFAULT false` to `questions` | Explicit flag for Daily Challenge pool. More reliable than inferring from difficulty_tier alone, since some medium questions may not suit a shared daily format. |

### Backend (Game Engine)

| What to do now | Why it matters later |
|---|---|
| Put question draw logic in a dedicated service method (`QuestionDrawService` or similar) | Rapid Fire swaps "draw once per game" for "draw once per turn". If draw logic is inline in game start code, it needs to be extracted later. A service method makes this a config swap. |
| Pass `gameMode` through game engine context from the start | If game mode is only read at match creation and then discarded, adding mode-specific rules later requires threading it back through the call stack. |

### Frontend

| What to do now | Why it matters later |
|---|---|
| Game UI should read question from game state, not assume it's static | Rapid Fire updates the question mid-game. If the UI hardcodes "the question is set at game start", it needs a significant rework. |
| Mode label/badge component (even if only `STANDARD` renders today) | When new modes ship, the UI has a place to display the mode name in match listings, lobbies, and history. |

---

## What "Parked" Means for Day-to-Day Development

- **Don't implement these features** until the core game is solid and the product has real users.
- **Do follow the guardrails above** — they are not stretch goals, they are just good hygiene.
- **Reference this doc** when making schema or service boundary decisions, specifically to check whether a choice closes off a mode option unnecessarily.
- If a decision would violate a guardrail, it's worth a 5-minute conversation before committing.

---

## Revisit Trigger

Come back to this document when:
- Daily active users are consistent and there's appetite for a retention feature → **Daily Challenge**
- Retention data shows players churning after the novelty of Standard H2H wears off → **Rapid Fire**
- Community asks for themed/competitive events → **Category Lock / Tournament**
