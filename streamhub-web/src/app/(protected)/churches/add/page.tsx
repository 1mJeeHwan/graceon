"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Loader2 } from "lucide-react";

import { useChurchChurchesCreate } from "@/apis/query/church/church";
import { type ChurchUpsertRequest } from "@/apis/query/graceOnAdminAPI.schemas";
import ThumbnailUpload from "@/components/content/ThumbnailUpload";
import WorshipTimeRows from "@/components/churches/WorshipTimeRows";
import {
  DENOMINATION_CODES,
  DENOMINATION_LABELS,
  FIELD_CLASS,
  buildChurchDefaults,
  buildChurchPayload,
  churchFormSchema,
  type ChurchFormValues,
} from "@/lib/church-form";
import { SUCCESS_CODE } from "@/types/api";

export default function ChurchAddPage() {
  const router = useRouter();
  const [message, setMessage] = useState<string | null>(null);
  const [thumbnail, setThumbnail] = useState<{ key: string; url: string } | null>(
    null,
  );

  const createMutation = useChurchChurchesCreate();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ChurchFormValues>({
    resolver: zodResolver(churchFormSchema),
    defaultValues: buildChurchDefaults(),
  });

  const onSubmit = (values: ChurchFormValues) => {
    setMessage(null);

    const payload: ChurchUpsertRequest = buildChurchPayload(
      values,
      thumbnail?.key,
    );

    createMutation.mutate(
      { data: payload },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            const newId = response.resultObject?.id;
            if (newId != null) {
              router.push(`/churches/${newId}`);
            } else {
              router.push("/churches");
            }
          } else {
            setMessage(response.resultMessage ?? "등록에 실패했습니다.");
          }
        },
        onError: () => setMessage("등록 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        href="/churches"
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-600 transition hover:text-slate-900"
      >
        <ArrowLeft className="h-4 w-4" />
        목록으로
      </Link>

      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-900">교회 등록</h1>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="rounded-md border border-slate-200 bg-white p-6"
        noValidate
      >
        {message && (
          <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {message}
          </p>
        )}

        <div className="grid grid-cols-1 gap-5">
          {/* Thumbnail */}
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">
              대표 이미지
            </label>
            <ThumbnailUpload
              previewUrl={thumbnail?.url}
              onUploaded={setThumbnail}
              onClear={() => setThumbnail(null)}
            />
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {/* Name */}
            <div className="sm:col-span-2">
              <label
                htmlFor="name"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                교회명 *
              </label>
              <input
                id="name"
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

            {/* Denomination */}
            <div>
              <label
                htmlFor="denomination"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                교단 *
              </label>
              <select
                id="denomination"
                className={FIELD_CLASS}
                {...register("denomination")}
              >
                {DENOMINATION_CODES.map((code) => (
                  <option key={code} value={code}>
                    {DENOMINATION_LABELS[code] ?? code}
                  </option>
                ))}
              </select>
            </div>

            {/* Region ID */}
            <div>
              <label
                htmlFor="regionId"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                지역 ID *
              </label>
              <input
                id="regionId"
                type="number"
                min={0}
                placeholder="예: 1100"
                className={FIELD_CLASS}
                {...register("regionId")}
              />
              {errors.regionId && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.regionId.message}
                </p>
              )}
            </div>

            {/* Latitude */}
            <div>
              <label
                htmlFor="latitude"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                위도 (latitude)
              </label>
              <input
                id="latitude"
                type="number"
                step="any"
                placeholder="-90 ~ 90"
                className={FIELD_CLASS}
                {...register("latitude")}
              />
              {errors.latitude && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.latitude.message}
                </p>
              )}
            </div>

            {/* Longitude */}
            <div>
              <label
                htmlFor="longitude"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                경도 (longitude)
              </label>
              <input
                id="longitude"
                type="number"
                step="any"
                placeholder="-180 ~ 180"
                className={FIELD_CLASS}
                {...register("longitude")}
              />
              {errors.longitude && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.longitude.message}
                </p>
              )}
            </div>

            {/* Zipcode */}
            <div>
              <label
                htmlFor="zipcode"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                우편번호
              </label>
              <input
                id="zipcode"
                type="text"
                className={FIELD_CLASS}
                {...register("zipcode")}
              />
            </div>

            {/* Phone */}
            <div>
              <label
                htmlFor="phone"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                전화번호
              </label>
              <input
                id="phone"
                type="text"
                placeholder="예: 02-1234-5678"
                className={FIELD_CLASS}
                {...register("phone")}
              />
            </div>

            {/* Address */}
            <div className="sm:col-span-2">
              <label
                htmlFor="address"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                주소
              </label>
              <input
                id="address"
                type="text"
                className={FIELD_CLASS}
                {...register("address")}
              />
            </div>

            {/* Address detail */}
            <div className="sm:col-span-2">
              <label
                htmlFor="addressDetail"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                상세주소
              </label>
              <input
                id="addressDetail"
                type="text"
                className={FIELD_CLASS}
                {...register("addressDetail")}
              />
            </div>

            {/* Pastor name */}
            <div>
              <label
                htmlFor="pastorName"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                담임목사
              </label>
              <input
                id="pastorName"
                type="text"
                className={FIELD_CLASS}
                {...register("pastorName")}
              />
            </div>

            {/* Homepage */}
            <div>
              <label
                htmlFor="homepageUrl"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                홈페이지
              </label>
              <input
                id="homepageUrl"
                type="text"
                placeholder="https://"
                className={FIELD_CLASS}
                {...register("homepageUrl")}
              />
            </div>

            {/* Open yn */}
            <div>
              <label
                htmlFor="openYn"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                공개여부
              </label>
              <select
                id="openYn"
                className={FIELD_CLASS}
                {...register("openYn")}
              >
                <option value="Y">공개</option>
                <option value="N">비공개</option>
              </select>
            </div>

            {/* Use yn */}
            <div>
              <label
                htmlFor="useYn"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                사용여부
              </label>
              <select id="useYn" className={FIELD_CLASS} {...register("useYn")}>
                <option value="Y">사용</option>
                <option value="N">미사용</option>
              </select>
            </div>

            {/* Facilities */}
            <div className="sm:col-span-2">
              <label
                htmlFor="facilities"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                시설 (쉼표로 구분)
              </label>
              <input
                id="facilities"
                type="text"
                placeholder="예: 주차장, 카페, 유아실"
                className={FIELD_CLASS}
                {...register("facilities")}
              />
            </div>

            {/* Introduction */}
            <div className="sm:col-span-2">
              <label
                htmlFor="introduction"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                소개
              </label>
              <textarea
                id="introduction"
                rows={3}
                className={FIELD_CLASS}
                {...register("introduction")}
              />
            </div>
          </div>

          <hr className="border-slate-200" />

          {/* Dynamic worship-time rows */}
          <WorshipTimeRows control={control} register={register} />
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Link
            href="/churches"
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
          >
            취소
          </Link>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
          >
            {createMutation.isPending && (
              <Loader2 className="h-4 w-4 animate-spin" />
            )}
            등록
          </button>
        </div>
      </form>
    </div>
  );
}
