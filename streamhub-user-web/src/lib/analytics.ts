// Lightweight web-analytics client. Posts page-view / content-view / session events to the
// backend ingest endpoint (POST /pub/v1/events) so the admin "콘텐츠 활동 분석" dashboard can
// surface popular vs underperforming content. Best-effort: must NEVER break the app.

const BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
const SID_KEY = "streamhub.sid";

export type EventType = "PAGE_VIEW" | "CONTENT_VIEW" | "SESSION_START";
export type ContentKind = "VIDEO" | "ALBUM" | "POST" | "PAGE";
export type DeviceKind = "PC" | "MOBILE" | "TABLET";

export interface TrackPayload {
  type: EventType;
  contentType?: ContentKind;
  targetId?: number | null;
  title?: string | null;
  path: string;
  referrer?: string | null;
  dwellMs?: number | null;
  memberId?: number | null;
}

/** Per-tab session id (sessionStorage). New tab = new session, like a typical web-analytics SDK. */
export function getSessionId(): string {
  if (typeof window === "undefined") return "";
  try {
    let sid = window.sessionStorage.getItem(SID_KEY);
    if (!sid) {
      sid = (crypto.randomUUID?.() ?? `${Date.now()}-${Math.round(Math.random() * 1e9)}`).slice(0, 60);
      window.sessionStorage.setItem(SID_KEY, sid);
    }
    return sid;
  } catch {
    return "anon";
  }
}

/** Coarse device class from viewport width (no UA sniffing needed for the demo). */
export function deviceType(): DeviceKind {
  if (typeof window === "undefined") return "PC";
  const w = window.innerWidth;
  if (w < 768) return "MOBILE";
  if (w < 1024) return "TABLET";
  return "PC";
}

/** Fire-and-forget event send (fetch keepalive so it survives navigation/unload). */
export function sendEvent(p: TrackPayload): void {
  if (typeof window === "undefined") return;
  try {
    const body = JSON.stringify({
      type: p.type,
      contentType: p.contentType ?? "PAGE",
      targetId: p.targetId ?? null,
      title: p.title ?? null,
      path: p.path,
      sessionId: getSessionId(),
      memberId: p.memberId ?? null,
      deviceType: deviceType(),
      referrer: p.referrer ?? null,
      dwellMs: p.dwellMs ?? null,
    });
    fetch(`${BASE}/pub/v1/events`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body,
      keepalive: true,
    }).catch(() => {
      /* analytics must never surface errors to the user */
    });
  } catch {
    /* ignore */
  }
}

/** Maps a route to its content classification (only album detail is a tracked content for now). */
export function classifyPath(path: string): {
  type: EventType;
  contentType: ContentKind;
  targetId: number | null;
} {
  const album = /^\/albums\/(\d+)/.exec(path);
  if (album) return { type: "CONTENT_VIEW", contentType: "ALBUM", targetId: Number(album[1]) };
  return { type: "PAGE_VIEW", contentType: "PAGE", targetId: null };
}
