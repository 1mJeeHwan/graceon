/** Shared helpers + Zod schema for the church create/edit forms (add + [id]). */

import { z } from "zod";

import {
  ChurchUpsertRequestDenomination,
  WorshipTimeDtoKind,
  type ChurchDetail,
  type ChurchUpsertRequest,
  type WorshipTimeDto,
} from "@/apis/query/graceOnAdminAPI.schemas";

/** Common input styling shared across the church forms (mirrors goods FIELD_CLASS). */
export const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

/** Fallback Korean labels for the denomination enum (used when the code/label API is unavailable). */
export const DENOMINATION_LABELS: Record<string, string> = {
  METHODIST: "감리교",
  PCK: "예장통합",
  HAPDONG: "예장합동",
  HOLINESS: "성결교",
  GOSPEL: "순복음",
  BAPTIST: "침례교",
  ETC: "기타",
};

/** Selectable denomination codes (order matches the backend enum). */
export const DENOMINATION_CODES = Object.values(
  ChurchUpsertRequestDenomination,
);

/** Korean labels for worship-time kinds. */
export const WORSHIP_KIND_LABELS: Record<string, string> = {
  SUNDAY: "주일예배",
  DAWN: "새벽예배",
  WEDNESDAY: "수요예배",
  FRIDAY: "금요예배",
  YOUTH: "청년예배",
  OTHER: "기타",
};

/** Selectable worship-time kinds (order matches the backend enum). */
export const WORSHIP_KIND_CODES = Object.values(WorshipTimeDtoKind);

/** Worship-time row in the dynamic form (string-typed for RHF inputs). */
const worshipRowSchema = z.object({
  kind: z.enum(["SUNDAY", "DAWN", "WEDNESDAY", "FRIDAY", "YOUTH", "OTHER"]),
  dayLabel: z.string().optional(),
  startTime: z.string().optional(),
  place: z.string().optional(),
  target: z.string().optional(),
});

/**
 * churchFormSchema validates the church create/edit form. Numeric fields stay as
 * strings (native input values) and are coerced at submit time via buildChurchPayload.
 */
export const churchFormSchema = z.object({
  name: z.string().min(1, "교회명을 입력하세요."),
  denomination: z.enum([
    "METHODIST",
    "PCK",
    "HAPDONG",
    "HOLINESS",
    "GOSPEL",
    "BAPTIST",
    "ETC",
  ]),
  regionId: z
    .string()
    .min(1, "지역 ID를 입력하세요.")
    .refine((value) => Number.isFinite(Number(value)), "지역 ID가 올바르지 않습니다."),
  latitude: z
    .string()
    .optional()
    .refine(
      (value) =>
        !value || (Number.isFinite(Number(value)) && Math.abs(Number(value)) <= 90),
      "위도는 -90 ~ 90 사이여야 합니다.",
    ),
  longitude: z
    .string()
    .optional()
    .refine(
      (value) =>
        !value || (Number.isFinite(Number(value)) && Math.abs(Number(value)) <= 180),
      "경도는 -180 ~ 180 사이여야 합니다.",
    ),
  zipcode: z.string().optional(),
  address: z.string().optional(),
  addressDetail: z.string().optional(),
  phone: z.string().optional(),
  pastorName: z.string().optional(),
  facilities: z.string().optional(),
  homepageUrl: z.string().optional(),
  introduction: z.string().optional(),
  openYn: z.enum(["Y", "N"]),
  useYn: z.enum(["Y", "N"]),
  worshipTimes: z.array(worshipRowSchema),
});

export type ChurchFormValues = z.infer<typeof churchFormSchema>;
export type WorshipTimeRow = z.infer<typeof worshipRowSchema>;

/** Converts an optional string input to a finite number, or undefined when blank. */
function toNumber(value?: string): number | undefined {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

/** Builds default form values from an existing detail (or an empty form). */
export function buildChurchDefaults(detail?: ChurchDetail): ChurchFormValues {
  return {
    name: detail?.name ?? "",
    denomination: detail?.denomination ?? "ETC",
    regionId: detail?.regionId != null ? String(detail.regionId) : "",
    latitude: detail?.latitude != null ? String(detail.latitude) : "",
    longitude: detail?.longitude != null ? String(detail.longitude) : "",
    zipcode: detail?.zipcode ?? "",
    address: detail?.address ?? "",
    addressDetail: detail?.addressDetail ?? "",
    phone: detail?.phone ?? "",
    pastorName: detail?.pastorName ?? "",
    facilities: detail?.facilities ?? "",
    homepageUrl: detail?.homepageUrl ?? "",
    introduction: detail?.introduction ?? "",
    openYn: detail?.openYn === "N" ? "N" : "Y",
    useYn: detail?.useYn === "N" ? "N" : "Y",
    worshipTimes: (detail?.worshipTimes ?? []).map((time) => ({
      kind: time.kind ?? "SUNDAY",
      dayLabel: time.dayLabel ?? "",
      startTime: time.startTime ?? "",
      place: time.place ?? "",
      target: time.target ?? "",
    })),
  };
}

/** Builds the API payload from validated form values + the chosen thumbnail key. */
export function buildChurchPayload(
  values: ChurchFormValues,
  thumbnailKey?: string,
): ChurchUpsertRequest {
  const worshipTimes: WorshipTimeDto[] = values.worshipTimes.map(
    (row, index) => ({
      kind: row.kind,
      dayLabel: row.dayLabel?.trim() || undefined,
      startTime: row.startTime?.trim() || undefined,
      place: row.place?.trim() || undefined,
      target: row.target?.trim() || undefined,
      sort: index,
    }),
  );

  return {
    name: values.name.trim(),
    denomination: values.denomination,
    regionId: Number(values.regionId),
    latitude: toNumber(values.latitude),
    longitude: toNumber(values.longitude),
    zipcode: values.zipcode?.trim() || undefined,
    address: values.address?.trim() || undefined,
    addressDetail: values.addressDetail?.trim() || undefined,
    phone: values.phone?.trim() || undefined,
    pastorName: values.pastorName?.trim() || undefined,
    facilities: values.facilities?.trim() || undefined,
    homepageUrl: values.homepageUrl?.trim() || undefined,
    introduction: values.introduction?.trim() || undefined,
    openYn: values.openYn,
    useYn: values.useYn,
    thumbnailKey: thumbnailKey || undefined,
    worshipTimes,
  };
}
