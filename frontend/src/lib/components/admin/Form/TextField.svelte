<script lang="ts">
    export let label: string;
    export let value: string | number;
    export let type = 'text';
    export let placeholder = '';
    export let required = false;
    export let disabled = false;
    export let error = '';
</script>

<div class="form-group">
    <label>
        {label}
        {#if required}<span class="required">*</span>{/if}
    </label>
    <div class="input-container">
        <input
            {type}
            bind:value
            {placeholder}
            {disabled}
            class:has-error={!!error}
        />
        {#if error}
            <span class="error-text">{error}</span>
        {/if}
    </div>
</div>

<style>
    .form-group {
        display: flex;
        flex-direction: column;
        gap: var(--space-xs);
        margin-bottom: var(--space-md);
    }

    label {
        color: var(--color-on-surface-variant);
        font-size: 0.85rem;
        font-weight: 600;
        letter-spacing: 0.5px;
    }

    .required {
        color: var(--color-error);
        margin-left: 0.2rem;
    }

    .input-container {
        display: flex;
        flex-direction: column;
        gap: var(--space-xs);
    }

    input {
        width: 100%;
        box-sizing: border-box;
        padding: var(--space-md);
        background: var(--color-surface);
        border: 1px solid var(--color-outline);
        border-radius: var(--radius-sm);
        color: var(--color-on-surface);
        font-size: 1rem;
        transition: all 0.2s;
    }

    input:focus {
        outline: none;
        border-color: var(--color-primary);
        border-width: 2px;
        padding: calc(var(--space-md) - 1px); /* Compensate for thicker border */
        box-shadow: 0 0 0 4px rgba(74, 222, 128, 0.1);
    }

    input.has-error {
        border-color: var(--color-error);
    }

    .error-text {
        color: var(--color-error);
        font-size: 0.75rem;
        font-weight: 500;
    }

    input:disabled {
        opacity: 0.5;
        background: #252525;
    }
</style>
