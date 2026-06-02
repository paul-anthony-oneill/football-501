"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useDailyChallenge, type CategoryChallenge } from "@/hooks/useDailyChallenge";

export default function DailyPage() {
  const router = useRouter();
  const { challenges, loading, error, date } = useDailyChallenge();
  const [starting, setStarting] = useState<string | null>(null);

  const handlePlay = async (slug: string, label: string) => {
    setStarting(slug);
    try {
      const res = await fetch(`/api/daily-challenge/${encodeURIComponent(slug)}/start`, {
        method: "POST",
      });
      if (!res.ok) {
        const text = await res.text().catch(() => "");
        let msg = "Failed to start challenge";
        try { const p = JSON.parse(text); msg = p.error || p.message || text; } catch { msg = text || msg; }
        throw new Error(msg);
      }
      const game = await res.json();
      // Store game state and redirect to main page (will restore from sessionStorage)
      sessionStorage.setItem("activeGameState", JSON.stringify({
        gameId: game.gameId,
        label: label,
        gameType: "daily-challenge",
      }));
      router.push("/");
    } catch (err) {
      alert((err as Error).message || "Error starting daily challenge");
    } finally {
      setStarting(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-black">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-h-accent mx-auto mb-4" />
          <p className="text-gray-400 text-lg font-vt323 tracking-widest">Loading challenges...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black text-white font-vt323">
      <div className="max-w-4xl mx-auto px-6 py-10">
        <header className="border-b border-h-rule pb-6 mb-8">
          <div className="flex items-baseline justify-between">
            <h1 className="text-h-accent text-[40px] tracking-widest">DAILY CHALLENGES</h1>
            <span className="text-h-dim text-[14px] tracking-widest uppercase">
              {date ? new Date(date).toLocaleDateString("en-GB", {
                weekday: "short", day: "numeric", month: "short", year: "numeric",
              }) : "Today"}
            </span>
          </div>
          <p className="text-h-dim text-[14px] mt-2 tracking-wider">
            One question per category. Everyone gets the same target. Share your result.
          </p>
        </header>

        {error && (
          <div className="text-red-500 text-center py-6 text-[18px]">{error}</div>
        )}

        {!error && challenges.length === 0 && (
          <div className="text-center py-16">
            <div className="text-h-dim text-[24px] tracking-widest mb-4">NO CHALLENGES TODAY</div>
            <p className="text-h-dim text-[14px]">Check back at midnight UTC for new challenges.</p>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {challenges.map((dc: CategoryChallenge) => (
            <div
              key={dc.categorySlug}
              className="border-2 border-h-accent rounded-sm p-6 bg-black flex flex-col"
            >
              <div className="flex items-baseline justify-between mb-3">
                <span className="font-bricolage font-bold text-[22px] tracking-tight">
                  {dc.categoryName}
                </span>
                <span className="font-plex text-[10px] tracking-widest text-h-dim uppercase">
                  Daily
                </span>
              </div>
              <div className="text-h-accent text-[48px] tracking-widest mb-3">
                {dc.startingScore.toString().padStart(3, "0")}
              </div>
              <div className="text-h-dim text-[14px] leading-snug mb-4 line-clamp-2">
                {dc.questionText || "Loading..."}
              </div>
              <button
                onClick={() => handlePlay(dc.categorySlug, dc.categoryName)}
                disabled={starting === dc.categorySlug}
                className="mt-auto border-2 border-h-accent text-h-accent text-[20px] tracking-widest py-3 px-6 hover:bg-h-accent hover:text-black transition-colors disabled:opacity-50"
              >
                {starting === dc.categorySlug ? "STARTING..." : "PLAY NOW"}
              </button>
            </div>
          ))}
        </div>

        <footer className="mt-12 pt-6 border-t border-h-rule text-center">
          <a
            href="/"
            className="text-h-dim text-[14px] tracking-widest hover:text-h-accent transition-colors"
          >
            ← BACK TO LOBBY
          </a>
        </footer>
      </div>
    </div>
  );
}
