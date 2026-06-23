/**
 * Hand-written client for modal-ad announcements (/v1/announcement), managed like banners. Ships
 * after the current Orval spec, so it's issued directly through `customInstance` (same auth/refresh
 * as generated hooks). Replaceable by `npm run gen` post-deploy.
 */
import { customInstance } from "./custom-instance";
import type { ApiResponse } from "@/types/api";

/** Structured link target — reuses the banner link model. Null = uses linkUrl directly. */
export type AnnouncementLinkType = "VIDEO" | "MUSIC" | "POST" | "URL";

/** A modal-ad announcement row (mirrors org.streamhub.api.v1.announcement.dto.AnnouncementDto). */
export interface AnnouncementDto {
  id?: number;
  title: string;
  imageUrl?: string | null;
  /** Request: raw URL for URL/legacy. Response: resolved click target. */
  linkUrl?: string | null;
  linkType?: AnnouncementLinkType | null;
  linkRefId?: number | null;
  linkLabel?: string | null;
  startAt?: string | null;
  endAt?: string | null;
  sortOrder: number;
  enabled: boolean;
  createdAt?: string | null;
}

export interface AnnouncementSearchRequest {
  enabled?: boolean;
}

export const announcementList = (req: AnnouncementSearchRequest, signal?: AbortSignal) =>
  customInstance<ApiResponse<AnnouncementDto[]>>({
    url: "/v1/announcement/list",
    method: "POST",
    data: req,
    signal,
  });

export const announcementDetail = (id: number, signal?: AbortSignal) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: `/v1/announcement/${id}`,
    method: "GET",
    signal,
  });

export const announcementCreate = (data: AnnouncementDto) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: "/v1/announcement",
    method: "POST",
    data,
  });

export const announcementUpdate = (id: number, data: AnnouncementDto) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: `/v1/announcement/${id}`,
    method: "PUT",
    data,
  });

export const announcementUpdateSort = (id: number, sortOrder: number) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: `/v1/announcement/${id}/sort`,
    method: "PUT",
    data: { sortOrder },
  });

export const announcementDelete = (id: number) =>
  customInstance<ApiResponse<void>>({
    url: `/v1/announcement/${id}`,
    method: "DELETE",
  });
