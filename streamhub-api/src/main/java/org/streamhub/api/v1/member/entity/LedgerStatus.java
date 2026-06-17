package org.streamhub.api.v1.member.entity;

/** Validity state of a point-ledger accrual entry. */
public enum LedgerStatus {
    /** 유효. */
    ACTIVE,
    /** 만료. */
    EXPIRED
}
