package org.streamhub.api.v1.donation;

import org.springframework.stereotype.Component;
import org.streamhub.api.v1.member.entity.PointLedger;
import org.streamhub.api.v1.member.repository.PointLedgerRepository;

/**
 * Appends rows to the {@link PointLedger}, computing each member's running balance from their
 * latest row. Shared by billing-cycle accrual and one-off donation accrual so both keep an
 * identical, serialized append path (demo-scope concurrency note: §7.3 of the spec).
 */
@Component
public class PointLedgerWriter {

    private final PointLedgerRepository pointLedgerRepository;

    public PointLedgerWriter(PointLedgerRepository pointLedgerRepository) {
        this.pointLedgerRepository = pointLedgerRepository;
    }

    /**
     * Appends one accrual row for a member and returns the resulting balance.
     *
     * @param memberId  member receiving the points
     * @param delta     signed point change (accrual is positive)
     * @param reason    human-readable ledger note
     * @param donationId source donation id (nullable)
     * @return the running balance after this append
     */
    public long append(Long memberId, long delta, String reason, Long donationId) {
        long previous = pointLedgerRepository.findTopByMemberIdOrderByIdDesc(memberId)
                .map(PointLedger::getBalanceAfter)
                .orElse(0L);
        long balanceAfter = previous + delta;
        pointLedgerRepository.save(PointLedger.builder()
                .memberId(memberId)
                .delta(delta)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .donationId(donationId)
                .build());
        return balanceAfter;
    }
}
