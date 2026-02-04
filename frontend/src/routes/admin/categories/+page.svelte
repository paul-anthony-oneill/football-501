<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale } from 'svelte/transition';
    import { adminApi } from '$lib/api/admin';
    import type { Category } from '$lib/types/admin';
    import DataTable from '$lib/components/admin/Table/DataTable.svelte';
    import FormModal from '$lib/components/admin/Modal/FormModal.svelte';
    import ConfirmDialog from '$lib/components/admin/Modal/ConfirmDialog.svelte';
    import CategoryForm from '$lib/components/admin/Category/CategoryForm.svelte';
    import Toast from '$lib/components/admin/Feedback/Toast.svelte';

    let categories: Category[] = [];
    let loading = true;
    let toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    // Modal states
    let showCreateModal = false;
    let showEditModal = false;
    let showDeleteDialog = false;
    let selectedCategory: Category | null = null;
    let actionLoading = false;

    const columns = [
        { key: 'name', label: 'Name' },
        { key: 'slug', label: 'Slug' },
        { key: 'questionCount', label: 'Questions' },
        { key: 'updatedAt', label: 'Last Updated' }
    ];

    onMount(loadCategories);

    async function loadCategories() {
        loading = true;
        try {
            categories = await adminApi.listCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            loading = false;
        }
    }

    function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
        toast = { message, type };
    }

    function handleCreate() {
        selectedCategory = null;
        showCreateModal = true;
    }

    function handleEdit(event: CustomEvent) {
        selectedCategory = event.detail;
        showEditModal = true;
    }

    function handleDeleteClick(event: CustomEvent) {
        selectedCategory = event.detail;
        if (selectedCategory && selectedCategory.questionCount > 0) {
            showToast(`Cannot delete category with ${selectedCategory.questionCount} questions`, 'error');
            return;
        }
        showDeleteDialog = true;
    }

    async function submitCreate(event: CustomEvent) {
        actionLoading = true;
        try {
            await adminApi.createCategory(event.detail);
            showToast('Category created successfully', 'success');
            showCreateModal = false;
            loadCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            actionLoading = false;
        }
    }

    async function submitUpdate(event: CustomEvent) {
        if (!selectedCategory) return;
        actionLoading = true;
        try {
            await adminApi.updateCategory(selectedCategory.id, event.detail);
            showToast('Category updated successfully', 'success');
            showEditModal = false;
            loadCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            actionLoading = false;
        }
    }

    async function confirmDelete() {
        if (!selectedCategory) return;
        actionLoading = true; // Use a local loading state for dialog if needed, or just block
        try {
            await adminApi.deleteCategory(selectedCategory.id);
            showToast('Category deleted successfully', 'success');
            showDeleteDialog = false;
            loadCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            actionLoading = false;
        }
    }
</script>

<div class="page-header">
    <h1>Categories</h1>
    <button class="create-btn" on:click={handleCreate}>+ New Category</button>
</div>

{#if toast}
    <Toast 
        message={toast.message} 
        type={toast.type} 
        on:close={() => toast = null} 
    />
{/if}

<DataTable
    {columns}
    data={categories}
    {loading}
    totalElements={categories.length}
    pageSize={100} 
    currentPage={0}
    on:edit={handleEdit}
    on:delete={handleDeleteClick}
/>

<!-- Create Modal -->
{#if showCreateModal}
    <div class="custom-modal-backdrop" on:click={() => showCreateModal = false} transition:fade>
        <div class="custom-modal" on:click|stopPropagation transition:scale>
            <h3>Create Category</h3>
            <CategoryForm 
                on:submit={submitCreate} 
                on:cancel={() => showCreateModal = false}
                loading={actionLoading}
            />
        </div>
    </div>
{/if}

<!-- Edit Modal -->
{#if showEditModal && selectedCategory}
    <div class="custom-modal-backdrop" on:click={() => showEditModal = false} transition:fade>
        <div class="custom-modal" on:click|stopPropagation transition:scale>
            <h3>Edit Category</h3>
            <CategoryForm 
                category={selectedCategory}
                on:submit={submitUpdate} 
                on:cancel={() => showEditModal = false}
                loading={actionLoading}
            />
        </div>
    </div>
{/if}

<ConfirmDialog
    open={showDeleteDialog}
    title="Delete Category"
    message={`Are you sure you want to delete '${selectedCategory?.name}'? This action cannot be undone.`}
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
        cursor: pointer;
    }

    .create-btn:hover {
        background: #22c55e;
    }

    /* Modal Styles matching FormModal */
    .custom-modal-backdrop {
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

    .custom-modal {
        background: #2a2a2a;
        padding: 2rem;
        border-radius: 12px;
        width: 90%;
        max-width: 500px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    }

    .custom-modal h3 {
        margin-top: 0;
        color: #fff;
        margin-bottom: 1.5rem;
    }
</style>
