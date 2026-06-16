import { Suspense } from "react";
import { MediaListView } from "@/components/MediaListView";

export default function MusicListPage() {
  return (
    <Suspense fallback={<div className="px-5 pt-4 text-sm text-inactive">불러오는 중…</div>}>
      <MediaListView type="SOUND" title="음악" searchPlaceholder="음악 검색" />
    </Suspense>
  );
}
