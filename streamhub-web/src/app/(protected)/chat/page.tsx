"use client";

import { useState } from "react";
import { Loader2, MessageSquare, Send, Sparkles } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  chatAdminSessionsCreate,
  chatAdminSessionsMessages,
  useChatAdminSessionsReplyCreate,
} from "@/apis/query/chat-admin/chat-admin";
import {
  type ChatMessageRow,
  ChatMessageRowRole,
  type ChatSessionRow,
} from "@/apis/query/graceOnAdminAPI.schemas";
import { SUCCESS_CODE } from "@/types/api";

const INTENT_LABELS: Record<string, string> = {
  PRODUCT_INQUIRY: "상품문의",
  ORDER_LOOKUP: "주문조회",
  FAQ: "FAQ",
  FALLBACK: "기타",
};

function intentLabel(intent?: string): string {
  if (!intent) {
    return "미분류";
  }
  return INTENT_LABELS[intent] ?? intent;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ChatPage() {
  const [selectedSessionKey, setSelectedSessionKey] = useState<string | null>(
    null,
  );
  const [draft, setDraft] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const sessionsQuery = useQuery({
    queryKey: ["chat-admin-sessions"],
    queryFn: ({ signal }) => chatAdminSessionsCreate(signal),
  });

  const messagesQuery = useQuery({
    queryKey: ["chat-admin-messages", selectedSessionKey],
    queryFn: ({ signal }) =>
      chatAdminSessionsMessages(selectedSessionKey as string, signal),
    enabled: selectedSessionKey != null,
  });

  const replyMutation = useChatAdminSessionsReplyCreate();

  const sessions: ChatSessionRow[] = sessionsQuery.data?.resultObject ?? [];
  const messages: ChatMessageRow[] = messagesQuery.data?.resultObject ?? [];

  const handleSelect = (sessionKey?: string) => {
    if (!sessionKey) {
      return;
    }
    setSelectedSessionKey(sessionKey);
    setDraft("");
    setMessage(null);
  };

  const handleSend = () => {
    const content = draft.trim();
    if (!selectedSessionKey || !content) {
      return;
    }
    setMessage(null);
    replyMutation.mutate(
      { sessionKey: selectedSessionKey, data: { content } },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setDraft("");
            messagesQuery.refetch();
            sessionsQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "전송에 실패했습니다.");
          }
        },
        onError: () => setMessage("전송 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-900">
            챗봇 상담
            <span className="inline-flex items-center gap-1 rounded-full bg-violet-100 px-2.5 py-0.5 text-xs font-medium text-violet-700">
              <Sparkles className="h-3 w-3" />
              AI 데모 · 룰베이스
            </span>
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            상담 세션을 선택해 대화 이력을 확인하고 직접 답변할 수 있습니다.
          </p>
        </div>
      </div>

      <div className="flex h-[calc(100vh-220px)] min-h-[480px] overflow-hidden rounded-md border border-slate-200 bg-white">
        {/* Left: session list */}
        <div className="flex w-80 flex-col border-r border-slate-200">
          <div className="border-b border-slate-200 px-4 py-3 text-xs font-medium text-slate-500">
            상담 세션 · {sessions.length.toLocaleString()}건
          </div>
          <div className="flex-1 overflow-y-auto">
            {sessionsQuery.isLoading ? (
              <div className="flex h-full items-center justify-center">
                <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
              </div>
            ) : sessionsQuery.isError ? (
              <div className="flex h-full items-center justify-center px-4 text-center">
                <p className="text-sm text-red-600">
                  세션 목록을 불러오지 못했습니다.
                </p>
              </div>
            ) : sessions.length === 0 ? (
              <div className="flex h-full items-center justify-center px-4 text-center">
                <p className="text-sm text-slate-400">상담 세션이 없습니다.</p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {sessions.map((session) => {
                  const isActive =
                    session.sessionKey === selectedSessionKey;
                  return (
                    <li key={session.sessionKey}>
                      <button
                        type="button"
                        onClick={() => handleSelect(session.sessionKey)}
                        className={`flex w-full flex-col gap-1.5 px-4 py-3 text-left transition ${
                          isActive
                            ? "bg-brand/5 border-l-2 border-brand"
                            : "border-l-2 border-transparent hover:bg-slate-50"
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="inline-flex items-center rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600">
                            {intentLabel(session.intent)}
                          </span>
                          {session.unanswered && (
                            <span className="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-medium text-amber-700">
                              미답변
                            </span>
                          )}
                        </div>
                        <p className="line-clamp-2 text-sm text-slate-800">
                          {session.lastMessage ?? "(메시지 없음)"}
                        </p>
                        <div className="flex items-center justify-between text-[11px] text-slate-400">
                          <span>{formatDateTime(session.lastMessageAt)}</span>
                          <span>{(session.messageCount ?? 0).toLocaleString()}건</span>
                        </div>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>

        {/* Right: message thread */}
        <div className="flex flex-1 flex-col">
          {selectedSessionKey == null ? (
            <div className="flex flex-1 flex-col items-center justify-center text-center text-slate-400">
              <MessageSquare className="mb-2 h-8 w-8" />
              <p className="text-sm">왼쪽에서 상담 세션을 선택하세요.</p>
            </div>
          ) : messagesQuery.isLoading ? (
            <div className="flex flex-1 items-center justify-center">
              <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
            </div>
          ) : messagesQuery.isError ? (
            <div className="flex flex-1 items-center justify-center px-4 text-center">
              <p className="text-sm text-red-600">
                대화 이력을 불러오지 못했습니다.
              </p>
            </div>
          ) : (
            <>
              <div className="flex-1 space-y-3 overflow-y-auto p-4">
                {messages.length === 0 ? (
                  <div className="flex h-full items-center justify-center text-center">
                    <p className="text-sm text-slate-400">
                      대화 내용이 없습니다.
                    </p>
                  </div>
                ) : (
                  messages.map((row) => {
                    const isUser = row.role === ChatMessageRowRole.USER;
                    return (
                      <div
                        key={row.id}
                        className={`flex ${isUser ? "justify-start" : "justify-end"}`}
                      >
                        <div
                          className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                            isUser
                              ? "bg-slate-100 text-slate-800"
                              : "bg-brand/10 text-slate-800"
                          }`}
                        >
                          <p className="whitespace-pre-wrap break-words">
                            {row.content ?? ""}
                          </p>
                          <div className="mt-1 flex items-center gap-2 text-[10px] text-slate-400">
                            <span>{isUser ? "고객" : "상담봇/운영자"}</span>
                            <span>{formatDateTime(row.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              {/* Reply box */}
              <div className="border-t border-slate-200 p-3">
                {message && (
                  <p className="mb-2 rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
                    {message}
                  </p>
                )}
                <div className="flex items-end gap-2">
                  <textarea
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    placeholder="답변을 입력하세요…"
                    rows={2}
                    maxLength={2000}
                    className="flex-1 resize-none rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                  />
                  <button
                    type="button"
                    onClick={handleSend}
                    disabled={replyMutation.isPending || draft.trim() === ""}
                    className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark disabled:opacity-50"
                  >
                    {replyMutation.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="h-4 w-4" />
                    )}
                    전송
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
