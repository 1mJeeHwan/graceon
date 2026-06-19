package org.streamhub.api.v1.pub.me.point.dto;

import org.streamhub.api.base.response.ResInfinityList;

/**
 * The member's own point summary ("내 포인트"): the current cached balance plus a paginated,
 * newest-first slice of the point ledger.
 *
 * @param balance the member's current point balance ({@code Member.pointBalance})
 * @param ledger  paginated point ledger entries, newest first
 */
public record MemberPointsDto(
        long balance,
        ResInfinityList<PointLedgerItem> ledger) {
}
