"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";

// ─── Types ────────────────────────────────────────────────────────────────────

export type ToastType = "success" | "error" | "info";

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  toasts: Toast[];
  addToast: (message: string, type?: ToastType) => void;
  removeToast: (id: string) => void;
}

// ─── Context ──────────────────────────────────────────────────────────────────

const ToastContext = createContext<ToastContextValue | null>(null);

// ─── Individual Toast component ───────────────────────────────────────────────

function ToastItem({ toast, onClose }: { toast: Toast; onClose: () => void }) {
  useEffect(() => {
    const duration = toast.type === "error" ? 8000 : 3000;
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [onClose, toast.type]);

  const icon = toast.type === "success" ? "✅" : toast.type === "error" ? "❌" : "ℹ️";

  const borderColor =
    toast.type === "success"
      ? "border-l-[var(--color-primary)]"
      : toast.type === "error"
      ? "border-l-[var(--color-error)]"
      : "border-l-[#7fcfff]";

  const bg =
    toast.type === "success"
      ? "bg-[#00522b]"
      : toast.type === "error"
      ? "bg-[#93000a]"
      : "bg-[#004a77]";

  return (
    <div
      className={`animate-slide-down flex items-center gap-4 min-w-80 max-w-[450px] rounded-xl border border-white/10 border-l-[6px] px-6 py-4 text-white shadow-[var(--shadow-3)] ${bg} ${borderColor}`}
      role="alert"
    >
      <span className="text-xl" aria-hidden="true">{icon}</span>
      <span className="flex-1 text-[0.95rem] font-semibold">{toast.message}</span>
      <button
        onClick={onClose}
        className="text-white/70 hover:text-white text-2xl leading-none p-1 bg-transparent border-none cursor-pointer transition-colors"
        aria-label="Close notification"
      >
        &times;
      </button>
    </div>
  );
}

// ─── Provider ────────────────────────────────────────────────────────────────

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const counterRef = useRef(0);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback((message: string, type: ToastType = "info") => {
    const id = `toast-${++counterRef.current}`;
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}

      {/* Toast stack — fixed top-right */}
      <div className="fixed top-6 right-6 z-[2000] flex flex-col gap-3 pointer-events-none">
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            <ToastItem
              toast={toast}
              onClose={() => removeToast(toast.id)}
            />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

// ─── Hook ────────────────────────────────────────────────────────────────────

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast must be used inside <ToastProvider>");
  }
  return ctx;
}
