"use client";

import { useState } from "react";
import { useSession } from "next-auth/react";
import { useQuery } from "@tanstack/react-query";
import {
  ChevronDown,
  ChevronUp,
  Image as ImageIcon,
  Loader2,
  Pencil,
  Plus,
  Trash2,
} from "lucide-react";

import {
  announcementList,
  announcementDelete,
  announcementUpdateSort,
  type AnnouncementDto,
} from "@/apis/announcement";
import AnnouncementFormDialog from "@/components/announcement/AnnouncementFormDialog";
import { canWrite } from "@/lib/auth-utils";
import { SUCCESS_CODE } from "@/types/api";

const FILTER_FIELD_CLASS =
  "rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

const formatPeriod = (value?: string | null) => {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
};

const isExpired = (endAt?: string | null) => {
  if (!endAt) {
    return false;
  }
  const end = new Date(endAt);
  if (Number.isNaN(end.getTime())) {
    return false;
  }
  return end.getTime() < Date.now();
};

export default function AnnouncementPage() {
  const { data: session } = useSession();
  const writable = canWrite(session?.user?.role);

  const [enabledFilter, setEnabledFilter] = useState<string>("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AnnouncementDto | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const listQuery = useQuery({
    queryKey: ["announcement-list", enabledFilter],
    queryFn: ({ signal }) =>
      announcementList(
        { enabled: enabledFilter === "" ? undefined : enabledFilter === "Y" },
        signal,
      ),
  });

  const items: AnnouncementDto[] = listQuery.data?.resultObject ?? [];

  const openCreate = () => {
    setEditing(null);
    setMessage(null);
    setDialogOpen(true);
  };

  const openEdit = (item: AnnouncementDto) => {
    setEditing(item);
    setMessage(null);
    setDialogOpen(true);
  };

  const handleSaved = () => {
    setDialogOpen(false);
    setEditing(null);
    setMessage("저장되었습니다.");
    listQuery.refetch();
  };

  const handleDelete = async (item: AnnouncementDto) => {
    if (item.id == null) {
      return;
    }
    if (!window.confirm(`'${item.title}'을(를) 삭제하시겠습니까?`)) {
      return;
    }
    setMessage(null);
    setBusy(true);
    try {
      const res = await announcementDelete(item.id);
      if (res.resultCode === SUCCESS_CODE) {
        setMessage("삭제되었습니다.");
        listQuery.refetch();
      } else {
        setMessage(res.resultMessage ?? "삭제에 실패했습니다.");
      }
    } catch {
      setMessage("삭제 중 오류가 발생했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const handleReorder = async (index: number, direction: "up" | "down") => {
    const target = items[index];
    const swapWith = items[direction === "up" ? index - 1 : index + 1];
    if (!target || !swapWith || target.id == null || swapWith.id == null) {
      return;
    }
    setMessage(null);
    setBusy(true);
    try {
      const a = await announcementUpdateSort(target.id, swapWith.sortOrder);
      const b = await announcementUpdateSort(swapWith.id, target.sortOrder);
      if (a.resultCode !== SUCCESS_CODE || b.resultCode !== SUCCESS_CODE) {
        setMessage("정렬 변경에 실패했습니다.");
        return;
      }
      setMessage("정렬 순서가 변경되었습니다.");
      listQuery.refetch();
    } catch {
      setMessage("정렬 변경 중 오류가 발생했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">안내창 관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            사용자 사이트에 이미지 팝업(모달 광고)으로 노출되는 안내창을 배너처럼 등록·관리합니다.
          </p>
        </div>
        {writable && (
          <button
            type="button"
            onClick={openCreate}
            className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
          >
            <Plus className="h-4 w-4" />
            안내창 등록
          </button>
        )}
      </div>

      {/* Filter */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label htmlFor="filter-enabled" className="mb-1 text-xs font-medium text-slate-600">
            노출 여부
          </label>
          <select
            id="filter-enabled"
            value={enabledFilter}
            onChange={(event) => setEnabledFilter(event.target.value)}
            className={FILTER_FIELD_CLASS}
          >
            <option value="">전체</option>
            <option value="Y">노출</option>
            <option value="N">미노출</option>
          </select>
        </div>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">총 {items.length.toLocaleString()}건</span>
        {message && (
          <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{message}</span>
        )}
      </div>

      {/* List */}
      {listQuery.isLoading ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">안내창 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">이미지</th>
                <th className="px-4 py-3">제목</th>
                <th className="px-4 py-3">링크</th>
                <th className="px-4 py-3">기간</th>
                <th className="px-4 py-3">정렬</th>
                <th className="px-4 py-3">노출</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-10 text-center text-slate-400">
                    등록된 안내창이 없습니다.
                  </td>
                </tr>
              ) : (
                items.map((item, index) => {
                  const expired = isExpired(item.endAt);
                  const isFirst = index === 0;
                  const isLast = index === items.length - 1;
                  return (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        {item.imageUrl ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img
                            src={item.imageUrl}
                            alt={item.title}
                            className="h-16 w-12 rounded border border-slate-200 object-cover"
                          />
                        ) : (
                          <div className="flex h-16 w-12 items-center justify-center rounded border border-dashed border-slate-200 text-slate-300">
                            <ImageIcon className="h-4 w-4" />
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">{item.title}</td>
                      <td className="px-4 py-3 text-xs text-slate-500">
                        {item.linkUrl ? (
                          <span className="truncate">{item.linkLabel || item.linkUrl}</span>
                        ) : (
                          "-"
                        )}
                      </td>
                      <td className="px-4 py-3 text-xs">
                        <div className="text-slate-700">{formatPeriod(item.startAt)}</div>
                        <div className={expired ? "text-red-600" : "text-slate-500"}>
                          ~ {formatPeriod(item.endAt)}
                          {expired && " (만료)"}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        <div className="flex items-center gap-2">
                          <span className="w-6 text-center tabular-nums">{item.sortOrder}</span>
                          <div className="flex flex-col">
                            <button
                              type="button"
                              onClick={() => handleReorder(index, "up")}
                              disabled={isFirst || busy || !writable}
                              className="rounded p-0.5 text-slate-400 transition hover:bg-slate-100 hover:text-brand disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-400"
                              aria-label="위로"
                            >
                              <ChevronUp className="h-4 w-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleReorder(index, "down")}
                              disabled={isLast || busy || !writable}
                              className="rounded p-0.5 text-slate-400 transition hover:bg-slate-100 hover:text-brand disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-400"
                              aria-label="아래로"
                            >
                              <ChevronDown className="h-4 w-4" />
                            </button>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            item.enabled
                              ? "bg-emerald-100 text-emerald-700"
                              : "bg-slate-200 text-slate-600"
                          }`}
                        >
                          {item.enabled ? "노출" : "미노출"}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openEdit(item)}
                            className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                            aria-label="수정"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          {writable && (
                            <button
                              type="button"
                              onClick={() => void handleDelete(item)}
                              disabled={busy}
                              className="rounded p-1.5 text-slate-500 transition hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                              aria-label="삭제"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {dialogOpen && (
        <AnnouncementFormDialog
          announcement={editing}
          onClose={() => setDialogOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
