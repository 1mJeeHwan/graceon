"use client";

import DOMPurify from "isomorphic-dompurify";

/** Heuristic: treat content as HTML once it contains a tag; otherwise it's a legacy plain body. */
const HTML_TAG = /<\/?[a-z][\s\S]*>/i;

const RICH_CLASS =
  "[&_h2]:mt-3 [&_h2]:text-lg [&_h2]:font-bold [&_h2]:text-active " +
  "[&_ul]:my-2 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:my-2 [&_ol]:list-decimal [&_ol]:pl-5 " +
  "[&_a]:text-primary [&_a]:underline [&_p]:mb-2 [&_img]:my-3 [&_img]:max-w-full [&_img]:rounded-card " +
  "[&_strong]:font-bold [&_em]:italic";

/**
 * Renders admin-authored body content. Rich (HTML) bodies are sanitized with DOMPurify before being
 * injected; legacy plain-text bodies (no tags) keep their line breaks via {@code whitespace-pre-wrap}.
 * Sanitization is the trust boundary even though authors are admins — defense in depth against
 * stored XSS.
 */
export function RichText({ content, className = "" }: { content: string | null; className?: string }) {
  if (!content) {
    return null;
  }
  if (!HTML_TAG.test(content)) {
    return <div className={`whitespace-pre-wrap ${className}`}>{content}</div>;
  }
  const clean = DOMPurify.sanitize(content, { USE_PROFILES: { html: true } });
  return (
    <div
      className={`${RICH_CLASS} ${className}`}
      dangerouslySetInnerHTML={{ __html: clean }}
    />
  );
}
