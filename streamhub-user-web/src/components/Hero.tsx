"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Play } from "lucide-react";
import clsx from "clsx";
import type { ContentListItem } from "@/lib/types";

/**
 * Swipeable hero banner (scroll-snap, no carousel lib). Dots reflect/seek the active slide.
 * Thumbnails are usually absent, so each slide is a branded gradient with the title overlaid.
 */
export function Hero({ items }: { items: ContentListItem[] }) {
  const ref = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);
  const router = useRouter();
  const slides = items.slice(0, 5);

  function onScroll() {
    const el = ref.current;
    if (!el) return;
    const i = Math.round(el.scrollLeft / el.clientWidth);
    if (i !== active) setActive(i);
  }

  function seek(i: number) {
    const el = ref.current;
    if (!el) return;
    el.scrollTo({ left: i * el.clientWidth, behavior: "smooth" });
  }

  if (slides.length === 0) return null;

  return (
    <div className="relative">
      <div
        ref={ref}
        onScroll={onScroll}
        className="flex snap-x snap-mandatory overflow-x-auto [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {slides.map((item, i) => (
          <button
            key={item.id}
            onClick={() => router.push(`/video/${item.id}`)}
            className="relative aspect-[16/10] w-full shrink-0 snap-start overflow-hidden text-left"
            aria-label={item.title}
          >
            {item.thumbnailUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={item.thumbnailUrl} alt="" className="h-full w-full object-cover" />
            ) : (
              <div
                className={clsx(
                  "h-full w-full",
                  i % 2 === 0
                    ? "bg-gradient-to-br from-primary/40 via-surface to-bg"
                    : "bg-gradient-to-br from-secondary/40 via-surface to-bg",
                )}
              />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/10 to-transparent" />
            <div className="absolute inset-x-0 bottom-0 p-5">
              <span className="mb-2 inline-flex items-center gap-1 rounded-full bg-primary/90 px-2 py-0.5 text-[11px] font-bold text-bg">
                <Play className="h-3 w-3 fill-bg" />
                지금 보기
              </span>
              <h2 className="ellipsis-2 text-xl font-bold leading-tight text-white drop-shadow">
                {item.title}
              </h2>
              {item.channelName && (
                <p className="mt-1 text-xs text-white/80">{item.channelName}</p>
              )}
            </div>
          </button>
        ))}
      </div>

      <div className="absolute inset-x-0 bottom-3 flex justify-center gap-1.5">
        {slides.map((_, i) => (
          <button
            key={i}
            onClick={() => seek(i)}
            aria-label={`${i + 1}번째 배너`}
            className={clsx(
              "h-1.5 rounded-full transition-all",
              i === active ? "w-4 bg-white" : "w-1.5 bg-white/50",
            )}
          />
        ))}
      </div>
    </div>
  );
}
