package org.streamhub.api.v1.inquiry.entity;

/**
 * Subject area of a 1:1 customer inquiry, used for routing and filtering.
 */
public enum InquiryCategory {
    /** Login, password, or account questions. */
    ACCOUNT,
    /** Billing, refunds, or payment failures. */
    PAYMENT,
    /** Shipping and delivery questions. */
    DELIVERY,
    /** Video/content playback or availability. */
    CONTENT,
    /** Anything that does not fit the other categories. */
    ETC
}
