import {
  SmsListItemChannel,
  SmsListItemKind,
  SmsListItemStatus,
} from "@/apis/query/streamHubAdminAPI.schemas";

const BADGE_BASE =
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium";
const NEUTRAL = "bg-slate-200 text-slate-600";

const KIND_CONFIG: Record<string, { label: string; className: string }> = {
  [SmsListItemKind.CUSTOM]: {
    label: "커스텀",
    className: "bg-indigo-100 text-indigo-700",
  },
  [SmsListItemKind.ORDER_PAID]: {
    label: "주문결제",
    className: "bg-sky-100 text-sky-700",
  },
  [SmsListItemKind.ORDER_SHIPPING]: {
    label: "배송",
    className: "bg-cyan-100 text-cyan-700",
  },
  [SmsListItemKind.DONATION_ONCE]: {
    label: "후원",
    className: "bg-rose-100 text-rose-700",
  },
};

const STATUS_CONFIG: Record<string, { label: string; className: string }> = {
  [SmsListItemStatus.QUEUED]: {
    label: "대기",
    className: "bg-amber-100 text-amber-700",
  },
  [SmsListItemStatus.SENT]: {
    label: "발송완료",
    className: "bg-emerald-100 text-emerald-700",
  },
  [SmsListItemStatus.FAILED]: {
    label: "실패",
    className: "bg-red-100 text-red-700",
  },
};

const CHANNEL_CONFIG: Record<string, { label: string; className: string }> = {
  [SmsListItemChannel.SMS]: {
    label: "SMS",
    className: "bg-slate-100 text-slate-600",
  },
  [SmsListItemChannel.LMS]: {
    label: "LMS",
    className: "bg-violet-100 text-violet-700",
  },
};

interface BadgeProps {
  value?: string;
}

/** SmsKindBadge renders a colored pill for an SMS message kind (커스텀/주문결제/배송/후원). */
export function SmsKindBadge({ value }: BadgeProps) {
  const config = value ? KIND_CONFIG[value] : undefined;
  return (
    <span className={`${BADGE_BASE} ${config?.className ?? NEUTRAL}`}>
      {config?.label ?? value ?? "-"}
    </span>
  );
}

/** SmsStatusBadge renders a colored pill for the send status (대기/발송완료/실패). */
export function SmsStatusBadge({ value }: BadgeProps) {
  const config = value ? STATUS_CONFIG[value] : undefined;
  return (
    <span className={`${BADGE_BASE} ${config?.className ?? NEUTRAL}`}>
      {config?.label ?? value ?? "-"}
    </span>
  );
}

/** SmsChannelBadge renders a pill distinguishing short SMS from long LMS messages. */
export function SmsChannelBadge({ value }: BadgeProps) {
  const config = value ? CHANNEL_CONFIG[value] : undefined;
  return (
    <span className={`${BADGE_BASE} ${config?.className ?? NEUTRAL}`}>
      {config?.label ?? value ?? "-"}
    </span>
  );
}

/**
 * SmsTestModeBadge flags rows sent in test/demo mode. The clone never sends a
 * real message — every send is logged only — so this badge appears on every row
 * whose testMode flag is truthy ("Y").
 */
export function SmsTestModeBadge({ value }: BadgeProps) {
  if (value !== "Y") {
    return null;
  }
  return (
    <span className={`${BADGE_BASE} bg-yellow-100 text-yellow-800`}>
      데모/테스트 발송
    </span>
  );
}
