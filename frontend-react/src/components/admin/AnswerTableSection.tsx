"use client";

import type { Answer, CreateAnswerRequest } from "@/lib/types/admin";
import DataTable from "@/components/admin/DataTable";
import AnswerForm from "@/components/admin/forms/AnswerForm";
import BulkImportModal from "@/components/admin/BulkImportModal";
import AnswerPreview from "@/components/admin/AnswerPreview";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import FormModal from "@/components/ui/FormModal";

// ─── Column config ────────────────────────────────────────────────────────────

const answerColumns = [
  { key: "displayText",  label: "Answer"     },
  { key: "score",        label: "Score"      },
  { key: "isValidDarts", label: "Valid Darts" },
  { key: "isBust",       label: "Bust"       },
];

// ─── Props ────────────────────────────────────────────────────────────────────

interface AnswerTableSectionProps {
  answers:       Answer[];
  previewMode:   boolean;
  setPreviewMode: (v: boolean) => void;

  showCreateAnswer:    boolean;
  setShowCreateAnswer: (v: boolean) => void;
  showEditAnswer:      boolean;
  setShowEditAnswer:   (v: boolean) => void;
  showBulkImport:      boolean;
  setShowBulkImport:   (v: boolean) => void;
  showDeleteAnswer:    boolean;
  setShowDeleteAnswer: (v: boolean) => void;
  selectedAnswer:      Answer | null;
  setSelectedAnswer:   (v: Answer | null) => void;
  answerLoading:       boolean;

  onCreateAnswer: (data: CreateAnswerRequest) => Promise<void>;
  onUpdateAnswer: (data: CreateAnswerRequest) => Promise<void>;
  onBulkImport:   (data: { answers: CreateAnswerRequest[] }) => Promise<void>;
  onDeleteAnswer: () => Promise<void>;
}

// ─── Component ────────────────────────────────────────────────────────────────

/**
 * Renders the "Answers" section of the question detail page, including:
 * - Preview / edit mode toggle
 * - Answer table (DataTable) or AnswerPreview read-only view
 * - Create / Edit / Bulk-import / Delete modals
 */
export default function AnswerTableSection({
  answers,
  previewMode,
  setPreviewMode,
  showCreateAnswer,
  setShowCreateAnswer,
  showEditAnswer,
  setShowEditAnswer,
  showBulkImport,
  setShowBulkImport,
  showDeleteAnswer,
  setShowDeleteAnswer,
  selectedAnswer,
  setSelectedAnswer,
  answerLoading,
  onCreateAnswer,
  onUpdateAnswer,
  onBulkImport,
  onDeleteAnswer,
}: AnswerTableSectionProps) {
  return (
    <div className="mt-12">
      {/* Section header */}
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

      {/* Table or preview */}
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
          onSubmit={onCreateAnswer}
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
            onSubmit={onUpdateAnswer}
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
          onSubmit={onBulkImport}
          onCancel={() => setShowBulkImport(false)}
        />
      </FormModal>

      {/* Delete answer confirmation */}
      <ConfirmDialog
        open={showDeleteAnswer}
        title="Delete Answer"
        message={`Are you sure you want to delete '${selectedAnswer?.displayText}'?`}
        onConfirm={onDeleteAnswer}
        onCancel={() => setShowDeleteAnswer(false)}
      />
    </div>
  );
}
