"use client";

import { useAnnouncement } from "@/lib/queries";
import { AnnouncementModal } from "./AnnouncementModal";

/**
 * Fetches the admin-managed modal-ad announcements and feeds them to the popup. Each active ad shows
 * once per browser (sequentially), re-showing only when the admin adds a new one.
 */
export function AnnouncementGate() {
  const { data } = useAnnouncement();
  return <AnnouncementModal ads={data ?? []} />;
}
