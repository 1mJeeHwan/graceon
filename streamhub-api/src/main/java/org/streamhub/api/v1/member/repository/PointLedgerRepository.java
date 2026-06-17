package org.streamhub.api.v1.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.member.entity.LedgerStatus;
import org.streamhub.api.v1.member.entity.PointLedger;

/**
 * JPA repository for {@link PointLedger}. The joined paginated listing lives in MyBatis
 * ({@code PointLedgerMapper}); this exposes the expiry-scan and prior-balance lookups.
 */
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    /** Expiry batch scan — uses {@code idx_point_ledger_expire}. */
    List<PointLedger> findByStatusAndExpireAtBefore(LedgerStatus status, LocalDateTime at);

    /** Most recent entry for a member — supplies the prior balance for the next accrual. */
    Optional<PointLedger> findTopByMemberIdOrderByIdDesc(Long memberId);

    long countByMemberId(Long memberId);
}
