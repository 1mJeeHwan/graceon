"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Megaphone, Save } from "lucide-react";

import { announcementGet, announcementSave } from "@/apis/announcement";
import { canWrite } from "@/lib/auth-utils";
import { SUCCESS_CODE } from "@/types/api";

const FIELD_CLASS =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand";

export default function AnnouncementPage() {
  const { data: session } = useSession();
  const writable = canWrite(session?.user?.role);

  const [enabled, setEnabled] = useState(false);
  const [text, setText] = useState("");
  const [linkUrl, setLinkUrl] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const configQuery = useQuery({
    queryKey: ["announcement-config"],
    queryFn: ({ signal }) => announcementGet(signal),
  });

  useEffect(() => {
    const config = configQuery.data?.resultObject;
    if (config) {
      setEnabled(config.enabled);
      setText(config.text ?? "");
      setLinkUrl(config.linkUrl ?? "");
    }
  }, [configQuery.data]);

  const handleSave = async () => {
    setMessage(null);
    setSaving(true);
    try {
      const res = await announcementSave({
        enabled,
        text: text.trim() || null,
        linkUrl: linkUrl.trim() || null,
      });
      setMessage(
        res.resultCode === SUCCESS_CODE
          ? "저장되었습니다. 사용자 사이트에 반영됩니다."
          : res.resultMessage ?? "저장에 실패했습니다.",
      );
    } catch {
      setMessage("저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl space-y-5">
      <header>
        <h1 className="text-xl font-bold text-slate-900">안내창 관리</h1>
        <p className="mt-1 text-sm text-slate-500">
          사용자 사이트 첫 방문 시 한 번 노출되는 안내 모달입니다. 문구를 바꾸면 다시 노출됩니다.
        </p>
      </header>

      {configQuery.isLoading ? (
        <div className="flex items-center justify-center py-20 text-slate-400">
          <Loader2 className="h-6 w-6 animate-spin" />
        </div>
      ) : (
        <div className="space-y-5 rounded-lg border border-slate-200 bg-white p-5">
          <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
            <input
              type="checkbox"
              checked={enabled}
              disabled={!writable}
              onChange={(event) => setEnabled(event.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
            />
            안내창 노출
          </label>

          <div className="space-y-1">
            <span className="text-sm font-medium text-slate-700">안내 문구</span>
            <textarea
              value={text}
              disabled={!writable}
              onChange={(event) => setText(event.target.value)}
              rows={3}
              maxLength={500}
              placeholder="예) 성탄 특별예배 안내 — 자세히 보기"
              className={FIELD_CLASS}
            />
          </div>

          <div className="space-y-1">
            <span className="text-sm font-medium text-slate-700">자세히 보기 링크 (선택)</span>
            <input
              value={linkUrl}
              disabled={!writable}
              onChange={(event) => setLinkUrl(event.target.value)}
              maxLength={500}
              placeholder="예) /churches 또는 https://..."
              className={FIELD_CLASS}
            />
          </div>

          {/* Live preview of the modal body */}
          <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold text-slate-400">
              <Megaphone className="h-3.5 w-3.5" /> 미리보기
            </p>
            {enabled && text.trim() ? (
              <div className="space-y-2">
                <p className="whitespace-pre-line text-sm text-slate-800">{text}</p>
                {linkUrl.trim() && (
                  <span className="inline-block rounded bg-brand px-2 py-1 text-xs font-semibold text-white">
                    자세히 보기
                  </span>
                )}
              </div>
            ) : (
              <p className="text-sm text-slate-400">노출되지 않습니다.</p>
            )}
          </div>

          {message && (
            <div className="rounded-md bg-brand/10 px-3 py-2 text-sm text-brand">{message}</div>
          )}

          {writable && (
            <button
              type="button"
              onClick={() => void handleSave()}
              disabled={saving}
              className="inline-flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
            >
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              저장
            </button>
          )}
        </div>
      )}
    </div>
  );
}
