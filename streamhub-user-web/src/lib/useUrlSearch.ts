"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useDebounce } from "./useDebounce";

/**
 * Search keyword bound to the URL `?q=` param: initial value comes from the URL, and the
 * debounced value is written back (replace, not push) so search is shareable, refresh-safe,
 * and survives back/forward without polluting history on every keystroke.
 *
 * Must be used inside a <Suspense> boundary (useSearchParams requirement).
 */
export function useUrlSearch() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [keyword, setKeyword] = useState(() => searchParams.get("q") ?? "");
  const debounced = useDebounce(keyword).trim();

  useEffect(() => {
    const url = debounced ? `${pathname}?q=${encodeURIComponent(debounced)}` : pathname;
    router.replace(url, { scroll: false });
  }, [debounced, pathname, router]);

  return { keyword, setKeyword, debounced };
}
