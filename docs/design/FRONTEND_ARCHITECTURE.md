# Frontend Design & Architecture (React)

**Version**: 2.0
**Last Updated**: 2026-05-25
**Status**: Implemented (Lobby + Teletext Theme)

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

The frontend has been modularized to separate the lobby logic from the active match logic.

```
src/
├── app/
│   ├── page.tsx (Main Orchestrator)
│   └── globals.css (Theme Definitions)
├── components/
│   ├── game/
│   │   ├── lobby/
│   │   │   └── LobbyView.tsx (Lobby & Mode Selection)
│   │   ├── match/
│   │   │   └── MatchView.tsx (Game UI & Scoreboard)
│   │   └── EntitySearch.tsx (Theme-aware Autocomplete)
```

---

## 3. Implementation Patterns

### Theme Switching

The orchestrator (`page.tsx`) handles the transition between Home and Game by updating the `gameStatus` and manipulating the body class.

```typescript
function startNewGame() {
  setGameStatus("IN_PROGRESS");
  document.body.classList.remove('theme-home');
  document.body.classList.add('theme-teletext');
}
```

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
