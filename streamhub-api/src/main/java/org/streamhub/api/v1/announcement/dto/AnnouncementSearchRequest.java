package org.streamhub.api.v1.announcement.dto;

/**
 * Announcement list filter. {@code enabled} is optional (null = all); results are ordered by
 * {@code sortOrder} ascending then {@code id} ascending.
 *
 * @param enabled optional visibility filter (true = 노출, false = 미노출, null = 전체)
 */
public record AnnouncementSearchRequest(Boolean enabled) {
}
