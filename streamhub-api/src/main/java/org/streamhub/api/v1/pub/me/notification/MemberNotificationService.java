package org.streamhub.api.v1.pub.me.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationScope;
import org.streamhub.api.v1.notification.entity.NotificationStatus;
import org.streamhub.api.v1.pub.me.notification.dto.NotificationItem;
import org.streamhub.api.v1.pub.me.notification.entity.NotificationRead;

/**
 * Read service for a logged-in member's notification feed under the public namespace.
 *
 * <p>A notification is visible to the member when it is successfully sent AND either broadcast or
 * targeted to that member (a {@code NOTIFICATION_RECIPIENT} row). Per-member <em>read state</em> is
 * layered on via the {@code NOTIFICATION_READ} overlay: each list row carries this member's read
 * flag, mark-read records a read marker (idempotent), and an unread count drives the badge.
 */
@Slf4j
@Service
public class MemberNotificationService {

    private static final NotificationScope BROADCAST = NotificationScope.BROADCAST;
    private static final NotificationStatus SENT = NotificationStatus.SUCCESS;

    private final MemberNotificationRepository notificationRepository;
    private final NotificationReadRepository readRepository;

    public MemberNotificationService(MemberNotificationRepository notificationRepository,
                                     NotificationReadRepository readRepository) {
        this.notificationRepository = notificationRepository;
        this.readRepository = readRepository;
    }

    /** A page of notifications visible to the member (newest first), each flagged with read state. */
    @Transactional(readOnly = true)
    public ResInfinityList<NotificationItem> notifications(Long memberId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(pageNumber, 0), Math.max(pageSize, 1));
        Page<NotificationLog> page = notificationRepository.findVisible(SENT, BROADCAST, memberId, pageable);

        List<Long> ids = page.getContent().stream().map(NotificationLog::getId).toList();
        Set<Long> readIds = ids.isEmpty() ? Set.of() : Set.copyOf(readRepository.findReadIds(memberId, ids));

        List<NotificationItem> contents = page.getContent().stream()
                .map(log -> new NotificationItem(
                        log.getId(), log.getTitle(), log.getContent(),
                        readIds.contains(log.getId()), log.getCreatedAt()))
                .toList();
        return ResInfinityList.of(contents, page.getTotalElements(), pageable.getPageSize());
    }

    /** Number of notifications visible to the member that are not yet read (badge count). */
    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        long visible = notificationRepository.countVisible(SENT, BROADCAST, memberId);
        long read = readRepository.countByMemberId(memberId);
        return Math.max(0, visible - read);
    }

    /** Marks one notification read for the member (idempotent). 404 if it isn't visible to them. */
    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        if (!notificationRepository.isVisible(notificationId, SENT, BROADCAST, memberId)) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        if (readRepository.existsByMemberIdAndNotificationId(memberId, notificationId)) {
            return; // already read — no-op
        }
        readRepository.save(NotificationRead.builder()
                .memberId(memberId)
                .notificationId(notificationId)
                .readAt(LocalDateTime.now())
                .build());
    }

    /** Marks every notification visible to the member read (the "모두 읽음" action). */
    @Transactional
    public void markAllRead(Long memberId) {
        List<Long> visibleIds = notificationRepository.findVisibleIds(SENT, BROADCAST, memberId);
        Set<Long> alreadyRead = Set.copyOf(readRepository.findAllReadIds(memberId));
        LocalDateTime now = LocalDateTime.now();
        List<NotificationRead> fresh = visibleIds.stream()
                .filter(id -> !alreadyRead.contains(id))
                .map(id -> NotificationRead.builder().memberId(memberId).notificationId(id).readAt(now).build())
                .toList();
        if (!fresh.isEmpty()) {
            readRepository.saveAll(fresh);
        }
    }
}
