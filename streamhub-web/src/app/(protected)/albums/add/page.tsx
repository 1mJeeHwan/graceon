"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Loader2 } from "lucide-react";

import { useAlbumCreate } from "@/apis/query/album/album";
import { type AlbumCreateRequest } from "@/apis/query/graceOnAdminAPI.schemas";
import ThumbnailUpload from "@/components/content/ThumbnailUpload";
import TrackRows from "@/components/albums/TrackRows";
import {
  FIELD_CLASS,
  GENRE_OPTIONS,
  albumFormSchema,
  buildAlbumDefaults,
  buildAlbumPayload,
  type AlbumFormValues,
} from "@/lib/album-form";
import { SUCCESS_CODE } from "@/types/api";

export default function AlbumAddPage() {
  const router = useRouter();
  const [message, setMessage] = useState<string | null>(null);
  const [cover, setCover] = useState<{ key: string; url: string } | null>(null);

  const createMutation = useAlbumCreate();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<AlbumFormValues>({
    resolver: zodResolver(albumFormSchema),
    defaultValues: buildAlbumDefaults(),
  });

  const onSubmit = (values: AlbumFormValues) => {
    setMessage(null);

    const payload: AlbumCreateRequest = buildAlbumPayload(values, cover?.key);

    createMutation.mutate(
      { data: payload },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            const newId = response.resultObject?.id;
            if (newId != null) {
              router.push(`/albums/${newId}`);
            } else {
              router.push("/albums");
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
        href="/albums"
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-600 transition hover:text-slate-900"
      >
        <ArrowLeft className="h-4 w-4" />
        목록으로
      </Link>

      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-900">앨범 등록</h1>
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
          {/* Cover */}
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">
              커버 이미지
            </label>
            <ThumbnailUpload
              previewUrl={cover?.url}
              onUploaded={setCover}
              onClear={() => setCover(null)}
            />
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {/* Title */}
            <div className="sm:col-span-2">
              <label
                htmlFor="title"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                앨범명 *
              </label>
              <input
                id="title"
                type="text"
                className={FIELD_CLASS}
                {...register("title")}
              />
              {errors.title && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.title.message}
                </p>
              )}
            </div>

            {/* Artist */}
            <div>
              <label
                htmlFor="artist"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                아티스트 *
              </label>
              <input
                id="artist"
                type="text"
                className={FIELD_CLASS}
                {...register("artist")}
              />
              {errors.artist && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.artist.message}
                </p>
              )}
            </div>

            {/* Label */}
            <div>
              <label
                htmlFor="label"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                레이블
              </label>
              <input
                id="label"
                type="text"
                className={FIELD_CLASS}
                {...register("label")}
              />
            </div>

            {/* Genre */}
            <div>
              <label
                htmlFor="genre"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                장르 *
              </label>
              <select id="genre" className={FIELD_CLASS} {...register("genre")}>
                {GENRE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Release date */}
            <div>
              <label
                htmlFor="releaseDate"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                발매일
              </label>
              <input
                id="releaseDate"
                type="date"
                className={FIELD_CLASS}
                {...register("releaseDate")}
              />
            </div>

            {/* Price */}
            <div>
              <label
                htmlFor="price"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                가격 *
              </label>
              <input
                id="price"
                type="number"
                min={0}
                className={FIELD_CLASS}
                {...register("price")}
              />
              {errors.price && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.price.message}
                </p>
              )}
            </div>

            {/* Stock */}
            <div>
              <label
                htmlFor="stock"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                재고
              </label>
              <input
                id="stock"
                type="number"
                min={0}
                className={FIELD_CLASS}
                {...register("stock")}
              />
            </div>

            {/* Status */}
            <div>
              <label
                htmlFor="status"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                상태
              </label>
              <select
                id="status"
                className={FIELD_CLASS}
                {...register("status")}
              >
                <option value="ON_SALE">판매중</option>
                <option value="HIDDEN">숨김</option>
              </select>
            </div>

            {/* Description */}
            <div className="sm:col-span-2">
              <label
                htmlFor="description"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                설명
              </label>
              <textarea
                id="description"
                rows={3}
                className={FIELD_CLASS}
                {...register("description")}
              />
            </div>
          </div>

          <hr className="border-slate-200" />

          {/* Dynamic track rows */}
          <TrackRows control={control} register={register} />
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Link
            href="/albums"
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
