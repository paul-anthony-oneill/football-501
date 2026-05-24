"use client";

import { useState } from "react";
import TextField from "@/components/ui/TextField";
import TextArea from "@/components/ui/TextArea";
import type { Answer, CreateAnswerRequest } from "@/lib/types/admin";

interface AnswerFormProps {
  answer?: Answer;
  loading?: boolean;
  onSubmit: (data: CreateAnswerRequest) => void;
  onCancel: () => void;
}

export default function AnswerForm({
  answer,
  loading = false,
  onSubmit,
  onCancel,
}: AnswerFormProps) {
  const isEdit = !!answer;

  const [displayText, setDisplayText] = useState(answer?.displayText ?? "");
  const [score, setScore] = useState(answer?.score?.toString() ?? "");
  const [metadataJson, setMetadataJson] = useState(
    answer?.metadata ? JSON.stringify(answer.metadata, null, 2) : "{}"
  );
  const [errors, setErrors] = useState<Record<string, string>>({});

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!displayText.trim()) errs.displayText = "Display text is required";
    if (!score) {
      errs.score = "Score is required";
    } else {
      const n = Number(score);
      if (isNaN(n) || n < 1 || n > 300) errs.score = "Score must be between 1 and 300";
    }
    try {
      JSON.parse(metadataJson);
    } catch {
      errs.metadata = "Invalid JSON format";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      displayText,
      score: Number(score),
      metadata: JSON.parse(metadataJson),
    });
  }

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        label="Display Text"
        value={displayText}
        onChange={setDisplayText}
        required
        error={errors.displayText}
        disabled={loading}
        placeholder="e.g. Erling Haaland"
      />

      <TextField
        label="Score"
        value={score}
        onChange={setScore}
        type="number"
        required
        error={errors.score}
        disabled={loading}
        placeholder="e.g. 52"
      />

      <TextArea
        label="Metadata (JSON)"
        value={metadataJson}
        onChange={setMetadataJson}
        rows={3}
        error={errors.metadata}
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
          {loading ? "Saving…" : isEdit ? "Update Answer" : "Create Answer"}
        </button>
      </div>
    </form>
  );
}
