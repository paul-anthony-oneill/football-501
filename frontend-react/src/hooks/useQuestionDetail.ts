"use client";

import { useCallback, useEffect, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import { useToast } from "@/context/ToastContext";
import type {
  Answer,
  Category,
  CreateAnswerRequest,
  CreateQuestionRequest,
  Question,
  UpdateQuestionRequest,
} from "@/lib/types/admin";

// ─── Public return type ───────────────────────────────────────────────────────

export interface UseQuestionDetailReturn {
  // ── Data ──────────────────────────────────────────────────────────────────
  question:   Question | null;
  categories: Category[];
  answers:    Answer[];
  loading:    boolean;

  // ── Question editing ──────────────────────────────────────────────────────
  isEditing:       boolean;
  setIsEditing:    (v: boolean) => void;
  questionLoading: boolean;
  rematerializing: boolean;

  // ── Answer preview ────────────────────────────────────────────────────────
  previewMode:    boolean;
  setPreviewMode: (v: boolean) => void;

  // ── Answer modal state ────────────────────────────────────────────────────
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

  // ── Actions ───────────────────────────────────────────────────────────────
  handleUpdateQuestion:  (data: CreateQuestionRequest | UpdateQuestionRequest) => Promise<void>;
  handleStatusTransition: () => Promise<void>;
  handleRematerialize:   () => Promise<void>;
  handleCreateAnswer:    (data: CreateAnswerRequest) => Promise<void>;
  handleUpdateAnswer:    (data: CreateAnswerRequest) => Promise<void>;
  handleBulkImport:      (data: { answers: CreateAnswerRequest[] }) => Promise<void>;
  handleDeleteAnswer:    () => Promise<void>;
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

/**
 * `useQuestionDetail` — owns all data-fetching and mutation logic for the
 * question detail admin page.
 *
 * Extracted from `questions/[id]/page.tsx` so that the page component is a
 * thin coordinator that wires together presentational sub-components.
 */
export function useQuestionDetail(questionId: string): UseQuestionDetailReturn {
  const { addToast } = useToast();

  // ── Core data ──────────────────────────────────────────────────────────────
  const [question,   setQuestion]   = useState<Question | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [answers,    setAnswers]    = useState<Answer[]>([]);
  const [loading,    setLoading]    = useState(true);

  // ── Question editing ───────────────────────────────────────────────────────
  const [isEditing,       setIsEditing]       = useState(false);
  const [questionLoading, setQuestionLoading] = useState(false);
  const [rematerializing, setRematerializing] = useState(false);

  // ── Answer preview ─────────────────────────────────────────────────────────
  const [previewMode, setPreviewMode] = useState(false);

  // ── Answer modals ──────────────────────────────────────────────────────────
  const [showCreateAnswer, setShowCreateAnswer] = useState(false);
  const [showEditAnswer,   setShowEditAnswer]   = useState(false);
  const [showBulkImport,   setShowBulkImport]   = useState(false);
  const [showDeleteAnswer, setShowDeleteAnswer] = useState(false);
  const [selectedAnswer,   setSelectedAnswer]   = useState<Answer | null>(null);
  const [answerLoading,    setAnswerLoading]    = useState(false);

  // ── Loaders ────────────────────────────────────────────────────────────────

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

  // ── Question actions ───────────────────────────────────────────────────────

  async function handleUpdateQuestion(
    data: CreateQuestionRequest | UpdateQuestionRequest
  ) {
    setQuestionLoading(true);
    try {
      const updated = await adminApi.updateQuestion(questionId, data as UpdateQuestionRequest);
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

    const statusConfig = {
      draft:   { nextStatus: "active"   as const },
      active:  { nextStatus: "retired"  as const },
      retired: { nextStatus: "active"   as const },
    };

    const { nextStatus } = statusConfig[question.status];
    try {
      const updated = await adminApi.updateQuestionStatus(questionId, { status: nextStatus });
      setQuestion(updated);
      addToast(`Question is now ${nextStatus}`, "success");
    } catch (err) {
      addToast((err as Error).message, "error");
    }
  }

  /**
   * Re-materialise answers for an active question after scraper data refresh.
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

  // ── Answer actions ─────────────────────────────────────────────────────────

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
        addToast(`Imported ${result.created} answers (Skipped ${result.skipped})`, "success");
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

  // ── Return ─────────────────────────────────────────────────────────────────

  return {
    question,
    categories,
    answers,
    loading,
    isEditing,
    setIsEditing,
    questionLoading,
    rematerializing,
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
    handleUpdateQuestion,
    handleStatusTransition,
    handleRematerialize,
    handleCreateAnswer,
    handleUpdateAnswer,
    handleBulkImport,
    handleDeleteAnswer,
  };
}
