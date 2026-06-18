"use client";

import { useMemo, useState } from "react";
import { Loader2 } from "lucide-react";

import { type AlbumListItem } from "@/apis/query/streamHubAdminAPI.schemas";

interface FeaturedAlbumsFieldProps {
  albums: AlbumListItem[];
  isLoading: boolean;
  isError: boolean;
  selectedIds: number[];
  onChange: (next: number[]) => void;
}

export default function FeaturedAlbumsField({
  albums,
  isLoading,
  isError,
  selectedIds,
  onChange,
}: FeaturedAlbumsFieldProps) {
  const [keyword, setKeyword] = useState("");

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const filtered = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) {
      return albums;
    }
    return albums.filter(
      (album) =>
        (album.title ?? "").toLowerCase().includes(term) ||
        (album.artist ?? "").toLowerCase().includes(term),
    );
  }, [albums, keyword]);

  const toggle = (id: number) => {
    if (selectedSet.has(id)) {
      onChange(selectedIds.filter((value) => value !== id));
    } else {
      onChange([...selectedIds, id]);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <input
          type="text"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="음반명 / 아티스트 검색"
          className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
        />
        <span className="text-sm text-slate-600">
          선택 {selectedIds.length}건
        </span>
      </div>

      {isLoading ? (
        <div className="flex h-32 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : isError ? (
        <div className="flex h-32 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">음반 목록을 불러오지 못했습니다.</p>
        </div>
      ) : (
        <div className="max-h-64 overflow-y-auto rounded-md border border-slate-200 bg-white">
          {filtered.length === 0 ? (
            <p className="px-3 py-8 text-center text-sm text-slate-400">
              조회된 음반이 없습니다.
            </p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {filtered.map((album) => {
                if (album.id == null) {
                  return null;
                }
                const checked = selectedSet.has(album.id);
                return (
                  <li key={album.id}>
                    <label className="flex cursor-pointer items-center gap-3 px-3 py-2.5 hover:bg-slate-50">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggle(album.id as number)}
                        className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
                      />
                      <span className="text-sm font-medium text-slate-900">
                        {album.title ?? "-"}
                      </span>
                      <span className="text-xs text-slate-500">
                        {album.artist ?? "-"}
                      </span>
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
