package org.streamhub.api.v1.member.entity;

/**
 * Membership grade shared across the member, point, and subscription domains.
 *
 * <p>Single source of truth for grade values — the subscription / membership-plan
 * domain references this same enum rather than defining its own (no {@code PlanGrade}
 * duplicate). Grades are presentation/benefit tiers, not operational authorities.
 */
public enum MemberGrade {
    /** 브론즈. */
    BRONZE,
    /** 실버. */
    SILVER,
    /** 골드. */
    GOLD,
    /** 후원천사. */
    ANGEL
}
