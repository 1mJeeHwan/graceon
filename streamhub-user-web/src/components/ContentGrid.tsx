import { ContentCard } from "./ContentCard";
import type { ContentListItem } from "@/lib/types";

/** Two-column mobile grid of content cards. */
export function ContentGrid({ items }: { items: ContentListItem[] }) {
  return (
    <div className="grid grid-cols-2 gap-x-3 gap-y-5 px-5">
      {items.map((item) => (
        <ContentCard key={item.id} item={item} />
      ))}
    </div>
  );
}
