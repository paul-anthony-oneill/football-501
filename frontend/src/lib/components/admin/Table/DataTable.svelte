<script lang="ts">
    import { createEventDispatcher } from 'svelte';

    export let columns: { key: string; label: string; sortable?: boolean }[] = [];
    export let data: any[] = [];
    export let loading = false;
    export let totalElements = 0;
    export let pageSize = 10;
    export let currentPage = 0;
    export let actions = true;

    const dispatch = createEventDispatcher();

    function handlePageChange(newPage: number) {
        if (newPage >= 0 && newPage < Math.ceil(totalElements / pageSize)) {
            dispatch('pageChange', newPage);
        }
    }
</script>

<div class="table-container card">
    <table>
        <thead>
            <tr>
                {#each columns as column}
                    <th>{column.label}</th>
                {/each}
                {#if actions}
                    <th class="actions-header">Actions</th>
                {/if}
            </tr>
        </thead>
        <tbody>
            {#if loading}
                <tr>
                    <td colspan={columns.length + (actions ? 1 : 0)} class="loading">
                        <div class="loading-spinner"></div>
                        <span>Loading...</span>
                    </td>
                </tr>
            {:else if data.length === 0}
                <tr>
                    <td colspan={columns.length + (actions ? 1 : 0)} class="empty">No data found</td>
                </tr>
            {:else}
                {#each data as item}
                    <tr>
                        {#each columns as column}
                            <td>{item[column.key]}</td>
                        {/each}
                        {#if actions}
                            <td class="actions">
                                <slot name="actions" {item}>
                                    <button class="icon-btn edit" on:click={() => dispatch('edit', item)} title="Edit">
                                        ✏️
                                    </button>
                                    <button class="icon-btn delete" on:click={() => dispatch('delete', item)} title="Delete">
                                        🗑️
                                    </button>
                                </slot>
                            </td>
                        {/if}
                    </tr>
                {/each}
            {/if}
        </tbody>
    </table>

    <div class="pagination">
        <button class="page-btn" disabled={currentPage === 0} on:click={() => handlePageChange(currentPage - 1)}>
            &larr; Previous
        </button>
        <span class="page-info">Page {currentPage + 1} of {Math.max(1, Math.ceil(totalElements / pageSize))}</span>
        <button
            class="page-btn"
            disabled={currentPage >= Math.ceil(totalElements / pageSize) - 1}
            on:click={() => handlePageChange(currentPage + 1)}
        >
            Next &rarr;
        </button>
    </div>
</div>

<style>
    .table-container {
        width: 100%;
        overflow-x: auto;
        background: var(--color-surface);
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-2);
        border: 1px solid var(--color-outline);
    }

    .card {
        padding: var(--space-md);
    }

    table {
        width: 100%;
        border-collapse: collapse;
        color: var(--color-on-surface);
    }

    th,
    td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--color-outline);
    }

    th {
        font-weight: 700;
        color: var(--color-primary);
        font-size: 0.85rem;
        text-transform: uppercase;
        letter-spacing: 1px;
        background: rgba(74, 222, 128, 0.05);
    }

    .actions-header {
        text-align: right;
    }

    .actions {
        display: flex;
        gap: var(--space-sm);
        justify-content: flex-end;
    }

    .loading,
    .empty {
        text-align: center;
        padding: var(--space-xxl);
        color: var(--color-on-surface-variant);
    }

    .loading-spinner {
        width: 2rem;
        height: 2rem;
        border: 3px solid var(--color-surface-variant);
        border-top-color: var(--color-primary);
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto var(--space-md);
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }

    .pagination {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: var(--space-md);
        padding-top: var(--space-md);
        border-top: 1px solid var(--color-outline);
    }

    .page-btn {
        padding: var(--space-sm) var(--space-md);
        background: var(--color-surface-variant);
        color: var(--color-on-surface-variant);
        border-radius: var(--radius-sm);
        font-size: 0.85rem;
        font-weight: 600;
    }

    .page-btn:hover:not(:disabled) {
        background: var(--color-primary-container);
        color: var(--color-on-primary-container);
    }

    .page-info {
        font-size: 0.85rem;
        color: var(--color-on-surface-variant);
        font-weight: 500;
    }

    .icon-btn {
        background: var(--color-surface-variant);
        width: 2.5rem;
        height: 2.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: var(--radius-sm);
        font-size: 1rem;
    }

    .icon-btn:hover {
        filter: brightness(1.2);
    }

    .icon-btn.edit:hover { background: var(--color-primary-container); }
    .icon-btn.delete:hover { background: var(--color-error-container); }
</style>
