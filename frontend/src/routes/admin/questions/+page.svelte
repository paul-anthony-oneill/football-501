<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { adminApi } from '$lib/api/admin';
    import type { Question, Category } from '$lib/types/admin';
    import DataTable from '$lib/components/admin/Table/DataTable.svelte';
    import Select from '$lib/components/admin/Form/Select.svelte';
    import ConfirmDialog from '$lib/components/admin/Modal/ConfirmDialog.svelte';
    import Toast from '$lib/components/admin/Feedback/Toast.svelte';

    let questions: Question[] = [];
    let categories: Category[] = [];
    let loading = true;
    let toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    // Filters
    let selectedCategoryId = '';
    let selectedActiveStatus: string | boolean = ''; // '' for all, true/false for specific

    // Pagination
    let totalElements = 0;
    let pageSize = 10;
    let currentPage = 0;

    // Delete dialog
    let showDeleteDialog = false;
    let selectedQuestion: Question | null = null;
    let actionLoading = false;

    const columns = [
        { key: 'questionText', label: 'Question' },
        { key: 'categoryName', label: 'Category' },
        { key: 'answerCount', label: 'Answers' },
        { key: 'isActive', label: 'Active' },
    ];

    onMount(async () => {
        await Promise.all([loadCategories(), loadQuestions()]);
    });

    async function loadCategories() {
        try {
            categories = await adminApi.listCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    }

    async function loadQuestions() {
        loading = true;
        try {
            // Convert select string value to boolean or undefined
            let isActive: boolean | undefined = undefined;
            if (selectedActiveStatus === 'true') isActive = true;
            if (selectedActiveStatus === 'false') isActive = false;

            const response = await adminApi.listQuestions(
                selectedCategoryId || undefined,
                isActive,
                currentPage,
                pageSize
            );
            questions = response.content;
            totalElements = response.totalElements;
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            loading = false;
        }
    }

    function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
        toast = { message, type };
    }

    function handleFilterChange() {
        currentPage = 0;
        loadQuestions();
    }

    function handlePageChange(event: CustomEvent) {
        currentPage = event.detail;
        loadQuestions();
    }

    function handleEdit(event: CustomEvent) {
        goto(`/admin/questions/${event.detail.id}`);
    }

    function handleDeleteClick(event: CustomEvent) {
        selectedQuestion = event.detail;
        showDeleteDialog = true;
    }

    async function confirmDelete() {
        if (!selectedQuestion) return;
        actionLoading = true;
        try {
            await adminApi.deleteQuestion(selectedQuestion.id);
            showToast('Question deleted successfully', 'success');
            showDeleteDialog = false;
            loadQuestions();
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            actionLoading = false;
        }
    }

    // Options for Active filter
    const activeOptions = [
        { value: '', label: 'All Status' },
        { value: 'true', label: 'Active' },
        { value: 'false', label: 'Inactive' }
    ];

    $: categoryOptions = [
        { value: '', label: 'All Categories' },
        ...categories.map(c => ({ value: c.id, label: c.name }))
    ];
</script>

<div class="page-header">
    <h1>Questions</h1>
    <a href="/admin/questions/create" class="create-btn">+ New Question</a>
</div>

{#if toast}
    <Toast 
        message={toast.message} 
        type={toast.type} 
        on:close={() => toast = null} 
    />
{/if}

<div class="filters">
    <div class="filter-item">
        <Select
            label="Category"
            bind:value={selectedCategoryId}
            options={categoryOptions}
            on:change={handleFilterChange}
        />
        <!-- Note: Select component might need an on:change dispatch if bind:value doesn't trigger parent update properly with reactive statement.
             But bind:value is reactive. We need to trigger loadQuestions when value changes.
             Svelte's bind:value updates variable. We can use reactive statement:
             $: if (selectedCategoryId !== undefined) loadQuestions();
             But that might trigger too often or initially.
             Let's use a watch or manual change handler if Select supports it.
             Wait, Select component binds value. I can use $: loadQuestions() but it needs to be debounced or careful.
             Better: Use explicit "Apply" or simple reactive block.
        -->
    </div>
    <div class="filter-item">
        <Select
            label="Status"
            bind:value={selectedActiveStatus}
            options={activeOptions}
        />
    </div>
    <div class="filter-actions">
        <button on:click={handleFilterChange}>Apply Filters</button>
    </div>
</div>

<DataTable
    {columns}
    data={questions}
    {loading}
    {totalElements}
    {pageSize}
    {currentPage}
    on:pageChange={handlePageChange}
    on:edit={handleEdit}
    on:delete={handleDeleteClick}
>
    <!-- Custom cell rendering if needed, e.g. for boolean -->
    <!-- DataTable implementation assumes text. For boolean we might want custom slot. -->
    <!-- DataTable slot="actions" is for actions column. -->
    <!-- I should update DataTable to support custom cell slots or just format data. -->
    <!-- I'll just map data before passing or rely on string conversion. -->
</DataTable>

<ConfirmDialog
    open={showDeleteDialog}
    title="Delete Question"
    message={`Are you sure you want to delete this question? All ${selectedQuestion?.answerCount || 0} answers will also be deleted.`}
    on:confirm={confirmDelete}
    on:cancel={() => showDeleteDialog = false}
/>

<style>
    .page-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
    }

    h1 {
        margin: 0;
        color: #4ade80;
    }

    .create-btn {
        background: #4ade80;
        color: #000;
        border: none;
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        font-weight: 600;
        text-decoration: none;
        display: inline-block;
    }

    .create-btn:hover {
        background: #22c55e;
    }

    .filters {
        display: flex;
        gap: 1rem;
        margin-bottom: 2rem;
        background: #2a2a2a;
        padding: 1rem;
        border-radius: 8px;
        align-items: flex-end;
    }

    .filter-item {
        flex: 1;
        max-width: 200px;
    }

    .filter-actions {
        margin-bottom: 1rem; /* Align with input fields */
    }

    .filter-actions button {
        background: #3b82f6;
        color: white;
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        border: none;
        cursor: pointer;
    }
</style>
