package org.streamhub.api.v1.statistics.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.streamhub.api.v1.statistics.dto.ChannelWatchItem;
import org.streamhub.api.v1.statistics.dto.SummaryResponse;
import org.streamhub.api.v1.statistics.dto.TopContentItem;
import org.streamhub.api.v1.statistics.dto.TrendPoint;

/**
 * MyBatis aggregation queries for the dashboard. Maps to
 * {@code resources/mappers/StatMapper.xml}.
 */
@Mapper
public interface StatMapper {

    /** @param churchId restricts the member counts to one church; null = platform-wide */
    SummaryResponse summary(@Param("since") LocalDateTime since, @Param("churchId") Long churchId);

    /** @param churchId restricts the trend to one church; null = platform-wide */
    List<TrendPoint> memberTrend(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 @Param("churchId") Long churchId);

    List<TopContentItem> topContents(@Param("limit") int limit);

    List<ChannelWatchItem> watchByChannel(@Param("since") LocalDateTime since);
}
