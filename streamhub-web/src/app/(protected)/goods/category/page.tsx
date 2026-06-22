"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  goodsCategoryList,
  useGoodsCategoryDelete,
} from "@/apis/query/goods-category/goods-category";
import { type GoodsCategoryNodeDto } from "@/apis/query/streamHubAdminAPI.schemas";
import GoodsCategoryFormDialog from "@/components/goods-category/GoodsCategoryFormDialog";
import { SUCCESS_CODE } from "@/types/api";

type DialogState =
  | { mode: "create-root" }
  | { mode: "create-child"; parent: GoodsCategoryNodeDto }
  | { mode: "edit"; category: GoodsCategoryNodeDto };

/**
 * Order a flat list of category nodes into a depth-first traversal so children
 * render directly beneath their parent. Falls back to the API order if a node's
 * parent is missing.
 */
function buildOrderedTree(
  nodes: GoodsCategoryNodeDto[],
): GoodsCategoryNodeDto[] {
  const childrenByParent = new Map<number | "root", GoodsCategoryNodeDto[]>();
  for (const node of nodes) {
    const key = node.parentId ?? "root";
    const bucket = childrenByParent.get(key) ?? [];
    bucket.push(node);
    childrenByParent.set(key, bucket);
  }
  for (const bucket of childrenByParent.values()) {
    bucket.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  }

  const ordered: GoodsCategoryNodeDto[] = [];
  const visit = (parentKey: number | "root") => {
    const bucket = childrenByParent.get(parentKey) ?? [];
    for (const node of bucket) {
      ordered.push(node);
      if (node.id != null) {
        visit(node.id);
      }
    }
  };
  visit("root");

  // Append any orphaned nodes whose parent was not found in the list.
  if (ordered.length !== nodes.length) {
    const seen = new Set(ordered.map((node) => node.id));
    for (const node of nodes) {
      if (!seen.has(node.id)) {
        ordered.push(node);
      }
    }
  }

  return ordered;
}

export default function GoodsCategoryPage() {
  const [dialog, setDialog] = useState<DialogState | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["goods-category-list"],
    queryFn: ({ signal }) => goodsCategoryList(signal),
  });

  const deleteMutation = useGoodsCategoryDelete();

  const ordered = useMemo(() => {
    const nodes: GoodsCategoryNodeDto[] = listQuery.data?.resultObject ?? [];
    return buildOrderedTree(nodes);
  }, [listQuery.data]);

  const openCreateRoot = () => {
    setMessage(null);
    setDialog({ mode: "create-root" });
  };

  const openCreateChild = (parent: GoodsCategoryNodeDto) => {
    setMessage(null);
    setDialog({ mode: "create-child", parent });
  };

  const openEdit = (category: GoodsCategoryNodeDto) => {
    setMessage(null);
    setDialog({ mode: "edit", category });
  };

  const handleSaved = () => {
    setDialog(null);
    setMessage("저장되었습니다.");
    listQuery.refetch();
  };

  const handleDelete = (category: GoodsCategoryNodeDto) => {
    if (category.id == null) {
      return;
    }
    if (
      !window.confirm(
        `'${category.name ?? "카테고리"}'을(를) 삭제하시겠습니까? 하위 카테고리가 있으면 함께 삭제될 수 있습니다.`,
      )
    ) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: category.id },
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
          <h1 className="text-xl font-semibold text-slate-900">카테고리 관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            상품 카테고리를 3단 트리 구조로 관리합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreateRoot}
          className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          <Plus className="h-4 w-4" />
          최상위 카테고리 등록
        </button>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {ordered.length.toLocaleString()}건
        </span>
        {message && (
          <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
            {message}
          </span>
        )}
      </div>

      {/* Tree */}
      {listQuery.isLoading ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">
            카테고리 목록을 불러오지 못했습니다.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">카테고리명</th>
                <th className="px-4 py-3 w-28">정렬순서</th>
                <th className="px-4 py-3 w-20">노출</th>
                <th className="px-4 py-3 text-right w-40">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {ordered.length === 0 ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    등록된 카테고리가 없습니다.
                  </td>
                </tr>
              ) : (
                ordered.map((node) => {
                  const depth = node.depth ?? 0;
                  const canAddChild = depth < 2;
                  return (
                    <tr key={node.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-900">
                        <span
                          style={{ paddingLeft: `${depth * 1.5}rem` }}
                          className="inline-flex items-center gap-1.5"
                        >
                          {depth > 0 && (
                            <span className="text-slate-300">
                              {"└".padStart(1)}
                            </span>
                          )}
                          {node.id != null ? (
                            <Link
                              href={`/goods?categoryId=${node.id}`}
                              className="text-brand hover:underline"
                              title="이 카테고리 상품 보기"
                            >
                              {node.name ?? "-"}
                            </Link>
                          ) : (
                            (node.name ?? "-")
                          )}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {node.sortOrder ?? 0}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            node.useYn === "N"
                              ? "bg-slate-200 text-slate-600"
                              : "bg-emerald-100 text-emerald-700"
                          }`}
                        >
                          {node.useYn === "N" ? "숨김" : "노출"}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          {canAddChild && (
                            <button
                              type="button"
                              onClick={() => openCreateChild(node)}
                              className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                              aria-label="하위 추가"
                            >
                              <Plus className="h-4 w-4" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => openEdit(node)}
                            className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                            aria-label="수정"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(node)}
                            disabled={deleteMutation.isPending}
                            className="rounded p-1.5 text-slate-500 transition hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                            aria-label="삭제"
                          >
                            <Trash2 className="h-4 w-4" />
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

      {dialog && (
        <GoodsCategoryFormDialog
          category={dialog.mode === "edit" ? dialog.category : null}
          parent={dialog.mode === "create-child" ? dialog.parent : null}
          onClose={() => setDialog(null)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
