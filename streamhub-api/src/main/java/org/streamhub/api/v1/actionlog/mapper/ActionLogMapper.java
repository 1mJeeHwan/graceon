package org.streamhub.api.v1.actionlog.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.streamhub.api.v1.actionlog.dto.ActionLogItem;

/** MyBatis mapper for the audit-log list. Maps to {@code resources/mappers/ActionLogMapper.xml}. */
@Mapper
public interface ActionLogMapper {

    List<ActionLogItem> selectList(
            @Param("action") String action,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    long countList(@Param("action") String action, @Param("keyword") String keyword);

    /**
     * Keyset page: the {@code size} newest rows strictly older than the {@code (cursorCreatedAt,
     * cursorId)} sort key, or the newest rows when the cursor is null. Callers pass {@code size + 1}
     * to detect a following page without a count.
     */
    List<ActionLogItem> selectAfter(
            @Param("action") String action,
            @Param("keyword") String keyword,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("size") int size);
}
