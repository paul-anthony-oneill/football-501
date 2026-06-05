"use client";

import { useEffect, useRef, useState } from "react";
import { getFlagEmoji, formatNationality } from "@/utils/country";
import {
  loadEntityCache,
  getEntityCacheSync,
  searchEntities,
  type CachedEntity,
} from "@/lib/api/entityCache";

interface EntitySuggestion {
  id: string;
  name: string;
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
 * Loads all entities for the active type once on mount and filters client-side
 * on every keystroke — no per-keystroke API calls, no rate-limit pressure.
 * Falls back to an async fetch path while the cache is still loading.
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

  // Warm the cache as soon as the component mounts so it's ready before typing starts.
  useEffect(() => {
    loadEntityCache(entityType);
  }, [entityType]);

  function applyResults(results: CachedEntity[]) {
    setSuggestions(results.map((e) => ({ id: e.id, name: e.name, nationality: e.nationality ?? "" })));
    setShowSuggestions(results.length > 0);
    setNoMatch(results.length === 0);
    setActiveIndex(-1);
    setLoading(false);
  }

  async function fetchSuggestionsAsync(query: string) {
    setLoading(true);
    const entities = await loadEntityCache(entityType);
    applyResults(searchEntities(entities, query));
  }

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

    // Synchronous path — cache already loaded, no debounce needed
    const cached = getEntityCacheSync(entityType);
    if (cached) {
      applyResults(searchEntities(cached, val));
      return;
    }

    // Async path — first load still in flight, debounce to avoid hammering
    debounceRef.current = setTimeout(() => fetchSuggestionsAsync(val), 300);
  }

  useEffect(() => {
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, []);

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (showSuggestions && suggestions.length > 0) {
        const idx = activeIndex >= 0 ? activeIndex : 0;
        selectSuggestion(suggestions[idx]);
      } else if (!showSuggestions && value.trim().length >= 4) {
        setNoMatch(true);
      }
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

  function selectSuggestion(entity: EntitySuggestion) {
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
        aria-label="Search player name"
      />

      {value.length > 0 && value.length < 4 && (
        <p className="mt-1 text-xs text-tele-cyan px-1 opacity-80">
          Keep typing for suggestions…
        </p>
      )}

      {loading && value.length >= 4 && (
        <p className="mt-1 text-xs text-tele-cyan px-1 opacity-80">
          Loading…
        </p>
      )}

      {noMatch && !loading && value.length >= 4 && (
        <p className="mt-1 text-xs text-tele-danger px-1">
          No match — try a different spelling
        </p>
      )}

      {showSuggestions && (
        <ul className="ta-list absolute bottom-full left-0 right-0 mb-2 p-0 m-0 list-none bg-black border-2 border-white overflow-y-auto max-h-[300px] z-50 shadow-[0_-12px_32px_rgba(0,0,0,0.5)]">
          {suggestions.map((entity, idx) => (
            <li key={entity.id}>
              <button
                onMouseDown={(e) => { e.preventDefault(); selectSuggestion(entity); }}
                data-active={activeIndex === idx ? "1" : "0"}
                className={`ta-opt w-full text-left grid grid-cols-[1fr_auto] gap-4 items-center px-5.5 py-3 bg-transparent border-0 border-b border-white border-dashed last:border-b-0 cursor-pointer transition-colors active:bg-[#00008c] ${
                  activeIndex === idx ? "bg-[#00008c]" : "hover:bg-[#00008c]"
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
