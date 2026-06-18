package org.streamhub.api.v1.analytics.entity;

/** Client device class reported by the user site. Stored via {@code @Enumerated(STRING)}. */
public enum DeviceKind {
    PC,
    MOBILE,
    TABLET
}
