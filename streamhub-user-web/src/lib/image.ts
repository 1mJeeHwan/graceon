// Demo thumbnails were seeded with picsum.photos URLs, but that host is currently unreachable,
// so those album covers / goods / campaign banners render broken. Until the seed data is
// re-pointed, normalize any picsum URL at the API boundary to a deterministic, license-free
// Unsplash photo (Unsplash License — free, no attribution required) themed to the surface
// encoded in the seed slug (e.g. `…/seed/album24/…` → a music photo). Real uploads, working
// content thumbnails, and nulls pass through untouched.

const Q = "?w=640&q=80&auto=format&fit=crop";
const u = (id: string) => `https://images.unsplash.com/photo-${id}${Q}`;

// All ids verified to resolve (HTTP 200, image/jpeg).
const POOLS: Record<string, string[]> = {
  // album → music: piano / keys / sheet music / guitar / vocal mic
  album: [
    "1520523839897-bd0b52f945a0",
    "1511671782779-c97d3d27a1d4",
    "1465847899084-d164df4dedc6",
    "1510915361894-db8b60106cb1",
    "1493225457124-a3eb161ffa5f",
  ].map(u),
  // goods → merch: books / mug / apparel / stationery
  goods: [
    "1544947950-fa07a98d237f",
    "1514228742587-6b1558fcca3d",
    "1556905055-8f358a7a47b2",
    "1517842645767-c639042777db",
  ].map(u),
  // campaign / banner → events: crowd / hands raised / community / praise stage
  campaign: [
    "1492684223066-81342ee5ff30",
    "1501281668745-f7f57925c3b4",
    "1526976668912-1a811878dd37",
    "1507692049790-de58290a4334",
  ].map(u),
  banner: [
    "1492684223066-81342ee5ff30",
    "1438232992991-995b7058bbb3",
    "1507692049790-de58290a4334",
    "1526976668912-1a811878dd37",
  ].map(u),
};

// Fallback when the seed slug has no themed pool: church/worship scenes.
const DEFAULT_POOL = [
  "1438032005730-c779502df39b",
  "1473177104440-ffee2f376098",
  "1519491050282-cf00c82424b4",
  "1510590337019-5ef8d3d32116",
  "1529070538774-1843cb3265df",
].map(u);

/**
 * Returns a usable image URL. picsum.photos URLs (currently dead) are swapped for a deterministic
 * themed Unsplash photo; everything else (real images, working thumbnails, null) is returned as-is.
 */
export function fixImageUrl(url: string | null): string | null {
  if (!url || !url.includes("picsum.photos")) {
    return url;
  }
  const match = url.match(/\/seed\/([a-z]+)(\d+)/i);
  const category = (match?.[1] ?? "").toLowerCase();
  const index = match ? parseInt(match[2], 10) : 0;
  const pool = POOLS[category] ?? DEFAULT_POOL;
  return pool[Math.abs(index) % pool.length];
}
