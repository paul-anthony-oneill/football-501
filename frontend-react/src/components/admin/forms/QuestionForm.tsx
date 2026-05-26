"use client";

import { useState } from "react";
import TextField from "@/components/ui/TextField";
import TextArea from "@/components/ui/TextArea";
import Select from "@/components/ui/Select";
import type {
  Question,
  Category,
  CreateQuestionRequest,
  UpdateQuestionRequest,
} from "@/lib/types/admin";
import { METRIC_KEY_OPTIONS } from "@/lib/types/admin";

interface QuestionFormProps {
  question?: Question;
  categories: Category[];
  loading?: boolean;
  onSubmit: (data: CreateQuestionRequest | UpdateQuestionRequest) => void;
  onCancel: () => void;
}

const difficultyOptions = [
  { value: "1", label: "Easy" },
  { value: "2", label: "Medium" },
  { value: "3", label: "Hard" },
];

const metricKeyOptions = [
  { value: "", label: "Select a metric" },
  ...METRIC_KEY_OPTIONS.map((o) => ({ value: o.value, label: o.label })),
];

export default function QuestionForm({
  question,
  categories,
  loading = false,
  onSubmit,
  onCancel,
}: QuestionFormProps) {
  const isEdit = !!question;

  const [categoryId, setCategoryId] = useState(question?.categoryId ?? "");
  const [questionText, setQuestionText] = useState(question?.questionText ?? "");
  const [metricKey, setMetricKey] = useState(question?.metricKey ?? "");
  const [minScore, setMinScore] = useState(question?.minScore?.toString() ?? "");
  const [difficulty, setDifficulty] = useState(
    question?.difficulty?.toString() ?? "2"
  );
  const [configJson, setConfigJson] = useState(
    question?.config ? JSON.stringify(question.config, null, 2) : "{}"
  );
  const [errors, setErrors] = useState<Record<string, string>>({});

  const categoryOptions = [
    { value: "", label: "Select a Category" },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!categoryId) errs.categoryId = "Category is required";
    if (!questionText.trim()) errs.questionText = "Question text is required";
    if (!metricKey.trim()) errs.metricKey = "Metric key is required";
    try {
      JSON.parse(configJson);
    } catch {
      errs.config = "Invalid JSON format";
    }
    if (minScore && isNaN(Number(minScore))) errs.minScore = "Must be a number";
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      categoryId,
      questionText,
      metricKey,
      minScore: minScore ? Number(minScore) : undefined,
      difficulty: Number(difficulty),
      config: JSON.parse(configJson),
    });
  }

  return (
    <form onSubmit={handleSubmit}>
      <Select
        label="Category"
        value={categoryId}
        onChange={setCategoryId}
        options={categoryOptions}
        required
        error={errors.categoryId}
        disabled={loading}
      />

      <Select
        label="Difficulty"
        value={difficulty}
        onChange={setDifficulty}
        options={difficultyOptions}
        required
        disabled={loading}
      />

      <TextArea
        label="Question Text"
        value={questionText}
        onChange={setQuestionText}
        required
        rows={2}
        error={errors.questionText}
        disabled={loading}
        placeholder="e.g. Which player has the most Premier League appearances?"
      />

      <Select
        label="Metric Key"
        value={metricKey}
        onChange={setMetricKey}
        options={metricKeyOptions}
        required
        error={errors.metricKey}
        disabled={loading}
      />

      <TextField
        label="Minimum Score (Optional)"
        value={minScore}
        onChange={setMinScore}
        type="number"
        error={errors.minScore}
        disabled={loading}
        placeholder="0"
      />

      <TextArea
        label="Configuration (JSON)"
        value={configJson}
        onChange={setConfigJson}
        rows={5}
        error={errors.config}
        disabled={loading}
        placeholder="JSON object"
      />

      <div className="flex justify-end gap-4 mt-8 pt-4 border-t border-white/10">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="px-6 py-3 rounded-lg bg-[#444] text-white font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-6 py-3 rounded-lg bg-[var(--color-primary)] text-black font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          {loading ? "Saving…" : isEdit ? "Update Question" : "Create Question"}
        </button>
      </div>
    </form>
  );
}
