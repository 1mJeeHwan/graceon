"use client";

import { useState } from "react";
import Link from "next/link";
import { ShoppingBag } from "lucide-react";
import type { GoodsListItem } from "@/lib/goods";

/** Goods card: square thumbnail + name + price, with a "품절" overlay when sold out. */
export function GoodsCard({ item }: { item: GoodsListItem }) {
  const [failed, setFailed] = useState(false);
  const showImage = item.thumbnailUrl && !failed;

  return (
    <Link href={`/goods/${item.id}`} className="block w-full">
      <div className="thumb aspect-square">
        {showImage ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={item.thumbnailUrl ?? ""}
            alt={item.name}
            loading="lazy"
            onError={() => setFailed(true)}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 grid place-items-center bg-gradient-to-br from-card to-surface">
            <ShoppingBag className="h-8 w-8 text-inactive" />
          </div>
        )}
        {item.soldOut && (
          <div className="absolute inset-0 grid place-items-center bg-bg/60 backdrop-blur-[1px]">
            <span className="rounded-full bg-bg/80 px-3 py-1 text-xs font-bold text-inactive">
              품절
            </span>
          </div>
        )}
      </div>
      <div className="mt-10px">
        <p className="ellipsis-1 text-16px font-bold leading-20px text-active">{item.name}</p>
        <p className="mt-1 text-[13px] font-bold text-primary">{item.price.toLocaleString()}원</p>
      </div>
    </Link>
  );
}
