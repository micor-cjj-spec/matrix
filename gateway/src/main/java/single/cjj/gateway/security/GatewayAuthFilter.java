package single.cjj.gateway.security;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String IM_WEBSOCKET_PATH = "/api/im/ws";

    private static final List<String> WHITELIST = List.of(
            "/api/auth/**",
            "/api/actuator/**",
            "/api/swagger-ui/**",
            "/api/v3/api-docs/**",
            // OpenAPI 使用 AppKey + HMAC，由 openapi-service 再次执行强校验。
            "/api/open-api/**",
            // 调度执行器内部接口由 scheduler-service 使用共享密钥再次校验。
            "/api/scheduler/executors/register",
            "/api/scheduler/executors/heartbeat",
            "/api/scheduler/callback/**"
    );

    private static final List<String> INTERNAL_IDENTITY_HEADERS = List.of(
            "X-User-Id",
            "X-Tenant-Id",
            "X-OpenApi-App-Id",
            "X-OpenApi-Tenant-Id",
            "X-OpenApi-Verified"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        String path = originalRequest.getURI().getPath();
        String token = resolveToken(originalRequest, path);
        ServerHttpRequest request = sanitizeIdentityHeaders(removeWebSocketToken(originalRequest, path));
        ServerWebExchange sanitizedExchange = exchange.mutate().request(request).build();

        if (isWhitelisted(path)) {
            return chain.filter(sanitizedExchange);
        }

        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange.getResponse(), "未登录或token缺失");
        }

        try {
            Claims claims = JwtUtils.parseToken(token);
            String userId = String.valueOf(claims.get("id"));
            Object tenantClaim = claims.get("tenantId");
            String tenantId = tenantClaim == null ? "default" : String.valueOf(tenantClaim);

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Tenant-Id", tenantId)
                    .build();
            return chain.filter(sanitizedExchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("gateway jwt verify failed: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "token无效或已过期");
        }
    }

    private String resolveToken(ServerHttpRequest request, String path) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (IM_WEBSOCKET_PATH.equals(path)) {
            return request.getQueryParams().getFirst("access_token");
        }
        return null;
    }

    private ServerHttpRequest removeWebSocketToken(ServerHttpRequest request, String path) {
        if (!IM_WEBSOCKET_PATH.equals(path) || !request.getQueryParams().containsKey("access_token")) {
            return request;
        }
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>(request.getQueryParams());
        query.remove("access_token");
        URI sanitizedUri = UriComponentsBuilder.fromUri(request.getURI())
                .replaceQueryParams(query)
                .build(true)
                .toUri();
        return request.mutate().uri(sanitizedUri).build();
    }

    private ServerHttpRequest sanitizeIdentityHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers ->
                INTERNAL_IDENTITY_HEADERS.forEach(headers::remove)
        ).build();
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
