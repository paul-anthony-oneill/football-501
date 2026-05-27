# Frontend Design & Architecture (React)

**Version**: 2.1  
**Last Updated**: 2026-05-27  
**Status**: Implemented — Lobby, Teletext Theme, Admin Page Decomposition, useGameLoop hook

---

## Overview

The Football 501 frontend has been overhauled to support a high-impact, themeable visual language. The architecture moves away from a standard "app" feel towards an immersive, cinematic experience that changes based on the game context.

## 1. Visual Identity & Themes

The project uses a **multi-tier theming system** powered by Tailwind v4 and CSS variables.

### Themes

| Theme | Selector | Context | Aesthetic |
|-------|----------|---------|-----------|
| **Home/Lobby** | `.theme-home` | Landing, Category Selection | Neutral, Typographic, Modern |
| **Teletext** | `.theme-teletext` | Game Mode | Retro, 8-bit, High Contrast |
| **Vinyl** | `.theme-vinyl` | Game Mode | 70s Record Sleeve, Warm (Planned) |
| **Big Screen** | `.theme-bigscreen` | Game Mode | Cinema Gold, Serif, Dramatic (Planned) |

### Typography (`next/font`)

| Font Family | Usage |
|-------------|-------|
| **Bricolage Grotesque** | Main Hero (501), Headers |
| **Hanken Grotesk** | Body text, Neutral UI |
| **IBM Plex Mono** | Labels, Technical data, Kickers |
| **VT323** | Teletext Theme (Retro Monospace) |

---

## 2. Technical Architecture

### CSS Infrastructure (Tailwind v4)

We utilize the `@theme inline` block in `globals.css` to define our core design tokens. 

```css
/* Example Token Usage */
.h-hero-num {
  @apply font-bricolage font-extrabold text-[200px] leading-[0.85];
  font-variation-settings: 'wdth' 80;
}
```

Key features implemented:
- **Dynamic Theming**: Switching the `body` class dynamically swaps all colors and fonts globally.
- **Trajectory Animation**: A custom SVG-based animation (`animate-draw`) anchors the Home screen.

### Component Structure

The frontend is modularized to separate the lobby logic from the active match logic, and to keep admin components focused on single concerns. The admin question detail page was decomposed in Phase 3 from 622 lines to 113 lines.

```
src/
├── app/
│   ├── page.tsx                    (97 lines — lobby state only; uses useGameLoop)
│   ├── globals.css                 (Theme Definitions)
│   └── admin/
│       ├── questions/
│       │   └── [id]/page.tsx       (113 lines — thin coordinator; uses useQuestionDetail)
│       └── categories/
├── components/
│   ├── game/
│   │   ├── lobby/
│   │   │   └── LobbyView.tsx       (Lobby & Mode Selection)
│   │   ├── match/
│   │   │   └── MatchView.tsx       (Game UI & Scoreboard)
│   │   └── EntitySearch.tsx        (Theme-aware Autocomplete)
│   └── admin/
│       ├── AnswerTableSection.tsx  (Answers table rendering)
│       ├── QuestionMetaPanel.tsx   (Question metadata form fields)
│       └── AnswerPreview.tsx       (Individual answer row rendering)
└── hooks/
    ├── useGameLoop.ts              (Game session state + API calls)
    └── useQuestionDetail.ts        (Admin question detail state + mutations)
```

---

## 3. Implementation Patterns

### State Management — Custom Hooks, Not Zustand

All game and admin state is managed through custom React hooks backed by `useState`. This matches the architecture decision in `CLAUDE.md`: no Redux or Zustand. A custom hook is sufficient and consistent with the React Context pattern already in use.

**`useGameLoop`** (`src/hooks/useGameLoop.ts`)  
Owns the entire game session. Exposes typed state and three actions:

| State field | Type | Description |
|---|---|---|
| `score` | `number` | Current score (starts at 501) |
| `question` | `string` | Active question text |
| `turnCount` | `number` | Turns taken |
| `gameStatus` | `GameStatus` | `NOT_STARTED \| IN_PROGRESS \| COMPLETED` |
| `moves` | `Move[]` | History, newest first |
| `entityType` | `string` | Autocomplete pool (e.g. `"footballer"`) |
| `hints` | `GameHints \| null` | In-game hint stats |

| Action | Signature | Description |
|---|---|---|
| `startNewGame` | `(categorySlug: string) => Promise<void>` | POST to `/api/practice/start` |
| `submitAnswer` | `(answer: string) => Promise<void>` | POST to `/api/practice/games/{id}/submit` |
| `exitGame` | `() => void` | Resets state, switches theme back to home |

WebSocket lifecycle will be added inside `useGameLoop` when multiplayer is implemented — `page.tsx` will not need to change.

**`useQuestionDetail`** (`src/hooks/useQuestionDetail.ts`)  
Owns the admin question detail workflow: fetching question data, form state for metadata edits, bulk answer import, and mutations. The `questions/[id]/page.tsx` component calls this hook and passes the returned values to focused sub-components.

### Theme Switching

Theme transitions are owned by `useGameLoop`, not by `page.tsx`. The hook applies the body class change as a side-effect of `startNewGame` and `exitGame`:

```typescript
// Inside useGameLoop — startNewGame
document.body.classList.remove("theme-home");
document.body.classList.add("theme-teletext");

// Inside useGameLoop — exitGame
document.body.classList.remove("theme-teletext");
document.body.classList.add("theme-home");
```

`page.tsx` only reads `gameStatus` from the hook and conditionally renders `LobbyView` or `MatchView`.

### Theme-Aware Components

Components like `EntitySearch` are designed to inherit styles from the parent theme. For example, in Teletext mode, the autocomplete list appears *above* the input and uses retro blue/white styling.

---

## 4. Design Assets

The original design handoff files are documented in the `docs/design` folder for reference.
- **Home Trajectory**: `M-100,600 C300,500 800,800 1540,100` (SVG Path)
- **Teletext Palette**: Cyan (#00ffff), Magenta (#ff00ff), Yellow (#ffff00), Green (#00ff00).

---

## 5. Future Implementation Guide

To add a new theme (e.g., Vinyl):
1.  Add the font to `layout.tsx` and `globals.css`.
2.  Define the `.theme-vinyl` variables in `globals.css`.
3.  Add the theme mapping in `LobbyView.tsx` (card stripes).
4.  Update `MatchView.tsx` to conditionally apply styles or separate into sub-components if the layout differs significantly.
