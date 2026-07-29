package org.streamhub.api.v1.donation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.donation.entity.Donation;
import org.streamhub.api.v1.donation.entity.Subscription;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;
import org.streamhub.api.v1.donation.repository.DonationRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionPlanRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionRepository;
import org.streamhub.api.v1.member.entity.MemberGrade;

/**
 * Unit tests for the two-layer billing idempotency guard in {@link BillingService#chargeOneCycle}.
 * The common duplicate (scheduler double-fire, post-commit retry) is caught by an existence check
 * before the insert; a genuine concurrent race is caught by the {@code uk_donation_cycle}
 * constraint and reported as {@link DuplicateCycleException}. Either way: no point accrual, no
 * cycle advance, and the same cycle is never billed twice.
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceIdempotencyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 18, 12, 0);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private DonationRepository donationRepository;
    @Mock
    private PointLedgerWriter pointLedgerWriter;
    @Mock
    private ActionLogPublisher actionLogPublisher;

    @InjectMocks
    private BillingService billingService;

    private Subscription activeSubscription() {
        return Subscription.builder()
                .memberId(1L)
                .planId(1L)
                .billingKeyMasked("bk_****1234")
                .status(SubscriptionStatus.ACTIVE)
                .cycleNo(0)
                .nextBillingAt(NOW)
                .startedAt(NOW)
                .createdAt(NOW)
                .build();
    }

    private SubscriptionPlan plan() {
        return SubscriptionPlan.builder()
                .name("Gold Plan")
                .grade(MemberGrade.GOLD)
                .price(10000L)
                .periodMonths(1)
                .pointRate(5)
                .active("Y")
                .build();
    }

    /**
     * The ordinary duplicate — a scheduler double-fire or a retry after a prior commit. It must be
     * detected <i>before</i> the insert: reaching the constraint would mark the transaction
     * rollback-only, and returning "successfully" from that state still fails at commit.
     */
    @Test
    void alreadyChargedCycle_isSkippedBeforeAnyInsert() {
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan()));
        when(donationRepository.existsBySubscriptionIdAndCycleNo(any(), any())).thenReturn(true);

        billingService.chargeOneCycle(1L, NOW);

        // No insert is attempted at all, so the transaction stays clean and committable.
        verify(donationRepository, never()).saveAndFlush(any(Donation.class));
        verify(pointLedgerWriter, never()).append(anyLong(), anyLong(), anyString(), anyLong());
        verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
        verify(actionLogPublisher, never()).publish(anyString(), anyString(), anyString(), anyString());
        assertThat(sub.getCycleNo()).isEqualTo(0);
    }

    /**
     * Two runners racing inside the window between the existence check and the insert. The
     * constraint catches it, and it surfaces as {@link DuplicateCycleException} rather than a
     * generic failure so the scheduler does not answer a healthy subscription with a FAILED record
     * and a retry-backoff strike.
     */
    @Test
    void concurrentDuplicateInsert_raisesDuplicateCycle_notABillingFailure() {
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan()));
        when(donationRepository.existsBySubscriptionIdAndCycleNo(any(), any())).thenReturn(false);
        when(donationRepository.saveAndFlush(any(Donation.class)))
                .thenThrow(new DataIntegrityViolationException("uk_donation_cycle"));

        assertThatThrownBy(() -> billingService.chargeOneCycle(1L, NOW))
                .isInstanceOf(DuplicateCycleException.class);

        verify(pointLedgerWriter, never()).append(anyLong(), anyLong(), anyString(), anyLong());
        verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
        assertThat(sub.getCycleNo()).isEqualTo(0);
    }

    @Test
    void inactiveSubscription_isSkippedEntirely() {
        Subscription sub = activeSubscription();
        sub.pause(); // now PAUSED
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        billingService.chargeOneCycle(1L, NOW);

        verify(donationRepository, never()).saveAndFlush(any(Donation.class));
        verify(pointLedgerWriter, never()).append(anyLong(), anyLong(), anyString(), anyLong());
    }
}
