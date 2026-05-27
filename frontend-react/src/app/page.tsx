"use client";

import { useEffect, useState } from "react";
import LobbyView from "@/components/game/lobby/LobbyView";
import MatchView from "@/components/game/match/MatchView";
import { useGameLoop } from "@/hooks/useGameLoop";
import { useToast } from "@/context/ToastContext";

// ─── Types ────────────────────────────────────────────────────────────────────

interface Category {
  id: string;
  name: string;
  slug: string;
  description: string;
  leader?: { name: string; score: number };
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function GamePage() {
  const { addToast: showToast } = useToast();

  // Game loop — owns all game-session state and API calls
  const {
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
  } = useGameLoop();

  // Lobby state — owned here because it's unrelated to the game loop itself
  const [categories,           setCategories]           = useState<Category[]>([]);
  const [selectedCategorySlug, setSelectedCategorySlug] = useState("football");
  const [playerName,           setPlayerName]           = useState("GUEST_PLAYER");
  const [gameMode,             setGameMode]             = useState<\"solo\" | \"ranked\">(\"solo\");

  // ── Init ───────────────────────────────────────────────────────────────────

  useEffect(() => {
    fetch("/api/categories")
      .then((r) => (r.ok ? r.json() : Promise.reject(r)))
      .then((data: Category[]) => {
        // Augment with mock leader data until the backend exposes it
        const enriched = data.map((cat) => ({
          ...cat,
          leader: { name: "PLAYER_ONE", score: 12 },
        }));
        setCategories(enriched);
        const def = data.find((c) => c.slug === "football");
        setSelectedCategorySlug(def ? def.slug : data[0]?.slug ?? "football");
      })
      .catch(() => showToast("Failed to load categories", "error"));
  }, [showToast]);

  // ── Render ─────────────────────────────────────────────────────────────────

  if (gameStatus === "NOT_STARTED") {
    return (
      <LobbyView
        categories={categories}
        selectedCategorySlug={selectedCategorySlug}
        onSelectCategory={setSelectedCategorySlug}
        onStartGame={() => startNewGame(selectedCategorySlug)}
        playerName={playerName}
        onPlayerNameChange={setPlayerName}
        gameMode={gameMode}
        onGameModeChange={setGameMode}
      />
    );
  }

  const selectedCategory = categories.find((c) => c.slug === selectedCategorySlug);

  return (
    <MatchView
      score={score}
      question={question}
      turnCount={turnCount}
      moves={moves}
      onExit={exitGame}
      onSubmitAnswer={submitAnswer}
      onPlayAgain={() => startNewGame(selectedCategorySlug)}
      categoryName={selectedCategory?.name || "Football"}
      categorySub={selectedCategory?.description || "Darts Edition"}
      entityType={entityType}
      isWin={gameStatus === "COMPLETED"}
      hints={hints}
    />
  );
}
