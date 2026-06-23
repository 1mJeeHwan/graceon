package org.streamhub.api.v1.announcement.dto;

/**
 * Display-order update payload for the drag-to-reorder action.
 *
 * @param sortOrder new zero-based display order
 */
public record AnnouncementSortRequest(int sortOrder) {
}
