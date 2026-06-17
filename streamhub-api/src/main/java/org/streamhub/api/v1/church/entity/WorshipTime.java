package org.streamhub.api.v1.church.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A worship service time for a church (1:N). Replaced via the goods-style
 * delete-then-reinsert strategy.
 */
@Entity
@Table(name = "WORSHIP_TIME", indexes = {
        @Index(name = "idx_worship_church", columnList = "church_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorshipTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → CHURCH. */
    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 15)
    private WorshipKind kind;

    /** Display day label ("주일", "수요", "매일"). */
    @Column(name = "day_label", nullable = false, length = 20)
    private String dayLabel;

    /** Display-only start time, e.g. {@code "11:00"}. */
    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    @Column(name = "place", length = 50)
    private String place;

    @Column(name = "target", length = 30)
    private String target;

    /** Display order. */
    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Builder
    private WorshipTime(Long churchId, WorshipKind kind, String dayLabel, String startTime,
                        String place, String target, Integer sort) {
        this.churchId = churchId;
        this.kind = kind;
        this.dayLabel = dayLabel;
        this.startTime = startTime;
        this.place = place;
        this.target = target;
        this.sort = sort != null ? sort : 0;
    }
}
