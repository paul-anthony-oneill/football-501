"use client";

import { useState } from "react";
import { useToast } from "@/context/ToastContext";

// ─── Types ────────────────────────────────────────────────────────────────────

export interface Move {
  answer: string;
  result: string;
  scoreBefore: number;
  scoreAfter: number;
  matchedAnswer?: string;
  scoreValue?: number;
}

export interface GameHints {
  /** Remaining unused answers worth exactly 180 points. Shown while score > 180. */
  maxScoresLeft: number;
  /** Remaining unused answers that would win the game in one move. Shown while score ≤ 180. */
  checkoutsLeft: number;
}

export type GameStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

// ─── Hook ─────────────────────────────────────────────────────────────────────

export interface GameLoopState {
  /** Current game score (starts at 501, counts down to 0). */
  score: number;
  /** The active question text from the server. Empty string before game starts. */
  question: string;
  /** Number of turns taken so far. */
  turnCount: number;
  /** Overall lifecycle of the game session. */
  gameStatus: GameStatus;
  /** Move history, newest first. */
  moves: Move[];
  /** Entity type driving the autocomplete dropdown (e.g. "footballer", "city"). */
  entityType: string;
  /** In-game hint stats from the server; null until the first response. */
  hints: GameHints | null;
}

export interface GameLoopActions {
  /** Start a new solo game for the given category slug. */
  startNewGame: (categorySlug: string) => Promise<void>;
  /** Submit an answer for the current game turn. */
  submitAnswer: (answer: string) => Promise<void>;
  /** Exit the current game and return to the lobby. */
  exitGame: () => void;
}

/**
 * `useGameLoop` — owns all game session state and the API calls that drive it.
 *
 * Extracted from `GamePage` so the page component can stay focused on layout
 * and lobby-selection concerns. WebSocket support will be added here when
 * the multiplayer feature is implemented.
 *
 * @returns combined game state and action callbacks
 */
export function useGameLoop(): GameLoopState & GameLoopActions {
  const { addToast } = useToast();

  const [score,      setScore]      = useState(501);
  const [question,   setQuestion]   = useState("");
  const [turnCount,  setTurnCount]  = useState(0);
  const [gameStatus, setGameStatus] = useState<GameStatus>("NOT_STARTED");
  const [moves,      setMoves]      = useState<Move[]>([]);
  const [entityType, setEntityType] = useState("footballer");
  const [hints,      setHints]      = useState<GameHints | null>(null);

  // Internal game ID used to address subsequent move submissions
  const [gameId, setGameId] = useState<string | null>(null);

  // ── Actions ─────────────────────────────────────────────────────────────────

  async function startNewGame(categorySlug: string) {
    try {
      const res = await fetch("/api/solo/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ categorySlug }),
      });

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to start game");
        throw new Error(msg);
      }

      const game = await res.json();
      setGameId(game.gameId);
      setScore(game.currentScore);
      setQuestion(game.questionText);
      setTurnCount(0);
      setMoves([]);
      setEntityType(game.entityType ?? "footballer");
      setHints(game.hints ?? null);
      setGameStatus("IN_PROGRESS");

      document.body.classList.remove("theme-home");
      document.body.classList.add("theme-teletext");

      addToast("Game started!", "success");
    } catch (err) {
      addToast((err as Error).message || "Error starting game", "error");
    }
  }

  async function submitAnswer(answer: string) {
    if (!gameId || !answer.trim()) return;

    try {
      const res = await fetch(`/api/solo/games/${gameId}/submit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answer: answer.trim() }),
      });

      if (!res.ok) throw new Error("Validation failed");

      const result = await res.json();

      const newMove: Move = {
        answer:        answer.trim(),
        result:        result.result,
        scoreBefore:   score,
        scoreAfter:    result.scoreAfter,
        matchedAnswer: result.matchedAnswer,
        scoreValue:    result.scoreValue,
      };

      setMoves((prev) => [newMove, ...prev]);
      setScore(result.scoreAfter);
      setTurnCount((prev) => prev + 1);
      setHints(result.gameState?.hints ?? null);

      if      (result.result === "VALID")   addToast(`Correct! -${result.scoreValue}`, "success");
      else if (result.result === "BUST")    addToast("BUST!", "error");
      else if (result.result === "INVALID") addToast("Not a valid answer — try again", "error");

      if (result.isWin) setGameStatus("COMPLETED");
    } catch {
      addToast("Error validating answer", "error");
    }
  }

  function exitGame() {
    setGameStatus("NOT_STARTED");
    setGameId(null);
    document.body.classList.remove("theme-teletext");
    document.body.classList.add("theme-home");
  }

  // ── Return ───────────────────────────────────────────────────────────────────

  return {
    score,
    question,
    turnCount,
    gameStatus,
    moves,
    entityType,
    hints,
    startNewGame,
    submitAnswer,
    exitGame,
  };
}
