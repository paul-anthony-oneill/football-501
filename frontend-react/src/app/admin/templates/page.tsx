"use client";

import { useCallback, useEffect, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import type { QuestionTemplate, GeneratorResult } from "@/lib/types/admin";
import { useToast } from "@/context/ToastContext";

// ── Column definitions ──────────────────────────────────────────────────────

// Metric key → human label
const METRIC_LABELS: Record<string, string> = {
  goals:           "Goals",
  appearances:     "Appearances",
  assists:         "Assists",
  goals_assists:   "Goals + Assists",
  clean_sheets:    "Clean sheets",
  sub_appearances: "Substitute appearances",
};

// Materializer key → short display label
function shortMaterializerLabel(key: string): string {
  const map: Record<string, string> = {
    "football.team_competition_metric_since":   "Team · since",
    "football.team_competition_season_metric":  "Team · season",
    "football.player_competition_metric_since": "League · since",
    "football.player_career_metric":            "Career",
  };
  return map[key] ?? key;
}

export default function TemplatesPage() {
  const { addToast } = useToast();

  const [templates, setTemplates]           = useState<QuestionTemplate[]>([]);
  const [loading, setLoading]               = useState(true);
  const [generatingAll, setGeneratingAll]   = useState(false);
  // Tracks which individual template IDs are currently generating
  const [generatingIds, setGeneratingIds]   = useState<Set<string>>(new Set());

  const loadTemplates = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listTemplates();
      setTemplates(data);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => { loadTemplates(); }, [loadTemplates]);

  // ── Generate All ───────────────────────────────────────────────────────────

  async function handleGenerateAll() {
    setGeneratingAll(true);
    try {
      const result: GeneratorResult = await adminApi.generateAll();
      addToast(
        `Generated: ${result.created} new draft${result.created !== 1 ? "s" : ""}, ${result.skipped} already existed.`,
        "success"
      );
      await loadTemplates();
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setGeneratingAll(false);
    }
  }

  // ── Generate single template ───────────────────────────────────────────────

  async function handleGenerate(template: QuestionTemplate) {
    setGeneratingIds((prev) => new Set(prev).add(template.id));
    try {
      const result: GeneratorResult = await adminApi.generateForTemplate(template.id);
      addToast(
        `${template.displayName}: ${result.created} new draft${result.created !== 1 ? "s" : ""}, ${result.skipped} already existed.`,
        "success"
      );
      await loadTemplates();
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setGeneratingIds((prev) => {
        const next = new Set(prev);
        next.delete(template.id);
        return next;
      });
    }
  }

  // ── Totals row ─────────────────────────────────────────────────────────────

  const totalDrafts  = templates.reduce((s, t) => s + t.draftCount,  0);
  const totalActive  = templates.reduce((s, t) => s + t.activeCount, 0);

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="m-0 text-[var(--color-primary)]">Question Templates</h1>
          <p className="m-0 mt-1 text-[var(--color-on-surface-variant)] text-sm">
            Templates define question shapes. Generate draft questions from them, then
            activate drafts to materialise answers.
          </p>
        </div>
        <button
          onClick={handleGenerateAll}
          disabled={generatingAll || loading}
          className="bg-[var(--color-primary)] text-black px-6 py-3 rounded-lg font-semibold hover:bg-[#22c55e] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {generatingAll ? "Generating…" : "⚡ Generate All"}
        </button>
      </div>

      {/* Summary chips */}
      {!loading && (
        <div className="flex gap-4 mb-6">
          <div className="bg-[#2a2a2a] rounded-lg px-5 py-3 text-center">
            <div className="text-2xl font-bold text-[var(--color-primary)]">{templates.length}</div>
            <div className="text-xs text-[var(--color-on-surface-variant)] mt-0.5">Templates</div>
          </div>
          <div className="bg-[#2a2a2a] rounded-lg px-5 py-3 text-center">
            <div className="text-2xl font-bold text-[#9ca3af]">{totalDrafts.toLocaleString()}</div>
            <div className="text-xs text-[var(--color-on-surface-variant)] mt-0.5">Draft questions</div>
          </div>
          <div className="bg-[#2a2a2a] rounded-lg px-5 py-3 text-center">
            <div className="text-2xl font-bold text-[#4ade80]">{totalActive.toLocaleString()}</div>
            <div className="text-xs text-[var(--color-on-surface-variant)] mt-0.5">Active questions</div>
          </div>
        </div>
      )}

      {/* Table */}
      {loading ? (
        <div className="text-[var(--color-on-surface-variant)] py-16 text-center">Loading…</div>
      ) : (
        <div className="bg-[#2a2a2a] rounded-xl overflow-hidden border border-[var(--color-outline)]">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-[var(--color-outline)]">
                {["Template", "Metric", "Scope", "Drafts", "Active", "Status", ""].map((h) => (
                  <th
                    key={h}
                    className="text-left px-5 py-4 text-xs font-semibold text-[var(--color-on-surface-variant)] uppercase tracking-wide"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {templates.map((t, i) => {
                const isGenerating = generatingIds.has(t.id);
                return (
                  <tr
                    key={t.id}
                    className={[
                      "border-b border-[var(--color-outline)] last:border-0 transition-colors hover:bg-[rgba(255,255,255,0.03)]",
                      i % 2 === 0 ? "" : "bg-[rgba(255,255,255,0.01)]",
                    ].join(" ")}
                  >
                    {/* Template name + text preview */}
                    <td className="px-5 py-4">
                      <div className="font-medium text-[var(--color-on-surface)]">
                        {t.displayName}
                      </div>
                      <div className="text-xs text-[var(--color-on-surface-variant)] mt-0.5 font-mono">
                        {t.textTemplate}
                      </div>
                    </td>

                    {/* Metric */}
                    <td className="px-5 py-4 text-sm">
                      <span className="bg-[rgba(99,102,241,0.15)] text-[#818cf8] px-2 py-0.5 rounded text-xs font-medium">
                        {METRIC_LABELS[t.metricKey] ?? t.metricKey}
                      </span>
                    </td>

                    {/* Materializer scope */}
                    <td className="px-5 py-4 text-sm text-[var(--color-on-surface-variant)]">
                      {shortMaterializerLabel(t.materializerKey)}
                    </td>

                    {/* Draft count */}
                    <td className="px-5 py-4 text-sm text-[#9ca3af] font-mono">
                      {t.draftCount.toLocaleString()}
                    </td>

                    {/* Active count */}
                    <td className="px-5 py-4 text-sm text-[#4ade80] font-mono">
                      {t.activeCount.toLocaleString()}
                    </td>

                    {/* Status badges */}
                    <td className="px-5 py-4">
                      <div className="flex flex-col gap-1">
                        <span
                          className={[
                            "px-2 py-0.5 rounded text-xs font-medium border w-fit",
                            t.active
                              ? "bg-[rgba(74,222,128,0.1)] text-[#4ade80] border-[#4ade80]"
                              : "bg-[rgba(239,68,68,0.1)] text-[#ef4444] border-[#ef4444]",
                          ].join(" ")}
                        >
                          {t.active ? "Active" : "Inactive"}
                        </span>
                        {!t.hasMaterializer && (
                          <span className="px-2 py-0.5 rounded text-xs font-medium border bg-[rgba(251,191,36,0.1)] text-[#fbbf24] border-[#fbbf24] w-fit">
                            ⚠ No materializer
                          </span>
                        )}
                      </div>
                    </td>

                    {/* Generate button */}
                    <td className="px-5 py-4 text-right">
                      <button
                        onClick={() => handleGenerate(t)}
                        disabled={isGenerating || !t.active}
                        title={
                          !t.active
                            ? "Template is inactive"
                            : "Generate draft questions from this template"
                        }
                        className="bg-[#3b82f6] text-white px-4 py-2 rounded-lg text-sm font-medium border-none cursor-pointer hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        {isGenerating ? "…" : "Generate"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {templates.length === 0 && (
            <div className="text-center py-16 text-[var(--color-on-surface-variant)]">
              No templates found.
            </div>
          )}
        </div>
      )}

      {/* Help text */}
      <div className="mt-6 p-4 bg-[#2a2a2a] rounded-lg border border-[var(--color-outline)]">
        <h3 className="m-0 mb-2 text-sm font-semibold text-[var(--color-on-surface)]">
          How the pipeline works
        </h3>
        <ol className="m-0 pl-5 text-sm text-[var(--color-on-surface-variant)] space-y-1">
          <li>Click <strong>Generate</strong> (or <strong>Generate All</strong>) to create <em>draft</em> questions for every valid parameter combination.</li>
          <li>Go to <strong>Questions</strong>, filter by <em>Draft</em>, and activate the ones you want in the game.</li>
          <li>Activating a question automatically materialises its answer set from the scraped data.</li>
          <li>After the scraper refreshes data, use <strong>Rematerialize</strong> on the question detail page to update cached answers.</li>
        </ol>
      </div>
    </div>
  );
}
