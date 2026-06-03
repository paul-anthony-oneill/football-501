"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getFlagEmoji, formatNationality } from "@/utils/country";
import { apiFetch } from "@/lib/api/client";

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
  onSelect: (value: string, entityId?: string) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

/**
 * Autocomplete input for named-entity answers.
 *
 * - Fires after 4 characters to avoid overly broad early results.
 * - Shows "Keep typing…" hint at 1–3 characters so the player knows
 *   suggestions are coming.
 * - Clicking a suggestion, or pressing Enter (snaps to first if none highlighted),
 *   calls onSelect and clears the input. Submission is handled by the parent.
 * - Accent-insensitive: typing "aguero" surfaces "Sergio Agüero".
 *
 * The search hits GET /api/entities/search?type={entityType}&query={query},
 * which queries the global `entities` table — NOT the `answers` table for
 * the current question.  A name appearing in the dropdown tells the player
 * nothing about whether it is a valid answer.
 */
export default function EntitySearch({
  entityType = "footballer",
  onSelect,
  placeholder = "Enter answer...",
  className = "",
  disabled = false,
}: EntitySearchProps) {
  const [value, setValue] = useState("");
  const [suggestions, setSuggestions] = useState<EntitySuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [noMatch, setNoMatch] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const fetchSuggestions = useCallback(
    async (query: string) => {
      setLoading(true);
      try {
        const res = await apiFetch(
          `/api/entities/search?type=${encodeURIComponent(entityType)}&query=${encodeURIComponent(query)}`
        );
        if (res.ok) {
          const data: EntitySuggestion[] = await res.json();
          setSuggestions(data);
          setShowSuggestions(data.length > 0);
          setActiveIndex(-1);
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
    setValue(val);
    setNoMatch(false);

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

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (showSuggestions && suggestions.length > 0) {
        // Snap to first item if nothing is highlighted, then select
        const idx = activeIndex >= 0 ? activeIndex : 0;
        selectEntity(suggestions[idx]);
      } else if (!showSuggestions && value.trim().length >= 4) {
        // Typed something but no suggestions came back
        setNoMatch(true);
      }
      // If <4 chars, the "Keep typing" hint already guides the user — do nothing
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!showSuggestions && suggestions.length > 0) setShowSuggestions(true);
      setActiveIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : 0));
    } else if (e.key === "Escape") {
      setShowSuggestions(false);
    }
  }

  function selectEntity(entity: EntitySuggestion) {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    setValue("");
    setSuggestions([]);
    setShowSuggestions(false);
    setActiveIndex(-1);
    setNoMatch(false);
    onSelect(entity.name, entity.id);
    inputRef.current?.focus();
  }

  function handleBlur() {
    // Delay so a mousedown on a suggestion fires before the blur hides the list.
    setTimeout(() => setShowSuggestions(false), 200);
  }

  return (
    <div className="relative flex-1 flex flex-col min-w-0">
      <input
        ref={inputRef}
        type="text"
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onBlur={handleBlur}
        placeholder={placeholder}
        autoComplete="off"
        disabled={disabled}
        className={className}
      />

      {/* Hint shown while typing 1–3 characters */}
      {value.length > 0 && value.length < 4 && (
        <p className="mt-1 text-xs text-tele-cyan px-1 opacity-80">
          Keep typing for suggestions…
        </p>
      )}

      {/* No-match hint — shown when ≥4 chars typed but no results */}
      {noMatch && value.length >= 4 && (
        <p className="mt-1 text-xs text-tele-danger px-1">
          No match — try a different spelling
        </p>
      )}

      {showSuggestions && (
        <ul className="ta-list absolute bottom-full left-0 right-0 mb-2 p-0 m-0 list-none bg-black border-2 border-white overflow-y-auto max-h-[300px] z-50 shadow-[0_-12px_32px_rgba(0,0,0,0.5)]">
          {suggestions.map((entity, idx) => (
            <li key={entity.id}>
              <button
                onMouseDown={(e) => { e.preventDefault(); selectEntity(entity); }}
                data-active={activeIndex === idx ? "1" : "0"}
                className={`ta-opt w-full text-left grid grid-cols-[1fr_auto] gap-4 items-center px-5.5 py-3 bg-transparent border-0 border-b border-white border-dashed last:border-b-0 cursor-pointer transition-colors ${
                  activeIndex === idx ? 'bg-[#00008c]' : 'hover:bg-[#00008c]'
                }`}
              >
                <span className="ta-opt-name text-white text-[22px] tracking-wide overflow-hidden text-ellipsis whitespace-nowrap">
                  {entity.name.toUpperCase()}
                </span>
                {entity.nationality && (
                  <span className="ta-opt-club text-tele-cyan text-[18px] whitespace-nowrap">
                    {getFlagEmoji(entity.nationality)} {formatNationality(entity.nationality)}
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
