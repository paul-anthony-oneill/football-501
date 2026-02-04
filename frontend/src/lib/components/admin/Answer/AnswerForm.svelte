<script lang="ts">
    import TextField from '$lib/components/admin/Form/TextField.svelte';
    import TextArea from '$lib/components/admin/Form/TextArea.svelte';
    import type { Answer, CreateAnswerRequest } from '$lib/types/admin';
    import { createEventDispatcher } from 'svelte';

    export let answer: Answer | undefined = undefined;
    export let loading = false;

    let displayText = answer?.displayText || '';
    let score = answer?.score?.toString() || '';
    let metadataJson = answer?.metadata ? JSON.stringify(answer.metadata, null, 2) : '{}';
    
    let errors: Record<string, string> = {};

    const dispatch = createEventDispatcher();

    function validate() {
        errors = {};
        if (!displayText.trim()) errors.displayText = 'Display text is required';
        if (!score) {
            errors.score = 'Score is required';
        } else {
            const numScore = Number(score);
            if (isNaN(numScore) || numScore < 1 || numScore > 300) {
                errors.score = 'Score must be between 1 and 300';
            }
        }
        
        try {
            JSON.parse(metadataJson);
        } catch (e) {
            errors.metadata = 'Invalid JSON format';
        }

        return Object.keys(errors).length === 0;
    }

    function handleSubmit() {
        if (!validate()) return;

        const data: CreateAnswerRequest = {
            displayText,
            score: Number(score),
            metadata: JSON.parse(metadataJson)
        };

        dispatch('submit', data);
    }
</script>

<form on:submit|preventDefault={handleSubmit}>
    <TextField
        label="Display Text"
        bind:value={displayText}
        required
        error={errors.displayText}
        disabled={loading}
        placeholder="e.g. Erling Haaland"
    />

    <TextField
        label="Score"
        bind:value={score}
        type="number"
        required
        error={errors.score}
        disabled={loading}
        placeholder="e.g. 52"
    />

    <TextArea
        label="Metadata (JSON)"
        bind:value={metadataJson}
        rows={3}
        error={errors.metadata}
        disabled={loading}
        placeholder="JSON object"
    />

    <div class="actions">
        <button type="button" class="cancel-btn" on:click={() => dispatch('cancel')} disabled={loading}>
            Cancel
        </button>
        <button type="submit" class="submit-btn" disabled={loading}>
            {loading ? 'Saving...' : (answer ? 'Update Answer' : 'Create Answer')}
        </button>
    </div>
</form>

<style>
    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 1rem;
        margin-top: 2rem;
        padding-top: 1rem;
        border-top: 1px solid #444;
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

    .submit-btn {
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
