package org.streamhub.api.v1.member.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.member.entity.LedgerSourceType;
import org.streamhub.api.v1.member.entity.LedgerStatus;

/**
 * One row of the point-ledger list, joined with the owning member's name/email/church.
 * Populated by MyBatis ({@code PointMapper.selectList} / {@code selectById}).
 */
@Getter
@Setter
@NoArgsConstructor
public class PointLedgerListItem {
    private Long id;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private Long churchId;
    private long delta;
    private long balanceAfter;
    private String reason;
    private LedgerSourceType sourceType;
    private LedgerStatus status;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
