"use client";

import { Suspense, useEffect, useState } from "react";
import { ShoppingBag } from "lucide-react";
import clsx from "clsx";
import { useGoods } from "@/lib/goods";
import { useUrlSearch } from "@/lib/useUrlSearch";
import { GoodsCard } from "@/components/GoodsCard";
import { SearchBar } from "@/components/SearchBar";
import { Pagination } from "@/components/Pagination";
import { CardSkeletonGrid, EmptyState, ErrorState } from "@/components/States";

const PAGE_SIZE = 12;

const TITLE = (
  <div className="flex items-center gap-2 px-5">
    <ShoppingBag className="h-6 w-6 text-primary" />
    <h1 className="text-2xl font-bold tracking-tight">굿즈샵</h1>
  </div>
);

const SUBTITLE = (
  <p className="mt-1 px-5 text-sm text-inactive">아티스트 굿즈를 둘러보세요.</p>
);

/** Goods list: URL-synced keyword search + card grid + pagination. */
function GoodsListView() {
  const { keyword, setKeyword, debounced } = useUrlSearch();
  const [page, setPage] = useState(0);

  // A new search always restarts at the first page.
  useEffect(() => {
    setPage(0);
  }, [debounced]);

  const params = { keyword: debounced || undefined, pageNumber: page, pageSize: PAGE_SIZE };
  const { data, isLoading, isError, error, isPlaceholderData, refetch } = useGoods(params);

  return (
    <section className="animate-fade-up pt-4">
      {TITLE}
      {SUBTITLE}

      <div className="px-5 pb-2 pt-4">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="굿즈 검색" />
      </div>

      {isLoading ? (
        <div className="px-5 pt-1">
          <CardSkeletonGrid square count={PAGE_SIZE} />
        </div>
      ) : isError ? (
        <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
      ) : !data || data.contents.length === 0 ? (
        <EmptyState message={debounced ? `'${debounced}' 검색 결과가 없습니다.` : "굿즈가 없습니다."} />
      ) : (
        <div className={clsx("transition-opacity", isPlaceholderData && "opacity-60")}>
          <p className="px-5 pb-3 pt-1 text-sm text-inactive">
            <span className="font-bold text-active">{data.totalCount}</span>개의 굿즈
          </p>
          <div className="grid grid-cols-2 gap-x-3 gap-y-5 px-5">
            {data.contents.map((item) => (
              <GoodsCard key={item.id} item={item} />
            ))}
          </div>
          <Pagination pageNumber={page} totalPage={data.totalPage} onChange={setPage} />
        </div>
      )}
    </section>
  );
}

/** Skeleton matching the loaded goods list (header + search + grid) to avoid a layout jump. */
function GoodsFallback() {
  return (
    <section className="pt-4" aria-busy="true">
      {TITLE}
      {SUBTITLE}
      <div className="px-5 pb-2 pt-4">
        <div className="skeleton h-[42px] w-full rounded-xl" />
      </div>
      <div className="px-5 pt-1">
        <CardSkeletonGrid square count={PAGE_SIZE} />
      </div>
    </section>
  );
}

export default function GoodsPage() {
  return (
    <Suspense fallback={<GoodsFallback />}>
      <GoodsListView />
    </Suspense>
  );
}
