package single.cjj.im.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import single.cjj.im.realtime.RealtimeModels.PushBroadcast;
import single.cjj.im.realtime.RealtimeModels.RouteRecord;
import single.cjj.im.realtime.WebSocketSessionRegistry.SessionContext;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class RedisRouteRegistry {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final Duration routeTtl;

    public RedisRouteRegistry(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                              @Value("${im.websocket.instance-id:${spring.application.name:im-service}-${server.port:10005}}") String instanceId,
                              @Value("${im.websocket.route-ttl-seconds:90}") long routeTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.routeTtl = Duration.ofSeconds(Math.max(30, routeTtlSeconds));
    }

    public String instanceId() { return instanceId; }
    public String instanceChannel() { return "im:push:" + instanceId; }

    public void register(SessionContext context) { try { writeRoute(context, System.currentTimeMillis()); } catch (RuntimeException ignored) {} }
    public void touch(SessionContext context) { try { writeRoute(context, System.currentTimeMillis()); } catch (RuntimeException ignored) {} }
    public void unregister(SessionContext context) {
        try { redisTemplate.opsForHash().delete(routeKey(context.tenantId(), context.userId()), routeField(context.session().getId())); }
        catch (RuntimeException ignored) {}
    }

    public Set<String> findRemoteInstanceIds(String tenantId, String userId) {
        Map<Object, Object> routes = redisTemplate.opsForHash().entries(routeKey(tenantId, userId));
        if (routes.isEmpty()) return Set.of();
        long staleBefore = System.currentTimeMillis() - routeTtl.multipliedBy(2).toMillis();
        Set<String> instanceIds = new HashSet<>();
        for (Object value : routes.values()) {
            RouteRecord route = readRoute(String.valueOf(value));
            if (route != null && route.lastSeenAt() >= staleBefore && !instanceId.equals(route.instanceId())) instanceIds.add(route.instanceId());
        }
        return instanceIds;
    }

    public void publish(String targetInstanceId, PushBroadcast broadcast) {
        try { redisTemplate.convertAndSend("im:push:" + targetInstanceId, objectMapper.writeValueAsString(broadcast)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("跨实例推送事件序列化失败", e); }
    }

    private void writeRoute(SessionContext context, long now) {
        RouteRecord route = new RouteRecord(context.tenantId(), context.userId(), instanceId, context.session().getId(), context.deviceId(), context.clientType(), now);
        String key = routeKey(context.tenantId(), context.userId());
        try {
            redisTemplate.opsForHash().put(key, routeField(context.session().getId()), objectMapper.writeValueAsString(route));
            redisTemplate.expire(key, routeTtl);
        } catch (JsonProcessingException e) { throw new IllegalStateException("在线路由序列化失败", e); }
    }

    private RouteRecord readRoute(String json) { try { return objectMapper.readValue(json, RouteRecord.class); } catch (JsonProcessingException e) { return null; } }
    private String routeKey(String tenantId, String userId) { return "im:route:" + tenantId + ":" + userId; }
    private String routeField(String sessionId) { return instanceId + ":" + sessionId; }
}
