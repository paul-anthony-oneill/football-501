<script lang="ts">
    import { createEventDispatcher } from 'svelte';
    import { fade, scale } from 'svelte/transition';

    export let open = false;
    export let title = 'Confirm';
    export let message = 'Are you sure you want to proceed?';
    export let confirmText = 'Confirm';
    export let cancelText = 'Cancel';
    export let type: 'danger' | 'info' = 'danger';

    const dispatch = createEventDispatcher();

    function onConfirm() {
        dispatch('confirm');
    }

    function onCancel() {
        dispatch('cancel');
    }
</script>

{#if open}
    <div class="backdrop" on:click={onCancel} transition:fade>
        <div class="modal" on:click|stopPropagation transition:scale>
            <h3>{title}</h3>
            <p>{message}</p>
            <div class="actions">
                <button class="cancel-btn" on:click={onCancel}>{cancelText}</button>
                <button class="confirm-btn {type}" on:click={onConfirm}>{confirmText}</button>
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
        padding: 2rem;
        border-radius: 12px;
        width: 90%;
        max-width: 400px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    }

    h3 {
        margin: 0 0 1rem 0;
        color: #fff;
    }

    p {
        color: #d1d5db;
        margin-bottom: 2rem;
        line-height: 1.5;
    }

    .actions {
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

    .confirm-btn.danger {
        background: #ef4444;
        color: #fff;
    }

    .confirm-btn.info {
        background: #3b82f6;
        color: #fff;
    }

    button:hover {
        opacity: 0.9;
    }
</style>
