"use client";

import { Ticket } from "lucide-react";
import clsx from "clsx";
import { useMyCoupons, type MyCouponItem } from "@/lib/me";
import { formatDate } from "@/lib/format";
import { SectionShell } from "./SectionShell";

/** Human discount, e.g. "3,000원 할인" or "10% 할인". */
function discountLabel(c: MyCouponItem): string {
  return c.discountType === "PERCENT"
    ? `${c.discountValue}% 할인`
    : `${c.discountValue.toLocaleString()}원 할인`;
}

/** The member's coupon wallet — active coupons, dimmed once used. */
export function CouponsSection({ token }: { token: string }) {
  const { data, isLoading, isError } = useMyCoupons(token);
  const coupons = data ?? [];

  return (
    <SectionShell
      icon={Ticket}
      title="쿠폰함"
      isLoading={isLoading}
      isError={isError}
      isEmpty={coupons.length === 0}
      errorMessage="쿠폰을 불러오지 못했습니다."
      emptyIcon={Ticket}
      emptyMessage="사용 가능한 쿠폰이 없습니다."
    >
      <ul className="space-y-2.5">
        {coupons.map((c) => (
          <li
            key={c.id}
            className={clsx(
              "flex items-center gap-3 rounded-card border px-4 py-3.5",
              c.used ? "border-border/60 bg-surface opacity-55" : "border-primary/30 bg-primary/5",
            )}
          >
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <p className="ellipsis-1 text-sm font-bold text-active">{c.name}</p>
                {c.used && (
                  <span className="shrink-0 rounded-full bg-card px-2 py-0.5 text-[10px] font-bold text-inactive">
                    사용완료
                  </span>
                )}
              </div>
              <p className="mt-0.5 text-[11px] text-inactive">
                코드 <span className="font-mono text-active">{c.code}</span>
                {c.minOrderAmount > 0 && ` · ${c.minOrderAmount.toLocaleString()}원 이상`}
                {c.validUntil && ` · ~${formatDate(c.validUntil)}`}
              </p>
            </div>
            <span className="shrink-0 text-sm font-extrabold text-primary">{discountLabel(c)}</span>
          </li>
        ))}
      </ul>
    </SectionShell>
  );
}
