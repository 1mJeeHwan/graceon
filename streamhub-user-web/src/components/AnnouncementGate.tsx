"use client";

import { useAnnouncement } from "@/lib/queries";
import { AnnouncementModal } from "./AnnouncementModal";

/**
 * Fetches the admin-managed announcement config and feeds it to the one-time modal. Replaces the
 * previously hard-coded layout constant, so editing the 안내창 no longer requires a code change.
 */
export function AnnouncementGate() {
  const { data } = useAnnouncement();
  return (
    <AnnouncementModal
      enabled={Boolean(data?.enabled && data?.text)}
      text={data?.text ?? ""}
      link={data?.linkUrl || undefined}
    />
  );
}
