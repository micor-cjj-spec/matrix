package single.cjj.im.realtime;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.realtime.RealtimeModels.NotificationContext;
import single.cjj.im.realtime.RealtimeModels.PushEventRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RealtimeNotificationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public RealtimeNotificationRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<LocalNotificationRecord> findNotificationByMessageAndUser(String messageId, String userId) {
        return jdbc.query("SELECT * FROM im_local_notification WHERE message_id=:messageId AND user_id=:userId LIMIT 1",
                Map.of("messageId", messageId, "userId", userId), DataClassRowMapper.newInstance(LocalNotificationRecord.class)).stream().findFirst();
    }

    public Optional<NotificationContext> findNotificationContext(String notificationId, String userId) {
        String sql = "SELECT m.tenant_id,n.* FROM im_local_notification n JOIN im_message_task m ON m.id=n.message_id WHERE n.id=:notificationId AND n.user_id=:userId LIMIT 1";
        return jdbc.query(sql, Map.of("notificationId", notificationId, "userId", userId), new NotificationContextRowMapper()).stream().findFirst();
    }

    public List<String> findUnreadTenants(String userId) {
        return jdbc.queryForList("SELECT DISTINCT m.tenant_id FROM im_local_notification n JOIN im_message_task m ON m.id=n.message_id WHERE n.user_id=:userId AND n.read_status='UNREAD'", Map.of("userId", userId), String.class);
    }

    public long nextVersion(String tenantId, String userId, LocalDateTime now) {
        jdbc.update("INSERT IGNORE INTO im_user_sync_cursor(tenant_id,user_id,current_version,updated_time) VALUES(:tenantId,:userId,0,:now)", Map.of("tenantId", tenantId, "userId", userId, "now", now));
        Long current = jdbc.queryForObject("SELECT current_version FROM im_user_sync_cursor WHERE tenant_id=:tenantId AND user_id=:userId FOR UPDATE", Map.of("tenantId", tenantId, "userId", userId), Long.class);
        long next = (current == null ? 0L : current) + 1L;
        jdbc.update("UPDATE im_user_sync_cursor SET current_version=:next,updated_time=:now WHERE tenant_id=:tenantId AND user_id=:userId", Map.of("next", next, "now", now, "tenantId", tenantId, "userId", userId));
        return next;
    }

    public long currentVersion(String tenantId, String userId) {
        Long value = jdbc.queryForObject("SELECT COALESCE(MAX(current_version),0) FROM im_user_sync_cursor WHERE tenant_id=:tenantId AND user_id=:userId", Map.of("tenantId", tenantId, "userId", userId), Long.class);
        return value == null ? 0L : value;
    }

    public void insertPushEvent(PushEventRecord record) {
        String sql = "INSERT INTO im_push_event(id,event_id,tenant_id,user_id,version,event_type,notification_id,payload_json,status,created_time,pushed_time) VALUES(:id,:eventId,:tenantId,:userId,:version,:eventType,:notificationId,CAST(:payloadJson AS JSON),:status,:createdTime,:pushedTime)";
        jdbc.update(sql, beanParams(record));
    }

    public List<PushEventRecord> listPushEvents(String tenantId, String userId, long afterVersion, int limit) {
        return jdbc.query("SELECT * FROM im_push_event WHERE tenant_id=:tenantId AND user_id=:userId AND version>:afterVersion ORDER BY version LIMIT :limit", Map.of("tenantId", tenantId, "userId", userId, "afterVersion", afterVersion, "limit", limit), DataClassRowMapper.newInstance(PushEventRecord.class));
    }

    public void markPushEventStatus(String eventId, String status, LocalDateTime pushedTime) {
        jdbc.update("UPDATE im_push_event SET status=:status,pushed_time=:pushedTime WHERE event_id=:eventId", new MapSqlParameterSource().addValue("eventId", eventId).addValue("status", status).addValue("pushedTime", pushedTime));
    }

    public void markNotificationPushStatus(String notificationId, String status) { jdbc.update("UPDATE im_local_notification SET push_status=:status WHERE id=:notificationId", Map.of("notificationId", notificationId, "status", status)); }

    public void upsertDeliveryReceipt(String tenantId, String notificationId, String userId, String deviceId, String clientType, String status, LocalDateTime deliveredTime, LocalDateTime readTime, LocalDateTime now) {
        String sql = "INSERT INTO im_delivery_receipt(id,tenant_id,notification_id,user_id,device_id,client_type,status,delivered_time,read_time,created_time,updated_time) VALUES(REPLACE(UUID(),'-',''),:tenantId,:notificationId,:userId,:deviceId,:clientType,:status,:deliveredTime,:readTime,:now,:now) ON DUPLICATE KEY UPDATE status=VALUES(status),delivered_time=COALESCE(VALUES(delivered_time),delivered_time),read_time=COALESCE(VALUES(read_time),read_time),updated_time=VALUES(updated_time)";
        jdbc.update(sql, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("notificationId", notificationId).addValue("userId", userId).addValue("deviceId", deviceId).addValue("clientType", clientType).addValue("status", status).addValue("deliveredTime", deliveredTime).addValue("readTime", readTime).addValue("now", now));
    }

    public void markNotificationReceiptsRead(String notificationId, String userId, LocalDateTime now) { jdbc.update("UPDATE im_delivery_receipt SET status='READ',read_time=COALESCE(read_time,:now),updated_time=:now WHERE notification_id=:notificationId AND user_id=:userId", Map.of("notificationId", notificationId, "userId", userId, "now", now)); }
    public void markAllReceiptsRead(String userId, LocalDateTime now) { jdbc.update("UPDATE im_delivery_receipt SET status='READ',read_time=COALESCE(read_time,:now),updated_time=:now WHERE user_id=:userId", Map.of("userId", userId, "now", now)); }

    private MapSqlParameterSource beanParams(Object bean) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        try { for (java.lang.reflect.RecordComponent component : bean.getClass().getRecordComponents()) source.addValue(component.getName(), component.getAccessor().invoke(bean)); return source; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("读取实时推送SQL参数失败", e); }
    }

    private static final class NotificationContextRowMapper implements RowMapper<NotificationContext> {
        public NotificationContext mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new NotificationContext(rs.getString("tenant_id"), rs.getString("id"), rs.getString("message_id"), rs.getString("recipient_id"), rs.getString("user_id"), rs.getString("title"), rs.getString("content"), rs.getString("message_type"), rs.getString("business_type"), rs.getString("business_id"), rs.getString("action_url"), rs.getString("push_status"), rs.getString("read_status"), rs.getTimestamp("read_time") == null ? null : rs.getTimestamp("read_time").toLocalDateTime(), rs.getTimestamp("created_time").toLocalDateTime());
        }
    }
}
