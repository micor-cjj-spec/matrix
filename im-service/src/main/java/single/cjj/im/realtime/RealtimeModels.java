package single.cjj.im.realtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public final class RealtimeModels {
    private RealtimeModels() {}

    public static final class EventType {
        public static final String SYSTEM_CONNECTED = "SYSTEM_CONNECTED";
        public static final String SYSTEM_PING = "SYSTEM_PING";
        public static final String SYSTEM_PONG = "SYSTEM_PONG";
        public static final String DELIVER_ACK = "DELIVER_ACK";
        public static final String READ_ACK = "READ_ACK";
        public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
        public static final String NOTIFICATION_READ = "NOTIFICATION_READ";
        public static final String NOTIFICATIONS_READ_ALL = "NOTIFICATIONS_READ_ALL";
        private EventType() {}
    }

    public static final class PushStatus {
        public static final String PENDING = "PENDING";
        public static final String PUSHED = "PUSHED";
        public static final String ROUTED = "ROUTED";
        public static final String OFFLINE = "OFFLINE";
        public static final String DELIVERED = "DELIVERED";
        private PushStatus() {}
    }

    public static final class ReceiptStatus {
        public static final String PUSHED = "PUSHED";
        public static final String DELIVERED = "DELIVERED";
        public static final String READ = "READ";
        private ReceiptStatus() {}
    }

    public record PushEnvelope(String eventId, String eventType, long version, long timestamp, String traceId, JsonNode data) {}
    public record ClientFrame(String eventType, String eventId, String notificationId, Long lastVersion) {}
    public record PushEventRecord(String id, String eventId, String tenantId, String userId, long version, String eventType, String notificationId, String payloadJson, String status, LocalDateTime createdTime, LocalDateTime pushedTime) {}
    public record SyncResponse(long currentVersion, boolean hasMore, List<PushEnvelope> events) {}
    public record NotificationContext(String tenantId, String notificationId, String messageId, String recipientId, String userId, String title, String content, String messageType, String businessType, String businessId, String actionUrl, String pushStatus, String readStatus, LocalDateTime readTime, LocalDateTime createdTime) {}
    public record RouteRecord(String tenantId, String userId, String instanceId, String sessionId, String deviceId, String clientType, long lastSeenAt) {}
    public record PushBroadcast(String tenantId, String userId, PushEnvelope envelope) {}
    public record SessionDelivery(String sessionId, String deviceId, String clientType, boolean success) {}
    public record PushAttemptResult(int matchedSessions, int pushedSessions) {}
}
