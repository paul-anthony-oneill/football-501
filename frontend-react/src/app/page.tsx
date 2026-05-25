"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import EntitySearch from "@/components/game/EntitySearch";

// ─── Types ────────────────────────────────────────────────────────────────────

interface Category {
  id: string;
  name: string;
  slug: string;
  description: string;
}

type GameStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";
type FeedbackType = "success" | "error" | "info";

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
  // Game state
  const [gameId, setGameId] = useState<string | null>(null);
  const [playerId] = useState(() => crypto.randomUUID());
  const [score, setScore] = useState(501);
  const [question, setQuestion] = useState("");
  const [turnCount, setTurnCount] = useState(0);
  const [gameStatus, setGameStatus] = useState<GameStatus>("NOT_STARTED");
  const [isWin, setIsWin] = useState(false);

  // Input state
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [feedbackType, setFeedbackType] = useState<FeedbackType>("info");

  // Category state
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategorySlug, setSelectedCategorySlug] = useState("football");
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  // Settings
  const [animationsEnabled, setAnimationsEnabled] = useState(true);
  const [showSettings, setShowSettings] = useState(false);

  // Animation overlays
  const [isShaking, setIsShaking] = useState(false);
  const [isBusting, setIsBusting] = useState(false);
  const [isCalculating, setIsCalculating] = useState(false);
  const [displayPoints, setDisplayPoints] = useState(0);
  const [showPoints, setShowPoints] = useState(false);

  // Move history
  const [moves, setMoves] = useState<Move[]>([]);

  const submitButtonRef = useRef<HTMLButtonElement>(null);

  // ── Init ───────────────────────────────────────────────────────────────────

  useEffect(() => {
    // Load settings
    try {
      const saved = localStorage.getItem("football501_settings");
      if (saved) {
        const s = JSON.parse(saved);
        setAnimationsEnabled(s.animationsEnabled ?? true);
      }
    } catch {}

    // Load categories
    fetch("/api/categories")
      .then((r) => r.ok ? r.json() : Promise.reject(r))
      .then((data: Category[]) => {
        setCategories(data);
        if (data.length > 0) {
          const def = data.find((c) => c.slug === "football");
          setSelectedCategorySlug(def ? def.slug : data[0].slug);
        }
      })
      .catch(() => {
        setFeedback("Failed to load categories. Is the backend running?");
        setFeedbackType("error");
      })
      .finally(() => setCategoriesLoading(false));
  }, []);

  function toggleAnimations() {
    const next = !animationsEnabled;
    setAnimationsEnabled(next);
    localStorage.setItem("football501_settings", JSON.stringify({ animationsEnabled: next }));
  }

  // ── Animation helpers ──────────────────────────────────────────────────────

  async function countPoints(target: number) {
    if (!animationsEnabled) return;
    setDisplayPoints(0);
    setShowPoints(true);
    const duration = 1500;
    const interval = 20;
    const steps = duration / interval;
    const increment = target / steps;
    let current = 0;
    for (let i = 0; i < steps; i++) {
      await sleep(interval);
      current = Math.min(target, Math.round(current + increment));
      setDisplayPoints(current);
    }
    setDisplayPoints(target);
    await sleep(800);
    setShowPoints(false);
  }

  async function triggerBust() {
    if (!animationsEnabled) return;
    setIsBusting(true);
    await sleep(2000);
    setIsBusting(false);
  }

  async function triggerShake() {
    if (!animationsEnabled) return;
    setIsShaking(true);
    await sleep(500);
    setIsShaking(false);
  }

  function sleep(ms: number) {
    return new Promise<void>((r) => setTimeout(r, ms));
  }

  // ── Game actions ───────────────────────────────────────────────────────────

  async function startGame() {
    setLoading(true);
    setFeedback("");
    setMoves([]);
    setIsBusting(false);
    setIsShaking(false);

    try {
      const res = await fetch("/api/practice/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ playerId, categorySlug: selectedCategorySlug }),
      });
      if (!res.ok) throw new Error("Failed to start game");
      const data = await res.json();
      setGameId(data.gameId);
      setScore(data.currentScore);
      setQuestion(data.questionText);
      setTurnCount(data.turnCount);
      setGameStatus(data.status);
      setIsWin(data.isWin);
      setFeedback("Game started! Good luck!");
      setFeedbackType("success");
    } catch {
      setFeedback("Error starting game. Is the backend running?");
      setFeedbackType("error");
    } finally {
      setLoading(false);
    }
  }

  async function submitAnswer() {
    if (!answer.trim() || !gameId || loading) return;

    setLoading(true);
    if (animationsEnabled) setIsCalculating(true);
    setFeedback("");
    const submitted = answer;
    setAnswer("");

    try {
      if (animationsEnabled) await sleep(1200);

      const res = await fetch(`/api/practice/games/${gameId}/submit?playerId=${playerId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answer: submitted }),
      });
      if (!res.ok) throw new Error("Failed to submit answer");
      const data = await res.json();
      setIsCalculating(false);

      if (data.result === "INVALID") {
        if (animationsEnabled) await triggerShake();
        setFeedback("✗ Invalid answer. Try again!");
        setFeedbackType("error");
      } else {
        if (data.scoreValue > 0 && animationsEnabled) await countPoints(data.scoreValue);

        if (data.result === "BUST") {
          if (animationsEnabled) await triggerBust();
          setFeedback(`✗ Bust! ${data.reason}. No score change.`);
          setFeedbackType("error");
        } else if (data.result === "CHECKOUT") {
          setFeedback(`🎉 YOU WIN! Final score: ${data.scoreAfter} in ${turnCount} turns!`);
          setFeedbackType("success");
        } else {
          setFeedback(`✓ ${data.matchedAnswer}: ${data.scoreValue} points!`);
          setFeedbackType("success");
        }
      }

      setScore(data.gameState.currentScore);
      setTurnCount(data.gameState.turnCount);
      setGameStatus(data.gameState.status);
      setIsWin(data.isWin);

      setMoves((prev) =>
        [
          {
            answer: submitted,
            result: data.result,
            scoreBefore: data.scoreBefore,
            scoreAfter: data.scoreAfter,
            matchedAnswer: data.matchedAnswer,
            scoreValue: data.scoreValue,
          },
          ...prev,
        ].slice(0, 10)
      );
    } catch {
      setIsCalculating(false);
      setFeedback("Error submitting answer");
      setFeedbackType("error");
    } finally {
      setLoading(false);
    }
  }

  // ── Derived ────────────────────────────────────────────────────────────────

  const currentCategoryName =
    categories.find((c) => c.slug === selectedCategorySlug)?.name ?? "";

  const mainClasses = [
    "max-w-[900px] mx-auto px-6 py-4 min-h-screen flex flex-col",
    isShaking ? "animate-shaking" : "",
    isBusting ? "animate-busting" : "",
  ]
    .filter(Boolean)
    .join(" ");

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <main className={mainClasses}>
      {/* Top nav */}
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-[1.75rem] m-0 tracking-tight text-[var(--color-primary)]">
          Football 501 ⚽
        </h1>
        <div className="relative">
          <button
            onClick={() => setShowSettings((v) => !v)}
            className="bg-[var(--color-surface-variant)] border border-[var(--color-outline)] text-[var(--color-on-surface)] px-3 py-2 rounded-[var(--radius-sm)] text-sm"
          >
            ⚙️ Settings
          </button>
          {showSettings && (
            <div className="absolute top-[calc(100%+0.5rem)] right-0 bg-[var(--color-surface-variant)] border border-[var(--color-outline)] rounded-[var(--radius-sm)] p-4 min-w-[220px] shadow-[var(--shadow-3)] z-50 animate-slide-down">
              <div className="flex justify-between items-center text-[0.85rem]">
                <span>Game Animations</span>
                <button
                  onClick={toggleAnimations}
                  className={`px-3 py-1 rounded-[var(--radius-xs)] text-[0.7rem] font-extrabold ${
                    animationsEnabled
                      ? "bg-[var(--color-primary)] text-[var(--color-on-primary)]"
                      : "bg-[var(--color-outline)] text-[var(--color-on-surface)]"
                  }`}
                >
                  {animationsEnabled ? "ON" : "OFF"}
                </button>
              </div>
              <div className="h-px bg-[var(--color-outline)] opacity-30 my-2" />
              <Link
                href="/admin"
                className="block text-center text-[0.8rem] text-[var(--color-primary)] no-underline font-semibold"
              >
                Admin Dashboard
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Points overlay */}
      {showPoints && (
        <div className="fixed inset-0 flex justify-center items-center z-[1000] bg-black/85">
          <div className="text-[8rem] font-black text-[var(--color-primary)]">
            {displayPoints}
          </div>
        </div>
      )}

      {/* Bust overlay */}
      {isBusting && (
        <div className="fixed inset-0 flex justify-center items-center z-[1000] bg-[rgba(147,0,10,0.9)]">
          <div className="text-[8rem] font-black text-white">BUST!</div>
        </div>
      )}

      {/* ── Start Screen ────────────────────────────────────────── */}
      {gameStatus === "NOT_STARTED" && (
        <div className="bg-[var(--color-surface-variant)] rounded-[var(--radius-md)] p-6 shadow-[var(--shadow-2)] mb-6 text-center max-w-[500px] mx-auto mt-16">
          <h2>Ready to play?</h2>
          <p className="text-[var(--color-on-surface-variant)]">
            Get your score from 501 to 0 by naming football players that match the question.
          </p>

          <div className="my-8 text-left flex flex-col gap-2">
            <label htmlFor="category" className="text-[0.85rem] font-semibold text-[var(--color-primary)]">
              Choose your challenge:
            </label>
            {categoriesLoading ? (
              <span className="text-[var(--color-on-surface-variant)] text-sm">
                Loading categories…
              </span>
            ) : (
              <select
                id="category"
                value={selectedCategorySlug}
                onChange={(e) => setSelectedCategorySlug(e.target.value)}
                className="w-full text-[1.1rem] p-4 bg-[var(--color-surface)] text-[var(--color-on-surface)] border border-[var(--color-outline)] rounded-[var(--radius-sm)] focus:outline-none focus:border-[var(--color-primary)]"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.slug}>
                    {c.name}
                  </option>
                ))}
              </select>
            )}
          </div>

          {feedback && (
            <div
              className={`mb-4 px-4 py-2 rounded-[var(--radius-sm)] font-semibold text-[0.9rem] text-left ${
                feedbackType === "error"
                  ? "bg-[rgba(239,68,68,0.15)] text-[#ffb4ab]"
                  : "bg-[rgba(74,222,128,0.15)] text-[#4ade80]"
              }`}
            >
              {feedback}
            </div>
          )}

          <button
            onClick={startGame}
            disabled={loading || categoriesLoading}
            className="w-full bg-[var(--color-primary)] text-[var(--color-on-primary)] px-8 py-4 rounded-[var(--radius-sm)] font-extrabold uppercase tracking-[0.5px] text-[1.1rem] disabled:opacity-50 disabled:cursor-not-allowed hover:brightness-110 hover:-translate-y-0.5 transition-all"
          >
            {loading ? "Starting…" : "Start New Game"}
          </button>
        </div>
      )}

      {/* ── Game Screen ─────────────────────────────────────────── */}
      {gameStatus !== "NOT_STARTED" && (
        <div className="game-container flex flex-col gap-6">
          {/* Game header: question + score */}
          <div className="bg-[var(--color-surface-variant)] rounded-[var(--radius-md)] p-6 shadow-[var(--shadow-2)] flex justify-between items-center gap-8 bg-gradient-to-br from-[var(--color-surface-variant)] to-[#2a332a]">
            <div className="flex-1">
              <span className="text-[0.75rem] font-extrabold text-[var(--color-primary)] opacity-80 uppercase tracking-[1px]">
                Current Category: {currentCategoryName}
              </span>
              <p className="text-[1.35rem] font-semibold m-0 mt-1 leading-snug">
                {question}
              </p>
            </div>
            <div className="flex gap-8">
              <div className="text-right">
                <span className="block text-[0.75rem] font-extrabold text-[var(--color-primary)] opacity-80 uppercase tracking-[1px]">
                  Score
                </span>
                <span
                  className={`block text-[2.5rem] font-black tabular-nums text-[var(--color-primary)] ${
                    isCalculating ? "animate-tension-pulse text-amber-400" : ""
                  }`}
                >
                  {score}
                </span>
              </div>
              <div className="text-right">
                <span className="block text-[0.75rem] font-extrabold text-[var(--color-primary)] opacity-80 uppercase tracking-[1px]">
                  Turns
                </span>
                <span className="block text-[2.5rem] font-black tabular-nums">
                  {turnCount}
                </span>
              </div>
            </div>
          </div>

          {/* Input area */}
          {gameStatus === "IN_PROGRESS" && (
            <div className="bg-[var(--color-surface-variant)] rounded-[var(--radius-md)] p-4 shadow-[var(--shadow-2)]">
              <div className="flex gap-4">
                <div className="flex-1">
                  <EntitySearch
                    entityType={question?.config?.entity_type ?? "footballer"}
                    value={answer}
                    onChange={setAnswer}
                    onSubmit={submitAnswer}
                    disabled={loading}
                    placeholder="Enter answer..."
                  />
                </div>
                <button
                  ref={submitButtonRef}
                  onClick={submitAnswer}
                  disabled={loading || !answer.trim()}
                  className="bg-[var(--color-primary)] text-[var(--color-on-primary)] px-8 py-3 rounded-[var(--radius-sm)] font-extrabold uppercase tracking-[0.5px] disabled:opacity-50 disabled:cursor-not-allowed hover:brightness-110 transition-all"
                >
                  {isCalculating ? "…" : "Submit"}
                </button>
              </div>

              {feedback && !showPoints && !isBusting && (
                <div
                  className={`mt-4 px-4 py-2 rounded-[var(--radius-sm)] font-semibold text-[0.9rem] animate-slide-up ${
                    feedbackType === "success"
                      ? "bg-[rgba(74,222,128,0.15)] text-[#4ade80]"
                      : "bg-[rgba(239,68,68,0.15)] text-[#ffb4ab]"
                  }`}
                >
                  {feedback}
                </div>
              )}
            </div>
          )}

          {/* Win card */}
          {isWin && (
            <div className="bg-gradient-to-br from-[#00522b] to-[#1a1c1a] border-2 border-[var(--color-primary)] rounded-[var(--radius-md)] p-6 shadow-[var(--shadow-2)] text-center">
              <span className="text-[4rem] block mb-2">🏆</span>
              <h2 className="text-[var(--color-primary)]">CHECKOUT!</h2>
              <div className="flex justify-center gap-16 my-8">
                {[
                  { label: "Total Turns", value: turnCount },
                  { label: "Efficiency", value: `${Math.round(501 / turnCount)} pts/turn` },
                ].map((s) => (
                  <div key={s.label}>
                    <span className="block text-[var(--color-on-surface-variant)] text-[0.8rem]">
                      {s.label}
                    </span>
                    <span className="text-[2rem] font-extrabold">{s.value}</span>
                  </div>
                ))}
              </div>
              <div className="flex justify-center gap-4">
                <button
                  onClick={startGame}
                  className="bg-[var(--color-primary)] text-[var(--color-on-primary)] px-8 py-4 rounded-[var(--radius-sm)] font-extrabold uppercase"
                >
                  Play Again
                </button>
                <button
                  onClick={() => setGameStatus("NOT_STARTED")}
                  className="bg-transparent border border-[var(--color-outline)] text-[var(--color-on-surface)] px-8 py-4 rounded-[var(--radius-sm)] font-semibold"
                >
                  Change Category
                </button>
              </div>
            </div>
          )}

          {/* Move history */}
          {moves.length > 0 && (
            <div>
              <div className="flex justify-between items-end mb-4 px-1">
                <h3 className="text-[0.85rem] m-0 text-[var(--color-on-surface-variant)] uppercase tracking-[1.5px]">
                  Match History
                </h3>
                <span className="text-[0.8rem] opacity-60">{moves.length} moves</span>
              </div>
              <div className="flex flex-col gap-2">
                {moves.map((move, i) => {
                  const resultKey = move.result.toLowerCase();
                  const borderColor =
                    resultKey === "valid"
                      ? "border-l-[var(--color-primary)]"
                      : resultKey === "checkout"
                      ? "border-l-[#22c55e]"
                      : "border-l-[var(--color-error)]";

                  return (
                    <div
                      key={i}
                      className={`bg-[var(--color-surface-variant)] p-4 rounded-[var(--radius-sm)] flex justify-between items-center border-l-4 ${borderColor} ${
                        resultKey === "checkout" ? "bg-[rgba(74,222,128,0.05)]" : ""
                      }`}
                    >
                      <div className="flex flex-col">
                        <span className="font-bold text-base">
                          {move.matchedAnswer || move.answer}
                        </span>
                        <span className="text-[0.7rem] uppercase opacity-60 tracking-[1px]">
                          {move.result}
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="block text-[var(--color-primary)] font-extrabold text-[1.1rem]">
                          {move.scoreValue && move.scoreValue > 0 ? `-${move.scoreValue}` : "0"}
                        </span>
                        <span className="text-[0.8rem] opacity-60 tabular-nums">
                          {move.scoreBefore} → {move.scoreAfter}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </main>
  );
}
