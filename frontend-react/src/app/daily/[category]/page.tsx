"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api/client";
import { useToast } from "@/context/ToastContext";

interface CategoryStatus {
  categorySlug: string;
  categoryName: string;
  startingScore: number;
  questionText: string;
  hasChallenge: boolean;
}

export default function DailyCategoryPage() {
  const params = useParams();
  const router = useRouter();
  const categorySlug = params.category as string;

  const [status, setStatus] = useState<CategoryStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    apiFetch(`/api/daily-challenge/${encodeURIComponent(categorySlug)}`)
      .then(async (res) => {
        if (!res.ok) {
          if (res.status === 404) throw new Error("No challenge found for this category today");
          throw new Error("Failed to load challenge");
        }
        return res.json();
      })
      .then((data) => {
        setStatus(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "Error loading challenge");
        setLoading(false);
      });
  }, [categorySlug]);

  const handlePlay = async () => {
    if (!status) return;
    setStarting(true);
    try {
      const res = await apiFetch(`/api/daily-challenge/${encodeURIComponent(categorySlug)}/start`, {
        method: "POST",
      });
      if (!res.ok) {
        const text = await res.text().catch(() => "");
        let msg = "Failed to start challenge";
        try { const p = JSON.parse(text); msg = p.error || p.message || text; } catch { msg = text || msg; }
        throw new Error(msg);
      }
      const game = await res.json();
      sessionStorage.setItem("activeGameState", JSON.stringify({
        gameId: game.gameId,
        label: status.categoryName,
        gameType: "daily-challenge",
      }));
      router.push("/");
    } catch (err) {
      addToast((err as Error).message || "Error starting daily challenge", "error");
    } finally {
      setStarting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-black">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-h-accent mx-auto mb-4" />
          <p className="text-gray-400 text-lg font-vt323 tracking-widest">Loading...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-black text-white font-vt323 gap-6">
        <div className="text-red-500 text-[24px] tracking-widest">{error}</div>
        <a
          href="/daily"
          className="text-h-accent text-[18px] tracking-widest hover:underline"
        >
          ← View all challenges
        </a>
      </div>
    );
  }

  if (!status) return null;

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-black text-white font-vt323 p-6">
      <div className="max-w-md w-full border-2 border-h-accent rounded-sm p-8 bg-black">
        <div className="flex items-baseline justify-between mb-4">
          <h1 className="text-h-accent text-[28px] tracking-widest">
            {status.categoryName} Daily
          </h1>
          <span className="text-h-dim text-[12px] tracking-widest uppercase">Today</span>
        </div>

        <div className="text-h-accent text-[64px] tracking-widest text-center mb-4">
          {status.startingScore.toString().padStart(3, "0")}
        </div>

        <p className="text-h-dim text-[14px] leading-snug mb-8 text-center">
          {status.questionText}
        </p>

        <button
          onClick={handlePlay}
          disabled={starting}
          className="w-full border-2 border-h-accent text-h-accent text-[24px] tracking-widest py-4 hover:bg-h-accent hover:text-black transition-colors disabled:opacity-50"
        >
          {starting ? "STARTING..." : "PLAY NOW"}
        </button>

        <div className="mt-6 text-center">
          <a
            href="/daily"
            className="text-h-dim text-[14px] tracking-widest hover:text-h-accent transition-colors"
          >
            ← All challenges
          </a>
        </div>
      </div>

      <footer className="mt-8 text-h-dim text-[12px] tracking-widest">
        Shared via Trivia 501 ·{" "}
        <a href="/daily" className="text-h-accent hover:underline">
          trivia501.com/daily
        </a>
      </footer>
    </div>
  );
}
