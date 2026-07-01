"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { Loader2, Plus } from "lucide-react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { albumList } from "@/apis/query/album/album";
import {
  AlbumSearchRequestGenre,
  AlbumSearchRequestStatus,
  type AlbumListItem,
  type AlbumSearchRequest,
} from "@/apis/query/graceOnAdminAPI.schemas";
import { GENRE_LABELS, STATUS_LABELS } from "@/lib/album-form";

const AlbumGrid = dynamic(() => import("@/components/albums/AlbumGrid"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[560px] items-center justify-center rounded-md border border-slate-200 bg-white">
      <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
    </div>
  ),
});

const PAGE_SIZE = 10;

type GenreFilter = "ALL" | AlbumSearchRequestGenre;
type StatusFilter = "ALL" | AlbumSearchRequestStatus;

const GENRE_OPTIONS: { value: GenreFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  ...Object.values(AlbumSearchRequestGenre).map((value) => ({
    value,
    label: GENRE_LABELS[value] ?? value,
  })),
];

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  ...Object.values(AlbumSearchRequestStatus).map((value) => ({
    value,
    label: STATUS_LABELS[value] ?? value,
  })),
];

export default function AlbumsPage() {
  // Committed search criteria (applied on 검색 / page change).
  const [keyword, setKeyword] = useState("");
  const [genre, setGenre] = useState<GenreFilter>("ALL");
  const [status, setStatus] = useState<StatusFilter>("ALL");
  const [pageNumber, setPageNumber] = useState(1);

  // Draft inputs (not yet applied to the query).
  const [keywordDraft, setKeywordDraft] = useState("");
  const [genreDraft, setGenreDraft] = useState<GenreFilter>("ALL");
  const [statusDraft, setStatusDraft] = useState<StatusFilter>("ALL");

  const searchRequest = useMemo<AlbumSearchRequest>(
    () => ({
      // UI pageNumber is 1-based (for display); the backend expects 0-based.
      pageNumber: pageNumber - 1,
      pageSize: PAGE_SIZE,
      keyword: keyword.trim() || undefined,
      genre: genre === "ALL" ? undefined : genre,
      status: status === "ALL" ? undefined : status,
    }),
    [pageNumber, keyword, genre, status],
  );

  // List is a POST search, but it's a read — model it as a cached query keyed by
  // the criteria so page/filter changes refetch and prior results stay visible.
  const listQuery = useQuery({
    queryKey: ["album-list", searchRequest],
    queryFn: ({ signal }) => albumList(searchRequest, signal),
    placeholderData: keepPreviousData,
  });

  const result = listQuery.data?.resultObject;
  const rows: AlbumListItem[] = result?.contents ?? [];
  const totalCount = result?.totalCount ?? 0;
  const totalPage = result?.totalPage ?? 0;

  const handleSearch = () => {
    setKeyword(keywordDraft);
    setGenre(genreDraft);
    setStatus(statusDraft);
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
          <h1 className="text-xl font-semibold text-slate-900">앨범관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            앨범을 조회하고 트랙리스트 / 가격 / 노출상태를 관리합니다.
          </p>
        </div>
        <Link
          href="/albums/add"
          className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          <Plus className="h-4 w-4" />
          앨범 등록
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
            placeholder="앨범명 / 아티스트"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="genre"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            장르
          </label>
          <select
            id="genre"
            value={genreDraft}
            onChange={(event) => setGenreDraft(event.target.value as GenreFilter)}
            className="w-40 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            {GENRE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="status"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            상태
          </label>
          <select
            id="status"
            value={statusDraft}
            onChange={(event) =>
              setStatusDraft(event.target.value as StatusFilter)
            }
            className="w-32 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
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
        <AlbumGrid rows={rows} />
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
