"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getFlagEmoji } from "@/utils/country";

interface Suggestion {
  id: string;
  name: string;
  nationality: string;
}

interface PlayerSearchProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  disabled?: boolean;
  placeholder?: string;
}

export default function PlayerSearch({
  value,
  onChange,
  onSubmit,
  disabled = false,
  placeholder = "Enter player name...",
}: PlayerSearchProps) {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const fetchSuggestions = useCallback(async (query: string) => {
    setLoading(true);
    try {
      const res = await fetch(
        `/api/players/search?query=${encodeURIComponent(query)}`
      );
      if (res.ok) {
        const data: Suggestion[] = await res.json();
        setSuggestions(data);
        setShowSuggestions(data.length > 0);
      }
    } catch (err) {
      console.error("Error fetching player suggestions:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    onChange(val);

    // Clear pending debounce
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (val.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    debounceRef.current = setTimeout(() => {
      fetchSuggestions(val);
    }, 300);
  }

  // Cancel pending debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  function selectPlayer(player: Suggestion) {
    onChange(player.name);
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
    // Delay so click on a suggestion fires before hiding
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

      {/* Suggestions dropdown */}
      {showSuggestions && (
        <ul className="absolute top-full left-0 right-0 mt-2 p-0 m-0 list-none bg-[#2a2a2a] border border-[var(--color-primary)] rounded-lg max-h-[200px] overflow-y-auto z-10 shadow-[0_4px_6px_rgba(0,0,0,0.3)] animate-slide-down">
          {suggestions.map((player) => (
            <li key={player.id}>
              <button
                onMouseDown={() => selectPlayer(player)} // mousedown fires before blur
                className="w-full text-left px-4 py-3 bg-transparent border-none text-white cursor-pointer flex justify-between items-center hover:bg-[#374151] transition-colors"
              >
                <span className="font-medium">{player.name}</span>
                {player.nationality && (
                  <span className="text-[#9ca3af] text-sm" title={player.nationality}>
                    {getFlagEmoji(player.nationality)}
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
