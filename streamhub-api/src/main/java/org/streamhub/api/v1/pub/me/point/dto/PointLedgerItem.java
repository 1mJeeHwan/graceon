package org.streamhub.api.v1.pub.me.point.dto;

import java.time.LocalDateTime;

/**
 * One entry in the member's own point ledger ("내 포인트" 내역). Maps directly from
 * {@link org.streamhub.api.v1.member.entity.PointLedger}: {@code amount} is the signed
 * {@code delta} (positive accrues, negative uses/deducts), {@code type} is the ledger
 * {@code sourceType} name, and {@code memo} is the human-readable {@code reason}.
 *
 * @param id        ledger entry id
 * @param amount    signed point change (+ accrue / - use)
 * @param type      ledger source type ({@code MANUAL} / {@code DONATION} / {@code EXPIRY} …)
 * @param memo      human-readable reason
 * @param createdAt when the entry was recorded
 */
public record PointLedgerItem(
        Long id,
        long amount,
        String type,
        String memo,
        LocalDateTime createdAt) {
}
