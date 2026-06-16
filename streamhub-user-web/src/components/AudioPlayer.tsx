"use client";

import { Music } from "lucide-react";

/** Album-style audio player: artwork + native HTML5 audio controls. */
export function AudioPlayer({
  src,
  title,
  channelName,
  thumbnailUrl,
}: {
  src: string;
  title: string;
  channelName: string | null;
  thumbnailUrl: string | null;
}) {
  return (
    <div className="px-5">
      <div className="mx-auto mt-2 aspect-square w-full max-w-[280px] overflow-hidden rounded-card bg-gradient-to-br from-primary/30 via-surface to-bg">
        {thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={thumbnailUrl} alt={title} className="h-full w-full object-cover" />
        ) : (
          <div className="grid h-full w-full place-items-center">
            <Music className="h-14 w-14 text-inactive" />
          </div>
        )}
      </div>
      <div className="mt-5 text-center">
        <h1 className="text-lg font-bold text-active">{title}</h1>
        {channelName && <p className="mt-1 text-sm text-inactive">{channelName}</p>}
      </div>
      <audio key={src} controls preload="metadata" className="mt-4 w-full" aria-label={title}>
        <source src={src} />
        브라우저가 오디오 재생을 지원하지 않습니다.
      </audio>
    </div>
  );
}
