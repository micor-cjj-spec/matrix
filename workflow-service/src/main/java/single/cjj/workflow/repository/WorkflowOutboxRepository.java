package single.cjj.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class WorkflowOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OutboxRow> findDispatchable(int limit) {
        return jdbcTemplate.query("""
                SELECT id, event_id, instance_id, event_type, payload_json,
                       status, retry_count, next_retry_time, created_at
                FROM wf_event_outbox
                WHERE status IN ('PENDING', 'FAILED')
                  AND (next_retry_time IS NULL OR next_retry_time <= NOW())
                ORDER BY created_at
                LIMIT ?
                """, this::mapRow, limit);
    }

    public boolean claim(String id) {
        return jdbcTemplate.update("""
                UPDATE wf_event_outbox
                SET status = 'SENDING'
                WHERE id = ?
                  AND status IN ('PENDING', 'FAILED')
                  AND (next_retry_time IS NULL OR next_retry_time <= NOW())
                """, id) == 1;
    }

    public void markSent(String id) {
        jdbcTemplate.update("""
                UPDATE wf_event_outbox
                SET status = 'SUCCESS', sent_at = NOW(), next_retry_time = NULL
                WHERE id = ? AND status = 'SENDING'
                """, id);
    }

    public void markFailed(String id, LocalDateTime nextRetryTime) {
        jdbcTemplate.update("""
                UPDATE wf_event_outbox
                SET status = 'FAILED', retry_count = retry_count + 1,
                    next_retry_time = ?
                WHERE id = ? AND status = 'SENDING'
                """, nextRetryTime, id);
    }

    private OutboxRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxRow(
                rs.getString("id"),
                rs.getString("event_id"),
                rs.getString("instance_id"),
                rs.getString("event_type"),
                rs.getString("payload_json"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                rs.getObject("next_retry_time", LocalDateTime.class),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    public record OutboxRow(
            String id,
            String eventId,
            String instanceId,
            String eventType,
            String payloadJson,
            String status,
            int retryCount,
            LocalDateTime nextRetryTime,
            LocalDateTime createdAt
    ) {
    }
}
