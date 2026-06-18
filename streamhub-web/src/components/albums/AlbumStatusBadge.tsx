import { GENRE_LABELS, STATUS_LABELS } from "@/lib/album-form";

const BADGE_BASE =
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium";
const NEUTRAL = "bg-slate-200 text-slate-600";

const STATUS_CLASS: Record<string, string> = {
  ON_SALE: "bg-emerald-100 text-emerald-700",
  HIDDEN: "bg-slate-200 text-slate-600",
};

interface AlbumStatusBadgeProps {
  value?: string;
}

/**
 * AlbumStatusBadge renders a colored pill for an album sale status
 * (판매중 / 숨김). Unknown values fall back to a neutral badge.
 */
export function AlbumStatusBadge({ value }: AlbumStatusBadgeProps) {
  const label = value ? (STATUS_LABELS[value] ?? value) : "-";
  const className = (value && STATUS_CLASS[value]) || NEUTRAL;
  return <span className={`${BADGE_BASE} ${className}`}>{label}</span>;
}

interface AlbumGenreBadgeProps {
  value?: string;
}

/** AlbumGenreBadge renders a neutral pill with the Korean genre label. */
export function AlbumGenreBadge({ value }: AlbumGenreBadgeProps) {
  const label = value ? (GENRE_LABELS[value] ?? value) : "-";
  return (
    <span className={`${BADGE_BASE} bg-indigo-50 text-indigo-700`}>{label}</span>
  );
}
