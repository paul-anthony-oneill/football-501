"use client";

import { useState, useMemo } from "react";
import LobbyView from "@/components/game/lobby/LobbyView";
import MatchView from "@/components/game/match/MatchView";
import { useGameLoop } from "@/hooks/useGameLoop";
import { CATEGORIES } from "@/lib/questionHierarchy";

// ─── Types ────────────────────────────────────────────────────────────────────

interface LobbyCategory {
  id: string;
  name: string;
  slug: string;
  description: string;
  theme?: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Extract the top-level category slug from a hierarchical path like "football:premier-league:goals:man-city" */
function rootSlug(pathSlug: string): string {
  return pathSlug.split(":")[0] ?? pathSlug;
}

/** Derive a display category name from the selection label (e.g. "Football > Premier League > Goals > Random") */
function categoryLabel(label: string): { name: string; sub: string } {
  const parts = label.split(" > ");
  const name = parts[0] ?? "Trivia";
  const sub = parts.slice(1).join(" > ") || "Darts Edition";
  return { name, sub };
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function GamePage() {
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

  // Flatten the static hierarchy for the lobby card grid
  const lobbyCategories: LobbyCategory[] = useMemo(
    () =>
      CATEGORIES.map((c) => ({
        id: c.id,
        name: c.name,
        slug: c.id,
        description: c.description,
        theme: c.theme,
      })),
    [],
  );

  // Lobby state
  const [playerName, setPlayerName] = useState("GUEST_PLAYER");
  const [gameMode, setGameMode] = useState<"solo" | "ranked">("solo");
  // Track the last selection so we can replay and display in MatchView
  const [lastSlug, setLastSlug] = useState("football");
  const [lastLabel, setLastLabel] = useState("Football");

  // ── Handlers ─────────────────────────────────────────────────────────────────

  const handleStartGame = async (slug: string, label: string) => {
    setLastSlug(slug);
    setLastLabel(label);
    // For now we pass the root category slug to the backend, which matches
    // the flat /api/categories structure. The full hierarchical path is
    // preserved in lastSlug/lastLabel for when the backend supports it.
    await startNewGame(rootSlug(slug));
  };

  const handlePlayAgain = async () => {
    await startNewGame(rootSlug(lastSlug));
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  if (gameStatus === "NOT_STARTED") {
    return (
      <LobbyView
        categories={lobbyCategories}
        onStartGame={handleStartGame}
        playerName={playerName}
        onPlayerNameChange={setPlayerName}
        gameMode={gameMode}
        onGameModeChange={setGameMode}
      />
    );
  }

  const { name: catName, sub: catSub } = categoryLabel(lastLabel);

  return (
    <MatchView
      score={score}
      question={question}
      turnCount={turnCount}
      moves={moves}
      onExit={exitGame}
      onSubmitAnswer={submitAnswer}
      onPlayAgain={handlePlayAgain}
      categoryName={catName}
      categorySub={catSub}
      entityType={entityType}
      isWin={gameStatus === "COMPLETED"}
      hints={hints}
    />
  );
}
