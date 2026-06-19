"use client";

import { useCallback, useRef } from "react";
import { Lock, Music, Pause, Play, X } from "lucide-react";
import clsx from "clsx";
import { DemoBadge } from "../DemoBadge";
import { useAudioPlayer } from "./AudioPlayerProvider";

/** Seconds → "m:ss", always at least "0:00" (unlike formatDuration which blanks 0). */
function clock(sec: number): string {
  const total = Math.max(0, Math.floor(sec));
  const m = Math.floor(total / 60);
  const s = String(total % 60).padStart(2, "0");
  return `${m}:${s}`;
}

/**
 * Bottom-fixed mini player — the single playback surface for previews, full album tracks and
 * music-tab content. Sits just above the TabBar inside the phone frame. Shows cover, title/artist,
 * a seekable progress bar, elapsed/total time, play/pause and close. While a stream is being
 * prepared the bar shows load progress; a gated full track shows a purchase hint.
 */
export function MiniPlayer() {
  const {
    current,
    isPlaying,
    currentTime,
    duration,
    loading,
    loadProgress,
    error,
    toggle,
    seek,
    stop,
  } = useAudioPlayer();
  const barRef = useRef<HTMLDivElement>(null);

  const seekToClientX = useCallback(
    (clientX: number) => {
      const el = barRef.current;
      if (!el || duration <= 0) return;
      const rect = el.getBoundingClientRect();
      const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
      seek(ratio * duration);
    },
    [duration, seek],
  );

  const onScrub = useCallback(
    (e: React.PointerEvent<HTMLDivElement>) => {
      if (loading || error) return;
      e.currentTarget.setPointerCapture(e.pointerId);
      seekToClientX(e.clientX);
      const move = (ev: PointerEvent) => seekToClientX(ev.clientX);
      const up = () => {
        window.removeEventListener("pointermove", move);
        window.removeEventListener("pointerup", up);
      };
      window.addEventListener("pointermove", move);
      window.addEventListener("pointerup", up);
    },
    [loading, error, seekToClientX],
  );

  if (!current) return null;

  const playbackPct = duration > 0 ? Math.min(100, (currentTime / duration) * 100) : 0;
  const barPct = error ? 100 : loading ? Math.max(5, loadProgress) : playbackPct;
  const isDemo = current.kind === "preview";
  const seekable = !loading && !error && duration > 0;

  const statusLine = error
    ? error === "gated"
      ? "구매한 회원만 전체 재생할 수 있습니다"
      : "재생할 수 없습니다"
    : loading
      ? `재생 준비 중 ${Math.round(loadProgress)}%`
      : `${current.subtitle} · ${clock(currentTime)} / ${
          duration > 0 ? clock(duration) : "--:--"
        }`;

  return (
    <div className="fixed bottom-[60px] left-1/2 z-40 w-full max-w-[480px] -translate-x-1/2 px-3 pb-1">
      <div className="overflow-hidden rounded-card border border-border bg-card/95 shadow-lg backdrop-blur-md">
        <div
          ref={barRef}
          onPointerDown={onScrub}
          role="slider"
          aria-label="재생 위치"
          aria-valuemin={0}
          aria-valuemax={Math.round(duration)}
          aria-valuenow={Math.round(currentTime)}
          tabIndex={seekable ? 0 : -1}
          className={clsx("group relative h-1.5 w-full bg-border", seekable && "cursor-pointer")}
        >
          <div
            className={clsx(
              "h-full transition-[width] duration-150 ease-linear",
              error ? "bg-point" : loading ? "bg-primary/60" : "bg-primary",
            )}
            style={{ width: `${barPct}%` }}
          />
          {seekable && (
            <span
              className="absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary opacity-0 shadow transition-opacity group-hover:opacity-100"
              style={{ left: `${playbackPct}%` }}
            />
          )}
        </div>

        <div className="flex items-center gap-3 px-3 py-2.5">
          <div className="h-10 w-10 shrink-0 overflow-hidden rounded-md bg-surface">
            {current.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={current.coverUrl} alt="" className="h-full w-full object-cover" />
            ) : (
              <div className="grid h-full w-full place-items-center">
                <Music className="h-4 w-4 text-inactive" />
              </div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5">
              <p className="ellipsis-1 text-[13px] font-bold text-active">{current.title}</p>
              {isDemo && <DemoBadge className="shrink-0" label="데모" />}
              {current.kind === "full" && (
                <span className="flex shrink-0 items-center gap-0.5 rounded-full bg-primary/10 px-1.5 py-0.5 text-[9px] font-bold text-primary">
                  <Lock className="h-2.5 w-2.5" />
                  전체
                </span>
              )}
            </div>
            <p className={clsx("ellipsis-1 text-[11px]", error ? "text-point" : "text-inactive")}>
              {statusLine}
            </p>
          </div>

          <button
            onClick={toggle}
            disabled={!!error || loading}
            aria-label={isPlaying ? "일시정지" : "재생"}
            className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-primary text-bg transition active:scale-95 disabled:opacity-40"
          >
            {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="ml-0.5 h-4 w-4" />}
          </button>
          <button
            onClick={stop}
            aria-label="닫기"
            className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-inactive active:text-active"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  );
}
