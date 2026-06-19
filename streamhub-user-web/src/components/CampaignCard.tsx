"use client";

import { useState } from "react";
import Link from "next/link";
import { CalendarRange } from "lucide-react";
import clsx from "clsx";
import { campaignStatusLabel, type CampaignListItem } from "@/lib/campaigns";
import { formatDate } from "@/lib/format";

/** Badge tint per known status; unknown statuses get the neutral inactive style. */
function statusClass(status: string): string {
  switch (status) {
    case "ACTIVE":
      return "bg-primary/15 text-primary";
    case "SCHEDULED":
      return "bg-active/10 text-active";
    default:
      return "bg-card text-inactive";
  }
}

/** Campaign/event card: 16:9 banner (gradient fallback) + status badge + title + period. */
export function CampaignCard({ item }: { item: CampaignListItem }) {
  const [failed, setFailed] = useState(false);
  const showImage = item.imageUrl && !failed;
  const period = `${formatDate(item.startAt)} ~ ${formatDate(item.endAt)}`;

  return (
    <Link href={`/campaigns/${item.id}`} className="block w-full">
      <div className="thumb aspect-video">
        {showImage ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={item.imageUrl ?? ""}
            alt={item.title}
            loading="lazy"
            onError={() => setFailed(true)}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 grid place-items-center bg-gradient-to-br from-primary/25 via-card to-surface">
            <CalendarRange className="h-8 w-8 text-inactive" />
          </div>
        )}
        <span
          className={clsx(
            "absolute left-1.5 top-1.5 rounded px-1.5 py-0.5 text-[10px] font-bold backdrop-blur",
            statusClass(item.status),
          )}
        >
          {campaignStatusLabel(item.status)}
        </span>
      </div>
      <div className="mt-10px">
        <p className="ellipsis-2 text-16px font-bold leading-20px text-active">{item.title}</p>
        {item.summary && (
          <p className="ellipsis-1 mt-0.5 text-12px font-medium leading-20px text-inactive">
            {item.summary}
          </p>
        )}
        <p className="ellipsis-1 mt-1 flex items-center gap-1 text-[11px] font-medium text-inactive">
          <CalendarRange className="h-3 w-3 shrink-0" />
          {period}
        </p>
      </div>
    </Link>
  );
}
