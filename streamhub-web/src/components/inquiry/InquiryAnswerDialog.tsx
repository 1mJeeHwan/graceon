"use client";

import { useEffect, useState } from "react";
import { Loader2, X } from "lucide-react";

import { useInquiryAnswerUpdate } from "@/apis/query/inquiry/inquiry";
import {
  InquiryDtoCategory,
  InquiryDtoStatus,
  type InquiryDto,
} from "@/apis/query/graceOnAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

/** Korean labels for inquiry categories. */
const CATEGORY_LABEL: Record<InquiryDtoCategory, string> = {
  [InquiryDtoCategory.ACCOUNT]: "계정",
  [InquiryDtoCategory.PAYMENT]: "결제",
  [InquiryDtoCategory.DELIVERY]: "배송",
  [InquiryDtoCategory.CONTENT]: "콘텐츠",
  [InquiryDtoCategory.ETC]: "기타",
};

interface InquiryAnswerDialogProps {
  /** Inquiry to answer. */
  inquiry: InquiryDto;
  onClose: () => void;
  /** Called after a successful answer so the parent can refetch. */
  onAnswered: () => void;
}

/**
 * InquiryAnswerDialog is a modal that shows the inquiry content and lets an
 * admin write/update the answer. Submitting issues the answer mutation which
 * also transitions the inquiry status to ANSWERED on the backend.
 */
export default function InquiryAnswerDialog({
  inquiry,
  onClose,
  onAnswered,
}: InquiryAnswerDialogProps) {
  const [answerContent, setAnswerContent] = useState(
    inquiry.answerContent ?? "",
  );
  const [message, setMessage] = useState<string | null>(null);

  const answerMutation = useInquiryAnswerUpdate();
  const isPending = answerMutation.isPending;

  useEffect(() => {
    setAnswerContent(inquiry.answerContent ?? "");
    setMessage(null);
  }, [inquiry]);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (inquiry.id == null) {
      return;
    }
    const trimmed = answerContent.trim();
    if (!trimmed) {
      setMessage("답변 내용을 입력해 주세요.");
      return;
    }
    setMessage(null);
    answerMutation.mutate(
      { id: inquiry.id, data: { answerContent: trimmed } },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            onAnswered();
          } else {
            setMessage(response.resultMessage ?? "답변 등록에 실패했습니다.");
          }
        },
        onError: () => setMessage("답변 등록 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="flex max-h-[90vh] w-full max-w-lg flex-col rounded-md bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">문의 답변</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="닫기"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          onSubmit={handleSubmit}
          className="flex min-h-0 flex-1 flex-col px-5 py-4"
          noValidate
        >
          <div className="min-h-0 flex-1 overflow-y-auto">
            {message && (
              <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                {message}
              </p>
            )}

            <div className="mb-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <span className="rounded-full bg-slate-100 px-2.5 py-0.5 font-medium text-slate-600">
                {inquiry.category
                  ? CATEGORY_LABEL[inquiry.category]
                  : "미분류"}
              </span>
              <span>작성자: {inquiry.memberName ?? "-"}</span>
              {inquiry.createdAt && <span>접수: {inquiry.createdAt}</span>}
            </div>

            <h3 className="mb-1 text-sm font-semibold text-slate-900">
              {inquiry.title ?? "(제목 없음)"}
            </h3>
            <div className="mb-4 whitespace-pre-wrap rounded-md border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
              {inquiry.content ?? "(내용 없음)"}
            </div>

            <label
              htmlFor="inquiry-answer"
              className="mb-1 block text-xs font-medium text-slate-500"
            >
              답변 내용 *
            </label>
            <textarea
              id="inquiry-answer"
              rows={6}
              value={answerContent}
              onChange={(event) => setAnswerContent(event.target.value)}
              placeholder="고객에게 전달할 답변을 입력하세요."
              className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
            />
            {inquiry.status === InquiryDtoStatus.ANSWERED &&
              inquiry.answeredAt && (
                <p className="mt-1 text-xs text-slate-400">
                  최근 답변: {inquiry.answeredAt}
                </p>
              )}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              답변 등록
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
