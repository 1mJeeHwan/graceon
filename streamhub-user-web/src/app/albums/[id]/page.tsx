"use client";

import { useState } from "react";
import Link from "next/link";
import { Disc3, Info, Lock, MapPin, ShoppingCart } from "lucide-react";
import { useAlbum, GENRE_LABELS, type TrackDto } from "@/lib/albums";
import { ApiError } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { BackLink } from "@/components/BackLink";
import { DemoBadge } from "@/components/DemoBadge";
import { TrackRow } from "@/components/TrackRow";
import { HlsTrackPlayer } from "@/components/HlsTrackPlayer";
import { EmptyState, ErrorState } from "@/components/States";
import { CheckoutModal } from "@/components/CheckoutModal";

/**
 * One track row plus, for tracks with an encrypted full-track HLS stream, a "전체 듣기 (암호화)"
 * toggle that mounts {@link HlsTrackPlayer}. The 30-second preview (TrackRow) stays available to
 * everyone; purchase is not pre-checked — the player attempts playback and surfaces the 403 gate.
 */
function TrackListItem({
  track,
  albumId,
  albumTitle,
  artist,
  coverUrl,
}: {
  track: TrackDto;
  albumId: number;
  albumTitle: string;
  artist: string;
  coverUrl: string | null;
}) {
  const [fullOpen, setFullOpen] = useState(false);

  return (
    <div>
      <TrackRow
        track={track}
        albumId={albumId}
        albumTitle={albumTitle}
        artist={artist}
        coverUrl={coverUrl}
      />
      {track.hasFullTrack && (
        <div className="px-2 pb-1">
          <button
            onClick={() => setFullOpen((v) => !v)}
            className="flex items-center gap-1.5 text-[11px] font-bold text-primary active:opacity-70"
          >
            <Lock className="h-3 w-3" />
            {fullOpen ? "전체 재생 닫기" : "전체 듣기 (암호화)"}
          </button>
          {fullOpen && (
            <HlsTrackPlayer albumId={albumId} trackId={track.id} title={track.title} />
          )}
        </div>
      )}
    </div>
  );
}

export default function AlbumDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = useAlbum(id);
  const [coverFailed, setCoverFailed] = useState(false);
  const [added, setAdded] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);

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

            {/* Purchase — price emphasized, cart + buy. The buy CTA is also pinned bottom (sticky). */}
            <div className="mt-5 rounded-card border border-primary/30 bg-primary/5 px-4 py-4">
              <div className="flex items-end justify-between">
                <span className="text-xs font-semibold text-inactive">판매가</span>
                {data.price != null ? (
                  <span className="text-2xl font-extrabold tracking-tight text-primary">
                    {data.price.toLocaleString()}
                    <span className="ml-0.5 text-base font-bold">원</span>
                  </span>
                ) : (
                  <span className="text-sm font-medium text-inactive">가격 미정</span>
                )}
              </div>
              <div className="mt-3.5 flex gap-2">
                <button
                  onClick={() => setAdded(true)}
                  className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-border bg-bg py-3 text-sm font-bold text-active active:bg-card"
                >
                  <ShoppingCart className="h-4 w-4" />
                  {added ? "담김 (데모)" : "장바구니"}
                </button>
                <button
                  onClick={() => setCheckoutOpen(true)}
                  className="btn-primary flex-[1.4] py-3 text-sm font-bold"
                >
                  구매하기
                </button>
              </div>
              <p className="mt-2.5 flex items-center gap-1.5 text-[11px] leading-tight text-inactive">
                <Info className="h-3.5 w-3.5 shrink-0" />
                실결제는 없지만 주문은 실제로 생성됩니다(로그인 필요).
              </p>
            </div>
            <CheckoutModal
              open={checkoutOpen}
              onClose={() => setCheckoutOpen(false)}
              item={{ albumId: data.id, name: data.title, price: data.price ?? 0 }}
            />
          </div>

          {/* Track list */}
          <div className="mt-7 px-5">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-active">트랙 {data.tracks.length}곡</h2>
              <DemoBadge />
            </div>
            <p className="mt-1.5 flex items-center gap-1.5 text-[11px] text-inactive">
              <Info className="h-3.5 w-3.5 shrink-0" />
              미리듣기는 샘플 음원(SoundHelix)으로 재생되는 데모입니다.
            </p>
            <div className="mt-2 divide-y divide-border/40">
              {data.tracks.map((track) => (
                <TrackListItem
                  key={track.id}
                  track={track}
                  albumId={data.id}
                  albumTitle={data.title}
                  artist={data.artist}
                  coverUrl={data.coverUrl}
                />
              ))}
            </div>
          </div>

          {/* Store CTA */}
          <div className="mt-7 px-5">
            <Link
              href="/stores"
              className="flex items-center justify-center gap-2 rounded-xl border border-border py-3 text-sm font-medium text-active active:bg-card"
            >
              <MapPin className="h-4 w-4 text-primary" />이 음반 판매 매장 보기
            </Link>
          </div>

          {/* Sticky purchase bar — keeps the buy CTA reachable while scrolling tracks. */}
          <div className="sticky bottom-0 z-40 mt-7 border-t border-border bg-bg/95 px-5 py-3 backdrop-blur">
            <button
              onClick={() => setCheckoutOpen(true)}
              className="btn-primary w-full py-3.5 text-[15px] font-bold"
            >
              <ShoppingCart className="h-4.5 w-4.5" />
              {data.price != null ? `${data.price.toLocaleString()}원 구매하기` : "구매하기"}
            </button>
          </div>
        </article>
      ) : null}
    </div>
  );
}
