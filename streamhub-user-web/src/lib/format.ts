/** Seconds → "12:34" or "1:02:03". Returns "" for null/0. */
export function formatDuration(sec: number | null | undefined): string {
  if (!sec || sec <= 0) return "";
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = Math.floor(sec % 60);
  const mm = h > 0 ? String(m).padStart(2, "0") : String(m);
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

/** Compact view count, e.g. 1234 → "1.2천", 23000 → "2.3만". */
export function formatViews(n: number | null | undefined): string {
  const v = n ?? 0;
  if (v >= 10000) return `${(v / 10000).toFixed(1).replace(/\.0$/, "")}만`;
  if (v >= 1000) return `${(v / 1000).toFixed(1).replace(/\.0$/, "")}천`;
  return String(v);
}

/** ISO datetime → "2026.06.16". */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}.${m}.${day}`;
}

/** Comma-joined hashtag string → trimmed array (list items carry a string). */
export function splitHashtags(s: string | null | undefined): string[] {
  if (!s) return [];
  return s
    .split(",")
    .map((t) => t.trim())
    .filter(Boolean);
}
