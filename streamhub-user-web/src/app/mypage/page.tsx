"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarDays, Church, LogOut, Mail, Phone, Receipt, ShoppingBag, User } from "lucide-react";
import clsx from "clsx";
import { useAuth } from "@/lib/auth";
import { useMyOrders, ORDER_STATUS_LABELS, type OrderStatus } from "@/lib/orders";
import { formatDate } from "@/lib/format";
import { NearbyChurchesSection } from "@/components/NearbyChurchesSection";

function Row({ icon: Icon, label, value }: { icon: typeof Mail; label: string; value: string | null }) {
  return (
    <div className="flex items-center gap-3 px-4 py-3.5">
      <Icon className="h-4 w-4 shrink-0 text-inactive" />
      <span className="w-16 shrink-0 text-sm text-inactive">{label}</span>
      <span className="truncate text-sm font-medium text-active">{value || "-"}</span>
    </div>
  );
}

/** Status pill color: PAID is primary, terminal-failure states are muted/point. */
function statusClass(status: OrderStatus): string {
  if (status === "PAID") return "bg-primary/15 text-primary";
  if (status === "CANCELLED" || status === "FAILED") return "bg-point/15 text-point";
  return "bg-card text-inactive";
}

/** Purchase history — only rendered for signed-in members (token-gated GET /pub/v1/orders). */
function OrderHistorySection({ token }: { token: string }) {
  const { data, isLoading, isError } = useMyOrders(token);
  const orders = data ?? [];

  return (
    <section className="mt-7">
      <h2 className="flex items-center gap-2 pb-3 text-base font-bold text-active">
        <ShoppingBag className="h-4.5 w-4.5 text-primary" />
        구매 내역
      </h2>

      {isLoading ? (
        <div className="space-y-2.5">
          {Array.from({ length: 2 }).map((_, i) => (
            <div key={i} className="skeleton h-[68px] rounded-card" />
          ))}
        </div>
      ) : isError ? (
        <p className="rounded-card border border-border bg-surface px-4 py-5 text-center text-sm text-inactive">
          구매 내역을 불러오지 못했습니다.
        </p>
      ) : orders.length === 0 ? (
        <div className="rounded-card border border-border bg-surface px-4 py-7 text-center">
          <Receipt className="mx-auto h-7 w-7 text-inactive" />
          <p className="mt-2 text-sm text-inactive">아직 구매한 음반이 없습니다.</p>
        </div>
      ) : (
        <ul className="divide-y divide-border/60 overflow-hidden rounded-card border border-border/70 bg-surface">
          {orders.map((order) => (
            <li key={order.orderNo} className="px-4 py-3.5">
              <div className="flex items-center justify-between gap-3">
                <p className="ellipsis-1 text-sm font-bold text-active">{order.firstItemName}</p>
                <span
                  className={clsx(
                    "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-bold",
                    statusClass(order.status),
                  )}
                >
                  {ORDER_STATUS_LABELS[order.status] ?? order.status}
                </span>
              </div>
              <div className="mt-1 flex items-center justify-between gap-3">
                <span className="font-mono text-[11px] text-inactive">{order.orderNo}</span>
                <span className="text-sm font-bold text-primary">
                  {order.total.toLocaleString()}원
                </span>
              </div>
              <p className="mt-0.5 text-[11px] text-inactive">{formatDate(order.orderedAt)}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default function MyPage() {
  const { member, token, loading, logout } = useAuth();
  const router = useRouter();
  const [leaving, setLeaving] = useState(false);

  // Redirect unauthenticated visitors to login — but not while we're logging out (→ home).
  useEffect(() => {
    if (!loading && !member && !leaving) router.replace("/login");
  }, [loading, member, leaving, router]);

  if (loading || !member) {
    return (
      <div className="animate-fade-up px-5 pt-10">
        <div className="skeleton mx-auto h-20 w-20 rounded-full" />
        <div className="skeleton mx-auto mt-4 h-6 w-32 rounded" />
      </div>
    );
  }

  return (
    <section className="animate-fade-up px-5 pt-8">
      <div className="flex flex-col items-center">
        <div className="grid h-20 w-20 place-items-center rounded-full bg-gradient-to-br from-primary/40 to-secondary/40">
          <User className="h-9 w-9 text-active" />
        </div>
        <h1 className="mt-3 text-xl font-bold">{member.name}</h1>
        <p className="text-sm text-inactive">{member.email}</p>
      </div>

      <div className="mt-7 divide-y divide-border/60 overflow-hidden rounded-card border border-border/70 bg-surface">
        <Row icon={Mail} label="이메일" value={member.email} />
        <Row icon={Church} label="교회" value={member.churchName} />
        <Row icon={Phone} label="연락처" value={member.phone} />
        <Row icon={CalendarDays} label="가입일" value={formatDate(member.createdAt)} />
      </div>

      <button
        onClick={() => {
          setLeaving(true);
          logout();
          router.replace("/");
        }}
        className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl border border-border py-3 text-sm font-medium text-active active:bg-card"
      >
        <LogOut className="h-4 w-4" />
        로그아웃
      </button>

      {/* Purchase history — token is guaranteed non-null here (member is set). */}
      {token && <OrderHistorySection token={token} />}

      {/* Near-me churches widget — parent already pads px-5, so cancel it here. */}
      <div className="-mx-5 mt-2">
        <NearbyChurchesSection />
      </div>
    </section>
  );
}
