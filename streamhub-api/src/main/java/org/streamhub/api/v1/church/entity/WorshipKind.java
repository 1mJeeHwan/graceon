package org.streamhub.api.v1.church.entity;

/** Kind of worship service. Stored via {@code @Enumerated(STRING)}. */
public enum WorshipKind {
    /** 주일예배. */
    SUNDAY,
    /** 새벽예배. */
    DAWN,
    /** 수요예배. */
    WEDNESDAY,
    /** 금요(철야)예배. */
    FRIDAY,
    /** 청년/학생예배. */
    YOUTH,
    /** 기타. */
    OTHER
}
