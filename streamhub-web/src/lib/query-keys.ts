import type { Query } from "@tanstack/react-query";
import type {
  MemberSearchRequest,
  OrderSearchRequest,
} from "@/apis/query/graceOnAdminAPI.schemas";

/**
 * Shared React Query key factories and invalidation predicates.
 *
 * Screens used to call `listQuery.refetch()` after a mutation, which only refreshes the screen the
 * user happens to be on. Everything else — the dashboard KPIs above all — kept serving its cached
 * value until the 60s staleTime lapsed, so approving a member left the "new members" figure wrong
 * on the very next click.
 *
 * Hand-written list keys are hierarchical (`[area, "list", criteria]`) so a mutation can invalidate
 * a whole area by prefix without knowing which screens are mounted, while a filter change is still
 * its own cache entry that `keepPreviousData` can page through.
 */
export const memberKeys = {
  all: ["member"] as const,
  list: (criteria: MemberSearchRequest) => ["member", "list", criteria] as const,
};

export const orderKeys = {
  all: ["order"] as const,
  list: (criteria: OrderSearchRequest) => ["order", "list", criteria] as const,
};

/**
 * Dashboard and statistics aggregates.
 *
 * These queries come from the generated Orval client, which keys them by request path
 * (`["/v1/dashboard/summary"]`) rather than by a domain prefix we control — so they cannot be
 * invalidated with a `queryKey` prefix and need a predicate instead. Matching on the path prefix
 * keeps working when a new dashboard endpoint is generated, which an explicit key list would not.
 */
const DASHBOARD_PATH_PREFIXES = ["/v1/dashboard", "/v1/statistics", "/v1/visit"];

export const dashboardKeys = {
  /** Pass as `invalidateQueries({ predicate: dashboardKeys.matches })`. */
  matches: (query: Query) => {
    const first = query.queryKey[0];
    return (
      typeof first === "string" &&
      DASHBOARD_PATH_PREFIXES.some((prefix) => first.startsWith(prefix))
    );
  },
};
