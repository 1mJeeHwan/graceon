/** Shared helpers + Zod schema for the album create/edit forms (add + [id]). */

import { z } from "zod";

import {
  AlbumCreateRequestGenre,
  AlbumCreateRequestStatus,
  type AlbumCreateRequest,
  type AlbumDetail,
} from "@/apis/query/graceOnAdminAPI.schemas";

/** Common input styling shared across the album forms (mirrors goods FIELD_CLASS). */
export const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

/** Korean labels for each album genre (used in selects + list/detail display). */
export const GENRE_LABELS: Record<string, string> = {
  WORSHIP: "워십",
  HYMN: "찬송가",
  GOSPEL: "복음성가",
  CCM: "CCM",
  CAROL: "캐롤",
  INSTRUMENTAL: "연주곡",
  KIDS: "키즈",
};

/** Selectable genre options in form order. */
export const GENRE_OPTIONS = Object.values(AlbumCreateRequestGenre).map(
  (value) => ({ value, label: GENRE_LABELS[value] ?? value }),
);

/** Korean labels for album sale status. */
export const STATUS_LABELS: Record<string, string> = {
  ON_SALE: "판매중",
  HIDDEN: "숨김",
};

/** Track row in the dynamic form (string-typed for RHF inputs). */
const trackRowSchema = z.object({
  title: z.string().min(1, "트랙명을 입력하세요."),
  durationSec: z.string().optional(),
  previewUrl: z.string().optional(),
});

/**
 * albumFormSchema validates the album create/edit form. Numeric fields are kept
 * as strings (native input values) and coerced at submit time via buildPayload.
 */
export const albumFormSchema = z.object({
  title: z.string().min(1, "앨범명을 입력하세요."),
  artist: z.string().min(1, "아티스트를 입력하세요."),
  label: z.string().optional(),
  genre: z.enum([
    "WORSHIP",
    "HYMN",
    "GOSPEL",
    "CCM",
    "CAROL",
    "INSTRUMENTAL",
    "KIDS",
  ]),
  releaseDate: z.string().optional(),
  description: z.string().optional(),
  status: z.enum(["ON_SALE", "HIDDEN"]),
  price: z
    .string()
    .min(1, "가격을 입력하세요.")
    .refine((value) => Number(value) >= 0, "가격은 0 이상이어야 합니다."),
  stock: z.string().optional(),
  tracks: z.array(trackRowSchema),
});

export type AlbumFormValues = z.infer<typeof albumFormSchema>;
export type AlbumTrackRow = z.infer<typeof trackRowSchema>;

/** Converts an optional string input to a finite number, or undefined when blank. */
function toNumber(value?: string): number | undefined {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

/** Builds default form values from an existing detail (or an empty form). */
export function buildAlbumDefaults(detail?: AlbumDetail): AlbumFormValues {
  return {
    title: detail?.title ?? "",
    artist: detail?.artist ?? "",
    label: detail?.label ?? "",
    genre: (detail?.genre as AlbumFormValues["genre"]) ?? "WORSHIP",
    releaseDate: detail?.releaseDate ?? "",
    description: detail?.description ?? "",
    status: detail?.status === "HIDDEN" ? "HIDDEN" : "ON_SALE",
    price: detail?.price != null ? String(detail.price) : "",
    stock: detail?.stock != null ? String(detail.stock) : "",
    tracks: (detail?.tracks ?? []).map((track) => ({
      title: track.title ?? "",
      durationSec: track.durationSec != null ? String(track.durationSec) : "",
      previewUrl: track.previewUrl ?? "",
    })),
  };
}

/** Builds the API payload from validated form values + the chosen cover key. */
export function buildAlbumPayload(
  values: AlbumFormValues,
  coverKey?: string,
): AlbumCreateRequest {
  return {
    title: values.title.trim(),
    artist: values.artist.trim(),
    label: values.label?.trim() || undefined,
    genre: values.genre as AlbumCreateRequest["genre"],
    releaseDate: values.releaseDate?.trim() || undefined,
    description: values.description?.trim() || undefined,
    coverKey: coverKey || undefined,
    status: values.status as AlbumCreateRequestStatus,
    price: toNumber(values.price) ?? 0,
    stock: toNumber(values.stock),
    tracks: values.tracks.map((track, index) => ({
      trackNo: index + 1,
      title: track.title.trim(),
      durationSec: toNumber(track.durationSec),
      previewUrl: track.previewUrl?.trim() || undefined,
    })),
  };
}
