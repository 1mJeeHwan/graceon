"use client";

import { useState } from "react";
import { ChevronDown, ChevronUp, GripVertical } from "lucide-react";

import { type HomeSection } from "@/apis/query/streamHubAdminAPI.schemas";

interface HomeSectionsFieldProps {
  sections: HomeSection[];
  onChange: (next: HomeSection[]) => void;
}

const SECTION_LABELS: Record<string, string> = {
  worshipLive: "예배 실황",
  latestVideos: "최신 영상",
  ccmAlbums: "CCM 음반",
  nearbyChurch: "내 주변 교회",
};

const labelFor = (key: string | undefined): string => {
  if (!key) {
    return "(알 수 없는 섹션)";
  }
  return SECTION_LABELS[key] ?? key;
};

const move = (
  sections: HomeSection[],
  from: number,
  to: number,
): HomeSection[] => {
  if (to < 0 || to >= sections.length || from === to) {
    return sections;
  }
  const next = [...sections];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
};

export default function HomeSectionsField({
  sections,
  onChange,
}: HomeSectionsFieldProps) {
  const [dragIndex, setDragIndex] = useState<number | null>(null);

  const toggleEnabled = (index: number) => {
    const next = sections.map((section, idx) =>
      idx === index ? { ...section, enabled: !section.enabled } : section,
    );
    onChange(next);
  };

  const handleDrop = (targetIndex: number) => {
    if (dragIndex === null) {
      return;
    }
    onChange(move(sections, dragIndex, targetIndex));
    setDragIndex(null);
  };

  if (sections.length === 0) {
    return (
      <p className="rounded-md border border-dashed border-slate-300 px-3 py-6 text-center text-sm text-slate-400">
        구성된 홈 섹션이 없습니다.
      </p>
    );
  }

  return (
    <ul className="space-y-2">
      {sections.map((section, index) => (
        <li
          key={section.key ?? `section-${index}`}
          draggable
          onDragStart={() => setDragIndex(index)}
          onDragOver={(event) => event.preventDefault()}
          onDrop={() => handleDrop(index)}
          onDragEnd={() => setDragIndex(null)}
          className={`flex items-center gap-3 rounded-md border bg-white px-3 py-2.5 transition ${
            dragIndex === index
              ? "border-brand opacity-60"
              : "border-slate-200 hover:border-slate-300"
          }`}
        >
          <span
            className="cursor-grab text-slate-400 active:cursor-grabbing"
            aria-hidden
          >
            <GripVertical className="h-4 w-4" />
          </span>

          <div className="flex flex-col">
            <span className="text-sm font-medium text-slate-900">
              {labelFor(section.key)}
            </span>
            <span className="font-mono text-xs text-slate-400">
              {section.key ?? "-"}
            </span>
          </div>

          <div className="ml-auto flex items-center gap-1">
            <button
              type="button"
              onClick={() => onChange(move(sections, index, index - 1))}
              disabled={index === 0}
              aria-label="위로 이동"
              className="rounded p-1 text-slate-500 transition hover:bg-slate-100 disabled:opacity-30"
            >
              <ChevronUp className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => onChange(move(sections, index, index + 1))}
              disabled={index === sections.length - 1}
              aria-label="아래로 이동"
              className="rounded p-1 text-slate-500 transition hover:bg-slate-100 disabled:opacity-30"
            >
              <ChevronDown className="h-4 w-4" />
            </button>

            <label className="ml-2 inline-flex cursor-pointer items-center gap-2 text-xs text-slate-600">
              <input
                type="checkbox"
                checked={section.enabled ?? false}
                onChange={() => toggleEnabled(index)}
                className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
              />
              노출
            </label>
          </div>
        </li>
      ))}
    </ul>
  );
}
