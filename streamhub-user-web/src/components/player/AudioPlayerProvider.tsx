"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import Hls from "hls.js";
import { getStoredToken } from "@/lib/auth";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
const DEFAULT_PREVIEW_LENGTH = 30;

/**
 * A playable audio source for the single app-wide player. Three kinds share one surface so that
 * the music tab, music detail and album page all play the exact same way:
 *  - `preview`: a 30s album-track clip (public HLS when packaged, else the direct previewUrl)
 *  - `full`: an album track's AES-128 encrypted full HLS stream (purchasers only; key is gated)
 *  - `content`: a music-tab content item played in full (public HLS when packaged, else mediaUrl)
 */
export type PlayableSource =
  | {
      kind: "preview";
      id: string;
      title: string;
      subtitle: string;
      coverUrl: string | null;
      albumId: number;
      trackId: number;
      previewUrl: string | null;
      previewStartSec: number;
      previewLengthSec: number;
      hasPreviewHls: boolean;
    }
  | {
      kind: "full";
      id: string;
      title: string;
      subtitle: string;
      coverUrl: string | null;
      albumId: number;
      trackId: number;
    }
  | {
      kind: "content";
      id: string;
      title: string;
      subtitle: string;
      coverUrl: string | null;
      contentId: number;
      hlsAvailable: boolean;
      mediaUrl: string;
    };

/** Convenience builders so callers don't hand-roll the discriminated source + id. */
export function previewSource(args: {
  albumId: number;
  trackId: number;
  title: string;
  artist: string;
  coverUrl: string | null;
  previewUrl: string | null;
  previewStartSec: number;
  previewLengthSec: number;
  hasPreviewHls: boolean;
}): PlayableSource {
  return {
    kind: "preview",
    id: `preview:${args.albumId}:${args.trackId}`,
    title: args.title,
    subtitle: args.artist,
    coverUrl: args.coverUrl,
    albumId: args.albumId,
    trackId: args.trackId,
    previewUrl: args.previewUrl,
    previewStartSec: args.previewStartSec,
    previewLengthSec: args.previewLengthSec,
    hasPreviewHls: args.hasPreviewHls,
  };
}

export function fullTrackSource(args: {
  albumId: number;
  trackId: number;
  title: string;
  artist: string;
  coverUrl: string | null;
}): PlayableSource {
  return {
    kind: "full",
    id: `full:${args.albumId}:${args.trackId}`,
    title: args.title,
    subtitle: args.artist,
    coverUrl: args.coverUrl,
    albumId: args.albumId,
    trackId: args.trackId,
  };
}

export function contentSource(args: {
  contentId: number;
  title: string;
  subtitle: string;
  coverUrl: string | null;
  hlsAvailable: boolean;
  mediaUrl: string;
}): PlayableSource {
  return {
    kind: "content",
    id: `content:${args.contentId}`,
    title: args.title,
    subtitle: args.subtitle,
    coverUrl: args.coverUrl,
    contentId: args.contentId,
    hlsAvailable: args.hlsAvailable,
    mediaUrl: args.mediaUrl,
  };
}

export type PlayerError = "gated" | "fatal";

interface AudioPlayerValue {
  current: PlayableSource | null;
  isPlaying: boolean;
  /** Seconds elapsed within the playing window (0 at the clip/track start). */
  currentTime: number;
  /** Window length in seconds (preview length, or full track duration once known). */
  duration: number;
  /** True while the stream is being prepared (HLS), before it can play. */
  loading: boolean;
  /** 0→100 "preparing to play" progress. */
  loadProgress: number;
  /** "gated" → full track not purchased (403 on key); "fatal" → source unplayable. */
  error: PlayerError | null;
  play: (source: PlayableSource) => void;
  toggle: () => void;
  seek: (seconds: number) => void;
  stop: () => void;
  isCurrent: (id: string) => boolean;
}

const AudioPlayerContext = createContext<AudioPlayerValue | null>(null);

interface SourceUrls {
  hls: string | null;
  direct: string | null;
  keyAuth: boolean;
}

function sourceUrls(source: PlayableSource): SourceUrls {
  switch (source.kind) {
    case "preview":
      return {
        hls: source.hasPreviewHls
          ? `${API_BASE}/pub/v1/albums/${source.albumId}/tracks/${source.trackId}/preview/index.m3u8`
          : null,
        direct: source.previewUrl,
        keyAuth: false,
      };
    case "full":
      return {
        hls: `${API_BASE}/pub/v1/albums/${source.albumId}/tracks/${source.trackId}/hls/index.m3u8`,
        direct: null,
        keyAuth: true,
      };
    case "content":
      return {
        hls: source.hlsAvailable
          ? `${API_BASE}/pub/v1/contents/${source.contentId}/hls/index.m3u8`
          : null,
        direct: source.mediaUrl,
        keyAuth: false,
      };
  }
}

interface PlayWindow {
  /** Element time offset where the window starts (preview direct seeks here; HLS starts at 0). */
  base: number;
  /** Window length, or null for a full-length track (no cutoff). */
  length: number | null;
}

function computeWindow(source: PlayableSource, viaHls: boolean): PlayWindow {
  if (source.kind === "preview") {
    return {
      base: viaHls ? 0 : source.previewStartSec ?? 0,
      length: source.previewLengthSec || DEFAULT_PREVIEW_LENGTH,
    };
  }
  return { base: 0, length: null };
}

/**
 * App-wide single-audio player. Exactly one HTMLAudioElement exists, so starting a new source
 * inherently stops the previous one. Preview clips are clamped to their window and auto-paused at
 * the cutoff; full tracks and content play their whole length. Encrypted full tracks attach the
 * member Bearer token to the gated {@code /hls/key} request only. The audio element is created
 * lazily inside the first user gesture (mobile autoplay policy).
 */
export function AudioPlayerProvider({ children }: { children: React.ReactNode }) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);
  const windowRef = useRef<PlayWindow>({ base: 0, length: null });

  const [current, setCurrent] = useState<PlayableSource | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadProgress, setLoadProgress] = useState(0);
  const [error, setError] = useState<PlayerError | null>(null);

  // Latest source kept in a ref so the long-lived audio listeners read fresh values.
  const currentRef = useRef<PlayableSource | null>(null);
  currentRef.current = current;

  const destroyHls = useCallback(() => {
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }
  }, []);

  const syncDuration = useCallback((audio: HTMLAudioElement) => {
    const w = windowRef.current;
    if (w.length != null) {
      setDuration(w.length);
    } else if (Number.isFinite(audio.duration) && audio.duration > 0) {
      setDuration(audio.duration);
    }
  }, []);

  const ensureAudio = useCallback((): HTMLAudioElement => {
    if (audioRef.current) return audioRef.current;
    const audio = new Audio();
    audio.preload = "metadata";

    audio.addEventListener("timeupdate", () => {
      if (!currentRef.current) return;
      const w = windowRef.current;
      const into = audio.currentTime - w.base;
      const clamped = w.length != null ? Math.min(Math.max(0, into), w.length) : Math.max(0, into);
      setCurrentTime(clamped);
      // Preview cutoff: stop once the clip window is exhausted.
      if (w.length != null && into >= w.length) {
        audio.pause();
        audio.currentTime = w.base;
        setCurrentTime(0);
        setIsPlaying(false);
      }
    });
    audio.addEventListener("durationchange", () => syncDuration(audio));
    audio.addEventListener("loadedmetadata", () => syncDuration(audio));
    audio.addEventListener("play", () => setIsPlaying(true));
    audio.addEventListener("pause", () => setIsPlaying(false));
    audio.addEventListener("ended", () => setIsPlaying(false));
    audio.addEventListener("canplay", () => {
      setLoadProgress(100);
      setLoading(false);
      syncDuration(audio);
    });
    audio.addEventListener("error", () => {
      setIsPlaying(false);
      setLoading(false);
      setError("fatal");
    });

    audioRef.current = audio;
    return audio;
  }, [syncDuration]);

  // Streams an HLS source via hls.js, attaching the member token to the gated key when needed.
  const playViaHlsJs = useCallback(
    (audio: HTMLAudioElement, source: PlayableSource, urls: SourceUrls) => {
      destroyHls();
      windowRef.current = computeWindow(source, true);
      setLoading(true);
      setLoadProgress(0);

      const hls = new Hls({
        xhrSetup: (xhr, url) => {
          if (urls.keyAuth && url.includes("/hls/key")) {
            const token = getStoredToken();
            if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
          }
        },
      });
      hlsRef.current = hls;
      let firstFrag = true;
      const bump = (p: number) => setLoadProgress((prev) => (p > prev ? p : prev));
      hls.on(Hls.Events.MANIFEST_LOADING, () => bump(15));
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        bump(45);
        try {
          audio.currentTime = 0;
        } catch {
          // currentTime may throw before metadata; the canplay path covers it.
        }
        void audio.play().catch(() => setIsPlaying(false));
      });
      hls.on(Hls.Events.FRAG_LOADED, () => {
        if (firstFrag) {
          firstFrag = false;
          bump(70);
        }
      });
      hls.on(Hls.Events.FRAG_BUFFERED, () => bump(90));
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal) return;
        const isKeyLoadError = data.details === Hls.ErrorDetails.KEY_LOAD_ERROR;
        const is403 = data.response?.code === 403;
        if (isKeyLoadError && is403) {
          setError("gated");
          setLoading(false);
          return;
        }
        // Public streams fall back to their direct URL when the manifest itself fails.
        if (urls.direct) {
          playDirectRef.current?.(audio, source, urls.direct);
          return;
        }
        setError("fatal");
        setLoading(false);
      });
      hls.loadSource(urls.hls as string);
      hls.attachMedia(audio);
    },
    [destroyHls],
  );

  // Plays a direct (non-HLS, or Safari-native-HLS) URL, seeking preview clips to their start.
  const playDirect = useCallback(
    (audio: HTMLAudioElement, source: PlayableSource, url: string) => {
      destroyHls();
      const nativeHls = url.endsWith(".m3u8");
      windowRef.current = computeWindow(source, nativeHls);
      if (nativeHls) {
        setLoading(true);
        setLoadProgress(30);
      } else {
        setLoading(false);
      }
      const start = windowRef.current.base;
      const startPlayback = () => {
        try {
          audio.currentTime = start;
        } catch {
          // currentTime may throw before metadata; loadedmetadata path covers it.
        }
        void audio.play().catch(() => setIsPlaying(false));
      };
      if (audio.src !== url) audio.src = url;
      if (audio.readyState < 1) {
        audio.addEventListener("loadedmetadata", startPlayback, { once: true });
        audio.load();
      } else {
        startPlayback();
      }
    },
    [destroyHls],
  );

  // Lets playViaHlsJs reach playDirect for its fatal-fallback without a declaration cycle.
  const playDirectRef = useRef<typeof playDirect | null>(null);
  playDirectRef.current = playDirect;

  const play = useCallback(
    (source: PlayableSource) => {
      const audio = ensureAudio();
      const sameSource = currentRef.current?.id === source.id;

      currentRef.current = source;
      setCurrent(source);
      setCurrentTime(0);
      setDuration(0);
      setError(null);

      // Same source already prepared → just restart it (cheap, keeps the HLS pipeline warm).
      if (sameSource && (hlsRef.current || audio.src)) {
        try {
          audio.currentTime = windowRef.current.base;
        } catch {
          // ignore
        }
        void audio.play().catch(() => setIsPlaying(false));
        return;
      }

      const urls = sourceUrls(source);
      const canNativeHls = audio.canPlayType("application/vnd.apple.mpegurl") !== "";

      if (urls.hls && Hls.isSupported()) {
        playViaHlsJs(audio, source, urls);
        return;
      }
      if (urls.hls && canNativeHls) {
        playDirect(audio, source, urls.hls);
        return;
      }
      if (urls.direct) {
        playDirect(audio, source, urls.direct);
        return;
      }
      // Encrypted full track on Safari (no key-injection hook) with no fallback → gated.
      setError(urls.keyAuth ? "gated" : "fatal");
    },
    [ensureAudio, playViaHlsJs, playDirect],
  );

  const toggle = useCallback(() => {
    const audio = audioRef.current;
    if (!audio || !currentRef.current) return;
    if (audio.paused) void audio.play().catch(() => setIsPlaying(false));
    else audio.pause();
  }, []);

  const seek = useCallback((seconds: number) => {
    const audio = audioRef.current;
    if (!audio || !currentRef.current) return;
    const w = windowRef.current;
    const max = w.length != null ? w.length : Number.isFinite(audio.duration) ? audio.duration : 0;
    const clamped = Math.min(Math.max(0, seconds), max || 0);
    try {
      audio.currentTime = w.base + clamped;
      setCurrentTime(clamped);
    } catch {
      // currentTime may throw before metadata loads; ignore.
    }
  }, []);

  const stop = useCallback(() => {
    const audio = audioRef.current;
    destroyHls();
    if (audio) {
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    }
    windowRef.current = { base: 0, length: null };
    currentRef.current = null;
    setCurrent(null);
    setCurrentTime(0);
    setDuration(0);
    setIsPlaying(false);
    setLoading(false);
    setLoadProgress(0);
    setError(null);
  }, [destroyHls]);

  const isCurrent = useCallback((id: string) => currentRef.current?.id === id, []);

  // Tear down the audio element + hls if the provider ever unmounts.
  useEffect(() => {
    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      audioRef.current?.pause();
      audioRef.current = null;
    };
  }, []);

  const value = useMemo<AudioPlayerValue>(
    () => ({
      current,
      isPlaying,
      currentTime,
      duration,
      loading,
      loadProgress,
      error,
      play,
      toggle,
      seek,
      stop,
      isCurrent,
    }),
    [current, isPlaying, currentTime, duration, loading, loadProgress, error, play, toggle, seek, stop, isCurrent],
  );

  return <AudioPlayerContext.Provider value={value}>{children}</AudioPlayerContext.Provider>;
}

/** Access the global audio player. Must be used under <AudioPlayerProvider>. */
export function useAudioPlayer(): AudioPlayerValue {
  const ctx = useContext(AudioPlayerContext);
  if (!ctx) throw new Error("useAudioPlayer must be used within <AudioPlayerProvider>");
  return ctx;
}
