import { Suspense } from "react";
import { Disc3 } from "lucide-react";
import { AlbumListView } from "@/components/AlbumListView";
import { CardSkeletonGrid } from "@/components/States";

/** Skeleton matching the loaded album list (header + search + grid) to avoid a layout jump. */
function AlbumsFallback() {
  return (
    <section className="pt-4" aria-busy="true">
      <div className="flex items-center gap-2 px-5">
        <Disc3 className="h-6 w-6 text-primary" />
        <h1 className="text-2xl font-bold tracking-tight">음반</h1>
      </div>
      <p className="mt-1 px-5 text-sm text-inactive">찬양 음반을 둘러보고 30초 미리듣기로 들어보세요.</p>
      <div className="px-5 pb-2 pt-4">
        <div className="skeleton h-[42px] w-full rounded-xl" />
      </div>
      <div className="px-5 pt-3">
        <CardSkeletonGrid square count={12} />
      </div>
    </section>
  );
}

export default function AlbumsPage() {
  return (
    <Suspense fallback={<AlbumsFallback />}>
      <AlbumListView />
    </Suspense>
  );
}
