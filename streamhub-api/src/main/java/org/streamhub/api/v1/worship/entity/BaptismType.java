package org.streamhub.api.v1.worship.entity;

/** Baptism stage (세례 단계). Stored via {@code @Enumerated(STRING)}. */
public enum BaptismType {
    /** 없음. */
    NONE,
    /** 세례. */
    BAPTISM,
    /** 입교. */
    CONFIRMATION,
    /** 유아세례. */
    INFANT_BAPTISM
}
