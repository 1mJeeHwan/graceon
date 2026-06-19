"use client";

/** Extracts the 11-char YouTube video id from common URL forms, or null when not a YouTube link. */
function youtubeId(url: string | null | undefined): string | null {
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

/**
 * Responsive video player. When {@code src} is a YouTube link it embeds the YouTube player
 * (no hosting/storage needed); otherwise it falls back to a direct HTML5 {@code <video>} for a
 * file URL — so existing direct-URL videos keep working.
 */
export function VideoPlayer({ src, title }: { src: string; title: string }) {
  const ytId = youtubeId(src);

  if (ytId) {
    return (
      <div className="overflow-hidden bg-black sm:rounded-card">
        <iframe
          key={ytId}
          className="aspect-video w-full"
          src={`https://www.youtube.com/embed/${ytId}`}
          title={title}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
        />
      </div>
    );
  }

  return (
    <div className="overflow-hidden bg-black sm:rounded-card">
      <video
        key={src}
        controls
        playsInline
        preload="metadata"
        className="aspect-video w-full bg-black"
        aria-label={title}
      >
        <source src={src} />
        브라우저가 비디오 재생을 지원하지 않습니다.
      </video>
    </div>
  );
}
