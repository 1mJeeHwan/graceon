// Public campaign / event domain — public surface for the user site.
// Types mirror the backend public DTOs (org.streamhub.api.v1.campaign). Kept in sync by hand
// (no codegen here, unlike the admin site), reusing the shared request/query helpers.

"use client";

import { useQuery } from "@tanstack/react-query";
import { query, request } from "./api";
import { fixImageUrl } from "./image";
import type { InfinityList } from "./types";

/** Known campaign lifecycle states. Unknown values fall back to the raw string. */
export type CampaignStatus = "SCHEDULED" | "ACTIVE" | "ENDED";

/** Korean labels for campaign status badges. Unknown statuses render the raw value. */
const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "예정",
  ACTIVE: "진행중",
  ENDED: "종료",
};

/** Status badge label: mapped Korean text, or the raw status when unmapped. */
export function campaignStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}

/** One row of the campaign list (GET /pub/v1/campaigns). */
export interface CampaignListItem {
  id: number;
  title: string;
  summary: string | null;
  imageUrl: string | null;
  status: string;
  startAt: string;
  endAt: string;
}

/** Full campaign detail (GET /pub/v1/campaigns/{id}). */
export interface CampaignDetail {
  id: number;
  title: string;
  summary: string | null;
  description: string | null;
  imageUrl: string | null;
  status: string;
  startAt: string;
  endAt: string;
}

export interface CampaignListParams {
  // Must match the backend paging fields (pageNumber/pageSize), same as AlbumSearchRequest.
  pageNumber?: number;
  pageSize?: number;
}

export const campaignApi = {
  list: (p: CampaignListParams = {}) =>
    request<InfinityList<CampaignListItem>>(`/pub/v1/campaigns${query({ ...p })}`).then((r) => ({
      ...r,
      contents: r.contents.map((c) => ({ ...c, imageUrl: fixImageUrl(c.imageUrl) })),
    })),
  detail: (id: number) =>
    request<CampaignDetail>(`/pub/v1/campaigns/${id}`).then((d) => ({ ...d, imageUrl: fixImageUrl(d.imageUrl) })),
};

export const campaignKeys = {
  list: (p: CampaignListParams) => ["campaigns", p] as const,
  detail: (id: number) => ["campaign", id] as const,
};

export function useCampaigns(params: CampaignListParams) {
  return useQuery({
    queryKey: campaignKeys.list(params),
    queryFn: () => campaignApi.list(params),
    placeholderData: (prev) => prev, // no flicker between pages
  });
}

export function useCampaign(id: number) {
  return useQuery({
    queryKey: campaignKeys.detail(id),
    queryFn: () => campaignApi.detail(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}
