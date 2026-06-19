"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Loader2, PackageCheck, Truck, X } from "lucide-react";
import clsx from "clsx";
import {
  orderApi,
  useOrderReceipt,
  ORDER_STATUS_LABELS,
  type OrderStatus,
  type Tracking,
} from "@/lib/orders";

const krw = (n: number) => `${n.toLocaleString()}원`;

/** Date + HH:mm for a receipt timestamp; "" for null/invalid. */
function dateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

function statusClass(status: OrderStatus): string {
  if (status === "PAID" || status === "DONE") return "bg-primary/15 text-primary";
  if (status === "CANCEL" || status === "RETURN") return "bg-point/15 text-point";
  return "bg-card text-inactive";
}

const PAY_PROVIDER_LABELS: Record<string, string> = {
  TOSS: "토스",
  KAKAO: "카카오페이",
  PAYPAL: "PayPal",
  CARD: "신용·체크카드",
  MOCK: "데모 결제",
};

const PAY_METHOD_LABELS: Record<string, string> = {
  CARD: "카드",
  BANK: "계좌이체",
};

function AmountRow({ label, value, negative = false }: { label: string; value: number; negative?: boolean }) {
  return (
    <div className="flex items-center justify-between py-1 text-sm">
      <span className="text-inactive">{label}</span>
      <span className={clsx("tabular-nums", negative ? "text-point" : "text-active")}>
        {negative ? `- ${krw(value)}` : krw(value)}
      </span>
    </div>
  );
}

/** On-demand 배송조회 expander backed by the courier API (same seam as the order list). */
function TrackingPanel({ orderNo, token }: { orderNo: string; token: string }) {
  const [tracking, setTracking] = useState<Tracking | null>(null);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setOpen((v) => !v);
    if (tracking || loading) return;
    setLoading(true);
    setError(null);
    try {
      setTracking(await orderApi.tracking(orderNo, token));
    } catch (err) {
      setError(err instanceof Error ? err.message : "배송 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        type="button"
        onClick={load}
        className="flex items-center gap-1.5 text-xs font-semibold text-primary active:underline"
      >
        <Truck className="h-4 w-4" />
        배송조회
      </button>
      {open && (
        <div className="mt-2 rounded-lg border border-border bg-bg px-3 py-2.5">
          {loading ? (
            <div className="flex items-center gap-2 text-xs text-inactive">
              <Loader2 className="h-3.5 w-3.5 animate-spin" /> 배송 정보를 불러오는 중…
            </div>
          ) : error ? (
            <p className="text-xs text-point">{error}</p>
          ) : tracking ? (
            <div>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-inactive">
                <span>
                  택배사 <strong className="text-active">{tracking.carrierName ?? "-"}</strong>
                </span>
                <span
                  className={clsx(
                    "inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-bold",
                    tracking.completed ? "bg-primary/15 text-primary" : "bg-secondary/15 text-secondary",
                  )}
                >
                  {tracking.completed && <PackageCheck className="h-3 w-3" />}
                  {tracking.completed ? "배달완료" : "배송중"}
                </span>
              </div>
              {tracking.events.length > 0 ? (
                <ol className="mt-2 space-y-2 border-l border-border pl-3">
                  {tracking.events.map((event, i) => (
                    <li key={i} className="relative">
                      <span className="absolute -left-[15px] top-1 h-1.5 w-1.5 rounded-full bg-primary" />
                      <p className="text-xs text-active">{event.description ?? "-"}</p>
                      <p className="text-[11px] text-inactive">
                        {event.location ?? ""} · {event.time ?? ""}
                      </p>
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="mt-2 text-[11px] text-inactive">아직 등록된 배송 이벤트가 없습니다.</p>
              )}
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}

/**
 * Receipt detail modal ("영수증 상세"). Rendered via a portal to {@code document.body} so the
 * fixed bottom-sheet escapes the page's {@code animate-fade-up} transform (which would otherwise
 * re-anchor the fixed overlay). Lazily fetches the member-scoped receipt for {@code orderNo}.
 */
export function ReceiptModal({
  orderNo,
  token,
  onClose,
}: {
  orderNo: string;
  token: string;
  onClose: () => void;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  const { data, isLoading, isError } = useOrderReceipt(orderNo, token);

  if (!mounted) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-black/60"
      role="dialog"
      aria-modal="true"
      aria-label="영수증"
      onClick={onClose}
    >
      <div
        className="mx-auto flex max-h-[88dvh] w-full max-w-[480px] flex-col overflow-hidden rounded-t-2xl border-x border-t border-border bg-bg animate-fade-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-border bg-surface px-4 py-3.5">
          <h2 className="text-base font-bold text-active">영수증</h2>
          <button
            type="button"
            aria-label="닫기"
            onClick={onClose}
            className="rounded-lg p-1.5 text-inactive transition active:bg-card"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-4 py-4">
          {isLoading ? (
            <div className="flex items-center justify-center gap-2 py-12 text-sm text-inactive">
              <Loader2 className="h-4 w-4 animate-spin" /> 영수증을 불러오는 중…
            </div>
          ) : isError || !data ? (
            <p className="py-12 text-center text-sm text-inactive">영수증을 불러오지 못했습니다.</p>
          ) : (
            <div className="space-y-5">
              {/* Order header */}
              <div className="rounded-card border border-border bg-surface px-4 py-3.5">
                <div className="flex items-center justify-between gap-3">
                  <span className="font-mono text-xs text-inactive">{data.orderNo}</span>
                  <span
                    className={clsx(
                      "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-bold",
                      statusClass(data.status),
                    )}
                  >
                    {ORDER_STATUS_LABELS[data.status] ?? data.status}
                  </span>
                </div>
                <dl className="mt-2.5 space-y-1 text-[12px]">
                  <div className="flex justify-between">
                    <dt className="text-inactive">주문자</dt>
                    <dd className="text-active">{data.orderedName ?? "-"}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-inactive">주문일시</dt>
                    <dd className="text-active">{dateTime(data.orderedAt) || "-"}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-inactive">결제일시</dt>
                    <dd className="text-active">{dateTime(data.paidAt) || "-"}</dd>
                  </div>
                </dl>
              </div>

              {/* Line items */}
              <div>
                <h3 className="mb-2 text-xs font-bold text-inactive">주문 상품</h3>
                <ul className="divide-y divide-border/60 overflow-hidden rounded-card border border-border/70 bg-surface">
                  {data.items.map((line, i) => (
                    <li key={i} className="flex items-center justify-between gap-3 px-4 py-3">
                      <div className="min-w-0">
                        <p className="ellipsis-1 text-sm font-medium text-active">{line.goodsName}</p>
                        {line.optionName && (
                          <p className="ellipsis-1 text-[11px] text-inactive">{line.optionName}</p>
                        )}
                        <p className="mt-0.5 text-[11px] text-inactive">
                          {krw(line.unitPrice)} × {line.qty}
                        </p>
                      </div>
                      <span className="shrink-0 text-sm font-bold tabular-nums text-active">
                        {krw(line.lineTotal)}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Amount breakdown */}
              <div className="rounded-card border border-border bg-surface px-4 py-3">
                <AmountRow label="상품 합계" value={data.goodsTotal} />
                <AmountRow label="배송비" value={data.shipFee} />
                {data.couponDiscount > 0 && (
                  <AmountRow label="쿠폰 할인" value={data.couponDiscount} negative />
                )}
                {data.pointUsed > 0 && <AmountRow label="포인트 사용" value={data.pointUsed} negative />}
                <div className="mt-1.5 flex items-center justify-between border-t border-border pt-2.5">
                  <span className="text-sm font-bold text-active">결제 금액</span>
                  <span className="text-lg font-extrabold tabular-nums text-primary">
                    {krw(data.total)}
                  </span>
                </div>
              </div>

              {/* Payment info */}
              <div>
                <h3 className="mb-2 text-xs font-bold text-inactive">결제 정보</h3>
                <dl className="space-y-1 rounded-card border border-border bg-surface px-4 py-3 text-[12px]">
                  <div className="flex justify-between">
                    <dt className="text-inactive">결제수단</dt>
                    <dd className="text-active">
                      {data.payProvider
                        ? PAY_PROVIDER_LABELS[data.payProvider] ?? data.payProvider
                        : "-"}
                      {data.payMethod ? ` · ${PAY_METHOD_LABELS[data.payMethod] ?? data.payMethod}` : ""}
                    </dd>
                  </div>
                  {data.txnId && (
                    <div className="flex justify-between gap-3">
                      <dt className="shrink-0 text-inactive">거래번호</dt>
                      <dd className="ellipsis-1 font-mono text-[11px] text-active">{data.txnId}</dd>
                    </div>
                  )}
                </dl>
              </div>

              {/* Shipping (only when an address exists) */}
              {data.receiverName && (
                <div>
                  <h3 className="mb-2 text-xs font-bold text-inactive">배송 정보</h3>
                  <dl className="space-y-1 rounded-card border border-border bg-surface px-4 py-3 text-[12px]">
                    <div className="flex justify-between">
                      <dt className="text-inactive">수령인</dt>
                      <dd className="text-active">
                        {data.receiverName}
                        {data.receiverPhone ? ` · ${data.receiverPhone}` : ""}
                      </dd>
                    </div>
                    {data.receiverAddr && (
                      <div className="flex justify-between gap-3">
                        <dt className="shrink-0 text-inactive">주소</dt>
                        <dd className="text-right text-active">{data.receiverAddr}</dd>
                      </div>
                    )}
                    {data.trackingNo && (
                      <div className="flex justify-between gap-3">
                        <dt className="shrink-0 text-inactive">송장</dt>
                        <dd className="text-active">
                          {data.shipCompany ? `${data.shipCompany} ` : ""}
                          {data.trackingNo}
                        </dd>
                      </div>
                    )}
                    <div className="pt-1.5">
                      <TrackingPanel orderNo={data.orderNo} token={token} />
                    </div>
                  </dl>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}
