"use client";

import { useState } from "react";
import { Loader2, X } from "lucide-react";

import { useGoodsInquiryAnswerUpdate } from "@/apis/query/goods-inquiry/goods-inquiry";
import {
  type GoodsInquiryDto,
  GoodsInquiryDtoAnswerStatus,
} from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

interface GoodsInquiryAnswerDialogProps {
  inquiry: GoodsInquiryDto;
  onClose: () => void;
  onAnswered: () => void;
}

/**
 * GoodsInquiryAnswerDialog shows a single inquiry's content and lets an admin
 * write or update the answer via the answer-update mutation.
 */
export default function GoodsInquiryAnswerDialog({
  inquiry,
  onClose,
  onAnswered,
}: GoodsInquiryAnswerDialogProps) {
  const [answerContent, setAnswerContent] = useState(
    inquiry.answerContent ?? "",
  );
  const [error, setError] = useState<string | null>(null);

  const answerMutation = useGoodsInquiryAnswerUpdate();

  const isAnswered =
    inquiry.answerStatus === GoodsInquiryDtoAnswerStatus.ANSWERED;

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (inquiry.id == null) {
      return;
    }
    const trimmed = answerContent.trim();
    if (!trimmed) {
      setError("답변 내용을 입력해 주세요.");
      return;
    }
    setError(null);
    answerMutation.mutate(
      { id: inquiry.id, data: { answerContent: trimmed } },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            onAnswered();
          } else {
            setError(response.resultMessage ?? "답변 등록에 실패했습니다.");
          }
        },
        onError: () => setError("답변 등록 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-lg bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-900">문의 답변</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="닫기"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-5 py-4">
          <div className="mb-4 space-y-3 rounded-md bg-slate-50 p-4">
            <div>
              <span className="text-xs font-medium text-slate-500">제목</span>
              <p className="mt-0.5 text-sm font-medium text-slate-900">
                {inquiry.title ?? "-"}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-slate-500">작성자</span>
              <p className="mt-0.5 text-sm text-slate-700">
                {inquiry.memberName ?? "-"}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-slate-500">
                문의 내용
              </span>
              <p className="mt-0.5 whitespace-pre-wrap text-sm text-slate-700">
                {inquiry.content ?? "-"}
              </p>
            </div>
          </div>

          <div className="mb-4">
            <label
              htmlFor="answer-content"
              className="mb-1 block text-xs font-medium text-slate-600"
            >
              답변 내용
            </label>
            <textarea
              id="answer-content"
              value={answerContent}
              onChange={(event) => setAnswerContent(event.target.value)}
              rows={5}
              placeholder="답변을 입력하세요."
              className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
            />
          </div>

          {error && (
            <p className="mb-3 text-sm text-red-600">{error}</p>
          )}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={answerMutation.isPending}
              className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
            >
              {answerMutation.isPending && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              {isAnswered ? "답변 수정" : "답변 등록"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
