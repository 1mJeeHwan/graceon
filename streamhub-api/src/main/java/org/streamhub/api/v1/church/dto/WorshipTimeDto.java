package org.streamhub.api.v1.church.dto;

import org.streamhub.api.v1.church.entity.WorshipKind;
import org.streamhub.api.v1.church.entity.WorshipTime;

/**
 * A single worship-service row, used both in detail responses and as a create/update input
 * (the form sends a dynamic array, replaced on save).
 *
 * @param kind      worship kind
 * @param dayLabel  display day label ("주일", "수요", "매일")
 * @param startTime display-only start time, e.g. {@code "11:00"}
 * @param place     place ("본당", "교육관")
 * @param target    audience ("전체", "장년", "청년")
 * @param sort      display order
 */
public record WorshipTimeDto(
        WorshipKind kind,
        String dayLabel,
        String startTime,
        String place,
        String target,
        Integer sort) {

    /** Maps a persisted {@link WorshipTime} to its DTO. */
    public static WorshipTimeDto from(WorshipTime entity) {
        return new WorshipTimeDto(
                entity.getKind(), entity.getDayLabel(), entity.getStartTime(),
                entity.getPlace(), entity.getTarget(), entity.getSort());
    }
}
