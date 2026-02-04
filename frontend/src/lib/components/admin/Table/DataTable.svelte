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

<div class="table-container">
    <table>
        <thead>
            <tr>
                {#each columns as column}
                    <th>{column.label}</th>
                {/each}
                {#if actions}
                    <th>Actions</th>
                {/if}
            </tr>
        </thead>
        <tbody>
            {#if loading}
                <tr>
                    <td colspan={columns.length + (actions ? 1 : 0)} class="loading">Loading...</td>
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
                                    <button class="edit-btn" on:click={() => dispatch('edit', item)}>Edit</button>
                                    <button class="delete-btn" on:click={() => dispatch('delete', item)}>Delete</button>
                                </slot>
                            </td>
                        {/if}
                    </tr>
                {/each}
            {/if}
        </tbody>
    </table>

    <div class="pagination">
        <button disabled={currentPage === 0} on:click={() => handlePageChange(currentPage - 1)}>
            Previous
        </button>
        <span>Page {currentPage + 1} of {Math.max(1, Math.ceil(totalElements / pageSize))}</span>
        <button
            disabled={currentPage >= Math.ceil(totalElements / pageSize) - 1}
            on:click={() => handlePageChange(currentPage + 1)}
        >
            Next
        </button>
    </div>
</div>

<style>
    .table-container {
        width: 100%;
        overflow-x: auto;
        background: #2a2a2a;
        border-radius: 8px;
        padding: 1rem;
    }

    table {
        width: 100%;
        border-collapse: collapse;
        color: #fff;
    }

    th,
    td {
        padding: 1rem;
        text-align: left;
        border-bottom: 1px solid #444;
    }

    th {
        font-weight: 600;
        color: #9ca3af;
    }

    .actions {
        display: flex;
        gap: 0.5rem;
    }

    .loading,
    .empty {
        text-align: center;
        padding: 2rem;
        color: #9ca3af;
    }

    .pagination {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 1rem;
        margin-top: 1rem;
        color: #fff;
    }

    button {
        padding: 0.5rem 1rem;
        border-radius: 4px;
        border: none;
        cursor: pointer;
        font-size: 0.9rem;
    }

    .edit-btn {
        background: #3b82f6;
        color: white;
    }

    .delete-btn {
        background: #ef4444;
        color: white;
    }

    .pagination button {
        background: #444;
        color: white;
    }

    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
</style>
