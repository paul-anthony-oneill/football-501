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

	// User Settings
	let animationsEnabled = true;
	let showSettings = false;

	// Animation state
	let isShaking = false;
	let isBusting = false;
	let isCalculating = false;
	let displayPoints = 0;
	let showPoints = false;

	// Move history
	let moves: Array<{ answer: string; result: string; scoreBefore: number; scoreAfter: number; matchedAnswer?: string; scoreValue?: number }> = [];

	// Input element reference for focus management
	let inputElement: HTMLInputElement;

	onMount(async () => {
		// Load settings
		const savedSettings = localStorage.getItem('football501_settings');
		if (savedSettings) {
			try {
				const settings = JSON.parse(savedSettings);
				animationsEnabled = settings.animationsEnabled ?? true;
			} catch (e) {
				console.error('Error parsing settings', e);
			}
		}

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

	function toggleAnimations() {
		animationsEnabled = !animationsEnabled;
		localStorage.setItem('football501_settings', JSON.stringify({ animationsEnabled }));
	}

	async function startGame() {
		loading = true;
		feedback = '';
		moves = [];
		isBusting = false;
		isShaking = false;

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

	async function countPoints(target: number) {
		if (!animationsEnabled) return;
		
		displayPoints = 0;
		showPoints = true;
		const duration = 1500; // 1.5 seconds
		const interval = 20;
		const steps = duration / interval;
		const increment = target / steps;
		
		for (let i = 0; i < steps; i++) {
			await new Promise(r => setTimeout(r, interval));
			displayPoints = Math.min(target, Math.round(displayPoints + increment));
		}
		displayPoints = target;
		await new Promise(r => setTimeout(r, 800)); // Dramatic pause at full score
		showPoints = false;
	}

	async function triggerBust() {
		if (!animationsEnabled) return;
		isBusting = true;
		await new Promise(r => setTimeout(r, 2000));
		isBusting = false;
	}

	async function triggerShake() {
		if (!animationsEnabled) return;
		isShaking = true;
		await new Promise(r => setTimeout(r, 500));
		isShaking = false;
	}

	async function submitAnswer() {
		if (!answer.trim() || !gameId || loading) return;

		loading = true;
		if (animationsEnabled) isCalculating = true;
		feedback = '';
		const submittedAnswer = answer;
		answer = ''; // Clear input immediately

		try {
			// Build-up tension
			if (animationsEnabled) {
				await new Promise(r => setTimeout(r, 1200));
			}

			const response = await fetch(`${API_BASE}/practice/games/${gameId}/submit?playerId=${playerId}`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ answer: submittedAnswer })
			});

			if (!response.ok) {
				throw new Error('Failed to submit answer');
			}

			const data = await response.json();
			isCalculating = false;

			// Handle different result types with animations
			if (data.result === 'INVALID') {
				if (animationsEnabled) await triggerShake();
				feedback = `✗ Invalid answer. Try again!`;
				feedbackType = 'error';
			} else {
				// For VALID, BUST, and CHECKOUT, show the points counting
				if (data.scoreValue > 0 && animationsEnabled) {
					await countPoints(data.scoreValue);
				}

				if (data.result === 'BUST') {
					if (animationsEnabled) await triggerBust();
					feedback = `✗ Bust! ${data.reason}. No score change.`;
					feedbackType = 'error';
				} else if (data.result === 'CHECKOUT') {
					feedback = `🎉 YOU WIN! Final score: ${data.scoreAfter} in ${turnCount} turns!`;
					feedbackType = 'success';
				} else {
					feedback = `✓ ${data.matchedAnswer}: ${data.scoreValue} points!`;
					feedbackType = 'success';
				}
			}

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
					scoreAfter: data.scoreAfter,
					matchedAnswer: data.matchedAnswer,
					scoreValue: data.scoreValue
				},
				...moves
			].slice(0, 10);
		} catch (error) {
			isCalculating = false;
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

<main class:shaking={isShaking} class:busting={isBusting}>
	<div class="top-nav">
		<h1>Football 501 ⚽</h1>
		<div class="settings-container">
			<button class="settings-toggle" on:click={() => showSettings = !showSettings}>
				⚙️ Settings
			</button>
			{#if showSettings}
				<div class="settings-dropdown">
					<div class="setting-item">
						<span>Game Animations</span>
						<button 
							class="toggle-btn" 
							class:active={animationsEnabled}
							on:click={toggleAnimations}
						>
							{animationsEnabled ? 'ON' : 'OFF'}
						</button>
					</div>
					<div class="setting-divider"></div>
					<a href="/admin" class="admin-link">Admin Dashboard</a>
				</div>
			{/if}
		</div>
	</div>

	{#if showPoints}
		<div class="points-overlay">
			<div class="points-counter">
				<span class="plus">+</span>
				<span class="count">{displayPoints}</span>
			</div>
		</div>
	{/if}

	{#if isBusting}
		<div class="bust-overlay">
			<div class="bust-text">BUST!</div>
		</div>
	{/if}

	{#if gameStatus === 'NOT_STARTED'}
		<div class="start-screen card">
			<h2>Ready to play?</h2>
			<p>Get your score from 501 to 0 by naming football players that match the question.</p>
			
			<div class="category-select">
				<label for="category">Choose your challenge:</label>
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

			<button class="primary-btn large" on:click={startGame} disabled={loading || categoriesLoading}>
				{loading ? 'Starting...' : 'Start New Game'}
			</button>
		</div>
	{:else}
		<div class="game-container">
			<!-- Game Header with Question and Score -->
			<div class="game-header card">
				<div class="question-block">
					<span class="label">CURRENT CATEGORY: {categories.find(c => c.slug === selectedCategorySlug)?.name}</span>
					<p class="question-text">{question}</p>
				</div>
				<div class="stats-block">
					<div class="stat-item">
						<span class="label">SCORE</span>
						<span class="stat-value highlight" class:calculating={isCalculating}>{score}</span>
					</div>
					<div class="stat-item">
						<span class="label">TURNS</span>
						<span class="stat-value">{turnCount}</span>
					</div>
				</div>
			</div>

			<!-- Interaction Area -->
			{#if gameStatus === 'IN_PROGRESS'}
				<div class="interaction-card card">
					<div class="input-row">
						<div class="search-wrapper">
							<PlayerSearch
								bind:value={answer}
								disabled={loading}
								on:submit={submitAnswer}
								placeholder="Enter player name..."
							/>
						</div>
						<button class="primary-btn" on:click={submitAnswer} disabled={loading || !answer.trim()}>
							{#if isCalculating}
								<span class="calculating-dots">...</span>
							{:else}
								Submit
							{/if}
						</button>
					</div>
					
					{#if feedback && !showPoints && !isBusting}
						<div class="feedback-toast {feedbackType}">
							{feedback}
						</div>
					{/if}
				</div>
			{/if}

			<!-- Win State Summary -->
			{#if isWin}
				<div class="win-card card">
					<div class="win-header">
						<span class="trophy">🏆</span>
						<h2>CHECKOUT!</h2>
					</div>
					<div class="win-stats">
						<div class="win-stat">
							<span class="win-stat-label">Total Turns</span>
							<span class="win-stat-value">{turnCount}</span>
						</div>
						<div class="win-stat">
							<span class="win-stat-label">Efficiency</span>
							<span class="win-stat-value">{Math.round(501 / turnCount)} pts/turn</span>
						</div>
					</div>
					<div class="win-actions">
						<button class="primary-btn" on:click={startGame}>Play Again</button>
						<button class="secondary-btn" on:click={() => gameStatus = 'NOT_STARTED'}>Change Category</button>
					</div>
				</div>
			{/if}

			<!-- Move History -->
			{#if moves.length > 0}
				<div class="history-section">
					<div class="history-header">
						<h3>MATCH HISTORY</h3>
						<span class="count">{moves.length} moves</span>
					</div>
					<div class="moves-list">
						{#each moves as move}
							<div class="move-card {move.result.toLowerCase()}">
								<div class="move-main">
									<span class="move-name">{move.matchedAnswer || move.answer}</span>
									<span class="move-status">{move.result}</span>
								</div>
								<div class="move-details">
									<span class="move-points">
										{#if move.scoreValue > 0}-{move.scoreValue}{:else}0{/if}
									</span>
									<span class="move-progression">{move.scoreBefore} → {move.scoreAfter}</span>
								</div>
							</div>
						{/each}
					</div>
				</div>
			{/if}
		</div>
	{/if}
</main>

<style>
	main {
		max-width: 900px;
		margin: 0 auto;
		padding: var(--space-md) var(--space-lg);
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	.top-nav {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: var(--space-xl);
	}

	h1 {
		font-size: 1.75rem;
		margin: 0;
		letter-spacing: -0.5px;
	}

	.card {
		background: var(--color-surface-variant);
		border-radius: var(--radius-md);
		padding: var(--space-lg);
		box-shadow: var(--shadow-2);
		margin-bottom: var(--space-lg);
	}

	.start-screen {
		text-align: center;
		max-width: 500px;
		margin: var(--space-xxl) auto;
	}

	.category-select {
		margin: var(--space-xl) 0;
		text-align: left;
		display: flex;
		flex-direction: column;
		gap: var(--space-sm);
	}

	.category-select select {
		width: 100%;
		font-size: 1.1rem;
		padding: var(--space-md);
	}

	.primary-btn {
		background: var(--color-primary);
		color: var(--color-on-primary);
		padding: var(--space-md) var(--space-xl);
		border-radius: var(--radius-sm);
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.5px;
	}

	.primary-btn:hover:not(:disabled) {
		filter: brightness(1.1);
		transform: translateY(-2px);
	}

	.primary-btn.large {
		width: 100%;
		font-size: 1.1rem;
	}

	.secondary-btn {
		background: transparent;
		border: 1px solid var(--color-outline);
		color: var(--color-on-surface);
		padding: var(--space-md) var(--space-xl);
		border-radius: var(--radius-sm);
		font-weight: 600;
	}

	.game-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: var(--space-xl);
		background: linear-gradient(135deg, var(--color-surface-variant), #2a332a);
	}

	.question-block {
		flex: 1;
	}

	.label {
		font-size: 0.75rem;
		font-weight: 800;
		color: var(--color-primary);
		opacity: 0.8;
		letter-spacing: 1px;
	}

	.question-text {
		font-size: 1.35rem;
		font-weight: 600;
		margin: var(--space-xs) 0 0;
		line-height: 1.3;
	}

	.stats-block {
		display: flex;
		gap: var(--space-xl);
	}

	.stat-item {
		text-align: right;
	}

	.stat-value {
		display: block;
		font-size: 2.5rem;
		font-weight: 900;
		font-variant-numeric: tabular-nums;
	}

	.stat-value.highlight {
		color: var(--color-primary);
	}

	.interaction-card {
		padding: var(--space-md);
	}

	.input-row {
		display: flex;
		gap: var(--space-md);
	}

	.search-wrapper {
		flex: 1;
	}

	.feedback-toast {
		margin-top: var(--space-md);
		padding: var(--space-sm) var(--space-md);
		border-radius: var(--radius-sm);
		font-weight: 600;
		font-size: 0.9rem;
		animation: slide-up 0.3s ease;
	}

	@keyframes slide-up {
		from { transform: translateY(10px); opacity: 0; }
		to { transform: translateY(0); opacity: 1; }
	}

	.feedback-toast.success { background: rgba(74, 222, 128, 0.15); color: #4ade80; }
	.feedback-toast.error { background: rgba(239, 68, 68, 0.15); color: #ffb4ab; }

	.win-card {
		text-align: center;
		background: linear-gradient(135deg, #00522b, #1a1c1a);
		border: 2px solid var(--color-primary);
	}

	.trophy { font-size: 4rem; display: block; margin-bottom: var(--space-sm); }

	.win-stats {
		display: flex;
		justify-content: center;
		gap: var(--space-xxl);
		margin: var(--space-xl) 0;
	}

	.win-stat-label { display: block; color: var(--color-on-surface-variant); font-size: 0.8rem; }
	.win-stat-value { font-size: 2rem; font-weight: 800; }

	.win-actions {
		display: flex;
		justify-content: center;
		gap: var(--space-md);
	}

	.history-section {
		margin-top: var(--space-md);
	}

	.history-header {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
		margin-bottom: var(--space-md);
		padding: 0 var(--space-xs);
	}

	.history-header h3 {
		font-size: 0.85rem;
		margin: 0;
		color: var(--color-on-surface-variant);
		letter-spacing: 1.5px;
	}

	.history-header .count { font-size: 0.8rem; opacity: 0.6; }

	.moves-list {
		display: flex;
		flex-direction: column;
		gap: var(--space-sm);
	}

	.move-card {
		background: var(--color-surface-variant);
		padding: var(--space-md);
		border-radius: var(--radius-sm);
		display: flex;
		justify-content: space-between;
		align-items: center;
		border-left: 4px solid var(--color-outline);
	}

	.move-card.valid { border-left-color: var(--color-primary); }
	.move-card.checkout { border-left-color: #22c55e; background: rgba(74, 222, 128, 0.05); }
	.move-card.bust, .move-card.invalid { border-left-color: var(--color-error); }

	.move-main { display: flex; flex-direction: column; }
	.move-name { font-weight: 700; font-size: 1rem; }
	.move-status { font-size: 0.7rem; text-transform: uppercase; opacity: 0.6; letter-spacing: 1px; }

	.move-details { text-align: right; }
	.move-points { display: block; color: var(--color-primary); font-weight: 800; font-size: 1.1rem; }
	.move-progression { font-size: 0.8rem; opacity: 0.6; font-variant-numeric: tabular-nums; }

	/* Animations and Overlays */
	.points-overlay, .bust-overlay {
		position: fixed;
		top: 0;
		left: 0;
		width: 100vw;
		height: 100vh;
		display: flex;
		justify-content: center;
		align-items: center;
		z-index: 1000;
	}

	.points-overlay { background: rgba(0, 0, 0, 0.85); }
	.bust-overlay { background: rgba(147, 0, 10, 0.9); }

	.points-counter { font-size: 8rem; font-weight: 900; color: var(--color-primary); }
	.bust-text { font-size: 8rem; font-weight: 900; color: white; }

	.stat-value.calculating {
		animation: tension-pulse 0.5s infinite alternate;
		color: #f59e0b;
	}

	@keyframes tension-pulse {
		from { transform: scale(1); }
		to { transform: scale(1.1); }
	}

	.settings-container { position: relative; }
	.settings-toggle {
		background: var(--color-surface-variant);
		border: 1px solid var(--color-outline);
		color: var(--color-on-surface);
		padding: 0.5rem 0.75rem;
		border-radius: var(--radius-sm);
	}

	.settings-dropdown {
		position: absolute;
		top: calc(100% + 0.5rem);
		right: 0;
		background: var(--color-surface-variant);
		border: 1px solid var(--color-outline);
		border-radius: var(--radius-sm);
		padding: var(--space-md);
		min-width: 220px;
		box-shadow: var(--shadow-3);
	}

	.setting-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		font-size: 0.85rem;
	}

	.toggle-btn {
		background: var(--color-outline);
		color: var(--color-on-surface);
		padding: 0.25rem 0.75rem;
		border-radius: var(--radius-xs);
		font-size: 0.7rem;
		font-weight: 800;
	}

	.toggle-btn.active {
		background: var(--color-primary);
		color: var(--color-on-primary);
	}

	.setting-divider {
		height: 1px;
		background: var(--color-outline);
		margin: var(--space-sm) 0;
		opacity: 0.3;
	}

	.admin-link {
		display: block;
		text-align: center;
		font-size: 0.8rem;
		color: var(--color-primary);
		text-decoration: none;
		font-weight: 600;
	}

	@media (max-width: 600px) {
		.game-header { flex-direction: column; align-items: flex-start; gap: var(--space-md); }
		.stats-block { width: 100%; justify-content: space-between; }
		.stat-value { font-size: 2rem; }
		.input-row { flex-direction: column; }
		.primary-btn { width: 100%; }
	}
</style>
