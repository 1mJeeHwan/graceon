package org.streamhub.api.v1.pub.me.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.jwt.MemberTokenResolver;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.me.notification.dto.NotificationItem;

/**
 * Member notification-center endpoints under the public ({@code /pub/**}, permitAll) namespace.
 * The member is resolved from the Bearer member token via the shared {@link MemberTokenResolver}
 * (a missing/invalid member token is a 401).
 *
 * <p><b>Model limitation.</b> The backing {@code NOTIFICATION_LOG} is a broadcast send-log with no
 * member targeting and no per-member read state. The feed is therefore the most recent
 * successfully-sent broadcasts (shared by all members), {@code read} is always {@code false}, and
 * the read endpoint is a safe no-op. See {@link MemberNotificationService} for details.
 */
@Tag(name = "Member Notifications", description = "사용자 사이트 알림센터 (회원): 최근 알림 목록 / 읽음")
@RestController
@RequestMapping("/pub/v1/me/notifications")
public class MemberNotificationController {

    private final MemberNotificationService notificationService;
    private final MemberTokenResolver memberTokenResolver;

    public MemberNotificationController(MemberNotificationService notificationService,
                                        MemberTokenResolver memberTokenResolver) {
        this.notificationService = notificationService;
        this.memberTokenResolver = memberTokenResolver;
    }

    @Operation(summary = "내 알림 목록",
            description = "최근 발송된 알림을 최신순으로 페이징해 반환한다. 알림은 브로드캐스트 로그라 회원별 스코프/읽음상태가 없어 read는 항상 false다.")
    @GetMapping
    public ResultDTO<ResInfinityList<NotificationItem>> notifications(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Long memberId = memberTokenResolver.resolve(authorization);
        return ResultDTO.ok(notificationService.notifications(memberId, pageNumber, pageSize));
    }

    @Operation(summary = "알림 읽음",
            description = "알림 1건을 읽음 처리한다. 브로드캐스트 로그라 회원별 읽음상태를 저장할 모델이 없어 안전한 no-op로 성공을 반환한다.")
    @PostMapping("/{id}/read")
    public ResultDTO<Void> markRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        Long memberId = memberTokenResolver.resolve(authorization);
        notificationService.markRead(memberId, id);
        return ResultDTO.ok();
    }
}
