"use client";

import { useState } from "react";
import { Disc3, Info, Pause, Play } from "lucide-react";
import { useAlbum, GENRE_LABELS, type TrackDto } from "@/lib/albums";
import { ApiError } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { BackLink } from "@/components/BackLink";
import { DemoBadge } from "@/components/DemoBadge";
import { TrackRow } from "@/components/TrackRow";
import { fullTrackSource, useAudioPlayer } from "@/components/player/AudioPlayerProvider";
import { EmptyState, ErrorState } from "@/components/States";
import { ProductFeedback } from "@/components/ProductFeedback";

/**
 * One track row plus, for tracks with a full-track HLS stream, a "전체 재생" button that plays the
 * full stream through the global player (same surface as previews and the music tab). Music is free
 * to listen — full tracks are no longer purchase-gated. The 30-second preview (TrackRow) stays as a
 * quick listen.
 */
function TrackListItem({
  track,
  albumId,
  artist,
  coverUrl,
}: {
  track: TrackDto;
  albumId: number;
  artist: string;
  coverUrl: string | null;
}) {
  const { play, toggle, isPlaying, isCurrent } = useAudioPlayer();
  const fullActive = isCurrent(`full:${albumId}:${track.id}`);
  const fullPlaying = fullActive && isPlaying;

  const onFull = () => {
    if (fullActive) {
      toggle();
      return;
    }
    play(
      fullTrackSource({ albumId, trackId: track.id, title: track.title, artist, coverUrl }),
    );
  };

  return (
    <div>
      <TrackRow track={track} albumId={albumId} artist={artist} coverUrl={coverUrl} />
      {track.hasFullTrack && (
        <div className="px-2 pb-1">
          <button
            onClick={onFull}
            className="flex items-center gap-1.5 text-[11px] font-bold text-primary active:opacity-70"
          >
            {fullPlaying ? <Pause className="h-3 w-3" /> : <Play className="h-3 w-3" />}
            {fullPlaying ? "전체 재생 중 (일시정지)" : "전체 재생"}
          </button>
        </div>
      )}
    </div>
  );
}

export default function AlbumDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = useAlbum(id);
  const [coverFailed, setCoverFailed] = useState(false);

  return (
    <div className="animate-fade-up">
      <div className="px-5 pt-4">
        <BackLink href="/albums" label="음반 목록" />
      </div>

      {isLoading ? (
        <div className="px-5 pt-4">
          <div className="skeleton mx-auto aspect-square w-full max-w-[280px] rounded-card" />
          <div className="skeleton mx-auto mt-5 h-6 w-2/5 rounded" />
          <div className="skeleton mx-auto mt-3 h-4 w-1/4 rounded" />
        </div>
      ) : isError ? (
        <div className="pt-3">
          {(error as ApiError)?.status === 404 ? (
            <EmptyState message="음반을 찾을 수 없습니다." />
          ) : (
            <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
          )}
        </div>
      ) : data ? (
        <article className="pt-3">
          {/* Cover + meta */}
          <div className="px-5">
            <div className="mx-auto aspect-square w-full max-w-[280px] overflow-hidden rounded-card bg-gradient-to-br from-card to-surface">
              {data.coverUrl && !coverFailed ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={data.coverUrl}
                  alt={data.title}
                  onError={() => setCoverFailed(true)}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="grid h-full w-full place-items-center">
                  <Disc3 className="h-14 w-14 text-inactive" />
                </div>
              )}
            </div>

            <div className="mt-5 text-center">
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-bold text-primary">
                {GENRE_LABELS[data.genre]}
              </span>
              <h1 className="mt-2 text-xl font-bold text-active">{data.title}</h1>
              <p className="mt-1 text-sm font-medium text-inactive">{data.artist}</p>
              <p className="mt-1 text-xs text-inactive">
                {data.label && <span>{data.label} · </span>}
                {data.releaseDate && <span>{formatDate(data.releaseDate)} 발매 · </span>}
                <span>{data.trackCount}곡</span>
              </p>
            </div>

            {data.description && (
              <p className="mt-4 whitespace-pre-line text-sm leading-relaxed text-inactive">
                {data.description}
              </p>
            )}

            {data.goodsItemId != null && (
              <ProductFeedback goodsItemId={data.goodsItemId} productName={data.title} />
            )}
          </div>

          {/* Track list */}
          <div className="mt-7 px-5">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-active">트랙 {data.tracks.length}곡</h2>
              <DemoBadge />
            </div>
            <p className="mt-1.5 flex items-center gap-1.5 text-[11px] text-inactive">
              <Info className="h-3.5 w-3.5 shrink-0" />
              전곡을 무료로 감상할 수 있습니다. 음원은 샘플(SoundHelix) 데모입니다.
            </p>
            <div className="mt-2 divide-y divide-border/40">
              {data.tracks.map((track) => (
                <TrackListItem
                  key={track.id}
                  track={track}
                  albumId={data.id}
                  artist={data.artist}
                  coverUrl={data.coverUrl}
                />
              ))}
            </div>
          </div>

          <div className="h-7" />
        </article>
      ) : null}
    </div>
  );
}
