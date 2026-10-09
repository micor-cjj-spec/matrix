package single.cjj.im.reliability;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ImOperationsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ImOperationsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findStaleOutboxIds(LocalDateTime deadline, int limit) {
        return jdbc.queryForList("""
                SELECT id
                FROM im_outbox_event
                WHERE status='PROCESSING' AND updated_time<:deadline
                ORDER BY updated_time,id
                LIMIT :limit
                """, Map.of("deadline", deadline, "limit", limit), String.class);
    }

    public int recoverStaleOutbox(String id, LocalDateTime nextRetryTime, LocalDateTime now) {
        return jdbc.update("""
                UPDATE im_outbox_event
                SET status='RETRYING',
                    retry_count=retry_count+1,
                    next_retry_time=:nextRetryTime,
                    last_error='OUTBOX_PROCESSING_TIMEOUT',
                    processing_started_time=NULL,
                    updated_time=:now
                WHERE id=:id AND status='PROCESSING'
                """, Map.of("id", id, "nextRetryTime", nextRetryTime, "now", now));
    }

    public void insertDeadLetter(String id,
                                 String queueName,
                                 String messageId,
                                 String payload,
                                 String reason,
                                 LocalDateTime now) {
        jdbc.update("""
                INSERT INTO im_dead_letter (
                    id,queue_name,message_id,payload_json,reason,status,created_time,updated_time
                ) VALUES (
                    :id,:queueName,:messageId,:payload,:reason,'PENDING',:now,:now
                )
                ON DUPLICATE KEY UPDATE
                    reason=VALUES(reason),payload_json=VALUES(payload_json),updated_time=VALUES(updated_time)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("queueName", queueName)
                .addValue("messageId", messageId)
                .addValue("payload", payload)
                .addValue("reason", truncate(reason, 1000))
                .addValue("now", now));
    }

    public List<String> findAggregateMismatchMessageIds(int limit) {
        return jdbc.queryForList("""
                SELECT m.id
                FROM im_message_task m
                WHERE m.status IN ('ACCEPTED','PROCESSING')
                   OR m.success_channels <> (
                       SELECT COUNT(*) FROM im_channel_task c
                       WHERE c.message_id=m.id AND c.status='SUCCESS'
                   )
                   OR m.failed_channels <> (
                       SELECT COUNT(*) FROM im_channel_task c
                       WHERE c.message_id=m.id
                         AND c.status IN ('FAILED','DEAD','UNKNOWN','EXPIRED','CANCELLED')
                   )
                ORDER BY m.updated_time,m.id
                LIMIT :limit
                """, Map.of("limit", limit), String.class);
    }

    public List<String> findMissingLocalNotificationTaskIds(int limit) {
        return jdbc.queryForList("""
                SELECT c.id
                FROM im_channel_task c
                JOIN im_message_recipient r ON r.id=c.recipient_id
                WHERE c.channel_type='LOCAL'
                  AND c.status='SUCCESS'
                  AND NOT EXISTS (
                      SELECT 1 FROM im_local_notification n
                      WHERE n.message_id=c.message_id AND n.user_id=r.receiver_id
                  )
                ORDER BY c.updated_time,c.id
                LIMIT :limit
                """, Map.of("limit", limit), String.class);
    }

    public int requeueMissingLocalTask(String channelTaskId, LocalDateTime now) {
        return jdbc.update("""
                UPDATE im_channel_task
                SET status='RETRYING',
                    next_retry_time=:now,
                    last_error_code='LOCAL_NOTIFICATION_MISSING',
                    last_error_message='渠道成功但站内消息缺失，已由对账任务重新投递',
                    updated_time=:now
                WHERE id=:id AND channel_type='LOCAL' AND status='SUCCESS'
                """, Map.of("id", channelTaskId, "now", now));
    }

    public void insertRecoveryOutbox(String id,
                                     String eventId,
                                     String channelTaskId,
                                     String payload,
                                     LocalDateTime now) {
        jdbc.update("""
                INSERT IGNORE INTO im_outbox_event (
                    id,event_id,aggregate_type,aggregate_id,event_type,payload_json,status,retry_count,
                    next_retry_time,last_error,created_time,published_time,processing_started_time,updated_time
                ) VALUES (
                    :id,:eventId,'CHANNEL_TASK',:channelTaskId,'CHANNEL_TASK_RECONCILED',:payload,
                    'PENDING',0,:now,NULL,:now,NULL,NULL,:now
                )
                """, Map.of(
                "id", id,
                "eventId", eventId,
                "channelTaskId", channelTaskId,
                "payload", payload,
                "now", now
        ));
    }

    public List<FinalMessageCandidate> findFinalMessagesMissingCallbacks(int limit) {
        return jdbc.query("""
                SELECT m.id,m.message_no,m.app_code,m.request_id,m.status,m.callback_url,m.updated_time
                FROM im_message_task m
                WHERE m.callback_url IS NOT NULL
                  AND m.callback_url<>''
                  AND m.status IN ('SUCCESS','PARTIAL_SUCCESS','FAILED','UNKNOWN','EXPIRED')
                  AND NOT EXISTS (
                      SELECT 1 FROM im_callback_task c
                      WHERE c.message_id=m.id AND c.message_status=m.status
                  )
                ORDER BY m.updated_time,m.id
                LIMIT :limit
                """, Map.of("limit", limit), (rs, rowNum) -> new FinalMessageCandidate(
                rs.getString("id"),
                rs.getString("message_no"),
                rs.getString("app_code"),
                rs.getString("request_id"),
                rs.getString("status"),
                rs.getString("callback_url"),
                rs.getTimestamp("updated_time").toLocalDateTime()
        ));
    }

    public int insertCallbackTask(CallbackTaskRecord task) {
        return jdbc.update("""
                INSERT IGNORE INTO im_callback_task (
                    id,event_id,message_id,message_status,app_code,callback_url,callback_secret_ciphertext,
                    payload_json,status,retry_count,max_retry_count,next_retry_time,processing_started_time,
                    response_code,response_body,last_error,created_time,updated_time
                ) VALUES (
                    :id,:eventId,:messageId,:messageStatus,:appCode,:callbackUrl,:callbackSecretCiphertext,
                    :payloadJson,:status,:retryCount,:maxRetryCount,:nextRetryTime,:processingStartedTime,
                    :responseCode,:responseBody,:lastError,:createdTime,:updatedTime
                )
                """, beanParams(task));
    }

    public List<CallbackTaskRecord> findDueCallbackTasks(LocalDateTime now, int limit) {
        return jdbc.query("""
                SELECT *
                FROM im_callback_task
                WHERE status IN ('PENDING','RETRYING')
                  AND (next_retry_time IS NULL OR next_retry_time<=:now)
                ORDER BY created_time,id
                LIMIT :limit
                """, Map.of("now", now, "limit", limit), (rs, rowNum) -> new CallbackTaskRecord(
                rs.getString("id"),
                rs.getString("event_id"),
                rs.getString("message_id"),
                rs.getString("message_status"),
                rs.getString("app_code"),
                rs.getString("callback_url"),
                rs.getString("callback_secret_ciphertext"),
                rs.getString("payload_json"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                rs.getInt("max_retry_count"),
                timestamp(rs.getTimestamp("next_retry_time")),
                timestamp(rs.getTimestamp("processing_started_time")),
                (Integer) rs.getObject("response_code"),
                rs.getString("response_body"),
                rs.getString("last_error"),
                rs.getTimestamp("created_time").toLocalDateTime(),
                rs.getTimestamp("updated_time").toLocalDateTime()
        ));
    }

    public int claimCallback(String id, LocalDateTime now) {
        return jdbc.update("""
                UPDATE im_callback_task
                SET status='PROCESSING',processing_started_time=:now,updated_time=:now
                WHERE id=:id AND status IN ('PENDING','RETRYING')
                  AND (next_retry_time IS NULL OR next_retry_time<=:now)
                """, Map.of("id", id, "now", now));
    }

    public void markCallbackSuccess(String id, int responseCode, String responseBody, LocalDateTime now) {
        jdbc.update("""
                UPDATE im_callback_task
                SET status='SUCCESS',response_code=:responseCode,response_body=:responseBody,
                    last_error=NULL,processing_started_time=NULL,updated_time=:now
                WHERE id=:id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("responseCode", responseCode)
                .addValue("responseBody", truncate(responseBody, 2000))
                .addValue("now", now));
    }

    public void markCallbackRetry(String id,
                                  int retryCount,
                                  LocalDateTime nextRetryTime,
                                  Integer responseCode,
                                  String responseBody,
                                  String error,
                                  LocalDateTime now) {
        jdbc.update("""
                UPDATE im_callback_task
                SET status='RETRYING',retry_count=:retryCount,next_retry_time=:nextRetryTime,
                    response_code=:responseCode,response_body=:responseBody,last_error=:error,
                    processing_started_time=NULL,updated_time=:now
                WHERE id=:id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("nextRetryTime", nextRetryTime)
                .addValue("responseCode", responseCode)
                .addValue("responseBody", truncate(responseBody, 2000))
                .addValue("error", truncate(error, 1000))
                .addValue("now", now));
    }

    public void markCallbackDead(String id,
                                 int retryCount,
                                 Integer responseCode,
                                 String responseBody,
                                 String error,
                                 LocalDateTime now) {
        jdbc.update("""
                UPDATE im_callback_task
                SET status='DEAD',retry_count=:retryCount,next_retry_time=NULL,
                    response_code=:responseCode,response_body=:responseBody,last_error=:error,
                    processing_started_time=NULL,updated_time=:now
                WHERE id=:id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("responseCode", responseCode)
                .addValue("responseBody", truncate(responseBody, 2000))
                .addValue("error", truncate(error, 1000))
                .addValue("now", now));
    }

    public List<String> findStaleCallbackIds(LocalDateTime deadline, int limit) {
        return jdbc.queryForList("""
                SELECT id FROM im_callback_task
                WHERE status='PROCESSING' AND processing_started_time<:deadline
                ORDER BY processing_started_time,id
                LIMIT :limit
                """, Map.of("deadline", deadline, "limit", limit), String.class);
    }

    public int recoverStaleCallback(String id, LocalDateTime nextRetryTime, LocalDateTime now) {
        return jdbc.update("""
                UPDATE im_callback_task
                SET status='RETRYING',retry_count=retry_count+1,next_retry_time=:nextRetryTime,
                    last_error='CALLBACK_PROCESSING_TIMEOUT',processing_started_time=NULL,updated_time=:now
                WHERE id=:id AND status='PROCESSING'
                """, Map.of("id", id, "nextRetryTime", nextRetryTime, "now", now));
    }

    public void updateMessageCallbackStatus(String messageId, String callbackStatus, LocalDateTime now) {
        jdbc.update("""
                UPDATE im_message_task
                SET callback_status=:callbackStatus,updated_time=:now
                WHERE id=:messageId
                """, Map.of("messageId", messageId, "callbackStatus", callbackStatus, "now", now));
    }

    private MapSqlParameterSource beanParams(CallbackTaskRecord task) {
        return new MapSqlParameterSource()
                .addValue("id", task.id())
                .addValue("eventId", task.eventId())
                .addValue("messageId", task.messageId())
                .addValue("messageStatus", task.messageStatus())
                .addValue("appCode", task.appCode())
                .addValue("callbackUrl", task.callbackUrl())
                .addValue("callbackSecretCiphertext", task.callbackSecretCiphertext())
                .addValue("payloadJson", task.payloadJson())
                .addValue("status", task.status())
                .addValue("retryCount", task.retryCount())
                .addValue("maxRetryCount", task.maxRetryCount())
                .addValue("nextRetryTime", task.nextRetryTime())
                .addValue("processingStartedTime", task.processingStartedTime())
                .addValue("responseCode", task.responseCode())
                .addValue("responseBody", task.responseBody())
                .addValue("lastError", task.lastError())
                .addValue("createdTime", task.createdTime())
                .addValue("updatedTime", task.updatedTime());
    }

    private LocalDateTime timestamp(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record FinalMessageCandidate(
            String messageId,
            String messageNo,
            String appCode,
            String requestId,
            String messageStatus,
            String callbackUrl,
            LocalDateTime occurredTime
    ) {
    }

    public record CallbackTaskRecord(
            String id,
            String eventId,
            String messageId,
            String messageStatus,
            String appCode,
            String callbackUrl,
            String callbackSecretCiphertext,
            String payloadJson,
            String status,
            int retryCount,
            int maxRetryCount,
            LocalDateTime nextRetryTime,
            LocalDateTime processingStartedTime,
            Integer responseCode,
            String responseBody,
            String lastError,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }
}
