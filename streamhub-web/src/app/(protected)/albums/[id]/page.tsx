"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Loader2 } from "lucide-react";

import {
  useAlbumDelete,
  useAlbumDetail,
  useAlbumUpdate,
} from "@/apis/query/album/album";
import {
  type AlbumCreateRequest,
  type AlbumDetail,
} from "@/apis/query/streamHubAdminAPI.schemas";
import { formatDate } from "@/lib/format";
import {
  AlbumGenreBadge,
  AlbumStatusBadge,
} from "@/components/albums/AlbumStatusBadge";
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

interface ReadonlyFieldProps {
  label: string;
  value: React.ReactNode;
}

function ReadonlyField({ label, value }: ReadonlyFieldProps) {
  return (
    <div>
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <div className="mt-1 text-sm text-slate-900">{value}</div>
    </div>
  );
}

function money(value?: number | null): string {
  return value != null ? Number(value).toLocaleString() : "-";
}

/** Formats a track duration in seconds as `m:ss`. */
function duration(value?: number | null): string {
  if (value == null) {
    return "-";
  }
  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export default function AlbumDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const albumId = Number(params.id);

  const [isEditing, setIsEditing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [cover, setCover] = useState<{ key: string; url: string } | null>(null);

  const detailQuery = useAlbumDetail(albumId, {
    query: { enabled: Number.isFinite(albumId) },
  });
  const updateMutation = useAlbumUpdate();
  const deleteMutation = useAlbumDelete();

  const detail: AlbumDetail | undefined = detailQuery.data?.resultObject;

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<AlbumFormValues>({
    resolver: zodResolver(albumFormSchema),
    defaultValues: buildAlbumDefaults(),
  });

  // Sync form values whenever the fetched detail changes.
  useEffect(() => {
    if (detail) {
      reset(buildAlbumDefaults(detail));
    }
  }, [detail, reset]);

  const startEditing = () => {
    setMessage(null);
    reset(buildAlbumDefaults(detail));
    setCover(null);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    reset(buildAlbumDefaults(detail));
    setCover(null);
    setIsEditing(false);
  };

  const onSubmit = (values: AlbumFormValues) => {
    setMessage(null);

    // Keep the existing cover key unless the user uploaded a replacement.
    const coverKey = cover?.key ?? detail?.coverKey;

    const payload: AlbumCreateRequest = buildAlbumPayload(values, coverKey);

    updateMutation.mutate(
      { id: albumId, data: payload },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("저장되었습니다.");
            setIsEditing(false);
            setCover(null);
            detailQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "저장에 실패했습니다.");
          }
        },
        onError: () => setMessage("저장 중 오류가 발생했습니다."),
      },
    );
  };

  const handleDelete = () => {
    if (!window.confirm("이 앨범을 삭제하시겠습니까?")) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: albumId },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            router.push("/albums");
          } else {
            setMessage(response.resultMessage ?? "삭제에 실패했습니다.");
          }
        },
        onError: () => setMessage("삭제 중 오류가 발생했습니다."),
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
        <h1 className="text-xl font-semibold text-slate-900">앨범 상세</h1>
      </div>

      {detailQuery.isPending ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : detailQuery.isError || !detail ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">앨범 정보를 불러오지 못했습니다.</p>
        </div>
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="rounded-md border border-slate-200 bg-white p-6"
          noValidate
        >
          {message && (
            <p className="mb-4 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700">
              {message}
            </p>
          )}

          {/* Cover */}
          <div className="mb-6">
            <p className="mb-1 text-xs font-medium text-slate-500">커버 이미지</p>
            {isEditing ? (
              <ThumbnailUpload
                previewUrl={cover?.url ?? detail.coverUrl ?? undefined}
                onUploaded={setCover}
                onClear={() => setCover(null)}
              />
            ) : detail.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={detail.coverUrl}
                alt="커버"
                className="h-40 w-40 rounded-md object-cover"
              />
            ) : (
              <div className="flex h-40 w-40 items-center justify-center rounded-md bg-slate-100 text-sm text-slate-400">
                커버 없음
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {/* Title */}
            <div className="sm:col-span-2">
              <label
                htmlFor="title"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                앨범명
              </label>
              {isEditing ? (
                <>
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
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.title ?? "-"}
                </p>
              )}
            </div>

            {/* Artist */}
            <div>
              <label
                htmlFor="artist"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                아티스트
              </label>
              {isEditing ? (
                <>
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
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.artist ?? "-"}
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
              {isEditing ? (
                <input
                  id="label"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("label")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.label ?? "-"}
                </p>
              )}
            </div>

            {/* Genre */}
            <div>
              <label
                htmlFor="genre"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                장르
              </label>
              {isEditing ? (
                <select
                  id="genre"
                  className={FIELD_CLASS}
                  {...register("genre")}
                >
                  {GENRE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="mt-1">
                  <AlbumGenreBadge value={detail.genre ?? undefined} />
                </div>
              )}
            </div>

            {/* Release date */}
            <div>
              <label
                htmlFor="releaseDate"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                발매일
              </label>
              {isEditing ? (
                <input
                  id="releaseDate"
                  type="date"
                  className={FIELD_CLASS}
                  {...register("releaseDate")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.releaseDate ?? "-"}
                </p>
              )}
            </div>

            {/* Price */}
            <div>
              <label
                htmlFor="price"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                가격
              </label>
              {isEditing ? (
                <>
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
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {money(detail.price)}원
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
              {isEditing ? (
                <input
                  id="stock"
                  type="number"
                  min={0}
                  className={FIELD_CLASS}
                  {...register("stock")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {money(detail.stock)}
                </p>
              )}
            </div>

            {/* Status */}
            <div>
              <label
                htmlFor="status"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                상태
              </label>
              {isEditing ? (
                <select
                  id="status"
                  className={FIELD_CLASS}
                  {...register("status")}
                >
                  <option value="ON_SALE">판매중</option>
                  <option value="HIDDEN">숨김</option>
                </select>
              ) : (
                <div className="mt-1">
                  <AlbumStatusBadge value={detail.status ?? undefined} />
                </div>
              )}
            </div>

            {/* Description */}
            <div className="sm:col-span-2">
              <label
                htmlFor="description"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                설명
              </label>
              {isEditing ? (
                <textarea
                  id="description"
                  rows={3}
                  className={FIELD_CLASS}
                  {...register("description")}
                />
              ) : (
                <p className="mt-1 whitespace-pre-wrap text-sm text-slate-900">
                  {detail.description ?? "-"}
                </p>
              )}
            </div>
          </div>

          <hr className="my-6 border-slate-200" />

          {/* Tracks */}
          {isEditing ? (
            <TrackRows control={control} register={register} />
          ) : (
            <div>
              <p className="mb-2 text-xs font-medium text-slate-500">
                트랙리스트
              </p>
              {detail.tracks && detail.tracks.length > 0 ? (
                <ul className="divide-y divide-slate-100 rounded-md border border-slate-200">
                  {detail.tracks.map((track, index) => (
                    <li
                      key={track.id ?? index}
                      className="flex items-center gap-3 px-3 py-2 text-sm text-slate-900"
                    >
                      <span className="w-6 text-right text-xs text-slate-400">
                        {track.trackNo ?? index + 1}
                      </span>
                      <span className="flex-1 font-medium">
                        {track.title ?? "-"}
                      </span>
                      <span className="text-xs text-slate-500">
                        {duration(track.durationSec)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-slate-400">등록된 트랙이 없습니다.</p>
              )}
            </div>
          )}

          <hr className="my-6 border-slate-200" />

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
            <ReadonlyField
              label="트랙수"
              value={(detail.trackCount ?? 0).toLocaleString()}
            />
            <ReadonlyField
              label="조회수"
              value={(detail.viewCount ?? 0).toLocaleString()}
            />
            <ReadonlyField label="등록일" value={formatDate(detail.createdAt)} />
          </div>

          <div className="mt-6 flex items-center justify-between gap-2">
            <div>
              {!isEditing && (
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                  className="flex items-center gap-1.5 rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {deleteMutation.isPending && (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  )}
                  삭제
                </button>
              )}
            </div>

            <div className="flex gap-2">
              {isEditing ? (
                <>
                  <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={updateMutation.isPending}
                    className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                  >
                    취소
                  </button>
                  <button
                    type="submit"
                    disabled={updateMutation.isPending}
                    className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {updateMutation.isPending && (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    )}
                    저장
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={startEditing}
                  className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
                >
                  수정
                </button>
              )}
            </div>
          </div>
        </form>
      )}
    </div>
  );
}
