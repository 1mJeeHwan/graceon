"use client";

import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import Link from "next/link";
import { Lock, ShieldCheck } from "lucide-react";
import { getStoredToken } from "@/lib/auth";

// Mirror the private BASE in src/lib/api.ts — the encrypted-HLS endpoints live under the same
// public API host. The playlist/segments are public; only the AES key request is gated.
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type PlayerError = "gated" | "fatal";

/**
 * Full-track player for an AES-128 encrypted HLS stream (purchasers only).
 *
 * Flow: hls.js fetches the public `index.m3u8` and the cross-origin encrypted `.ts` segments
 * straight from the CDN, then requests the RELATIVE `key` (resolved to `.../hls/key`). Only the
 * key request carries the member Bearer token, via {@link Hls} `xhrSetup` keyed on the URL. The
 * backend returns the 16-byte key only to a member who purchased the album (403 otherwise).
 */
export function HlsTrackPlayer({
  albumId,
  trackId,
  title,
}: {
  albumId: number;
  trackId: number;
  title: string;
}) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [error, setError] = useState<PlayerError | null>(null);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const playlistUrl = `${API_BASE}/pub/v1/albums/${albumId}/tracks/${trackId}/hls/index.m3u8`;
    setError(null);

    // hls.js path (Chrome/Firefox/Edge + Android) — the primary path, because only here can we
    // inject the Authorization header onto the gated key request.
    if (Hls.isSupported()) {
      const hls = new Hls({
        xhrSetup: (xhr, url) => {
          // Attach the member token ONLY to the gated key request — never to the public
          // playlist or the cross-origin CDN segments (which take no auth).
          if (url.includes("/hls/key")) {
            const token = getStoredToken();
            if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
          }
        },
      });

      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal) return;
        // A 403 on the key request means the member hasn't purchased this album. hls.js reports
        // it as a KEY_LOAD_ERROR with the HTTP status on data.response.code.
        const isKeyLoadError = data.details === Hls.ErrorDetails.KEY_LOAD_ERROR;
        const is403 = data.response?.code === 403;
        setError(isKeyLoadError && is403 ? "gated" : "fatal");
      });

      hls.loadSource(playlistUrl);
      hls.attachMedia(audio);

      return () => {
        hls.destroy();
      };
    }

    // Safari native-HLS fallback. CAVEAT: native HLS issues the AES key request itself and gives
    // us no hook to add the Authorization header, so the gated key returns 403 and full playback
    // will NOT authorize on Safari. The hls.js path above is the supported one; this only lets the
    // <audio> element attempt the stream rather than appearing dead.
    if (audio.canPlayType("application/vnd.apple.mpegurl")) {
      audio.src = playlistUrl;
      const onErr = () => setError("gated");
      audio.addEventListener("error", onErr);
      return () => audio.removeEventListener("error", onErr);
    }

    setError("fatal");
  }, [albumId, trackId]);

  return (
    <div className="mt-2 rounded-lg border border-primary/30 bg-primary/5 px-3 py-3">
      <div className="flex items-center justify-between gap-2">
        <p className="ellipsis-1 min-w-0 text-sm font-bold text-active">{title}</p>
        <span className="flex shrink-0 items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
          <ShieldCheck className="h-3 w-3" />
          암호화 스트리밍 (HLS·AES-128)
        </span>
      </div>

      {/* eslint-disable-next-line jsx-a11y/media-has-caption */}
      <audio ref={audioRef} controls className="mt-2.5 w-full" />

      {error === "gated" && (
        <div className="mt-2.5 rounded-lg bg-point/10 px-3 py-2.5 text-center">
          <p className="flex items-center justify-center gap-1.5 text-xs font-bold text-point">
            <Lock className="h-3.5 w-3.5" />
            구매한 회원만 전체 재생할 수 있습니다
          </p>
          <Link
            href={`/albums/${albumId}`}
            className="btn-primary mt-2.5 inline-flex px-4 py-2 text-xs font-bold"
          >
            구매하고 전체 듣기
          </Link>
        </div>
      )}

      {error === "fatal" && (
        <p className="mt-2.5 text-center text-xs text-inactive">재생할 수 없습니다.</p>
      )}
    </div>
  );
}
