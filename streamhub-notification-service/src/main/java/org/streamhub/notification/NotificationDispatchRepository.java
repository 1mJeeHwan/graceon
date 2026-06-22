package org.streamhub.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Store for {@link NotificationDispatch} — backs the Kafka consumer (write) and the
 * {@code /v1/notification-dispatches} read API (search).
 */
public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    /**
     * Filtered, paged dispatch search. {@code channel}/{@code status} are exact matches; {@code keyword}
     * is a LIKE over title / content / masked target. Null filters are ignored. Newest first.
     */
    @Query(value = "select d from NotificationDispatch d where "
            + "(:channel is null or d.channel = :channel) and "
            + "(:status is null or d.status = :status) and "
            + "(:keyword is null or d.title like concat('%', :keyword, '%') "
            + "or d.content like concat('%', :keyword, '%') "
            + "or d.targetMasked like concat('%', :keyword, '%')) "
            + "order by d.createdAt desc, d.id desc",
            countQuery = "select count(d) from NotificationDispatch d where "
            + "(:channel is null or d.channel = :channel) and "
            + "(:status is null or d.status = :status) and "
            + "(:keyword is null or d.title like concat('%', :keyword, '%') "
            + "or d.content like concat('%', :keyword, '%') "
            + "or d.targetMasked like concat('%', :keyword, '%'))")
    Page<NotificationDispatch> search(@Param("channel") String channel,
                                      @Param("status") String status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);
}
