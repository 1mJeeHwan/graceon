import {
  PaymentListItemKind,
  PaymentListItemPayStatus,
} from "@/apis/query/graceOnAdminAPI.schemas";

/** Receipt-kind badge metadata (입금 vs 환불). */
export const KIND_META: Record<
  PaymentListItemKind,
  { label: string; className: string }
> = {
  [PaymentListItemKind.PAY]: {
    label: "입금",
    className: "bg-emerald-100 text-emerald-700",
  },
  [PaymentListItemKind.REFUND]: {
    label: "환불",
    className: "bg-rose-100 text-rose-700",
  },
};

/** Order pay-status badge metadata (C4 seam states). */
export const PAY_STATUS_META: Record<
  PaymentListItemPayStatus,
  { label: string; className: string }
> = {
  [PaymentListItemPayStatus.NONE]: {
    label: "미결제",
    className: "bg-slate-100 text-slate-500",
  },
  [PaymentListItemPayStatus.REQUESTED]: {
    label: "요청됨",
    className: "bg-amber-100 text-amber-700",
  },
  [PaymentListItemPayStatus.PENDING]: {
    label: "승인대기",
    className: "bg-amber-100 text-amber-700",
  },
  [PaymentListItemPayStatus.APPROVED]: {
    label: "승인",
    className: "bg-emerald-100 text-emerald-700",
  },
  [PaymentListItemPayStatus.FAILED]: {
    label: "실패",
    className: "bg-rose-100 text-rose-700",
  },
  [PaymentListItemPayStatus.CANCELED]: {
    label: "취소",
    className: "bg-slate-200 text-slate-600",
  },
};

/** Human label for a payment method code (BANK/CARD). */
export const PAY_METHOD_LABEL: Record<string, string> = {
  BANK: "무통장",
  CARD: "카드",
};
