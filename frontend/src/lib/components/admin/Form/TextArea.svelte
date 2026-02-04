<script lang="ts">
    export let label: string;
    export let value: string;
    export let placeholder = '';
    export let required = false;
    export let disabled = false;
    export let rows = 3;
    export let error = '';
</script>

<div class="form-group">
    <label>
        {label}
        {#if required}<span class="required">*</span>{/if}
    </label>
    <textarea
        bind:value
        {placeholder}
        {disabled}
        {rows}
        class:has-error={!!error}
    ></textarea>
    {#if error}
        <span class="error-text">{error}</span>
    {/if}
</div>

<style>
    .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-bottom: 1rem;
    }

    label {
        color: #d1d5db;
        font-size: 0.9rem;
    }

    .required {
        color: #ef4444;
        margin-left: 0.2rem;
    }

    textarea {
        padding: 0.75rem;
        background: #1a1a1a;
        border: 1px solid #444;
        border-radius: 6px;
        color: #fff;
        font-size: 1rem;
        resize: vertical;
        font-family: inherit;
    }

    textarea:focus {
        outline: none;
        border-color: #4ade80;
    }

    textarea.has-error {
        border-color: #ef4444;
    }

    .error-text {
        color: #ef4444;
        font-size: 0.8rem;
    }

    textarea:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
</style>
