"use client";

import { useMemo, useState } from "react";
import { Loader2, MessageSquare, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  goodsInquiryList,
  useGoodsInquiryDelete,
} from "@/apis/query/goods-inquiry/goods-inquiry";
import {
  type GoodsInquiryDto,
  GoodsInquiryDtoAnswerStatus,
  type GoodsInquirySearchRequestAnswerStatus,
} from "@/apis/query/streamHubAdminAPI.schemas";
import GoodsInquiryAnswerDialog from "@/components/goods-inquiry/GoodsInquiryAnswerDialog";
import { SUCCESS_CODE } from "@/types/api";

type StatusFilter = "ALL" | GoodsInquirySearchRequestAnswerStatus;

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: GoodsInquiryDtoAnswerStatus.WAITING, label: "미답변" },
  { value: GoodsInquiryDtoAnswerStatus.ANSWERED, label: "답변완료" },
];

function formatDate(value?: string): string {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}

export default function GoodsInquiryPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [answering, setAnswering] = useState<GoodsInquiryDto | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["goods-inquiry-list", statusFilter],
    queryFn: ({ signal }) =>
      goodsInquiryList(
        statusFilter === "ALL" ? {} : { answerStatus: statusFilter },
        signal,
      ),
  });

  const deleteMutation = useGoodsInquiryDelete();

  const inquiries: GoodsInquiryDto[] = useMemo(
    () => listQuery.data?.resultObject ?? [],
    [listQuery.data],
  );

  const unansweredCount = useMemo(
    () =>
      inquiries.filter(
        (inquiry) =>
          inquiry.answerStatus === GoodsInquiryDtoAnswerStatus.WAITING,
      ).length,
    [inquiries],
  );

  const openAnswer = (inquiry: GoodsInquiryDto) => {
    setMessage(null);
    setAnswering(inquiry);
  };

  const handleAnswered = () => {
    setAnswering(null);
    setMessage("답변이 등록되었습니다.");
    listQuery.refetch();
  };

  const handleDelete = (inquiry: GoodsInquiryDto) => {
    if (inquiry.id == null) {
      return;
    }
    if (
      !window.confirm(`'${inquiry.title ?? "문의"}'을(를) 삭제하시겠습니까?`)
    ) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: inquiry.id },
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
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-900">굿즈 문의</h1>
        <p className="mt-1 text-sm text-slate-500">
          상품에 대한 고객 문의를 확인하고 답변을 등록합니다.
        </p>
      </div>

      {/* Filter */}
      <div className="mb-4 flex flex-wrap items-center gap-2 rounded-md border border-slate-200 bg-white p-4">
        <span className="mr-1 text-xs font-medium text-slate-600">답변 상태</span>
        {STATUS_FILTERS.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => setStatusFilter(filter.value)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
              statusFilter === filter.value
                ? "bg-brand text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {/* Summary */}
      <div className="mb-3 flex flex-wrap items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {inquiries.length.toLocaleString()}건
        </span>
        <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700">
          미답변 {unansweredCount.toLocaleString()}건
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
          <p className="text-sm text-red-600">
            문의 목록을 불러오지 못했습니다.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">제목</th>
                <th className="px-4 py-3">작성자</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">작성일</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {inquiries.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 문의가 없습니다.
                  </td>
                </tr>
              ) : (
                inquiries.map((inquiry) => {
                  const answered =
                    inquiry.answerStatus ===
                    GoodsInquiryDtoAnswerStatus.ANSWERED;
                  return (
                    <tr key={inquiry.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {inquiry.title ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {inquiry.memberName ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            answered
                              ? "bg-emerald-100 text-emerald-700"
                              : "bg-amber-100 text-amber-800 ring-1 ring-amber-300"
                          }`}
                        >
                          {answered ? "답변완료" : "미답변"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {formatDate(inquiry.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openAnswer(inquiry)}
                            className="inline-flex items-center gap-1 rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                            aria-label="답변"
                          >
                            <MessageSquare className="h-4 w-4" />
                            <span className="text-xs font-medium">답변</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(inquiry)}
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

      {answering && (
        <GoodsInquiryAnswerDialog
          inquiry={answering}
          onClose={() => setAnswering(null)}
          onAnswered={handleAnswered}
        />
      )}
    </div>
  );
}
