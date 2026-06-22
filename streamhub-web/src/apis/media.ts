/**
 * Hand-written client for the media library (/v1/media).
 *
 * These endpoints ship after the current Orval spec, so they are issued directly through
 * `customInstance`/`axiosInstance` (the same mutators the generated hooks use, so auth + token
 * refresh behave identically). Once the backend is deployed and `npm run gen` is run, generated
 * hooks can supersede this file.
 */
import { axiosInstance, customInstance } from "./custom-instance";
import type { ApiResponse } from "@/types/api";

/** One media-library asset (mirrors org.streamhub.api.v1.media.dto.MediaAssetDto). */
export interface MediaAssetDto {
  id: number;
  key: string;
  url: string;
  category: string;
  originalName: string | null;
  contentType: string | null;
  sizeBytes: number | null;
  createdAt: string;
}

/** Paginated list payload (mirrors ResInfinityList). */
export interface MediaListResponse {
  contents: MediaAssetDto[];
  totalCount: number;
  totalPage: number;
}

export interface MediaListParams {
  category?: string;
  keyword?: string;
  pageNumber?: number;
  pageSize?: number;
}

export const mediaList = (params: MediaListParams, signal?: AbortSignal) =>
  customInstance<ApiResponse<MediaListResponse>>({
    url: "/v1/media",
    method: "GET",
    params,
    signal,
  });

export const mediaCategories = (signal?: AbortSignal) =>
  customInstance<ApiResponse<string[]>>({
    url: "/v1/media/categories",
    method: "GET",
    signal,
  });

export const mediaDelete = (id: number) =>
  customInstance<ApiResponse<void>>({
    url: `/v1/media/${id}`,
    method: "DELETE",
  });

/**
 * Uploads one file as multipart form-data (the generated hook serializes multipart as JSON, which
 * the backend rejects — see apis/upload.ts for the same workaround). Returns the created asset.
 */
export async function mediaUpload(
  file: File,
  category?: string,
): Promise<MediaAssetDto | undefined> {
  const form = new FormData();
  form.append("file", file);
  const suffix = category ? `?category=${encodeURIComponent(category)}` : "";
  const res = await axiosInstance.post<ApiResponse<MediaAssetDto>>(
    `/v1/media/upload${suffix}`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return res.data.resultObject;
}
