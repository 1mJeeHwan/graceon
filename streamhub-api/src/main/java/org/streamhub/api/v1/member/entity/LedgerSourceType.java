package org.streamhub.api.v1.member.entity;

/**
 * Origin of a point-ledger entry.
 *
 * <p>{@code DONATION} lets the recurring-donation billing flow append to the same
 * single point ledger, keeping one source of truth for the member point balance.
 */
public enum LedgerSourceType {
    /** 수동 지급/차감. */
    MANUAL,
    /** 만료 회수. */
    EXPIRE,
    /** 후원 적립. */
    DONATION
}
