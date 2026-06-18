import Link from "next/link";
import { Megaphone } from "lucide-react";

/**
 * Thin announcement strip under the app bar, driven by the admin site-config. Rendered by the
 * (server) root layout when enabled. Links somewhere when a link is set, else plain text.
 */
export function AnnouncementBar({ text, link }: { text: string; link?: string }) {
  if (!text.trim()) return null;

  const content = (
    <div className="flex items-center justify-center gap-2 px-5 py-2 text-center">
      <Megaphone className="h-3.5 w-3.5 shrink-0 text-primary" />
      <span className="ellipsis-1 text-[12px] font-medium text-active">{text}</span>
    </div>
  );

  const className = "block border-b border-border/60 bg-primary/10";
  return link ? (
    <Link href={link} className={className}>
      {content}
    </Link>
  ) : (
    <div className={className}>{content}</div>
  );
}
