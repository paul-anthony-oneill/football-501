"use client";

import { useRef, useState } from "react";
import type { BulkCreateAnswersRequest, CreateAnswerRequest } from "@/lib/types/admin";

interface BulkImportModalProps {
  loading?: boolean;
  onSubmit: (data: BulkCreateAnswersRequest) => void;
  onCancel: () => void;
}

export default function BulkImportModal({
  loading = false,
  onSubmit,
  onCancel,
}: BulkImportModalProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [parsedData, setParsedData] = useState<CreateAnswerRequest[]>([]);
  const [preview, setPreview] = useState<CreateAnswerRequest[]>([]);
  const [error, setError] = useState("");

  function parseCSV(content: string) {
    try {
      const lines = content.split("\n").filter((l) => l.trim());
      if (lines.length < 2) {
        setError("CSV file is empty or missing data");
        setParsedData([]);
        setPreview([]);
        return;
      }

      const data: CreateAnswerRequest[] = [];
      for (let i = 1; i < lines.length; i++) {
        const parts = lines[i].trim().split(",");
        if (parts.length >= 2) {
          const displayText = parts[0].trim();
          const scoreStr = parts[parts.length - 1].trim();
          const score = Number(scoreStr);
          if (displayText && !isNaN(score)) {
            data.push({ displayText, score });
          }
        }
      }

      setParsedData(data);
      setPreview(data.slice(0, 5));
      setError("");
    } catch {
      setError("Failed to parse CSV");
      setParsedData([]);
      setPreview([]);
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => parseCSV(ev.target?.result as string);
    reader.readAsText(file);
  }

  function handleSubmit() {
    if (parsedData.length > 0) {
      onSubmit({ answers: parsedData });
    }
  }

  return (
    <div className="flex flex-col gap-6">
      {/* File picker */}
      <div className="flex items-center gap-4">
        <label
          htmlFor="csv-file"
          className="inline-block px-6 py-3 bg-[#3b82f6] text-white rounded-lg font-medium cursor-pointer hover:opacity-90 transition-opacity"
        >
          Choose CSV File
          <input
            ref={fileInputRef}
            id="csv-file"
            type="file"
            accept=".csv"
            className="hidden"
            onChange={handleFileChange}
            disabled={loading}
          />
        </label>
        <span className="text-[0.9rem] text-[#9ca3af]">Format: displayText,score</span>
      </div>

      {error && (
        <div className="text-[#ef4444] bg-[rgba(239,68,68,0.1)] px-3 py-3 rounded-lg">
          {error}
        </div>
      )}

      {preview.length > 0 && (
        <div className="bg-[#222] p-4 rounded-lg">
          <h4 className="m-0 mb-2 text-[#d1d5db] font-medium">
            Preview ({parsedData.length} items)
          </h4>
          <table className="w-full border-collapse text-[#ccc]">
            <thead>
              <tr>
                <th className="text-left p-2 text-[0.9rem] text-[#9ca3af] border-b border-[#333]">
                  Display Text
                </th>
                <th className="text-left p-2 text-[0.9rem] text-[#9ca3af] border-b border-[#333]">
                  Score
                </th>
              </tr>
            </thead>
            <tbody>
              {preview.map((item, i) => (
                <tr key={i}>
                  <td className="p-2 border-b border-[#333]">{item.displayText}</td>
                  <td className="p-2 border-b border-[#333]">{item.score}</td>
                </tr>
              ))}
              {parsedData.length > 5 && (
                <tr>
                  <td colSpan={2} className="p-2 text-center text-[#6b7280]">
                    …and {parsedData.length - 5} more
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Actions */}
      <div className="flex justify-end gap-4 pt-4 border-t border-white/10">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="px-6 py-3 rounded-lg bg-[#444] text-white font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={loading || parsedData.length === 0}
          className="px-6 py-3 rounded-lg bg-[var(--color-primary)] text-black font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          {loading ? "Importing…" : `Import ${parsedData.length} Answers`}
        </button>
      </div>
    </div>
  );
}
