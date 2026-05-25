"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getFlagEmoji } from "@/utils/country";

interface EntitySuggestion {
  id: string;
  name: string;
  /** Hint shown alongside the name (e.g. country code rendered as a flag emoji). */
  nationality: string;
}

interface EntitySearchProps {
  /**
   * Scopes the autocomplete to a specific pool of named entities.
   * Must match the `entity_type` value in the active question's config.
   * Examples: "footballer" | "city" | "country" | "director"
   * Defaults to "footballer" for backward compatibility.
   */
  entityType?: string;
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Autocomplete input for named-entity answers.
 *
 * - Fires after 4 characters to avoid overly broad early results.
 * - Shows "Keep typing…" hint at 1–3 characters so the player knows
 *   suggestions are coming.
 * - Selecting a suggestion fills the input without auto-submitting,
 *   so the player can confirm before pressing Enter / Submit.
 * - Accent-insensitive: typing "aguero" surfaces "Sergio Agüero".
 *
 * The search hits GET /api/entities/search?type={entityType}&query={query},
 * which queries the global `entities` table — NOT the `answers` table for
 * the current question.  A name appearing in the dropdown tells the player
 * nothing about whether it is a valid answer.
 */
export default function EntitySearch({
  entityType = "footballer",
  value,
  onChange,
  onSubmit,
  disabled = false,
  placeholder = "Enter answer...",
}: EntitySearchProps) {
  const [suggestions, setSuggestions] = useState<EntitySuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const fetchSuggestions = useCallback(
    async (query: string) => {
      setLoading(true);
      try {
        const res = await fetch(
          `/api/entities/search?type=${encodeURIComponent(entityType)}&query=${encodeURIComponent(query)}`
        );
        if (res.ok) {
          const data: EntitySuggestion[] = await res.json();
          setSuggestions(data);
          setShowSuggestions(data.length > 0);
        }
      } catch (err) {
        console.error("Error fetching entity suggestions:", err);
      } finally {
        setLoading(false);
      }
    },
    [entityType]
  );

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    onChange(val);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (val.length < 4) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    debounceRef.current = setTimeout(() => {
      fetchSuggestions(val);
    }, 300);
  }

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  function selectEntity(entity: EntitySuggestion) {
    onChange(entity.name);
    setSuggestions([]);
    setShowSuggestions(false);
    inputRef.current?.focus();
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      setShowSuggestions(false);
      onSubmit();
    }
  }

  function handleBlur() {
    // Delay so a mousedown on a suggestion fires before the blur hides the list.
    setTimeout(() => setShowSuggestions(false), 200);
  }

  return (
    <div className="relative w-full">
      {/* Input row */}
      <div className="relative flex items-center">
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onBlur={handleBlur}
          disabled={disabled}
          placeholder={placeholder}
          autoComplete="off"
          className="w-full box-border px-4 py-3 text-base border-2 border-[var(--color-primary)] rounded-lg bg-[#1a1a1a] text-white focus:outline-none focus:border-[#22c55e] focus:shadow-[0_0_0_3px_rgba(74,222,128,0.1)] disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        />
        {loading && (
          <div className="absolute right-4 w-4 h-4 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin-slow" />
        )}
      </div>

      {/* Hint shown while typing 1–3 characters */}
      {value.length > 0 && value.length < 4 && (
        <p className="mt-1 text-xs text-[#9ca3af] px-1">
          Keep typing for suggestions…
        </p>
      )}

      {/* Suggestions dropdown */}
      {showSuggestions && (
        <ul className="absolute top-full left-0 right-0 mt-2 p-0 m-0 list-none bg-[#2a2a2a] border border-[var(--color-primary)] rounded-lg max-h-[200px] overflow-y-auto z-10 shadow-[0_4px_6px_rgba(0,0,0,0.3)] animate-slide-down">
          {suggestions.map((entity) => (
            <li key={entity.id}>
              <button
                onMouseDown={() => selectEntity(entity)} // mousedown fires before blur
                className="w-full text-left px-4 py-3 bg-transparent border-none text-white cursor-pointer flex justify-between items-center hover:bg-[#374151] transition-colors"
              >
                <span className="font-medium">{entity.name}</span>
                {entity.nationality && (
                  <span className="text-[#9ca3af] text-sm" title={entity.nationality}>
                    {getFlagEmoji(entity.nationality)}
                  </span>
                )}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
