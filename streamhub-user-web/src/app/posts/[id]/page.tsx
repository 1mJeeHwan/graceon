"use client";

import { CalendarDays } from "lucide-react";
import { usePost } from "@/lib/queries";
import { ApiError } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { BackLink } from "@/components/BackLink";
import { EmptyState, ErrorState } from "@/components/States";

export default function PostDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = usePost(id);

  return (
    <div className="animate-fade-up px-5 pt-4">
      <BackLink href="/posts" label="소식 목록" />

      {isLoading ? (
        <div className="mt-5 space-y-3">
          <div className="skeleton h-7 w-3/4 rounded" />
          <div className="skeleton h-4 w-1/4 rounded" />
          <div className="skeleton h-40 w-full rounded-card" />
        </div>
      ) : isError ? (
        <div className="mt-3">
          {(error as ApiError)?.status === 404 ? (
            <EmptyState message="게시글을 찾을 수 없습니다." />
          ) : (
            <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
          )}
        </div>
      ) : data ? (
        <article className="mt-4">
          <h1 className="text-2xl font-bold leading-tight">{data.title}</h1>
          <p className="mt-3 flex items-center gap-1.5 text-sm text-inactive">
            <CalendarDays className="h-4 w-4" />
            {formatDate(data.createdAt)}
          </p>
          <div className="mt-5 whitespace-pre-wrap text-[15px] leading-relaxed text-active/90">{data.body}</div>
        </article>
      ) : null}
    </div>
  );
}
