"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { Loader2, Plus } from "lucide-react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import {
  churchChurchesList,
  useChurchChurchesDenominations,
} from "@/apis/query/church/church";
import {
  ChurchSearchRequestDenomination,
  type ChurchListItem,
  type ChurchSearchRequest,
} from "@/apis/query/graceOnAdminAPI.schemas";
import { DENOMINATION_LABELS } from "@/lib/church-form";

const ChurchGrid = dynamic(() => import("@/components/churches/ChurchGrid"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[560px] items-center justify-center rounded-md border border-slate-200 bg-white">
      <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
    </div>
  ),
});

const PAGE_SIZE = 10;

type DenominationFilter = "ALL" | ChurchSearchRequestDenomination;

export default function ChurchesPage() {
  // Committed search criteria (applied on 검색 / page change).
  const [keyword, setKeyword] = useState("");
  const [denomination, setDenomination] = useState<DenominationFilter>("ALL");
  const [regionId, setRegionId] = useState("");
  const [pageNumber, setPageNumber] = useState(1);

  // Draft inputs (not yet applied to the query).
  const [keywordDraft, setKeywordDraft] = useState("");
  const [denominationDraft, setDenominationDraft] =
    useState<DenominationFilter>("ALL");
  const [regionDraft, setRegionDraft] = useState("");

  // 교단 select options come from the backend code/label endpoint; fall back to
  // the static label map when the response has no usable entries.
  const denominationsQuery = useChurchChurchesDenominations();
  const denominationOptions = useMemo(() => {
    const fromApi = denominationsQuery.data?.resultObject ?? [];
    if (fromApi.length > 0) {
      return fromApi
        .filter((item): item is { code: string; label: string } =>
          Boolean(item.code),
        )
        .map((item) => ({
          code: item.code,
          label: item.label ?? DENOMINATION_LABELS[item.code] ?? item.code,
        }));
    }
    return Object.values(ChurchSearchRequestDenomination).map((code) => ({
      code,
      label: DENOMINATION_LABELS[code] ?? code,
    }));
  }, [denominationsQuery.data]);

  const searchRequest = useMemo<ChurchSearchRequest>(
    () => ({
      // UI pageNumber is 1-based (for display); the backend expects 0-based.
      pageNumber: pageNumber - 1,
      pageSize: PAGE_SIZE,
      keyword: keyword.trim() || undefined,
      denomination:
        denomination === "ALL"
          ? undefined
          : (denomination as ChurchSearchRequestDenomination),
      regionId: regionId.trim() ? Number(regionId) : undefined,
    }),
    [pageNumber, keyword, denomination, regionId],
  );

  // List is a POST search, but it's a read — model it as a cached query keyed by
  // the criteria so page/filter changes refetch and prior results stay visible.
  const listQuery = useQuery({
    queryKey: ["church-list", searchRequest],
    queryFn: ({ signal }) => churchChurchesList(searchRequest, signal),
    placeholderData: keepPreviousData,
  });

  const result = listQuery.data?.resultObject;
  const rows: ChurchListItem[] = result?.contents ?? [];
  const totalCount = result?.totalCount ?? 0;
  const totalPage = result?.totalPage ?? 0;

  const handleSearch = () => {
    setKeyword(keywordDraft);
    setDenomination(denominationDraft);
    setRegionId(regionDraft);
    setPageNumber(1);
  };

  const goToPage = (next: number) => {
    if (next < 1 || (totalPage > 0 && next > totalPage)) {
      return;
    }
    setPageNumber(next);
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">교회관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            교회를 조회하고 교단 / 좌표 / 예배시간을 관리합니다.
          </p>
        </div>
        <Link
          href="/churches/add"
          className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          <Plus className="h-4 w-4" />
          교회 등록
        </Link>
      </div>

      {/* Search / filter bar */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="keyword"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            검색어
          </label>
          <input
            id="keyword"
            type="text"
            value={keywordDraft}
            onChange={(event) => setKeywordDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                handleSearch();
              }
            }}
            placeholder="교회명 / 담임목사 / 주소"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="denomination"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            교단
          </label>
          <select
            id="denomination"
            value={denominationDraft}
            onChange={(event) =>
              setDenominationDraft(event.target.value as DenominationFilter)
            }
            className="w-40 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            <option value="ALL">전체</option>
            {denominationOptions.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="regionId"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            지역 ID
          </label>
          <input
            id="regionId"
            type="number"
            min={0}
            value={regionDraft}
            onChange={(event) => setRegionDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                handleSearch();
              }
            }}
            placeholder="예: 1100"
            className="w-32 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>

        <button
          type="button"
          onClick={handleSearch}
          className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          검색
        </button>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {totalCount.toLocaleString()}건
        </span>
      </div>

      {/* Results */}
      {listQuery.isLoading ? (
        <div className="flex h-[560px] items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError ? (
        <div className="flex h-[560px] items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <ChurchGrid rows={rows} />
      )}

      {/* Pagination */}
      {totalPage > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            type="button"
            onClick={() => goToPage(pageNumber - 1)}
            disabled={pageNumber <= 1}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          >
            이전
          </button>
          <span className="text-sm text-slate-600">
            {pageNumber} / {totalPage}
          </span>
          <button
            type="button"
            onClick={() => goToPage(pageNumber + 1)}
            disabled={pageNumber >= totalPage}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
