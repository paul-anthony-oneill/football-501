"use client";

import { useCallback, useEffect, useState } from "react";
import { useToast } from "@/context/ToastContext";

interface EntityCounts {
  [entityType: string]: number;
}

// Known entity types and what they're used for
const ENTITY_TYPE_INFO: Record<string, { label: string; description: string }> = {
  footballer: {
    label: "Footballer",
    description: "Player names for football questions",
  },
  team: {
    label: "Team",
    description: "Club / national team names",
  },
  competition: {
    label: "Competition",
    description: "League and cup names",
  },
  country: {
    label: "Country",
    description: "Country names for nationality questions",
  },
  city: {
    label: "City",
    description: "City names",
  },
};

export default function EntitiesPage() {
  const { addToast } = useToast();
  const [counts, setCounts] = useState<EntityCounts | null>(null);
  const [loading, setLoading] = useState(true);

  const loadCounts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch("/api/admin/entities/counts");
      if (!res.ok) throw new Error("Failed to load entity counts");
      const data: EntityCounts = await res.json();
      setCounts(data);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadCounts();
  }, [loadCounts]);

  // Merge seeded counts with known types so zero-count types still appear
  const allTypes = Array.from(
    new Set([...Object.keys(ENTITY_TYPE_INFO), ...Object.keys(counts ?? {})])
  );

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="m-0 text-[var(--color-primary)]">Entity Pool</h1>
          <p className="m-0 mt-1 text-[var(--color-on-surface-variant)] text-sm">
            Named entities available in the autocomplete dropdown during gameplay.
            Each question type uses one pool (e.g. "footballer"). The pool must be seeded
            before a question of that type is activated.
          </p>
        </div>
        <button
          onClick={loadCounts}
          disabled={loading}
          className="px-4 py-2 rounded-lg border border-[#444] bg-[#2a2a2a] text-white text-sm cursor-pointer hover:opacity-80 transition-opacity disabled:opacity-50"
        >
          ↺ Refresh
        </button>
      </div>

      {loading ? (
        <div className="text-[var(--color-on-surface-variant)] py-16 text-center">
          Loading…
        </div>
      ) : (
        <>
          {/* Count cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
            {allTypes.map((type) => {
              const count = counts?.[type] ?? 0;
              const info = ENTITY_TYPE_INFO[type];
              const isEmpty = count === 0;

              return (
                <div
                  key={type}
                  className={[
                    "rounded-xl p-6 border",
                    isEmpty
                      ? "border-[#ef4444] bg-[rgba(239,68,68,0.05)]"
                      : "border-[var(--color-outline)] bg-[#2a2a2a]",
                  ].join(" ")}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <div className="font-semibold text-[var(--color-on-surface)]">
                        {info?.label ?? type}
                      </div>
                      <div className="text-xs text-[var(--color-on-surface-variant)] mt-0.5 font-mono">
                        {type}
                      </div>
                    </div>
                    {isEmpty && (
                      <span className="text-xs px-2 py-1 rounded bg-[rgba(239,68,68,0.15)] text-[#ef4444] border border-[#ef4444] font-medium">
                        ⚠ Empty
                      </span>
                    )}
                  </div>

                  <div
                    className={[
                      "text-4xl font-bold font-mono",
                      isEmpty ? "text-[#ef4444]" : "text-[var(--color-primary)]",
                    ].join(" ")}
                  >
                    {count.toLocaleString()}
                  </div>

                  {info && (
                    <div className="text-xs text-[var(--color-on-surface-variant)] mt-2">
                      {info.description}
                    </div>
                  )}

                  {isEmpty && (
                    <div className="mt-3 text-xs text-[#ef4444]">
                      Autocomplete will be empty for questions using this type.
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Help panel */}
          <div className="p-5 rounded-xl bg-[#2a2a2a] border border-[var(--color-outline)]">
            <h3 className="m-0 mb-3 text-sm font-semibold text-[var(--color-on-surface)]">
              How to seed the entity pool
            </h3>
            <ol className="m-0 pl-5 text-sm text-[var(--color-on-surface-variant)] space-y-2">
              <li>
                <strong>Automatic (recommended):</strong> Use{" "}
                <strong>Bulk Import</strong> on any question's detail page. Each
                imported answer's display text is automatically registered in the
                entities table.
              </li>
              <li>
                <strong>Manual (dev only):</strong> Run{" "}
                <code className="bg-[#1a1a1a] px-1 rounded text-xs">
                  backend/src/main/resources/db/seed_entities_dev.sql
                </code>{" "}
                against your local database to seed ~100 Premier League
                footballers.
              </li>
              <li>
                <strong>Verify:</strong> After seeding, click <em>Refresh</em>{" "}
                above. The count for "footballer" should be non-zero before
                activating a question with{" "}
                <code className="bg-[#1a1a1a] px-1 rounded text-xs">
                  entity_type: footballer
                </code>
                .
              </li>
            </ol>
          </div>
        </>
      )}
    </div>
  );
}
