"use client";

import { useState } from "react";
import { Bell } from "lucide-react";
import { useMyNotifications } from "@/lib/me";
import { formatDate } from "@/lib/format";
import { SectionShell } from "./SectionShell";
import { Pagination } from "@/components/Pagination";

const PAGE_SIZE = 6;

/** The member's notifications (newest first, paged). */
export function NotificationsSection({ token }: { token: string }) {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useMyNotifications(token, page, PAGE_SIZE);
  const items = data?.contents ?? [];

  return (
    <SectionShell
      icon={Bell}
      title="알림"
      isLoading={isLoading}
      isError={isError}
      isEmpty={items.length === 0}
      errorMessage="알림을 불러오지 못했습니다."
      emptyIcon={Bell}
      emptyMessage="받은 알림이 없습니다."
    >
      <ul className="divide-y divide-border/60 overflow-hidden rounded-card border border-border/70 bg-surface">
        {items.map((n) => (
          <li key={n.id} className="px-4 py-3">
            <p className="text-sm font-bold text-active">{n.title}</p>
            {n.body && <p className="mt-1 whitespace-pre-line text-sm text-inactive">{n.body}</p>}
            <p className="mt-1 text-[11px] text-inactive">{formatDate(n.createdAt)}</p>
          </li>
        ))}
      </ul>
      <Pagination pageNumber={page} totalPage={data?.totalPage ?? 1} onChange={setPage} />
    </SectionShell>
  );
}
