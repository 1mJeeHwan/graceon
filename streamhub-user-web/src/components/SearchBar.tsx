"use client";

import { Search, X } from "lucide-react";

/** Controlled search input (caller debounces). */
export function SearchBar({
  value,
  onChange,
  placeholder = "검색어를 입력하세요",
  autoFocus = false,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  autoFocus?: boolean;
}) {
  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-inactive" />
      <input
        type="search"
        // eslint-disable-next-line jsx-a11y/no-autofocus
        autoFocus={autoFocus}
        enterKeyHint="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="input"
        aria-label="검색"
      />
      {value && (
        <button
          onClick={() => onChange("")}
          aria-label="지우기"
          className="absolute right-3 top-1/2 -translate-y-1/2 text-inactive active:text-active"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
