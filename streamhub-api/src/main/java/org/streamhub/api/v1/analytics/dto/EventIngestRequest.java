package org.streamhub.api.v1.analytics.dto;

/**
 * Ingest payload posted by the user site for one analytics event. Every field is lenient/nullable
 * — the enums arrive as raw strings and are parsed defensively by the service (bad input is
 * clamped to a default rather than rejected), since this endpoint is hit directly by the browser.
 *
 * @param type        event type string (PAGE_VIEW / CONTENT_VIEW / SESSION_START)
 * @param contentType content kind string (VIDEO / ALBUM / POST / PAGE)
 * @param targetId    id of the viewed content (null for plain page views)
 * @param title       denormalized content title sent by the client
 * @param path        the client route, e.g. {@code /albums/2}
 * @param sessionId   client-generated session id
 * @param memberId    signed-in member id (null for anonymous)
 * @param deviceType  device class string (PC / MOBILE / TABLET)
 * @param referrer    referrer source
 * @param dwellMs     time spent on the content/page in ms (sent on leave)
 */
public record EventIngestRequest(String type, String contentType, Long targetId, String title,
                                 String path, String sessionId, Long memberId, String deviceType,
                                 String referrer, Long dwellMs) {
}
