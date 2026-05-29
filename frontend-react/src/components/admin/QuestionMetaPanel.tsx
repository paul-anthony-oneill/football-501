"use client";

import type {
  Answer,
  Category,
  CreateQuestionRequest,
  Question,
  QuestionStatus,
  UpdateQuestionRequest,
} from "@/lib/types/admin";
import QuestionForm from "@/components/admin/forms/QuestionForm";

// ─── Status config ────────────────────────────────────────────────────────────

const statusConfig: Record<
  QuestionStatus,
  { label: string; badgeClass: string; nextStatus: QuestionStatus; actionLabel: string }
> = {
  draft: {
    label:       "Draft",
    badgeClass:  "bg-[rgba(156,163,175,0.15)] text-[#9ca3af] border-[#9ca3af]",
    nextStatus:  "active",
    actionLabel: "Activate",
  },
  active: {
    label:       "Active",
    badgeClass:  "bg-[rgba(74,222,128,0.1)] text-[#4ade80] border-[#4ade80]",
    nextStatus:  "retired",
    actionLabel: "Retire",
  },
  retired: {
    label:       "Retired",
    badgeClass:  "bg-[rgba(239,68,68,0.1)] text-[#ef4444] border-[#ef4444]",
    nextStatus:  "active",
    actionLabel: "Re-activate",
  },
};

// ─── Viability helpers ────────────────────────────────────────────────────────

type ViabilityStatus = "empty" | "impossible" | "tight" | "ok";

const viabilityBadge: Record<ViabilityStatus, { label: string; cls: string }> = {
  empty:      { label: "No answers",    cls: "bg-[rgba(156,163,175,0.15)] text-[#9ca3af] border-[#9ca3af]" },
  impossible: { label: "✗ Impossible",  cls: "bg-[rgba(239,68,68,0.1)]    text-[#ef4444] border-[#ef4444]" },
  tight:      { label: "⚠ Tight",       cls: "bg-[rgba(251,191,36,0.1)]   text-[#fbbf24] border-[#fbbf24]" },
  ok:         { label: "✓ Completable", cls: "bg-[rgba(74,222,128,0.1)]   text-[#4ade80] border-[#4ade80]" },
};

function deriveViabilityStatus(pool: number): ViabilityStatus {
  if (pool === 0)    return "empty";
  if (pool < 501)    return "impossible";
  if (pool < 1000)   return "tight";
  return "ok";
}

// ─── StatCard (used only inside this module) ──────────────────────────────────

function StatCard({
  label,
  value,
  sub,
  accent,
}: {
  label:  string;
  value:  string | number;
  sub?:   string;
  accent?: "green" | "amber";
}) {
  const valueClass =
    accent === "green" ? "text-[#4ade80]" :
    accent === "amber" ? "text-[#fbbf24]" :
    "text-white";
  return (
    <div className="bg-[#1a1a1a] p-4 rounded-lg text-center">
      <div className={`text-2xl font-bold font-mono mb-1 ${valueClass}`}>{value}</div>
      <div className="text-[0.8rem] text-[#9ca3af]">{label}</div>
      {sub && <div className="text-[0.72rem] text-[#555] mt-0.5">{sub}</div>}
    </div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface QuestionMetaPanelProps {
  question:        Question;
  categories:      Category[];
  answers:         Answer[];
  isEditing:       boolean;
  setIsEditing:    (v: boolean) => void;
  questionLoading: boolean;
  rematerializing: boolean;
  onUpdateQuestion:    (data: CreateQuestionRequest | UpdateQuestionRequest) => Promise<void>;
  onStatusTransition:  () => Promise<void>;
  onRematerialize:     () => Promise<void>;
}

/**
 * Renders the question header (title, status badge, action buttons),
 * the optional inline edit form, and the viability metrics panel.
 */
export default function QuestionMetaPanel({
  question,
  categories,
  answers,
  isEditing,
  setIsEditing,
  questionLoading,
  rematerializing,
  onUpdateQuestion,
  onStatusTransition,
  onRematerialize,
}: QuestionMetaPanelProps) {
  const sc = statusConfig[question.status];

  // Derived viability metrics
  const coverage       = Math.round((question.validDartsCount / Math.max(1, question.answerCount)) * 100);
  const viabilityStatus = deriveViabilityStatus(question.totalPointsPool);

  const validAnswers    = answers.filter((a) => a.isValidDarts && !a.isBust);
  const maxValidScore   = validAnswers.reduce((max, a) => Math.max(max, a.score), 0);
  const minAnswersNeeded = maxValidScore > 0 ? Math.ceil(501 / maxValidScore) : null;

  return (
    <>
      {/* Question header */}
      <div className="flex justify-between items-start mb-8">
        <div>
          <h1 className="m-0 mb-2 text-[var(--color-primary)]">
            {question.questionText}
          </h1>
          <div className="flex gap-3 items-center flex-wrap">
            <span className="px-2 py-1 rounded text-[0.8rem] font-medium bg-[#333] text-[#d1d5db]">
              {question.categoryName}
            </span>
            <span
              className={`px-2 py-1 rounded text-[0.8rem] font-medium border ${sc.badgeClass}`}
            >
              {sc.label}
            </span>
            <span className="text-[#9ca3af] text-[0.9rem]">
              Key: {question.metricKey}
            </span>
          </div>
        </div>

        <div className="flex gap-3">
          {/* Rematerialise — only available when active */}
          {question.status === "active" && (
            <button
              onClick={onRematerialize}
              disabled={rematerializing}
              title="Re-compute answers from latest scraper data"
              className="px-4 py-2 rounded-lg border border-[#4f46e5] bg-[rgba(79,70,229,0.15)] text-[#818cf8] text-[0.9rem] cursor-pointer hover:opacity-80 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {rematerializing ? "Refreshing…" : "↻ Rematerialize"}
            </button>
          )}
          <button
            onClick={onStatusTransition}
            className="px-4 py-2 rounded-lg border border-[#444] bg-[#2a2a2a] text-white text-[0.9rem] cursor-pointer hover:opacity-80 transition-opacity"
          >
            {sc.actionLabel}
          </button>
          <button
            onClick={() => setIsEditing(!isEditing)}
            className="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-black text-[0.9rem] font-medium cursor-pointer hover:opacity-90 transition-opacity border-0"
          >
            {isEditing ? "Cancel Edit" : "Edit Question"}
          </button>
        </div>
      </div>

      {/* Inline edit form */}
      {isEditing && (
        <div className="bg-[#2a2a2a] p-6 rounded-lg mb-8 border border-[#444] animate-fade-in">
          <QuestionForm
            question={question}
            categories={categories}
            loading={questionLoading}
            onSubmit={onUpdateQuestion}
            onCancel={() => setIsEditing(false)}
          />
        </div>
      )}

      {/* Viability panel */}
      <div className="bg-[#2a2a2a] rounded-xl p-6 mb-8 border border-[#333]">
        <div className="flex items-center justify-between mb-5">
          <span className="text-xs font-semibold uppercase tracking-widest text-[#9ca3af]">
            Viability
          </span>
          <span
            className={`px-2 py-1 rounded text-[0.8rem] font-medium border ${viabilityBadge[viabilityStatus].cls}`}
          >
            {viabilityBadge[viabilityStatus].label}
          </span>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <StatCard label="Total Answers"       value={question.answerCount} />
          <StatCard label="Valid Darts"          value={question.validDartsCount} sub={`${coverage}% coverage`} />
          <StatCard label="High-Value (101–180)" value={question.highValueAnswerCount} accent="green" />
          <StatCard
            label="Min to Finish"
            value={minAnswersNeeded !== null ? `~${minAnswersNeeded}` : "—"}
            sub={maxValidScore > 0 ? `max score: ${maxValidScore}` : "no answers yet"}
          />
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-white/10 text-sm">
          <div>
            <span className="text-[#9ca3af]">Points Pool: </span>
            <span className="text-white font-mono font-bold">
              {question.totalPointsPool > 0
                ? question.totalPointsPool.toLocaleString() + " pts"
                : "—"}
            </span>
            {question.totalPointsPool > 0 && (
              <span className="ml-2 text-[#9ca3af]">
                {question.totalPointsPool >= 501
                  ? `(${(question.totalPointsPool / 501).toFixed(1)}× the 501 minimum)`
                  : `(${(501 - question.totalPointsPool).toLocaleString()} pts short — cannot be finished)`}
              </span>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
