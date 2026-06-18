// Dynamic UI settings, served read-only by the backend (GET /pub/v1/site-config) and
// edited in the admin site. The user app consumes it for theme/accent/announcement/home
// layout. Types mirror org.streamhub.api.v1.siteconfig.dto.SiteConfigData.

import { useQuery } from "@tanstack/react-query";
import { query, request } from "./api";
import type { ResultDTO } from "./types";

export type SectionKey = "worshipLive" | "latestVideos" | "ccmAlbums" | "nearbyChurch";

export interface HomeSection {
  key: SectionKey | string;
  enabled: boolean;
}

export interface SiteConfig {
  defaultTheme: "dark" | "light";
  accentColor: string;
  announcement: { enabled: boolean; text: string; link: string };
  homeSections: HomeSection[];
  featuredAlbumIds: number[];
}

/** Safe fallback used when the API is unreachable or returns a malformed payload. */
export const DEFAULT_SITE_CONFIG: SiteConfig = {
  defaultTheme: "dark",
  accentColor: "#40C1DF",
  announcement: { enabled: false, text: "", link: "" },
  homeSections: [
    { key: "worshipLive", enabled: true },
    { key: "latestVideos", enabled: true },
    { key: "ccmAlbums", enabled: true },
    { key: "nearbyChurch", enabled: true },
  ],
  featuredAlbumIds: [],
};

const BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

/**
 * Server-side fetch (used in the root layout, no CORS). Never throws — returns defaults on any
 * failure so the app always renders. `no-store` so admin edits show on the next request.
 */
export async function fetchSiteConfig(): Promise<SiteConfig> {
  try {
    const res = await fetch(`${BASE}/pub/v1/site-config`, { cache: "no-store" });
    if (!res.ok) return DEFAULT_SITE_CONFIG;
    const envelope = (await res.json()) as ResultDTO<SiteConfig>;
    return normalize(envelope?.resultObject);
  } catch {
    return DEFAULT_SITE_CONFIG;
  }
}

/** Fills any missing fields with defaults so consumers can rely on the shape. */
export function normalize(c: Partial<SiteConfig> | null | undefined): SiteConfig {
  if (!c) return DEFAULT_SITE_CONFIG;
  return {
    defaultTheme: c.defaultTheme === "light" ? "light" : "dark",
    accentColor: typeof c.accentColor === "string" && c.accentColor ? c.accentColor : DEFAULT_SITE_CONFIG.accentColor,
    announcement: {
      enabled: !!c.announcement?.enabled,
      text: c.announcement?.text ?? "",
      link: c.announcement?.link ?? "",
    },
    homeSections:
      Array.isArray(c.homeSections) && c.homeSections.length > 0
        ? c.homeSections.map((s) => ({ key: s.key, enabled: !!s.enabled }))
        : DEFAULT_SITE_CONFIG.homeSections,
    featuredAlbumIds: Array.isArray(c.featuredAlbumIds) ? c.featuredAlbumIds : [],
  };
}

/**
 * Converts "#40C1DF" → "64 193 223" (space-separated RGB channels) for the Tailwind token
 * `--primary` (`rgb(var(--primary) / <alpha>)`). Returns null for malformed input.
 */
export function hexToRgbChannels(hex: string): string | null {
  const m = /^#?([0-9a-fA-F]{6})$/.exec(hex.trim());
  if (!m) return null;
  const n = parseInt(m[1], 16);
  return `${(n >> 16) & 255} ${(n >> 8) & 255} ${n & 255}`;
}

// --- client hooks (home page) ------------------------------------------------

/** Client-side site config (home rendering); falls back to defaults via `normalize`. */
export function useSiteConfig() {
  return useQuery({
    queryKey: ["site-config"],
    queryFn: () => request<SiteConfig>("/pub/v1/site-config"),
    select: (c) => normalize(c),
    staleTime: 60_000,
  });
}

/** A promotional banner (GET /pub/v1/banners). Mirrors org.streamhub.api.v1.banner.dto.BannerDto. */
export interface Banner {
  id: number;
  title: string;
  position: string;
  device: string;
  imageUrl: string;
  linkUrl: string | null;
  startAt: string | null;
  endAt: string | null;
  sortOrder: number;
  useYn: string;
  createdAt: string;
}

/** Active banners for a position (e.g. "MAIN_TOP"), already filtered/ordered by the server. */
export function useBanners(position?: string) {
  return useQuery({
    queryKey: ["banners", position ?? "all"],
    queryFn: () => request<Banner[]>(`/pub/v1/banners${query({ position })}`),
    staleTime: 60_000,
  });
}
