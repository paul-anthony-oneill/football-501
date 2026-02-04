<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import { adminApi } from '$lib/api/admin';
    import type { Question, Category, Answer } from '$lib/types/admin';
    import QuestionForm from '$lib/components/admin/Question/QuestionForm.svelte';
    import AnswerForm from '$lib/components/admin/Answer/AnswerForm.svelte';
    import BulkImportModal from '$lib/components/admin/Answer/BulkImportModal.svelte';
    import DataTable from '$lib/components/admin/Table/DataTable.svelte';
    import ConfirmDialog from '$lib/components/admin/Modal/ConfirmDialog.svelte';
    import Toast from '$lib/components/admin/Feedback/Toast.svelte';

    $: questionId = $page.params.id;

    let question: Question | null = null;
    let categories: Category[] = [];
    let answers: Answer[] = [];
    let loading = true;
    let toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    // Edit Question State
    let isEditingQuestion = false;
    let questionLoading = false;

    // Answer State
    let showCreateAnswerModal = false;
    let showEditAnswerModal = false;
    let showBulkImportModal = false;
    let showDeleteAnswerDialog = false;
    let selectedAnswer: Answer | null = null;
    let answerLoading = false;

    const answerColumns = [
        { key: 'displayText', label: 'Answer' },
        { key: 'score', label: 'Score' },
        { key: 'isValidDarts', label: 'Valid Darts' },
        { key: 'isBust', label: 'Bust' }
    ];

    $: if (questionId) {
        loadQuestion();
        loadAnswers();
    }

    onMount(async () => {
        await loadCategories();
        // Initial load is handled by reactive statement if questionId is present
        loading = false;
    });

    async function loadQuestion() {
        if (!questionId) return;
        try {
            question = await adminApi.getQuestion(questionId);
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    }

    async function loadCategories() {
        try {
            categories = await adminApi.listCategories();
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    }

    async function loadAnswers() {
        if (!questionId) return;
        try {
            answers = await adminApi.listAnswers(questionId);
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    }

    function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
        toast = { message, type };
    }

    // Question Actions
    async function handleUpdateQuestion(event: CustomEvent) {
        if (!questionId) return;
        questionLoading = true;
        try {
            question = await adminApi.updateQuestion(questionId, event.detail);
            showToast('Question updated successfully', 'success');
            isEditingQuestion = false;
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            questionLoading = false;
        }
    }

    async function toggleActive() {
        if (!question || !questionId) return;
        try {
            question = await adminApi.toggleQuestionActive(questionId);
            showToast(`Question is now ${question.isActive ? 'Active' : 'Inactive'}`, 'success');
        } catch (error: any) {
            showToast(error.message, 'error');
        }
    }

    // Answer Actions
    function handleCreateAnswer() {
        selectedAnswer = null;
        showCreateAnswerModal = true;
    }

    function handleEditAnswer(event: CustomEvent) {
        selectedAnswer = event.detail;
        showEditAnswerModal = true;
    }

    function handleDeleteAnswerClick(event: CustomEvent) {
        selectedAnswer = event.detail;
        showDeleteAnswerDialog = true;
    }

    async function submitCreateAnswer(event: CustomEvent) {
        if (!questionId) return;
        answerLoading = true;
        try {
            await adminApi.createAnswer(questionId, event.detail);
            showToast('Answer created successfully', 'success');
            showCreateAnswerModal = false;
            loadAnswers();
            loadQuestion(); // update counts
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            answerLoading = false;
        }
    }

    async function submitUpdateAnswer(event: CustomEvent) {
        if (!selectedAnswer || !questionId) return;
        answerLoading = true;
        try {
            await adminApi.updateAnswer(selectedAnswer.id, event.detail);
            showToast('Answer updated successfully', 'success');
            showEditAnswerModal = false;
            loadAnswers();
            loadQuestion(); // update counts
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            answerLoading = false;
        }
    }

    async function submitBulkImport(event: CustomEvent) {
        if (!questionId) return;
        answerLoading = true;
        try {
            const result = await adminApi.bulkCreateAnswers(questionId, event.detail);
            
            if (result.errors && result.errors.length > 0) {
                showToast(`Imported ${result.created}, Skipped ${result.skipped}, Errors: ${result.errors.length}`, 'info');
                console.error('Import errors:', result.errors);
            } else {
                showToast(`Imported ${result.created} answers successfully (Skipped ${result.skipped})`, 'success');
            }
            
            showBulkImportModal = false;
            loadAnswers();
            loadQuestion(); // update counts
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            answerLoading = false;
        }
    }

    async function confirmDeleteAnswer() {
        if (!selectedAnswer) return;
        answerLoading = true;
        try {
            await adminApi.deleteAnswer(selectedAnswer.id);
            showToast('Answer deleted successfully', 'success');
            showDeleteAnswerDialog = false;
            loadAnswers();
            loadQuestion(); // update counts
        } catch (error: any) {
            showToast(error.message, 'error');
        } finally {
            answerLoading = false;
        }
    }
</script>

<div class="page-container">
    {#if toast}
        <Toast 
            message={toast.message} 
            type={toast.type} 
            on:close={() => toast = null} 
        />
    {/if}

    {#if loading}
        <div class="loading">Loading...</div>
    {:else if question}
        <!-- Question Header -->
        <div class="section-header">
            <div>
                <h1>{question.questionText}</h1>
                <div class="meta">
                    <span class="badge category">{question.categoryName}</span>
                    <span class="badge status {question.isActive ? 'active' : 'inactive'}">
                        {question.isActive ? 'Active' : 'Inactive'}
                    </span>
                    <span class="metric">Key: {question.metricKey}</span>
                </div>
            </div>
            <div class="actions">
                <button class="action-btn" on:click={toggleActive}>
                    {question.isActive ? 'Deactivate' : 'Activate'}
                </button>
                <button class="action-btn primary" on:click={() => isEditingQuestion = !isEditingQuestion}>
                    {isEditingQuestion ? 'Cancel Edit' : 'Edit Question'}
                </button>
            </div>
        </div>

        {#if isEditingQuestion}
            <div class="edit-section" transition:fade>
                <QuestionForm
                    {question}
                    {categories}
                    loading={questionLoading}
                    on:submit={handleUpdateQuestion}
                    on:cancel={() => isEditingQuestion = false}
                />
            </div>
        {/if}

        <!-- Stats Section -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="value">{question.answerCount}</div>
                <div class="label">Total Answers</div>
            </div>
            <div class="stat-card">
                <div class="value">{question.validDartsCount}</div>
                <div class="label">Valid Darts</div>
            </div>
            <div class="stat-card">
                <div class="value">{Math.round((question.validDartsCount / Math.max(1, question.answerCount)) * 100)}%</div>
                <div class="label">Coverage</div>
            </div>
        </div>

        <!-- Answers Section -->
        <div class="answers-section">
            <div class="section-header">
                <h2>Answers</h2>
                <div class="actions">
                    <button class="action-btn" on:click={() => showBulkImportModal = true}>
                        Bulk Import
                    </button>
                    <button class="action-btn primary" on:click={handleCreateAnswer}>
                        + Add Answer
                    </button>
                </div>
            </div>

            <DataTable
                columns={answerColumns}
                data={answers}
                loading={loading}
                totalElements={answers.length}
                pageSize={50}
                currentPage={0}
                on:edit={handleEditAnswer}
                on:delete={handleDeleteAnswerClick}
            />
        </div>
    {:else}
        <div class="error">Question not found</div>
    {/if}
</div>

<!-- Create/Edit Answer Modal -->
{#if showCreateAnswerModal || (showEditAnswerModal && selectedAnswer)}
    <div class="custom-modal-backdrop" on:click={() => { showCreateAnswerModal = false; showEditAnswerModal = false; }} transition:fade>
        <div class="custom-modal" on:click|stopPropagation transition:scale>
            <h3>{showCreateAnswerModal ? 'Create Answer' : 'Edit Answer'}</h3>
            <AnswerForm
                answer={selectedAnswer || undefined}
                loading={answerLoading}
                on:submit={showCreateAnswerModal ? submitCreateAnswer : submitUpdateAnswer}
                on:cancel={() => { showCreateAnswerModal = false; showEditAnswerModal = false; }}
            />
        </div>
    </div>
{/if}

<!-- Bulk Import Modal -->
{#if showBulkImportModal}
    <div class="custom-modal-backdrop" on:click={() => showBulkImportModal = false} transition:fade>
        <div class="custom-modal large" on:click|stopPropagation transition:scale>
            <h3>Bulk Import Answers</h3>
            <BulkImportModal
                loading={answerLoading}
                on:submit={submitBulkImport}
                on:cancel={() => showBulkImportModal = false}
            />
        </div>
    </div>
{/if}

<ConfirmDialog
    open={showDeleteAnswerDialog}
    title="Delete Answer"
    message={`Are you sure you want to delete '${selectedAnswer?.displayText}'?`}
    on:confirm={confirmDeleteAnswer}
    on:cancel={() => showDeleteAnswerDialog = false}
/>

<style>
    .page-container {
        max-width: 1000px;
        margin: 0 auto;
    }

    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 2rem;
    }

    h1 {
        margin: 0 0 0.5rem 0;
        color: #4ade80;
    }

    .meta {
        display: flex;
        gap: 0.75rem;
        align-items: center;
    }

    .badge {
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        font-size: 0.8rem;
        font-weight: 500;
    }

    .badge.category {
        background: #333;
        color: #d1d5db;
    }

    .badge.status.active {
        background: rgba(74, 222, 128, 0.1);
        color: #4ade80;
        border: 1px solid #4ade80;
    }

    .badge.status.inactive {
        background: rgba(239, 68, 68, 0.1);
        color: #ef4444;
        border: 1px solid #ef4444;
    }

    .metric {
        color: #9ca3af;
        font-size: 0.9rem;
    }

    .actions {
        display: flex;
        gap: 0.75rem;
    }

    .action-btn {
        padding: 0.5rem 1rem;
        border-radius: 6px;
        border: 1px solid #444;
        background: #2a2a2a;
        color: #fff;
        cursor: pointer;
        font-size: 0.9rem;
    }

    .action-btn.primary {
        background: #4ade80;
        color: #000;
        border-color: #4ade80;
    }

    .edit-section {
        background: #2a2a2a;
        padding: 1.5rem;
        border-radius: 8px;
        margin-bottom: 2rem;
        border: 1px solid #444;
    }

    .stats-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 1rem;
        margin-bottom: 2rem;
    }

    .stat-card {
        background: #2a2a2a;
        padding: 1.5rem;
        border-radius: 8px;
        text-align: center;
    }

    .stat-card .value {
        font-size: 2rem;
        font-weight: bold;
        color: #fff;
        margin-bottom: 0.25rem;
    }

    .stat-card .label {
        color: #9ca3af;
        font-size: 0.9rem;
    }

    .answers-section {
        margin-top: 3rem;
    }

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

    .custom-modal.large {
        max-width: 800px;
    }

    .custom-modal h3 {
        margin-top: 0;
        color: #fff;
        margin-bottom: 1.5rem;
    }
</style>
