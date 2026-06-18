package org.streamhub.api.v1.analytics.entity;

/** Kind of analytics event captured from the user site. Stored via {@code @Enumerated(STRING)}. */
public enum EventType {
    PAGE_VIEW,
    CONTENT_VIEW,
    SESSION_START
}
