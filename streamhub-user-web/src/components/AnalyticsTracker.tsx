"use client";

import { useEffect, useRef } from "react";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { classifyPath, sendEvent } from "@/lib/analytics";

/**
 * Invisible client tracker mounted once in the root layout. Records a SESSION_START, then one
 * view event per page (PAGE_VIEW, or CONTENT_VIEW for album detail) with its dwell time — sent
 * when the visitor leaves the page (route change) or the tab. Best-effort; never blocks the UI.
 */
export function AnalyticsTracker() {
  const pathname = usePathname();
  const { member } = useAuth();

  const memberId = useRef<number | null>(null);
  memberId.current = member?.id ?? null;

  const currentPath = useRef<string | null>(null);
  const enteredAt = useRef<number>(0);
  const referrer = useRef<string | null>(null);
  const flushed = useRef(false);

  // Sends the view+dwell for the page being left (once).
  const flush = () => {
    const path = currentPath.current;
    if (!path || flushed.current) return;
    flushed.current = true;
    const dwellMs = Math.max(0, Math.round(performance.now() - enteredAt.current));
    const c = classifyPath(path);
    sendEvent({
      type: c.type,
      contentType: c.contentType,
      targetId: c.targetId,
      path,
      dwellMs,
      referrer: referrer.current,
      memberId: memberId.current,
    });
  };

  // Route changes: finish the previous page, then start tracking the new one.
  useEffect(() => {
    if (!pathname) return;
    if (currentPath.current === null) {
      // first page of the session
      sendEvent({ type: "SESSION_START", path: pathname, memberId: memberId.current });
      referrer.current = typeof document !== "undefined" ? document.referrer || null : null;
    } else if (pathname !== currentPath.current) {
      flush();
      referrer.current = currentPath.current; // internal navigation source
    }
    currentPath.current = pathname;
    enteredAt.current = performance.now();
    flushed.current = false;
  }, [pathname]);

  // Final page: flush on tab close / bfcache hide.
  useEffect(() => {
    const onHide = () => flush();
    window.addEventListener("pagehide", onHide);
    return () => window.removeEventListener("pagehide", onHide);
  }, []);

  return null;
}
