"use client";

import { useMemo, useState } from "react";
import { Loader2, MapPin, Pencil, Plus, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { storeList, useStoreDelete } from "@/apis/query/store/store";
import { type StoreDto } from "@/apis/query/streamHubAdminAPI.schemas";
import StoreFormDialog from "@/components/stores/StoreFormDialog";
import { SUCCESS_CODE } from "@/types/api";

export default function StoresPage() {
  const [keyword, setKeyword] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<StoreDto | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["store-list"],
    queryFn: ({ signal }) => storeList(signal),
  });

  const deleteMutation = useStoreDelete();

  const filtered = useMemo(() => {
    const stores: StoreDto[] = listQuery.data?.resultObject ?? [];
    const term = keyword.trim().toLowerCase();
    if (!term) {
      return stores;
    }
    return stores.filter(
      (store) =>
        (store.name ?? "").toLowerCase().includes(term) ||
        (store.address ?? "").toLowerCase().includes(term),
    );
  }, [listQuery.data, keyword]);

  const openCreate = () => {
    setEditing(null);
    setMessage(null);
    setDialogOpen(true);
  };

  const openEdit = (store: StoreDto) => {
    setEditing(store);
    setMessage(null);
    setDialogOpen(true);
  };

  const handleSaved = () => {
    setDialogOpen(false);
    setEditing(null);
    setMessage("저장되었습니다.");
    listQuery.refetch();
  };

  const handleDelete = (store: StoreDto) => {
    if (store.id == null) {
      return;
    }
    if (!window.confirm(`'${store.name ?? "매장"}'을(를) 삭제하시겠습니까?`)) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: store.id },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("삭제되었습니다.");
            listQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "삭제에 실패했습니다.");
          }
        },
        onError: () => setMessage("삭제 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">매장관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            오프라인 매장 정보와 좌표(위도/경도)를 관리합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          <Plus className="h-4 w-4" />
          매장 등록
        </button>
      </div>

      {/* Search bar */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="store-keyword"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            검색어
          </label>
          <input
            id="store-keyword"
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="매장명 / 주소"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {filtered.length.toLocaleString()}건
        </span>
        {message && (
          <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
            {message}
          </span>
        )}
      </div>

      {/* List */}
      {listQuery.isLoading ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">매장 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">매장명</th>
                <th className="px-4 py-3">주소</th>
                <th className="px-4 py-3">전화번호</th>
                <th className="px-4 py-3">영업시간</th>
                <th className="px-4 py-3">좌표</th>
                <th className="px-4 py-3">노출</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 매장이 없습니다.
                  </td>
                </tr>
              ) : (
                filtered.map((store) => (
                  <tr key={store.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900">
                      {store.name ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {store.address ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {store.phone ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {store.openHours ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {store.lat != null && store.lng != null ? (
                        <span className="inline-flex items-center gap-1 text-xs text-slate-500">
                          <MapPin className="h-3.5 w-3.5" />
                          {store.lat.toFixed(4)}, {store.lng.toFixed(4)}
                        </span>
                      ) : (
                        "-"
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          store.useYn === "N"
                            ? "bg-slate-200 text-slate-600"
                            : "bg-emerald-100 text-emerald-700"
                        }`}
                      >
                        {store.useYn === "N" ? "숨김" : "노출"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => openEdit(store)}
                          className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                          aria-label="수정"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(store)}
                          disabled={deleteMutation.isPending}
                          className="rounded p-1.5 text-slate-500 transition hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                          aria-label="삭제"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {dialogOpen && (
        <StoreFormDialog
          store={editing}
          onClose={() => setDialogOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
