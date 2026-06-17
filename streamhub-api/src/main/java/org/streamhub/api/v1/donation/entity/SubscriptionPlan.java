package org.streamhub.api.v1.donation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.streamhub.api.v1.member.entity.MemberGrade;

/** A membership / recurring-donation plan. Grade reuses the shared {@link MemberGrade}. */
@Entity
@Table(name = "SUBSCRIPTION_PLAN", indexes = {
        @Index(name = "idx_plan_active", columnList = "active"),
        @Index(name = "idx_plan_grade", columnList = "grade")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private MemberGrade grade;

    /** Monthly charge amount (KRW). */
    @Column(name = "price", nullable = false)
    private Long price;

    /** Billing period in months (default 1). */
    @Column(name = "period_months", nullable = false)
    private Integer periodMonths;

    /** Grace-point accrual rate as a percentage of the charge (e.g. 5). */
    @Column(name = "point_rate", nullable = false)
    private Integer pointRate;

    @Column(name = "benefit", length = 500)
    private String benefit;

    /** "Y"/"N". */
    @Column(name = "active", nullable = false, length = 1)
    private String active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private SubscriptionPlan(String name, MemberGrade grade, Long price, Integer periodMonths,
                             Integer pointRate, String benefit, String active,
                             LocalDateTime createdAt) {
        this.name = name;
        this.grade = grade;
        this.price = price;
        this.periodMonths = periodMonths != null ? periodMonths : 1;
        this.pointRate = pointRate != null ? pointRate : 0;
        this.benefit = benefit;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Updates editable plan fields. */
    public void update(String name, MemberGrade grade, Long price, Integer periodMonths,
                       Integer pointRate, String benefit, String active) {
        this.name = name;
        this.grade = grade;
        this.price = price;
        this.periodMonths = periodMonths;
        this.pointRate = pointRate;
        this.benefit = benefit;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    /** Deactivates the plan ({@code active = "N"}). */
    public void deactivate() {
        this.active = "N";
        this.updatedAt = LocalDateTime.now();
    }
}
