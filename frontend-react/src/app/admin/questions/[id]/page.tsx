"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { adminApi } from "@/lib/api/admin";
import type {
  Answer,
  Category,
  CreateAnswerRequest,
  CreateQuestionRequest,
  Question,
  QuestionStatus,
  UpdateQuestionRequest,
} from "@/lib/types/admin";
import DataTable from "@/components/admin/DataTable";
import AnswerForm from "@/components/admin/forms/AnswerForm";
import BulkImportModal from "@/components/admin/BulkImportModal";
import QuestionForm from "@/components/admin/forms/QuestionForm";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import FormModal from "@/components/ui/FormModal";
import { useToast } from "@/context/ToastContext";

const answerColumns = [
  { key: "displayText",  label: "Answer" },
  { key: "score",        label: "Score" },
  { key: "isValidDarts", label: "Valid Darts" },
  { key: "isBust",       label: "Bust" },
];

// ── Status helpers ──────────────────────────────────────────────────────────

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

export default function QuestionDetailPage() {
  const { id: questionId } = useParams<{ id: string }>();
  const { addToast } = useToast();
  const router = useRouter();

  const [question, setQuestion] = useState<Question | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [answers, setAnswers] = useState<Answer[]>([]);
  const [loading, setLoading] = useState(true);

  // Question editing
  const [isEditing, setIsEditing] = useState(false);
  const [questionLoading, setQuestionLoading] = useState(false);
  const [rematerializing, setRematerializing] = useState(false);

  // Answer preview toggle
  const [previewMode, setPreviewMode] = useState(false);

  // Answer modals
  const [showCreateAnswer, setShowCreateAnswer] = useState(false);
  const [showEditAnswer, setShowEditAnswer] = useState(false);
  const [showBulkImport, setShowBulkImport] = useState(false);
  const [showDeleteAnswer, setShowDeleteAnswer] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState<Answer | null>(null);
  const [answerLoading, setAnswerLoading] = useState(false);

  const loadQuestion = useCallback(async () => {
    try {
      const q = await adminApi.getQuestion(questionId);
      setQuestion(q);
    } catch (err) {
      addToast((err as Error).message, "error");
    }
  }, [questionId, addToast]);

  const loadAnswers = useCallback(async () => {
    try {
      const data = await adminApi.listAnswers(questionId);
      setAnswers(data);
    } catch (err) {
      addToast((err as Error).message, "error");
    }
  }, [questionId, addToast]);

  useEffect(() => {
    async function init() {
      setLoading(true);
      await Promise.all([
        loadQuestion(),
        loadAnswers(),
        adminApi.listCategories().then(setCategories).catch(() => {}),
      ]);
      setLoading(false);
    }
    init();
  }, [loadQuestion, loadAnswers]);

  // ── Question actions ─────────────────────────────────────────────────────

  async function handleUpdateQuestion(
    data: CreateQuestionRequest | UpdateQuestionRequest
  ) {
    setQuestionLoading(true);
    try {
      const updated = await adminApi.updateQuestion(
        questionId,
        data as UpdateQuestionRequest
      );
      setQuestion(updated);
      addToast("Question updated successfully", "success");
      setIsEditing(false);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setQuestionLoading(false);
    }
  }

  /**
   * Advance the question through its lifecycle:
   *   draft → active → retired → active → …
   */
  async function handleStatusTransition() {
    if (!question) return;
    const { nextStatus } = statusConfig[question.status];
    try {
      const updated = await adminApi.updateQuestionStatus(questionId, {
        status: nextStatus,
      });
      setQuestion(updated);
      addToast(`Question is now ${statusConfig[nextStatus].label}`, "success");
    } catch (err) {
      addToast((err as Error).message, "error");
    }
  }

  /**
   * Re-materialize answers for an active question after scraper data refresh.
   */
  async function handleRematerialize() {
    if (!question) return;
    setRematerializing(true);
    try {
      const result = await adminApi.rematerializeQuestion(questionId);
      addToast(`Re-materialized: ${result.answersUpserted} answers updated.`, "success");
      await Promise.all([loadAnswers(), loadQuestion()]);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setRematerializing(false);
    }
  }

  // ── Answer actions ───────────────────────────────────────────────────────

  async function handleCreateAnswer(data: CreateAnswerRequest) {
    setAnswerLoading(true);
    try {
      await adminApi.createAnswer(questionId, data);
      addToast("Answer created successfully", "success");
      setShowCreateAnswer(false);
      await Promise.all([loadAnswers(), loadQuestion()]);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setAnswerLoading(false);
    }
  }

  async function handleUpdateAnswer(data: CreateAnswerRequest) {
    if (!selectedAnswer) return;
    setAnswerLoading(true);
    try {
      await adminApi.updateAnswer(selectedAnswer.id, data);
      addToast("Answer updated successfully", "success");
      setShowEditAnswer(false);
      await Promise.all([loadAnswers(), loadQuestion()]);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setAnswerLoading(false);
    }
  }

  async function handleBulkImport(data: { answers: CreateAnswerRequest[] }) {
    setAnswerLoading(true);
    try {
      const result = await adminApi.bulkCreateAnswers(questionId, data);
      if (result.errors?.length) {
        addToast(
          `Imported ${result.created}, Skipped ${result.skipped}, Errors: ${result.errors.length}`,
          "info"
        );
      } else {
        addToast(
          `Imported ${result.created} answers (Skipped ${result.skipped})`,
          "success"
        );
      }
      setShowBulkImport(false);
      await Promise.all([loadAnswers(), loadQuestion()]);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setAnswerLoading(false);
    }
  }

  async function handleDeleteAnswer() {
    if (!selectedAnswer) return;
    setAnswerLoading(true);
    try {
      await adminApi.deleteAnswer(selectedAnswer.id);
      addToast("Answer deleted successfully", "success");
      setShowDeleteAnswer(false);
      await Promise.all([loadAnswers(), loadQuestion()]);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setAnswerLoading(false);
    }
  }

  // ── Render ───────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="flex justify-center items-center h-40 text-[var(--color-on-surface-variant)]">
        <div className="w-8 h-8 border-[3px] border-[var(--color-surface-variant)] border-t-[var(--color-primary)] rounded-full animate-spin-slow mr-4" />
        Loading…
      </div>
    );
  }

  if (!question) {
    return (
      <div className="text-[var(--color-error)] text-center py-20">
        Question not found.
      </div>
    );
  }

  const sc = statusConfig[question.status];
  const coverage = Math.round(
    (question.validDartsCount / Math.max(1, question.answerCount)) * 100
  );

  return (
    <div className="max-w-4xl mx-auto">
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
          {/* Rematerialize — only available when active */}
          {question.status === "active" && (
            <button
              onClick={handleRematerialize}
              disabled={rematerializing}
              title="Re-compute answers from latest scraper data"
              className="px-4 py-2 rounded-lg border border-[#4f46e5] bg-[rgba(79,70,229,0.15)] text-[#818cf8] text-[0.9rem] cursor-pointer hover:opacity-80 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {rematerializing ? "Refreshing…" : "↻ Rematerialize"}
            </button>
          )}
          {/* Status transition button */}
          <button
            onClick={handleStatusTransition}
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
            onSubmit={handleUpdateQuestion}
            onCancel={() => setIsEditing(false)}
          />
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-8">
        {[
          { value: question.answerCount,    label: "Total Answers" },
          { value: question.validDartsCount, label: "Valid Darts" },
          { value: `${coverage}%`,          label: "Coverage" },
        ].map((stat) => (
          <div key={stat.label} className="bg-[#2a2a2a] p-6 rounded-lg text-center">
            <div className="text-3xl font-bold text-white mb-1">{stat.value}</div>
            <div className="text-[0.9rem] text-[#9ca3af]">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Answers section */}
      <div className="mt-12">
        <div className="flex justify-between items-center mb-6">
          <h2 className="m-0 text-[var(--color-primary)]">Answers</h2>
          <div className="flex gap-3">
            <button
              onClick={() => setPreviewMode(!previewMode)}
              className="px-4 py-2 rounded-lg border border-[#6366f1] bg-[rgba(99,102,241,0.1)] text-[#818cf8] text-[0.9rem] cursor-pointer hover:opacity-80 transition-opacity"
            >
              {previewMode ? "✏️ Edit Mode" : "👁 Preview Answers"}
            </button>
            {!previewMode && (
              <>
                <button
                  onClick={() => setShowBulkImport(true)}
                  className="px-4 py-2 rounded-lg border border-[#444] bg-[#2a2a2a] text-white text-[0.9rem] cursor-pointer hover:opacity-80 transition-opacity"
                >
                  Bulk Import
                </button>
                <button
                  onClick={() => { setSelectedAnswer(null); setShowCreateAnswer(true); }}
                  className="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-black text-[0.9rem] font-medium cursor-pointer hover:opacity-90 transition-opacity border-0"
                >
                  + Add Answer
                </button>
              </>
            )}
          </div>
        </div>

        {previewMode ? (
          <AnswerPreview answers={answers} />
        ) : (
          <DataTable
            columns={answerColumns}
            data={answers}
            loading={false}
            totalElements={answers.length}
            pageSize={50}
            currentPage={0}
            renderActions={(item) => (
              <>
                <button
                  onClick={() => { setSelectedAnswer(item); setShowEditAnswer(true); }}
                  title="Edit"
                  className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-primary-container)] transition-colors"
                >
                  ✏️
                </button>
                <button
                  onClick={() => { setSelectedAnswer(item); setShowDeleteAnswer(true); }}
                  title="Delete"
                  className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-error-container)] transition-colors"
                >
                  🗑️
                </button>
              </>
            )}
          />
        )}
      </div>

      {/* Create Answer modal */}
      <FormModal
        open={showCreateAnswer}
        title="Create Answer"
        onSave={() => {}}
        onCancel={() => setShowCreateAnswer(false)}
        loading={answerLoading}
      >
        <AnswerForm
          loading={answerLoading}
          onSubmit={handleCreateAnswer}
          onCancel={() => setShowCreateAnswer(false)}
        />
      </FormModal>

      {/* Edit Answer modal */}
      <FormModal
        open={showEditAnswer && !!selectedAnswer}
        title="Edit Answer"
        onSave={() => {}}
        onCancel={() => setShowEditAnswer(false)}
        loading={answerLoading}
      >
        {selectedAnswer && (
          <AnswerForm
            answer={selectedAnswer}
            loading={answerLoading}
            onSubmit={handleUpdateAnswer}
            onCancel={() => setShowEditAnswer(false)}
          />
        )}
      </FormModal>

      {/* Bulk import modal */}
      <FormModal
        open={showBulkImport}
        title="Bulk Import Answers"
        onSave={() => {}}
        onCancel={() => setShowBulkImport(false)}
        loading={answerLoading}
      >
        <BulkImportModal
          loading={answerLoading}
          onSubmit={handleBulkImport}
          onCancel={() => setShowBulkImport(false)}
        />
      </FormModal>

      {/* Delete answer confirm */}
      <ConfirmDialog
        open={showDeleteAnswer}
        title="Delete Answer"
        message={`Are you sure you want to delete '${selectedAnswer?.displayText}'?`}
        onConfirm={handleDeleteAnswer}
        onCancel={() => setShowDeleteAnswer(false)}
      />
    </div>
  );
}

// ── Answer Preview ───────────────────────────────────────────────────────────

function AnswerPreview({ answers }: { answers: Answer[] }) {
  // Answers arrive pre-sorted by score DESC from the backend
  const validDarts = answers.filter(a => a.isValidDarts && !a.isBust);
  const scores = answers.map(a => a.score);
  const scoreMin = scores.length ? Math.min(...scores) : 0;
  const scoreMax = scores.length ? Math.max(...scores) : 0;

  return (
    <div>
      {/* Summary strip */}
      <div className="flex gap-6 mb-4 flex-wrap text-sm text-[#9ca3af]">
        <span>{answers.length} total</span>
        <span className="text-[#4ade80]">{validDarts.length} valid darts</span>
        {answers.length > 0 && (
          <span>scores: {scoreMin}–{scoreMax}</span>
        )}
        {answers.length > 0 && (
          <span className="text-[#9ca3af]">
            {answers.length - validDarts.length} bust / invalid
          </span>
        )}
      </div>

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
              <span className="text-[#4ade80] font-mono font-bold text-right pr-4">{a.score}</span>
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
