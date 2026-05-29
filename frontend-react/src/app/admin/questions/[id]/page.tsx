"use client";

import { useParams } from "next/navigation";
import { useQuestionDetail } from "@/hooks/useQuestionDetail";
import QuestionMetaPanel from "@/components/admin/QuestionMetaPanel";
import AnswerTableSection from "@/components/admin/AnswerTableSection";

// ─── Component ────────────────────────────────────────────────────────────────

/**
 * Question detail admin page.
 *
 * This component is a thin coordinator: it reads route params, delegates all
 * data-fetching and mutation logic to `useQuestionDetail`, and composes the
 * two main sub-panels.
 */
export default function QuestionDetailPage() {
  const { id: questionId } = useParams<{ id: string }>();

  const {
    // Data
    question,
    categories,
    answers,
    loading,

    // Question editing
    isEditing,
    setIsEditing,
    questionLoading,
    rematerializing,

    // Preview toggle
    previewMode,
    setPreviewMode,

    // Answer modal state
    showCreateAnswer, setShowCreateAnswer,
    showEditAnswer,   setShowEditAnswer,
    showBulkImport,   setShowBulkImport,
    showDeleteAnswer, setShowDeleteAnswer,
    selectedAnswer,   setSelectedAnswer,
    answerLoading,

    // Actions
    handleUpdateQuestion,
    handleStatusTransition,
    handleRematerialize,
    handleCreateAnswer,
    handleUpdateAnswer,
    handleBulkImport,
    handleDeleteAnswer,
  } = useQuestionDetail(questionId);

  // ── Loading / error states ─────────────────────────────────────────────────

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

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-4xl mx-auto">
      <QuestionMetaPanel
        question={question}
        categories={categories}
        answers={answers}
        isEditing={isEditing}
        setIsEditing={setIsEditing}
        questionLoading={questionLoading}
        rematerializing={rematerializing}
        onUpdateQuestion={handleUpdateQuestion}
        onStatusTransition={handleStatusTransition}
        onRematerialize={handleRematerialize}
      />

      <AnswerTableSection
        answers={answers}
        previewMode={previewMode}
        setPreviewMode={setPreviewMode}
        showCreateAnswer={showCreateAnswer}
        setShowCreateAnswer={setShowCreateAnswer}
        showEditAnswer={showEditAnswer}
        setShowEditAnswer={setShowEditAnswer}
        showBulkImport={showBulkImport}
        setShowBulkImport={setShowBulkImport}
        showDeleteAnswer={showDeleteAnswer}
        setShowDeleteAnswer={setShowDeleteAnswer}
        selectedAnswer={selectedAnswer}
        setSelectedAnswer={setSelectedAnswer}
        answerLoading={answerLoading}
        onCreateAnswer={handleCreateAnswer}
        onUpdateAnswer={handleUpdateAnswer}
        onBulkImport={handleBulkImport}
        onDeleteAnswer={handleDeleteAnswer}
      />
    </div>
  );
}
