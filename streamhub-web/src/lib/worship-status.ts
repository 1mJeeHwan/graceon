/**
 * Worship/new-family registration status presentation + transition rules for the
 * worship admin screens.
 *
 * The authoritative state machine lives in the backend
 * (`WorshipService` status transitions). `ALLOWED_TRANSITIONS` here is a UX-only
 * mirror used to render the right transition buttons — any illegal transition the
 * frontend might still send is rejected by the backend.
 */

/** Canonical worship registration status keys (matches backend enum). */
export type WorshipStatus = "RECEIVED" | "CONTACTED" | "COMPLETED" | "CANCELED";

/** Korean labels for each status. */
export const STATUS_LABEL: Record<WorshipStatus, string> = {
  RECEIVED: "접수",
  CONTACTED: "연락완료",
  COMPLETED: "등록완료",
  CANCELED: "취소",
};

/** Tailwind badge classes for each status. */
export const STATUS_COLOR: Record<WorshipStatus, string> = {
  RECEIVED: "bg-amber-100 text-amber-700",
  CONTACTED: "bg-blue-100 text-blue-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  CANCELED: "bg-red-100 text-red-700",
};

/**
 * Ordered "happy path" for the stepper visualization. The branch state
 * (CANCELED) is not part of the linear flow and is shown separately.
 */
export const STATUS_FLOW: WorshipStatus[] = [
  "RECEIVED",
  "CONTACTED",
  "COMPLETED",
];

/** Allowed forward/branch transitions per status (UX guard only). */
export const ALLOWED_TRANSITIONS: Record<WorshipStatus, WorshipStatus[]> = {
  RECEIVED: ["CONTACTED", "CANCELED"],
  CONTACTED: ["COMPLETED", "CANCELED"],
  COMPLETED: [],
  CANCELED: [],
};

/** Destructive transitions that require an explicit confirmation modal. */
export const DESTRUCTIVE_TRANSITIONS: WorshipStatus[] = ["CANCELED"];

/** True when the transition is destructive (CANCELED) and needs a modal. */
export function isDestructive(status: WorshipStatus): boolean {
  return DESTRUCTIVE_TRANSITIONS.includes(status);
}

/** Gender labels. */
export const GENDER_LABEL: Record<string, string> = {
  MALE: "남",
  FEMALE: "여",
};

/** Register department labels. */
export const REGISTER_DEPT_LABEL: Record<string, string> = {
  INFANT: "영아부",
  CHILDREN: "유년부",
  YOUTH: "청소년부",
  YOUNG_ADULT: "청년부",
  ADULT: "장년부",
  SENIOR: "노년부",
};

/** Baptism type labels. */
export const BAPTISM_TYPE_LABEL: Record<string, string> = {
  NONE: "없음",
  BAPTISM: "세례",
  CONFIRMATION: "입교",
  INFANT_BAPTISM: "유아세례",
};

/** Resolve a label from a map, falling back to the raw value or "-". */
export function labelOf(
  map: Record<string, string>,
  value?: string | null,
): string {
  if (!value) {
    return "-";
  }
  return map[value] ?? value;
}
