package org.streamhub.api.v1.logarchive;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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

    /** Rows serialized and uploaded per S3 object. Bounds peak heap regardless of backlog size. */
    private static final int ARCHIVE_CHUNK_SIZE = 5_000;

    /**
     * Runaway guard. At {@value #ARCHIVE_CHUNK_SIZE} rows per part this still covers a million rows
     * in one run; hitting it means something is very wrong, so the run reports failure and leaves
     * the source data alone rather than purging a partial archive.
     */
    private static final int MAX_ARCHIVE_PARTS = 200;

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
        int uploaded = uploadInChunks(ACTION_LOG_PREFIX, stamp, page ->
                actionLogRepository.findByCreatedAtBeforeOrderByIdAsc(cutoff, page));
        if (uploaded <= 0) {
            return 0;
        }
        int purged = logPurger.purgeActionLogs(cutoff);
        log.info("Archived and purged {} action-log row(s) older than {}.", purged, cutoff);
        return purged;
    }

    private int archiveSecurityEvents(LocalDateTime cutoff, String stamp) {
        int uploaded = uploadInChunks(SECURITY_EVENT_PREFIX, stamp, page ->
                securityEventRepository.findByCreatedAtBeforeOrderByIdAsc(cutoff, page));
        if (uploaded <= 0) {
            return 0;
        }
        logPurger.purgeSecurityEvents(cutoff);
        log.info("Archived and purged {} security-event row(s) older than {}.", uploaded, cutoff);
        return uploaded;
    }

    /**
     * Pages through the matching rows, uploading one JSONL object per chunk, and returns how many
     * rows were archived (0 when there was nothing to do, -1 when an upload failed).
     *
     * <p>Chunking is the whole point: the previous version materialised every row, concatenated it
     * into one {@link StringBuilder}, and converted that to a single {@code byte[]} — three copies
     * of the entire backlog resident at once, on a 1 GB instance, for a table that grows without
     * bound between runs. Failure still means "do not purge", so a partial upload leaves the source
     * rows intact and the next run redoes them.
     */
    private int uploadInChunks(String prefix, String stamp,
                               java.util.function.Function<Pageable, List<?>> fetch) {
        int total = 0;
        int part = 0;
        while (true) {
            // Offset paging is correct here precisely because nothing is deleted mid-loop: the
            // candidate set is fixed by the cutoff and ordered by id, so page N+1 continues where
            // page N stopped. The purge runs once, at the end, after every part is safely uploaded.
            List<?> chunk = fetch.apply(PageRequest.of(part, ARCHIVE_CHUNK_SIZE));
            if (chunk.isEmpty()) {
                break;
            }
            String key = prefix + stamp + (part == 0 ? "" : "-" + part) + ".jsonl";
            try {
                storageService.putBytes(key, toJsonl(chunk), NDJSON_CONTENT_TYPE);
            } catch (RuntimeException e) {
                log.warn("Archive upload failed at {}; skipping purge to preserve data: {}",
                        key, e.getMessage());
                return -1;
            }
            total += chunk.size();
            part++;
            if (chunk.size() < ARCHIVE_CHUNK_SIZE) {
                break;
            }
            if (part >= MAX_ARCHIVE_PARTS) {
                log.warn("Archive hit the {}-part cap at {} rows; the remainder is left for the "
                        + "next run and nothing is purged.", MAX_ARCHIVE_PARTS, total);
                return -1;
            }
        }
        return total;
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
