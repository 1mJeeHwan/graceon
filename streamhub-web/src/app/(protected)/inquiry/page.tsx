"use client";

import { useMemo, useState } from "react";
import { CheckCircle2, Loader2, MessageSquare, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  inquiryList,
  useInquiryCloseUpdate,
  useInquiryDelete,
} from "@/apis/query/inquiry/inquiry";
import {
  InquiryDtoCategory,
  InquiryDtoStatus,
  type InquiryDto,
  type InquirySearchRequestCategory,
  type InquirySearchRequestStatus,
} from "@/apis/query/streamHubAdminAPI.schemas";
import InquiryAnswerDialog from "@/components/inquiry/InquiryAnswerDialog";
import { SUCCESS_CODE } from "@/types/api";

type StatusFilter = "ALL" | InquirySearchRequestStatus;
type CategoryFilter = "ALL" | InquirySearchRequestCategory;

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: InquiryDtoStatus.OPEN, label: "미답변" },
  { value: InquiryDtoStatus.ANSWERED, label: "답변완료" },
  { value: InquiryDtoStatus.CLOSED, label: "종료" },
];

const CATEGORY_FILTERS: { value: CategoryFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: InquiryDtoCategory.ACCOUNT, label: "계정" },
  { value: InquiryDtoCategory.PAYMENT, label: "결제" },
  { value: InquiryDtoCategory.DELIVERY, label: "배송" },
  { value: InquiryDtoCategory.CONTENT, label: "콘텐츠" },
  { value: InquiryDtoCategory.ETC, label: "기타" },
];

const CATEGORY_LABEL: Record<InquiryDtoCategory, string> = {
  [InquiryDtoCategory.ACCOUNT]: "계정",
  [InquiryDtoCategory.PAYMENT]: "결제",
  [InquiryDtoCategory.DELIVERY]: "배송",
  [InquiryDtoCategory.CONTENT]: "콘텐츠",
  [InquiryDtoCategory.ETC]: "기타",
};

const STATUS_BADGE: Record<
  InquiryDtoStatus,
  { label: string; className: string }
> = {
  [InquiryDtoStatus.OPEN]: {
    label: "미답변",
    className: "bg-amber-100 text-amber-800 ring-1 ring-amber-300",
  },
  [InquiryDtoStatus.ANSWERED]: {
    label: "답변완료",
    className: "bg-emerald-100 text-emerald-700",
  },
  [InquiryDtoStatus.CLOSED]: {
    label: "종료",
    className: "bg-slate-200 text-slate-600",
  },
};

/** OPEN inquiries sort first so unanswered items surface at the top. */
const STATUS_ORDER: Record<InquiryDtoStatus, number> = {
  [InquiryDtoStatus.OPEN]: 0,
  [InquiryDtoStatus.ANSWERED]: 1,
  [InquiryDtoStatus.CLOSED]: 2,
};

export default function InquiryPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>("ALL");
  const [answering, setAnswering] = useState<InquiryDto | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["inquiry-list", statusFilter, categoryFilter],
    queryFn: ({ signal }) =>
      inquiryList(
        {
          status: statusFilter === "ALL" ? undefined : statusFilter,
          category: categoryFilter === "ALL" ? undefined : categoryFilter,
        },
        signal,
      ),
  });

  const closeMutation = useInquiryCloseUpdate();
  const deleteMutation = useInquiryDelete();

  const inquiries = useMemo<InquiryDto[]>(() => {
    const list = listQuery.data?.resultObject ?? [];
    return [...list].sort((a, b) => {
      const orderA = a.status ? STATUS_ORDER[a.status] : 99;
      const orderB = b.status ? STATUS_ORDER[b.status] : 99;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return (b.createdAt ?? "").localeCompare(a.createdAt ?? "");
    });
  }, [listQuery.data]);

  const openCount = useMemo(
    () =>
      inquiries.filter((item) => item.status === InquiryDtoStatus.OPEN).length,
    [inquiries],
  );

  const handleAnswered = () => {
    setAnswering(null);
    setMessage("답변이 등록되었습니다.");
    listQuery.refetch();
  };

  const handleClose = (inquiry: InquiryDto) => {
    if (inquiry.id == null) {
      return;
    }
    if (!window.confirm("이 문의를 종료 처리하시겠습니까?")) {
      return;
    }
    setMessage(null);
    closeMutation.mutate(
      { id: inquiry.id },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("문의를 종료했습니다.");
            listQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "종료 처리에 실패했습니다.");
          }
        },
        onError: () => setMessage("종료 처리 중 오류가 발생했습니다."),
      },
    );
  };

  const handleDelete = (inquiry: InquiryDto) => {
    if (inquiry.id == null) {
      return;
    }
    if (
      !window.confirm(
        `'${inquiry.title ?? "문의"}'을(를) 삭제하시겠습니까?`,
      )
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
        <h1 className="text-xl font-semibold text-slate-900">1:1 문의</h1>
        <p className="mt-1 text-sm text-slate-500">
          고객 문의를 확인하고 답변/종료 처리합니다. 미답변 문의가 우선
          노출됩니다.
        </p>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="inquiry-status"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            상태
          </label>
          <select
            id="inquiry-status"
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value as StatusFilter)
            }
            className="w-44 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            {STATUS_FILTERS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col">
          <label
            htmlFor="inquiry-category"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            분류
          </label>
          <select
            id="inquiry-category"
            value={categoryFilter}
            onChange={(event) =>
              setCategoryFilter(event.target.value as CategoryFilter)
            }
            className="w-44 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            {CATEGORY_FILTERS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Summary */}
      <div className="mb-3 flex flex-wrap items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {inquiries.length.toLocaleString()}건
        </span>
        <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-800">
          미답변 {openCount.toLocaleString()}건
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
          <p className="text-sm text-red-600">문의 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">분류</th>
                <th className="px-4 py-3">제목</th>
                <th className="px-4 py-3">작성자</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">접수일</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {inquiries.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 문의가 없습니다.
                  </td>
                </tr>
              ) : (
                inquiries.map((inquiry) => {
                  const badge = inquiry.status
                    ? STATUS_BADGE[inquiry.status]
                    : null;
                  const isClosed = inquiry.status === InquiryDtoStatus.CLOSED;
                  return (
                    <tr key={inquiry.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-slate-700">
                        {inquiry.category
                          ? CATEGORY_LABEL[inquiry.category]
                          : "-"}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {inquiry.title ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {inquiry.memberName ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        {badge ? (
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${badge.className}`}
                          >
                            {badge.label}
                          </span>
                        ) : (
                          "-"
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-500">
                        {inquiry.createdAt ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => setAnswering(inquiry)}
                            className="flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-brand"
                          >
                            <MessageSquare className="h-3.5 w-3.5" />
                            답변
                          </button>
                          <button
                            type="button"
                            onClick={() => handleClose(inquiry)}
                            disabled={isClosed || closeMutation.isPending}
                            className="flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-emerald-700 disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-slate-600"
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            종료
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
        <InquiryAnswerDialog
          inquiry={answering}
          onClose={() => setAnswering(null)}
          onAnswered={handleAnswered}
        />
      )}
    </div>
  );
}
