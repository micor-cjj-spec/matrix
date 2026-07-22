package single.cjj.im.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import single.cjj.im.realtime.RealtimeModels.PushEnvelope;
import single.cjj.im.realtime.RealtimeModels.SessionDelivery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    private final Map<String, Map<String, SessionContext>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionRegistry(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public void register(SessionContext context) {
        sessions.computeIfAbsent(userKey(context.tenantId(), context.userId()), ignored -> new ConcurrentHashMap<>()).put(context.session().getId(), context);
    }

    public SessionContext unregister(String tenantId, String userId, String sessionId) {
        String key = userKey(tenantId, userId);
        Map<String, SessionContext> userSessions = sessions.get(key);
        if (userSessions == null) return null;
        SessionContext removed = userSessions.remove(sessionId);
        if (userSessions.isEmpty()) sessions.remove(key, userSessions);
        return removed;
    }

    public List<SessionDelivery> send(String tenantId, String userId, PushEnvelope envelope) {
        Map<String, SessionContext> userSessions = sessions.get(userKey(tenantId, userId));
        if (userSessions == null || userSessions.isEmpty()) return List.of();
        String payload = serialize(envelope);
        List<SessionDelivery> deliveries = new ArrayList<>();
        for (SessionContext context : List.copyOf(userSessions.values())) {
            boolean success = sendText(context.session(), payload);
            deliveries.add(new SessionDelivery(context.session().getId(), context.deviceId(), context.clientType(), success));
            if (!success && !context.session().isOpen()) unregister(tenantId, userId, context.session().getId());
        }
        return deliveries;
    }

    public boolean sendToSession(WebSocketSession session, PushEnvelope envelope) { return sendText(session, serialize(envelope)); }
    public int sessionCount(String tenantId, String userId) { Map<String, SessionContext> values = sessions.get(userKey(tenantId, userId)); return values == null ? 0 : values.size(); }

    private boolean sendText(WebSocketSession session, String payload) {
        if (!session.isOpen()) return false;
        try {
            synchronized (session) {
                if (!session.isOpen()) return false;
                session.sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException | IllegalStateException e) { return false; }
    }

    private String serialize(PushEnvelope envelope) {
        try { return objectMapper.writeValueAsString(envelope); }
        catch (JsonProcessingException e) { throw new IllegalStateException("WebSocket事件序列化失败", e); }
    }

    private String userKey(String tenantId, String userId) { return tenantId + ":" + userId; }
    public record SessionContext(String tenantId, String userId, String deviceId, String clientType, WebSocketSession session) {}
}
