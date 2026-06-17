package org.streamhub.api.v1.dashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The 6-KPI strip for the operations dashboard. Each field is a {@link KpiDelta}
 * (current value + previous-period comparison + sparkline). Cached in Redis (60s).
 */
@Getter
@Setter
@NoArgsConstructor
public class DashboardSummaryResponse {

    /** 오늘 후원·매출 — today's settled order revenue + paid donations (KRW). */
    private KpiDelta todayRevenue;

    /** 신규 구독 — subscriptions started today. */
    private KpiDelta newSubscriptions;

    /** 진행 중 주문 — orders not yet in a terminal state (PLACED/PAID/READY/SHIPPING). */
    private KpiDelta openOrders;

    /**
     * 미답변 문의 — unanswered inquiries. No INQUIRY table exists yet, so this is
     * always 0; replace the mapper subquery once the inquiry domain lands.
     */
    private KpiDelta unansweredInquiry;

    /** 재고 경고 — goods items at or below their low-stock threshold. */
    private KpiDelta lowStock;

    /** 활성 구독자 — currently ACTIVE recurring-donation subscriptions. */
    private KpiDelta activeSubscribers;
}
