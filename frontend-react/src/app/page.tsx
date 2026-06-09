"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import LobbyView from "@/components/game/lobby/LobbyView";
import MatchView from "@/components/game/match/MatchView";
import AnimatedScorePopup from "@/components/game/AnimatedScorePopup";
import { useGameLoop, getSavedLabel } from "@/hooks/useGameLoop";
import { useDailyChallenge } from "@/hooks/useDailyChallenge";
import { useToast } from "@/context/ToastContext";
import { apiFetch } from "@/lib/api/client";
import type { FootballFilter } from "@/lib/api/footballApi";

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Extract the top-level category slug from a hierarchical path like "football:premier-league" */
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

// ─── Auth redirect handler (must be inside Suspense — uses useSearchParams) ───

function AuthRequiredRedirect() {
  const searchParams = useSearchParams();
  const { addToast } = useToast();
  useEffect(() => {
    if (searchParams.get("auth_required")) {
      addToast("Please sign in to access admin pages", "info");
      const url = new URL(window.location.href);
      url.searchParams.delete("auth_required");
      window.history.replaceState({}, "", url.toString());
    }
  }, [searchParams, addToast]);
  return null;
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
    isAnimating,
    flashVersion,
    popup,
    gameType,
    gameId,
    onPopupComplete,
    startNewGame,
    startDailyChallenge,
    submitAnswer,
    exitGame,
  } = useGameLoop();

  // Daily challenge status
  const { challenges: dailyChallenges, loading: dailyLoading } = useDailyChallenge();

  const { addToast } = useToast();

  // Share state
  const [sharing, setSharing] = useState(false);

  // Track the last selection so we can replay and display in MatchView.
  // On mount, try to recover the label from a saved game (refresh recovery).
  const [lastSlug, setLastSlug] = useState(() => getSavedLabel() ?? "football");
  const [lastLabel, setLastLabel] = useState(() => {
    const saved = getSavedLabel();
    return saved ?? "Football";
  });

  // ── Handlers ─────────────────────────────────────────────────────────────────

  const handleStartGame = async (slug: string, label: string, targetScore: number, footballFilter?: FootballFilter) => {
    setLastSlug(slug);
    setLastLabel(label);
    await startNewGame(rootSlug(slug), label, targetScore, footballFilter);
  };

  const handleStartDailyChallenge = async (categorySlug: string, label: string) => {
    setLastSlug(categorySlug);
    setLastLabel(label);
    await startDailyChallenge(categorySlug, label);
  };

  const handleShare = async () => {
    if (!gameId || sharing) return;
    setSharing(true);
    try {
      const res = await apiFetch(`/api/daily-challenge/share/${gameId}`);
      if (!res.ok) throw new Error("Failed to get share data");
      const data = await res.json();

      // Build Wordle-style share text
      const emojiMap: Record<string, string> = {
        VALID: "🟩",    // 🟩
        BUST: "🟥",     // 🟥
        INVALID: "⬜",
        CHECKOUT: "🎯", // 🎯
      };
      const emojiLine = (data.moveEmojis as string[])
        .map((e: string) => emojiMap[e] ?? "⬜")
        .join("");

      const dateStr = data.challengeDate
        ? new Date(data.challengeDate).toLocaleDateString("en-GB", {
            day: "numeric", month: "short", year: "numeric",
          })
        : "Today";

      const shareText = [
        `⚽ FOOTBALL 501 — ${data.categoryName?.toString().toUpperCase() ?? "DAILY"}`,
        `${dateStr} — Target: ${data.startingScore}`,
        "",
        emojiLine,
        `Score: ${(data.finalScore as number) <= 0 ? "000" : String(data.finalScore).padStart(3, "0")} | ${data.turnCount} turns`,
        "",
        `${window.location.origin}/daily/${data.categorySlug ?? "football"}`,
      ].join("\n");

      // Try native share on mobile, fall back to clipboard
      if (navigator.share) {
        await navigator.share({ text: shareText });
      } else {
        await navigator.clipboard.writeText(shareText);
      }
      addToast("Result copied to clipboard!", "success");
    } catch (err) {
      // User cancelled share — not an error
      if ((err as Error).name !== "AbortError") {
        addToast("Failed to share result", "error");
      }
    } finally {
      setSharing(false);
    }
  };

  const handlePlayAgain = async () => {
    await startNewGame(rootSlug(lastSlug), lastLabel);
  };

  // ── Restoring state ─────────────────────────────────────────────────────────

  // AuthRequiredRedirect uses useSearchParams — must live inside Suspense
  const authRedirect = (
    <Suspense fallback={null}>
      <AuthRequiredRedirect />
    </Suspense>
  );

  if (gameStatus === "RESTORING") {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950">
        {authRedirect}
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500 mx-auto mb-4" />
          <p className="text-gray-400 text-lg">Restoring game...</p>
        </div>
      </div>
    );
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  if (gameStatus === "NOT_STARTED") {
    return (
      <>
        {authRedirect}
        <LobbyView
          onStartGame={handleStartGame}
          onStartDailyChallenge={handleStartDailyChallenge}
          dailyChallenges={dailyChallenges}
          dailyLoading={dailyLoading}
        />
      </>
    );
  }

  const { name: catName, sub: catSub } = categoryLabel(lastLabel);

  return (
    <>
      {authRedirect}
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
        disabled={isAnimating}
        flashVersion={flashVersion}
        onShare={gameType === "daily-challenge" ? handleShare : undefined}
        sharing={sharing}
        gameId={gameId}
        debugGameType={gameType}
      />
      {popup && (
        <AnimatedScorePopup
          scoreValue={popup.scoreValue}
          result={popup.result}
          reason={popup.reason}
          onComplete={onPopupComplete}
        />
      )}
    </>
  );
}
