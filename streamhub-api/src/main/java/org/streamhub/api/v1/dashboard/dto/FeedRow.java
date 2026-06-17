package org.streamhub.api.v1.dashboard.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal row of the activity-feed union (orders + subscriptions + donations).
 * The service maps this raw row into a presentation-ready {@link FeedItem}.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedRow {

    /** Source row id (within its own table). */
    private Long sourceId;

    /** Feed category: {@code ORDER} | {@code SUBSCRIPTION} | {@code DONATION}. */
    private String kind;

    /** Status / type token carried from the source row (e.g. order status, donation type). */
    private String status;

    /** Numeric amount carried from the source row when present (order total / donation amount). */
    private Long amount;

    /** Member display name behind the event. */
    private String memberName;

    /** When the event happened (source timestamp). */
    private LocalDateTime occurredAt;
}
