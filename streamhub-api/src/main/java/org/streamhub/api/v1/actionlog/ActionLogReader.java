package org.streamhub.api.v1.actionlog;

import org.streamhub.api.base.response.ResCursorList;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.v1.actionlog.dto.ActionLogCursorRequest;
import org.streamhub.api.v1.actionlog.dto.ActionLogItem;
import org.streamhub.api.v1.actionlog.dto.ActionLogSearchRequest;

/**
 * Read seam for the audit log, selected by {@code app.actionlog.source}:
 *
 * <ul>
 *   <li>{@link LocalActionLogReader} (default, {@code local}) — reads {@code ACTION_LOG} from the
 *       monolith's own DB via MyBatis. Used when this app consumes its own audit events.</li>
 *   <li>{@link RemoteActionLogReader} ({@code remote}) — calls the extracted
 *       {@code streamhub-audit-service}'s {@code /v1/action-logs} read API, which owns the audit data
 *       in its own schema (DB-per-service). Used in the MSA split. See docs/msa-split.md.</li>
 * </ul>
 *
 * <p>The {@link ActionLogController} depends only on this interface — the data's location is a
 * config decision, not a call-site one.
 */
public interface ActionLogReader {

    /** Returns a filtered, paginated page of audit-log rows (offset paging, with a total count). */
    ResInfinityList<ActionLogItem> list(ActionLogSearchRequest request);

    /**
     * Returns the page that follows the given cursor (keyset paging, no count). Used by the audit
     * screen's "더 보기", where pages get deep and rows arrive while the operator reads.
     */
    ResCursorList<ActionLogItem> listAfter(ActionLogCursorRequest request);
}
