"use client";

import { useState, useEffect, useRef } from "react";

/**
 * Eases a value change with a cubic ease-in-out curve and subtle random wobble.
 *
 * When `target` changes, the hook animates from the currently-displayed value to
 * the new target over 2–4 seconds. Returns both the animated display value and
 * whether an animation is currently in progress.
 */
export function useAnimatedScore(target: number): {
  display: number;
  isAnimating: boolean;
} {
  const [display, setDisplay] = useState(target);
  const displayRef = useRef(target);
  const prevTarget = useRef(target);
  const frameRef = useRef<number | null>(null);

  displayRef.current = display;

  const isAnimating = display !== target;

  useEffect(() => {
    if (target === prevTarget.current) return;

    const startValue = displayRef.current;
    const endValue = target;
    prevTarget.current = target;

    if (frameRef.current !== null) {
      cancelAnimationFrame(frameRef.current);
    }

    const duration = 2000 + Math.random() * 2000;
    const wobbleAmp = 0.08 + Math.random() * 0.12;
    const wobbleHz = 2 + Math.random() * 3;
    const range = endValue - startValue;
    const startTime = performance.now();

    const tick = (now: number) => {
      const elapsed = now - startTime;
      const raw = Math.min(elapsed / duration, 1);

      const eased =
        raw < 0.5
          ? 4 * raw * raw * raw
          : 1 - Math.pow(-2 * raw + 2, 3) / 2;

      const wobble =
        Math.sin(raw * Math.PI * wobbleHz) * wobbleAmp * (1 - raw);
      const progress = Math.max(0, Math.min(1, eased + wobble));
      const current = Math.round(startValue + range * progress);

      setDisplay(current);

      if (raw < 1) {
        frameRef.current = requestAnimationFrame(tick);
      } else {
        setDisplay(endValue);
        frameRef.current = null;
      }
    };

    frameRef.current = requestAnimationFrame(tick);

    return () => {
      if (frameRef.current !== null) {
        cancelAnimationFrame(frameRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);

  return { display, isAnimating };
}
