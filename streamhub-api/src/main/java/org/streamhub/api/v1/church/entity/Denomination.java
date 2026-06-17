package org.streamhub.api.v1.church.entity;

/**
 * Church denomination. Stored via {@code @Enumerated(STRING)}. Korean labels are
 * resolved on the frontend ({@code DENOMINATION_LABELS}); the backend stores/searches
 * the enum name only.
 */
public enum Denomination {
    /** 감리교. */
    METHODIST,
    /** 장로교(통합, 예장통합). */
    PCK,
    /** 장로교(합동, 예장합동). */
    HAPDONG,
    /** 성결교. */
    HOLINESS,
    /** 순복음. */
    GOSPEL,
    /** 침례교. */
    BAPTIST,
    /** 기타. */
    ETC
}
