"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import { Loader2, LogIn, MessageSquarePlus, Star, X } from "lucide-react";
import clsx from "clsx";
import { useAuth } from "@/lib/auth";
import { useCreateInquiry, useCreateReview } from "@/lib/me";

/** Bottom-sheet modal shell (portal to body, escaping the page's animate-fade-up transform). */
function FormModal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-black/60"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onClick={onClose}
    >
      <div
        className="mx-auto flex max-h-[88dvh] w-full max-w-[480px] flex-col overflow-hidden rounded-t-2xl border-x border-t border-border bg-bg animate-fade-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-border bg-surface px-4 py-3.5">
          <h2 className="text-base font-bold text-active">{title}</h2>
          <button type="button" aria-label="닫기" onClick={onClose} className="rounded-lg p-1.5 text-inactive transition active:bg-card">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-4 py-4">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

const FIELD = "w-full rounded-xl border border-border bg-surface px-3.5 py-2.5 text-sm text-active outline-none placeholder:text-inactive/60 focus:border-primary";

function InquiryForm({ goodsItemId, token, onDone }: { goodsItemId: number; token: string; onDone: () => void }) {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const create = useCreateInquiry(token);
  const valid = title.trim().length > 0 && content.trim().length > 0;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valid || create.isPending) return;
        create.mutate(
          { goodsItemId, title: title.trim(), content: content.trim() },
          { onSuccess: onDone },
        );
      }}
    >
      <label className="mb-1 block text-xs font-semibold text-inactive">제목</label>
      <input className={FIELD} value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} placeholder="문의 제목" />
      <label className="mb-1 mt-3 block text-xs font-semibold text-inactive">문의 내용</label>
      <textarea className={clsx(FIELD, "min-h-[120px] resize-y")} value={content} maxLength={1000} onChange={(e) => setContent(e.target.value)} placeholder="궁금한 점을 남겨주세요." />
      {create.isError && <p className="mt-2 text-xs text-point">문의 등록에 실패했습니다. 다시 시도해 주세요.</p>}
      <button type="submit" disabled={!valid || create.isPending} className="btn-primary mt-4 flex w-full items-center justify-center gap-2 py-3 text-sm font-bold disabled:opacity-50">
        {create.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        문의 등록
      </button>
    </form>
  );
}

function ReviewForm({ goodsItemId, token, onDone }: { goodsItemId: number; token: string; onDone: () => void }) {
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const create = useCreateReview(token);
  const valid = content.trim().length > 0 && rating >= 1 && rating <= 5;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!valid || create.isPending) return;
        create.mutate({ goodsItemId, rating, content: content.trim() }, { onSuccess: onDone });
      }}
    >
      <label className="mb-1.5 block text-xs font-semibold text-inactive">별점</label>
      <div className="flex items-center gap-1">
        {Array.from({ length: 5 }).map((_, i) => (
          <button key={i} type="button" aria-label={`${i + 1}점`} onClick={() => setRating(i + 1)}>
            <Star className={clsx("h-7 w-7", i < rating ? "fill-secondary text-secondary" : "text-inactive")} />
          </button>
        ))}
      </div>
      <label className="mb-1 mt-3 block text-xs font-semibold text-inactive">후기 내용</label>
      <textarea className={clsx(FIELD, "min-h-[120px] resize-y")} value={content} maxLength={1000} onChange={(e) => setContent(e.target.value)} placeholder="상품은 어떠셨나요?" />
      {create.isError && <p className="mt-2 text-xs text-point">후기 등록에 실패했습니다. 다시 시도해 주세요.</p>}
      <button type="submit" disabled={!valid || create.isPending} className="btn-primary mt-4 flex w-full items-center justify-center gap-2 py-3 text-sm font-bold disabled:opacity-50">
        {create.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        후기 등록
      </button>
    </form>
  );
}

/**
 * "문의하기 · 후기쓰기" entry for a purchasable product (album with a goodsItemId). Opens a portal
 * form modal that posts to {@code /pub/v1/me/inquiries|reviews}; anonymous visitors get a login CTA.
 * Submitted items appear in the member's mypage "내 문의 / 내 후기".
 */
export function ProductFeedback({ goodsItemId, productName }: { goodsItemId: number; productName: string }) {
  const { token } = useAuth();
  const [open, setOpen] = useState<"inquiry" | "review" | null>(null);
  const [done, setDone] = useState<"inquiry" | "review" | null>(null);

  if (!token) {
    return (
      <div className="mt-5 rounded-card border border-border bg-surface px-4 py-3.5">
        <Link href="/login" className="flex items-center justify-center gap-2 text-sm font-semibold text-primary active:underline">
          <LogIn className="h-4 w-4" />
          로그인하고 문의·후기 남기기
        </Link>
      </div>
    );
  }

  return (
    <div className="mt-5">
      <div className="flex gap-2">
        <button
          onClick={() => { setOpen("inquiry"); setDone(null); }}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-border bg-bg py-3 text-sm font-bold text-active active:bg-card"
        >
          <MessageSquarePlus className="h-4 w-4" />
          문의하기
        </button>
        <button
          onClick={() => { setOpen("review"); setDone(null); }}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-border bg-bg py-3 text-sm font-bold text-active active:bg-card"
        >
          <Star className="h-4 w-4" />
          후기쓰기
        </button>
      </div>
      {done && (
        <p className="mt-2 rounded-lg bg-primary/10 px-3 py-2 text-center text-xs font-semibold text-primary">
          {done === "inquiry" ? "문의가 등록되었습니다. 마이페이지 '내 문의'에서 확인하세요." : "후기가 등록되었습니다. 마이페이지 '내 후기'에서 확인하세요."}
        </p>
      )}

      {open === "inquiry" && (
        <FormModal title={`${productName} 문의하기`} onClose={() => setOpen(null)}>
          <InquiryForm goodsItemId={goodsItemId} token={token} onDone={() => { setOpen(null); setDone("inquiry"); }} />
        </FormModal>
      )}
      {open === "review" && (
        <FormModal title={`${productName} 후기쓰기`} onClose={() => setOpen(null)}>
          <ReviewForm goodsItemId={goodsItemId} token={token} onDone={() => { setOpen(null); setDone("review"); }} />
        </FormModal>
      )}
    </div>
  );
}
