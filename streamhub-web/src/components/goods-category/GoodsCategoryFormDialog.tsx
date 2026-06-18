"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Loader2, X } from "lucide-react";

import {
  useGoodsCategoryCreate,
  useGoodsCategoryUpdate,
} from "@/apis/query/goods-category/goods-category";
import {
  type GoodsCategoryNodeDto,
  type GoodsCategorySaveRequest,
} from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

interface CategoryFormValues {
  name: string;
  sortOrder: string;
  useYn: string;
}

interface GoodsCategoryFormDialogProps {
  /** Category being edited, or null/undefined when creating a new one. */
  category?: GoodsCategoryNodeDto | null;
  /** Parent category for a new child node; null/undefined creates a root node. */
  parent?: GoodsCategoryNodeDto | null;
  onClose: () => void;
  /** Called after a successful create/update so the parent can refetch. */
  onSaved: () => void;
}

/**
 * GoodsCategoryFormDialog is a modal create/edit form for a goods category.
 * When `category` has an id it issues an update (name/sortOrder/useYn only);
 * otherwise it creates a new node under `parent` (or at root when no parent).
 */
export default function GoodsCategoryFormDialog({
  category,
  parent,
  onClose,
  onSaved,
}: GoodsCategoryFormDialogProps) {
  const isEdit = category?.id != null;
  const [message, setMessage] = useState<string | null>(null);

  const createMutation = useGoodsCategoryCreate();
  const updateMutation = useGoodsCategoryUpdate();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    defaultValues: {
      name: category?.name ?? "",
      sortOrder: category?.sortOrder != null ? String(category.sortOrder) : "0",
      useYn: category?.useYn ?? "Y",
    },
  });

  useEffect(() => {
    reset({
      name: category?.name ?? "",
      sortOrder: category?.sortOrder != null ? String(category.sortOrder) : "0",
      useYn: category?.useYn ?? "Y",
    });
  }, [category, reset]);

  const handleResult = (resultCode?: string, resultMessage?: string) => {
    if (resultCode === SUCCESS_CODE) {
      onSaved();
    } else {
      setMessage(resultMessage ?? "저장에 실패했습니다.");
    }
  };

  const onSubmit = (values: CategoryFormValues) => {
    setMessage(null);
    const parsedSort = Number(values.sortOrder);
    const payload: GoodsCategorySaveRequest = {
      name: values.name.trim(),
      sortOrder: Number.isFinite(parsedSort) ? parsedSort : 0,
      useYn: values.useYn,
    };

    if (isEdit && category?.id != null) {
      updateMutation.mutate(
        { id: category.id, data: payload },
        {
          onSuccess: (response) =>
            handleResult(response.resultCode, response.resultMessage),
          onError: () => setMessage("저장 중 오류가 발생했습니다."),
        },
      );
    } else {
      createMutation.mutate(
        { data: { ...payload, parentId: parent?.id } },
        {
          onSuccess: (response) =>
            handleResult(response.resultCode, response.resultMessage),
          onError: () => setMessage("저장 중 오류가 발생했습니다."),
        },
      );
    }
  };

  const title = isEdit
    ? "카테고리 수정"
    : parent?.id != null
      ? `하위 카테고리 등록 (상위: ${parent.name ?? "-"})`
      : "카테고리 등록";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-md rounded-md bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">{title}</h2>
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

          <div className="grid grid-cols-1 gap-4">
            {/* Name */}
            <div>
              <label
                htmlFor="category-name"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                카테고리명 *
              </label>
              <input
                id="category-name"
                type="text"
                className={FIELD_CLASS}
                {...register("name", { required: "카테고리명을 입력하세요." })}
              />
              {errors.name && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.name.message}
                </p>
              )}
            </div>

            {/* Sort order */}
            <div>
              <label
                htmlFor="category-sort"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                정렬순서
              </label>
              <input
                id="category-sort"
                type="number"
                className={FIELD_CLASS}
                {...register("sortOrder")}
              />
            </div>

            {/* Use yn */}
            <div>
              <label
                htmlFor="category-use"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                노출여부
              </label>
              <select
                id="category-use"
                className={FIELD_CLASS}
                {...register("useYn")}
              >
                <option value="Y">노출</option>
                <option value="N">숨김</option>
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
