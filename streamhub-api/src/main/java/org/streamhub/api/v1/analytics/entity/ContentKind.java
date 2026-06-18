package org.streamhub.api.v1.analytics.entity;

/** The kind of content (or plain page) an event refers to. Stored via {@code @Enumerated(STRING)}. */
public enum ContentKind {
    VIDEO,
    ALBUM,
    POST,
    PAGE
}
