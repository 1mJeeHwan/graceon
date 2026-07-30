import { describe, expect, it } from "vitest";

import { classifyPath } from "../analytics";

describe("classifyPath", () => {
  it("tracks album detail as a content view", () => {
    expect(classifyPath("/albums/12")).toEqual({
      type: "CONTENT_VIEW",
      contentType: "ALBUM",
      targetId: 12,
    });
  });

  it("tracks video detail as a content view", () => {
    expect(classifyPath("/video/7")).toEqual({
      type: "CONTENT_VIEW",
      contentType: "VIDEO",
      targetId: 7,
    });
  });

  it("keeps list routes as plain page views", () => {
    for (const path of ["/", "/albums", "/video", "/churches"]) {
      expect(classifyPath(path)).toEqual({
        type: "PAGE_VIEW",
        contentType: "PAGE",
        targetId: null,
      });
    }
  });

  it("does not mistake a non-numeric segment for a target id", () => {
    expect(classifyPath("/video/featured").type).toBe("PAGE_VIEW");
  });
});
