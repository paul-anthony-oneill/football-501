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

  const confirmBg =
    type === "danger" ? "bg-[#ef4444]" : "bg-[#3b82f6]";

  return (
    <div
      ref={dialogRef}
      className="fixed inset-0 bg-black/70 flex justify-center items-center z-[1000] animate-fade-in"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-message"
    >
      <div
        className="bg-[#2a2a2a] p-8 rounded-xl w-[90%] max-w-[400px] shadow-[0_4px_20px_rgba(0,0,0,0.5)]"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id="confirm-title" className="m-0 mb-4 text-white">{title}</h3>
        <p id="confirm-message" className="text-[#d1d5db] mb-8 leading-relaxed">{message}</p>
        <div className="flex justify-end gap-4">
          <button
            className="px-6 py-3 rounded-lg bg-[#444] text-white font-medium text-base hover:opacity-90 transition-opacity"
            onClick={onCancel}
          >
            {cancelText}
          </button>
          <button
            className={`px-6 py-3 rounded-lg text-white font-medium text-base hover:opacity-90 transition-opacity ${confirmBg}`}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
