package single.cjj.im.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.ReadStatus;
import single.cjj.im.domain.ImModels.RecipientRecord;
import single.cjj.im.repository.ImMessageRepository;
import single.cjj.im.realtime.RealtimeModels.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RealtimeNotificationService {
    private final ImMessageRepository messageRepository;
    private final RealtimeNotificationRepository realtimeRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final RedisRouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;

    public RealtimeNotificationService(ImMessageRepository messageRepository, RealtimeNotificationRepository realtimeRepository, WebSocketSessionRegistry sessionRegistry, RedisRouteRegistry routeRegistry, ObjectMapper objectMapper) {
        this.messageRepository = messageRepository; this.realtimeRepository = realtimeRepository; this.sessionRegistry = sessionRegistry; this.routeRegistry = routeRegistry; this.objectMapper = objectMapper;
    }

    @Transactional
    public String createLocalNotification(MessageRecord message, RecipientRecord recipient, ChannelTaskRecord channelTask) {
        LocalDateTime now = LocalDateTime.now();
        LocalNotificationRecord candidate = new LocalNotificationRecord(id(), message.id(), recipient.id(), recipient.receiverId(), channelTask.subject(), channelTask.content(), message.messageType(), message.businessType(), message.businessId(), message.actionUrl(), "CREATED", ReadStatus.UNREAD, null, now);
        int inserted = messageRepository.insertLocalNotificationIfAbsent(candidate);
        LocalNotificationRecord notification = inserted > 0 ? candidate : realtimeRepository.findNotificationByMessageAndUser(message.id(), recipient.receiverId()).orElse(candidate);
        if (inserted > 0) {
            ObjectNode data = objectMapper.valueToTree(notification); data.put("notificationId", notification.id());
            PushEnvelope envelope = persistEvent(message.tenantId(), recipient.receiverId(), EventType.NOTIFICATION_CREATED, notification.id(), data, now);
            afterCommit(() -> dispatch(message.tenantId(), recipient.receiverId(), envelope));
        }
        return notification.id();
    }

    @Transactional
    public boolean markRead(String notificationId, String userId, String deviceId, String clientType) {
        NotificationContext context = realtimeRepository.findNotificationContext(notificationId, userId).orElse(null);
        if (context == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if (messageRepository.markNotificationRead(notificationId, userId, now) == 0) return false;
        realtimeRepository.markNotificationReceiptsRead(notificationId, userId, now);
        if (deviceId != null && !deviceId.isBlank()) realtimeRepository.upsertDeliveryReceipt(context.tenantId(), notificationId, userId, deviceId, normalizeClientType(clientType), ReceiptStatus.READ, now, now, now);
        ObjectNode data = objectMapper.createObjectNode(); data.put("notificationId", notificationId); data.put("readTime", now.toString()); data.put("unreadCount", messageRepository.countLocalNotifications(userId, ReadStatus.UNREAD));
        PushEnvelope envelope = persistEvent(context.tenantId(), userId, EventType.NOTIFICATION_READ, notificationId, data, now);
        afterCommit(() -> dispatch(context.tenantId(), userId, envelope));
        return true;
    }

    @Transactional
    public int markAllRead(String userId) {
        List<String> tenants = realtimeRepository.findUnreadTenants(userId); LocalDateTime now = LocalDateTime.now();
        int updated = messageRepository.markAllNotificationsRead(userId, now); if (updated == 0) return 0;
        realtimeRepository.markAllReceiptsRead(userId, now);
        for (String tenantId : tenants) {
            ObjectNode data = objectMapper.createObjectNode(); data.put("updated", updated); data.put("readTime", now.toString()); data.put("unreadCount", 0);
            PushEnvelope envelope = persistEvent(tenantId, userId, EventType.NOTIFICATIONS_READ_ALL, null, data, now);
            afterCommit(() -> dispatch(tenantId, userId, envelope));
        }
        return updated;
    }

    public SyncResponse sync(String tenantId, String userId, long afterVersion, int requestedLimit) {
        int limit = Math.min(200, Math.max(1, requestedLimit));
        List<PushEventRecord> records = realtimeRepository.listPushEvents(tenantId, userId, Math.max(0, afterVersion), limit); List<PushEnvelope> events = new ArrayList<>(records.size());
        for (PushEventRecord record : records) events.add(new PushEnvelope(record.eventId(), record.eventType(), record.version(), record.createdTime().toInstant(ZoneOffset.UTC).toEpochMilli(), record.eventId(), readJson(record.payloadJson())));
        long currentVersion = realtimeRepository.currentVersion(tenantId, userId); long lastReturned = records.isEmpty() ? afterVersion : records.get(records.size()-1).version();
        return new SyncResponse(currentVersion, lastReturned < currentVersion, events);
    }

    public PushEnvelope connectedEnvelope(String tenantId, String userId) { ObjectNode data = objectMapper.createObjectNode(); data.put("currentVersion", realtimeRepository.currentVersion(tenantId, userId)); data.put("instanceId", routeRegistry.instanceId()); return transientEnvelope(EventType.SYSTEM_CONNECTED, data); }
    public PushEnvelope pongEnvelope() { ObjectNode data = objectMapper.createObjectNode(); data.put("serverTime", System.currentTimeMillis()); return transientEnvelope(EventType.SYSTEM_PONG, data); }

    public void ackDelivered(String tenantId, String userId, String eventId, String notificationId, String deviceId, String clientType) {
        if (notificationId == null || notificationId.isBlank()) return; LocalDateTime now = LocalDateTime.now();
        realtimeRepository.upsertDeliveryReceipt(tenantId, notificationId, userId, deviceId, normalizeClientType(clientType), ReceiptStatus.DELIVERED, now, null, now);
        if (eventId != null && !eventId.isBlank()) realtimeRepository.markPushEventStatus(eventId, PushStatus.DELIVERED, now);
        realtimeRepository.markNotificationPushStatus(notificationId, "DELIVERED");
    }

    public PushAttemptResult deliverRemoteBroadcast(PushBroadcast broadcast) { return deliverLocally(broadcast.tenantId(), broadcast.userId(), broadcast.envelope()); }

    private PushEnvelope persistEvent(String tenantId, String userId, String eventType, String notificationId, JsonNode data, LocalDateTime now) {
        long version = realtimeRepository.nextVersion(tenantId, userId, now); String eventId = id();
        realtimeRepository.insertPushEvent(new PushEventRecord(id(), eventId, tenantId, userId, version, eventType, notificationId, writeJson(data), PushStatus.PENDING, now, null));
        return new PushEnvelope(eventId, eventType, version, now.toInstant(ZoneOffset.UTC).toEpochMilli(), eventId, data);
    }

    private void dispatch(String tenantId, String userId, PushEnvelope envelope) {
        PushAttemptResult local = deliverLocally(tenantId, userId, envelope); Set<String> remote;
        try { remote = routeRegistry.findRemoteInstanceIds(tenantId, userId); PushBroadcast b = new PushBroadcast(tenantId, userId, envelope); for (String instanceId : remote) routeRegistry.publish(instanceId, b); }
        catch (RuntimeException e) { remote = Set.of(); }
        String status = local.pushedSessions() > 0 ? PushStatus.PUSHED : (!remote.isEmpty() ? PushStatus.ROUTED : PushStatus.OFFLINE); LocalDateTime now = LocalDateTime.now();
        realtimeRepository.markPushEventStatus(envelope.eventId(), status, now); String notificationId = notificationId(envelope); if (notificationId != null) realtimeRepository.markNotificationPushStatus(notificationId, status);
    }

    private PushAttemptResult deliverLocally(String tenantId, String userId, PushEnvelope envelope) {
        List<SessionDelivery> deliveries = sessionRegistry.send(tenantId, userId, envelope); int pushed = 0; String notificationId = notificationId(envelope); LocalDateTime now = LocalDateTime.now();
        for (SessionDelivery delivery : deliveries) if (delivery.success()) { pushed++; if (notificationId != null) realtimeRepository.upsertDeliveryReceipt(tenantId, notificationId, userId, delivery.deviceId(), delivery.clientType(), ReceiptStatus.PUSHED, null, null, now); }
        if (pushed > 0) { realtimeRepository.markPushEventStatus(envelope.eventId(), PushStatus.PUSHED, now); if (notificationId != null) realtimeRepository.markNotificationPushStatus(notificationId, PushStatus.PUSHED); }
        return new PushAttemptResult(deliveries.size(), pushed);
    }

    private PushEnvelope transientEnvelope(String eventType, JsonNode data) { String eventId = id(); return new PushEnvelope(eventId, eventType, 0, System.currentTimeMillis(), eventId, data); }
    private String notificationId(PushEnvelope envelope) { JsonNode data = envelope.data(); if (data == null || !data.hasNonNull("notificationId")) return null; String value = data.get("notificationId").asText(); return value.isBlank() ? null : value; }
    private String normalizeClientType(String value) { return value == null || value.isBlank() ? "UNKNOWN" : value.trim().toUpperCase(); }
    private void afterCommit(Runnable action) { if (!TransactionSynchronizationManager.isSynchronizationActive()) { action.run(); return; } TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { public void afterCommit() { action.run(); } }); }
    private String writeJson(JsonNode data) { try { return objectMapper.writeValueAsString(data); } catch (JsonProcessingException e) { throw new IllegalStateException("实时推送事件序列化失败", e); } }
    private JsonNode readJson(String json) { try { return objectMapper.readTree(json); } catch (JsonProcessingException e) { return objectMapper.createObjectNode().put("raw", json); } }
    private String id() { return UUID.randomUUID().toString().replace("-", ""); }
}
