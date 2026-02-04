<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { adminApi } from '$lib/api/admin';
    import type { Category } from '$lib/types/admin';
    import QuestionForm from '$lib/components/admin/Question/QuestionForm.svelte';
    import Toast from '$lib/components/admin/Feedback/Toast.svelte';

    let categories: Category[] = [];
    let loading = false;
    let toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    onMount(async () => {
        try {
            categories = await adminApi.listCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    });

    function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
        toast = { message, type };
    }

    async function handleSubmit(event: CustomEvent) {
        loading = true;
        try {
            const question = await adminApi.createQuestion(event.detail);
            showToast('Question created successfully', 'success');
            // Redirect to detail page to add answers
            setTimeout(() => {
                goto(`/admin/questions/${question.id}`);
            }, 1000);
        } catch (error: any) {
            showToast(error.message, 'error');
            loading = false;
        }
    }

    function handleCancel() {
        goto('/admin/questions');
    }
</script>

<div class="page-container">
    <h1>Create Question</h1>
    
    {#if toast}
        <Toast 
            message={toast.message} 
            type={toast.type} 
            on:close={() => toast = null} 
        />
    {/if}

    <div class="form-container">
        <QuestionForm
            {categories}
            {loading}
            on:submit={handleSubmit}
            on:cancel={handleCancel}
        />
    </div>
</div>

<style>
    .page-container {
        max-width: 800px;
        margin: 0 auto;
    }

    h1 {
        color: #4ade80;
        margin-bottom: 2rem;
    }

    .form-container {
        background: #2a2a2a;
        padding: 2rem;
        border-radius: 12px;
    }
</style>
