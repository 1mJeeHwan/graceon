"use client";

import { useRef, useState } from "react";
import { useSession } from "next-auth/react";
import { useQuery } from "@tanstack/react-query";
import {
  ChevronLeft,
  ChevronRight,
  Copy,
  Loader2,
  Trash2,
  Upload,
} from "lucide-react";

import {
  mediaCategories,
  mediaDelete,
  mediaList,
  mediaUpload,
  type MediaAssetDto,
} from "@/apis/media";
import { canWrite } from "@/lib/auth-utils";
import { SUCCESS_CODE } from "@/types/api";

const PAGE_SIZE = 24;

const FIELD_CLASS =
  "rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

function formatSize(bytes: number | null): string {
  if (!bytes || bytes <= 0) {
    return "외부 링크";
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(0)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value: string): string {
  return value.replace("T", " ").slice(0, 16);
}

export default function MediaLibraryPage() {
  const { data: session } = useSession();
  const writable = canWrite(session?.user?.role);

  const [category, setCategory] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [uploadCategory, setUploadCategory] = useState("general");
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const listQuery = useQuery({
    queryKey: ["media-list", category, keyword, page],
    queryFn: ({ signal }) =>
      mediaList(
        {
          category: category || undefined,
          keyword: keyword || undefined,
          pageNumber: page,
          pageSize: PAGE_SIZE,
        },
        signal,
      ),
  });

  const categoriesQuery = useQuery({
    queryKey: ["media-categories"],
    queryFn: ({ signal }) => mediaCategories(signal),
  });

  const assets: MediaAssetDto[] = listQuery.data?.resultObject?.contents ?? [];
  const totalCount = listQuery.data?.resultObject?.totalCount ?? 0;
  const totalPage = listQuery.data?.resultObject?.totalPage ?? 0;
  const categories = categoriesQuery.data?.resultObject ?? [];

  const applySearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setKeyword(keywordInput.trim());
  };

  const handleUploadClick = () => fileInputRef.current?.click();

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }
    setMessage(null);
    setUploading(true);
    let ok = 0;
    try {
      for (const file of Array.from(files)) {
        if (!file.type.startsWith("image/")) {
          continue;
        }
        await mediaUpload(file, uploadCategory.trim() || "general");
        ok += 1;
      }
      setMessage(ok > 0 ? `${ok}개 이미지를 업로드했습니다.` : "이미지 파일만 업로드할 수 있습니다.");
      setPage(0);
      await listQuery.refetch();
      await categoriesQuery.refetch();
    } catch {
      setMessage("업로드 중 오류가 발생했습니다.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleCopy = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url);
      setMessage("이미지 URL을 복사했습니다.");
    } catch {
      setMessage("URL 복사에 실패했습니다.");
    }
  };

  const handleDelete = async (asset: MediaAssetDto) => {
    if (!window.confirm(`'${asset.originalName ?? "이미지"}'을(를) 삭제하시겠습니까?`)) {
      return;
    }
    setMessage(null);
    try {
      const res = await mediaDelete(asset.id);
      if (res.resultCode === SUCCESS_CODE) {
        setMessage("삭제되었습니다.");
        await listQuery.refetch();
      } else {
        setMessage(res.resultMessage ?? "삭제에 실패했습니다.");
      }
    } catch {
      setMessage("삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-900">미디어 라이브러리</h1>
          <p className="mt-1 text-sm text-slate-500">
            업로드한 이미지를 한 곳에서 관리합니다. 배너·공지·이벤트 본문에서 URL을 복사해 사용하세요.
          </p>
        </div>
        {writable && (
          <div className="flex items-center gap-2">
            <input
              value={uploadCategory}
              onChange={(event) => setUploadCategory(event.target.value)}
              placeholder="카테고리"
              className={`${FIELD_CLASS} w-28`}
              aria-label="업로드 카테고리"
            />
            <button
              type="button"
              onClick={handleUploadClick}
              disabled={uploading}
              className="inline-flex items-center gap-1.5 rounded-md bg-brand px-3 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
            >
              {uploading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Upload className="h-4 w-4" />
              )}
              이미지 업로드
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              hidden
              onChange={(event) => void handleFiles(event.target.files)}
            />
          </div>
        )}
      </header>

      <div className="flex flex-wrap items-center gap-2">
        <select
          value={category}
          onChange={(event) => {
            setPage(0);
            setCategory(event.target.value);
          }}
          className={FIELD_CLASS}
          aria-label="카테고리 필터"
        >
          <option value="">전체 카테고리</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <form onSubmit={applySearch} className="flex items-center gap-2">
          <input
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            placeholder="파일명 검색"
            className={`${FIELD_CLASS} w-48`}
          />
          <button
            type="submit"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            검색
          </button>
        </form>
        <span className="ml-auto text-sm text-slate-500">
          총 <span className="font-semibold text-slate-800">{totalCount}</span>개
        </span>
      </div>

      {message && (
        <div className="rounded-md bg-brand/10 px-3 py-2 text-sm text-brand">{message}</div>
      )}

      {listQuery.isLoading ? (
        <div className="flex items-center justify-center py-20 text-slate-400">
          <Loader2 className="h-6 w-6 animate-spin" />
        </div>
      ) : assets.length === 0 ? (
        <div className="rounded-lg border border-dashed border-slate-300 py-20 text-center text-sm text-slate-400">
          이미지가 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
          {assets.map((asset) => (
            <div
              key={asset.id}
              className="group overflow-hidden rounded-lg border border-slate-200 bg-white"
            >
              <div className="relative aspect-square bg-slate-100">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={asset.url}
                  alt={asset.originalName ?? ""}
                  loading="lazy"
                  className="h-full w-full object-cover"
                />
                <div className="absolute inset-x-0 bottom-0 flex justify-end gap-1 bg-gradient-to-t from-black/55 to-transparent p-1.5 opacity-0 transition-opacity group-hover:opacity-100">
                  <button
                    type="button"
                    onClick={() => void handleCopy(asset.url)}
                    title="URL 복사"
                    className="rounded bg-white/90 p-1.5 text-slate-700 hover:bg-white"
                  >
                    <Copy className="h-3.5 w-3.5" />
                  </button>
                  {writable && (
                    <button
                      type="button"
                      onClick={() => void handleDelete(asset)}
                      title="삭제"
                      className="rounded bg-white/90 p-1.5 text-rose-600 hover:bg-white"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              </div>
              <div className="space-y-1 p-2">
                <p className="truncate text-xs font-medium text-slate-800" title={asset.originalName ?? ""}>
                  {asset.originalName ?? "(이름 없음)"}
                </p>
                <div className="flex items-center justify-between text-[11px] text-slate-400">
                  <span className="rounded bg-slate-100 px-1.5 py-0.5 text-slate-500">
                    {asset.category}
                  </span>
                  <span>{formatSize(asset.sizeBytes)}</span>
                </div>
                <p className="text-[11px] text-slate-400">{formatDate(asset.createdAt)}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPage > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            disabled={page === 0}
            className="rounded-md border border-slate-300 p-1.5 text-slate-600 disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-slate-600">
            {page + 1} / {totalPage}
          </span>
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(p + 1, totalPage - 1))}
            disabled={page >= totalPage - 1}
            className="rounded-md border border-slate-300 p-1.5 text-slate-600 disabled:opacity-40"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
