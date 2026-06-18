"use client";

import {
  useFieldArray,
  type Control,
  type UseFormRegister,
} from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";

import { FIELD_CLASS, type AlbumFormValues } from "@/lib/album-form";

interface TrackRowsProps {
  control: Control<AlbumFormValues>;
  register: UseFormRegister<AlbumFormValues>;
}

/**
 * TrackRows renders the dynamic tracklist using useFieldArray. Each row carries
 * a track title, duration (seconds), and an optional preview url. The track
 * number is derived from row order at submit time.
 */
export default function TrackRows({ control, register }: TrackRowsProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name: "tracks",
  });

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <p className="text-xs font-medium text-slate-500">트랙리스트</p>
        <button
          type="button"
          onClick={() =>
            append({ title: "", durationSec: "", previewUrl: "" })
          }
          className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2.5 py-1 text-xs font-medium text-slate-700 transition hover:bg-slate-100"
        >
          <Plus className="h-3.5 w-3.5" />
          트랙 추가
        </button>
      </div>

      {fields.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-300 bg-slate-50 px-3 py-3 text-center text-xs text-slate-400">
          등록된 트랙이 없습니다.
        </p>
      ) : (
        <div className="space-y-2">
          {fields.map((field, index) => (
            <div
              key={field.id}
              className="grid grid-cols-12 items-start gap-2 rounded-md border border-slate-200 p-2"
            >
              <div className="col-span-1 flex justify-center pt-2 text-xs font-medium text-slate-500">
                {index + 1}
              </div>
              <div className="col-span-4">
                <input
                  type="text"
                  placeholder="트랙명"
                  className={FIELD_CLASS}
                  {...register(`tracks.${index}.title` as const)}
                />
              </div>
              <div className="col-span-2">
                <input
                  type="number"
                  min={0}
                  placeholder="길이(초)"
                  className={FIELD_CLASS}
                  {...register(`tracks.${index}.durationSec` as const)}
                />
              </div>
              <div className="col-span-4">
                <input
                  type="text"
                  placeholder="미리듣기 URL"
                  className={FIELD_CLASS}
                  {...register(`tracks.${index}.previewUrl` as const)}
                />
              </div>
              <div className="col-span-1 flex justify-center pt-1.5">
                <button
                  type="button"
                  onClick={() => remove(index)}
                  className="rounded p-1 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                  aria-label="트랙 삭제"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
