package single.cjj.im.realtime;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Component
public class ImWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String ATTR_TENANT_ID = "im.tenantId";
    public static final String ATTR_USER_ID = "im.userId";
    public static final String ATTR_DEVICE_ID = "im.deviceId";
    public static final String ATTR_CLIENT_TYPE = "im.clientType";

    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String,Object> attributes) {
        String userId = request.getHeaders().getFirst("X-User-Id"); if (!StringUtils.hasText(userId)) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false; }
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id"); if (!StringUtils.hasText(tenantId)) tenantId = "default";
        var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        attributes.put(ATTR_TENANT_ID, safe(tenantId, "default", 64)); attributes.put(ATTR_USER_ID, safe(userId, null, 128));
        attributes.put(ATTR_DEVICE_ID, safe(query.getFirst("deviceId"), UUID.randomUUID().toString().replace("-", ""), 128));
        attributes.put(ATTR_CLIENT_TYPE, safe(query.getFirst("clientType"), "WEB", 32).toUpperCase()); return true;
    }
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
    private String safe(String value, String defaultValue, int maxLength) { String result = StringUtils.hasText(value) ? value.trim() : defaultValue; if (result == null) return null; return result.length() <= maxLength ? result : result.substring(0,maxLength); }
}
