package org.streamhub.api.v1.banner.dto;

/**
 * Display-order update payload for the drag-to-reorder action.
 *
 * @param sortOrder new zero-based display order
 */
public record BannerSortRequest(int sortOrder) {
}
