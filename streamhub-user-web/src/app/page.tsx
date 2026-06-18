"use client";

import { useHome } from "@/lib/queries";
import { useAlbums, type AlbumListItem } from "@/lib/albums";
import { useSiteConfig, DEFAULT_SITE_CONFIG } from "@/lib/siteConfig";
import { Hero } from "@/components/Hero";
import { ContentContainer } from "@/components/ContentContainer";
import { HRow, HItem } from "@/components/HRow";
import { ContentCard } from "@/components/ContentCard";
import { AlbumCard } from "@/components/AlbumCard";
import { PostCard } from "@/components/PostCard";
import { NearbyChurchesSection } from "@/components/NearbyChurchesSection";
import { HomeTopBanner } from "@/components/HomeTopBanner";
import { EmptyState, ErrorState } from "@/components/States";

function HomeSkeleton() {
  return (
    <div>
      <div className="skeleton aspect-[16/10] w-full" />
      <div className="mt-7 space-y-5 px-5">
        <div className="skeleton h-6 w-32 rounded" />
        <div className="flex gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="w-[240px]">
              <div className="skeleton aspect-video rounded-card" />
              <div className="skeleton mt-2.5 h-4 w-4/5 rounded" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/** "CCM 음반" carousel — gives album purchases equal billing with video/music. */
function CcmAlbumSection() {
  const { data, isLoading } = useAlbums({ pageNumber: 0, pageSize: 10 });
  const albums = data?.contents ?? [];

  if (isLoading) {
    return (
      <ContentContainer title="CCM 음반" moreHref="/albums">
        <HRow>
          {Array.from({ length: 4 }).map((_, i) => (
            <HItem key={i} width={150}>
              <div className="skeleton aspect-square rounded-card" />
              <div className="skeleton mt-2.5 h-4 w-4/5 rounded" />
            </HItem>
          ))}
        </HRow>
      </ContentContainer>
    );
  }
  if (albums.length === 0) return null;

  return (
    <ContentContainer title="CCM 음반" moreHref="/albums">
      <HRow>
        {albums.map((album) => (
          <HItem key={album.id} width={150}>
            <AlbumCard item={album} />
          </HItem>
        ))}
      </HRow>
    </ContentContainer>
  );
}

/** Admin-pinned "추천 음반" row. Resolves featured ids against the album list (small dataset). */
function FeaturedAlbumsSection({ ids }: { ids: number[] }) {
  const { data } = useAlbums({ pageNumber: 0, pageSize: 200 });
  if (ids.length === 0) return null;
  const all = data?.contents ?? [];
  const picked = ids
    .map((id) => all.find((a) => a.id === id))
    .filter((a): a is AlbumListItem => Boolean(a));
  if (picked.length === 0) return null;

  return (
    <ContentContainer title="추천 음반" moreHref="/albums">
      <HRow>
        {picked.map((album) => (
          <HItem key={album.id} width={150}>
            <AlbumCard item={album} />
          </HItem>
        ))}
      </HRow>
    </ContentContainer>
  );
}

export default function HomePage() {
  const { data, isLoading, isError, error, refetch } = useHome();
  const { data: config } = useSiteConfig();
  const cfg = config ?? DEFAULT_SITE_CONFIG;

  if (isLoading) return <HomeSkeleton />;
  if (isError)
    return (
      <div className="pt-6">
        <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
      </div>
    );
  if (!data) return <div className="pt-6"><EmptyState /></div>;

  const enabled = (key: string) => cfg.homeSections.find((s) => s.key === key)?.enabled ?? true;

  // Admin-ordered, toggleable home sections. The order/visibility comes from site-config.
  const renderSection = (key: string) => {
    if (!enabled(key)) return null;
    switch (key) {
      case "worshipLive":
        return <Hero key="worshipLive" items={data.videos} />;
      case "latestVideos":
        return (
          <ContentContainer key="latestVideos" title="최신 영상" moreHref="/video">
            {data.videos.length > 0 ? (
              <HRow>
                {data.videos.map((v) => (
                  <HItem key={v.id} width={240}>
                    <ContentCard item={v} />
                  </HItem>
                ))}
              </HRow>
            ) : (
              <EmptyState message="등록된 영상이 없습니다." />
            )}
          </ContentContainer>
        );
      case "ccmAlbums":
        return <CcmAlbumSection key="ccmAlbums" />;
      case "nearbyChurch":
        return <NearbyChurchesSection key="nearbyChurch" />;
      default:
        return null;
    }
  };

  return (
    <div className="animate-fade-up pb-4">
      <HomeTopBanner />
      <FeaturedAlbumsSection ids={cfg.featuredAlbumIds} />

      {cfg.homeSections.map((s) => renderSection(s.key))}

      {/* Always-on sections (not part of the configurable set). */}
      <ContentContainer title="찬양 음악" moreHref="/music">
        {data.musics.length > 0 ? (
          <HRow>
            {data.musics.map((m) => (
              <HItem key={m.id} width={150}>
                <ContentCard item={m} />
              </HItem>
            ))}
          </HRow>
        ) : (
          <EmptyState message="등록된 음악이 없습니다." />
        )}
      </ContentContainer>

      <ContentContainer title="새로운 소식" moreHref="/posts">
        {data.posts.length > 0 ? (
          <div className="space-y-2.5 px-5">
            {data.posts.slice(0, 4).map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        ) : (
          <EmptyState message="등록된 소식이 없습니다." />
        )}
      </ContentContainer>
    </div>
  );
}
