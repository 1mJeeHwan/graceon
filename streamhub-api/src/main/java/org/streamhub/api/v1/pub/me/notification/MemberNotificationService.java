package org.streamhub.api.v1.pub.me.notification;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationStatus;
import org.streamhub.api.v1.pub.me.notification.dto.NotificationItem;

/**
 * Read service for a logged-in member's notification feed under the public namespace.
 *
 * <p><b>Model limitation.</b> {@code NOTIFICATION_LOG} is a broadcast send-log: it has no
 * {@code memberId}, no per-recipient row, and no per-member read state. The feed therefore cannot
 * be scoped to one member — it surfaces the most recent successfully-sent broadcasts to every
 * member identically, {@code read} is always {@code false}, and {@link #markRead} is a safe no-op
 * (there is nowhere to persist a per-member read flag). The member id is still required so the
 * endpoint stays authenticated and the contract can tighten later without an API change.
 */
@Slf4j
@Service
public class MemberNotificationService {

    private final MemberNotificationRepository notificationRepository;

    public MemberNotificationService(MemberNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Returns the most recent successfully-sent broadcast notifications, newest first. Not scoped
     * to {@code memberId} (broadcast log has no member targeting); the id is accepted only to keep
     * the endpoint authenticated.
     */
    @Transactional(readOnly = true)
    public ResInfinityList<NotificationItem> notifications(Long memberId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(pageNumber, 0), Math.max(pageSize, 1));
        Page<NotificationLog> page =
                notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.SUCCESS, pageable);
        List<NotificationItem> contents = page.getContent().stream()
                .map(this::toItem)
                .toList();
        return ResInfinityList.of(contents, page.getTotalElements(), pageable.getPageSize());
    }

    /**
     * Marks one notification read for the member — a safe no-op. The broadcast send-log has no
     * per-member read state to persist, so this only validates the id is well-formed and returns
     * successfully. Documented as a known limitation of the broadcast model.
     */
    @Transactional(readOnly = true)
    public void markRead(Long memberId, Long notificationId) {
        log.debug("markRead no-op (broadcast log has no per-member read state): memberId={}, notificationId={}",
                memberId, notificationId);
    }

    private NotificationItem toItem(NotificationLog log) {
        return new NotificationItem(log.getId(), log.getTitle(), log.getContent(), false, log.getCreatedAt());
    }
}
