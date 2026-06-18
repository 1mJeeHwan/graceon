/** Shared helpers + Zod schema for the store create/edit form. */

import { z } from "zod";

import type { StoreDto } from "@/apis/query/streamHubAdminAPI.schemas";

/** Common input styling shared across the store form (mirrors goods FIELD_CLASS). */
export const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

/** Optional latitude/longitude validator: blank passes, otherwise must be numeric. */
const coordSchema = z
  .string()
  .optional()
  .refine(
    (value) => value == null || value.trim() === "" || Number.isFinite(Number(value)),
    "숫자를 입력하세요.",
  );

/**
 * storeFormSchema validates the store create/edit form. Numeric fields are kept
 * as strings (native input values) and coerced at submit time via buildPayload.
 */
export const storeFormSchema = z.object({
  name: z.string().min(1, "매장명을 입력하세요."),
  regionId: z.string().optional(),
  address: z.string().optional(),
  phone: z.string().optional(),
  lat: coordSchema,
  lng: coordSchema,
  openHours: z.string().optional(),
  useYn: z.enum(["Y", "N"]),
});

export type StoreFormValues = z.infer<typeof storeFormSchema>;

/** Converts an optional string input to a finite number, or undefined when blank. */
function toNumber(value?: string): number | undefined {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

/** Builds default form values from an existing store (or an empty form). */
export function buildStoreDefaults(store?: StoreDto): StoreFormValues {
  return {
    name: store?.name ?? "",
    regionId: store?.regionId != null ? String(store.regionId) : "",
    address: store?.address ?? "",
    phone: store?.phone ?? "",
    lat: store?.lat != null ? String(store.lat) : "",
    lng: store?.lng != null ? String(store.lng) : "",
    openHours: store?.openHours ?? "",
    useYn: store?.useYn === "N" ? "N" : "Y",
  };
}

/** Builds the API payload from validated form values (id supplied separately). */
export function buildStorePayload(values: StoreFormValues): StoreDto {
  return {
    name: values.name.trim(),
    regionId: toNumber(values.regionId),
    address: values.address?.trim() || undefined,
    phone: values.phone?.trim() || undefined,
    lat: toNumber(values.lat),
    lng: toNumber(values.lng),
    openHours: values.openHours?.trim() || undefined,
    useYn: values.useYn,
  };
}
