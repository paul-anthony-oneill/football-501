<script lang="ts">
	import { onMount } from 'svelte';
	import PlayerSearch from '$lib/components/PlayerSearch.svelte';

	// API Base URL - adjust if backend runs on different port
	const API_BASE = 'http://localhost:8080/api';

	interface Category {
		id: string;
		name: string;
		slug: string;
		description: string;
	}

	// Game state
	let gameId: string | null = null;
	let playerId: string = crypto.randomUUID();
	let score = 501;
	let question = '';
	let turnCount = 0;
	let gameStatus = 'NOT_STARTED'; // NOT_STARTED, IN_PROGRESS, COMPLETED
	let isWin = false;

	// Input state
	let answer = '';
	let loading = false;
	let feedback = '';
	let feedbackType: 'success' | 'error' | 'info' = 'info';
	
	// Category state
	let categories: Category[] = [];
	let selectedCategorySlug = 'football';
	let categoriesLoading = true;

	// Move history
	let moves: Array<{ answer: string; result: string; scoreBefore: number; scoreAfter: number }> = [];

	// Input element reference for focus management
	let inputElement: HTMLInputElement;

	onMount(async () => {
		try {
			const response = await fetch(`${API_BASE}/categories`);
			if (response.ok) {
				categories = await response.json();
				if (categories.length > 0) {
					// Default to 'football' if exists, otherwise first one
					const defaultCat = categories.find(c => c.slug === 'football');
					selectedCategorySlug = defaultCat ? defaultCat.slug : categories[0].slug;
				}
			}
		} catch (error) {
			console.error('Error fetching categories:', error);
			feedback = 'Failed to load categories. Is the backend running?';
			feedbackType = 'error';
		} finally {
			categoriesLoading = false;
		}
	});

	async function startGame() {
		loading = true;
		feedback = '';
		moves = [];

		try {
			const response = await fetch(`${API_BASE}/practice/start`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					playerId,
					categorySlug: selectedCategorySlug
				})
			});

			if (!response.ok) {
				throw new Error('Failed to start game');
			}

			const data = await response.json();
			gameId = data.gameId;
			score = data.currentScore;
			question = data.questionText;
			turnCount = data.turnCount;
			gameStatus = data.status;
			isWin = data.isWin;

			feedback = 'Game started! Good luck!';
			feedbackType = 'success';
		} catch (error) {
			feedback = 'Error starting game. Is the backend running?';
			feedbackType = 'error';
			console.error('Error starting game:', error);
		} finally {
			loading = false;
		}
	}

	async function submitAnswer() {
		if (!answer.trim() || !gameId || loading) return;

		loading = true;
		const submittedAnswer = answer;
		answer = ''; // Clear input immediately

		try {
			const response = await fetch(`${API_BASE}/practice/games/${gameId}/submit?playerId=${playerId}`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ answer: submittedAnswer })
			});

			if (!response.ok) {
				throw new Error('Failed to submit answer');
			}

			const data = await response.json();

			// Update game state
			score = data.gameState.currentScore;
			turnCount = data.gameState.turnCount;
			gameStatus = data.gameState.status;
			isWin = data.isWin;

			// Add to move history
			moves = [
				{
					answer: submittedAnswer,
					result: data.result,
					scoreBefore: data.scoreBefore,
					scoreAfter: data.scoreAfter
				},
				...moves
			].slice(0, 10); // Keep last 10 moves

			// Set feedback
			switch (data.result) {
				case 'VALID':
					feedback = `✓ ${data.matchedAnswer}: ${data.scoreValue} points! Score: ${data.scoreAfter}`;
					feedbackType = 'success';
					break;
				case 'CHECKOUT':
					feedback = `🎉 YOU WIN! Final score: ${data.scoreAfter} in ${turnCount} turns!`;
					feedbackType = 'success';
					break;
				case 'BUST':
					feedback = `✗ Bust! ${data.reason}. No score change.`;
					feedbackType = 'error';
					break;
				case 'INVALID':
					feedback = `✗ Invalid answer. Try again!`;
					feedbackType = 'error';
					break;
			}
		} catch (error) {
			feedback = 'Error submitting answer';
			feedbackType = 'error';
			console.error('Error submitting answer:', error);
		} finally {
			loading = false;
			// Keep input focused for continuous gameplay
			setTimeout(() => inputElement?.focus(), 0);
		}
	}

	function handleKeyPress(event: KeyboardEvent) {
		if (event.key === 'Enter' && !loading) {
			submitAnswer();
		}
	}
</script>

<main>
	<h1>Football 501 ⚽</h1>

	{#if gameStatus === 'NOT_STARTED'}
		<div class="start-screen">
			<p>Welcome to Football 501! Get your score from 501 to 0 by naming football players.</p>
			
			<div class="category-select">
				<label for="category">Select Category:</label>
				{#if categoriesLoading}
					<span class="loading-text">Loading categories...</span>
				{:else}
					<select id="category" bind:value={selectedCategorySlug}>
						{#each categories as category}
							<option value={category.slug}>{category.name}</option>
						{/each}
					</select>
				{/if}
			</div>

			<button on:click={startGame} disabled={loading || categoriesLoading}>
				{loading ? 'Starting...' : 'Start Game'}
			</button>
		</div>
	{:else}
		<div class="game-screen">
			<!-- Score Display -->
			<div class="score-display">
				<div class="score">
					<span class="label">Score:</span>
					<span class="value">{score}</span>
				</div>
				<div class="turns">
					<span class="label">Turns:</span>
					<span class="value">{turnCount}</span>
				</div>
			</div>

			<!-- Question -->
			<div class="question">
				<h2>Question:</h2>
				<p>{question}</p>
			</div>

			<!-- Answer Input -->
			{#if gameStatus === 'IN_PROGRESS'}
				<div class="answer-input">
					<div class="search-container">
						<PlayerSearch
							bind:value={answer}
							disabled={loading}
							on:submit={submitAnswer}
							placeholder="Type player name..."
						/>
					</div>
					<button on:click={submitAnswer} disabled={loading || !answer.trim()}>
						{loading ? 'Submitting...' : 'Submit'}
					</button>
				</div>
			{/if}

			<!-- Feedback -->
			{#if feedback}
				<div class="feedback {feedbackType}">
					{feedback}
				</div>
			{/if}

			<!-- Win State -->
			{#if isWin}
				<div class="win-state">
					<h2>🎉 Congratulations!</h2>
					<p>You completed the game in {turnCount} turns!</p>
					<button on:click={startGame}>Play Again</button>
				</div>
			{/if}

			<!-- Move History -->
			{#if moves.length > 0}
				<div class="move-history">
					<h3>Recent Moves:</h3>
					<div class="moves">
						{#each moves as move}
							<div class="move {move.result.toLowerCase()}">
								<span class="move-answer">{move.answer}</span>
								<span class="move-result">{move.result}</span>
								<span class="move-score">{move.scoreBefore} → {move.scoreAfter}</span>
							</div>
						{/each}
					</div>
				</div>
			{/if}
		</div>
	{/if}
</main>

<style>
	:global(body) {
		margin: 0;
		padding: 0;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell,
			sans-serif;
		background: #1a1a1a;
		color: #fff;
	}

	main {
		max-width: 800px;
		margin: 0 auto;
		padding: 2rem;
	}

	h1 {
		text-align: center;
		font-size: 2.5rem;
		margin-bottom: 2rem;
		color: #4ade80;
	}

	.start-screen {
		text-align: center;
		margin-top: 4rem;
	}

	.start-screen p {
		font-size: 1.2rem;
		margin-bottom: 2rem;
		color: #d1d5db;
	}

	.category-select {
		margin-bottom: 2rem;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}

	.category-select label {
		color: #9ca3af;
		font-size: 0.9rem;
	}

	.category-select select {
		padding: 0.75rem 2rem;
		font-size: 1rem;
		background: #2a2a2a;
		color: #fff;
		border: 1px solid #4ade80;
		border-radius: 8px;
		cursor: pointer;
		min-width: 200px;
		outline: none;
	}

	.category-select select:focus {
		border-color: #22c55e;
		box-shadow: 0 0 0 3px rgba(74, 222, 128, 0.1);
	}

	.loading-text {
		color: #9ca3af;
		font-style: italic;
	}

	button {
		background: #4ade80;
		color: #000;
		border: none;
		padding: 0.75rem 2rem;
		font-size: 1rem;
		font-weight: 600;
		border-radius: 8px;
		cursor: pointer;
		transition: all 0.2s;
	}

	button:hover:not(:disabled) {
		background: #22c55e;
		transform: translateY(-2px);
	}

	button:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.score-display {
		display: flex;
		justify-content: center;
		gap: 3rem;
		margin-bottom: 2rem;
		padding: 1.5rem;
		background: #2a2a2a;
		border-radius: 12px;
	}

	.score,
	.turns {
		text-align: center;
	}

	.label {
		display: block;
		font-size: 0.875rem;
		color: #9ca3af;
		margin-bottom: 0.5rem;
	}

	.value {
		display: block;
		font-size: 2.5rem;
		font-weight: bold;
		color: #4ade80;
	}

	.question {
		background: #2a2a2a;
		padding: 1.5rem;
		border-radius: 12px;
		margin-bottom: 2rem;
	}

	.question h2 {
		margin: 0 0 1rem 0;
		font-size: 1.2rem;
		color: #4ade80;
	}

	.question p {
		margin: 0;
		font-size: 1.1rem;
		line-height: 1.6;
	}

	.answer-input {
		display: flex;
		gap: 1rem;
		margin-bottom: 1rem;
		align-items: flex-start;
	}

	.search-container {
		flex: 1;
	}

	.feedback {
		padding: 1rem;
		border-radius: 8px;
		margin-bottom: 1rem;
		font-weight: 500;
	}

	.feedback.success {
		background: rgba(74, 222, 128, 0.1);
		border: 1px solid #4ade80;
		color: #4ade80;
	}

	.feedback.error {
		background: rgba(239, 68, 68, 0.1);
		border: 1px solid #ef4444;
		color: #ef4444;
	}

	.feedback.info {
		background: rgba(59, 130, 246, 0.1);
		border: 1px solid #3b82f6;
		color: #3b82f6;
	}

	.win-state {
		text-align: center;
		padding: 2rem;
		background: rgba(74, 222, 128, 0.1);
		border: 2px solid #4ade80;
		border-radius: 12px;
		margin-bottom: 2rem;
	}

	.win-state h2 {
		color: #4ade80;
		margin-bottom: 1rem;
	}

	.move-history {
		margin-top: 2rem;
	}

	.move-history h3 {
		color: #9ca3af;
		font-size: 1rem;
		margin-bottom: 1rem;
	}

	.moves {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.move {
		display: grid;
		grid-template-columns: 1fr auto auto;
		gap: 1rem;
		padding: 0.75rem 1rem;
		background: #2a2a2a;
		border-radius: 8px;
		font-size: 0.9rem;
	}

	.move.valid {
		border-left: 3px solid #4ade80;
	}

	.move.checkout {
		border-left: 3px solid #22c55e;
		background: rgba(74, 222, 128, 0.05);
	}

	.move.bust,
	.move.invalid {
		border-left: 3px solid #ef4444;
	}

	.move-answer {
		color: #fff;
		font-weight: 500;
	}

	.move-result {
		color: #9ca3af;
		text-transform: uppercase;
		font-size: 0.75rem;
	}

	.move-score {
		color: #4ade80;
		font-weight: 600;
	}
</style>
