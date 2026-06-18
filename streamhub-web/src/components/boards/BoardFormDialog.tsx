"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, X } from "lucide-react";

import { useBoardCreate, useBoardUpdate } from "@/apis/query/board/board";
import { type BoardDto } from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

/** Level field validator: 1~10 정수만 허용. */
const levelSchema = z
  .string()
  .min(1, "레벨을 입력하세요.")
  .refine((value) => {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed >= 1 && parsed <= 10;
  }, "1~10 사이의 정수를 입력하세요.");

const boardFormSchema = z.object({
  code: z.string().min(1, "게시판 코드를 입력하세요."),
  name: z.string().min(1, "게시판명을 입력하세요."),
  readLevel: levelSchema,
  writeLevel: levelSchema,
  sortOrder: z.string().optional(),
  useYn: z.enum(["Y", "N"]),
});

type BoardFormValues = z.infer<typeof boardFormSchema>;

/** Converts an optional string input to a finite number, or undefined when blank. */
function toNumber(value?: string): number | undefined {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function buildDefaults(board?: BoardDto | null): BoardFormValues {
  return {
    code: board?.code ?? "",
    name: board?.name ?? "",
    readLevel: board?.readLevel != null ? String(board.readLevel) : "1",
    writeLevel: board?.writeLevel != null ? String(board.writeLevel) : "1",
    sortOrder: board?.sortOrder != null ? String(board.sortOrder) : "",
    useYn: board?.useYn === "N" ? "N" : "Y",
  };
}

function buildPayload(values: BoardFormValues): BoardDto {
  return {
    code: values.code.trim(),
    name: values.name.trim(),
    readLevel: toNumber(values.readLevel),
    writeLevel: toNumber(values.writeLevel),
    sortOrder: toNumber(values.sortOrder),
    useYn: values.useYn,
  };
}

interface BoardFormDialogProps {
  /** Board being edited, or null/undefined when creating a new one. */
  board?: BoardDto | null;
  onClose: () => void;
  /** Called after a successful create/update so the parent can refetch. */
  onSaved: () => void;
}

/**
 * BoardFormDialog is a modal create/edit form for a board. When `board` has an
 * id it issues an update; otherwise it creates a new board. 읽기/쓰기 권한은
 * 1~10 레벨로 관리합니다.
 */
export default function BoardFormDialog({
  board,
  onClose,
  onSaved,
}: BoardFormDialogProps) {
  const isEdit = board?.id != null;
  const [message, setMessage] = useState<string | null>(null);

  const createMutation = useBoardCreate();
  const updateMutation = useBoardUpdate();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<BoardFormValues>({
    resolver: zodResolver(boardFormSchema),
    defaultValues: buildDefaults(board),
  });

  useEffect(() => {
    reset(buildDefaults(board));
  }, [board, reset]);

  const handleResult = (resultCode?: string, resultMessage?: string) => {
    if (resultCode === SUCCESS_CODE) {
      onSaved();
    } else {
      setMessage(resultMessage ?? "저장에 실패했습니다.");
    }
  };

  const onSubmit = (values: BoardFormValues) => {
    setMessage(null);
    const payload: BoardDto = buildPayload(values);

    if (isEdit && board?.id != null) {
      updateMutation.mutate(
        { id: board.id, data: payload },
        {
          onSuccess: (response) =>
            handleResult(response.resultCode, response.resultMessage),
          onError: () => setMessage("저장 중 오류가 발생했습니다."),
        },
      );
    } else {
      createMutation.mutate(
        { data: payload },
        {
          onSuccess: (response) =>
            handleResult(response.resultCode, response.resultMessage),
          onError: () => setMessage("저장 중 오류가 발생했습니다."),
        },
      );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-lg rounded-md bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">
            {isEdit ? "게시판 수정" : "게시판 등록"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="닫기"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="px-5 py-4" noValidate>
          {message && (
            <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {message}
            </p>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {/* Code */}
            <div>
              <label
                htmlFor="board-code"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                게시판 코드 *
              </label>
              <input
                id="board-code"
                type="text"
                className={FIELD_CLASS}
                {...register("code")}
              />
              {errors.code && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.code.message}
                </p>
              )}
            </div>

            {/* Name */}
            <div>
              <label
                htmlFor="board-name"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                게시판명 *
              </label>
              <input
                id="board-name"
                type="text"
                className={FIELD_CLASS}
                {...register("name")}
              />
              {errors.name && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.name.message}
                </p>
              )}
            </div>

            {/* Read level */}
            <div>
              <label
                htmlFor="board-read-level"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                읽기 권한 (1~10) *
              </label>
              <input
                id="board-read-level"
                type="number"
                min={1}
                max={10}
                className={FIELD_CLASS}
                {...register("readLevel")}
              />
              {errors.readLevel && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.readLevel.message}
                </p>
              )}
            </div>

            {/* Write level */}
            <div>
              <label
                htmlFor="board-write-level"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                쓰기 권한 (1~10) *
              </label>
              <input
                id="board-write-level"
                type="number"
                min={1}
                max={10}
                className={FIELD_CLASS}
                {...register("writeLevel")}
              />
              {errors.writeLevel && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.writeLevel.message}
                </p>
              )}
            </div>

            {/* Sort order */}
            <div>
              <label
                htmlFor="board-sort"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                정렬 순서
              </label>
              <input
                id="board-sort"
                type="number"
                min={0}
                className={FIELD_CLASS}
                {...register("sortOrder")}
              />
            </div>

            {/* Use yn */}
            <div>
              <label
                htmlFor="board-use"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                사용여부
              </label>
              <select
                id="board-use"
                className={FIELD_CLASS}
                {...register("useYn")}
              >
                <option value="Y">사용</option>
                <option value="N">미사용</option>
              </select>
            </div>
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
              저장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
