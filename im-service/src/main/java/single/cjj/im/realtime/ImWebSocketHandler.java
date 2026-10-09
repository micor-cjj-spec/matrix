package single.cjj.im.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import single.cjj.im.realtime.RealtimeModels.ClientFrame;
import single.cjj.im.realtime.WebSocketSessionRegistry.SessionContext;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSessionRegistry sessionRegistry; private final RedisRouteRegistry routeRegistry; private final RealtimeNotificationService realtimeService; private final ObjectMapper objectMapper;
    public ImWebSocketHandler(WebSocketSessionRegistry sessionRegistry, RedisRouteRegistry routeRegistry, RealtimeNotificationService realtimeService, ObjectMapper objectMapper) { this.sessionRegistry=sessionRegistry; this.routeRegistry=routeRegistry; this.realtimeService=realtimeService; this.objectMapper=objectMapper; }

    public void afterConnectionEstablished(WebSocketSession session) { SessionContext context=context(session); sessionRegistry.register(context); routeRegistry.register(context); sessionRegistry.sendToSession(session,realtimeService.connectedEnvelope(context.tenantId(),context.userId())); }
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionContext context=context(session); ClientFrame frame=objectMapper.readValue(message.getPayload(),ClientFrame.class); if(frame.eventType()==null)return;
        switch(frame.eventType()) {
            case RealtimeModels.EventType.SYSTEM_PING -> { routeRegistry.touch(context); sessionRegistry.sendToSession(session,realtimeService.pongEnvelope()); }
            case RealtimeModels.EventType.DELIVER_ACK -> realtimeService.ackDelivered(context.tenantId(),context.userId(),frame.eventId(),frame.notificationId(),context.deviceId(),context.clientType());
            case RealtimeModels.EventType.READ_ACK -> realtimeService.markRead(frame.notificationId(),context.userId(),context.deviceId(),context.clientType());
            default -> {}
        }
    }
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception { remove(session); if(session.isOpen())session.close(CloseStatus.SERVER_ERROR); }
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { remove(session); }
    private void remove(WebSocketSession session) { SessionContext context=context(session); SessionContext removed=sessionRegistry.unregister(context.tenantId(),context.userId(),session.getId()); routeRegistry.unregister(removed==null?context:removed); }
    private SessionContext context(WebSocketSession session) { return new SessionContext(String.valueOf(session.getAttributes().get(ImWebSocketHandshakeInterceptor.ATTR_TENANT_ID)),String.valueOf(session.getAttributes().get(ImWebSocketHandshakeInterceptor.ATTR_USER_ID)),String.valueOf(session.getAttributes().get(ImWebSocketHandshakeInterceptor.ATTR_DEVICE_ID)),String.valueOf(session.getAttributes().get(ImWebSocketHandshakeInterceptor.ATTR_CLIENT_TYPE)),session); }
}
