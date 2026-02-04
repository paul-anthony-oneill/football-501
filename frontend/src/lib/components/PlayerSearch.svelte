<script lang="ts">
	import { createEventDispatcher } from 'svelte';
	import { getFlagEmoji } from '$lib/utils/country';

	export let value = '';
	export let disabled = false;
	export let placeholder = 'Enter player name...';

	const dispatch = createEventDispatcher();
	let suggestions: Array<{ id: string; name: string; nationality: string }> = [];
	let showSuggestions = false;
	let loading = false;
	let debounceTimer: NodeJS.Timeout;
	let inputElement: HTMLInputElement;

	// API Base URL - strictly for search
	// Note: We should ideally pass this as a prop or env var, but keeping consistent with +page.svelte
	const API_BASE = 'http://localhost:8080/api';

	function handleInput(event: Event) {
		const target = event.target as HTMLInputElement;
		value = target.value;
		
		clearTimeout(debounceTimer);
		
		if (value.length < 2) {
			suggestions = [];
			showSuggestions = false;
			return;
		}

		debounceTimer = setTimeout(() => {
			fetchSuggestions(value);
		}, 300);
	}

	async function fetchSuggestions(query: string) {
		loading = true;
		try {
			const response = await fetch(`${API_BASE}/players/search?query=${encodeURIComponent(query)}`);
			if (response.ok) {
				suggestions = await response.json();
				showSuggestions = suggestions.length > 0;
			}
		} catch (error) {
			console.error('Error fetching suggestions:', error);
		} finally {
			loading = false;
		}
	}

	function selectPlayer(player: { name: string }) {
		value = player.name;
		suggestions = [];
		showSuggestions = false;
		inputElement.focus();
	}

	function handleKeydown(event: KeyboardEvent) {
		if (event.key === 'Enter') {
			event.preventDefault(); // Prevent form submission if inside one
			dispatch('submit');
			showSuggestions = false;
		}
		// TODO: Add arrow key navigation
	}

	function handleBlur() {
		// Delay hiding to allow click event on suggestion to fire
		setTimeout(() => {
			showSuggestions = false;
		}, 200);
	}
</script>

<div class="player-search">
	<div class="input-wrapper">
		<input
			type="text"
			bind:this={inputElement}
			{value}
			{disabled}
			{placeholder}
			on:input={handleInput}
			on:keydown={handleKeydown}
			on:blur={handleBlur}
			autocomplete="off"
		/>
		{#if loading}
			<div class="spinner"></div>
		{/if}
	</div>

	{#if showSuggestions}
		<ul class="suggestions">
			{#each suggestions as player}
				<li>
					<button on:click={() => selectPlayer(player)} class="suggestion-btn">
						<span class="name">{player.name}</span>
						{#if player.nationality}
							<span class="nationality" title={player.nationality}>{getFlagEmoji(player.nationality)}</span>
						{/if}
					</button>
				</li>
			{/each}
		</ul>
	{/if}
</div>

<style>
	.player-search {
		position: relative;
		width: 100%;
	}

	.input-wrapper {
		position: relative;
		display: flex;
		align-items: center;
	}

	input {
		width: 100%;
		padding: 0.75rem 1rem;
		font-size: 1rem;
		border: 2px solid #4ade80;
		border-radius: 8px;
		background: #1a1a1a;
		color: #fff;
		box-sizing: border-box;
	}

	input:focus {
		outline: none;
		border-color: #22c55e;
		box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.1);
	}

	input:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.spinner {
		position: absolute;
		right: 1rem;
		width: 1rem;
		height: 1rem;
		border: 2px solid #4ade80;
		border-top-color: transparent;
		border-radius: 50%;
		animation: spin 1s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	.suggestions {
		position: absolute;
		top: 100%;
		left: 0;
		right: 0;
		margin: 0.5rem 0 0;
		padding: 0;
		list-style: none;
		background: #2a2a2a;
		border: 1px solid #4ade80;
		border-radius: 8px;
		max-height: 200px;
		overflow-y: auto;
		z-index: 10;
		box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
	}

	.suggestion-btn {
		width: 100%;
		text-align: left;
		padding: 0.75rem 1rem;
		background: none;
		border: none;
		color: #fff;
		cursor: pointer;
		display: flex;
		justify-content: space-between;
		align-items: center;
		transition: background 0.2s;
	}

	.suggestion-btn:hover {
		background: #374151;
	}

	.name {
		font-weight: 500;
	}

	.nationality {
		color: #9ca3af;
		font-size: 0.875rem;
	}
</style>
