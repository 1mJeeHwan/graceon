"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, X } from "lucide-react";

import { useStoreCreate, useStoreUpdate } from "@/apis/query/store/store";
import { type StoreDto } from "@/apis/query/graceOnAdminAPI.schemas";
import {
  FIELD_CLASS,
  buildStoreDefaults,
  buildStorePayload,
  storeFormSchema,
  type StoreFormValues,
} from "@/lib/store-form";
import { SUCCESS_CODE } from "@/types/api";

interface StoreFormDialogProps {
  /** Store being edited, or null/undefined when creating a new one. */
  store?: StoreDto | null;
  onClose: () => void;
  /** Called after a successful create/update so the parent can refetch. */
  onSaved: () => void;
}

/**
 * StoreFormDialog is a modal create/edit form for a store. When `store` has an
 * id it issues an update; otherwise it creates a new store. Coordinates
 * (위도/경도) are optional numeric inputs.
 */
export default function StoreFormDialog({
  store,
  onClose,
  onSaved,
}: StoreFormDialogProps) {
  const isEdit = store?.id != null;
  const [message, setMessage] = useState<string | null>(null);

  const createMutation = useStoreCreate();
  const updateMutation = useStoreUpdate();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StoreFormValues>({
    resolver: zodResolver(storeFormSchema),
    defaultValues: buildStoreDefaults(store ?? undefined),
  });

  useEffect(() => {
    reset(buildStoreDefaults(store ?? undefined));
  }, [store, reset]);

  const handleResult = (resultCode?: string, resultMessage?: string) => {
    if (resultCode === SUCCESS_CODE) {
      onSaved();
    } else {
      setMessage(resultMessage ?? "저장에 실패했습니다.");
    }
  };

  const onSubmit = (values: StoreFormValues) => {
    setMessage(null);
    const payload: StoreDto = buildStorePayload(values);

    if (isEdit && store?.id != null) {
      updateMutation.mutate(
        { id: store.id, data: payload },
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
            {isEdit ? "매장 수정" : "매장 등록"}
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
            {/* Name */}
            <div className="sm:col-span-2">
              <label
                htmlFor="store-name"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                매장명 *
              </label>
              <input
                id="store-name"
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

            {/* Region id */}
            <div>
              <label
                htmlFor="store-region"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                지역 ID
              </label>
              <input
                id="store-region"
                type="number"
                min={0}
                className={FIELD_CLASS}
                {...register("regionId")}
              />
            </div>

            {/* Phone */}
            <div>
              <label
                htmlFor="store-phone"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                전화번호
              </label>
              <input
                id="store-phone"
                type="text"
                className={FIELD_CLASS}
                {...register("phone")}
              />
            </div>

            {/* Address */}
            <div className="sm:col-span-2">
              <label
                htmlFor="store-address"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                주소
              </label>
              <input
                id="store-address"
                type="text"
                className={FIELD_CLASS}
                {...register("address")}
              />
            </div>

            {/* Lat */}
            <div>
              <label
                htmlFor="store-lat"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                위도 (lat)
              </label>
              <input
                id="store-lat"
                type="text"
                inputMode="decimal"
                placeholder="예: 37.5665"
                className={FIELD_CLASS}
                {...register("lat")}
              />
              {errors.lat && (
                <p className="mt-1 text-xs text-red-600">{errors.lat.message}</p>
              )}
            </div>

            {/* Lng */}
            <div>
              <label
                htmlFor="store-lng"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                경도 (lng)
              </label>
              <input
                id="store-lng"
                type="text"
                inputMode="decimal"
                placeholder="예: 126.9780"
                className={FIELD_CLASS}
                {...register("lng")}
              />
              {errors.lng && (
                <p className="mt-1 text-xs text-red-600">{errors.lng.message}</p>
              )}
            </div>

            {/* Open hours */}
            <div>
              <label
                htmlFor="store-hours"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                영업시간
              </label>
              <input
                id="store-hours"
                type="text"
                placeholder="예: 10:00 - 21:00"
                className={FIELD_CLASS}
                {...register("openHours")}
              />
            </div>

            {/* Use yn */}
            <div>
              <label
                htmlFor="store-use"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                노출여부
              </label>
              <select
                id="store-use"
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
