"use client";

import { useMemo, useState } from "react";
import { Loader2, Pencil } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  goodsStockList,
  useGoodsStockSoldoutUpdate,
  useGoodsStockStockUpdate,
} from "@/apis/query/goods-stock/goods-stock";
import {
  type GoodsStockDto,
  type GoodsStockSearchRequest,
} from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

const isSoldOut = (item: GoodsStockDto) => item.soldOut === "Y";

const isLowStock = (item: GoodsStockDto) =>
  item.stock != null &&
  item.notiQty != null &&
  item.stock <= item.notiQty;

export default function GoodsStockPage() {
  const [keyword, setKeyword] = useState("");
  const [lowStock, setLowStock] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [editing, setEditing] = useState<GoodsStockDto | null>(null);

  const searchRequest = useMemo<GoodsStockSearchRequest>(
    () => ({
      keyword: keyword.trim() || undefined,
      lowStock: lowStock ? "Y" : undefined,
      sortByStockAsc: true,
    }),
    [keyword, lowStock],
  );

  const listQuery = useQuery({
    queryKey: ["goods-stock-list", searchRequest],
    queryFn: ({ signal }) => goodsStockList(searchRequest, signal),
  });

  const stockUpdateMutation = useGoodsStockStockUpdate();
  const soldoutMutation = useGoodsStockSoldoutUpdate();

  const items: GoodsStockDto[] = listQuery.data?.resultObject ?? [];

  const handleStockSaved = () => {
    setEditing(null);
    setMessage("재고가 수정되었습니다.");
    listQuery.refetch();
  };

  const handleStockSave = (stock: number, notiQty: number) => {
    if (editing?.id == null) {
      return;
    }
    setMessage(null);
    stockUpdateMutation.mutate(
      { id: editing.id, data: { stock, notiQty } },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            handleStockSaved();
          } else {
            setMessage(response.resultMessage ?? "재고 수정에 실패했습니다.");
          }
        },
        onError: () => setMessage("재고 수정 중 오류가 발생했습니다."),
      },
    );
  };

  const handleSoldoutToggle = (item: GoodsStockDto) => {
    if (item.id == null) {
      return;
    }
    setMessage(null);
    soldoutMutation.mutate(
      { id: item.id },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("품절 상태가 변경되었습니다.");
            listQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "품절 전환에 실패했습니다.");
          }
        },
        onError: () => setMessage("품절 전환 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-900">옵션·재고 관리</h1>
        <p className="mt-1 text-sm text-slate-500">
          상품 옵션별 재고와 통보 수량을 관리하고 품절 여부를 전환합니다.
        </p>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="stock-keyword"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            검색어
          </label>
          <input
            id="stock-keyword"
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="코드 / 상품명"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>
        <label className="flex items-center gap-2 pb-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={lowStock}
            onChange={(event) => setLowStock(event.target.checked)}
            className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
          />
          재고부족만
        </label>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {items.length.toLocaleString()}건
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
          <p className="text-sm text-red-600">재고 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">코드</th>
                <th className="px-4 py-3">상품명</th>
                <th className="px-4 py-3 text-right">판매가</th>
                <th className="px-4 py-3 text-right">재고</th>
                <th className="px-4 py-3 text-right">통보수량</th>
                <th className="px-4 py-3">품절</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 재고가 없습니다.
                  </td>
                </tr>
              ) : (
                items.map((item) => {
                  const low = isLowStock(item);
                  return (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-mono text-xs text-slate-600">
                        {item.code ?? "-"}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {item.name ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-700">
                        {item.price != null
                          ? `${item.price.toLocaleString()}원`
                          : "-"}
                      </td>
                      <td
                        className={`px-4 py-3 text-right font-semibold ${
                          low ? "text-red-600" : "text-slate-900"
                        }`}
                      >
                        {item.stock != null
                          ? item.stock.toLocaleString()
                          : "-"}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-700">
                        {item.notiQty != null
                          ? item.notiQty.toLocaleString()
                          : "-"}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          onClick={() => handleSoldoutToggle(item)}
                          disabled={soldoutMutation.isPending}
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium transition disabled:opacity-50 ${
                            isSoldOut(item)
                              ? "bg-red-100 text-red-700 hover:bg-red-200"
                              : "bg-emerald-100 text-emerald-700 hover:bg-emerald-200"
                          }`}
                        >
                          {isSoldOut(item) ? "품절" : "판매중"}
                        </button>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end">
                          <button
                            type="button"
                            onClick={() => {
                              setMessage(null);
                              setEditing(item);
                            }}
                            className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                            aria-label="재고 수정"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
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

      {editing && (
        <StockEditDialog
          item={editing}
          saving={stockUpdateMutation.isPending}
          onClose={() => setEditing(null)}
          onSave={handleStockSave}
        />
      )}
    </div>
  );
}

interface StockEditDialogProps {
  item: GoodsStockDto;
  saving: boolean;
  onClose: () => void;
  onSave: (stock: number, notiQty: number) => void;
}

function StockEditDialog({
  item,
  saving,
  onClose,
  onSave,
}: StockEditDialogProps) {
  const [stock, setStock] = useState(String(item.stock ?? 0));
  const [notiQty, setNotiQty] = useState(String(item.notiQty ?? 0));

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const stockNum = Number(stock);
    const notiNum = Number(notiQty);
    if (
      !Number.isFinite(stockNum) ||
      !Number.isFinite(notiNum) ||
      stockNum < 0 ||
      notiNum < 0
    ) {
      return;
    }
    onSave(stockNum, notiNum);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-md bg-white p-5 shadow-lg"
      >
        <h2 className="text-base font-semibold text-slate-900">재고 수정</h2>
        <p className="mt-1 text-sm text-slate-500">
          {item.name ?? "-"} ({item.code ?? "-"})
        </p>

        <div className="mt-4 flex flex-col gap-3">
          <div className="flex flex-col">
            <label
              htmlFor="edit-stock"
              className="mb-1 text-xs font-medium text-slate-600"
            >
              재고
            </label>
            <input
              id="edit-stock"
              type="number"
              min={0}
              value={stock}
              onChange={(event) => setStock(event.target.value)}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
            />
          </div>
          <div className="flex flex-col">
            <label
              htmlFor="edit-noti"
              className="mb-1 text-xs font-medium text-slate-600"
            >
              통보수량
            </label>
            <input
              id="edit-noti"
              type="number"
              min={0}
              value={notiQty}
              onChange={(event) => setNotiQty(event.target.value)}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
            />
          </div>
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
          >
            {saving && <Loader2 className="h-4 w-4 animate-spin" />}
            저장
          </button>
        </div>
      </form>
    </div>
  );
}
