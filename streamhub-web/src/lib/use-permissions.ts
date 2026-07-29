"use client";

import { useMemo } from "react";
import { useSession } from "next-auth/react";
import { canWrite, type AdminRole } from "@/lib/auth-utils";

/** Tooltip shown on every write control a read-only operator cannot use. */
export const READ_ONLY_HINT = "읽기 전용 계정입니다. 조회만 가능합니다.";

/**
 * Whether the signed-in operator may perform write actions, plus the props to spread onto a write
 * control so it explains itself when they may not.
 *
 * <p>The backend already refuses these calls — every mutation endpoint is gated by a
 * {@code resource:write} authority and answers 403 — so this is a UX contract, not a security
 * boundary. It exists because the README publishes a read-only `viewer` account for browsing the
 * demo, and until now most screens rendered their 저장/삭제/승인 buttons to that account exactly as
 * they would to a real operator. The button looked available, the click failed, and the screen
 * blamed the user. `auth-utils.ts` even documented that the frontend hides such actions; only 7 of
 * 28 write-capable screens actually did.
 *
 * <p>Disabled rather than hidden on purpose: the demo account exists so someone can see what the
 * console does. Hiding the controls would make working features look missing, so they stay visible,
 * inert, and labelled with the reason.
 */
export function useWritePermission(): {
  writable: boolean;
  /** Spread onto a `<button>`: disables it and explains why, for read-only operators. */
  writeGuardProps: { disabled?: true; title?: string };
} {
  const { data: session } = useSession();
  const writable = canWrite(session?.user?.role as AdminRole | undefined);
  // Memoized so the object identity only changes when the permission does. Callers put these props
  // in dependency arrays — AG Grid column definitions close over them — and a fresh object every
  // render would silently defeat the memo it was added to.
  const writeGuardProps = useMemo(
    () => (writable ? {} : { disabled: true as const, title: READ_ONLY_HINT }),
    [writable],
  );
  return { writable, writeGuardProps };
}
