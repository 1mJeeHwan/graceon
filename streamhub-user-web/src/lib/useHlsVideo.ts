"use client";

import { useEffect, useState, type RefObject } from "react";
import Hls from "hls.js";

export type HlsVideoError = "fatal";

export interface HlsVideoState {
  /** True while the stream is being prepared (until it can play). */
  loading: boolean;
  /** 0→100 "preparing to play" progress, driven by hls.js (or native buffering) events. */
  progress: number;
  error: HlsVideoError | null;
}

/**
 * Loads an HLS playlist into the given {@code <video>} element via hls.js (Safari native fallback)
 * and exposes a 0→100% "preparing to play" progress until the stream can play.
 *
 * <p>Public HLS only — no key auth is needed for video streams. Passing a null {@code playlistUrl}
 * disables the hook (caller falls back to a direct source).
 */
export function useHlsVideo(
  videoRef: RefObject<HTMLVideoElement>,
  opts: { playlistUrl: string | null },
): HlsVideoState {
  const { playlistUrl } = opts;
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<HlsVideoError | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !playlistUrl) return;

    setLoading(true);
    setProgress(0);
    setError(null);

    // Progress only moves forward — events can arrive out of order across segments.
    const bump = (p: number) => setProgress((prev) => (p > prev ? p : prev));
    const onCanPlay = () => {
      setProgress(100);
      setLoading(false);
    };
    video.addEventListener("canplay", onCanPlay);

    if (Hls.isSupported()) {
      const hls = new Hls();
      let firstFrag = true;
      hls.on(Hls.Events.MANIFEST_LOADING, () => bump(15));
      hls.on(Hls.Events.MANIFEST_PARSED, () => bump(45));
      hls.on(Hls.Events.FRAG_LOADED, () => {
        if (firstFrag) {
          firstFrag = false;
          bump(70);
        }
      });
      hls.on(Hls.Events.FRAG_BUFFERED, () => bump(90));
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal) return;
        setError("fatal");
        setLoading(false);
      });
      hls.loadSource(playlistUrl);
      hls.attachMedia(video);
      return () => {
        video.removeEventListener("canplay", onCanPlay);
        hls.destroy();
      };
    }

    // Safari native HLS — public streams play fine.
    if (video.canPlayType("application/vnd.apple.mpegurl")) {
      const onLoadStart = () => bump(20);
      const onProgress = () => {
        try {
          if (video.duration > 0 && video.buffered.length > 0) {
            const pct = (video.buffered.end(0) / video.duration) * 100;
            bump(Math.min(95, Math.max(20, pct)));
          }
        } catch {
          // buffered may throw before metadata loads
        }
      };
      const onErr = () => {
        setError("fatal");
        setLoading(false);
      };
      video.addEventListener("loadstart", onLoadStart);
      video.addEventListener("progress", onProgress);
      video.addEventListener("error", onErr);
      video.src = playlistUrl;
      return () => {
        video.removeEventListener("canplay", onCanPlay);
        video.removeEventListener("loadstart", onLoadStart);
        video.removeEventListener("progress", onProgress);
        video.removeEventListener("error", onErr);
      };
    }

    setError("fatal");
    setLoading(false);
    return () => video.removeEventListener("canplay", onCanPlay);
  }, [videoRef, playlistUrl]);

  return { loading, progress, error };
}
