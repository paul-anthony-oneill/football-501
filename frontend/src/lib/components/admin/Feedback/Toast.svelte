<script lang="ts">
    import { onMount, createEventDispatcher } from 'svelte';
    import { fade, fly } from 'svelte/transition';

    export let message = '';
    export let type: 'success' | 'error' | 'info' = 'info';
    export let duration = 3000;

    const dispatch = createEventDispatcher();

    onMount(() => {
        if (duration > 0) {
            const timer = setTimeout(() => {
                dispatch('close');
            }, duration);
            return () => clearTimeout(timer);
        }
    });
</script>

<div class="toast {type}" transition:fly={{ y: -50, duration: 300 }}>
    <div class="content">{message}</div>
    <button on:click={() => dispatch('close')}>&times;</button>
</div>

<style>
    .toast {
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        color: #fff;
        display: flex;
        align-items: center;
        gap: 1rem;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        z-index: 2000;
        min-width: 300px;
    }

    .success {
        background: #059669;
        border-left: 4px solid #34d399;
    }

    .error {
        background: #dc2626;
        border-left: 4px solid #f87171;
    }

    .info {
        background: #2563eb;
        border-left: 4px solid #60a5fa;
    }

    .content {
        flex: 1;
    }

    button {
        background: none;
        border: none;
        color: rgba(255, 255, 255, 0.8);
        font-size: 1.25rem;
        cursor: pointer;
        padding: 0;
    }

    button:hover {
        color: #fff;
    }
</style>
