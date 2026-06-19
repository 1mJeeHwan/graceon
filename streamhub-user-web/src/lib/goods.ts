// Goods shop — public surface for the user site.
// Types mirror the backend public DTOs (org.streamhub.api.v1.goods). Kept in sync by hand
// (no codegen here, unlike the admin site), reusing the shared request/query helpers.

"use client";

import { useQuery } from "@tanstack/react-query";
import { query, request } from "./api";
import type { InfinityList } from "./types";

/** One row of the goods list (GET /pub/v1/goods). */
export interface GoodsListItem {
  id: number;
  name: string;
  price: number;
  thumbnailUrl: string | null;
  soldOut: boolean;
}

/** Full goods detail (GET /pub/v1/goods/{id}). */
export interface GoodsDetail {
  id: number;
  name: string;
  price: number;
  stock: number | null;
  soldOut: boolean;
  description: string | null;
  imageUrls: string[];
}

export interface GoodsListParams {
  keyword?: string;
  // Must match the backend search request fields (pageNumber/pageSize), like albums.
  pageNumber?: number;
  pageSize?: number;
}

export const goodsApi = {
  list: (p: GoodsListParams = {}) =>
    request<InfinityList<GoodsListItem>>(`/pub/v1/goods${query({ ...p })}`),
  detail: (id: number) => request<GoodsDetail>(`/pub/v1/goods/${id}`),
};

export const goodsKeys = {
  list: (p: GoodsListParams) => ["goods", p] as const,
  detail: (id: number) => ["goods-item", id] as const,
};

export function useGoods(params: GoodsListParams) {
  return useQuery({
    queryKey: goodsKeys.list(params),
    queryFn: () => goodsApi.list(params),
    placeholderData: (prev) => prev, // no flicker between pages/searches
  });
}

export function useGood(id: number) {
  return useQuery({
    queryKey: goodsKeys.detail(id),
    queryFn: () => goodsApi.detail(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}
