"use client";

import { useState } from "react";

interface HowToPlayPanelProps {
  variant?: "home" | "teletext";
}

export default function HowToPlayPanel({ variant = "home" }: HowToPlayPanelProps) {
  const [open, setOpen] = useState(false);

  const isTeletext = variant === "teletext";

  return (
    <div className={isTeletext ? "font-vt323 text-white" : ""}>
      <button
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className={
          isTeletext
            ? "border-2 border-tele-cyan text-tele-cyan px-4 py-2 text-[18px] tracking-widest hover:bg-white hover:text-black transition-colors w-full text-left flex justify-between items-center"
            : "font-plex text-[11px] tracking-[0.25em] text-h-dim uppercase border border-h-rule px-4 py-2.5 rounded-sm hover:border-h-accent hover:text-h-accent transition-colors w-full text-left flex justify-between items-center"
        }
      >
        <span>HOW TO PLAY</span>
        <span className={open ? "rotate-180" : ""}>&#9660;</span>
      </button>

      {open && (
        <div
          className={
            isTeletext
              ? "mt-3 border border-[#333] p-4 space-y-4"
              : "mt-3 border border-h-rule p-4 rounded-sm space-y-4"
          }
        >
          <section>
            <h3
              className={
                isTeletext
                  ? "text-tele-cyan tracking-widest text-[20px] mb-2"
                  : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Starting Score
            </h3>
            <p
              className={
                isTeletext ? "text-white/80 text-[16px]" : "text-h-dim text-[14px]"
              }
            >
              You start with <strong>501</strong> points. Each correct answer
              reduces your score. First to reach exactly 0 wins — a{" "}
              <em>checkout</em>.
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext
                  ? "text-tele-cyan tracking-widest text-[20px] mb-2"
                  : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Scoring
            </h3>
            <p
              className={
                isTeletext ? "text-white/80 text-[16px]" : "text-h-dim text-[14px]"
              }
            >
              Each turn a question is displayed. Type a player name matching the
              question to &ldquo;throw a dart.&rdquo; The player&rsquo;s stat value becomes
              your dart score (1–180). Valid values range 1–180, except 163,
              166, 169, 172, 173, 175, 176, 178, 179 (impossible with three darts).
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext
                  ? "text-tele-cyan tracking-widest text-[20px] mb-2"
                  : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Bust Rules
            </h3>
            <p
              className={
                isTeletext ? "text-white/80 text-[16px]" : "text-h-dim text-[14px]"
              }
            >
              You <em>bust</em> if your remaining score after a throw drops
              below -10, or if your dart score exceeds 180. A bust resets your
              score to its value before the throw.
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext
                  ? "text-tele-cyan tracking-widest text-[20px] mb-2"
                  : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Checkout Range
            </h3>
            <p
              className={
                isTeletext ? "text-white/80 text-[16px]" : "text-h-dim text-[14px]"
              }
            >
              When your score is between 0 and 180, you can <em>checkout</em>{" "}
              (win) by scoring exactly enough to reach 0. A checkout is any
              score that leaves you between <strong>-10 and 0</strong>.
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext
                  ? "text-tele-cyan tracking-widest text-[20px] mb-2"
                  : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Hints Explained
            </h3>
            <ul
              className={
                isTeletext
                  ? "text-white/80 text-[16px] list-disc pl-4 space-y-1"
                  : "text-h-dim text-[14px] list-disc pl-4 space-y-1"
              }
            >
              <li>
                <strong>180s Left</strong> — Answers still available worth exactly
                180 points. Visible when score &gt; 180.
              </li>
              <li>
                <strong>Checkouts</strong> — Answers that would end the game in
                one throw. Visible when score &le; 180.
              </li>
            </ul>
          </section>
        </div>
      )}
    </div>
  );
}
