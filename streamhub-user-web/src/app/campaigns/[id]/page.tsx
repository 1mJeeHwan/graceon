"use client";

import { useState } from "react";
import { CalendarRange, CalendarHeart } from "lucide-react";
import clsx from "clsx";
import { useCampaign, campaignStatusLabel } from "@/lib/campaigns";
import { ApiError } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { BackLink } from "@/components/BackLink";
import { RichText } from "@/components/RichText";
import { EmptyState, ErrorState } from "@/components/States";

/** Badge tint per known status; unknown statuses get the neutral inactive style. */
function statusClass(status: string): string {
  switch (status) {
    case "ACTIVE":
      return "bg-primary/15 text-primary";
    case "SCHEDULED":
      return "bg-active/10 text-active";
    default:
      return "bg-card text-inactive";
  }
}

/** Public campaign/event detail: hero image, status badge, period, and description body. */
export default function CampaignDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = useCampaign(id);
  const [imageFailed, setImageFailed] = useState(false);

  return (
    <div className="animate-fade-up">
      <div className="px-5 pt-4">
        <BackLink href="/campaigns" label="이벤트 목록" />
      </div>

      {isLoading ? (
        <div className="px-5 pt-4">
          <div className="skeleton aspect-video w-full rounded-card" />
          <div className="skeleton mt-5 h-6 w-3/5 rounded" />
          <div className="skeleton mt-3 h-4 w-2/5 rounded" />
        </div>
      ) : isError ? (
        <div className="pt-3">
          {(error as ApiError)?.status === 404 ? (
            <EmptyState message="이벤트를 찾을 수 없습니다." />
          ) : (
            <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
          )}
        </div>
      ) : data ? (
        <article className="pt-3">
          {/* Hero banner */}
          <div className="px-5">
            <div className="thumb aspect-video w-full">
              {data.imageUrl && !imageFailed ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={data.imageUrl}
                  alt={data.title}
                  onError={() => setImageFailed(true)}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="absolute inset-0 grid place-items-center bg-gradient-to-br from-primary/25 via-card to-surface">
                  <CalendarHeart className="h-12 w-12 text-inactive" />
                </div>
              )}
            </div>

            {/* Title + meta */}
            <div className="mt-5">
              <span
                className={clsx(
                  "rounded-full px-2.5 py-0.5 text-[11px] font-bold",
                  statusClass(data.status),
                )}
              >
                {campaignStatusLabel(data.status)}
              </span>
              <h1 className="mt-2 text-xl font-bold leading-snug text-active">{data.title}</h1>
              <p className="mt-2 flex items-center gap-1.5 text-sm font-medium text-inactive">
                <CalendarRange className="h-4 w-4 shrink-0 text-primary" />
                {formatDate(data.startAt)} ~ {formatDate(data.endAt)}
              </p>
            </div>

            {data.summary && (
              <p className="mt-4 text-sm font-medium leading-relaxed text-active">{data.summary}</p>
            )}

            <RichText
              content={data.description}
              className="mt-4 text-sm leading-relaxed text-inactive"
            />
          </div>
        </article>
      ) : null}
    </div>
  );
}
