"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Search, X } from "lucide-react";

import { useMemberList } from "@/apis/query/member/member";
import type {
  MemberListItem,
  MemberSearchRequest,
} from "@/apis/query/graceOnAdminAPI.schemas";
import {
  notificationSend,
  type NotificationSendChannel,
  type NotificationSendRequest,
} from "@/apis/custom-notification";
import { FIELD_CLASS } from "@/lib/goods-form";
import { SUCCESS_CODE } from "@/types/api";

/** Channels offered in the send form (PUSH is the default). */
const CHANNEL_OPTIONS: { value: NotificationSendChannel; label: string }[] = [
  { value: "PUSH", label: "PUSH" },
  { value: "SMS", label: "SMS" },
  { value: "EMAIL", label: "EMAIL" },
];

/** Page size for the in-modal member search. */
const MEMBER_SEARCH_SIZE = 20;

// The form's structural validity (title/content/channel/scope) is handled by
// Zod; the "at least one member when TARGETED" rule lives in the submit handler
// because it depends on selection state held outside the form.
const notificationSendSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, "제목을 입력하세요.")
    .max(100, "제목은 100자 이내로 입력하세요."),
  content: z
    .string()
    .trim()
    .min(1, "내용을 입력하세요.")
    .max(2000, "내용은 2000자 이내로 입력하세요."),
  channel: z.enum(["SMS", "PUSH", "EMAIL"]),
  scope: z.enum(["BROADCAST", "TARGETED"]),
});

type NotificationSendFormValues = z.infer<typeof notificationSendSchema>;

interface NotificationSendModalProps {
  onClose: () => void;
  /** Called after a successful send so the parent can invalidate/refetch logs. */
  onSuccess: () => void;
}

/**
 * NotificationSendModal posts a notification via notificationSend. The operator
 * picks a channel, writes a title/content, and chooses an audience: 전체 회원
 * (BROADCAST) or 특정 회원 (TARGETED) — the latter reveals an in-modal member
 * search with a checkbox list. The clone never delivers a real message; the row
 * is logged in test mode only.
 */
export default function NotificationSendModal({
  onClose,
  onSuccess,
}: NotificationSendModalProps) {
  const {
    register,
    handleSubmit,
    watch,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<NotificationSendFormValues>({
    resolver: zodResolver(notificationSendSchema),
    defaultValues: {
      title: "",
      content: "",
      channel: "PUSH",
      scope: "BROADCAST",
    },
  });

  const scope = watch("scope");
  const contentLength = watch("content")?.length ?? 0;

  // Member search + selection state (lives outside RHF so the checkbox list can
  // accumulate picks across multiple searches).
  const [keyword, setKeyword] = useState("");
  const [searchedKeyword, setSearchedKeyword] = useState<string | null>(null);
  const [results, setResults] = useState<MemberListItem[]>([]);
  const [selected, setSelected] = useState<Map<number, MemberListItem>>(
    new Map(),
  );
  const [sending, setSending] = useState(false);

  const memberSearch = useMemberList();

  const handleMemberSearch = () => {
    const trimmed = keyword.trim();
    const request: MemberSearchRequest = {
      pageNumber: 0,
      pageSize: MEMBER_SEARCH_SIZE,
      keyword: trimmed || undefined,
    };
    memberSearch.mutate(
      { data: request },
      {
        onSuccess: (response) => {
          setSearchedKeyword(trimmed);
          if (response.resultCode === SUCCESS_CODE) {
            setResults(response.resultObject?.contents ?? []);
          } else {
            setResults([]);
          }
        },
        onError: () => {
          setSearchedKeyword(trimmed);
          setResults([]);
        },
      },
    );
  };

  const toggleMember = (member: MemberListItem) => {
    if (member.id == null) {
      return;
    }
    const memberId = member.id;
    setSelected((prev) => {
      const next = new Map(prev);
      if (next.has(memberId)) {
        next.delete(memberId);
      } else {
        next.set(memberId, member);
      }
      return next;
    });
    clearErrors("root");
  };

  const removeSelected = (memberId: number) => {
    setSelected((prev) => {
      const next = new Map(prev);
      next.delete(memberId);
      return next;
    });
  };

  const selectedList = Array.from(selected.values());

  const onSubmit = (values: NotificationSendFormValues) => {
    const memberIds =
      values.scope === "TARGETED" ? Array.from(selected.keys()) : [];

    if (values.scope === "TARGETED" && memberIds.length === 0) {
      setError("root", { message: "수신할 회원을 1명 이상 선택하세요." });
      return;
    }

    const payload: NotificationSendRequest = {
      channel: values.channel,
      title: values.title.trim(),
      content: values.content.trim(),
      scope: values.scope,
      memberIds,
    };

    setSending(true);
    clearErrors("root");
    notificationSend(payload)
      .then((response) => {
        if (response.resultCode === SUCCESS_CODE) {
          onSuccess();
          onClose();
        } else {
          setError("root", {
            message: response.resultMessage ?? "발송에 실패했습니다.",
          });
        }
      })
      .catch(() =>
        setError("root", { message: "발송 중 오류가 발생했습니다." }),
      )
      .finally(() => setSending(false));
  };

  const targetedWithNone =
    scope === "TARGETED" && selected.size === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="flex max-h-[90vh] w-full max-w-lg flex-col rounded-md bg-white shadow-xl">
        <div className="flex items-start justify-between border-b border-slate-200 px-5 py-3">
          <div>
            <h3 className="text-base font-semibold text-slate-900">알림 발송</h3>
            <p className="mt-1 inline-flex items-center rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800">
              데모/테스트 발송 · 실제로 전송되지 않고 내역만 기록됩니다
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            aria-label="닫기"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex min-h-0 flex-1 flex-col px-5 py-4"
          noValidate
        >
          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto">
            {errors.root && (
              <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                {errors.root.message}
              </p>
            )}

            {/* Channel */}
            <div>
              <label
                htmlFor="notif-channel"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                채널
              </label>
              <select
                id="notif-channel"
                className={FIELD_CLASS}
                {...register("channel")}
              >
                {CHANNEL_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Title */}
            <div>
              <label
                htmlFor="notif-title"
                className="mb-1 block text-xs font-medium text-slate-500"
              >
                제목 *
              </label>
              <input
                id="notif-title"
                type="text"
                placeholder="알림 제목을 입력하세요."
                className={FIELD_CLASS}
                {...register("title")}
              />
              {errors.title && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.title.message}
                </p>
              )}
            </div>

            {/* Content */}
            <div>
              <div className="mb-1 flex items-center justify-between">
                <label
                  htmlFor="notif-content"
                  className="block text-xs font-medium text-slate-500"
                >
                  내용 *
                </label>
                <span className="text-xs text-slate-400">
                  {contentLength} / 2000
                </span>
              </div>
              <textarea
                id="notif-content"
                rows={4}
                placeholder="발송할 내용을 입력하세요."
                className={FIELD_CLASS}
                {...register("content")}
              />
              {errors.content && (
                <p className="mt-1 text-xs text-red-600">
                  {errors.content.message}
                </p>
              )}
            </div>

            {/* Scope */}
            <div>
              <span className="mb-1 block text-xs font-medium text-slate-500">
                수신 대상
              </span>
              <div className="flex gap-4">
                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="radio"
                    value="BROADCAST"
                    className="text-brand focus:ring-brand"
                    {...register("scope")}
                  />
                  전체 회원
                </label>
                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="radio"
                    value="TARGETED"
                    className="text-brand focus:ring-brand"
                    {...register("scope")}
                  />
                  특정 회원
                </label>
              </div>
            </div>

            {/* Member picker (only when TARGETED) */}
            {scope === "TARGETED" && (
              <div className="rounded-md border border-slate-200 p-3">
                {/* Selected chips */}
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-600">
                    {selected.size}명 선택
                  </span>
                </div>
                {selectedList.length > 0 && (
                  <div className="mb-3 flex flex-wrap gap-1.5">
                    {selectedList.map((member) => (
                      <span
                        key={member.id}
                        className="inline-flex items-center gap-1 rounded-full bg-brand/10 px-2.5 py-0.5 text-xs font-medium text-brand"
                      >
                        {member.name ?? `#${member.id}`}
                        <button
                          type="button"
                          onClick={() => removeSelected(member.id as number)}
                          className="text-brand/70 hover:text-brand"
                          aria-label="선택 해제"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                )}

                {/* Search input */}
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter") {
                        event.preventDefault();
                        handleMemberSearch();
                      }
                    }}
                    placeholder="이름 / 이메일 / 전화로 검색"
                    className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                  />
                  <button
                    type="button"
                    onClick={handleMemberSearch}
                    disabled={memberSearch.isPending}
                    className="flex items-center gap-1 rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                  >
                    {memberSearch.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Search className="h-4 w-4" />
                    )}
                    검색
                  </button>
                </div>

                {/* Results list */}
                <div className="mt-2 max-h-48 overflow-y-auto rounded-md border border-slate-100">
                  {memberSearch.isPending ? (
                    <div className="flex h-24 items-center justify-center">
                      <Loader2 className="h-4 w-4 animate-spin text-slate-400" />
                    </div>
                  ) : searchedKeyword === null ? (
                    <p className="px-3 py-6 text-center text-xs text-slate-400">
                      검색어를 입력해 회원을 조회하세요.
                    </p>
                  ) : results.length === 0 ? (
                    <p className="px-3 py-6 text-center text-xs text-slate-400">
                      조회된 회원이 없습니다.
                    </p>
                  ) : (
                    <ul className="divide-y divide-slate-100">
                      {results.map((member) => {
                        const checked =
                          member.id != null && selected.has(member.id);
                        return (
                          <li key={member.id}>
                            <label className="flex cursor-pointer items-center gap-2 px-3 py-2 hover:bg-slate-50">
                              <input
                                type="checkbox"
                                checked={checked}
                                onChange={() => toggleMember(member)}
                                className="text-brand focus:ring-brand"
                              />
                              <span className="min-w-0 flex-1">
                                <span className="block truncate text-sm text-slate-800">
                                  {member.name ?? "(이름 없음)"}
                                </span>
                                <span className="block truncate text-xs text-slate-400">
                                  {member.email ?? "-"}
                                  {member.churchName
                                    ? ` · ${member.churchName}`
                                    : ""}
                                </span>
                              </span>
                            </label>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={sending}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={sending || targetedWithNone}
              className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {sending && <Loader2 className="h-4 w-4 animate-spin" />}
              발송
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
