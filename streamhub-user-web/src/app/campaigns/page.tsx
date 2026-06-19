"use client";

import { useState } from "react";
import { CalendarHeart } from "lucide-react";
import clsx from "clsx";
import { useCampaigns } from "@/lib/campaigns";
import { CampaignCard } from "@/components/CampaignCard";
import { Pagination } from "@/components/Pagination";
import { CardSkeletonGrid, EmptyState, ErrorState } from "@/components/States";

const PAGE_SIZE = 12;

/** Public campaign/event list: banner card grid + pagination. */
export default function CampaignsPage() {
  const [page, setPage] = useState(0);
  const params = { pageNumber: page, pageSize: PAGE_SIZE };
  const { data, isLoading, isError, error, isPlaceholderData, refetch } = useCampaigns(params);

  return (
    <section className="animate-fade-up pt-4">
      <div className="flex items-center gap-2 px-5">
        <CalendarHeart className="h-6 w-6 text-primary" />
        <h1 className="text-2xl font-bold tracking-tight">이벤트</h1>
      </div>
      <p className="mt-1 px-5 pb-1 text-sm text-inactive">
        진행 중이거나 예정된 캠페인·이벤트 소식을 확인하세요.
      </p>

      {isLoading ? (
        <div className="px-5 pt-3">
          <CardSkeletonGrid count={PAGE_SIZE} />
        </div>
      ) : isError ? (
        <div className="pt-3">
          <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
        </div>
      ) : !data || data.contents.length === 0 ? (
        <div className="pt-3">
          <EmptyState message="진행 중인 이벤트가 없습니다." />
        </div>
      ) : (
        <div className={clsx("transition-opacity", isPlaceholderData && "opacity-60")}>
          <p className="px-5 pb-3 pt-2 text-sm text-inactive">
            <span className="font-bold text-active">{data.totalCount}</span>개의 이벤트
          </p>
          <div className="grid grid-cols-2 gap-x-3 gap-y-5 px-5">
            {data.contents.map((item) => (
              <CampaignCard key={item.id} item={item} />
            ))}
          </div>
          <Pagination pageNumber={page} totalPage={data.totalPage} onChange={setPage} />
        </div>
      )}
    </section>
  );
}
