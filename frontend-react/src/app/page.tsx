"use client";

import { useEffect, useState } from "react";
import LobbyView from "@/components/game/lobby/LobbyView";
import MatchView from "@/components/game/match/MatchView";
import { useToast } from "@/context/ToastContext";

// ─── Types ────────────────────────────────────────────────────────────────────

interface Category {
  id: string;
  name: string;
  slug: string;
  description: string;
  leader?: { name: string; score: number };
}

type GameStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

interface Move {
  answer: string;
  result: string;
  scoreBefore: number;
  scoreAfter: number;
  matchedAnswer?: string;
  scoreValue?: number;
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function GamePage() {
  const { addToast: showToast } = useToast();

  // Core state
  const [gameId, setGameId] = useState<string | null>(null);
  // Guest player ID — stable for the session, sent with every request
  const [playerId] = useState<string>(() => crypto.randomUUID());
  const [score, setScore] = useState(501);
  const [question, setQuestion] = useState("");
  const [turnCount, setTurnCount] = useState(0);
  const [gameStatus, setGameStatus] = useState<GameStatus>("NOT_STARTED");
  const [moves, setMoves] = useState<Move[]>([]);

  // Lobby state
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategorySlug, setSelectedCategorySlug] = useState("football");
  const [playerName, setPlayerName] = useState("GUEST_PLAYER");
  const [gameMode, setGameMode] = useState<'practice' | 'ranked'>('practice');

  // ── Init ───────────────────────────────────────────────────────────────────

  useEffect(() => {
    // Load categories from backend
    fetch("/api/categories")
      .then((r) => r.ok ? r.json() : Promise.reject(r))
      .then((data: Category[]) => {
        // Mocking leader data for the new UI until backend supports it
        const enhancedData = data.map(cat => ({
          ...cat,
          leader: { name: "PLAYER_ONE", score: 12 }
        }));
        setCategories(enhancedData);
        if (data.length > 0) {
          const def = data.find((c) => c.slug === "football");
          setSelectedCategorySlug(def ? def.slug : data[0].slug);
        }
      })
      .catch(() => {
        showToast("Failed to load categories", "error");
      });
  }, [showToast]);

  // ── Handlers ───────────────────────────────────────────────────────────────

  async function startNewGame() {
    try {
      const res = await fetch("/api/practice/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ playerId, categorySlug: selectedCategorySlug }),
      });

      if (!res.ok) throw new Error("Failed to start game");

      const game = await res.json();
      setGameId(game.gameId);
      setScore(game.currentScore);
      setQuestion(game.questionText);
      setTurnCount(0);
      setMoves([]);
      setGameStatus("IN_PROGRESS");
      
      // Switch body theme
      document.body.classList.remove('theme-home');
      document.body.classList.add('theme-teletext');
      
      showToast("Game started!", "success");
    } catch (err) {
      showToast("Error starting game", "error");
    }
  }

  async function submitAnswer(answer: string) {
    if (!gameId || !answer.trim()) return;

    try {
      const res = await fetch(`/api/practice/games/${gameId}/submit?playerId=${playerId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answer: answer.trim() }),
      });

      if (!res.ok) throw new Error("Validation failed");

      const result = await res.json();

      const newMove: Move = {
        answer: answer.trim(),
        result: result.result,
        scoreBefore: score,
        scoreAfter: result.scoreAfter,
        matchedAnswer: result.matchedAnswer,
        scoreValue: result.scoreValue,
      };

      setMoves([newMove, ...moves]);
      setScore(result.scoreAfter);
      setTurnCount(prev => prev + 1);

      if (result.result === 'VALID') {
        showToast(`Correct! -${result.scoreValue}`, "success");
      } else if (result.result === 'BUST') {
        showToast("BUST!", "error");
      }

      if (result.isWin) {
        setGameStatus("COMPLETED");
        showToast("CHECKOUT! You win!", "success");
      }
    } catch (err) {
      showToast("Error validating answer", "error");
    }
  }

  function exitGame() {
    setGameStatus("NOT_STARTED");
    document.body.classList.remove('theme-teletext');
    document.body.classList.add('theme-home');
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  if (gameStatus === "NOT_STARTED") {
    return (
      <LobbyView 
        categories={categories}
        selectedCategorySlug={selectedCategorySlug}
        onSelectCategory={setSelectedCategorySlug}
        onStartGame={startNewGame}
        playerName={playerName}
        onPlayerNameChange={setPlayerName}
        gameMode={gameMode}
        onGameModeChange={setGameMode}
      />
    );
  }

  const selectedCategory = categories.find(c => c.slug === selectedCategorySlug);

  return (
    <MatchView 
      score={score}
      question={question}
      turnCount={turnCount}
      moves={moves}
      onExit={exitGame}
      onSubmitAnswer={submitAnswer}
      categoryName={selectedCategory?.name || "Football"}
      categorySub={selectedCategory?.description || "Darts Edition"}
    />
  );
}
