package org.streamhub.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The notification service's read API — the dispatch log is owned here (DB-per-service). An internal
 * service endpoint reached server-to-server (or via the API gateway); operator authorization stays
 * at the monolith / gateway boundary.
 */
@RestController
public class NotificationDispatchQueryController {

    private final NotificationDispatchQueryService queryService;

    public NotificationDispatchQueryController(NotificationDispatchQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/v1/notification-dispatches")
    public NotificationDispatchPage list(@RequestParam(required = false) Integer pageNumber,
                                         @RequestParam(required = false) Integer pageSize,
                                         @RequestParam(required = false) String channel,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        return queryService.list(pageNumber, pageSize, channel, status, keyword);
    }
}
