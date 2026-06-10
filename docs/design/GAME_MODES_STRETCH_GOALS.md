# Game Modes — Active & Parked

**Last updated**: 2026-06-08 (single-player pivot — multiplayer modes deferred indefinitely, async friend challenges added, Free Play reframed)
**Status**: Daily Challenge + Free Play are active. All multiplayer modes are parked.

---

## Why This Document Exists

1. **We don't forget the ideas** — the design thinking is captured while it's fresh.
2. **We build with awareness** — some cheap decisions in the core game keep future options open. This doc flags the ones that matter.
3. **Onboarding is easier** — anyone joining the project can see where the roadmap is heading.

---

## Core Design Principle

> The interesting difficulty in Trivia 501 is **strategic**, not just knowledge-based.

Knowing that Thierry Henry scored 30 goals in a season is trivia. Knowing *when* to play him — holding him back for the exact score you need, or burning him early to reduce your options — is the game.

This means:
- **Question difficulty != game difficulty.** The same question can be trivial or brutal depending on score state and what answers remain.
- **Easy/medium questions can still produce hard games.** A well-known club with many valid answers is actually a *better* question because players have more genuine decisions to make.
- **Niche/obscure questions are not automatically "fun hard"** — they often just mean fewer valid answers, which reduces the strategic space.

Keep this principle in mind when building question selection logic.

---

## Active Modes

### 1. Daily Challenge — Core Mode

**Status**: Built, needs polish (see P0 items in BACKLOG.md)

**Concept**: One question per category per day, shared globally. Same starting score for everyone. Emoji-grid result sharing. Wordle-style.

**Rules**:
- Question is drawn at midnight UTC and fixed for 24 hours.
- Each player gets one attempt per category. No retries.
- Starting score randomly selected from a curated pool of 20-30 values (101-501 range). Currently 9 values; expanding the pool is P0.
- No turn timer — play at your own pace.
- No leaderboards (too easy to cheat). Sharing results with friends is the social mechanic.
- Trust-based — no replay enforcement.

**Question pool**: Easy/medium questions only (`difficulty_score <= 3.5`, `suitable_for_daily = true`). Well-known clubs, top leagues, questions with large answer pools. The fun is in the strategy, not in obscure knowledge.

**Why it's the core mode**: Daily Challenge is the highest-retention feature. Even players with no friends playing the game have a reason to open it every day.

---

### 2. Free Play — Secondary Mode

**Status**: Built, needs UI reframe (P0 — rename from "practice", remove practice language)

**Concept**: Open-ended solo mode. Pick any category, league, club, stat type and play on your own terms. No daily limit, no opponent, no pressure.

**Key difference from Daily Challenge**: Free Play is not "practice for the daily" — it's a separate way to play. The daily challenge is a curated, shared puzzle; Free Play is self-directed exploration.

**Features**:
- Lobby drill-down nav: category -> league -> club -> stat type
- Optional turn timer (can be disabled)
- Same scoring mechanics as Daily Challenge
- Question refresh available at game start

---

## Parked — Multiplayer Modes (Deferred Indefinitely)

The following modes were designed for competitive multiplayer. They are parked as of 2026-06-08 when the product direction shifted to single-player daily challenges. The designs are retained for reference. Multiplayer infrastructure (WebSocket, matchmaking, ranked play) is also parked.

### 3. Head to Head — Standard

The existing game design. One question per game. Both players draw from the same answer pool. Deep strategic play: answer management, reading the opponent, holding back high-value players for the right moment.

Parked with multiplayer.

### 4. Head to Head — Rapid Fire

**Concept**: The question changes every dart. Each turn, a brand new question is drawn.

**Key differences from Standard**:
- You only need **one good answer** per question — not a full squad's worth of knowledge.
- Obscure/niche questions become **fun rather than frustrating**.
- **No answer pool depletion** between players — each player faces a fresh question on their turn.
- The strategic question shifts from "who do I play?" to "what score do I *want* to hit this turn?"

**Open design question — shared or independent questions?**
- **Option A (Shared)**: Both players answer the same question in a round. More head-to-head feel.
- **Option B (Independent)**: Each dart draws its own question. More chaotic, faster.

Decision not made — to be resolved if this mode is ever revived.

Parked with multiplayer.

### 5. Draft Mode

**Concept**: Before each turn, 3 questions are shown. The active player picks one.

Adds mind-games: pick the question your opponent is weakest on. Creates a metagame of question awareness on top of the answer game.

Parked with multiplayer.

### 6. Category Lock

**Concept**: Players agree on a category at match start (e.g. "La Liga goals only"). All questions draw exclusively from that pool.

Great for themed nights, rivalry matches, or players who want to specialise.

Parked with multiplayer.

### 7. Blind Mode

**Concept**: The question is hidden until it's your turn. No pre-thinking between turns.

Tests instinct over preparation. Could combine with Rapid Fire for maximum chaos.

Parked with multiplayer.

### 8. Tournament / League

Structured brackets or league tables built on top of the existing head-to-head system. Potentially mixes modes within a series.

Parked with multiplayer.

---

## Planned (Single-Player) — Not Yet Built

### Async Friend Challenges

**Status**: P2 stretch goal

**Concept**: Play the same daily challenge question as a friend and compare results. Not real-time — each player plays independently, then sees the other's result when both have finished.

**Social mechanic**: "I played today's Football daily, scored 87. Can you beat that?" Share links deep-link into the challenge. If the recipient plays within 24 hours, results are compared side-by-side with the same emoji-grid format.

**Why it matters for retention**: Wordle spread through share links. Async challenges are the same mechanic — each shared result is organic acquisition. No friend system, no social graph, just share links.

**Dependencies**: Share links + OG metadata (P1), player profiles for result comparison (P1).

---

## Architectural Guardrails

These are **small, cheap decisions** to make now that keep future options open. None require building the stretch features.

**Active** (serve current modes):
| Guardrail | Status | Why it matters |
|---|---|---|
| `game_mode VARCHAR` on `matches`, default `'STANDARD'` | Done (V19) | Distinguishes Daily Challenge from Free Play. Already in use. |
| `suitable_for_daily BOOLEAN` on `questions` | Done (V19) | Explicit Daily Challenge pool flag. |
| `difficulty_score NUMERIC(4,2)` on `questions` | Done (V13) | Continuous 0-10 scale. Used for daily challenge pool filtering. |
| Question draw logic in a dedicated service method | Active | Not yet extracted — inline in `GameService`. Do this when touching draw logic next. |
| Mode label/badge component | Active | Give the UI a place to display "Daily Challenge" vs "Free Play" vs future modes. |

**Parked** (multiplayer-only, no urgency):
| Guardrail | Why it matters |
|---|---|
| Store `question_id` on `game_moves` (not just on `games`) | Rapid Fire needs per-move question tracking. Parked with multiplayer. |
| Pass `gameMode` through game engine context from the start | Mode-specific rules. Already done — `game_mode` is on the `matches` row. |

---

## What "Parked" Means for Day-to-Day Development

- **Don't implement the parked features**. They may return as a future phase but have no timeline.
- **Do follow the active guardrails**. They are cheap and serve the current modes.
- **The parked guardrails are not urgent**. Don't add `question_id` to `game_moves` just because this doc says so — only when you're touching `game_moves` for another reason.
- If a decision would violate an active guardrail, it's worth a short conversation before committing.

---

## Revisit Trigger

Come back to this document when:
- Daily active users are consistent and there's appetite for more replayability -> **Free Play custom rules**
- Share link adoption is high and players ask for more social features -> **Async friend challenges**
- A real business case emerges for multiplayer -> **Revive the parked multiplayer modes**
