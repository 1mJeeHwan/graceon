package org.streamhub.api.v1.donation.entity;

/** Settlement state of a donation record. */
public enum DonationStatus {
    /** 결제완료. */
    PAID,
    /** 취소. */
    CANCELED,
    /** 실패. */
    FAILED
}
