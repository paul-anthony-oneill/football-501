"use client";

import { useEffect } from "react";

interface FormModalProps {
  open: boolean;
  title: string;
  onSave: () => void;
  onCancel: () => void;
  loading?: boolean;
  saveText?: string;
  cancelText?: string;
  children: React.ReactNode;
}

export default function FormModal({
  open,
  title,
  onSave,
  onCancel,
  loading = false,
  saveText = "Save",
  cancelText = "Cancel",
  children,
}: FormModalProps) {
  // Lock body scroll while modal is open
  useEffect(() => {
    if (open) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 bg-black/70 flex justify-center items-center z-[1000] animate-fade-in"
      onClick={onCancel}
    >
      <div
        className="bg-[#2a2a2a] rounded-xl w-[90%] max-w-[600px] max-h-[90vh] flex flex-col shadow-[0_4px_20px_rgba(0,0,0,0.5)]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-white/10 flex justify-between items-center">
          <h3 className="m-0 text-white font-semibold">{title}</h3>
          <button
            className="bg-transparent border-none text-[#9ca3af] text-2xl cursor-pointer p-0 leading-none hover:text-white transition-colors"
            onClick={onCancel}
            aria-label="Close"
          >
            &times;
          </button>
        </div>

        {/* Content */}
        <div className="px-6 py-5 overflow-y-auto flex-1">{children}</div>

        {/* Footer */}
        <div className="px-6 py-5 border-t border-white/10 flex justify-end gap-4">
          <button
            className="px-6 py-3 rounded-lg bg-[#444] text-white font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
            onClick={onCancel}
            disabled={loading}
          >
            {cancelText}
          </button>
          <button
            className="px-6 py-3 rounded-lg bg-[var(--color-primary)] text-black font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
            onClick={onSave}
            disabled={loading}
          >
            {loading ? "Saving…" : saveText}
          </button>
        </div>
      </div>
    </div>
  );
}
