/**
 * Hand-written client for POST /v1/notification/send.
 *
 * This endpoint ships after the current Orval spec, so it is not yet present in
 * the generated `src/apis/query/` client. Until the spec is regenerated post-
 * deploy, the request is issued directly through `customInstance` (the same
 * mutator the generated hooks use, so auth + token refresh behave identically).
 *
 * Once the backend is deployed and `npm run gen` is run, the generated
 * `notification.ts` hook supersedes this file and it can be removed.
 */
import { customInstance } from "./custom-instance";

import type { NotificationLogDto } from "./query/streamHubAdminAPI.schemas";

/** Delivery channel for a notification send. */
export type NotificationSendChannel = "SMS" | "PUSH" | "EMAIL";

/**
 * Audience scope. BROADCAST targets every member (memberIds ignored); TARGETED
 * sends only to the listed member ids (at least one required).
 */
export type NotificationSendScope = "BROADCAST" | "TARGETED";

/** Request body for POST /v1/notification/send. */
export interface NotificationSendRequest {
  channel: NotificationSendChannel;
  title: string;
  content: string;
  scope: NotificationSendScope;
  /** Ignored when scope is BROADCAST; required (1+) when scope is TARGETED. */
  memberIds: number[];
}

/**
 * Standard backend envelope. resultCode === "0000" indicates success and
 * resultObject carries the created send log.
 */
export interface ResultDTONotificationSend {
  resultCode?: string;
  resultMessage?: string;
  resultObject?: NotificationLogDto;
}

/**
 * notificationSend posts a notification send request. It returns the unwrapped
 * ResultDTO envelope; callers check `resultCode === SUCCESS_CODE`.
 */
export const notificationSend = (
  data: NotificationSendRequest,
  signal?: AbortSignal,
): Promise<ResultDTONotificationSend> => {
  return customInstance<ResultDTONotificationSend>({
    url: `/v1/notification/send`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data,
    signal,
  });
};
