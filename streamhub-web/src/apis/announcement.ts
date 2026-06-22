/**
 * Hand-written client for the site announcement config (/v1/announcement). Ships after the current
 * Orval spec, so it's issued directly through `customInstance` (same auth/refresh as generated
 * hooks). Replaceable by `npm run gen` post-deploy.
 */
import { customInstance } from "./custom-instance";
import type { ApiResponse } from "@/types/api";

/** Announcement config (mirrors org.streamhub.api.v1.announcement.dto.AnnouncementDto). */
export interface AnnouncementDto {
  enabled: boolean;
  text: string | null;
  linkUrl: string | null;
}

export const announcementGet = (signal?: AbortSignal) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: "/v1/announcement",
    method: "GET",
    signal,
  });

export const announcementSave = (data: AnnouncementDto) =>
  customInstance<ApiResponse<AnnouncementDto>>({
    url: "/v1/announcement",
    method: "PUT",
    data,
  });
