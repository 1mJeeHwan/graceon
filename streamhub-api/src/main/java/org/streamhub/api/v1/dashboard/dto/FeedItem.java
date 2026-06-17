package org.streamhub.api.v1.dashboard.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One presentation-ready line in the real-time activity feed. The server emits the
 * absolute {@link #occurredAt}; relative-time rendering ("3분 전") is the front-end's
 * responsibility. Never cached — always the latest.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedItem {

    /** Stable composite id ({@code kind + sourceId}) for React keys. */
    private String id;

    /** Feed category: {@code ORDER} | {@code SUBSCRIPTION} | {@code DONATION}. */
    private String kind;

    /** Completed sentence, e.g. {@code "김O준님이 정기후원 결제(9,900원)"}. */
    private String message;

    /** Masked member display name behind the event. */
    private String actorName;

    /** Source timestamp (server time zone). */
    private LocalDateTime occurredAt;
}
