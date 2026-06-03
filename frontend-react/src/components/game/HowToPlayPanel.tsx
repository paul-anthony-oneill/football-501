"use client";

import { useState } from "react";

interface HowToPlayPanelProps {
  variant?: "home" | "teletext";
}

export default function HowToPlayPanel({
  variant = "home",
}: HowToPlayPanelProps) {
  const [open, setOpen] = useState(false);

  const isTeletext = variant === "teletext";

  return (
    <div className={isTeletext ? "font-vt323 text-white" : ""}>
      <button
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className={
          isTeletext ?
            "border-2 border-tele-cyan text-tele-cyan px-4 py-2 text-[18px] tracking-widest hover:bg-white hover:text-black transition-colors w-full text-left flex justify-between items-center"
          : "font-plex text-[11px] tracking-[0.25em] text-h-dim uppercase border border-h-rule px-4 py-2.5 rounded-sm hover:border-h-accent hover:text-h-accent transition-colors w-full text-left flex justify-between items-center"
        }
      >
        <span>HOW TO PLAY</span>
        <span className={open ? "rotate-180" : ""}>&#9660;</span>
      </button>

      {open && (
        <div
          className={
            isTeletext ?
              "mt-3 border border-[#333] p-4 space-y-4"
            : "mt-3 border border-h-rule p-4 rounded-sm space-y-4"
          }
        >
          <section>
            <h3
              className={
                isTeletext ?
                  "text-tele-cyan tracking-widest text-[20px] mb-2"
                : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Starting Score
            </h3>
            <p
              className={
                isTeletext ?
                  "text-white/80 text-[16px]"
                : "text-h-dim text-[14px]"
              }
            >
              You start with a points target, usually <strong>501</strong>. Each
              correct answer has a score, and that score is subtracted from your
              total. First to reach exactly 0 wins — a <em>checkout</em>.
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext ?
                  "text-tele-cyan tracking-widest text-[20px] mb-2"
                : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Scoring
            </h3>
            <p
              className={
                isTeletext ?
                  "text-white/80 text-[16px]"
                : "text-h-dim text-[14px]"
              }
            >
              Submitting an answer is our equivalent to &ldquo;throwing a
              dart.&rdquo; The score you get for that answer depends on the
              question. For example, if the question is "Goals for Liverpool in
              Premier League since 2000", an answer of "Luis Suarez" would be
              worth 69 points, as he scored 69 goals for Liverpool in the
              Premier League. Valid values range 1–180, except 163, 166, 169,
              172, 173, 175, 176, 178, 179 (impossible with three darts).
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext ?
                  "text-tele-cyan tracking-widest text-[20px] mb-2"
                : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Bust Rules
            </h3>
            <p
              className={
                isTeletext ?
                  "text-white/80 text-[16px]"
                : "text-h-dim text-[14px]"
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
                isTeletext ?
                  "text-tele-cyan tracking-widest text-[20px] mb-2"
                : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Checkout Range
            </h3>
            <p
              className={
                isTeletext ?
                  "text-white/80 text-[16px]"
                : "text-h-dim text-[14px]"
              }
            >
              When your score is under 180, you could possibly <em>checkout</em>{" "}
              (win) with your next throw. A checkout is when your score reaches
              0. In real darts, you have to score enough to finish on exactly 0.
              But we give you a little more leeway, and any answer leaving you
              between <strong>-10 and 0</strong> will count as a checkout. Extra
              points for the closer you get to 0, though!
            </p>
          </section>

          <section>
            <h3
              className={
                isTeletext ?
                  "text-tele-cyan tracking-widest text-[20px] mb-2"
                : "font-bricolage font-bold text-lg mb-1"
              }
            >
              Hints Explained
            </h3>
            <ul
              className={
                isTeletext ?
                  "text-white/80 text-[16px] list-disc pl-4 space-y-1"
                : "text-h-dim text-[14px] list-disc pl-4 space-y-1"
              }
            >
              <li>
                <strong>180s Left</strong> — Answers still available for this
                question worth exactly 180 points. Visible when score &gt; 180.
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
