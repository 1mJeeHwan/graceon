package org.streamhub.api.v1.worship.entity;

/** Registration department (등록부서). Stored via {@code @Enumerated(STRING)}. */
public enum RegisterDept {
    /** 영아부. */
    INFANT,
    /** 아동부. */
    CHILDREN,
    /** 중고등부. */
    YOUTH,
    /** 청년부. */
    YOUNG_ADULT,
    /** 장년부. */
    ADULT,
    /** 노년부. */
    SENIOR
}
