"use client";

import type { ReactNode } from "react";

interface Column {
  key: string;
  label: string;
}

interface DataTableProps<T extends { id: string }> {
  columns: Column[];
  data: T[];
  loading?: boolean;
  totalElements?: number;
  pageSize?: number;
  currentPage?: number;
  /** Render custom action buttons per row. Receives the row item. */
  renderActions?: (item: T) => ReactNode;
  /**
   * Override cell rendering for specific columns.
   * Return `undefined` to fall back to default string rendering for that cell.
   */
  renderCell?: (key: string, item: T) => ReactNode;
  onPageChange?: (page: number) => void;
}

export default function DataTable<T extends { id: string }>({
  columns,
  data,
  loading = false,
  totalElements = 0,
  pageSize = 10,
  currentPage = 0,
  renderActions,
  renderCell,
  onPageChange,
}: DataTableProps<T>) {
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const showActions = !!renderActions;

  function handlePage(next: number) {
    if (next >= 0 && next < totalPages && onPageChange) {
      onPageChange(next);
    }
  }

  return (
    <div className="w-full overflow-x-auto bg-[var(--color-surface)] rounded-[var(--radius-md)] shadow-[var(--shadow-2)] border border-[var(--color-outline)] p-4">
      <table className="w-full border-collapse text-[var(--color-on-surface)]">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className="p-4 text-left font-bold text-[var(--color-primary)] text-[0.85rem] uppercase tracking-[1px] bg-[rgba(74,222,128,0.05)] border-b border-[var(--color-outline)]"
              >
                {col.label}
              </th>
            ))}
            {showActions && (
              <th className="p-4 text-right font-bold text-[var(--color-primary)] text-[0.85rem] uppercase tracking-[1px] bg-[rgba(74,222,128,0.05)] border-b border-[var(--color-outline)]">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td
                colSpan={columns.length + (showActions ? 1 : 0)}
                className="p-12 text-center text-[var(--color-on-surface-variant)]"
              >
                <div className="w-8 h-8 border-[3px] border-[var(--color-surface-variant)] border-t-[var(--color-primary)] rounded-full animate-spin-slow mx-auto mb-4" />
                <span>Loading…</span>
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (showActions ? 1 : 0)}
                className="p-12 text-center text-[var(--color-on-surface-variant)]"
              >
                No data found
              </td>
            </tr>
          ) : (
            data.map((item) => (
              <tr
                key={item.id}
                className="border-b border-[var(--color-outline)] hover:bg-[var(--color-surface-variant)]/20 transition-colors"
              >
                {columns.map((col) => {
                  const custom = renderCell?.(col.key, item);
                  return (
                    <td key={col.key} className="p-4">
                      {custom !== undefined
                        ? custom
                        : String((item as Record<string, unknown>)[col.key] ?? "")}
                    </td>
                  );
                })}
                {showActions && (
                  <td className="p-4">
                    <div className="flex gap-2 justify-end">
                      {renderActions(item)}
                    </div>
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {/* Pagination */}
      <div className="flex justify-between items-center mt-4 pt-4 border-t border-[var(--color-outline)]">
        <button
          disabled={currentPage === 0}
          onClick={() => handlePage(currentPage - 1)}
          className="px-4 py-2 bg-[var(--color-surface-variant)] text-[var(--color-on-surface-variant)] rounded-[var(--radius-sm)] text-[0.85rem] font-semibold disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[var(--color-primary-container)] hover:text-[var(--color-on-primary-container)] transition-colors"
        >
          ← Previous
        </button>
        <span className="text-[0.85rem] text-[var(--color-on-surface-variant)] font-medium">
          Page {currentPage + 1} of {totalPages}
        </span>
        <button
          disabled={currentPage >= totalPages - 1}
          onClick={() => handlePage(currentPage + 1)}
          className="px-4 py-2 bg-[var(--color-surface-variant)] text-[var(--color-on-surface-variant)] rounded-[var(--radius-sm)] text-[0.85rem] font-semibold disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[var(--color-primary-container)] hover:text-[var(--color-on-primary-container)] transition-colors"
        >
          Next →
        </button>
      </div>
    </div>
  );
}
