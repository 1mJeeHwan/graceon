"use client";

import { useMemo, useState } from "react";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { boardList, useBoardDelete } from "@/apis/query/board/board";
import { type BoardDto } from "@/apis/query/graceOnAdminAPI.schemas";
import BoardFormDialog from "@/components/boards/BoardFormDialog";
import { SUCCESS_CODE } from "@/types/api";

/** Renders a permission level as a compact "Lv.N" badge. */
function LevelBadge({ level }: { level?: number }) {
  if (level == null) {
    return <span className="text-slate-400">-</span>;
  }
  return (
    <span className="inline-flex items-center rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-700">
      Lv.{level}
    </span>
  );
}

export default function BoardsPage() {
  const [keyword, setKeyword] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<BoardDto | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["board-list"],
    queryFn: ({ signal }) => boardList(signal),
  });

  const deleteMutation = useBoardDelete();

  const filtered = useMemo(() => {
    const boards: BoardDto[] = listQuery.data?.resultObject ?? [];
    const term = keyword.trim().toLowerCase();
    if (!term) {
      return boards;
    }
    return boards.filter(
      (board) =>
        (board.name ?? "").toLowerCase().includes(term) ||
        (board.code ?? "").toLowerCase().includes(term),
    );
  }, [listQuery.data, keyword]);

  const openCreate = () => {
    setEditing(null);
    setMessage(null);
    setDialogOpen(true);
  };

  const openEdit = (board: BoardDto) => {
    setEditing(board);
    setMessage(null);
    setDialogOpen(true);
  };

  const handleSaved = () => {
    setDialogOpen(false);
    setEditing(null);
    setMessage("저장되었습니다.");
    listQuery.refetch();
  };

  const handleDelete = (board: BoardDto) => {
    if (board.id == null) {
      return;
    }
    if (!window.confirm(`'${board.name ?? "게시판"}'을(를) 삭제하시겠습니까?`)) {
      return;
    }
    setMessage(null);
    deleteMutation.mutate(
      { id: board.id },
      {
        onSuccess: (response) => {
          if (response.resultCode === SUCCESS_CODE) {
            setMessage("삭제되었습니다.");
            listQuery.refetch();
          } else {
            setMessage(response.resultMessage ?? "삭제에 실패했습니다.");
          }
        },
        onError: () => setMessage("삭제 중 오류가 발생했습니다."),
      },
    );
  };

  return (
    <div>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">게시판 관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            게시판과 읽기/쓰기 권한 레벨(1~10)을 관리합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark"
        >
          <Plus className="h-4 w-4" />
          게시판 등록
        </button>
      </div>

      {/* Search bar */}
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-col">
          <label
            htmlFor="board-keyword"
            className="mb-1 text-xs font-medium text-slate-600"
          >
            검색어
          </label>
          <input
            id="board-keyword"
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="게시판명 / 게시판코드"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
        </div>
      </div>

      {/* Summary */}
      <div className="mb-3 flex items-center gap-3">
        <span className="text-sm text-slate-600">
          총 {filtered.length.toLocaleString()}건
        </span>
        {message && (
          <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
            {message}
          </span>
        )}
      </div>

      {/* List */}
      {listQuery.isLoading ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
        </div>
      ) : listQuery.isError ? (
        <div className="flex h-64 items-center justify-center rounded-md border border-slate-200 bg-white">
          <p className="text-sm text-red-600">
            게시판 목록을 불러오지 못했습니다.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-medium text-slate-500">
              <tr>
                <th className="px-4 py-3">게시판코드</th>
                <th className="px-4 py-3">게시판명</th>
                <th className="px-4 py-3">읽기권한</th>
                <th className="px-4 py-3">쓰기권한</th>
                <th className="px-4 py-3">정렬</th>
                <th className="px-4 py-3">사용</th>
                <th className="px-4 py-3 text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-10 text-center text-slate-400"
                  >
                    조회된 게시판이 없습니다.
                  </td>
                </tr>
              ) : (
                filtered.map((board) => (
                  <tr key={board.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono text-xs text-slate-700">
                      {board.code ?? "-"}
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-900">
                      {board.name ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <LevelBadge level={board.readLevel} />
                    </td>
                    <td className="px-4 py-3">
                      <LevelBadge level={board.writeLevel} />
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {board.sortOrder ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          board.useYn === "N"
                            ? "bg-slate-200 text-slate-600"
                            : "bg-emerald-100 text-emerald-700"
                        }`}
                      >
                        {board.useYn === "N" ? "미사용" : "사용"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => openEdit(board)}
                          className="rounded p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-brand"
                          aria-label="수정"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(board)}
                          disabled={deleteMutation.isPending}
                          className="rounded p-1.5 text-slate-500 transition hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                          aria-label="삭제"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {dialogOpen && (
        <BoardFormDialog
          board={editing}
          onClose={() => setDialogOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}
