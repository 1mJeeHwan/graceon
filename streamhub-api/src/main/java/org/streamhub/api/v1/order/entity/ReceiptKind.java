package org.streamhub.api.v1.order.entity;

/** Kind of an order receipt record. */
public enum ReceiptKind {
    /** 입금. */
    PAY,
    /** 환불. */
    REFUND
}
