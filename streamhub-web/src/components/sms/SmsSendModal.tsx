"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, X } from "lucide-react";

import { useSmsSendCreate } from "@/apis/query/sms/sms";
import type { SmsSendRequest } from "@/apis/query/streamHubAdminAPI.schemas";
import { FIELD_CLASS } from "@/lib/goods-form";
import { SUCCESS_CODE } from "@/types/api";

// SmsSendRequest.content allows 0–2000 chars; we require at least 1 so an empty
// message can't be queued. The phone field stays lenient (digits, -, +, spaces)
// because the backend normalises it and never actually dials out in this clone.
const smsSendSchema = z.object({
  toNumber: z
    .string()
    .trim()
    .min(1, "수신번호를 입력하세요.")
    .regex(/^[0-9+\- ]+$/, "숫자와 - 만 입력할 수 있습니다."),
  content: z
    .string()
    .trim()
    .min(1, "내용을 입력하세요.")
    .max(2000, "내용은 2000자 이내로 입력하세요."),
});

type SmsSendFormValues = z.infer<typeof smsSendSchema>;

interface SmsSendModalProps {
  onClose: () => void;
  /** Called after a send is logged so the parent can refetch the history list. */
  onSent: () => void;
}

/**
 * SmsSendModal is the 커스텀 발송 form. It posts toNumber + content via
 * useSmsSendCreate. The clone never sends a real message — the row is logged in
 * test mode only — which the modal makes explicit to the operator.
 */
export default function SmsSendModal({ onClose, onSent }: SmsSendModalProps) {
  const sendMutation = useSmsSendCreate();

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors },
  } = useForm<SmsSendFormValues>({
    resolver: zodResolver(smsSendSchema),
    defaultValues: { toNumber: "", content: "" },
  });

  const contentLength = watch("content")?.length ?? 0;

  const onSubmit = (values: SmsSendFormValues) => {
    const payload: SmsSendRequest = {
      toNumber: values.toNumber.trim(),
      content: values.content.trim(),
    };

    sendMutation.mutate(
      { data: payload },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            onSent();
            onClose();
          } else {
            setError("root", {
              message: response.resultMessage ?? "발송에 실패했습니다.",
            });
          }
        },
        onError: () =>
          setError("root", { message: "발송 중 오류가 발생했습니다." }),
      },
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-md rounded-md bg-white p-6 shadow-lg">
        <div className="mb-1 flex items-start justify-between">
          <h3 className="text-base font-semibold text-slate-900">
            커스텀 문자 발송
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="닫기"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <p className="mb-4 inline-flex items-center rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800">
          데모/테스트 발송 · 실제로 전송되지 않고 내역만 기록됩니다
        </p>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          {errors.root && (
            <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {errors.root.message}
            </p>
          )}

          <div className="space-y-4">
            <div>
              <label
                htmlFor="toNumber"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                수신번호 *
              </label>
              <input
                id="toNumber"
                type="text"
                placeholder="예: 010-1234-5678"
                className={FIELD_CLASS}
                {...register("toNumber")}
              />
              {errors.toNumber && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.toNumber.message}
                </p>
              )}
            </div>

            <div>
              <div className="mb-1 flex items-center justify-between">
                <label
                  htmlFor="content"
                  className="block text-xs font-medium text-slate-500"
                >
                  내용 *
                </label>
                <span className="text-xs text-slate-400">
                  {contentLength} / 2000
                </span>
              </div>
              <textarea
                id="content"
                rows={5}
                placeholder="발송할 메시지를 입력하세요."
                className={FIELD_CLASS}
                {...register("content")}
              />
              {errors.content && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.content.message}
                </p>
              )}
            </div>
          </div>

          <div className="mt-6 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={sendMutation.isPending}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={sendMutation.isPending}
              className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {sendMutation.isPending && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              발송
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
