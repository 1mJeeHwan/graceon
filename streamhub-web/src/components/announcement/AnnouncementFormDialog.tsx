"use client";

import { useEffect, useRef, useState } from "react";
import { Images, Loader2, Upload, X } from "lucide-react";

import { mediaList, mediaUpload, type MediaAssetDto } from "@/apis/media";
import {
  announcementCreate,
  announcementUpdate,
  type AnnouncementDto,
  type AnnouncementLinkType,
} from "@/apis/announcement";
import { contentList } from "@/apis/query/content/content";
import { ContentSearchRequestType } from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

/** Link target type. "" = no link. Content types store the referenced id (path resolved server-side). */
type LinkType = "" | AnnouncementLinkType;

const LINK_TYPE_LABELS: Record<LinkType, string> = {
  "": "없음",
  VIDEO: "영상",
  MUSIC: "음악",
  POST: "게시글",
  URL: "직접 URL",
};

interface LinkSearchResult {
  id: number;
  title: string;
}

const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

/**
 * isSafeLinkUrl accepts only an absolute http(s):// URL or a single-leading-slash internal path.
 * Rejects scheme-relative "//host" (open-redirect) and dangerous schemes (javascript:/data:/...)
 * before the value is stored and later rendered into an href on the public site.
 */
function isSafeLinkUrl(raw: string): boolean {
  const value = raw.trim();
  if (!value) {
    return false;
  }
  if (value.startsWith("/")) {
    return !value.startsWith("//");
  }
  try {
    const parsed = new URL(value);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}

interface AnnouncementFormDialogProps {
  /** Announcement being edited, or null/undefined when creating a new one. */
  announcement?: AnnouncementDto | null;
  onClose: () => void;
  /** Called after a successful create/update so the parent can refetch. */
  onSaved: () => void;
}

interface FormState {
  title: string;
  imageUrl: string;
  linkType: LinkType;
  linkRefId: number | null;
  linkLabel: string;
  linkUrl: string;
  startAt: string;
  endAt: string;
  sortOrder: string;
  enabled: boolean;
}

const buildFormState = (a?: AnnouncementDto | null): FormState => {
  // Edit: prefer the stored linkType; legacy rows (no type but a url) edit as a raw URL.
  const linkType: LinkType = a?.linkType ?? (a?.linkUrl ? "URL" : "");
  return {
    title: a?.title ?? "",
    imageUrl: a?.imageUrl ?? "",
    linkType,
    linkRefId: a?.linkRefId ?? null,
    linkLabel: a?.linkLabel ?? "",
    // For content types the response linkUrl is the resolved path — don't show it in the URL box.
    linkUrl: linkType === "URL" ? (a?.linkUrl ?? "") : "",
    startAt: a?.startAt ?? "",
    endAt: a?.endAt ?? "",
    sortOrder: a?.sortOrder != null ? String(a.sortOrder) : "0",
    enabled: a?.enabled ?? true,
  };
};

/**
 * Modal create/edit form for a modal-ad announcement. When `announcement` has an id it issues an
 * update; otherwise it creates a new one. Mirrors the banner form (image upload + media library +
 * link picker), minus the banner-specific placement/device fields (an 안내창 is always a popup).
 */
export default function AnnouncementFormDialog({
  announcement,
  onClose,
  onSaved,
}: AnnouncementFormDialogProps) {
  const isEdit = announcement?.id != null;
  const [form, setForm] = useState<FormState>(() => buildFormState(announcement));
  const [message, setMessage] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setForm(buildFormState(announcement));
  }, [announcement]);

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  // --- Link target picker (search & select real content/posts) ---
  const [linkKeyword, setLinkKeyword] = useState("");
  const [linkResults, setLinkResults] = useState<LinkSearchResult[]>([]);
  const [linkSearching, setLinkSearching] = useState(false);

  const changeLinkType = (next: LinkType) => {
    setForm((prev) => ({ ...prev, linkType: next, linkRefId: null, linkLabel: "", linkUrl: "" }));
    setLinkResults([]);
    setLinkKeyword("");
  };

  const runLinkSearch = async () => {
    const keyword = linkKeyword.trim() || undefined;
    setLinkSearching(true);
    setLinkResults([]);
    try {
      if (form.linkType === "VIDEO" || form.linkType === "MUSIC") {
        const res = await contentList({
          keyword,
          type:
            form.linkType === "MUSIC"
              ? ContentSearchRequestType.SOUND
              : ContentSearchRequestType.VIDEO,
          pageNumber: 0, // backend pagination is 0-based
          pageSize: 8,
        });
        setLinkResults(
          (res.resultObject?.contents ?? []).map((c) => ({
            id: c.id ?? 0,
            title: c.title ?? `#${c.id}`,
          })),
        );
      } else if (form.linkType === "POST") {
        const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
        const qs = new URLSearchParams({ pageNumber: "0", pageSize: "8" });
        if (keyword) qs.set("keyword", keyword);
        const r = await fetch(`${base}/pub/v1/posts?${qs.toString()}`);
        const j = await r.json();
        const items: Array<{ id?: number; title?: string }> =
          j?.resultObject?.contents ?? j?.resultObject ?? [];
        setLinkResults(items.map((p) => ({ id: p.id ?? 0, title: p.title ?? `#${p.id}` })));
      }
    } catch {
      setMessage("콘텐츠 검색에 실패했습니다.");
    } finally {
      setLinkSearching(false);
    }
  };

  const selectLink = (item: LinkSearchResult) => {
    setForm((prev) => ({ ...prev, linkRefId: item.id, linkLabel: item.title }));
    setLinkResults([]);
    setLinkKeyword("");
  };

  // --- Media library picker (browse already-uploaded images) ---
  const [showLibrary, setShowLibrary] = useState(false);
  const [libraryItems, setLibraryItems] = useState<MediaAssetDto[]>([]);
  const [libraryLoading, setLibraryLoading] = useState(false);

  const toggleLibrary = async () => {
    const next = !showLibrary;
    setShowLibrary(next);
    if (next && libraryItems.length === 0) {
      setLibraryLoading(true);
      try {
        const res = await mediaList({ pageNumber: 0, pageSize: 24 });
        setLibraryItems(res.resultObject?.contents ?? []);
      } catch {
        setMessage("미디어 라이브러리를 불러오지 못했습니다.");
      } finally {
        setLibraryLoading(false);
      }
    }
  };

  const handleImageUpload = async (file: File | undefined) => {
    if (!file) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      setMessage("이미지 파일만 업로드할 수 있습니다.");
      return;
    }
    setMessage(null);
    setUploading(true);
    try {
      const asset = await mediaUpload(file, "announcement");
      if (asset?.url) {
        update("imageUrl", asset.url);
      } else {
        setMessage("업로드 응답이 올바르지 않습니다.");
      }
    } catch {
      setMessage("이미지 업로드에 실패했습니다.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage(null);

    if (!form.title.trim()) {
      setMessage("제목을 입력해 주세요.");
      return;
    }

    const isContentLink =
      form.linkType === "VIDEO" || form.linkType === "MUSIC" || form.linkType === "POST";
    if (isContentLink && form.linkRefId == null) {
      setMessage(`링크할 ${LINK_TYPE_LABELS[form.linkType]} 콘텐츠를 검색해서 선택해 주세요.`);
      return;
    }

    if (form.linkType === "URL") {
      const trimmedUrl = form.linkUrl.trim();
      if (trimmedUrl && !isSafeLinkUrl(trimmedUrl)) {
        setMessage(
          "링크 URL은 http:// 또는 https:// 로 시작하거나 /로 시작하는 내부 경로여야 합니다.",
        );
        return;
      }
    }

    const parsedSort = Number(form.sortOrder);
    const payload: AnnouncementDto = {
      title: form.title.trim(),
      imageUrl: form.imageUrl.trim() || undefined,
      linkType: form.linkType || undefined,
      linkRefId: isContentLink ? form.linkRefId ?? undefined : undefined,
      linkLabel: isContentLink ? form.linkLabel || undefined : undefined,
      linkUrl: form.linkType === "URL" ? form.linkUrl.trim() || undefined : undefined,
      startAt: form.startAt.trim() || undefined,
      endAt: form.endAt.trim() || undefined,
      sortOrder: Number.isFinite(parsedSort) ? parsedSort : 0,
      enabled: form.enabled,
    };

    setSaving(true);
    try {
      const res =
        isEdit && announcement?.id != null
          ? await announcementUpdate(announcement.id, payload)
          : await announcementCreate(payload);
      if (res.resultCode === SUCCESS_CODE) {
        onSaved();
      } else {
        setMessage(res.resultMessage ?? "저장에 실패했습니다.");
      }
    } catch {
      setMessage("저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-md bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">
            {isEdit ? "안내창 수정" : "안내창 등록"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="닫기"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-5 py-4" noValidate>
          {message && (
            <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{message}</p>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {/* Title */}
            <div className="sm:col-span-2">
              <label htmlFor="ann-title" className="mb-1 block text-xs font-medium text-slate-500">
                제목 *
              </label>
              <input
                id="ann-title"
                type="text"
                className={FIELD_CLASS}
                value={form.title}
                onChange={(event) => update("title", event.target.value)}
              />
            </div>

            {/* Image */}
            <div className="sm:col-span-2">
              <label htmlFor="ann-image" className="mb-1 block text-xs font-medium text-slate-500">
                모달 이미지 (비워두면 텍스트 모달) — 업로드 시 미디어 라이브러리에 저장됩니다
              </label>
              <div className="flex items-center gap-2">
                <input
                  id="ann-image"
                  type="text"
                  placeholder="https://... 또는 이미지 업로드"
                  className={FIELD_CLASS}
                  value={form.imageUrl}
                  onChange={(event) => update("imageUrl", event.target.value)}
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                  className="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                >
                  {uploading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Upload className="h-4 w-4" />
                  )}
                  업로드
                </button>
                <button
                  type="button"
                  onClick={() => void toggleLibrary()}
                  className="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  <Images className="h-4 w-4" />
                  라이브러리
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  hidden
                  onChange={(event) => void handleImageUpload(event.target.files?.[0])}
                />
              </div>

              {showLibrary && (
                <div className="mt-2 rounded-md border border-slate-200 p-2">
                  {libraryLoading ? (
                    <div className="flex h-24 items-center justify-center">
                      <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
                    </div>
                  ) : libraryItems.length === 0 ? (
                    <p className="py-6 text-center text-sm text-slate-400">
                      업로드된 이미지가 없습니다. 먼저 업로드해 주세요.
                    </p>
                  ) : (
                    <div className="grid max-h-56 grid-cols-3 gap-2 overflow-y-auto sm:grid-cols-4">
                      {libraryItems.map((asset) => (
                        <button
                          key={asset.id}
                          type="button"
                          onClick={() => {
                            update("imageUrl", asset.url);
                            setShowLibrary(false);
                          }}
                          title={asset.originalName ?? asset.key}
                          className={`overflow-hidden rounded-md border-2 transition ${
                            form.imageUrl === asset.url
                              ? "border-brand"
                              : "border-transparent hover:border-slate-300"
                          }`}
                        >
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img
                            src={asset.url}
                            alt={asset.originalName ?? ""}
                            className="h-16 w-full object-cover"
                          />
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {form.imageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={form.imageUrl}
                  alt=""
                  className="mt-2 h-40 w-full max-w-[200px] rounded-md border border-slate-200 object-cover"
                />
              )}
            </div>

            {/* Link target */}
            <div className="sm:col-span-2">
              <label
                htmlFor="ann-linktype"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                링크 대상 (자세히 보기 클릭 시 이동)
              </label>
              <select
                id="ann-linktype"
                className={FIELD_CLASS}
                value={form.linkType}
                onChange={(event) => changeLinkType(event.target.value as LinkType)}
              >
                {(Object.keys(LINK_TYPE_LABELS) as LinkType[]).map((t) => (
                  <option key={t || "none"} value={t}>
                    {LINK_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>

              {form.linkType === "URL" && (
                <input
                  type="text"
                  placeholder="https://... 또는 /내부경로"
                  className={`${FIELD_CLASS} mt-2`}
                  value={form.linkUrl}
                  onChange={(event) => update("linkUrl", event.target.value)}
                />
              )}

              {(form.linkType === "VIDEO" ||
                form.linkType === "MUSIC" ||
                form.linkType === "POST") && (
                <div className="mt-2 space-y-2">
                  {form.linkRefId != null ? (
                    <div className="flex items-center justify-between rounded-md border border-brand/40 bg-brand/5 px-3 py-2 text-sm">
                      <span className="truncate text-slate-800">
                        선택됨: {form.linkLabel || `#${form.linkRefId}`}{" "}
                        <span className="text-slate-400">(#{form.linkRefId})</span>
                      </span>
                      <button
                        type="button"
                        onClick={() => setForm((p) => ({ ...p, linkRefId: null, linkLabel: "" }))}
                        className="ml-2 shrink-0 text-xs text-slate-500 hover:text-red-600"
                      >
                        제거
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          placeholder={`${LINK_TYPE_LABELS[form.linkType]} 제목 검색`}
                          className={FIELD_CLASS}
                          value={linkKeyword}
                          onChange={(event) => setLinkKeyword(event.target.value)}
                          onKeyDown={(event) => {
                            if (event.key === "Enter") {
                              event.preventDefault();
                              void runLinkSearch();
                            }
                          }}
                        />
                        <button
                          type="button"
                          onClick={() => void runLinkSearch()}
                          disabled={linkSearching}
                          className="inline-flex shrink-0 items-center rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                        >
                          {linkSearching ? <Loader2 className="h-4 w-4 animate-spin" /> : "검색"}
                        </button>
                      </div>
                      {linkResults.length > 0 && (
                        <ul className="max-h-44 overflow-y-auto rounded-md border border-slate-200">
                          {linkResults.map((r) => (
                            <li key={r.id}>
                              <button
                                type="button"
                                onClick={() => selectLink(r)}
                                className="block w-full truncate px-3 py-2 text-left text-sm hover:bg-slate-50"
                              >
                                {r.title} <span className="text-slate-400">(#{r.id})</span>
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Start at */}
            <div>
              <label htmlFor="ann-start" className="mb-1 block text-xs font-medium text-slate-500">
                노출 시작
              </label>
              <input
                id="ann-start"
                type="datetime-local"
                className={FIELD_CLASS}
                value={form.startAt}
                onChange={(event) => update("startAt", event.target.value)}
              />
            </div>

            {/* End at */}
            <div>
              <label htmlFor="ann-end" className="mb-1 block text-xs font-medium text-slate-500">
                노출 종료
              </label>
              <input
                id="ann-end"
                type="datetime-local"
                className={FIELD_CLASS}
                value={form.endAt}
                onChange={(event) => update("endAt", event.target.value)}
              />
            </div>

            {/* Sort order */}
            <div>
              <label htmlFor="ann-sort" className="mb-1 block text-xs font-medium text-slate-500">
                정렬 순서
              </label>
              <input
                id="ann-sort"
                type="number"
                className={FIELD_CLASS}
                value={form.sortOrder}
                onChange={(event) => update("sortOrder", event.target.value)}
              />
            </div>

            {/* Enabled */}
            <div>
              <label htmlFor="ann-use" className="mb-1 block text-xs font-medium text-slate-500">
                노출 여부
              </label>
              <select
                id="ann-use"
                className={FIELD_CLASS}
                value={form.enabled ? "Y" : "N"}
                onChange={(event) => update("enabled", event.target.value === "Y")}
              >
                <option value="Y">노출</option>
                <option value="N">미노출</option>
              </select>
            </div>
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving && <Loader2 className="h-4 w-4 animate-spin" />}
              저장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
