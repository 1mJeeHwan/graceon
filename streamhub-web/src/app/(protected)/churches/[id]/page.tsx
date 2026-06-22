"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Loader2 } from "lucide-react";

import {
  useChurchChurchesDelete,
  useChurchChurchesDetail,
  useChurchChurchesUpdate,
} from "@/apis/query/church/church";
import {
  type ChurchDetail,
  type ChurchUpsertRequest,
} from "@/apis/query/streamHubAdminAPI.schemas";
import { formatDate } from "@/lib/format";
import {
  DenominationBadge,
  OpenBadge,
  UseYnBadge,
} from "@/components/churches/ChurchBadges";
import ThumbnailUpload from "@/components/content/ThumbnailUpload";
import WorshipTimeRows from "@/components/churches/WorshipTimeRows";
import {
  DENOMINATION_CODES,
  DENOMINATION_LABELS,
  FIELD_CLASS,
  WORSHIP_KIND_LABELS,
  buildChurchDefaults,
  buildChurchPayload,
  churchFormSchema,
  type ChurchFormValues,
} from "@/lib/church-form";
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

export default function ChurchDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const churchId = Number(params.id);

  const [isEditing, setIsEditing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [thumbnail, setThumbnail] = useState<{ key: string; url: string } | null>(
    null,
  );

  const detailQuery = useChurchChurchesDetail(churchId, {
    query: { enabled: Number.isFinite(churchId) },
  });
  const updateMutation = useChurchChurchesUpdate();
  const deleteMutation = useChurchChurchesDelete();

  const detail: ChurchDetail | undefined = detailQuery.data?.resultObject;

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<ChurchFormValues>({
    resolver: zodResolver(churchFormSchema),
    defaultValues: buildChurchDefaults(),
  });

  // Sync form values whenever the fetched detail changes.
  useEffect(() => {
    if (detail) {
      reset(buildChurchDefaults(detail));
    }
  }, [detail, reset]);

  const startEditing = () => {
    setMessage(null);
    reset(buildChurchDefaults(detail));
    setThumbnail(null);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    reset(buildChurchDefaults(detail));
    setThumbnail(null);
    setIsEditing(false);
  };

  const onSubmit = (values: ChurchFormValues) => {
    setMessage(null);

    // Keep the existing thumbnail key unless the user uploaded a replacement.
    const thumbnailKey = thumbnail?.key ?? detail?.thumbnailKey;

    const payload: ChurchUpsertRequest = buildChurchPayload(
      values,
      thumbnailKey,
    );

    updateMutation.mutate(
      { id: churchId, data: payload },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("저장되었습니다.");
            setIsEditing(false);
            setThumbnail(null);
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
    if (!window.confirm("이 교회를 삭제하시겠습니까?")) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: churchId },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            router.push("/churches");
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
        href="/churches"
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-600 transition hover:text-slate-900"
      >
        <ArrowLeft className="h-4 w-4" />
        목록으로
      </Link>

      <div className="mb-4 flex items-center justify-between gap-2">
        <h1 className="text-xl font-semibold text-slate-900">교회 상세</h1>
        {detail && (
          <div className="flex gap-2">
            <Link
              href={`/member?churchId=${detail.id}`}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
              title="이 교회 소속 회원 목록"
            >
              소속 회원 {detail.memberCount?.toLocaleString() ?? 0}명
            </Link>
            <Link
              href={`/worship?churchId=${detail.id}`}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
              title="이 교회 예배신청 목록"
            >
              예배신청 {detail.worshipRegistrationCount?.toLocaleString() ?? 0}건
            </Link>
          </div>
        )}
      </div>

      {detailQuery.isPending ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : detailQuery.isError || !detail ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">교회 정보를 불러오지 못했습니다.</p>
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

          {/* Thumbnail */}
          <div className="mb-6">
            <p className="mb-1 text-xs font-medium text-slate-500">대표 이미지</p>
            {isEditing ? (
              <ThumbnailUpload
                previewUrl={thumbnail?.url ?? detail.thumbnailUrl ?? undefined}
                onUploaded={setThumbnail}
                onClear={() => setThumbnail(null)}
              />
            ) : detail.thumbnailUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={detail.thumbnailUrl}
                alt="교회 이미지"
                className="h-40 w-full rounded-md object-contain"
              />
            ) : (
              <div className="flex h-40 w-full items-center justify-center rounded-md bg-slate-100 text-sm text-slate-400">
                이미지 없음
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {/* Name */}
            <div className="sm:col-span-2">
              <label
                htmlFor="name"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                교회명
              </label>
              {isEditing ? (
                <>
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
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.name ?? "-"}
                </p>
              )}
            </div>

            {/* Denomination */}
            <div>
              <label
                htmlFor="denomination"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                교단
              </label>
              {isEditing ? (
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
              ) : (
                <div className="mt-1">
                  <DenominationBadge value={detail.denomination ?? undefined} />
                </div>
              )}
            </div>

            {/* Region */}
            <div>
              <label
                htmlFor="regionId"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                지역 {isEditing ? "ID" : ""}
              </label>
              {isEditing ? (
                <>
                  <input
                    id="regionId"
                    type="number"
                    min={0}
                    className={FIELD_CLASS}
                    {...register("regionId")}
                  />
                  {errors.regionId && (
                    <p className="mt-1 text-xs text-red-600">
                      {errors.regionId.message}
                    </p>
                  )}
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.regionName ?? "-"}
                </p>
              )}
            </div>

            {/* Latitude */}
            <div>
              <label
                htmlFor="latitude"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                위도
              </label>
              {isEditing ? (
                <>
                  <input
                    id="latitude"
                    type="number"
                    step="any"
                    className={FIELD_CLASS}
                    {...register("latitude")}
                  />
                  {errors.latitude && (
                    <p className="mt-1 text-xs text-red-600">
                      {errors.latitude.message}
                    </p>
                  )}
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.latitude != null ? detail.latitude : "-"}
                </p>
              )}
            </div>

            {/* Longitude */}
            <div>
              <label
                htmlFor="longitude"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                경도
              </label>
              {isEditing ? (
                <>
                  <input
                    id="longitude"
                    type="number"
                    step="any"
                    className={FIELD_CLASS}
                    {...register("longitude")}
                  />
                  {errors.longitude && (
                    <p className="mt-1 text-xs text-red-600">
                      {errors.longitude.message}
                    </p>
                  )}
                </>
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.longitude != null ? detail.longitude : "-"}
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
              {isEditing ? (
                <input
                  id="zipcode"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("zipcode")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.zipcode ?? "-"}
                </p>
              )}
            </div>

            {/* Phone */}
            <div>
              <label
                htmlFor="phone"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                전화번호
              </label>
              {isEditing ? (
                <input
                  id="phone"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("phone")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.phone ?? "-"}
                </p>
              )}
            </div>

            {/* Address */}
            <div className="sm:col-span-2">
              <label
                htmlFor="address"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                주소
              </label>
              {isEditing ? (
                <input
                  id="address"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("address")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.address ?? "-"}
                </p>
              )}
            </div>

            {/* Address detail */}
            <div className="sm:col-span-2">
              <label
                htmlFor="addressDetail"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                상세주소
              </label>
              {isEditing ? (
                <input
                  id="addressDetail"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("addressDetail")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.addressDetail ?? "-"}
                </p>
              )}
            </div>

            {/* Pastor name */}
            <div>
              <label
                htmlFor="pastorName"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                담임목사
              </label>
              {isEditing ? (
                <input
                  id="pastorName"
                  type="text"
                  className={FIELD_CLASS}
                  {...register("pastorName")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.pastorName ?? "-"}
                </p>
              )}
            </div>

            {/* Homepage */}
            <div>
              <label
                htmlFor="homepageUrl"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                홈페이지
              </label>
              {isEditing ? (
                <input
                  id="homepageUrl"
                  type="text"
                  placeholder="https://"
                  className={FIELD_CLASS}
                  {...register("homepageUrl")}
                />
              ) : detail.homepageUrl ? (
                <a
                  href={detail.homepageUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 block text-sm text-brand hover:underline"
                >
                  {detail.homepageUrl}
                </a>
              ) : (
                <p className="mt-1 text-sm text-slate-900">-</p>
              )}
            </div>

            {/* Open yn */}
            <div>
              <label
                htmlFor="openYn"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                공개여부
              </label>
              {isEditing ? (
                <select
                  id="openYn"
                  className={FIELD_CLASS}
                  {...register("openYn")}
                >
                  <option value="Y">공개</option>
                  <option value="N">비공개</option>
                </select>
              ) : (
                <div className="mt-1">
                  <OpenBadge value={detail.openYn ?? undefined} />
                </div>
              )}
            </div>

            {/* Use yn */}
            <div>
              <label
                htmlFor="useYn"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                사용여부
              </label>
              {isEditing ? (
                <select
                  id="useYn"
                  className={FIELD_CLASS}
                  {...register("useYn")}
                >
                  <option value="Y">사용</option>
                  <option value="N">미사용</option>
                </select>
              ) : (
                <div className="mt-1">
                  <UseYnBadge value={detail.useYn ?? undefined} />
                </div>
              )}
            </div>

            {/* Facilities */}
            <div className="sm:col-span-2">
              <label
                htmlFor="facilities"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                시설 (쉼표로 구분)
              </label>
              {isEditing ? (
                <input
                  id="facilities"
                  type="text"
                  placeholder="예: 주차장, 카페, 유아실"
                  className={FIELD_CLASS}
                  {...register("facilities")}
                />
              ) : (
                <p className="mt-1 text-sm text-slate-900">
                  {detail.facilities || "-"}
                </p>
              )}
            </div>

            {/* Introduction */}
            <div className="sm:col-span-2">
              <label
                htmlFor="introduction"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                소개
              </label>
              {isEditing ? (
                <textarea
                  id="introduction"
                  rows={3}
                  className={FIELD_CLASS}
                  {...register("introduction")}
                />
              ) : (
                <p className="mt-1 whitespace-pre-wrap text-sm text-slate-900">
                  {detail.introduction ?? "-"}
                </p>
              )}
            </div>
          </div>

          <hr className="my-6 border-slate-200" />

          {/* Worship times */}
          {isEditing ? (
            <WorshipTimeRows control={control} register={register} />
          ) : (
            <div>
              <p className="mb-2 text-xs font-medium text-slate-500">예배시간</p>
              {detail.worshipTimes && detail.worshipTimes.length > 0 ? (
                <ul className="space-y-1">
                  {detail.worshipTimes.map((time, index) => (
                    <li
                      key={index}
                      className="flex flex-wrap items-center gap-2 text-sm text-slate-900"
                    >
                      <span className="font-medium">
                        {time.kind
                          ? WORSHIP_KIND_LABELS[time.kind] ?? time.kind
                          : "-"}
                      </span>
                      {time.dayLabel && (
                        <span className="text-xs text-slate-500">
                          {time.dayLabel}
                        </span>
                      )}
                      {time.startTime && (
                        <span className="text-xs text-slate-500">
                          {time.startTime}
                        </span>
                      )}
                      {time.place && (
                        <span className="text-xs text-slate-500">
                          · {time.place}
                        </span>
                      )}
                      {time.target && (
                        <span className="text-xs text-slate-500">
                          · {time.target}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-slate-400">
                  등록된 예배시간이 없습니다.
                </p>
              )}
            </div>
          )}

          <hr className="my-6 border-slate-200" />

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
            <ReadonlyField label="데이터 출처" value={detail.dataSource ?? "-"} />
            <ReadonlyField label="등록일" value={formatDate(detail.createdAt)} />
            <ReadonlyField label="수정일" value={formatDate(detail.updatedAt)} />
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
