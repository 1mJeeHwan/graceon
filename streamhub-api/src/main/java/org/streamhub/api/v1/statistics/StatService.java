package org.streamhub.api.v1.statistics;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.v1.statistics.dto.ChannelWatchItem;
import org.streamhub.api.v1.statistics.dto.SummaryResponse;
import org.streamhub.api.v1.statistics.dto.TopContentItem;
import org.streamhub.api.v1.statistics.dto.TrendPoint;
import org.streamhub.api.v1.statistics.mapper.StatMapper;

/**
 * Dashboard statistics via MyBatis aggregation. The summary is cached in Redis (60s TTL) since it
 * is the most-hit, recompute-heavy query.
 *
 * <p><b>Tenant scope.</b> {@code analytics:read} is granted to CHURCH_MANAGER as well as SYSTEM, so
 * every figure here must state whose data it is. Member-derived metrics are filtered by the
 * operator's church, exactly like {@code DashboardService}, and the cache key is scoped to match —
 * a shared {@code 'all'} key would have served one church's numbers to another.
 *
 * <p>Content and analytics metrics stay platform-wide because there is nothing to scope them by:
 * neither {@code CONTENT} nor {@code ANALYTICS_EVENT} carries a church column. That is a modelling
 * limit, not an oversight, and the two methods say so at their declaration. Giving them a real
 * tenant dimension means adding the column and backfilling it, not filtering here.
 */
@Slf4j
@Service
public class StatService {

    /** Look-back window (days) for the channel watch-time aggregation. */
    private static final int WATCH_WINDOW_DAYS = 90;

    private final StatMapper statMapper;

    public StatService(StatMapper statMapper) {
        this.statMapper = statMapper;
    }

    /**
     * Summary cards. Member counts are church-scoped; content counts are platform-wide (no church
     * column on {@code CONTENT}). Cached per church, or under {@code 'all'} for unscoped roles.
     */
    @Cacheable(cacheNames = "summary",
            key = "#principal == null || #principal.isUnscoped() ? 'all' : #principal.churchId()")
    public SummaryResponse getSummary(AdminPrincipal principal) {
        log.info("Computing dashboard summary (cache miss)");
        return statMapper.summary(LocalDateTime.now().minusDays(7), scopedChurchId(principal));
    }

    /** Daily signup trend, restricted to the operator's church when they have one. */
    public List<TrendPoint> getMemberTrend(int days, AdminPrincipal principal) {
        int range = days <= 0 ? 30 : days;
        LocalDateTime to = LocalDateTime.now();
        return statMapper.memberTrend(to.minusDays(range), to, scopedChurchId(principal));
    }

    /**
     * Most-viewed content, platform-wide. {@code CONTENT} has no church column, so this cannot be
     * scoped per tenant without a schema change.
     */
    public List<TopContentItem> getTopContents(int limit) {
        return statMapper.topContents(limit <= 0 ? 5 : limit);
    }

    /**
     * Watch time per channel bucket, aggregated from the live analytics pipeline (CONTENT_VIEW
     * dwell time in {@code ANALYTICS_EVENT}) rather than the dead {@code WATCH_HISTORY} table.
     * Looks back {@value #WATCH_WINDOW_DAYS} days. Platform-wide: {@code ANALYTICS_EVENT} records
     * a session, not a church.
     */
    public List<ChannelWatchItem> getWatchByChannel() {
        return statMapper.watchByChannel(LocalDateTime.now().minusDays(WATCH_WINDOW_DAYS));
    }

    /** Null (= no filter) for SYSTEM and the read-only VIEWER; the own church for CHURCH_MANAGER. */
    private Long scopedChurchId(AdminPrincipal principal) {
        return principal == null || principal.isUnscoped() ? null : principal.churchId();
    }
}
