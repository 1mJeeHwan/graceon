"use client";

import { useEffect } from "react";
import { Music, Pause, Play } from "lucide-react";
import { useContent } from "@/lib/queries";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { recordWatch } from "@/lib/me";
import { contentSource, useAudioPlayer } from "@/components/player/AudioPlayerProvider";
import type { ContentDetail } from "@/lib/types";
import { ContentMeta } from "@/components/ContentMeta";
import { BackLink } from "@/components/BackLink";
import { EmptyState, ErrorState } from "@/components/States";

/**
 * Music detail playback surface: artwork + a single play/pause button that hands the track to the
 * global player — the exact same playback used by album previews and full tracks. The transport
 * (progress bar, seek, time) lives in the bottom MiniPlayer, so every music screen plays alike.
 */
function MusicPlayback({ content }: { content: ContentDetail }) {
  const { play, toggle, isPlaying, isCurrent } = useAudioPlayer();
  const active = isCurrent(`content:${content.id}`);
  const playing = active && isPlaying;

  const onPlay = () => {
    if (active) {
      toggle();
      return;
    }
    play(
      contentSource({
        contentId: content.id,
        title: content.title,
        subtitle: content.channelName ?? "음악",
        coverUrl: content.thumbnailUrl,
        hlsAvailable: !!content.hlsPrefix,
        mediaUrl: content.mediaUrl,
      }),
    );
  };

  return (
    <div className="px-5">
      <div className="mx-auto mt-2 aspect-square w-full max-w-[280px] overflow-hidden rounded-card bg-gradient-to-br from-primary/30 via-surface to-bg">
        {content.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={content.thumbnailUrl} alt={content.title} className="h-full w-full object-cover" />
        ) : (
          <div className="grid h-full w-full place-items-center">
            <Music className="h-14 w-14 text-inactive" />
          </div>
        )}
      </div>
      <div className="mt-5 text-center">
        <h1 className="text-lg font-bold text-active">{content.title}</h1>
        {content.channelName && <p className="mt-1 text-sm text-inactive">{content.channelName}</p>}
      </div>
      <button
        onClick={onPlay}
        aria-label={playing ? "일시정지" : "재생"}
        className="btn-primary mx-auto mt-5 flex items-center justify-center gap-2 px-8 py-3 text-sm font-bold"
      >
        {playing ? <Pause className="h-5 w-5" /> : <Play className="ml-0.5 h-5 w-5" />}
        {playing ? "일시정지" : "재생"}
      </button>
    </div>
  );
}

export default function MusicDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = useContent(id);
  const { token } = useAuth();

  // Best-effort watch record: once per (member, content) view; no-op when anonymous.
  useEffect(() => {
    if (data && token) recordWatch(id, token);
  }, [data, token, id]);

  return (
    <div className="animate-fade-up">
      <div className="px-5 pt-4">
        <BackLink href="/music" label="음악 목록" />
      </div>

      {isLoading ? (
        <div className="px-5 pt-4">
          <div className="skeleton mx-auto aspect-square w-full max-w-[280px] rounded-card" />
          <div className="skeleton mx-auto mt-5 h-6 w-2/5 rounded" />
        </div>
      ) : isError ? (
        <div className="pt-3">
          {(error as ApiError)?.status === 404 ? (
            <EmptyState message="콘텐츠를 찾을 수 없습니다." />
          ) : (
            <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
          )}
        </div>
      ) : data ? (
        <article className="pt-3">
          <MusicPlayback content={data} />
          <div className="px-5">
            <ContentMeta
              viewCount={data.viewCount}
              createdAt={data.createdAt}
              hashtags={data.hashtags}
              description={data.description}
            />
          </div>
        </article>
      ) : null}
    </div>
  );
}
