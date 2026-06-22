"use client";

import { useEffect, useRef, useState } from "react";
import { useEditor, EditorContent, type Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import Link from "@tiptap/extension-link";
import {
  Bold,
  Italic,
  Heading2,
  List,
  ListOrdered,
  Link as LinkIcon,
  ImagePlus,
  Loader2,
  Undo2,
  Redo2,
} from "lucide-react";

import { mediaUpload } from "@/apis/media";

interface RichTextEditorProps {
  value: string;
  onChange: (html: string) => void;
  /** Media-library category for images uploaded from the toolbar. */
  category?: string;
  placeholder?: string;
}

function ToolbarButton({
  active,
  disabled,
  onClick,
  title,
  children,
}: {
  active?: boolean;
  disabled?: boolean;
  onClick: () => void;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      title={title}
      disabled={disabled}
      onMouseDown={(e) => e.preventDefault()}
      onClick={onClick}
      className={`rounded p-1.5 text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 ${
        active ? "bg-brand/10 text-brand" : ""
      }`}
    >
      {children}
    </button>
  );
}

/**
 * TipTap-based rich text editor that emits sanitizable HTML. The image button uploads to the media
 * library (so every embedded image is centrally managed) and inserts the returned CDN URL. Bind it
 * to a form field via value/onChange (use react-hook-form's Controller for RHF forms).
 */
export function RichTextEditor({
  value,
  onChange,
  category = "post",
  placeholder = "내용을 입력하세요…",
}: RichTextEditorProps) {
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const editor = useEditor({
    immediatelyRender: false, // Next.js SSR: defer first render to the client
    extensions: [
      StarterKit,
      Image.configure({ inline: false, allowBase64: false }),
      Link.configure({ openOnClick: false, autolink: true }),
    ],
    content: value || "",
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
    editorProps: {
      attributes: {
        class:
          "min-h-[200px] px-3 py-2 outline-none [&_h2]:mt-2 [&_h2]:text-lg [&_h2]:font-bold " +
          "[&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5 [&_a]:text-brand [&_a]:underline " +
          "[&_img]:my-2 [&_img]:max-w-full [&_img]:rounded [&_p]:leading-relaxed",
        "data-placeholder": placeholder,
      },
    },
  });

  // Sync external value (form load/reset) without disrupting active typing.
  useEffect(() => {
    if (editor && !editor.isFocused && value !== editor.getHTML()) {
      editor.commands.setContent(value || "", false);
    }
  }, [value, editor]);

  const handleImage = async (file: File | undefined, ed: Editor) => {
    if (!file || !file.type.startsWith("image/")) {
      return;
    }
    setUploading(true);
    try {
      const asset = await mediaUpload(file, category);
      if (asset?.url) {
        ed.chain().focus().setImage({ src: asset.url, alt: asset.originalName ?? "" }).run();
      }
    } catch {
      /* surfaced by the empty insert; keep editor usable */
    } finally {
      setUploading(false);
      if (fileRef.current) {
        fileRef.current.value = "";
      }
    }
  };

  const addLink = (ed: Editor) => {
    const url = window.prompt("링크 URL을 입력하세요");
    if (url === null) {
      return;
    }
    if (url === "") {
      ed.chain().focus().extendMarkRange("link").unsetLink().run();
      return;
    }
    ed.chain().focus().extendMarkRange("link").setLink({ href: url }).run();
  };

  if (!editor) {
    return (
      <div className="flex min-h-[240px] items-center justify-center rounded-md border border-slate-300 text-slate-400">
        <Loader2 className="h-5 w-5 animate-spin" />
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-md border border-slate-300 focus-within:border-brand focus-within:ring-1 focus-within:ring-brand">
      <div className="flex flex-wrap items-center gap-0.5 border-b border-slate-200 bg-slate-50 px-2 py-1">
        <ToolbarButton title="굵게" active={editor.isActive("bold")} onClick={() => editor.chain().focus().toggleBold().run()}>
          <Bold className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton title="기울임" active={editor.isActive("italic")} onClick={() => editor.chain().focus().toggleItalic().run()}>
          <Italic className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton
          title="제목"
          active={editor.isActive("heading", { level: 2 })}
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        >
          <Heading2 className="h-4 w-4" />
        </ToolbarButton>
        <span className="mx-1 h-5 w-px bg-slate-200" />
        <ToolbarButton title="글머리 목록" active={editor.isActive("bulletList")} onClick={() => editor.chain().focus().toggleBulletList().run()}>
          <List className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton title="번호 목록" active={editor.isActive("orderedList")} onClick={() => editor.chain().focus().toggleOrderedList().run()}>
          <ListOrdered className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton title="링크" active={editor.isActive("link")} onClick={() => addLink(editor)}>
          <LinkIcon className="h-4 w-4" />
        </ToolbarButton>
        <span className="mx-1 h-5 w-px bg-slate-200" />
        <ToolbarButton title="이미지 삽입" disabled={uploading} onClick={() => fileRef.current?.click()}>
          {uploading ? <Loader2 className="h-4 w-4 animate-spin" /> : <ImagePlus className="h-4 w-4" />}
        </ToolbarButton>
        <span className="ml-auto" />
        <ToolbarButton title="실행 취소" disabled={!editor.can().undo()} onClick={() => editor.chain().focus().undo().run()}>
          <Undo2 className="h-4 w-4" />
        </ToolbarButton>
        <ToolbarButton title="다시 실행" disabled={!editor.can().redo()} onClick={() => editor.chain().focus().redo().run()}>
          <Redo2 className="h-4 w-4" />
        </ToolbarButton>
      </div>
      <EditorContent editor={editor} className="bg-white text-sm text-slate-800" />
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        hidden
        onChange={(event) => void handleImage(event.target.files?.[0], editor)}
      />
    </div>
  );
}
