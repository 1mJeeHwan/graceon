"use client";

import { useState } from "react";
import { Eye, EyeOff, Loader2, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  goodsReviewList,
  useGoodsReviewDelete,
  useGoodsReviewDisplayUpdate,
} from "@/apis/query/goods-review/goods-review";
import { type GoodsReviewDto } from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

type DisplayFilter = "ALL" | "Y" | "N";

const FILTER_OPTIONS: ReadonlyArray<{ value: DisplayFilter; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "Y", label: "노출" },
  { value: "N", label: "숨김" },
];

const renderStars = (rating: number): string => {
  const filled = Math.max(0, Math.min(5, Math.round(rating)));
  return "★★★★★☆☆☆☆☆".slice(5 - filled, 10 - filled);
};

export default function GoodsReviewPage() {
  const [filter, setFilter] = useState<DisplayFilter>("ALL");
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["goods-review-list", filter],
    queryFn: () =>
      goodsReviewList({
        displayYn: filter === "ALL" ? undefined : filter,
      }),
  });

  const displayMutation = useGoodsReviewDisplayUpdate();
  const deleteMutation = useGoodsReviewDelete();

  const reviews: GoodsReviewDto[] = listQuery.data?.resultObject ?? [];

  const handleToggleDisplay = (review: GoodsReviewDto) => {
    if (review.id == null) {
      return;
    }
    const nextDisplayYn = review.displayYn === "Y" ? "N" : "Y";
    setMessage(null);
    displayMutation.mutate(
      { id: review.id, data: { displayYn: nextDisplayYn } },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage(
              nextDisplayYn === "Y"
                ? "후기를 노출 처리했습니다."
                : "후기를 숨김 처리했습니다.",
            );
            listQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "노출 변경에 실패했습니다.");
          }
        },
        onError: () => setMessage("노출 변경 중 오류가 발생했습니다."),
      },
    );
  };

  const handleDelete = (review: GoodsReviewDto) => {
    if (review.id == null) {
      return;
    }
    if (!window.confirm("이 후기를 삭제하시겠습니까?")) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: review.id },
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

  const isMutating = displayMutation.isPending || deleteMutation.isPending;

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-900">굿즈 후기</h1>
        <p className="mt-1 text-sm text-slate-500">
          상품 후기의 노출 여부를 관리하고, 부적절한 후기를 삭제합니다.
        </p>
      </div>

      {/* Filter bar */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="review-display-filter"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            노출 여부
          </label>
          <select
            id="review-display-filter"
            value={filter}
            onChange={(event) =>
              setFilter(event.target.value as DisplayFilter)
            }
            className="w-40 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            {FILTER_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {reviews.length.toLocaleString()}건
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
          <p className="text-sm text-red-600">후기 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">평점</th>
                <th className="px-4 py-3">작성자</th>
                <th className="px-4 py-3">내용</th>
                <th className="px-4 py-3">노출</th>
                <th className="px-4 py-3">작성일</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {reviews.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 후기가 없습니다.
                  </td>
                </tr>
              ) : (
                reviews.map((review) => (
                  <tr key={review.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 whitespace-nowrap text-amber-500">
                      <span aria-label={`평점 ${review.rating ?? 0}점`}>
                        {renderStars(review.rating ?? 0)}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-900">
                      {review.memberName ?? "-"}
                    </td>
                    <td className="max-w-md truncate px-4 py-3 text-slate-700">
                      {review.content ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          review.displayYn === "N"
                            ? "bg-slate-200 text-slate-600"
                            : "bg-emerald-100 text-emerald-700"
                        }`}
                      >
                        {review.displayYn === "N" ? "숨김" : "노출"}
                      </span>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-slate-500">
                      {review.createdAt
                        ? review.createdAt.slice(0, 10)
                        : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => handleToggleDisplay(review)}
                          disabled={isMutating}
                          className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand disabled:opacity-50"
                          aria-label={
                            review.displayYn === "N" ? "노출하기" : "숨기기"
                          }
                        >
                          {review.displayYn === "N" ? (
                            <Eye className="h-4 w-4" />
                          ) : (
                            <EyeOff className="h-4 w-4" />
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(review)}
                          disabled={isMutating}
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
    </div>
  );
}
