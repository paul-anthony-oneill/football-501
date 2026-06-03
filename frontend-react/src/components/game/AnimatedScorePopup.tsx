"use client";

import { useState, useEffect, useRef } from "react";

interface AnimatedScorePopupProps {
  scoreValue: number;
  result: "VALID" | "BUST" | "INVALID";
  onComplete: () => void;
}

type Phase = "counting" | "flashing" | "showing" | "invalid";

export default function AnimatedScorePopup({
  scoreValue,
  result,
  onComplete,
}: AnimatedScorePopupProps) {
  const prefersReducedMotion =
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const [display, setDisplay] = useState(0);
  const [phase, setPhase] = useState<Phase>(() => {
    if (result === "INVALID") return "invalid";
    if (prefersReducedMotion) return "showing";
    return "counting";
  });
  const frameRef = useRef<number | null>(null);

  const target = scoreValue;

  // ── Counting animation (0 → target) ──────────────────────────────────────
  useEffect(() => {
    if (phase !== "counting") return;

    const duration = 2000 + Math.random() * 2000;
    const wobbleAmp = 0.08 + Math.random() * 0.12;
    const wobbleHz = 2 + Math.random() * 3;
    const startTime = performance.now();

    const tick = (now: number) => {
      const elapsed = now - startTime;
      const raw = Math.min(elapsed / duration, 1);

      // Cubic ease-in-out
      const eased =
        raw < 0.5
          ? 4 * raw * raw * raw
          : 1 - Math.pow(-2 * raw + 2, 3) / 2;

      // Fading sinusoidal wobble
      const wobble =
        Math.sin(raw * Math.PI * wobbleHz) * wobbleAmp * (1 - raw);
      const progress = Math.max(0, Math.min(1, eased + wobble));

      setDisplay(Math.round(target * progress));

      if (raw < 1) {
        frameRef.current = requestAnimationFrame(tick);
      } else {
        setDisplay(target);
        setPhase(result === "BUST" ? "flashing" : "showing");
      }
    };

    frameRef.current = requestAnimationFrame(tick);

    return () => {
      if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase]);

  // ── Hold final state then dismiss ────────────────────────────────────────
  useEffect(() => {
    if (phase === "flashing") {
      const t = setTimeout(() => setPhase("showing"), 500);
      return () => clearTimeout(t);
    }
    if (phase === "showing") {
      const t = setTimeout(onComplete, 500);
      return () => clearTimeout(t);
    }
    if (phase === "invalid") {
      const t = setTimeout(onComplete, 1500);
      return () => clearTimeout(t);
    }
  }, [phase, onComplete]);

  // ── Render ───────────────────────────────────────────────────────────────

  if (phase === "invalid") {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 font-vt323">
        <div className="text-center flex flex-col items-center gap-4">
          <div className="text-[140px] text-tele-danger leading-none animate-pulse">
            ✗
          </div>
          <div className="text-tele-danger text-[32px] tracking-widest uppercase">
            Invalid Answer
          </div>
        </div>
      </div>
    );
  }

  const isRedPhase = phase === "flashing" || (phase === "showing" && result === "BUST");
  const showBustText = phase === "showing" && result === "BUST";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 font-vt323">
      <div className="text-center flex flex-col items-center gap-2">
        <div
          className={`text-[160px] leading-none font-normal transition-colors duration-150 ${
            isRedPhase ? "text-tele-danger" : "text-tele-green"
          } ${phase === "flashing" ? "animate-pulse" : ""}`}
        >
          {showBustText ? "BUST" : display}
        </div>
        {phase === "showing" && result !== "BUST" && (
          <div className="text-tele-cyan text-[24px] tracking-widest uppercase">
            Points Scored
          </div>
        )}
      </div>
    </div>
  );
}
