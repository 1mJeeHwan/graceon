package org.streamhub.api.v1.order.entity;

/**
 * Order state machine.
 *
 * <p>Happy path: {@code PLACED → PAID → READY → SHIPPING → DONE}, with branches
 * {@code CANCEL} / {@code RETURN}. The authoritative transition map lives in the
 * order service; this enum only declares the states.
 */
public enum OrderStatus {
    /** 주문. */
    PLACED,
    /** 입금(결제확정). */
    PAID,
    /** 배송준비. */
    READY,
    /** 배송중. */
    SHIPPING,
    /** 완료. */
    DONE,
    /** 취소. */
    CANCEL,
    /** 반품. */
    RETURN
}
