package org.streamhub.audit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Opaque keyset cursor over {@code (created_at DESC, id DESC)} — URL-safe Base64 of
 * {@code <epoch millis>:<id>}.
 *
 * <p>Byte-compatible with the monolith's {@code ActionLogCursorRequest}: the cursor travels from
 * this service to the monolith to the browser and back, so both ends must read the same encoding.
 * It is intentionally duplicated rather than shared — the two services own their schemas separately
 * and a shared library would couple their release cycles for ~30 lines.
 */
record ActionLogCursor(LocalDateTime createdAt, long id) {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** Decodes a cursor, or null when blank (first page). 400 on a malformed value. */
    static ActionLogCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("expected <millis>:<id>");
            }
            return new ActionLogCursor(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[0])), ZONE),
                    Long.parseLong(parts[1]));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 커서입니다");
        }
    }

    /** Encodes the sort key of {@code row} as the cursor pointing just past it. */
    static String encode(ActionLog row) {
        long millis = row.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((millis + ":" + row.getId()).getBytes(StandardCharsets.UTF_8));
    }
}
