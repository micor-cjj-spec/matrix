package single.cjj.im.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import single.cjj.im.domain.ImModels.ChannelStatusResponse;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.OutboxRecord;
import single.cjj.im.domain.ImModels.RecipientRecord;
import single.cjj.im.domain.ImModels.TemplateRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ImMessageRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ImMessageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<MessageRecord> findMessageByAppRequest(String appCode, String requestId) {
        String sql = "SELECT * FROM im_message_task WHERE app_code=:appCode AND request_id=:requestId LIMIT 1";
        return queryOne(sql, Map.of("appCode", appCode, "requestId", requestId), MessageRecord.class);
    }

    public Optional<MessageRecord> findMessageByNo(String messageNo) {
        String sql = "SELECT * FROM im_message_task WHERE message_no=:messageNo LIMIT 1";
        return queryOne(sql, Map.of("messageNo", messageNo), MessageRecord.class);
    }

    public Optional<MessageRecord> findMessageById(String id) {
        return queryOne("SELECT * FROM im_message_task WHERE id=:id LIMIT 1", Map.of("id", id), MessageRecord.class);
    }

    public void insertMessage(MessageRecord record) {
        String sql = """
                INSERT INTO im_message_task (
                    id,message_no,tenant_id,app_code,request_id,message_type,template_code,title,content,
                    priority,scheduled_time,expire_time,business_type,business_id,action_url,status,
                    total_channels,success_channels,failed_channels,callback_url,callback_status,
                    created_time,updated_time
                ) VALUES (
                    :id,:messageNo,:tenantId,:appCode,:requestId,:messageType,:templateCode,:title,:content,
                    :priority,:scheduledTime,:expireTime,:businessType,:businessId,:actionUrl,:status,
                    :totalChannels,:successChannels,:failedChannels,:callbackUrl,:callbackStatus,
                    :createdTime,:updatedTime
                )
                """;
        jdbc.update(sql, beanParams(record));
    }

    public void insertRecipient(RecipientRecord record) {
        String sql = """
                INSERT INTO im_message_recipient (
                    id,message_id,receiver_type,receiver_id,receiver_name,email,read_status,read_time,created_time
                ) VALUES (
                    :id,:messageId,:receiverType,:receiverId,:receiverName,:email,:readStatus,:readTime,:createdTime
                )
                """;
        jdbc.update(sql, beanParams(record));
    }

    public void insertChannelTask(ChannelTaskRecord record) {
        String sql = """
                INSERT INTO im_channel_task (
                    id,message_id,recipient_id,channel_type,subject,content,status,retry_count,max_retry_count,
                    next_retry_time,provider_code,provider_message_id,last_error_code,last_error_message,
                    sent_time,delivered_time,processing_started_time,created_time,updated_time
                ) VALUES (
                    :id,:messageId,:recipientId,:channelType,:subject,:content,:status,:retryCount,:maxRetryCount,
                    :nextRetryTime,:providerCode,:providerMessageId,:lastErrorCode,:lastErrorMessage,
                    :sentTime,:deliveredTime,:processingStartedTime,:createdTime,:updatedTime
                )
                """;
        jdbc.update(sql, beanParams(record));
    }

    public void insertOutbox(OutboxRecord record) {
        String sql = """
                INSERT INTO im_outbox_event (
                    id,event_id,aggregate_type,aggregate_id,event_type,payload_json,status,retry_count,
                    next_retry_time,last_error,created_time,published_time
                ) VALUES (
                    :id,:eventId,:aggregateType,:aggregateId,:eventType,:payloadJson,:status,:retryCount,
                    :nextRetryTime,:lastError,:createdTime,:publishedTime
                )
                """;
        jdbc.update(sql, beanParams(record));
    }

    public Optional<RecipientRecord> findRecipient(String id) {
        return queryOne("SELECT * FROM im_message_recipient WHERE id=:id LIMIT 1", Map.of("id", id), RecipientRecord.class);
    }

    public Optional<ChannelTaskRecord> findChannelTask(String id) {
        return queryOne("SELECT * FROM im_channel_task WHERE id=:id LIMIT 1", Map.of("id", id), ChannelTaskRecord.class);
    }

    public List<ChannelStatusResponse> findChannelStatusResponses(String messageId) {
        String sql = """
                SELECT c.id AS channel_task_id,
                       r.receiver_id AS recipient_id,
                       c.channel_type AS channel,
                       c.status,
                       c.retry_count,
                       c.provider_message_id,
                       c.last_error_code AS error_code,
                       c.last_error_message AS error_message,
                       c.sent_time,
                       c.delivered_time
                FROM im_channel_task c
                JOIN im_message_recipient r ON r.id=c.recipient_id
                WHERE c.message_id=:messageId
                ORDER BY c.created_time,c.id
                """;
        return jdbc.query(sql, Map.of("messageId", messageId), DataClassRowMapper.newInstance(ChannelStatusResponse.class));
    }

    public List<String> findChannelStatuses(String messageId) {
        return jdbc.queryForList(
                "SELECT status FROM im_channel_task WHERE message_id=:messageId",
                Map.of("messageId", messageId),
                String.class
        );
    }

    public int claimChannelTask(String id, LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='PROCESSING',processing_started_time=:now,updated_time=:now
                WHERE id=:id AND status IN ('PENDING','RETRYING')
                  AND (next_retry_time IS NULL OR next_retry_time<=:now)
                """;
        return jdbc.update(sql, Map.of("id", id, "now", now));
    }

    public void markChannelSuccess(String id, String providerMessageId, LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='SUCCESS',provider_message_id=:providerMessageId,sent_time=:now,
                    delivered_time=NULL,last_error_code=NULL,last_error_message=NULL,updated_time=:now
                WHERE id=:id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("providerMessageId", providerMessageId)
                .addValue("now", now);
        jdbc.update(sql, params);
    }

    public void markChannelRetry(String id,
                                 int retryCount,
                                 LocalDateTime nextRetryTime,
                                 String errorCode,
                                 String errorMessage,
                                 LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='RETRYING',retry_count=:retryCount,next_retry_time=:nextRetryTime,
                    last_error_code=:errorCode,last_error_message=:errorMessage,updated_time=:now
                WHERE id=:id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("nextRetryTime", nextRetryTime)
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", truncate(errorMessage, 1000))
                .addValue("now", now));
    }

    public void markChannelDead(String id,
                                int retryCount,
                                String errorCode,
                                String errorMessage,
                                LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='DEAD',retry_count=:retryCount,last_error_code=:errorCode,
                    last_error_message=:errorMessage,updated_time=:now
                WHERE id=:id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", truncate(errorMessage, 1000))
                .addValue("now", now));
    }

    public void markChannelExpired(String id, LocalDateTime now) {
        jdbc.update(
                "UPDATE im_channel_task SET status='EXPIRED',updated_time=:now WHERE id=:id",
                Map.of("id", id, "now", now)
        );
    }

    public List<ChannelTaskRecord> findStaleChannelTasks(String channelType, LocalDateTime deadline, int limit) {
        String sql = """
                SELECT * FROM im_channel_task
                WHERE channel_type=:channelType AND status='PROCESSING' AND processing_started_time<:deadline
                ORDER BY processing_started_time,id
                LIMIT :limit
                """;
        return jdbc.query(sql, Map.of("channelType", channelType, "deadline", deadline, "limit", limit),
                DataClassRowMapper.newInstance(ChannelTaskRecord.class));
    }

    public int recoverStaleLocalTask(String id, int retryCount, LocalDateTime nextRetryTime, LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='RETRYING',retry_count=:retryCount,next_retry_time=:nextRetryTime,last_error_code='PROCESSING_TIMEOUT',
                    last_error_message='本地提醒执行超时，已进入安全重试',updated_time=:now
                WHERE id=:id AND channel_type='LOCAL' AND status='PROCESSING'
                """;
        return jdbc.update(sql, Map.of("id", id, "retryCount", retryCount, "nextRetryTime", nextRetryTime, "now", now));
    }

    public int markStaleEmailTaskUnknown(String id, LocalDateTime now) {
        String sql = """
                UPDATE im_channel_task
                SET status='UNKNOWN',last_error_code='DELIVERY_RESULT_UNKNOWN',
                    last_error_message='邮件发送过程异常中断，发送结果不确定，请人工核对',updated_time=:now
                WHERE id=:id AND channel_type='EMAIL' AND status='PROCESSING'
                """;
        return jdbc.update(sql, Map.of("id", id, "now", now));
    }

    public void updateMessageAggregate(String messageId,
                                       String status,
                                       int successChannels,
                                       int failedChannels,
                                       LocalDateTime now) {
        String sql = """
                UPDATE im_message_task
                SET status=:status,success_channels=:successChannels,failed_channels=:failedChannels,updated_time=:now
                WHERE id=:messageId
                """;
        jdbc.update(sql, Map.of(
                "messageId", messageId,
                "status", status,
                "successChannels", successChannels,
                "failedChannels", failedChannels,
                "now", now
        ));
    }

    public List<OutboxRecord> findDueOutbox(LocalDateTime now, int limit) {
        String sql = """
                SELECT * FROM im_outbox_event
                WHERE status IN ('PENDING','RETRYING')
                  AND (next_retry_time IS NULL OR next_retry_time<=:now)
                ORDER BY created_time,id
                LIMIT :limit
                """;
        return jdbc.query(sql, Map.of("now", now, "limit", limit), DataClassRowMapper.newInstance(OutboxRecord.class));
    }

    public int claimOutbox(String id) {
        String sql = """
                UPDATE im_outbox_event
                SET status='PROCESSING'
                WHERE id=:id AND status IN ('PENDING','RETRYING')
                """;
        return jdbc.update(sql, Map.of("id", id));
    }

    public void markOutboxPublished(String id, LocalDateTime now) {
        jdbc.update(
                "UPDATE im_outbox_event SET status='PUBLISHED',published_time=:now,last_error=NULL WHERE id=:id",
                Map.of("id", id, "now", now)
        );
    }

    public void markOutboxRetry(String id, int retryCount, LocalDateTime nextRetryTime, String error) {
        String sql = """
                UPDATE im_outbox_event
                SET status='RETRYING',retry_count=:retryCount,next_retry_time=:nextRetryTime,last_error=:error
                WHERE id=:id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("nextRetryTime", nextRetryTime)
                .addValue("error", truncate(error, 1000)));
    }

    public void markOutboxDead(String id, int retryCount, String error) {
        String sql = """
                UPDATE im_outbox_event
                SET status='DEAD',retry_count=:retryCount,last_error=:error
                WHERE id=:id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("retryCount", retryCount)
                .addValue("error", truncate(error, 1000)));
    }

    public int insertLocalNotificationIfAbsent(LocalNotificationRecord record) {
        String sql = """
                INSERT IGNORE INTO im_local_notification (
                    id,message_id,recipient_id,user_id,title,content,message_type,business_type,business_id,
                    action_url,push_status,read_status,read_time,created_time
                ) VALUES (
                    :id,:messageId,:recipientId,:userId,:title,:content,:messageType,:businessType,:businessId,
                    :actionUrl,:pushStatus,:readStatus,:readTime,:createdTime
                )
                """;
        return jdbc.update(sql, beanParams(record));
    }

    public long countLocalNotifications(String userId, String readStatus) {
        String sql = "SELECT COUNT(*) FROM im_local_notification WHERE user_id=:userId"
                + (readStatus == null ? "" : " AND read_status=:readStatus");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        if (readStatus != null) {
            params.addValue("readStatus", readStatus);
        }
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    public List<LocalNotificationRecord> listLocalNotifications(String userId,
                                                                 String readStatus,
                                                                 int offset,
                                                                 int size) {
        String sql = "SELECT * FROM im_local_notification WHERE user_id=:userId"
                + (readStatus == null ? "" : " AND read_status=:readStatus")
                + " ORDER BY created_time DESC,id DESC LIMIT :size OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("size", size)
                .addValue("offset", offset);
        if (readStatus != null) {
            params.addValue("readStatus", readStatus);
        }
        return jdbc.query(sql, params, DataClassRowMapper.newInstance(LocalNotificationRecord.class));
    }

    public int markNotificationRead(String notificationId, String userId, LocalDateTime now) {
        String sql = """
                UPDATE im_local_notification
                SET read_status='READ',read_time=:now
                WHERE id=:notificationId AND user_id=:userId AND read_status='UNREAD'
                """;
        return jdbc.update(sql, Map.of("notificationId", notificationId, "userId", userId, "now", now));
    }

    public int markAllNotificationsRead(String userId, LocalDateTime now) {
        String sql = """
                UPDATE im_local_notification
                SET read_status='READ',read_time=:now
                WHERE user_id=:userId AND read_status='UNREAD'
                """;
        return jdbc.update(sql, Map.of("userId", userId, "now", now));
    }

    public Optional<TemplateRecord> findLatestEnabledTemplate(String templateCode) {
        String sql = """
                SELECT * FROM im_message_template
                WHERE template_code=:templateCode AND status='ENABLED'
                ORDER BY version DESC LIMIT 1
                """;
        return queryOne(sql, Map.of("templateCode", templateCode), TemplateRecord.class);
    }

    public void upsertTemplate(TemplateRecord record) {
        String sql = """
                INSERT INTO im_message_template (
                    id,template_code,template_name,message_type,local_title_template,local_body_template,
                    email_subject_template,email_body_template,default_channels,version,status,created_time,updated_time
                ) VALUES (
                    :id,:templateCode,:templateName,:messageType,:localTitleTemplate,:localBodyTemplate,
                    :emailSubjectTemplate,:emailBodyTemplate,:defaultChannels,:version,:status,:createdTime,:updatedTime
                )
                ON DUPLICATE KEY UPDATE
                    template_name=VALUES(template_name),message_type=VALUES(message_type),
                    local_title_template=VALUES(local_title_template),local_body_template=VALUES(local_body_template),
                    email_subject_template=VALUES(email_subject_template),email_body_template=VALUES(email_body_template),
                    default_channels=VALUES(default_channels),status=VALUES(status),updated_time=VALUES(updated_time)
                """;
        jdbc.update(sql, beanParams(record));
    }

    public List<TemplateRecord> listTemplates() {
        return jdbc.query(
                "SELECT * FROM im_message_template ORDER BY template_code,version DESC",
                Map.of(),
                DataClassRowMapper.newInstance(TemplateRecord.class)
        );
    }

    private <T> Optional<T> queryOne(String sql, Map<String, ?> params, Class<T> type) {
        List<T> values = jdbc.query(sql, params, DataClassRowMapper.newInstance(type));
        return values.stream().findFirst();
    }

    private MapSqlParameterSource beanParams(Object bean) {
        if (bean == null || !bean.getClass().isRecord()) {
            throw new IllegalArgumentException("Only Java records are supported as SQL parameter beans");
        }
        MapSqlParameterSource source = new MapSqlParameterSource();
        try {
            for (java.lang.reflect.RecordComponent component : bean.getClass().getRecordComponents()) {
                source.addValue(component.getName(), component.getAccessor().invoke(bean));
            }
            return source;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read SQL parameter record", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
