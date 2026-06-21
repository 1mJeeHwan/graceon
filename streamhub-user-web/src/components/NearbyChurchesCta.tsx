import Link from "next/link";
import { ChevronRight, MapPin } from "lucide-react";

/**
 * Home-screen call-to-action for the church finder. Instead of rendering nearby churches inline
 * (and prompting for geolocation on the home screen), this is a simple button that navigates to
 * the dedicated /churches tab where the full finder lives.
 */
export function NearbyChurchesCta() {
  return (
    <section className="px-5 pt-6">
      <Link
        href="/churches"
        aria-label="내 주변 교회 찾기"
        className="flex items-center gap-3 rounded-card border border-border/70 bg-surface p-4 transition-colors active:bg-card"
      >
        <div className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-primary/15">
          <MapPin className="h-5 w-5 text-primary" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-bold text-active">내 주변 교회 찾기</p>
          <p className="mt-0.5 text-[11px] text-inactive">현위치 기준으로 가까운 교회를 지도에서 찾아보세요</p>
        </div>
        <ChevronRight className="h-5 w-5 shrink-0 text-inactive" />
      </Link>
    </section>
  );
}
