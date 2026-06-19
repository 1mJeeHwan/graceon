package org.streamhub.api.v1.pub.me.donation.dto;

import java.time.LocalDateTime;

/**
 * One of the logged-in member's recurring-donation subscriptions.
 *
 * @param id            subscription id
 * @param name          plan name (from {@code SUBSCRIPTION_PLAN.name})
 * @param amount        monthly charge amount in KRW (from {@code SUBSCRIPTION_PLAN.price})
 * @param cycle         billing cycle, derived from the plan period (e.g. {@code MONTHLY},
 *                      {@code EVERY_3_MONTHS})
 * @param status        subscription status enum name ({@code ACTIVE}/{@code PAUSED}/{@code CANCELED})
 * @param nextBillingAt next scheduled billing time; null when PAUSED or CANCELED
 * @param startedAt     when the subscription started
 */
public record MyDonationItem(
        Long id,
        String name,
        long amount,
        String cycle,
        String status,
        LocalDateTime nextBillingAt,
        LocalDateTime startedAt) {
}
