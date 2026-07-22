package single.cjj.im.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import single.cjj.im.realtime.RealtimeModels.PushEnvelope;
import single.cjj.im.realtime.WebSocketSessionRegistry.SessionContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSessionRegistryTest {
    @Test void sendsToAllSessionsOfSameUser() throws Exception {
        WebSocketSessionRegistry registry=new WebSocketSessionRegistry(new ObjectMapper()); WebSocketSession first=session("s1"),second=session("s2");
        registry.register(new SessionContext("default","u1","d1","WEB",first)); registry.register(new SessionContext("default","u1","d2","APP",second));
        PushEnvelope envelope=new PushEnvelope("e1",RealtimeModels.EventType.NOTIFICATION_CREATED,1,System.currentTimeMillis(),"trace-1",JsonNodeFactory.instance.objectNode().put("notificationId","n1"));
        List<RealtimeModels.SessionDelivery> deliveries=registry.send("default","u1",envelope);
        assertThat(deliveries).hasSize(2).allMatch(RealtimeModels.SessionDelivery::success); verify(first).sendMessage(any(TextMessage.class)); verify(second).sendMessage(any(TextMessage.class));
    }
    private WebSocketSession session(String id){ WebSocketSession session=mock(WebSocketSession.class); when(session.getId()).thenReturn(id); when(session.isOpen()).thenReturn(true); return session; }
}
