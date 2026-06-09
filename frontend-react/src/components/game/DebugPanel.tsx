"use client";

import { useState, useEffect } from "react";
import { apiFetch } from "@/lib/api/client";

interface AnswerItem {
  id: string;
  displayText: string;
  score: number;
  isValidDarts: boolean;
  isBust: boolean;
}

interface DebugPanelProps {
  gameId: string | null;
  gameType: "freeplay" | "daily-challenge";
}

export default function DebugPanel({ gameId, gameType }: DebugPanelProps) {
  const [open, setOpen] = useState(false);
  const [answers, setAnswers] = useState<AnswerItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !gameId) return;
    setLoading(true);
    setError(null);
    const base =
      gameType === "daily-challenge" ? "/api/daily-challenge" : "/api/freeplay";
    apiFetch(`${base}/games/${gameId}/answers`)
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        setAnswers(
          (data as AnswerItem[]).sort((a, b) => b.score - a.score),
        );
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [open, gameId, gameType]);

  // Ctrl+Shift+D toggles the panel
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.ctrlKey && e.shiftKey && e.key === "D") {
        e.preventDefault();
        setOpen((v) => !v);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  if (!gameId) return null;

  return (
    <>
      {/* Toggle button */}
      <button
        onClick={() => setOpen((v) => !v)}
        className="fixed bottom-4 right-4 z-50 border border-tele-accent bg-black text-tele-accent px-2 py-1 text-sm font-vt323 hover:bg-tele-accent hover:text-black transition-colors"
        title="Debug: Show all answers (Ctrl+Shift+D)"
      >
        {open ? "[x] DEBUG" : "[?] DEBUG"}
      </button>

      {/* Panel */}
      {open && (
        <div className="fixed bottom-14 right-4 z-50 w-96 max-h-[70vh] border-2 border-tele-accent bg-black text-white font-vt323 shadow-lg flex flex-col">
          <div className="bg-tele-accent text-black px-3 py-1 text-sm flex justify-between items-center">
            <span>ALL ANSWERS ({answers.length})</span>
            <button
              onClick={() => setOpen(false)}
              className="hover:opacity-70 text-base leading-none"
            >
              ✕
            </button>
          </div>
          <div className="overflow-y-auto flex-1 p-2 text-sm">
            {loading && (
              <div className="text-tele-cyan py-4 text-center">Loading...</div>
            )}
            {error && (
              <div className="text-tele-danger py-4 text-center">
                Error: {error}
              </div>
            )}
            {!loading &&
              !error &&
              answers.map((a) => (
                <div
                  key={a.id}
                  className={`flex justify-between items-baseline py-1 border-b border-[#333] ${
                    !a.isValidDarts || a.isBust ? "opacity-50" : ""
                  }`}
                >
                  <span
                    className={`truncate flex-1 mr-2 ${
                      a.isBust
                        ? "text-tele-danger line-through"
                        : !a.isValidDarts
                          ? "text-[#888]"
                          : "text-tele-green"
                    }`}
                  >
                    {a.displayText}
                  </span>
                  <span className="text-tele-accent tabular-nums mr-2">
                    {a.score}
                  </span>
                  <span className="text-[11px] tracking-wider w-14 text-right">
                    {a.isBust ? (
                      <span className="text-tele-danger">BUST</span>
                    ) : !a.isValidDarts ? (
                      <span className="text-[#666]">INV</span>
                    ) : (
                      <span className="text-tele-green">OK</span>
                    )}
                  </span>
                </div>
              ))}
            {!loading && !error && answers.length === 0 && (
              <div className="text-[#888] py-4 text-center">
                No answers found
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
