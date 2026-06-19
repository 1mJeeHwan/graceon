package org.streamhub.api.v1.logarchive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.actionlog.entity.ActionLog;
import org.streamhub.api.v1.actionlog.repository.ActionLogRepository;
import org.streamhub.api.v1.security.entity.SecurityEvent;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/**
 * Weekly housekeeping for audit/security logs: archives rows older than the retention window to
 * object storage as JSONL, then purges them from the database to keep table size bounded.
 *
 * <p>Best-effort by design — if the archive upload fails for a log family, that family's rows are
 * left in place (data preservation) and the failure is logged; the other family still proceeds.
 * The purge runs through {@link LogPurger} (a separate bean) so the bulk delete executes inside a
 * real transaction, and only after a successful upload.
 *
 * <p>Triggered by {@link LogArchiveScheduler} on a cron, or manually by a SYSTEM operator. The
 * retention window defaults to 7 days ({@code app.log.retention-days}).
 */
@Slf4j
@Service
public class LogArchiveService {

    private static final String ACTION_LOG_PREFIX = "logs/archive/action-log/";
    private static final String SECURITY_EVENT_PREFIX = "logs/archive/security-event/";
    private static final String NDJSON_CONTENT_TYPE = "application/x-ndjson";

    private final ActionLogRepository actionLogRepository;
    private final SecurityEventRepository securityEventRepository;
    private final StorageService storageService;
    private final LogPurger logPurger;
    private final ObjectMapper objectMapper;
    private final int retentionDays;

    public LogArchiveService(
            ActionLogRepository actionLogRepository,
            SecurityEventRepository securityEventRepository,
            StorageService storageService,
            LogPurger logPurger,
            ObjectMapper objectMapper,
            @Value("${app.log.retention-days:7}") int retentionDays) {
        this.actionLogRepository = actionLogRepository;
        this.securityEventRepository = securityEventRepository;
        this.storageService = storageService;
        this.logPurger = logPurger;
        this.objectMapper = objectMapper;
        this.retentionDays = retentionDays;
    }

    /**
     * Archives then purges both log families older than the retention cutoff.
     *
     * @return the per-family count of rows that were archived and purged
     */
    public ArchiveResult archiveAndPurge() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        String stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        int actionLogs = archiveActionLogs(cutoff, stamp);
        int securityEvents = archiveSecurityEvents(cutoff, stamp);

        return new ArchiveResult(actionLogs, securityEvents);
    }

    private int archiveActionLogs(LocalDateTime cutoff, String stamp) {
        List<ActionLog> rows = actionLogRepository.findByCreatedAtBefore(cutoff);
        if (rows.isEmpty()) {
            return 0;
        }
        try {
            byte[] body = toJsonl(rows);
            storageService.putBytes(ACTION_LOG_PREFIX + stamp + ".jsonl", body, NDJSON_CONTENT_TYPE);
        } catch (RuntimeException e) {
            log.warn("Action-log archive upload failed; skipping purge to preserve data: {}",
                    e.getMessage());
            return 0;
        }
        int purged = logPurger.purgeActionLogs(cutoff);
        log.info("Archived and purged {} action-log row(s) older than {}.", purged, cutoff);
        return purged;
    }

    private int archiveSecurityEvents(LocalDateTime cutoff, String stamp) {
        List<SecurityEvent> rows = securityEventRepository.findByCreatedAtBefore(cutoff);
        if (rows.isEmpty()) {
            return 0;
        }
        try {
            byte[] body = toJsonl(rows);
            storageService.putBytes(SECURITY_EVENT_PREFIX + stamp + ".jsonl", body, NDJSON_CONTENT_TYPE);
        } catch (RuntimeException e) {
            log.warn("Security-event archive upload failed; skipping purge to preserve data: {}",
                    e.getMessage());
            return 0;
        }
        int count = rows.size();
        logPurger.purgeSecurityEvents(cutoff);
        log.info("Archived and purged {} security-event row(s) older than {}.", count, cutoff);
        return count;
    }

    /** Serializes records to JSONL (one JSON object per line). */
    private byte[] toJsonl(List<?> records) {
        StringBuilder sb = new StringBuilder();
        for (Object record : records) {
            try {
                sb.append(objectMapper.writeValueAsString(record)).append('\n');
            } catch (JsonProcessingException e) {
                // Skip an unserializable row rather than aborting the whole archive batch.
                log.warn("Skipping unserializable log record during archive: {}", e.getMessage());
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Per-family count of rows archived and purged in a single run. */
    public record ArchiveResult(int actionLogs, int securityEvents) {
    }
}
