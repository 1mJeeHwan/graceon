"use client";

import Link from "next/link";
import { Loader2, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { couponRedemptions } from "@/apis/query/coupon/coupon";
import type { CouponDto, CouponRedemptionItem } from "@/apis/query/streamHubAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

const formatDateTime = (value?: string): string => {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
};

/**
 * Modal listing a coupon's redemption history (who redeemed it and when), fetched from
 * {@code GET /v1/coupon/{id}/redemptions}. Member names link to the member detail screen. The
 * dataset is the real per-member redemption ledger; seeded coupons whose usage counter was set
 * without going through the redeem flow show an empty state.
 */
export default function CouponRedemptionsDialog({
  coupon,
  onClose,
}: {
  coupon: CouponDto;
  onClose: () => void;
}) {
  const query = useQuery({
    queryKey: ["coupon-redemptions", coupon.id],
    queryFn: ({ signal }) => couponRedemptions(coupon.id as number, signal),
    enabled: coupon.id != null,
  });

  const rows: CouponRedemptionItem[] =
    query.data?.resultCode === SUCCESS_CODE
      ? (query.data?.resultObject ?? [])
      : [];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-md border border-slate-200 bg-white shadow-lg"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">쿠폰 사용 내역</h2>
            <p className="mt-0.5 text-xs text-slate-500">
              {coupon.code} · {coupon.name}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-500 transition hover:bg-slate-100"
            aria-label="닫기"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="max-h-[60vh] overflow-y-auto p-5">
          {query.isLoading ? (
            <div className="flex h-32 items-center justify-center">
              <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
            </div>
          ) : query.isError ? (
            <p className="py-8 text-center text-sm text-red-600">
              사용 내역을 불러오지 못했습니다.
            </p>
          ) : rows.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">
              기록된 사용 이력이 없습니다.
            </p>
          ) : (
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="text-left text-xs font-medium text-slate-500">
                <tr>
                  <th className="px-2 py-2">회원</th>
                  <th className="px-2 py-2">사용 시각</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td className="px-2 py-2">
                      {row.memberId != null ? (
                        <Link
                          href={`/member/${row.memberId}`}
                          className="text-brand hover:underline"
                          title="회원 상세로 이동"
                        >
                          {row.memberName ?? `회원 #${row.memberId}`}
                        </Link>
                      ) : (
                        (row.memberName ?? "-")
                      )}
                    </td>
                    <td className="px-2 py-2 text-slate-500">
                      {formatDateTime(row.redeemedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
