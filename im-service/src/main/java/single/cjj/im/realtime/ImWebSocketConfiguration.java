package single.cjj.im.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import single.cjj.im.realtime.RealtimeModels.PushBroadcast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebSocket
public class ImWebSocketConfiguration implements WebSocketConfigurer {
    private final ImWebSocketHandler handler; private final ImWebSocketHandshakeInterceptor handshakeInterceptor; private final String[] allowedOrigins;
    public ImWebSocketConfiguration(ImWebSocketHandler handler, ImWebSocketHandshakeInterceptor handshakeInterceptor, @Value("${im.websocket.allowed-origin-patterns:*}") String allowedOrigins) { this.handler=handler; this.handshakeInterceptor=handshakeInterceptor; this.allowedOrigins=Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(v->!v.isEmpty()).toArray(String[]::new); }
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) { registry.addHandler(handler,"/im/ws").addInterceptors(handshakeInterceptor).setAllowedOriginPatterns(allowedOrigins.length==0?new String[]{"*"}:allowedOrigins); }
    @Bean RedisMessageListenerContainer imRedisPushListener(RedisConnectionFactory connectionFactory, RedisRouteRegistry routeRegistry, RealtimeNotificationService realtimeService, ObjectMapper objectMapper) {
        RedisMessageListenerContainer container=new RedisMessageListenerContainer(); container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message,pattern)->{ try { PushBroadcast broadcast=objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8),PushBroadcast.class); realtimeService.deliverRemoteBroadcast(broadcast); } catch(Exception ignored){} },new ChannelTopic(routeRegistry.instanceChannel())); return container;
    }
}
