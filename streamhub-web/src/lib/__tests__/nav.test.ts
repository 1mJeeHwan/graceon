import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

import { canViewSystem, canWrite, isReadOnly } from "../auth-utils";
import { pickActiveHref } from "../nav-active";

const HREFS = ["/goods", "/goods/stock", "/goods/category", "/content", "/content/stats", "/member"];

describe("pickActiveHref", () => {
  it("marks the exact match", () => {
    expect(pickActiveHref(HREFS, "/member")).toBe("/member");
  });

  it("prefers the deepest match so nested menus do not light up their parent", () => {
    expect(pickActiveHref(HREFS, "/goods/stock")).toBe("/goods/stock");
    expect(pickActiveHref(HREFS, "/content/stats")).toBe("/content/stats");
  });

  it("falls back to the parent on a detail route that has no menu of its own", () => {
    expect(pickActiveHref(HREFS, "/goods/123")).toBe("/goods");
    expect(pickActiveHref(HREFS, "/goods/stock/9")).toBe("/goods/stock");
  });

  it("matches on path segments only, never on a string prefix", () => {
    expect(pickActiveHref(HREFS, "/goods-archive")).toBeUndefined();
    expect(pickActiveHref(HREFS, "/dashboard")).toBeUndefined();
  });
});

describe("role gates", () => {
  it.each([
    ["SYSTEM", true, true, false],
    ["CHURCH_MANAGER", true, false, false],
    ["VIEWER", false, true, true],
  ] as const)("%s", (role, write, viewSystem, readOnly) => {
    expect(canWrite(role)).toBe(write);
    expect(canViewSystem(role)).toBe(viewSystem);
    expect(isReadOnly(role)).toBe(readOnly);
  });

  it("denies writes when the session has no role yet", () => {
    expect(canWrite(undefined)).toBe(false);
    expect(canViewSystem(null)).toBe(false);
  });
});

const SRC = join(__dirname, "..", "..");

/** Every `label`/`href` pair declared in the sidebar's NAV_SECTIONS. */
const NAV = [
  ...readFileSync(join(SRC, "components", "layout", "Sidebar.tsx"), "utf8").matchAll(
    /label: "([^"]+)",\s*href: "([^"]+)"/g,
  ),
].map(([, label, href]) => ({ label, href }));

/** Every routable path under `src/app/(protected)`, dynamic segments excluded. */
function routes(dir: string, prefix = ""): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    if (entry.name === "page.tsx") return [prefix || "/"];
    if (!entry.isDirectory() || entry.name.startsWith("[")) return [];
    const segment = entry.name.startsWith("(") ? "" : `/${entry.name}`;
    return routes(join(dir, entry.name), prefix + segment);
  });
}

describe("sidebar menu", () => {
  it("has entries", () => {
    expect(NAV.length).toBeGreaterThan(30);
  });

  it("has no duplicate href or label", () => {
    expect(new Set(NAV.map((n) => n.href)).size).toBe(NAV.length);
    expect(new Set(NAV.map((n) => n.label)).size).toBe(NAV.length);
  });

  it("links only to routes that exist", () => {
    const existing = new Set(routes(join(SRC, "app", "(protected)")));
    expect(NAV.map((n) => n.href).filter((href) => !existing.has(href))).toEqual([]);
  });

  it("lights exactly one menu on every route", () => {
    const hrefs = NAV.map((n) => n.href);
    const unresolved = routes(join(SRC, "app", "(protected)")).filter(
      (route) => pickActiveHref(hrefs, route) === undefined,
    );
    expect(unresolved).toEqual([]);
  });
});
