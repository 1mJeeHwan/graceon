package org.streamhub.api.v1.pub.me.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationScope;
import org.streamhub.api.v1.notification.entity.NotificationStatus;

/**
 * Read-side repository for the member notification feed. A notification is <em>visible</em> to a
 * member when it is successfully sent AND either broadcast (scope null/BROADCAST) or targeted to
 * that member (a {@code NOTIFICATION_RECIPIENT} row). Per-member read state lives in
 * {@link NotificationReadRepository}.
 */
public interface MemberNotificationRepository extends JpaRepository<NotificationLog, Long> {

    /** A page of notifications visible to the member (broadcast + targeted-to-member), newest first. */
    @Query("select n from NotificationLog n where n.status = :status and ("
            + "n.scope is null or n.scope = :broadcast "
            + "or n.id in (select r.notificationId from org.streamhub.api.v1.notification.entity.NotificationRecipient r "
            + "where r.memberId = :memberId)) order by n.createdAt desc, n.id desc")
    Page<NotificationLog> findVisible(@Param("status") NotificationStatus status,
                                      @Param("broadcast") NotificationScope broadcast,
                                      @Param("memberId") Long memberId,
                                      Pageable pageable);

    /** Count of notifications visible to the member (the denominator for the unread count). */
    @Query("select count(n) from NotificationLog n where n.status = :status and ("
            + "n.scope is null or n.scope = :broadcast "
            + "or n.id in (select r.notificationId from org.streamhub.api.v1.notification.entity.NotificationRecipient r "
            + "where r.memberId = :memberId))")
    long countVisible(@Param("status") NotificationStatus status,
                      @Param("broadcast") NotificationScope broadcast,
                      @Param("memberId") Long memberId);

    /** True when the notification exists, is sent, and is visible to the member (mark-read guard). */
    @Query("select case when count(n) > 0 then true else false end from NotificationLog n "
            + "where n.id = :id and n.status = :status and ("
            + "n.scope is null or n.scope = :broadcast "
            + "or n.id in (select r.notificationId from org.streamhub.api.v1.notification.entity.NotificationRecipient r "
            + "where r.memberId = :memberId))")
    boolean isVisible(@Param("id") Long id,
                      @Param("status") NotificationStatus status,
                      @Param("broadcast") NotificationScope broadcast,
                      @Param("memberId") Long memberId);

    /** All notification ids visible to the member (used to compute the unread set on read-all). */
    @Query("select n.id from NotificationLog n where n.status = :status and ("
            + "n.scope is null or n.scope = :broadcast "
            + "or n.id in (select r.notificationId from org.streamhub.api.v1.notification.entity.NotificationRecipient r "
            + "where r.memberId = :memberId))")
    List<Long> findVisibleIds(@Param("status") NotificationStatus status,
                              @Param("broadcast") NotificationScope broadcast,
                              @Param("memberId") Long memberId);
}
