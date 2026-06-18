"use client";

interface AccentColorFieldProps {
  value: string;
  onChange: (hex: string) => void;
}

const PRESETS: { label: string; hex: string }[] = [
  { label: "cyan", hex: "#40C1DF" },
  { label: "violet", hex: "#5B30FF" },
  { label: "rose", hex: "#FF1B58" },
  { label: "green", hex: "#16A34A" },
  { label: "amber", hex: "#F59E0B" },
];

const HEX_PATTERN = /^#[0-9a-fA-F]{6}$/;

export default function AccentColorField({
  value,
  onChange,
}: AccentColorFieldProps) {
  const safeColor = HEX_PATTERN.test(value) ? value : "#40C1DF";

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <input
          type="color"
          aria-label="액센트 색 선택기"
          value={safeColor}
          onChange={(event) => onChange(event.target.value.toUpperCase())}
          className="h-10 w-12 cursor-pointer rounded-md border border-slate-300 bg-white p-1"
        />
        <input
          type="text"
          aria-label="액센트 색 HEX 값"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="#40C1DF"
          className="w-32 rounded-md border border-slate-300 px-3 py-2 font-mono text-sm uppercase outline-none focus:border-brand focus:ring-1 focus:ring-brand"
        />
        <span
          className="inline-block h-9 w-16 rounded-md border border-slate-200"
          style={{ backgroundColor: safeColor }}
          aria-hidden
        />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {PRESETS.map((preset) => {
          const selected = value.toUpperCase() === preset.hex.toUpperCase();
          return (
            <button
              key={preset.hex}
              type="button"
              onClick={() => onChange(preset.hex)}
              aria-label={`${preset.label} (${preset.hex})`}
              title={`${preset.label} ${preset.hex}`}
              className={`h-8 w-8 rounded-full border-2 transition ${
                selected
                  ? "border-slate-900 ring-2 ring-slate-300"
                  : "border-white shadow-sm hover:scale-110"
              }`}
              style={{ backgroundColor: preset.hex }}
            />
          );
        })}
      </div>

      {!HEX_PATTERN.test(value) && (
        <p className="text-xs text-red-600">
          올바른 HEX 색상(#RRGGBB) 형식이 아닙니다.
        </p>
      )}
    </div>
  );
}
