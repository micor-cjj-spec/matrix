package single.cjj.openapi.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import single.cjj.openapi.dto.OpenApiEnvelope;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiDefinition;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.entity.OpenApiRequestLog;
import single.cjj.openapi.exception.OpenApiAuthException;
import single.cjj.openapi.mapper.OpenApiAppMapper;
import single.cjj.openapi.mapper.OpenApiDefinitionMapper;
import single.cjj.openapi.mapper.OpenApiGrantMapper;
import single.cjj.openapi.mapper.OpenApiRequestLogMapper;
import single.cjj.openapi.service.OpenApiSecretService;
import single.cjj.openapi.service.OpenApiSignatureService;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OpenApiAuthenticationFilter extends OncePerRequestFilter {

    private static final String APP_KEY_HEADER = "X-Matrix-App-Key";
    private static final String TIMESTAMP_HEADER = "X-Matrix-Timestamp";
    private static final String NONCE_HEADER = "X-Matrix-Nonce";
    private static final String SIGNATURE_HEADER = "X-Matrix-Signature";
    private static final String REQUEST_ID_HEADER = "X-Matrix-Request-Id";
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST");

    private final OpenApiAppMapper appMapper;
    private final OpenApiDefinitionMapper definitionMapper;
    private final OpenApiGrantMapper grantMapper;
    private final OpenApiRequestLogMapper requestLogMapper;
    private final OpenApiSecretService secretService;
    private final OpenApiSignatureService signatureService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final long replayWindowSeconds;
    private final int maxRequestBodyBytes;

    public OpenApiAuthenticationFilter(OpenApiAppMapper appMapper,
                                       OpenApiDefinitionMapper definitionMapper,
                                       OpenApiGrantMapper grantMapper,
                                       OpenApiRequestLogMapper requestLogMapper,
                                       OpenApiSecretService secretService,
                                       OpenApiSignatureService signatureService,
                                       StringRedisTemplate redisTemplate,
                                       ObjectMapper objectMapper,
                                       @Value("${matrix.openapi.replay-window-seconds:300}") long replayWindowSeconds,
                                       @Value("${matrix.openapi.max-request-body-bytes:1048576}") int maxRequestBodyBytes) {
        this.appMapper = appMapper;
        this.definitionMapper = definitionMapper;
        this.grantMapper = grantMapper;
        this.requestLogMapper = requestLogMapper;
        this.secretService = secretService;
        this.signatureService = signatureService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.replayWindowSeconds = Math.max(60, replayWindowSeconds);
        this.maxRequestBodyBytes = Math.max(1024, maxRequestBodyBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !relativePath(request).startsWith("/open-api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        LocalDateTime requestTime = LocalDateTime.now();
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        String errorCode = null;
        String errorMessage = null;
        OpenApiApp app = null;
        OpenApiDefinition definition = null;
        HttpServletRequest effectiveRequest = request;

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            String method = request.getMethod().toUpperCase();
            if (!ALLOWED_METHODS.contains(method)) {
                throw new OpenApiAuthException("OPENAPI_40001", "开放平台当前只支持GET和POST请求", 405);
            }

            byte[] requestBody = new byte[0];
            if ("POST".equals(method)) {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
                requestBody = cachedRequest.getCachedBody();
                if (requestBody.length > maxRequestBodyBytes) {
                    throw new OpenApiAuthException("OPENAPI_41301", "请求体超过开放平台大小限制", 413);
                }
                effectiveRequest = cachedRequest;
            }

            String appKey = requiredHeader(effectiveRequest, APP_KEY_HEADER, "OPENAPI_40101", "AppKey缺失");
            String timestamp = requiredHeader(effectiveRequest, TIMESTAMP_HEADER, "OPENAPI_40103", "时间戳缺失");
            String nonce = requiredHeader(effectiveRequest, NONCE_HEADER, "OPENAPI_40104", "Nonce缺失");
            String signature = requiredHeader(effectiveRequest, SIGNATURE_HEADER, "OPENAPI_40102", "签名缺失");

            validateTimestamp(timestamp);
            app = findApp(appKey);
            validateApp(app);

            String path = relativePath(effectiveRequest);
            definition = findDefinition(method, path);
            OpenApiGrant grant = findGrant(app.getId(), definition.getId());
            validateGrant(grant);
            validateIp(app, clientIp(effectiveRequest));

            String canonicalRequest = signatureService.canonicalRequest(
                    method,
                    path,
                    effectiveRequest.getParameterMap(),
                    requestBody,
                    timestamp,
                    nonce
            );
            String expectedSignature = signatureService.sign(
                    secretService.decrypt(app.getAppSecretCipher()),
                    canonicalRequest
            );
            if (!signatureService.verify(expectedSignature, signature)) {
                throw new OpenApiAuthException("OPENAPI_40102", "签名错误", 401);
            }

            validateNonce(app.getAppKey(), nonce);
            validateRateLimit(app, definition);

            effectiveRequest.setAttribute(
                    OpenApiContext.REQUEST_ATTRIBUTE,
                    new OpenApiContext(requestId, app, definition, grant)
            );
            effectiveRequest.setAttribute(
                    OpenApiContext.REQUEST_BODY_HASH_ATTRIBUTE,
                    signatureService.sha256Hex(requestBody)
            );
            filterChain.doFilter(effectiveRequest, response);
        } catch (OpenApiAuthException e) {
            errorCode = e.getCode();
            errorMessage = e.getMessage();
            writeError(response, e.getHttpStatus(), e.getCode(), e.getMessage(), requestId);
        } catch (Exception e) {
            errorCode = "OPENAPI_50001";
            errorMessage = e.getMessage();
            log.error("openapi request failed, requestId={}", requestId, e);
            if (!response.isCommitted()) {
                writeError(response, 500, errorCode, "OpenAPI内部服务异常", requestId);
            }
        } finally {
            writeRequestLog(
                    effectiveRequest,
                    response,
                    requestId,
                    requestTime,
                    startedAt,
                    app,
                    definition,
                    errorCode,
                    errorMessage
            );
        }
    }

    private OpenApiApp findApp(String appKey) {
        OpenApiApp app = appMapper.selectOne(new LambdaQueryWrapper<OpenApiApp>()
                .eq(OpenApiApp::getAppKey, appKey));
        if (app == null) {
            throw new OpenApiAuthException("OPENAPI_40101", "AppKey不存在", 401);
        }
        return app;
    }

    private OpenApiDefinition findDefinition(String method, String path) {
        List<OpenApiDefinition> definitions = definitionMapper.selectList(
                new LambdaQueryWrapper<OpenApiDefinition>()
                        .eq(OpenApiDefinition::getHttpMethod, method.toUpperCase())
                        .eq(OpenApiDefinition::getStatus, "PUBLISHED")
        );
        return definitions.stream()
                .filter(item -> pathMatcher.match(item.getExternalPath(), path))
                .findFirst()
                .orElseThrow(() -> new OpenApiAuthException(
                        "OPENAPI_40401", "API不存在或未发布", 404
                ));
    }

    private OpenApiGrant findGrant(Long appId, Long apiDefinitionId) {
        OpenApiGrant grant = grantMapper.selectOne(new LambdaQueryWrapper<OpenApiGrant>()
                .eq(OpenApiGrant::getAppId, appId)
                .eq(OpenApiGrant::getApiDefinitionId, apiDefinitionId));
        if (grant == null) {
            throw new OpenApiAuthException("OPENAPI_40302", "应用未获得该API的访问权限", 403);
        }
        return grant;
    }

    private void validateApp(OpenApiApp app) {
        LocalDateTime now = LocalDateTime.now();
        if (!"ENABLED".equalsIgnoreCase(app.getStatus())) {
            throw new OpenApiAuthException("OPENAPI_40301", "应用已停用", 403);
        }
        if (app.getValidFrom() != null && app.getValidFrom().isAfter(now)) {
            throw new OpenApiAuthException("OPENAPI_40301", "应用尚未生效", 403);
        }
        if (app.getValidTo() != null && app.getValidTo().isBefore(now)) {
            throw new OpenApiAuthException("OPENAPI_40301", "应用已过期", 403);
        }
    }

    private void validateGrant(OpenApiGrant grant) {
        LocalDateTime now = LocalDateTime.now();
        if (!"ENABLED".equalsIgnoreCase(grant.getStatus())) {
            throw new OpenApiAuthException("OPENAPI_40302", "API授权已停用", 403);
        }
        if (grant.getValidFrom() != null && grant.getValidFrom().isAfter(now)) {
            throw new OpenApiAuthException("OPENAPI_40302", "API授权尚未生效", 403);
        }
        if (grant.getValidTo() != null && grant.getValidTo().isBefore(now)) {
            throw new OpenApiAuthException("OPENAPI_40302", "API授权已过期", 403);
        }
    }

    private void validateTimestamp(String timestamp) {
        try {
            long value = Long.parseLong(timestamp);
            long diff = Math.abs(System.currentTimeMillis() - value);
            if (diff > replayWindowSeconds * 1000L) {
                throw new OpenApiAuthException("OPENAPI_40103", "时间戳已过期", 401);
            }
        } catch (NumberFormatException e) {
            throw new OpenApiAuthException("OPENAPI_40103", "时间戳格式错误", 401);
        }
    }

    private void validateNonce(String appKey, String nonce) {
        String key = "openapi:nonce:" + appKey + ":" + nonce;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                Duration.ofSeconds(replayWindowSeconds)
        );
        if (!Boolean.TRUE.equals(created)) {
            throw new OpenApiAuthException("OPENAPI_40104", "请求已被处理，禁止重放", 401);
        }
    }

    private void validateRateLimit(OpenApiApp app, OpenApiDefinition definition) {
        int limit = app.getQpsLimit() == null || app.getQpsLimit() <= 0 ? 10 : app.getQpsLimit();
        long second = System.currentTimeMillis() / 1000L;
        String key = "openapi:qps:" + app.getAppId() + ":" + definition.getApiCode() + ":" + second;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(2));
        }
        if (count != null && count > limit) {
            throw new OpenApiAuthException("OPENAPI_42901", "请求频率超过限制", 429);
        }
    }

    private void validateIp(OpenApiApp app, String clientIp) {
        if (!StringUtils.hasText(app.getIpWhitelist())) {
            return;
        }
        boolean allowed = Arrays.stream(app.getIpWhitelist().split(","))
                .map(String::trim)
                .anyMatch(value -> "*".equals(value) || value.equals(clientIp));
        if (!allowed) {
            throw new OpenApiAuthException("OPENAPI_40304", "客户端IP不在白名单", 403);
        }
    }

    private String requiredHeader(HttpServletRequest request, String name, String code, String message) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            throw new OpenApiAuthException(code, message, 401);
        }
        return value.trim();
    }

    private String relativePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String requestId(String provided) {
        String value = StringUtils.hasText(provided)
                ? provided.trim()
                : "req_" + UUID.randomUUID().toString().replace("-", "");
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private void writeError(HttpServletResponse response,
                            int status,
                            String code,
                            String message,
                            String requestId) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), OpenApiEnvelope.error(code, message, requestId));
    }

    private void writeRequestLog(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String requestId,
                                 LocalDateTime requestTime,
                                 long startedAt,
                                 OpenApiApp app,
                                 OpenApiDefinition definition,
                                 String errorCode,
                                 String errorMessage) {
        try {
            OpenApiRequestLog requestLog = new OpenApiRequestLog();
            requestLog.setRequestId(requestId);
            requestLog.setAppId(app == null ? null : app.getAppId());
            requestLog.setApiCode(definition == null ? null : definition.getApiCode());
            requestLog.setApiVersion(definition == null ? null : definition.getApiVersion());
            requestLog.setHttpMethod(request.getMethod());
            requestLog.setRequestPath(relativePath(request));
            requestLog.setClientIp(clientIp(request));
            requestLog.setHttpStatus(response.getStatus());
            requestLog.setSuccess(errorCode == null && response.getStatus() < 400);
            requestLog.setResponseCode(errorCode == null ? "0" : errorCode);
            requestLog.setDurationMs(System.currentTimeMillis() - startedAt);
            requestLog.setRequestTime(requestTime);
            requestLog.setResponseTime(LocalDateTime.now());
            requestLog.setErrorMessage(errorMessage == null ? null : truncate(errorMessage, 1000));
            requestLogMapper.insert(requestLog);
        } catch (Exception e) {
            log.warn("write openapi request log failed, requestId={}, message={}", requestId, e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
