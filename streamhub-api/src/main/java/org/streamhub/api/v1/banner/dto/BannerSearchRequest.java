package org.streamhub.api.v1.banner.dto;

import org.streamhub.api.v1.banner.entity.BannerDevice;
import org.streamhub.api.v1.banner.entity.BannerPosition;

/**
 * Banner list filter. All filters optional; results are ordered by {@code sortOrder} ascending
 * then {@code id} ascending.
 *
 * @param position optional placement-slot filter
 * @param device   optional target-device filter
 * @param useYn    optional visibility filter ({@code "Y"}/{@code "N"})
 */
public record BannerSearchRequest(BannerPosition position, BannerDevice device, String useYn) {
}
