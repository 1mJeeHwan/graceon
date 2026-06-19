"use client";

import { useRef, type PointerEvent as ReactPointerEvent } from "react";
import { Play, Pause, Volume2, VolumeX, Maximize, Minimize } from "lucide-react";

/** Unified playback state both YouTube and native <video> backends report into. */
export interface PlaybackState {
  playing: boolean;
  currentTime: number;
  duration: number;
  /** 0→1 fraction buffered/loaded ahead. */
  buffered: number;
  volume: number;
  muted: boolean;
  isFullscreen: boolean;
}

/** Commands the controls issue; each backend wires these to its own player API. */
export interface PlaybackCommands {
  togglePlay(): void;
  seek(seconds: number): void;
  setVolume(volume: number): void;
  toggleMute(): void;
  toggleFullscreen(): void;
}

/** mm:ss (or h:mm:ss past an hour) — NaN/Infinity render as 0:00. */
function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
  const total = Math.floor(seconds);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const mm = h > 0 ? String(m).padStart(2, "0") : String(m);
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

/**
 * Backend-agnostic custom control bar: play/pause, scrubber with buffer track, time readout,
 * volume, and fullscreen. Drives YouTube and native <video> identically through {@link PlaybackCommands}.
 */
export function VideoControls({
  state,
  commands,
  visible,
}: {
  state: PlaybackState;
  commands: PlaybackCommands;
  visible: boolean;
}) {
  const seekBarRef = useRef<HTMLDivElement>(null);
  const draggingRef = useRef(false);

  const duration = state.duration > 0 ? state.duration : 0;
  const playedPct = duration > 0 ? Math.min(100, (state.currentTime / duration) * 100) : 0;
  const bufferedPct = Math.min(100, Math.max(0, state.buffered * 100));

  const seekToClientX = (clientX: number) => {
    const bar = seekBarRef.current;
    if (!bar || duration <= 0) return;
    const rect = bar.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    commands.seek(ratio * duration);
  };

  const onSeekPointerDown = (e: ReactPointerEvent<HTMLDivElement>) => {
    draggingRef.current = true;
    e.currentTarget.setPointerCapture(e.pointerId);
    seekToClientX(e.clientX);
  };
  const onSeekPointerMove = (e: ReactPointerEvent<HTMLDivElement>) => {
    if (draggingRef.current) seekToClientX(e.clientX);
  };
  const onSeekPointerUp = (e: ReactPointerEvent<HTMLDivElement>) => {
    draggingRef.current = false;
    if (e.currentTarget.hasPointerCapture(e.pointerId)) e.currentTarget.releasePointerCapture(e.pointerId);
  };

  return (
    <div
      className={`absolute inset-x-0 bottom-0 z-10 bg-gradient-to-t from-black/80 to-transparent px-3 pb-2 pt-8 transition-opacity duration-200 ${
        visible ? "opacity-100" : "pointer-events-none opacity-0"
      }`}
    >
      {/* Scrubber */}
      <div
        ref={seekBarRef}
        role="slider"
        aria-label="재생 위치"
        aria-valuemin={0}
        aria-valuemax={Math.floor(duration)}
        aria-valuenow={Math.floor(state.currentTime)}
        tabIndex={0}
        className="group relative flex h-4 w-full cursor-pointer touch-none items-center"
        onPointerDown={onSeekPointerDown}
        onPointerMove={onSeekPointerMove}
        onPointerUp={onSeekPointerUp}
      >
        <div className="relative h-1 w-full overflow-hidden rounded-full bg-white/25">
          <div className="absolute inset-y-0 left-0 rounded-full bg-white/40" style={{ width: `${bufferedPct}%` }} />
          <div className="absolute inset-y-0 left-0 rounded-full bg-primary" style={{ width: `${playedPct}%` }} />
        </div>
        <div
          className="absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary opacity-0 transition-opacity group-hover:opacity-100"
          style={{ left: `${playedPct}%` }}
        />
      </div>

      {/* Buttons row */}
      <div className="mt-1 flex items-center gap-3 text-white">
        <button
          type="button"
          aria-label={state.playing ? "일시정지" : "재생"}
          onClick={commands.togglePlay}
          className="grid h-8 w-8 place-items-center rounded-full hover:bg-white/15"
        >
          {state.playing ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" />}
        </button>

        <div className="group flex items-center gap-1">
          <button
            type="button"
            aria-label={state.muted ? "음소거 해제" : "음소거"}
            onClick={commands.toggleMute}
            className="grid h-8 w-8 place-items-center rounded-full hover:bg-white/15"
          >
            {state.muted || state.volume === 0 ? <VolumeX size={18} /> : <Volume2 size={18} />}
          </button>
          <input
            type="range"
            min={0}
            max={1}
            step={0.05}
            aria-label="음량"
            value={state.muted ? 0 : state.volume}
            onChange={(e) => commands.setVolume(Number(e.target.value))}
            className="h-1 w-0 cursor-pointer accent-primary opacity-0 transition-all duration-200 group-hover:w-16 group-hover:opacity-100"
          />
        </div>

        <span className="text-[12px] tabular-nums text-white/90">
          {formatTime(state.currentTime)} / {formatTime(duration)}
        </span>

        <div className="flex-1" />

        <button
          type="button"
          aria-label={state.isFullscreen ? "전체화면 종료" : "전체화면"}
          onClick={commands.toggleFullscreen}
          className="grid h-8 w-8 place-items-center rounded-full hover:bg-white/15"
        >
          {state.isFullscreen ? <Minimize size={18} /> : <Maximize size={18} />}
        </button>
      </div>
    </div>
  );
}
