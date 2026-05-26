"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getFlagEmoji } from "@/utils/country";

interface EntitySuggestion {
  id: string;
  name: string;
  nationality: string;
}

interface EntitySearchProps {
  entityType?: string;
  onSelect: (value: string) => void;
  placeholder?: string;
  className?: string;
}

export default function EntitySearch({
  entityType = "footballer",
  onSelect,
  placeholder = "Enter answer...",
  className = "",
}: EntitySearchProps) {
  const [value, setValue] = useState("");
  const [suggestions, setSuggestions] = useState<EntitySuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
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

    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (val.length < 3) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    debounceRef.current = setTimeout(() => {
      fetchSuggestions(val);
    }, 200);
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      if (showSuggestions && activeIndex >= 0) {
        selectEntity(suggestions[activeIndex]);
      } else {
        onSelect(value);
        setValue("");
        setShowSuggestions(false);
      }
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : 0));
    } else if (e.key === "Escape") {
      setShowSuggestions(false);
    }
  }

  function selectEntity(entity: EntitySuggestion) {
    onSelect(entity.name);
    setValue("");
    setSuggestions([]);
    setShowSuggestions(false);
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
        className={className}
      />
      
      {showSuggestions && (
        <ul className="ta-list absolute bottom-full left-0 right-0 mb-2 p-0 m-0 list-none bg-black border-2 border-white overflow-hidden z-50 shadow-[0_-12px_32px_rgba(0,0,0,0.5)]">
          {suggestions.map((entity, idx) => (
            <li key={entity.id}>
              <button
                onMouseDown={() => selectEntity(entity)}
                data-active={activeIndex === idx ? "1" : "0"}
                className={`ta-opt w-full text-left grid grid-cols-[1fr_auto] gap-4 items-center px-5.5 py-3 bg-transparent border-0 border-b border-white border-dashed last:border-b-0 cursor-pointer transition-colors ${
                  activeIndex === idx ? 'bg-[#00008c]' : 'hover:bg-[#00008c]'
                }`}
              >
                <span className="ta-opt-name text-white text-[22px] tracking-wide">
                  {entity.name.toUpperCase()}
                </span>
                {entity.nationality && (
                  <span className="ta-opt-club text-tele-cyan text-[18px]">
                    {getFlagEmoji(entity.nationality)} {entity.nationality}
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
