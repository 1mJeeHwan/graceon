package org.streamhub.api.v1.order.entity;

/**
 * Payment progress state of an {@link Order} (C4 payment seam).
 *
 * <p>Happy path: {@code NONE → REQUESTED → APPROVED}, with branches {@code FAILED}
 * and {@code CANCELED}. Stored via {@code @Enumerated(STRING)}; default is {@code NONE}.
 */
public enum PayStatus {
    /** 결제 미시도(기본). */
    NONE,
    /** 결제 요청됨. */
    REQUESTED,
    /** 승인 대기(호환). */
    PENDING,
    /** 승인됨. */
    APPROVED,
    /** 실패. */
    FAILED,
    /** 취소. */
    CANCELED
}
