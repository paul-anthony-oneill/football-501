"use client";

import type { Answer } from "@/lib/types/admin";

// ─── Score buckets ────────────────────────────────────────────────────────────

const SCORE_BUCKETS = [
  { range: "101–180", min: 101, max: 180, color: "#4ade80" },
  { range: "51–100",  min: 51,  max: 100, color: "#60a5fa" },
  { range: "1–50",    min: 1,   max: 50,  color: "#9ca3af" },
] as const;

// ─── Component ────────────────────────────────────────────────────────────────

interface AnswerPreviewProps {
  answers: Answer[];
}

/**
 * Read-only preview of a question's answer set.
 *
 * Shows a summary strip, score-distribution buckets, and a sorted list of all
 * answers with validity badges. Answers are expected pre-sorted by score DESC
 * from the backend.
 */
export default function AnswerPreview({ answers }: AnswerPreviewProps) {
  const validDarts = answers.filter((a) => a.isValidDarts && !a.isBust);
  const scores     = answers.map((a) => a.score);
  const scoreMin   = scores.length ? Math.min(...scores) : 0;
  const scoreMax   = scores.length ? Math.max(...scores) : 0;

  return (
    <div>
      {/* Summary strip */}
      <div className="flex gap-6 mb-4 flex-wrap text-sm text-[#9ca3af]">
        <span>{answers.length} total</span>
        <span className="text-[#4ade80]">{validDarts.length} valid darts</span>
        {answers.length > 0 && <span>scores: {scoreMin}–{scoreMax}</span>}
        {answers.length > 0 && (
          <span className="text-[#9ca3af]">
            {answers.length - validDarts.length} bust / invalid
          </span>
        )}
      </div>

      {/* Score distribution buckets */}
      {answers.length > 0 && (
        <div className="flex gap-3 mb-4 flex-wrap">
          {SCORE_BUCKETS.map(({ range, min, max, color }) => {
            const count = answers.filter(
              (a) => a.score >= min && a.score <= max && a.isValidDarts && !a.isBust
            ).length;
            return (
              <div
                key={range}
                className="flex items-center gap-2 bg-[#1a1a1a] rounded-lg px-3 py-1.5"
              >
                <span style={{ color }} className="font-mono font-bold text-sm">
                  {count}
                </span>
                <span className="text-[#9ca3af] text-xs">{range}</span>
              </div>
            );
          })}
        </div>
      )}

      {/* Sorted rows */}
      <div className="rounded-xl overflow-hidden border border-[#444]">
        {answers.length === 0 ? (
          <div className="text-center text-[#555] py-10 text-sm">
            No answers yet. Add some using the Edit Mode.
          </div>
        ) : (
          answers.map((a, i) => (
            <div
              key={a.id}
              className="grid grid-cols-[36px_1fr_64px_110px] items-center px-4 py-2.5 border-b border-[#2a2a2a] last:border-0 odd:bg-[rgba(255,255,255,0.02)]"
            >
              <span className="text-[#555] text-xs font-mono">{i + 1}</span>
              <span className="text-white font-medium text-sm">{a.displayText}</span>
              <span className="text-[#4ade80] font-mono font-bold text-right pr-4">
                {a.score}
              </span>
              <span className="text-right text-xs">
                {a.isBust ? (
                  <span className="px-2 py-0.5 rounded font-mono bg-[rgba(239,68,68,0.15)] text-[#ef4444] border border-[#ef4444]">
                    BUST
                  </span>
                ) : !a.isValidDarts ? (
                  <span className="px-2 py-0.5 rounded font-mono bg-[rgba(251,191,36,0.15)] text-[#fbbf24] border border-[#fbbf24]">
                    INV DARTS
                  </span>
                ) : (
                  <span className="px-2 py-0.5 rounded font-mono bg-[rgba(74,222,128,0.1)] text-[#4ade80] border border-[#4ade80]">
                    ✓ valid
                  </span>
                )}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
