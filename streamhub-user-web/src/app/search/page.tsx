"use client";

import { Suspense } from "react";
import { Search } from "lucide-react";
import { useContents, usePosts } from "@/lib/queries";
import { useUrlSearch } from "@/lib/useUrlSearch";
import { SearchBar } from "@/components/SearchBar";
import { ContentGrid } from "@/components/ContentGrid";
import { PostCard } from "@/components/PostCard";
import { SectionHeader } from "@/components/SectionHeader";
import { CardSkeletonGrid, EmptyState, ErrorState } from "@/components/States";

const PREVIEW = 6;

/** Runs queries only when there is a keyword; shows video/music/posts previews with counts. */
function SearchResults({ keyword }: { keyword: string }) {
  const q = encodeURIComponent(keyword);
  const videos = useContents({ type: "VIDEO", keyword, pageSize: PREVIEW });
  const musics = useContents({ type: "SOUND", keyword, pageSize: PREVIEW });
  const posts = usePosts({ keyword, pageSize: PREVIEW });

  if (videos.isLoading || musics.isLoading || posts.isLoading) {
    return (
      <div className="px-5 pt-2">
        <CardSkeletonGrid count={6} />
      </div>
    );
  }
  if (videos.isError && musics.isError && posts.isError) {
    return (
      <ErrorState
        message={(videos.error as Error)?.message}
        onRetry={() => {
          videos.refetch();
          musics.refetch();
          posts.refetch();
        }}
      />
    );
  }

  const vTotal = videos.data?.totalCount ?? 0;
  const mTotal = musics.data?.totalCount ?? 0;
  const pTotal = posts.data?.totalCount ?? 0;

  if (vTotal === 0 && mTotal === 0 && pTotal === 0) {
    return <EmptyState message={`'${keyword}' 검색 결과가 없습니다.`} />;
  }

  return (
    <div className="space-y-8 pt-2">
      {vTotal > 0 && (
        <div>
          <SectionHeader title="영상" count={vTotal} moreHref={vTotal > PREVIEW ? `/video?q=${q}` : undefined} />
          <ContentGrid items={videos.data!.contents} />
        </div>
      )}
      {mTotal > 0 && (
        <div>
          <SectionHeader title="음악" count={mTotal} moreHref={mTotal > PREVIEW ? `/music?q=${q}` : undefined} />
          <ContentGrid items={musics.data!.contents} />
        </div>
      )}
      {pTotal > 0 && (
        <div>
          <SectionHeader title="소식" count={pTotal} moreHref={pTotal > PREVIEW ? `/posts?q=${q}` : undefined} />
          <div className="space-y-2.5 px-5">
            {posts.data!.contents.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function SearchInner() {
  const { keyword, setKeyword, debounced } = useUrlSearch();

  return (
    <section className="animate-fade-up pt-4">
      <h1 className="px-5 text-2xl font-bold tracking-tight">검색</h1>
      <div className="px-5 pb-2 pt-3">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="영상 · 음악 · 소식 검색" autoFocus />
      </div>

      {debounced ? (
        <SearchResults keyword={debounced} />
      ) : (
        <div className="flex flex-col items-center justify-center gap-3 py-20 text-center text-inactive">
          <Search className="h-9 w-9" />
          <p className="text-sm">검색어를 입력하면 결과가 표시됩니다.</p>
        </div>
      )}
    </section>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<div className="px-5 pt-4 text-sm text-inactive">불러오는 중…</div>}>
      <SearchInner />
    </Suspense>
  );
}
