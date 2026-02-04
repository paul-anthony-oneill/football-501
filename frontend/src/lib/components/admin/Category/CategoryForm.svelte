<script lang="ts">
    import TextField from '$lib/components/admin/Form/TextField.svelte';
    import TextArea from '$lib/components/admin/Form/TextArea.svelte';
    import type { Category, CreateCategoryRequest } from '$lib/types/admin';

    export let category: Category | undefined = undefined;
    export let loading = false;

    let name = category?.name || '';
    let slug = category?.slug || '';
    let description = category?.description || '';
    let errors: Record<string, string> = {};

    $: isEdit = !!category;

    function validate() {
        errors = {};
        if (!name.trim()) errors.name = 'Name is required';
        if (!isEdit && !slug.trim()) errors.slug = 'Slug is required';
        if (!isEdit && slug && !/^[a-z0-9-]+$/.test(slug)) {
            errors.slug = 'Slug must contain only lowercase letters, numbers, and hyphens';
        }
        return Object.keys(errors).length === 0;
    }

    // Auto-generate slug from name if creating
    function handleNameInput() {
        if (!isEdit && !category && name) {
            slug = name.toLowerCase()
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-|-$/g, '');
        }
    }

    import { createEventDispatcher } from 'svelte';
    const dispatch = createEventDispatcher();

    function handleSubmit() {
        if (!validate()) return;

        const data = {
            name,
            description,
            ...(isEdit ? {} : { slug })
        };

        dispatch('submit', data);
    }
</script>

<form on:submit|preventDefault={handleSubmit}>
    <TextField
        label="Name"
        bind:value={name}
        required
        error={errors.name}
        on:input={handleNameInput}
        disabled={loading}
    />

    <TextField
        label="Slug"
        bind:value={slug}
        required={!isEdit}
        disabled={isEdit || loading}
        error={errors.slug}
        placeholder="e.g. premier-league"
    />

    <TextArea
        label="Description"
        bind:value={description}
        disabled={loading}
        rows={3}
    />

    <div class="actions">
        <button type="button" class="cancel-btn" on:click={() => dispatch('cancel')} disabled={loading}>
            Cancel
        </button>
        <button type="submit" class="submit-btn" disabled={loading}>
            {loading ? 'Saving...' : (isEdit ? 'Update Category' : 'Create Category')}
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
