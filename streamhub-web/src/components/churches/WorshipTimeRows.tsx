"use client";

import { useFieldArray, type Control, type UseFormRegister } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";

import {
  FIELD_CLASS,
  WORSHIP_KIND_CODES,
  WORSHIP_KIND_LABELS,
  type ChurchFormValues,
} from "@/lib/church-form";

interface WorshipTimeRowsProps {
  control: Control<ChurchFormValues>;
  register: UseFormRegister<ChurchFormValues>;
}

/**
 * WorshipTimeRows renders the dynamic worship-schedule list using useFieldArray.
 * Each row carries a worship kind, day label, start time, place, and target.
 */
export default function WorshipTimeRows({
  control,
  register,
}: WorshipTimeRowsProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name: "worshipTimes",
  });

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <p className="text-xs font-medium text-slate-500">예배시간</p>
        <button
          type="button"
          onClick={() =>
            append({
              kind: "SUNDAY",
              dayLabel: "",
              startTime: "",
              place: "",
              target: "",
            })
          }
          className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2.5 py-1 text-xs font-medium text-slate-700 transition hover:bg-slate-100"
        >
          <Plus className="h-3.5 w-3.5" />
          예배 추가
        </button>
      </div>

      {fields.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-300 bg-slate-50 px-3 py-3 text-center text-xs text-slate-400">
          등록된 예배시간이 없습니다.
        </p>
      ) : (
        <div className="space-y-2">
          {fields.map((field, index) => (
            <div
              key={field.id}
              className="grid grid-cols-12 items-start gap-2 rounded-md border border-slate-200 p-2"
            >
              <div className="col-span-2">
                <select
                  className={FIELD_CLASS}
                  {...register(`worshipTimes.${index}.kind` as const)}
                >
                  {WORSHIP_KIND_CODES.map((code) => (
                    <option key={code} value={code}>
                      {WORSHIP_KIND_LABELS[code] ?? code}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-span-2">
                <input
                  type="text"
                  placeholder="요일 (예: 주일)"
                  className={FIELD_CLASS}
                  {...register(`worshipTimes.${index}.dayLabel` as const)}
                />
              </div>
              <div className="col-span-2">
                <input
                  type="time"
                  placeholder="시작"
                  className={FIELD_CLASS}
                  {...register(`worshipTimes.${index}.startTime` as const)}
                />
              </div>
              <div className="col-span-2">
                <input
                  type="text"
                  placeholder="장소 (예: 본당)"
                  className={FIELD_CLASS}
                  {...register(`worshipTimes.${index}.place` as const)}
                />
              </div>
              <div className="col-span-3">
                <input
                  type="text"
                  placeholder="대상 (예: 전 교인)"
                  className={FIELD_CLASS}
                  {...register(`worshipTimes.${index}.target` as const)}
                />
              </div>
              <div className="col-span-1 flex justify-center pt-1.5">
                <button
                  type="button"
                  onClick={() => remove(index)}
                  className="rounded p-1 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                  aria-label="예배시간 삭제"
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
