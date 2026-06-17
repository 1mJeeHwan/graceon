package org.streamhub.api.v1.dashboard.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.streamhub.api.v1.dashboard.dto.FeedRow;
import org.streamhub.api.v1.dashboard.dto.TrendRow;

/**
 * MyBatis aggregation queries for the operations dashboard. Maps to
 * {@code resources/mappers/DashboardMapper.xml}. Read-only — every method is a SELECT
 * over the commerce/donation tables ({@code ORDERS}, {@code DONATION},
 * {@code SUBSCRIPTION}, {@code GOODS_ITEM}, {@code MEMBER}). N+1-free: each KPI is a
 * single aggregate query.
 */
@Mapper
public interface DashboardMapper {

    /** Settled order revenue ({@code total} of DONE/SHIPPING/READY/PAID orders) in a window. */
    long sumOrderRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Sum of successfully PAID donations in a window. */
    long sumDonationAmount(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Subscriptions whose {@code started_at} falls in a window. */
    long countNewSubscriptions(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Orders in a non-terminal state (PLACED/PAID/READY/SHIPPING). */
    long countOpenOrders();

    /** Goods items at or below their low-stock threshold and still on sale. */
    long countLowStock();

    /** Currently ACTIVE recurring-donation subscriptions. */
    long countActiveSubscribers();

    /** Daily goods-revenue / recurring / once-donation totals across a date range (sparse). */
    List<TrendRow> dailyTrend(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Most recent N activity events (orders ∪ subscriptions ∪ donations), newest first. */
    List<FeedRow> recentActivity(@Param("limit") int limit);
}
