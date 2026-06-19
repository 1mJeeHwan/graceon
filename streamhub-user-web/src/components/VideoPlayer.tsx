"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Play } from "lucide-react";
import { useHlsVideo } from "@/lib/useHlsVideo";
import {
  loadYouTubeIframeApi,
  youtubeId,
  YT_STATE,
  type YouTubePlayer,
} from "@/lib/youtube";
import { HlsLoadingBar } from "./HlsLoadingBar";
import { VideoControls, type PlaybackCommands, type PlaybackState } from "./VideoControls";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

const INITIAL_STATE: PlaybackState = {
  playing: false,
  currentTime: 0,
  duration: 0,
  buffered: 0,
  volume: 1,
  muted: false,
  isFullscreen: false,
};

export interface VideoPlayerProps {
  src: string;
  title?: string;
  /** S3 prefix of the public HLS stream when packaged; together with contentId selects HLS playback. */
  hlsPrefix?: string | null;
  contentId?: number;
  poster?: string | null;
}

/**
 * Self-hosted custom video player. Renders one shared control bar over either a YouTube IFrame
 * player (when {@code src} is a YouTube link) or a native {@code <video>} (HLS via hls.js, or a
 * direct file URL). Both backends report into a single {@link PlaybackState} and obey the same
 * {@link PlaybackCommands}, so the controls behave identically regardless of source.
 */
export function VideoPlayer({ src, title, hlsPrefix, contentId, poster }: VideoPlayerProps) {
  const ytId = youtubeId(src);
  const isYouTube = ytId !== null;

  // HLS when the backend packaged a stream (hlsPrefix + id) or src is an .m3u8 playlist.
  const hlsUrl =
    hlsPrefix && contentId != null
      ? `${API_BASE}/pub/v1/contents/${contentId}/hls/index.m3u8`
      : src.endsWith(".m3u8")
        ? src
        : null;

  if (isYouTube) {
    return <YouTubeBackend videoId={ytId} title={title} />;
  }
  return <NativeBackend src={src} hlsUrl={hlsUrl} title={title} poster={poster} />;
}

/** Shared chrome: container, auto-hiding controls, idle timer. */
function PlayerShell({
  containerRef,
  state,
  commands,
  children,
  showControls,
}: {
  containerRef: React.RefObject<HTMLDivElement>;
  state: PlaybackState;
  commands: PlaybackCommands;
  children: React.ReactNode;
  showControls: boolean;
}) {
  const [active, setActive] = useState(true);
  const idleTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const wake = useCallback(() => {
    setActive(true);
    if (idleTimer.current) clearTimeout(idleTimer.current);
    // Keep controls up while paused; only auto-hide during playback.
    if (state.playing) idleTimer.current = setTimeout(() => setActive(false), 2500);
  }, [state.playing]);

  useEffect(() => {
    wake();
    return () => {
      if (idleTimer.current) clearTimeout(idleTimer.current);
    };
  }, [wake]);

  return (
    <div
      ref={containerRef}
      className="group relative overflow-hidden bg-black sm:rounded-card"
      onMouseMove={wake}
      onMouseLeave={() => state.playing && setActive(false)}
      onTouchStart={wake}
    >
      {children}
      {showControls && <VideoControls state={state} commands={commands} visible={active || !state.playing} />}
    </div>
  );
}

/** Fullscreen state synced from the document (shared by both backends). */
function useFullscreen(containerRef: React.RefObject<HTMLElement>): {
  isFullscreen: boolean;
  toggleFullscreen: () => void;
} {
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const onChange = () => setIsFullscreen(document.fullscreenElement != null);
    document.addEventListener("fullscreenchange", onChange);
    return () => document.removeEventListener("fullscreenchange", onChange);
  }, []);

  const toggleFullscreen = useCallback(() => {
    const el = containerRef.current;
    if (!el) return;
    if (document.fullscreenElement) void document.exitFullscreen();
    else void el.requestFullscreen?.();
  }, [containerRef]);

  return { isFullscreen, toggleFullscreen };
}

// ── YouTube backend ────────────────────────────────────────────────────────

function YouTubeBackend({ videoId, title }: { videoId: string; title?: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mountRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<YouTubePlayer | null>(null);
  const [state, setState] = useState<PlaybackState>(INITIAL_STATE);
  const [ready, setReady] = useState(false);
  const [failed, setFailed] = useState(false);
  const { isFullscreen, toggleFullscreen } = useFullscreen(containerRef);

  useEffect(() => {
    let cancelled = false;
    let raf = 0;

    const poll = () => {
      const p = playerRef.current;
      if (p) {
        setState((prev) => ({
          ...prev,
          currentTime: p.getCurrentTime(),
          duration: p.getDuration(),
          buffered: p.getVideoLoadedFraction(),
          playing: p.getPlayerState() === YT_STATE.PLAYING,
          muted: p.isMuted(),
          volume: p.getVolume() / 100,
        }));
      }
      raf = requestAnimationFrame(poll);
    };

    void loadYouTubeIframeApi().then((YT) => {
      if (cancelled || !mountRef.current) return;
      playerRef.current = new YT.Player(mountRef.current, {
        videoId,
        width: "100%",
        height: "100%",
        playerVars: { controls: 0, modestbranding: 1, playsinline: 1, rel: 0 },
        events: {
          onReady: () => {
            setReady(true);
            raf = requestAnimationFrame(poll);
          },
          onError: () => setFailed(true),
        },
      });
    });

    return () => {
      cancelled = true;
      if (raf) cancelAnimationFrame(raf);
      playerRef.current?.destroy();
      playerRef.current = null;
    };
  }, [videoId]);

  const commands: PlaybackCommands = {
    togglePlay: () => {
      const p = playerRef.current;
      if (!p) return;
      if (p.getPlayerState() === YT_STATE.PLAYING) p.pauseVideo();
      else p.playVideo();
    },
    seek: (s) => playerRef.current?.seekTo(s, true),
    setVolume: (v) => {
      const p = playerRef.current;
      if (!p) return;
      if (v > 0 && p.isMuted()) p.unMute();
      p.setVolume(Math.round(v * 100));
    },
    toggleMute: () => {
      const p = playerRef.current;
      if (!p) return;
      if (p.isMuted()) p.unMute();
      else p.mute();
    },
    toggleFullscreen,
  };

  return (
    <PlayerShell
      containerRef={containerRef}
      state={{ ...state, isFullscreen }}
      commands={commands}
      showControls={ready && !failed}
    >
      <div className="aspect-video w-full">
        <div ref={mountRef} className="h-full w-full" aria-label={title} />
      </div>
      {!ready && !failed && (
        <div className="absolute inset-0 grid place-items-center bg-black/40">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/30 border-t-white" />
        </div>
      )}
      {failed && (
        <div className="absolute inset-0 grid place-items-center bg-black/70 px-4 text-center text-sm text-white/90">
          영상을 재생할 수 없습니다.
        </div>
      )}
    </PlayerShell>
  );
}

// ── Native backend (HLS via hls.js, or direct file) ─────────────────────────

function NativeBackend({
  src,
  hlsUrl,
  title,
  poster,
}: {
  src: string;
  hlsUrl: string | null;
  title?: string;
  poster?: string | null;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [state, setState] = useState<PlaybackState>(INITIAL_STATE);
  const { isFullscreen, toggleFullscreen } = useFullscreen(containerRef);
  const { loading, progress, error } = useHlsVideo(videoRef, { playlistUrl: hlsUrl });

  // Sync unified state from native <video> events.
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const sync = () => {
      let buffered = 0;
      try {
        if (video.duration > 0 && video.buffered.length > 0) {
          buffered = video.buffered.end(video.buffered.length - 1) / video.duration;
        }
      } catch {
        // buffered may throw before metadata
      }
      setState({
        playing: !video.paused && !video.ended,
        currentTime: video.currentTime,
        duration: Number.isFinite(video.duration) ? video.duration : 0,
        buffered,
        volume: video.volume,
        muted: video.muted,
        isFullscreen: document.fullscreenElement != null,
      });
    };

    const events = ["timeupdate", "durationchange", "progress", "play", "pause", "volumechange", "ended", "loadedmetadata"];
    events.forEach((e) => video.addEventListener(e, sync));
    return () => events.forEach((e) => video.removeEventListener(e, sync));
  }, []);

  const commands: PlaybackCommands = {
    togglePlay: () => {
      const v = videoRef.current;
      if (!v) return;
      if (v.paused) void v.play().catch(() => {});
      else v.pause();
    },
    seek: (s) => {
      if (videoRef.current) videoRef.current.currentTime = s;
    },
    setVolume: (vol) => {
      const v = videoRef.current;
      if (!v) return;
      v.volume = vol;
      if (vol > 0) v.muted = false;
    },
    toggleMute: () => {
      const v = videoRef.current;
      if (v) v.muted = !v.muted;
    },
    toggleFullscreen,
  };

  // Direct-file <video> needs an explicit src; HLS sources are attached by useHlsVideo.
  const directSrc = hlsUrl ? undefined : src;

  return (
    <PlayerShell
      containerRef={containerRef}
      state={{ ...state, isFullscreen }}
      commands={commands}
      showControls={!error}
    >
      <video
        ref={videoRef}
        key={hlsUrl ?? src}
        src={directSrc}
        poster={poster ?? undefined}
        playsInline
        preload="metadata"
        className="aspect-video w-full bg-black"
        aria-label={title}
        onClick={commands.togglePlay}
      />
      {/* Center play affordance when paused and ready. */}
      {!loading && !error && !state.playing && (
        <button
          type="button"
          aria-label="재생"
          onClick={commands.togglePlay}
          className="absolute inset-0 grid place-items-center bg-black/20"
        >
          <span className="grid h-14 w-14 place-items-center rounded-full bg-black/55 text-white">
            <Play size={26} fill="currentColor" />
          </span>
        </button>
      )}
      {loading && hlsUrl && (
        <div className="absolute inset-x-0 bottom-0 px-3 pb-3">
          <HlsLoadingBar progress={progress} />
        </div>
      )}
      {error && (
        <div className="absolute inset-0 grid place-items-center bg-black/70 px-4 text-center text-sm text-white/90">
          영상을 재생할 수 없습니다.
        </div>
      )}
    </PlayerShell>
  );
}
