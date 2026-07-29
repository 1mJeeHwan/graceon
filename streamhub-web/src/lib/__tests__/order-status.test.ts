import { readFileSync } from "node:fs";
import { join } from "node:path";

import { describe, expect, it } from "vitest";

import { ALLOWED_TRANSITIONS, STATUS_FLOW, STATUS_LABEL, type OrderStatus } from "../order-status";

/**
 * The frontend transition map is a UX-only mirror of the backend state machine
 * (`OrderService.buildTransitions`). Drift shows up as buttons that render but 400 on click, so the
 * test parses the Java source and compares it instead of restating the rules by hand.
 */
function backendTransitions(): Record<string, string[]> {
  const source = readFileSync(
    join(__dirname, "../../../../streamhub-api/src/main/java/org/streamhub/api/v1/order/OrderService.java"),
    "utf8",
  );
  const body = source.slice(source.indexOf("buildTransitions() {"));
  return Object.fromEntries(
    [...body.matchAll(/map\.put\(OrderStatus\.(\w+), Set\.of\(([^)]*)\)\)/g)].map(([, from, to]) => [
      from,
      [...to.matchAll(/OrderStatus\.(\w+)/g)].map(([, status]) => status).sort(),
    ]),
  );
}

describe("order state machine", () => {
  it("mirrors the backend transitions exactly", () => {
    const mirrored = Object.fromEntries(
      Object.entries(ALLOWED_TRANSITIONS).map(([from, to]) => [from, [...to].sort()]),
    );
    expect(mirrored).toEqual(backendTransitions());
  });

  it("cannot leave a terminal status", () => {
    expect(ALLOWED_TRANSITIONS.CANCEL).toEqual([]);
    expect(ALLOWED_TRANSITIONS.RETURN).toEqual([]);
  });

  it("never walks the happy path backwards", () => {
    STATUS_FLOW.forEach((from, index) => {
      const backwards = ALLOWED_TRANSITIONS[from].filter(
        (to) => STATUS_FLOW.includes(to) && STATUS_FLOW.indexOf(to) < index,
      );
      expect(backwards).toEqual([]);
    });
  });

  it("labels every status", () => {
    (Object.keys(ALLOWED_TRANSITIONS) as OrderStatus[]).forEach((status) => {
      expect(STATUS_LABEL[status]).toBeTruthy();
    });
  });
});
