"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi } from "@/lib/api/admin";
import type { Category, Question } from "@/lib/types/admin";
import DataTable from "@/components/admin/DataTable";
import Select from "@/components/ui/Select";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import { useToast } from "@/context/ToastContext";

type QuestionRow = Question & { difficultyLabel: string };

const columns = [
  { key: "questionText", label: "Question" },
  { key: "categoryName", label: "Category" },
  { key: "difficultyLabel", label: "Difficulty" },
  { key: "answerCount", label: "Answers" },
  { key: "isActive", label: "Active" },
];

const activeOptions = [
  { value: "", label: "All Status" },
  { value: "true", label: "Active" },
  { value: "false", label: "Inactive" },
];

function difficultyLabel(d: number) {
  return d === 1 ? "Easy" : d === 3 ? "Hard" : "Medium";
}

export default function QuestionsPage() {
  const { addToast } = useToast();
  const router = useRouter();

  const [questions, setQuestions] = useState<QuestionRow[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  // Filters (held in state; applied only on "Apply Filters")
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("");

  // Applied filters (the ones actually sent to the API)
  const [appliedCategory, setAppliedCategory] = useState("");
  const [appliedStatus, setAppliedStatus] = useState("");

  // Delete dialog
  const [showDelete, setShowDelete] = useState(false);
  const [selectedQuestion, setSelectedQuestion] = useState<Question | null>(null);

  const loadQuestions = useCallback(
    async (page: number, catId: string, status: string) => {
      setLoading(true);
      try {
        let isActive: boolean | undefined;
        if (status === "true") isActive = true;
        if (status === "false") isActive = false;

        const res = await adminApi.listQuestions(
          catId || undefined,
          isActive,
          page,
          pageSize
        );
        setQuestions(
          res.content.map((q) => ({ ...q, difficultyLabel: difficultyLabel(q.difficulty) }))
        );
        setTotalElements(res.totalElements);
      } catch (err) {
        addToast((err as Error).message, "error");
      } finally {
        setLoading(false);
      }
    },
    [addToast]
  );

  // Load categories once
  useEffect(() => {
    adminApi.listCategories().then(setCategories).catch((err) => {
      addToast((err as Error).message, "error");
    });
  }, [addToast]);

  // Initial load
  useEffect(() => {
    loadQuestions(0, "", "");
  }, [loadQuestions]);

  function applyFilters() {
    setAppliedCategory(selectedCategory);
    setAppliedStatus(selectedStatus);
    setCurrentPage(0);
    loadQuestions(0, selectedCategory, selectedStatus);
  }

  function handlePageChange(page: number) {
    setCurrentPage(page);
    loadQuestions(page, appliedCategory, appliedStatus);
  }

  async function handleDelete() {
    if (!selectedQuestion) return;
    setActionLoading(true);
    try {
      await adminApi.deleteQuestion(selectedQuestion.id);
      addToast("Question deleted successfully", "success");
      setShowDelete(false);
      loadQuestions(currentPage, appliedCategory, appliedStatus);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  const categoryOptions = [
    { value: "", label: "All Categories" },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <h1 className="m-0 text-[var(--color-primary)]">Questions</h1>
        <Link
          href="/admin/questions/create"
          className="bg-[var(--color-primary)] text-black px-6 py-3 rounded-lg font-semibold no-underline hover:bg-[#22c55e] transition-colors"
        >
          + New Question
        </Link>
      </div>

      {/* Filters */}
      <div className="flex gap-4 mb-8 bg-[#2a2a2a] p-4 rounded-lg items-end">
        <div className="flex-1 max-w-[200px]">
          <Select
            label="Category"
            value={selectedCategory}
            onChange={setSelectedCategory}
            options={categoryOptions}
          />
        </div>
        <div className="flex-1 max-w-[200px]">
          <Select
            label="Status"
            value={selectedStatus}
            onChange={setSelectedStatus}
            options={activeOptions}
          />
        </div>
        <div className="mb-4">
          <button
            onClick={applyFilters}
            className="bg-[#3b82f6] text-white px-6 py-3 rounded-lg border-none cursor-pointer font-medium hover:opacity-90 transition-opacity"
          >
            Apply Filters
          </button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={questions}
        loading={loading}
        totalElements={totalElements}
        pageSize={pageSize}
        currentPage={currentPage}
        onPageChange={handlePageChange}
        renderActions={(item) => (
          <>
            <button
              onClick={() => router.push(`/admin/questions/${item.id}`)}
              title="Edit"
              className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-primary-container)] transition-colors"
            >
              ✏️
            </button>
            <button
              onClick={() => { setSelectedQuestion(item); setShowDelete(true); }}
              title="Delete"
              className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-error-container)] transition-colors"
            >
              🗑️
            </button>
          </>
        )}
      />

      <ConfirmDialog
        open={showDelete}
        title="Delete Question"
        message={`Are you sure you want to delete this question? All ${selectedQuestion?.answerCount ?? 0} answers will also be deleted.`}
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
      />
    </div>
  );
}
