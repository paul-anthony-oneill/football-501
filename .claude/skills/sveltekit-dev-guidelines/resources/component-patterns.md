# SvelteKit Component Patterns

## Component Structure

Every Svelte component follows this structure:

```svelte
<script lang="ts">
  // 1. Imports
  import type { Props } from './types';
  import OtherComponent from './OtherComponent.svelte';

  // 2. Props (exported)
  export let data: Props;
  export let onAction: () => void = () => {};

  // 3. Local state
  let inputValue = '';

  // 4. Reactive declarations
  $: isValid = inputValue.length > 2;

  // 5. Functions
  function handleSubmit() {
    if (isValid) onAction();
  }
</script>

<!-- Template -->
<div class="...">
  <OtherComponent />
</div>

<style>
  /* Only for styles not possible with Tailwind */
</style>
```

## Game-Specific Components

### Score Display
```svelte
<script lang="ts">
  export let score: number;
  export let isBust: boolean = false;
</script>

<div class="text-center {isBust ? 'text-red-600' : 'text-gray-900'}">
  <span class="text-6xl font-bold tabular-nums">{score}</span>
  {#if isBust}
    <p class="text-sm font-medium mt-1">BUST!</p>
  {/if}
</div>
```

### Timer Bar
```svelte
<script lang="ts">
  import { onDestroy } from 'svelte';

  export let seconds: number;
  export let onTimeout: () => void = () => {};

  let remaining = seconds;
  const interval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(interval);
      onTimeout();
    }
  }, 1000);

  onDestroy(() => clearInterval(interval));

  $: percentage = (remaining / seconds) * 100;
  $: colorClass = percentage > 50 ? 'bg-green-500' : percentage > 25 ? 'bg-yellow-500' : 'bg-red-500';
</script>

<div class="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
  <div
    class="h-full transition-all duration-1000 {colorClass}"
    style="width: {percentage}%"
  />
</div>
<p class="text-center text-sm text-gray-600 mt-1">{remaining}s</p>
```
