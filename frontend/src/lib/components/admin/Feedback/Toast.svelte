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
    <div class="icon">
        {#if type === 'success'}✅{:else if type === 'error'}❌{:else}ℹ️{/if}
    </div>
    <div class="content">{message}</div>
    <button class="close-btn" on:click={() => dispatch('close')} aria-label="Close">&times;</button>
</div>

<style>
    .toast {
        position: fixed;
        top: var(--space-lg);
        right: var(--space-lg);
        padding: var(--space-md) var(--space-lg);
        border-radius: var(--radius-md);
        color: white;
        display: flex;
        align-items: center;
        gap: var(--space-md);
        box-shadow: var(--shadow-3);
        z-index: 2000;
        min-width: 320px;
        max-width: 450px;
        border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .success {
        background: #00522b;
        border-left: 6px solid var(--color-primary);
    }

    .error {
        background: #93000a;
        border-left: 6px solid var(--color-error);
    }

    .info {
        background: #004a77;
        border-left: 6px solid #7fcfff;
    }

    .icon {
        font-size: 1.25rem;
    }

    .content {
        flex: 1;
        font-weight: 600;
        font-size: 0.95rem;
    }

    .close-btn {
        background: none;
        border: none;
        color: rgba(255, 255, 255, 0.7);
        font-size: 1.5rem;
        cursor: pointer;
        padding: var(--space-xs);
        line-height: 1;
        transition: color 0.2s;
    }

    .close-btn:hover {
        color: white;
    }
</style>
