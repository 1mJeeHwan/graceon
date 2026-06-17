package org.streamhub.api.v1.donation.entity;

/** Lifecycle state of a recurring-donation subscription. */
public enum SubscriptionStatus {
    /** 활성(정기청구 진행). */
    ACTIVE,
    /** 일시정지. */
    PAUSED,
    /** 해지. */
    CANCELED
}
