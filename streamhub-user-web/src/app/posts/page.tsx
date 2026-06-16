"use client";

import { Suspense, useEffect, useState } from "react";
import { usePosts } from "@/lib/queries";
import { useUrlSearch } from "@/lib/useUrlSearch";
import { PostCard } from "@/components/PostCard";
import { SearchBar } from "@/components/SearchBar";
import { Pagination } from "@/components/Pagination";
import { EmptyState, ErrorState } from "@/components/States";

const PAGE_SIZE = 10;

function PostSkeleton() {
  return (
    <div className="space-y-2.5 px-5">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="skeleton h-[72px] rounded-card" />
      ))}
    </div>
  );
}

function PostsListInner() {
  const { keyword, setKeyword, debounced } = useUrlSearch();
  const [page, setPage] = useState(0);

  useEffect(() => {
    setPage(0);
  }, [debounced]);

  const { data, isLoading, isError, error, isPlaceholderData, refetch } = usePosts({
    keyword: debounced || undefined,
    pageNumber: page,
    pageSize: PAGE_SIZE,
  });

  return (
    <section className="animate-fade-up pt-4">
      <h1 className="px-5 text-2xl font-bold tracking-tight">소식</h1>
      <div className="px-5 pb-2 pt-4">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="소식 검색" />
      </div>

      {isLoading ? (
        <PostSkeleton />
      ) : isError ? (
        <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
      ) : !data || data.contents.length === 0 ? (
        <EmptyState message={debounced ? `'${debounced}' 검색 결과가 없습니다.` : "등록된 소식이 없습니다."} />
      ) : (
        <div className={isPlaceholderData ? "opacity-60 transition-opacity" : "transition-opacity"}>
          {debounced && (
            <p className="px-5 pb-3 text-sm text-inactive">
              <span className="font-bold text-active">{data.totalCount}</span>건의 결과
            </p>
          )}
          <div className="space-y-2.5 px-5">
            {data.contents.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
          <Pagination pageNumber={page} totalPage={data.totalPage} onChange={setPage} />
        </div>
      )}
    </section>
  );
}

export default function PostsListPage() {
  return (
    <Suspense fallback={<div className="px-5 pt-4 text-sm text-inactive">불러오는 중…</div>}>
      <PostsListInner />
    </Suspense>
  );
}
