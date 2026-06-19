"use client";

import Link from "next/link";
import { ExternalLink, MapPin, Navigation, Phone } from "lucide-react";
import clsx from "clsx";
import type { ChurchNearbyItem } from "@/lib/churchTypes";
import { denominationLabel } from "@/lib/churchTypes";
import { formatDistance } from "@/lib/format";

/**
 * Distance-sorted church row. DB-backed churches link to the internal detail page; real-time
 * Kakao discovery rows (`dataSource="KAKAO_POI"`) open the Kakao map in a new tab instead, since
 * they have no internal detail. Hovering/focusing calls `onHover` so the parent can highlight the
 * matching map marker.
 */
export function ChurchCard({
  church,
  active,
  onHover,
}: {
  church: ChurchNearbyItem;
  active?: boolean;
  onHover?: (id: number | undefined) => void;
}) {
  const dist = formatDistance(church.distanceKm);
  const isPoi = church.dataSource === "KAKAO_POI";

  const className = clsx(
    "flex gap-3 rounded-card border bg-surface p-3 transition-colors",
    active ? "border-primary" : "border-border/70 hover:border-border",
  );
  const hoverProps = {
    onMouseEnter: () => onHover?.(church.id),
    onMouseLeave: () => onHover?.(undefined),
    onFocus: () => onHover?.(church.id),
  };

  const inner = (
    <>
      <div className="thumb h-16 w-16 shrink-0 rounded-lg">
        {church.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={church.thumbnailUrl} alt="" className="h-full w-full object-cover" loading="lazy" />
        ) : (
          <div className="grid h-full w-full place-items-center text-noimg">
            <Navigation className="h-5 w-5" />
          </div>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <h3 className="ellipsis-1 text-sm font-bold text-active">{church.name}</h3>
          {dist && (
            <span className="shrink-0 rounded-full bg-primary/15 px-2 py-0.5 text-[11px] font-bold text-primary">
              {dist}
            </span>
          )}
        </div>
        <p className="mt-0.5 flex items-center gap-1 text-[11px] text-inactive">
          {isPoi ? (
            <>
              <ExternalLink className="h-3 w-3 shrink-0" />
              카카오 지도에서 보기
            </>
          ) : (
            denominationLabel(church.denomination)
          )}
        </p>
        {church.address && (
          <p className="mt-1 flex items-start gap-1 text-[11px] text-inactive">
            <MapPin className="mt-0.5 h-3 w-3 shrink-0" />
            <span className="ellipsis-1">{church.address}</span>
          </p>
        )}
        {church.phone && (
          <p className="mt-0.5 flex items-center gap-1 text-[11px] text-inactive">
            <Phone className="h-3 w-3 shrink-0" />
            {church.phone}
          </p>
        )}
      </div>
    </>
  );

  if (isPoi) {
    return (
      <a
        href={church.externalUrl ?? "#"}
        target="_blank"
        rel="noreferrer"
        className={className}
        {...hoverProps}
      >
        {inner}
      </a>
    );
  }

  return (
    <Link href={`/churches/${church.id}`} className={className} {...hoverProps}>
      {inner}
    </Link>
  );
}
