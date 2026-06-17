package org.streamhub.api.v1.donation.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;
import org.streamhub.api.v1.member.entity.MemberGrade;

/** One row of the subscription list, joined with member and plan names. */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionListItem {
    private Long id;
    private Long memberId;
    private String memberName;
    private Long planId;
    private String planName;
    private MemberGrade planGrade;
    private SubscriptionStatus status;
    private Integer cycleNo;
    private String billingKeyMasked;
    private LocalDateTime nextBillingAt;
    private LocalDateTime startedAt;
}
