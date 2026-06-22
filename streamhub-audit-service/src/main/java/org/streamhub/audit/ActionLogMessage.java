package org.streamhub.audit;

/**
 * The action event payload as published by the monolith ({@code streamhub-api}). This is the
 * cross-service contract — kept structurally identical to the producer's record so JSON
 * (de)serialization lines up. The consumer owns only what the event carries (no access to the admin
 * domain DB), which is why {@code adminName} must be enriched by the producer (see docs/msa-split.md).
 */
public record ActionLogMessage(
        Long adminId,
        String adminName,
        String action,
        String targetType,
        String targetId,
        String detail,
        String ip) {
}
