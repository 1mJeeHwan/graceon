"use client";

import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import { X } from "lucide-react";
import type { AnnouncementAd } from "@/lib/types";
import { safeHref } from "@/lib/url";

/** Remembers which announcement ids the visitor has dismissed (shows each ad once). */
const SEEN_KEY = "graceon.announcement.seen";

function readSeen(): number[] {
  try {
    const raw = window.localStorage.getItem(SEEN_KEY);
    return raw ? (JSON.parse(raw) as number[]) : [];
  } catch {
    return [];
  }
}

/**
 * Modal-ad announcement popup, driven by the admin-managed list. Shows each active ad once per
 * browser (sequentially, in display order): dismissing one advances to the next unseen ad. Portals
 * to <body> so its fixed overlay anchors to the viewport regardless of transformed ancestors.
 */
export function AnnouncementModal({ ads }: { ads: AnnouncementAd[] }) {
  const [mounted, setMounted] = useState(false);
  const [seen, setSeen] = useState<number[]>([]);

  useEffect(() => {
    setMounted(true);
    setSeen(readSeen());
  }, []);

  // First active ad the visitor hasn't dismissed yet (ads arrive already ordered + active).
  const current = useMemo(
    () => ads.find((ad) => !seen.includes(ad.id)) ?? null,
    [ads, seen],
  );

  if (!mounted || !current) {
    return null;
  }

  const dismiss = () => {
    const next = Array.from(new Set([...seen, current.id]));
    try {
      window.localStorage.setItem(SEEN_KEY, JSON.stringify(next));
    } catch {
      /* storage blocked — just advance in-memory */
    }
    setSeen(next);
  };

  const href = current.linkUrl ? safeHref(current.linkUrl) : null;

  return createPortal(
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 px-6"
      role="dialog"
      aria-modal="true"
      aria-label="공지"
      onClick={dismiss}
    >
      <div
        className="w-full max-w-[360px] overflow-hidden rounded-2xl border border-border bg-bg animate-fade-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-border bg-surface px-4 py-3">
          <h2 className="truncate text-sm font-bold text-active">{current.title}</h2>
          <button
            type="button"
            aria-label="닫기"
            onClick={dismiss}
            className="rounded-lg p-1.5 text-inactive transition active:bg-card"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {current.imageUrl ? (
          href ? (
            <Link href={href} onClick={dismiss} aria-label={current.title}>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={current.imageUrl} alt={current.title} className="block w-full" />
            </Link>
          ) : (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={current.imageUrl} alt={current.title} className="block w-full" />
          )
        ) : (
          <div className="px-5 py-7">
            <p className="text-center text-[15px] leading-relaxed text-active">{current.title}</p>
          </div>
        )}

        <div className="flex gap-2 border-t border-border bg-surface px-4 py-3">
          {href ? (
            <>
              <button
                type="button"
                onClick={dismiss}
                className="flex-1 rounded-xl border border-border py-2.5 text-sm font-medium text-inactive active:bg-card"
              >
                닫기
              </button>
              <Link href={href} onClick={dismiss} className="btn-primary flex-1 py-2.5 text-sm">
                자세히 보기
              </Link>
            </>
          ) : (
            <button type="button" onClick={dismiss} className="btn-primary w-full py-2.5 text-sm">
              확인
            </button>
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}
