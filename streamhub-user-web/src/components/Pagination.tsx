"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import clsx from "clsx";

/** Prev/next pager. pageNumber is 0-based (matches the backend). */
export function Pagination({
  pageNumber,
  totalPage,
  onChange,
}: {
  pageNumber: number;
  totalPage: number;
  onChange: (page: number) => void;
}) {
  if (totalPage <= 1) return null;
  const canPrev = pageNumber > 0;
  const canNext = pageNumber < totalPage - 1;
  const btn = "grid h-10 w-10 place-items-center rounded-lg border border-border transition-colors";

  return (
    <div className="mt-7 flex items-center justify-center gap-3">
      <button
        onClick={() => canPrev && onChange(pageNumber - 1)}
        disabled={!canPrev}
        aria-label="이전 페이지"
        className={clsx(btn, canPrev ? "text-active active:bg-card" : "cursor-not-allowed text-inactive/40")}
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <span className="min-w-[4rem] text-center text-sm text-inactive">
        <span className="font-bold text-active">{pageNumber + 1}</span> / {totalPage}
      </span>
      <button
        onClick={() => canNext && onChange(pageNumber + 1)}
        disabled={!canNext}
        aria-label="다음 페이지"
        className={clsx(btn, canNext ? "text-active active:bg-card" : "cursor-not-allowed text-inactive/40")}
      >
        <ChevronRight className="h-5 w-5" />
      </button>
    </div>
  );
}
