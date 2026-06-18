import {
  STATUS_COLOR,
  STATUS_LABEL,
  type WorshipStatus,
} from "@/lib/worship-status";

interface WorshipStatusBadgeProps {
  status?: string;
}

/**
 * WorshipStatusBadge renders a colored pill for one of the 4 worship
 * registration statuses. Unknown values fall back to a neutral badge.
 */
export default function WorshipStatusBadge({
  status,
}: WorshipStatusBadgeProps) {
  const key = status as WorshipStatus | undefined;
  const label = (key && STATUS_LABEL[key]) ?? status ?? "-";
  const className =
    (key && STATUS_COLOR[key]) ?? "bg-slate-200 text-slate-600";

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}
    >
      {label}
    </span>
  );
}
