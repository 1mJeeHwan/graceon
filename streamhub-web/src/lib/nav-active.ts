/**
 * pickActiveHref returns the single menu entry that owns the current route: the longest `href`
 * the path sits under.
 *
 * <p>Highlighting every prefix match lit up two entries at once wherever one menu's route nests
 * under another's — 굿즈 관리 (`/goods`) stayed active on 옵션·재고 관리 (`/goods/stock`), and the
 * same for 카테고리·문의·후기 and for 콘텐츠 관리 (`/content`) under 콘텐츠 통계. Picking the
 * longest match keeps 굿즈 관리 correctly lit on a detail route like `/goods/123`, which has no
 * menu entry of its own, while letting the more specific entry win when there is one.
 */
export function pickActiveHref(hrefs: string[], pathname: string): string | undefined {
  return hrefs
    .filter((href) => pathname === href || pathname.startsWith(`${href}/`))
    .sort((a, b) => b.length - a.length)[0];
}
