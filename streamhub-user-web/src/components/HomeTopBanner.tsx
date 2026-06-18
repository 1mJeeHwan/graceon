"use client";

import { useState } from "react";
import Link from "next/link";
import { useBanners } from "@/lib/siteConfig";

/**
 * Admin-managed promotional banner at the top of the home screen (the top-sorted active
 * MAIN_TOP banner). Driven by GET /pub/v1/banners — uploading/activating a banner in the
 * admin makes it appear here with no code change. Rendered as one full-width promo with the
 * title overlaid (intentional banner, not a thumbnail strip). Hidden when none / image fails.
 */
export function HomeTopBanner() {
  const { data: banners } = useBanners("MAIN_TOP");
  const [failed, setFailed] = useState(false);
  const banner = banners?.[0];
  if (!banner || failed) return null;

  const card = (
    <div className="relative aspect-[1200/300] w-full overflow-hidden rounded-card bg-card">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={banner.imageUrl}
        alt={banner.title}
        onError={() => setFailed(true)}
        className="h-full w-full object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/10 to-transparent" />
      <span className="absolute bottom-2.5 left-3.5 right-3.5 ellipsis-1 text-sm font-bold text-white drop-shadow">
        {banner.title}
      </span>
    </div>
  );

  return (
    <div className="px-5 pt-4">
      {banner.linkUrl ? <Link href={banner.linkUrl}>{card}</Link> : card}
    </div>
  );
}
