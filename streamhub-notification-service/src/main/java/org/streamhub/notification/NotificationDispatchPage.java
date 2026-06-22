package org.streamhub.notification;

import java.util.List;

/**
 * Paged dispatch response. Mirrors the monolith's {@code ResInfinityList} shape
 * ({@code contents}/{@code totalCount}/{@code totalPage}) so a caller can deserialize it directly.
 */
public record NotificationDispatchPage(List<NotificationDispatchView> contents,
                                       long totalCount,
                                       int totalPage) {
}
