package org.streamhub.api.v1.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.streamhub.api.v1.notification.entity.NotificationChannel;
import org.streamhub.api.v1.notification.entity.NotificationScope;

/**
 * Admin "알림 발송" request. Log-only demo seam — nothing is actually sent; a SUCCESS log row is
 * recorded. {@code scope=BROADCAST} reaches every member (memberIds ignored); {@code TARGETED}
 * fans out to {@code memberIds} (must be non-empty), recorded in {@code NOTIFICATION_RECIPIENT}.
 *
 * @param channel   delivery channel (SMS/PUSH/EMAIL)
 * @param scope     audience (BROADCAST/TARGETED)
 * @param title     notification title
 * @param content   notification body
 * @param memberIds target member ids (required and non-empty when {@code scope == TARGETED})
 */
public record NotificationSendRequest(
        @NotNull NotificationChannel channel,
        @NotNull NotificationScope scope,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 500) String content,
        List<Long> memberIds) {
}
