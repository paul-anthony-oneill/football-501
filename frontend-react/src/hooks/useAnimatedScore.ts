"use client";

import { useState, useEffect, useRef } from "react";

/**
 * Tracks a score value, updating the display immediately when the target changes.
 *
 * Returns a {@link flashVersion} that increments on each change so the caller
 * can use it as a React {@code key} to trigger a CSS highlight animation.
 */
export function useAnimatedScore(target: number): {
  display: number;
  isAnimating: boolean;
  flashVersion: number;
} {
  const [display, setDisplay] = useState(target);
  const [flashVersion, setFlashVersion] = useState(0);
  const prevTarget = useRef(target);

  useEffect(() => {
    if (target === prevTarget.current) return;
    prevTarget.current = target;
    setDisplay(target);
    setFlashVersion((v) => v + 1);
  }, [target]);

  return { display, isAnimating: false, flashVersion };
}
