package org.streamhub.api.v1.worship.entity;

/**
 * Worship/new-family registration state (C2).
 *
 * <p>Transitions ({@code RECEIVED→{CONTACTED,CANCELED}}, {@code CONTACTED→{COMPLETED,CANCELED}},
 * {@code COMPLETED→{}}, {@code CANCELED→{}}) are enforced by the service, which is the
 * single source of truth. Stored via {@code @Enumerated(STRING)}.
 */
public enum RegistrationStatus {
    /** 접수. */
    RECEIVED,
    /** 연락완료. */
    CONTACTED,
    /** 등록완료. */
    COMPLETED,
    /** 취소. */
    CANCELED
}
