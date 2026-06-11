"use client";

import { useEffect, useRef } from "react";
import { useFocusTrap } from "@/hooks/useFocusTrap";

interface ConfirmDialogProps {
  open: boolean;
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  type?: "danger" | "info";
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmDialog({
  open,
  title = "Confirm",
  message = "Are you sure you want to proceed?",
  confirmText = "Confirm",
  cancelText = "Cancel",
  type = "danger",
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  useFocusTrap(dialogRef, open ? onCancel : undefined);

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

  const confirmClasses =
    type === "danger"
      ? "bg-danger text-white hover:opacity-90"
      : "bg-ink text-bg hover:opacity-90";

  return (
    <div
      ref={dialogRef}
      className="fixed inset-0 bg-black/60 backdrop-blur-sm flex justify-center items-center z-[1000] animate-fade-in"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-message"
    >
      <div
        className="bg-surface border border-line text-ink p-7 rounded-md w-[90%] max-w-[400px] shadow-[var(--shadow-pop)] animate-rise"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id="confirm-title" className="m-0 mb-3 font-display font-bold text-xl tracking-tight">{title}</h3>
        <p id="confirm-message" className="text-muted text-sm mb-7 leading-relaxed">{message}</p>
        <div className="flex justify-end gap-3">
          <button
            className="px-5 py-2.5 rounded-sm border border-line text-muted font-medium text-sm hover:text-ink hover:border-line-strong transition-colors"
            onClick={onCancel}
          >
            {cancelText}
          </button>
          <button
            className={`px-5 py-2.5 rounded-sm font-medium text-sm transition-opacity ${confirmClasses}`}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
