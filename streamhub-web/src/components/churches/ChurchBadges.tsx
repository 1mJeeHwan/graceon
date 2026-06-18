import { DENOMINATION_LABELS } from "@/lib/church-form";

const BADGE_BASE =
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium";

interface DenominationBadgeProps {
  value?: string;
}

/**
 * DenominationBadge renders a neutral pill with the Korean denomination label,
 * falling back to the raw code for unknown values.
 */
export function DenominationBadge({ value }: DenominationBadgeProps) {
  const label = value ? DENOMINATION_LABELS[value] ?? value : "-";
  return (
    <span className={`${BADGE_BASE} bg-indigo-100 text-indigo-700`}>
      {label}
    </span>
  );
}

interface OpenBadgeProps {
  value?: string;
}

/**
 * OpenBadge renders a green 공개 pill when openYn is "Y"; otherwise 비공개 (slate).
 */
export function OpenBadge({ value }: OpenBadgeProps) {
  const isOpen = value === "Y";
  return (
    <span
      className={`${BADGE_BASE} ${
        isOpen ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"
      }`}
    >
      {isOpen ? "공개" : "비공개"}
    </span>
  );
}

interface UseYnBadgeProps {
  value?: string;
}

/**
 * UseYnBadge renders 사용 (green) when useYn is not "N"; otherwise 미사용 (slate).
 */
export function UseYnBadge({ value }: UseYnBadgeProps) {
  const isUsed = value !== "N";
  return (
    <span
      className={`${BADGE_BASE} ${
        isUsed ? "bg-blue-100 text-blue-700" : "bg-slate-100 text-slate-500"
      }`}
    >
      {isUsed ? "사용" : "미사용"}
    </span>
  );
}
