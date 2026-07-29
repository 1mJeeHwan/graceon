"use client";

import { Eye } from "lucide-react";

import { useWritePermission } from "@/lib/use-permissions";

/**
 * Banner telling a read-only operator, up front, that this form cannot be submitted.
 *
 * <p>The submit button is already guarded, but on a create form that is the wrong place to find
 * out: the entry links are hidden for this role, so anyone who lands here typed the URL, and
 * without a notice they would fill in every field before discovering the save is inert. Renders
 * nothing for operators who can actually write.
 */
export default function ReadOnlyNotice() {
  const { writable } = useWritePermission();
  if (writable) {
    return null;
  }
  return (
    <div className="mb-4 flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
      <Eye className="h-4 w-4 shrink-0" />
      <span>읽기 전용 계정입니다. 이 화면은 둘러볼 수 있지만 저장할 수 없습니다.</span>
    </div>
  );
}
