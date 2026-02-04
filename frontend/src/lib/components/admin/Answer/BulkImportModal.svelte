<script lang="ts">
    import { createEventDispatcher } from 'svelte';
    import type { CreateAnswerRequest } from '$lib/types/admin';

    export let loading = false;

    const dispatch = createEventDispatcher();
    let csvContent = '';
    let parsedData: CreateAnswerRequest[] = [];
    let error = '';
    let preview: CreateAnswerRequest[] = [];

    function handleFileUpload(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                csvContent = e.target?.result as string;
                parseCSV();
            };
            reader.readAsText(file);
        }
    }

    function parseCSV() {
        try {
            const lines = csvContent.split('\n').filter(line => line.trim());
            if (lines.length < 2) { // Header + at least one row
                error = 'CSV file is empty or missing data';
                parsedData = [];
                preview = [];
                return;
            }

            // Assume header is displayText,score (case insensitive check optionally)
            // But strict format: displayText,score
            
            const data: CreateAnswerRequest[] = [];
            let headerFound = false;

            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                // Simple CSV parse (handling comma inside quotes is better but maybe overkill for now)
                // Let's do simple split by comma for MVP
                const parts = line.split(',');
                
                if (i === 0) {
                    // Check header
                    if (parts.length >= 2) {
                        headerFound = true;
                    }
                    continue;
                }

                if (parts.length >= 2) {
                    const displayText = parts[0].trim();
                    const scoreStr = parts[parts.length - 1].trim(); // Last part is score
                    // If name has commas, it might be split.
                    // Better approach: regex for "name",score or name,score
                    
                    const score = Number(scoreStr);
                    if (displayText && !isNaN(score)) {
                        data.push({ displayText, score });
                    }
                }
            }

            parsedData = data;
            preview = data.slice(0, 5);
            error = '';
        } catch (e) {
            error = 'Failed to parse CSV';
            parsedData = [];
            preview = [];
        }
    }

    function handleSubmit() {
        if (parsedData.length > 0) {
            dispatch('submit', { answers: parsedData });
        }
    }
</script>

<div class="bulk-import">
    <div class="upload-section">
        <label for="csv-file" class="file-label">
            Choose CSV File
            <input type="file" id="csv-file" accept=".csv" on:change={handleFileUpload} disabled={loading} />
        </label>
        <span class="help-text">Format: displayText,score</span>
    </div>

    {#if error}
        <div class="error-message">{error}</div>
    {/if}

    {#if preview.length > 0}
        <div class="preview-section">
            <h4>Preview ({parsedData.length} items)</h4>
            <table>
                <thead>
                    <tr>
                        <th>Display Text</th>
                        <th>Score</th>
                    </tr>
                </thead>
                <tbody>
                    {#each preview as item}
                        <tr>
                            <td>{item.displayText}</td>
                            <td>{item.score}</td>
                        </tr>
                    {/each}
                    {#if parsedData.length > 5}
                        <tr>
                            <td colspan="2" class="more">...and {parsedData.length - 5} more</td>
                        </tr>
                    {/if}
                </tbody>
            </table>
        </div>
    {/if}

    <div class="actions">
        <button type="button" class="cancel-btn" on:click={() => dispatch('cancel')} disabled={loading}>
            Cancel
        </button>
        <button 
            type="button" 
            class="submit-btn" 
            on:click={handleSubmit}
            disabled={loading || parsedData.length === 0}
        >
            {loading ? 'Importing...' : `Import ${parsedData.length} Answers`}
        </button>
    </div>
</div>

<style>
    .bulk-import {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
    }

    .upload-section {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .file-label {
        background: #3b82f6;
        color: white;
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 500;
        display: inline-block;
    }

    input[type="file"] {
        display: none;
    }

    .help-text {
        color: #9ca3af;
        font-size: 0.9rem;
    }

    .error-message {
        color: #ef4444;
        background: rgba(239, 68, 68, 0.1);
        padding: 0.75rem;
        border-radius: 6px;
    }

    .preview-section {
        background: #222;
        padding: 1rem;
        border-radius: 8px;
    }

    h4 {
        margin: 0 0 0.5rem 0;
        color: #d1d5db;
    }

    table {
        width: 100%;
        border-collapse: collapse;
        color: #ccc;
    }

    th, td {
        padding: 0.5rem;
        text-align: left;
        border-bottom: 1px solid #333;
    }

    th {
        color: #9ca3af;
        font-size: 0.9rem;
    }

    .more {
        text-align: center;
        color: #6b7280;
        padding: 0.5rem;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 1rem;
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
