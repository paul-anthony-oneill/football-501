<script lang="ts">
    import { createEventDispatcher } from 'svelte';
    import { fade, scale } from 'svelte/transition';

    export let open = false;
    export let title = '';
    export let loading = false;
    export let saveText = 'Save';
    export let cancelText = 'Cancel';

    const dispatch = createEventDispatcher();

    function onSave() {
        dispatch('save');
    }

    function onCancel() {
        dispatch('cancel');
    }
</script>

{#if open}
    <div class="backdrop" on:click={onCancel} transition:fade>
        <div class="modal" on:click|stopPropagation transition:scale>
            <div class="header">
                <h3>{title}</h3>
                <button class="close-btn" on:click={onCancel}>&times;</button>
            </div>
            
            <div class="content">
                <slot />
            </div>

            <div class="footer">
                <button class="cancel-btn" on:click={onCancel} disabled={loading}>{cancelText}</button>
                <button class="save-btn" on:click={onSave} disabled={loading}>
                    {loading ? 'Saving...' : saveText}
                </button>
            </div>
        </div>
    </div>
{/if}

<style>
    .backdrop {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    }

    .modal {
        background: #2a2a2a;
        border-radius: 12px;
        width: 90%;
        max-width: 600px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    }

    .header {
        padding: 1.5rem;
        border-bottom: 1px solid #444;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    h3 {
        margin: 0;
        color: #fff;
    }

    .close-btn {
        background: none;
        border: none;
        color: #9ca3af;
        font-size: 1.5rem;
        cursor: pointer;
        padding: 0;
    }

    .content {
        padding: 1.5rem;
        overflow-y: auto;
    }

    .footer {
        padding: 1.5rem;
        border-top: 1px solid #444;
        display: flex;
        justify-content: flex-end;
        gap: 1rem;
    }

    button {
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        border: none;
        cursor: pointer;
        font-weight: 500;
        font-size: 1rem;
    }

    .cancel-btn {
        background: #444;
        color: #fff;
    }

    .save-btn {
        background: #4ade80;
        color: #000;
    }

    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    button:hover:not(:disabled) {
        opacity: 0.9;
    }
</style>
