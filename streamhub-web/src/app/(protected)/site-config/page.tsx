"use client";

import { useEffect, useState } from "react";
import { Loader2, Save } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { albumList } from "@/apis/query/album/album";
import {
  siteConfigDetail,
  useSiteConfigUpdate,
} from "@/apis/query/site-config/site-config";
import {
  type AlbumListItem,
  type HomeSection,
  type SiteConfigData,
} from "@/apis/query/streamHubAdminAPI.schemas";
import AccentColorField from "@/components/site-config/AccentColorField";
import FeaturedAlbumsField from "@/components/site-config/FeaturedAlbumsField";
import HomeSectionsField from "@/components/site-config/HomeSectionsField";
import { SUCCESS_CODE } from "@/types/api";

const DEFAULT_THEME = "dark";
const DEFAULT_ACCENT = "#40C1DF";

interface FormState {
  defaultTheme: string;
  accentColor: string;
  announcementEnabled: boolean;
  announcementText: string;
  announcementLink: string;
  homeSections: HomeSection[];
  featuredAlbumIds: number[];
}

const toFormState = (config: SiteConfigData | undefined): FormState => ({
  defaultTheme: config?.defaultTheme ?? DEFAULT_THEME,
  accentColor: config?.accentColor ?? DEFAULT_ACCENT,
  announcementEnabled: config?.announcement?.enabled ?? false,
  announcementText: config?.announcement?.text ?? "",
  announcementLink: config?.announcement?.link ?? "",
  homeSections: config?.homeSections ?? [],
  featuredAlbumIds: config?.featuredAlbumIds ?? [],
});

export default function SiteConfigPage() {
  const [form, setForm] = useState<FormState>(toFormState(undefined));
  const [message, setMessage] = useState<string | null>(null);

  const configQuery = useQuery({
    queryKey: ["site-config"],
    queryFn: ({ signal }) => siteConfigDetail(signal),
  });

  const albumsQuery = useQuery({
    queryKey: ["site-config", "album-list"],
    queryFn: ({ signal }) => albumList({ pageSize: 200 }, signal),
  });

  const updateMutation = useSiteConfigUpdate();

  // Seed local edit state once the current config loads.
  useEffect(() => {
    if (configQuery.data?.resultObject) {
      setForm(toFormState(configQuery.data.resultObject));
    }
  }, [configQuery.data]);

  const albums: AlbumListItem[] =
    albumsQuery.data?.resultObject?.contents ?? [];

  const patch = <Key extends keyof FormState>(
    key: Key,
    value: FormState[Key],
  ) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = () => {
    setMessage(null);
    const body: SiteConfigData = {
      defaultTheme: form.defaultTheme,
      accentColor: form.accentColor,
      announcement: {
        enabled: form.announcementEnabled,
        text: form.announcementText,
        link: form.announcementLink,
      },
      // Saved order is the rendered order; unknown keys are preserved as-is.
      homeSections: form.homeSections,
      featuredAlbumIds: form.featuredAlbumIds,
    };

    updateMutation.mutate(
      { data: body },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("저장되었습니다.");
            configQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "저장에 실패했습니다.");
          }
        },
        onError: () => setMessage("저장 중 오류가 발생했습니다."),
      },
    );
  };

  if (configQuery.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
        <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
      </div>
    );
  }

  if (configQuery.isError) {
    return (
      <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
        <p className="text-sm text-red-600">
          사이트 설정을 불러오지 못했습니다.
        </p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">
            운영 사이트 UI 설정
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            사용자 사이트의 테마, 액센트 색, 공지바, 홈 섹션, 추천 음반을
            관리합니다.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {message && (
            <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
              {message}
            </span>
          )}
          <button
            type="button"
            onClick={handleSave}
            disabled={updateMutation.isPending}
            className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
          >
            {updateMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            저장
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {/* 기본 테마 */}
          <section className="rounded-md border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-900">기본 테마</h2>
            <p className="mt-1 text-xs text-slate-400">
              사용자가 직접 토글한 선택이 우선됩니다.
            </p>
            <div className="mt-3 inline-flex overflow-hidden rounded-md border border-slate-300">
              {(["dark", "light"] as const).map((theme) => {
                const active = form.defaultTheme === theme;
                return (
                  <button
                    key={theme}
                    type="button"
                    onClick={() => patch("defaultTheme", theme)}
                    className={`px-5 py-2 text-sm font-medium transition ${
                      active
                        ? "bg-brand text-white"
                        : "bg-white text-slate-600 hover:bg-slate-50"
                    }`}
                  >
                    {theme === "dark" ? "다크" : "라이트"}
                  </button>
                );
              })}
            </div>
          </section>

          {/* 액센트 색 */}
          <section className="rounded-md border border-slate-200 bg-white p-5">
            <h2 className="mb-3 text-sm font-semibold text-slate-900">
              액센트 색
            </h2>
            <AccentColorField
              value={form.accentColor}
              onChange={(hex) => patch("accentColor", hex)}
            />
          </section>

          {/* 공지바 */}
          <section className="rounded-md border border-slate-200 bg-white p-5">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-900">공지바</h2>
              <label className="inline-flex cursor-pointer items-center gap-2 text-sm text-slate-600">
                <input
                  type="checkbox"
                  checked={form.announcementEnabled}
                  onChange={(event) =>
                    patch("announcementEnabled", event.target.checked)
                  }
                  className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
                />
                사용
              </label>
            </div>
            <div className="mt-3 space-y-3">
              <div className="flex flex-col">
                <label
                  htmlFor="announcement-text"
                  className="mb-1 text-xs font-medium text-slate-600"
                >
                  문구
                </label>
                <input
                  id="announcement-text"
                  type="text"
                  value={form.announcementText}
                  onChange={(event) =>
                    patch("announcementText", event.target.value)
                  }
                  placeholder="공지 문구를 입력하세요"
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                />
              </div>
              <div className="flex flex-col">
                <label
                  htmlFor="announcement-link"
                  className="mb-1 text-xs font-medium text-slate-600"
                >
                  링크
                </label>
                <input
                  id="announcement-link"
                  type="text"
                  value={form.announcementLink}
                  onChange={(event) =>
                    patch("announcementLink", event.target.value)
                  }
                  placeholder="https://"
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                />
              </div>
            </div>
          </section>

          {/* 홈 섹션 순서·노출 */}
          <section className="rounded-md border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-900">
              홈 섹션 순서·노출
            </h2>
            <p className="mt-1 text-xs text-slate-400">
              드래그하거나 화살표 버튼으로 순서를 바꿀 수 있습니다. 저장 시 화면에
              보이는 순서대로 적용됩니다.
            </p>
            <div className="mt-3">
              <HomeSectionsField
                sections={form.homeSections}
                onChange={(next) => patch("homeSections", next)}
              />
            </div>
          </section>

          {/* 추천 음반 */}
          <section className="rounded-md border border-slate-200 bg-white p-5">
            <h2 className="mb-3 text-sm font-semibold text-slate-900">
              추천 음반
            </h2>
            <FeaturedAlbumsField
              albums={albums}
              isLoading={albumsQuery.isLoading}
              isError={albumsQuery.isError}
              selectedIds={form.featuredAlbumIds}
              onChange={(next) => patch("featuredAlbumIds", next)}
            />
          </section>
        </div>

        {/* 미리보기 */}
        <div className="lg:col-span-1">
          <div className="sticky top-4 rounded-md border border-slate-200 bg-white p-5">
            <h2 className="mb-3 text-sm font-semibold text-slate-900">
              미리보기
            </h2>
            <div
              className="rounded-md p-4"
              style={{
                backgroundColor:
                  form.defaultTheme === "dark" ? "#0F172A" : "#F8FAFC",
                color: form.defaultTheme === "dark" ? "#E2E8F0" : "#0F172A",
              }}
            >
              {form.announcementEnabled && form.announcementText && (
                <div
                  className="mb-3 rounded px-3 py-2 text-xs font-medium"
                  style={{
                    backgroundColor: form.accentColor,
                    color: "#FFFFFF",
                  }}
                >
                  {form.announcementText}
                </div>
              )}
              <div className="flex items-center gap-2">
                <span
                  className="inline-block h-6 w-6 rounded-full"
                  style={{ backgroundColor: form.accentColor }}
                  aria-hidden
                />
                <span className="text-sm font-semibold">StreamHub</span>
              </div>
              <button
                type="button"
                disabled
                className="mt-3 rounded-md px-4 py-1.5 text-xs font-medium text-white"
                style={{ backgroundColor: form.accentColor }}
              >
                기본 버튼
              </button>
            </div>
            <dl className="mt-4 space-y-1 text-xs text-slate-500">
              <div className="flex justify-between">
                <dt>테마</dt>
                <dd className="font-medium text-slate-700">
                  {form.defaultTheme}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>추천 음반</dt>
                <dd className="font-medium text-slate-700">
                  {form.featuredAlbumIds.length}건
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>노출 홈 섹션</dt>
                <dd className="font-medium text-slate-700">
                  {form.homeSections.filter((section) => section.enabled).length}
                  /{form.homeSections.length}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
