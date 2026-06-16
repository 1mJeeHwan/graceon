import { CalendarDays, Eye, Hash } from "lucide-react";
import { formatDate, formatViews } from "@/lib/format";

/** Shared meta block for content detail pages: views/date, hashtags, description. */
export function ContentMeta({
  viewCount,
  createdAt,
  hashtags,
  description,
}: {
  viewCount: number;
  createdAt: string;
  hashtags: string[];
  description: string | null;
}) {
  return (
    <>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-inactive">
        <span className="flex items-center gap-1.5">
          <Eye className="h-4 w-4" />
          조회 {formatViews(viewCount)}
        </span>
        <span className="flex items-center gap-1.5">
          <CalendarDays className="h-4 w-4" />
          {formatDate(createdAt)}
        </span>
      </div>

      {hashtags.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {hashtags.map((tag) => (
            <span
              key={tag}
              className="flex items-center gap-0.5 rounded-full border border-border bg-surface px-3 py-1 text-xs text-inactive"
            >
              <Hash className="h-3 w-3" />
              {tag}
            </span>
          ))}
        </div>
      )}

      {description && (
        <p className="mt-5 whitespace-pre-wrap rounded-card border border-border/70 bg-surface p-4 text-sm leading-relaxed text-active/90">
          {description}
        </p>
      )}
    </>
  );
}
