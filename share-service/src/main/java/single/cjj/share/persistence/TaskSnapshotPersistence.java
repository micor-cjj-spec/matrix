package single.cjj.share.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import single.cjj.share.model.Task;
import single.cjj.share.service.TaskService;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Durable transition layer for the existing shared-operation aggregate model.
 *
 * <p>The task domain remains in memory for the current service version, while a
 * MySQL snapshot is restored at startup and refreshed after mutations within the
 * configured flush interval. This removes restart data loss without changing the
 * public task API.</p>
 */
@Component
public class TaskSnapshotPersistence {

    private static final Logger log = LoggerFactory.getLogger(TaskSnapshotPersistence.class);
    private static final String SELECT_SQL = """
            SELECT payload_json
            FROM matrix_shared_task_snapshot
            ORDER BY created_at ASC
            """;
    private static final String UPSERT_SQL = """
            INSERT INTO matrix_shared_task_snapshot
                (task_id, payload_json, deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                payload_json = VALUES(payload_json),
                deleted = VALUES(deleted),
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectMapper snapshotReader;
    private final TaskService taskService;
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private volatile boolean initialized;
    private volatile boolean persistenceAvailable = true;

    public TaskSnapshotPersistence(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   TaskService taskService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.snapshotReader = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.taskService = taskService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreAfterStartup() {
        try {
            List<Task> persisted = jdbcTemplate.query(SELECT_SQL, (rs, rowNum) -> {
                try {
                    return snapshotReader.readValue(rs.getString("payload_json"), Task.class);
                } catch (JsonProcessingException ex) {
                    throw new IllegalStateException("Invalid shared task snapshot at row " + rowNum, ex);
                }
            });
            Map<String, Task> tasks = taskMap();
            if (!persisted.isEmpty()) {
                tasks.clear();
                persisted.forEach(task -> tasks.put(task.getId(), task));
                restoreSequences();
                log.info("Restored {} shared-operation tasks from MySQL", persisted.size());
            } else {
                initialized = true;
                flushNow();
                log.info("Initialized shared-operation task snapshots from seed data");
            }
            initialized = true;
        } catch (DataAccessException ex) {
            initialized = true;
            persistenceAvailable = false;
            log.error("Shared task persistence is unavailable. Apply shared_task_snapshot_v1.sql before production use.", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to restore shared-operation tasks", ex);
        }
    }

    @Scheduled(fixedDelayString = "${share.persistence.flush-ms:1000}")
    public void scheduledFlush() {
        if (!initialized || !persistenceAvailable) {
            return;
        }
        flushNow();
    }

    @PreDestroy
    public void flushBeforeShutdown() {
        if (initialized && persistenceAvailable) {
            flushNow();
        }
    }

    public void flushNow() {
        if (!persistenceAvailable || !flushing.compareAndSet(false, true)) {
            return;
        }
        try {
            List<Task> snapshot = new ArrayList<>(taskMap().values());
            for (Task task : snapshot) {
                jdbcTemplate.update(
                        UPSERT_SQL,
                        task.getId(),
                        writeJson(task),
                        task.isDeleted(),
                        Timestamp.from(task.getCreatedAt() == null ? Instant.now() : task.getCreatedAt())
                );
            }
        } catch (DataAccessException ex) {
            persistenceAvailable = false;
            log.error("Failed to flush shared-operation task snapshots; scheduled persistence has been disabled", ex);
        } finally {
            flushing.set(false);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Task> taskMap() {
        Field field = ReflectionUtils.findField(TaskService.class, "tasks");
        if (field == null) {
            throw new IllegalStateException("TaskService.tasks field is unavailable");
        }
        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, taskService);
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException("TaskService.tasks is not a Map");
        }
        return (Map<String, Task>) value;
    }

    private void restoreSequences() {
        long next = Math.max(System.currentTimeMillis(), 1_000_000L);
        setSequence("commentSequence", next);
        setSequence("eventSequence", next);
        setSequence("threadSequence", next);
    }

    private void setSequence(String fieldName, long value) {
        Field field = ReflectionUtils.findField(TaskService.class, fieldName);
        if (field == null) {
            return;
        }
        ReflectionUtils.makeAccessible(field);
        Object sequence = ReflectionUtils.getField(field, taskService);
        if (sequence instanceof AtomicLong atomicLong) {
            atomicLong.set(value);
        }
    }

    private String writeJson(Task task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize shared task " + task.getId(), ex);
        }
    }
}
