import { Suspense } from "react";
import { MediaListView } from "@/components/MediaListView";

export default function VideoListPage() {
  return (
    <Suspense fallback={<div className="px-5 pt-4 text-sm text-inactive">불러오는 중…</div>}>
      <MediaListView type="VIDEO" title="영상" searchPlaceholder="영상 검색" />
    </Suspense>
  );
}
