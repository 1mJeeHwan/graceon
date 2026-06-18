"use client";

import Link from "next/link";
import { useBanners } from "@/lib/siteConfig";

/**
 * Admin-managed promotional banner row at the top of the home screen (position MAIN_TOP).
 * Driven by GET /pub/v1/banners — uploading/activating a banner in the admin makes it appear
 * here with no code change. Renders nothing when there are no active banners.
 */
export function HomeTopBanner() {
  const { data: banners } = useBanners("MAIN_TOP");
  if (!banners || banners.length === 0) return null;

  return (
    <div className="hrow px-5 pt-4">
      {banners.map((b) => {
        const card = (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={b.imageUrl}
            alt={b.title}
            className="h-[120px] w-[300px] rounded-card object-cover"
          />
        );
        return (
          <div key={b.id} className="overflow-hidden rounded-card">
            {b.linkUrl ? <Link href={b.linkUrl}>{card}</Link> : card}
          </div>
        );
      })}
    </div>
  );
}
