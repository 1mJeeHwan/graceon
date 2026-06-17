package org.streamhub.api.v1.donation.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;
import org.streamhub.api.v1.member.entity.MemberGrade;

/** Subscription detail, joined with member and plan information. */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionDetail {
    private Long id;
    private Long memberId;
    private String memberName;
    private Long planId;
    private String planName;
    private MemberGrade planGrade;
    private Long planPrice;
    private Integer planPeriodMonths;
    private String billingKeyMasked;
    private SubscriptionStatus status;
    private Integer cycleNo;
    private LocalDateTime nextBillingAt;
    private LocalDateTime startedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
