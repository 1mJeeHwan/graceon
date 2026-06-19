// YouTube IFrame Player API helpers: id extraction, one-time script loading, and the minimal
// typed surface of window.YT.Player that our custom controls drive.

/** Minimal player-state enum values exposed by the IFrame API (we only branch on PLAYING). */
export const YT_STATE = {
  UNSTARTED: -1,
  ENDED: 0,
  PLAYING: 1,
  PAUSED: 2,
  BUFFERING: 3,
  CUED: 5,
} as const;

/** Subset of YT.Player methods our controls call. */
export interface YouTubePlayer {
  playVideo(): void;
  pauseVideo(): void;
  seekTo(seconds: number, allowSeekAhead: boolean): void;
  getCurrentTime(): number;
  getDuration(): number;
  getPlayerState(): number;
  getVideoLoadedFraction(): number;
  mute(): void;
  unMute(): void;
  isMuted(): boolean;
  setVolume(volume: number): void;
  getVolume(): number;
  destroy(): void;
}

interface YouTubePlayerVars {
  controls?: 0 | 1;
  modestbranding?: 0 | 1;
  playsinline?: 0 | 1;
  rel?: 0 | 1;
}

interface YouTubePlayerOptions {
  videoId: string;
  /** Iframe dimensions; pass "100%" so the player fills its aspect-video container. */
  width?: string | number;
  height?: string | number;
  playerVars?: YouTubePlayerVars;
  events?: {
    onReady?: (event: { target: YouTubePlayer }) => void;
    onStateChange?: (event: { data: number; target: YouTubePlayer }) => void;
    onError?: (event: { data: number }) => void;
  };
}

interface YouTubeNamespace {
  Player: new (element: HTMLElement | string, options: YouTubePlayerOptions) => YouTubePlayer;
}

declare global {
  interface Window {
    YT?: YouTubeNamespace;
    onYouTubeIframeAPIReady?: () => void;
  }
}

/** Extracts the 11-char YouTube video id from common URL forms, or null when not a YouTube link. */
export function youtubeId(url: string | null | undefined): string | null {
  if (!url) return null;
  const patterns = [
    /youtube\.com\/watch\?(?:.*&)?v=([\w-]{11})/,
    /youtu\.be\/([\w-]{11})/,
    /youtube\.com\/embed\/([\w-]{11})/,
    /youtube\.com\/shorts\/([\w-]{11})/,
  ];
  for (const re of patterns) {
    const m = url.match(re);
    if (m) return m[1];
  }
  return null;
}

const SCRIPT_SRC = "https://www.youtube.com/iframe_api";
let apiPromise: Promise<YouTubeNamespace> | null = null;

/**
 * Loads the YouTube IFrame Player API once and resolves with {@code window.YT} when ready.
 * Subsequent calls return the same promise (guards against duplicate <script> injection).
 */
export function loadYouTubeIframeApi(): Promise<YouTubeNamespace> {
  if (apiPromise) return apiPromise;

  apiPromise = new Promise<YouTubeNamespace>((resolve) => {
    if (window.YT?.Player) {
      resolve(window.YT);
      return;
    }

    // The API invokes this global once the script finishes loading. Chain any existing handler
    // so we don't clobber a concurrent loader.
    const previous = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      previous?.();
      if (window.YT) resolve(window.YT);
    };

    if (!document.querySelector(`script[src="${SCRIPT_SRC}"]`)) {
      const script = document.createElement("script");
      script.src = SCRIPT_SRC;
      script.async = true;
      document.head.appendChild(script);
    }
  });

  return apiPromise;
}
