"use client";

import { Frown, RotateCw, Inbox } from "lucide-react";
import clsx from "clsx";

/** Grid of shimmering card placeholders shown while a list loads. */
export function CardSkeletonGrid({ count = 8, square = false }: { count?: number; square?: boolean }) {
  return (
    <div className="grid grid-cols-2 gap-x-3 gap-y-5">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i}>
          <div className={clsx("skeleton rounded-card", square ? "aspect-square" : "aspect-video")} />
          <div className="mt-2.5 space-y-1.5">
            <div className="skeleton h-3.5 w-4/5 rounded" />
            <div className="skeleton h-3 w-2/5 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <div className="mx-5 flex flex-col items-center justify-center gap-3 rounded-card border border-border bg-surface py-14 text-center">
      <Frown className="h-9 w-9 text-inactive" />
      <p className="text-sm text-inactive">{message ?? "콘텐츠를 불러오지 못했습니다."}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-1 flex items-center gap-2 rounded-lg border border-border px-4 py-2 text-sm font-medium text-active active:bg-card"
        >
          <RotateCw className="h-4 w-4" />
          다시 시도
        </button>
      )}
    </div>
  );
}

export function EmptyState({ message }: { message?: string }) {
  return (
    <div className="mx-5 flex flex-col items-center justify-center gap-3 rounded-card border border-border bg-surface py-14 text-center">
      <Inbox className="h-9 w-9 text-inactive" />
      <p className="text-sm text-inactive">{message ?? "표시할 콘텐츠가 없습니다."}</p>
    </div>
  );
}
