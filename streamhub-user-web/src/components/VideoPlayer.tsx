"use client";

/** Responsive HTML5 video player. mediaUrl is a direct .mp4 — no extra deps needed. */
export function VideoPlayer({ src, title }: { src: string; title: string }) {
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
