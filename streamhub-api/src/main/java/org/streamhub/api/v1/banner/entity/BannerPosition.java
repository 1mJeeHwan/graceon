package org.streamhub.api.v1.banner.entity;

/** Placement slot of a banner on the front. Stored via {@code @Enumerated(STRING)}. */
public enum BannerPosition {
    MAIN_TOP,
    MAIN_MIDDLE,
    MAIN_BOTTOM,
    SIDE,
    POPUP
}
