package org.streamhub.api.v1.donation.entity;

/** Whether a donation is a one-off gift or a subscription billing cycle. */
public enum DonationType {
    /** 단건 후원. */
    ONCE,
    /** 정기후원(회차 청구). */
    SUBSCRIPTION
}
