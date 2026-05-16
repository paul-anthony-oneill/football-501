---
name: sveltekit-dev-guidelines
description: Frontend development guidelines for SvelteKit/TypeScript/Tailwind CSS applications. Covers component patterns, data fetching, file organization, stores, routing, loading states, performance, and TypeScript best practices for the Football-501 SvelteKit PWA.
---

# SvelteKit Development Guidelines

## Purpose

Establish consistent patterns for the Football-501 SvelteKit frontend (PWA) — data fetching, component structure, Svelte stores, routing, Tailwind styling, and TypeScript best practices.

## When to Use This Skill

- Creating new Svelte components or pages
- Fetching data from the Spring Boot API
- Setting up SvelteKit routes
- Managing reactive state with Svelte stores
- Styling with Tailwind CSS
- Implementing WebSocket connections (STOMP)
- TypeScript best practices

---

## Quick Start

### New Component Checklist

- [ ] Use `<script lang="ts">` for TypeScript
- [ ] Keep component under 300 lines — extract sub-components if larger
- [ ] Use Svelte stores for shared state
- [ ] Use `{#await}` blocks for async data — no manual `isLoading` flags
- [ ] Tailwind for all styling — no inline styles
- [ ] Export props with TypeScript types
- [ ] Handle loading and error states explicitly

### New Feature Checklist

- [ ] Create `src/lib/features/{feature-name}/` directory
- [ ] Create subdirectories: `api/`, `components/`, `stores/`, `types/`
- [ ] API service: `api/{feature}Api.ts`
- [ ] Types: `types/index.ts`
- [ ] Export from `index.ts`
- [ ] Add SvelteKit route: `src/routes/{feature}/+page.svelte`

---

## File Organization

```
src/
  lib/
    features/
      game/           # Game engine UI
        api/          # API calls to Spring Boot
        components/   # Game-specific components
        stores/       # Svelte stores for game state
        types/        # TypeScript interfaces
        index.ts      # Public exports
      auth/           # Authentication
      matchmaking/    # Queue & lobby
      leaderboard/    # Rankings
    components/       # Truly reusable components
      LoadingSpinner.svelte
      ErrorMessage.svelte
    stores/           # Global stores (user, websocket)
    utils/            # Utility functions
    types/            # Shared TypeScript types
  routes/
    +layout.svelte    # Root layout
    +page.svelte      # Home
    game/
      +page.svelte    # Active game view
    matchmaking/
      +page.svelte    # Matchmaking lobby
```

---

## Component Patterns

### Basic Component Structure

```svelte
<script lang="ts">
  import type { Player } from '$lib/types';

  export let player: Player;
  export let onSelect: (player: Player) => void = () => {};
</script>

<div class="flex items-center gap-2 p-3 rounded-lg bg-white shadow">
  <span class="font-semibold text-gray-800">{player.name}</span>
  <button
    class="ml-auto px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
    on:click={() => onSelect(player)}
  >
    Select
  </button>
</div>
```

### Loading States — Use `{#await}` Blocks

```svelte
<script lang="ts">
  import { fetchQuestion } from '$lib/features/game/api/gameApi';

  const questionPromise = fetchQuestion(questionId);
</script>

{#await questionPromise}
  <LoadingSpinner />
{:then question}
  <QuestionCard {question} />
{:catch error}
  <ErrorMessage message={error.message} />
{/await}
```

**NEVER use manual isLoading flags:**
```svelte
<!-- NEVER -->
{#if isLoading}
  <LoadingSpinner />
{:else}
  <Content />
{/if}
```

---

## Data Fetching — API Service Layer

Create `src/lib/features/{feature}/api/{feature}Api.ts`:

```typescript
// src/lib/features/game/api/gameApi.ts
const BASE_URL = '/api';

export const gameApi = {
  async getQuestion(questionId: string) {
    const res = await fetch(`${BASE_URL}/questions/${questionId}`, {
      credentials: 'include',
    });
    if (!res.ok) throw new Error(`Failed to fetch question: ${res.status}`);
    return res.json();
  },

  async submitAnswer(gameId: string, playerName: string) {
    const res = await fetch(`${BASE_URL}/games/${gameId}/answers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ playerName }),
    });
    if (!res.ok) throw new Error(`Answer rejected: ${res.status}`);
    return res.json();
  },
};
```

**Rules:**
- Always use `credentials: 'include'` for JWT cookie auth
- Throw errors on non-OK responses
- Type all responses
- API routes: `/api/games`, `/api/questions`, `/api/users` (proxied to Spring Boot)

---

## Svelte Stores

Use stores for shared reactive state:

```typescript
// src/lib/stores/gameStore.ts
import { writable, derived } from 'svelte/store';
import type { GameState } from '$lib/types';

export const gameState = writable<GameState | null>(null);

export const currentScore = derived(gameState, ($game) => ({
  player1: $game?.player1Score ?? 501,
  player2: $game?.player2Score ?? 501,
}));

export function updateScore(score: number) {
  gameState.update((state) => {
    if (!state) return null;
    return { ...state, player1Score: score };
  });
}
```

---

## WebSocket (STOMP)

```typescript
// src/lib/stores/websocketStore.ts
import { Client } from '@stomp/stompjs';
import { writable } from 'svelte/store';
import type { GameMessage } from '$lib/types';

export const wsConnected = writable(false);

let stompClient: Client;

export function connectToGame(gameId: string, token: string) {
  stompClient = new Client({
    brokerURL: `wss://api.football501.com/ws?token=${token}`,
    onConnect: () => wsConnected.set(true),
    onDisconnect: () => wsConnected.set(false),
  });

  stompClient.subscribe(`/topic/game/${gameId}`, (msg) => {
    const message: GameMessage = JSON.parse(msg.body);
    handleGameMessage(message);
  });

  stompClient.activate();
}

export function disconnect() {
  stompClient?.deactivate();
  wsConnected.set(false);
}
```

---

## Routing (SvelteKit)

Use SvelteKit file-based routing:

```
src/routes/
  +layout.svelte         # Shared layout with nav
  +page.svelte           # Home/dashboard
  game/
    +page.svelte         # Active game
    +page.ts             # Load game data (server/client)
  matchmaking/
    +page.svelte
  leaderboard/
    +page.svelte
    +page.ts
```

### Page load data:
```typescript
// src/routes/game/+page.ts
import type { PageLoad } from './$types';
import { gameApi } from '$lib/features/game/api/gameApi';

export const load: PageLoad = async ({ fetch, params }) => {
  const game = await gameApi.getActiveGame();
  return { game };
};
```

---

## Tailwind CSS Patterns

```svelte
<!-- Responsive layout -->
<div class="flex flex-col md:flex-row gap-4 p-4">

<!-- Game score display -->
<div class="text-6xl font-bold tabular-nums text-center py-8">
  {score}
</div>

<!-- Button variants -->
<button class="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium transition-colors">
  Submit Answer
</button>

<!-- Card -->
<div class="bg-white rounded-xl shadow-md p-6">
  <h2 class="text-xl font-bold text-gray-900 mb-4">{title}</h2>
</div>
```

**Rules:**
- Use Tailwind classes — no inline styles, no CSS-in-JS
- Responsive prefixes: `sm:`, `md:`, `lg:`
- Dark mode: `dark:` prefix when needed

---

## TypeScript Standards

```typescript
// Types: always explicit
interface GameState {
  gameId: string;
  player1Score: number;
  player2Score: number;
  currentTurn: 'player1' | 'player2';
  status: 'active' | 'completed' | 'abandoned';
}

// Use $lib imports (never relative paths beyond one level)
import type { GameState } from '$lib/types';
import { gameApi } from '$lib/features/game/api/gameApi';

// Strict: no `any`, explicit return types on exported functions
export function calculateBust(score: number): boolean {
  return score > 180 || score < -10;
}
```

---

## Core Principles

1. **`{#await}` for async** — no manual loading flags
2. **Svelte stores for shared state** — not prop-drilling
3. **Tailwind only** — no inline styles
4. **`$lib/` imports** — use path aliases
5. **`credentials: 'include'`** — for all authenticated API calls
6. **Types first** — define interfaces before writing logic
7. **Feature directories** — group by domain, not by type
