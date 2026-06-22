"use client";

import { useMemo, useState } from "react";
import { Loader2, Mail, MessageSquare, Send, Smartphone } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useSession } from "next-auth/react";

import {
  notificationList,
  notificationSummary,
} from "@/apis/query/notification/notification";
import {
  NotificationLogDtoChannel,
  NotificationLogDtoStatus,
  NotificationSearchRequestChannel,
  NotificationSearchRequestStatus,
  type NotificationLogDto,
  type NotificationLogDtoChannel as ChannelType,
  type NotificationLogDtoStatus as StatusType,
  type NotificationSearchRequest,
  type NotificationSummaryDto,
} from "@/apis/query/streamHubAdminAPI.schemas";
import NotificationSendModal from "@/components/notification/NotificationSendModal";
import { canWrite } from "@/lib/auth-utils";
import { SUCCESS_CODE } from "@/types/api";

const CHANNEL_META: Record<
  ChannelType,
  { label: string; className: string; icon: typeof Smartphone }
> = {
  SMS: {
    label: "SMS",
    className: "bg-sky-100 text-sky-700",
    icon: MessageSquare,
  },
  PUSH: {
    label: "PUSH",
    className: "bg-violet-100 text-violet-700",
    icon: Smartphone,
  },
  EMAIL: {
    label: "EMAIL",
    className: "bg-amber-100 text-amber-700",
    icon: Mail,
  },
};

const STATUS_META: Record<StatusType, { label: string; className: string }> = {
  SUCCESS: { label: "성공", className: "bg-emerald-100 text-emerald-700" },
  FAIL: { label: "실패", className: "bg-red-100 text-red-700" },
  PENDING: { label: "대기", className: "bg-amber-100 text-amber-700" },
};

const formatDateTime = (value?: string): string => {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

function SummaryCard({
  label,
  value,
  accent,
  onClick,
  active,
}: {
  label: string;
  value: number;
  accent: string;
  onClick?: () => void;
  active?: boolean;
}) {
  const body = (
    <>
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-semibold ${accent}`}>
        {value.toLocaleString()}
      </p>
    </>
  );
  if (!onClick) {
    return (
      <div className="rounded-md border border-slate-200 bg-white p-4">{body}</div>
    );
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-md border bg-white p-4 text-left transition hover:border-brand hover:shadow-sm focus:outline-none focus:ring-1 focus:ring-brand ${
        active ? "border-brand ring-1 ring-brand" : "border-slate-200"
      }`}
    >
      {body}
    </button>
  );
}

export default function NotificationsPage() {
  const [channel, setChannel] = useState<"" | ChannelType>("");
  const [status, setStatus] = useState<"" | StatusType>("");
  const [keyword, setKeyword] = useState("");
  const [sendOpen, setSendOpen] = useState(false);

  const { data: session } = useSession();
  const writable = canWrite(session?.user?.role);

  const searchBody: NotificationSearchRequest = useMemo(
    () => ({
      ...(channel
        ? { channel: channel as NotificationSearchRequestChannel }
        : {}),
      ...(status ? { status: status as NotificationSearchRequestStatus } : {}),
      ...(keyword.trim() ? { keyword: keyword.trim() } : {}),
    }),
    [channel, status, keyword],
  );

  const listQuery = useQuery({
    queryKey: ["notification-list", searchBody],
    queryFn: ({ signal }) => notificationList(searchBody, signal),
  });

  const summaryQuery = useQuery({
    queryKey: ["notification-summary"],
    queryFn: ({ signal }) => notificationSummary(signal),
  });

  const summary: NotificationSummaryDto =
    summaryQuery.data?.resultObject ?? {};
  const byChannel = summary.byChannel ?? {};

  const logs: NotificationLogDto[] = listQuery.data?.resultObject ?? [];
  const listOk =
    !listQuery.data || listQuery.data.resultCode === SUCCESS_CODE;

  // After a send, refetch both the log list and the summary so the new row and
  // updated counts appear immediately.
  const handleSent = () => {
    listQuery.refetch();
    summaryQuery.refetch();
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-semibold text-slate-900">
              알림센터
            </h1>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-medium text-slate-500">
              실발송 없음 · 데모 로그
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            SMS / PUSH / EMAIL 발송 로그를 채널·상태·키워드로 조회합니다.
          </p>
        </div>
        {writable && (
          <button
            type="button"
            onClick={() => setSendOpen(true)}
            className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
          >
            <Send className="h-4 w-4" />
            알림 발송
          </button>
        )}
      </div>

      {/* Summary cards */}
      <div className="mb-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <SummaryCard
          label="총 발송"
          value={summary.total ?? 0}
          accent="text-slate-900"
          onClick={() => setStatus("")}
          active={status === ""}
        />
        <SummaryCard
          label="성공"
          value={summary.successCount ?? 0}
          accent="text-emerald-600"
          onClick={() => setStatus(NotificationLogDtoStatus.SUCCESS)}
          active={status === NotificationLogDtoStatus.SUCCESS}
        />
        <SummaryCard
          label="실패"
          value={summary.failCount ?? 0}
          accent="text-red-600"
          onClick={() => setStatus(NotificationLogDtoStatus.FAIL)}
          active={status === NotificationLogDtoStatus.FAIL}
        />
        <SummaryCard
          label="대기"
          value={summary.pendingCount ?? 0}
          accent="text-amber-600"
          onClick={() => setStatus(NotificationLogDtoStatus.PENDING)}
          active={status === NotificationLogDtoStatus.PENDING}
        />
      </div>

      {/* By-channel breakdown */}
      <div className="mb-4 flex flex-wrap gap-2">
        {(Object.keys(CHANNEL_META) as ChannelType[]).map((key) => {
          const meta = CHANNEL_META[key];
          const Icon = meta.icon;
          const selected = channel === key;
          return (
            <button
              key={key}
              type="button"
              onClick={() => setChannel(selected ? "" : key)}
              aria-pressed={selected}
              className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition hover:opacity-80 ${meta.className} ${
                selected ? "ring-2 ring-brand ring-offset-1" : ""
              }`}
            >
              <Icon className="h-3.5 w-3.5" />
              {meta.label}
              <span className="font-semibold">
                {(byChannel[key] ?? 0).toLocaleString()}
              </span>
            </button>
          );
        })}
      </div>

      {/* Filter bar */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="notif-channel"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            채널
          </label>
          <select
            id="notif-channel"
            value={channel}
            onChange={(event) =>
              setChannel(event.target.value as "" | ChannelType)
            }
            className="w-40 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            <option value="">전체</option>
            {(
              Object.values(NotificationLogDtoChannel) as ChannelType[]
            ).map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="notif-status"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            상태
          </label>
          <select
            id="notif-status"
            value={status}
            onChange={(event) =>
              setStatus(event.target.value as "" | StatusType)
            }
            className="w-40 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          >
            <option value="">전체</option>
            {(
              Object.values(NotificationLogDtoStatus) as StatusType[]
            ).map((value) => (
              <option key={value} value={value}>
                {STATUS_META[value].label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col">
          <label
            htmlFor="notif-keyword"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            검색어
          </label>
          <input
            id="notif-keyword"
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="제목 / 수신자"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>
      </div>

      {/* Count */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {logs.length.toLocaleString()}건
        </span>
      </div>

      {/* Table */}
      {listQuery.isLoading ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError || !listOk ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">
            발송 로그를 불러오지 못했습니다.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">채널</th>
                <th className="px-4 py-3">수신자</th>
                <th className="px-4 py-3">제목</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">발송시각</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {logs.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 발송 로그가 없습니다.
                  </td>
                </tr>
              ) : (
                logs.map((log) => {
                  const channelMeta = log.channel
                    ? CHANNEL_META[log.channel]
                    : null;
                  const statusMeta = log.status
                    ? STATUS_META[log.status]
                    : null;
                  return (
                    <tr key={log.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        {channelMeta ? (
                          <span
                            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${channelMeta.className}`}
                          >
                            {channelMeta.label}
                          </span>
                        ) : (
                          "-"
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {log.targetMasked ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-slate-900">
                        <div className="font-medium">{log.title ?? "-"}</div>
                        {log.status === "FAIL" && log.failReason && (
                          <div className="mt-0.5 text-xs text-red-500">
                            {log.failReason}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {statusMeta ? (
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusMeta.className}`}
                          >
                            {statusMeta.label}
                          </span>
                        ) : (
                          "-"
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-500">
                        {formatDateTime(log.sentAt)}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {sendOpen && (
        <NotificationSendModal
          onClose={() => setSendOpen(false)}
          onSuccess={handleSent}
        />
      )}
    </div>
  );
}
