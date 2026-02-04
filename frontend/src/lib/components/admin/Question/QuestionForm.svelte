<script lang="ts">
    import TextField from '$lib/components/admin/Form/TextField.svelte';
    import TextArea from '$lib/components/admin/Form/TextArea.svelte';
    import Select from '$lib/components/admin/Form/Select.svelte';
    import type { Question, Category } from '$lib/types/admin';
    import { createEventDispatcher } from 'svelte';

    export let question: Question | undefined = undefined;
    export let categories: Category[] = [];
    export let loading = false;

    let categoryId = question?.categoryId || '';
    let questionText = question?.questionText || '';
    let metricKey = question?.metricKey || '';
    let minScore = question?.minScore?.toString() || '';
    let configJson = question?.config ? JSON.stringify(question.config, null, 2) : '{}';
    
    let errors: Record<string, string> = {};

    const dispatch = createEventDispatcher();

    function validate() {
        errors = {};
        if (!categoryId) errors.categoryId = 'Category is required';
        if (!questionText.trim()) errors.questionText = 'Question text is required';
        if (!metricKey.trim()) errors.metricKey = 'Metric key is required';
        
        try {
            JSON.parse(configJson);
        } catch (e) {
            errors.config = 'Invalid JSON format';
        }

        if (minScore && isNaN(Number(minScore))) {
            errors.minScore = 'Must be a number';
        }

        return Object.keys(errors).length === 0;
    }

    function handleSubmit() {
        if (!validate()) return;

        const data = {
            categoryId,
            questionText,
            metricKey,
            minScore: minScore ? Number(minScore) : undefined,
            config: JSON.parse(configJson)
        };

        dispatch('submit', data);
    }

    // Transform categories for Select component
    $: categoryOptions = [
        { value: '', label: 'Select a Category' },
        ...categories.map(c => ({ value: c.id, label: c.name }))
    ];
</script>

<form on:submit|preventDefault={handleSubmit}>
    <Select
        label="Category"
        bind:value={categoryId}
        options={categoryOptions}
        required
        error={errors.categoryId}
        disabled={loading}
    />

    <TextArea
        label="Question Text"
        bind:value={questionText}
        required
        rows={2}
        error={errors.questionText}
        disabled={loading}
        placeholder="e.g. Which player has the most Premier League appearances?"
    />

    <TextField
        label="Metric Key"
        bind:value={metricKey}
        required
        error={errors.metricKey}
        disabled={loading}
        placeholder="e.g. appearances"
    />

    <TextField
        label="Minimum Score (Optional)"
        bind:value={minScore}
        type="number"
        error={errors.minScore}
        disabled={loading}
        placeholder="0"
    />

    <TextArea
        label="Configuration (JSON)"
        bind:value={configJson}
        rows={5}
        error={errors.config}
        disabled={loading}
        placeholder="JSON object"
    />

    <div class="actions">
        <button type="button" class="cancel-btn" on:click={() => dispatch('cancel')} disabled={loading}>
            Cancel
        </button>
        <button type="submit" class="submit-btn" disabled={loading}>
            {loading ? 'Saving...' : (question ? 'Update Question' : 'Create Question')}
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
