package org.streamhub.api.v1.donation;

/**
 * Raised when a subscription cycle was already charged and the {@code uk_donation_cycle} constraint
 * caught the second attempt.
 *
 * <p>Distinct from a billing failure on purpose. Both roll the cycle transaction back, but they
 * demand opposite responses: a failure means the member was not charged and the subscription should
 * take a retry-backoff strike, whereas a duplicate means the member <i>was</i> charged and the
 * subscription is healthy. Without this distinction the scheduler penalizes a working subscription
 * for the crime of being processed twice.
 */
public class DuplicateCycleException extends RuntimeException {

    public DuplicateCycleException(Long subscriptionId, int cycleNo) {
        super("Cycle " + cycleNo + " for subscription " + subscriptionId + " was already charged");
    }
}
