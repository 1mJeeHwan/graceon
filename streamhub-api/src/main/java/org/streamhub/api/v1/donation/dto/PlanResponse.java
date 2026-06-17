package org.streamhub.api.v1.donation.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.member.entity.MemberGrade;

/** Membership plan response, including the count of active subscriptions on the plan. */
@Getter
@Setter
@NoArgsConstructor
public class PlanResponse {
    private Long id;
    private String name;
    private MemberGrade grade;
    private Long price;
    private Integer periodMonths;
    private Integer pointRate;
    private String benefit;
    private String active;
    private long activeSubscriptionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Builds a response from a plan entity and its active-subscription count. */
    public static PlanResponse of(SubscriptionPlan plan, long activeSubscriptionCount) {
        PlanResponse response = new PlanResponse();
        response.id = plan.getId();
        response.name = plan.getName();
        response.grade = plan.getGrade();
        response.price = plan.getPrice();
        response.periodMonths = plan.getPeriodMonths();
        response.pointRate = plan.getPointRate();
        response.benefit = plan.getBenefit();
        response.active = plan.getActive();
        response.activeSubscriptionCount = activeSubscriptionCount;
        response.createdAt = plan.getCreatedAt();
        response.updatedAt = plan.getUpdatedAt();
        return response;
    }
}
