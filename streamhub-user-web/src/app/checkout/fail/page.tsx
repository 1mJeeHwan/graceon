"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { XCircle } from "lucide-react";

/**
 * Toss redirect landing for a failed/cancelled payment. The window appends `code`/`message`
 * describing why (e.g. user cancel). No order is confirmed — the prepared order simply stays
 * unpaid. Shows the reason and lets the user retry.
 */
function CheckoutFailInner() {
  const params = useSearchParams();
  const code = params.get("code");
  const message = params.get("message") ?? "결제가 취소되었거나 실패했습니다.";

  return (
    <section className="animate-fade-up px-5 pt-12 text-center">
      <XCircle className="mx-auto h-14 w-14 text-point" />
      <h1 className="mt-3 text-xl font-bold text-active">결제를 완료하지 못했어요</h1>
      <p className="mx-auto mt-2 max-w-xs text-sm leading-relaxed text-inactive">{message}</p>
      {code && (
        <p className="mt-1 font-mono text-[11px] text-inactive/70">코드: {code}</p>
      )}
      <div className="mx-auto mt-6 flex max-w-sm gap-2">
        <Link href="/albums" className="btn-primary flex-1">
          다시 시도하기
        </Link>
        <Link
          href="/"
          className="flex-1 rounded-xl border border-border bg-surface px-4 py-3 text-center text-sm font-bold text-active transition active:bg-card"
        >
          홈으로
        </Link>
      </div>
    </section>
  );
}

export default function CheckoutFailPage() {
  return (
    <Suspense fallback={null}>
      <CheckoutFailInner />
    </Suspense>
  );
}
