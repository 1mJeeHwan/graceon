package org.streamhub.api.v1.inquiry.entity;

/**
 * Lifecycle state of a 1:1 customer inquiry.
 */
public enum InquiryStatus {
    /** Newly submitted, awaiting an operator answer. */
    OPEN,
    /** An operator has replied. */
    ANSWERED,
    /** Resolved and closed; no further action expected. */
    CLOSED
}
