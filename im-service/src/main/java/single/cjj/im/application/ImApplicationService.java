package single.cjj.im.application;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.im.service.ImBusinessException;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImApplicationService {

    private static final Set<String> ALL_CHANNELS = Set.of("LOCAL", "EMAIL");

    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final ImSecretCodec secretCodec;
    private final ImOpenApiProperties fallbackProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ImApplicationService(NamedParameterJdbcTemplate jdbc,
                                StringRedisTemplate redis,
                                ImSecretCodec secretCodec,
                                ImOpenApiProperties fallbackProperties) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.secretCodec = secretCodec;
        this.fallbackProperties = fallbackProperties;
    }

    public AuthenticatedApplication authenticate(String appCode, String sourceIp) {
        AuthenticatedApplication application = requireEnabled(appCode);
        if (!ipAllowed(sourceIp, application.allowedIps())) {
            throw new ApplicationAuthenticationException("调用方 IP 不在白名单内");
        }
        return application;
    }

    public void enforceRateLimit(AuthenticatedApplication application) {
        applyRateLimit(application);
    }

    public AuthenticatedApplication requireEnabled(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            throw new ApplicationAuthenticationException("调用应用不能为空");
        }
        ApplicationRow row = findRow(appCode.trim()).orElse(null);
        if (row != null) {
            if (!"ENABLED".equals(row.status())) {
                throw new ApplicationAuthenticationException("调用应用未启用");
            }
            return new AuthenticatedApplication(
                    row.appCode(),
                    row.appName(),
                    row.tenantId(),
                    secretCodec.decrypt(row.appSecretCiphertext()),
                    split(row.allowedChannels()),
                    split(row.allowedIps()),
                    row.rateLimitPerMinute(),
                    row.callbackUrl(),
                    secretCodec.decrypt(StringUtils.hasText(row.callbackSecretCiphertext())
                            ? row.callbackSecretCiphertext()
                            : row.appSecretCiphertext()),
                    false
            );
        }

        String fallbackSecret = fallbackProperties.getCredentials().get(appCode.trim());
        if (!StringUtils.hasText(fallbackSecret)) {
            throw new ApplicationAuthenticationException("未知或未启用的调用应用");
        }
        return new AuthenticatedApplication(
                appCode.trim(),
                appCode.trim(),
                "default",
                fallbackSecret,
                ALL_CHANNELS,
                Set.of("*"),
                fallbackProperties.getFallbackRateLimitPerMinute(),
                null,
                fallbackSecret,
                true
        );
    }

    public ApplicationSecretResponse upsert(ApplicationUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.appCode()) || !StringUtils.hasText(request.appName())) {
            throw new ImBusinessException("appCode 和 appName 不能为空");
        }
        Set<String> channels = normalizeChannels(request.allowedChannels());
        if (channels.isEmpty() || !ALL_CHANNELS.containsAll(channels)) {
            throw new ImBusinessException("allowedChannels 仅支持 LOCAL、EMAIL");
        }
        String appCode = request.appCode().trim();
        ApplicationRow existing = findRow(appCode).orElse(null);
        String generatedSecret = StringUtils.hasText(request.appSecret())
                ? request.appSecret().trim()
                : existing == null ? generateSecret() : null;
        String encryptedSecret = generatedSecret == null
                ? existing.appSecretCiphertext()
                : secretCodec.encrypt(generatedSecret);
        String callbackSecret = StringUtils.hasText(request.callbackSecret())
                ? request.callbackSecret().trim()
                : generatedSecret;
        String encryptedCallbackSecret = callbackSecret == null
                ? existing == null ? encryptedSecret : existing.callbackSecretCiphertext()
                : secretCodec.encrypt(callbackSecret);
        LocalDateTime now = LocalDateTime.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("appCode", appCode)
                .addValue("appName", request.appName().trim())
                .addValue("tenantId", textOrDefault(request.tenantId(), "default"))
                .addValue("appSecretCiphertext", encryptedSecret)
                .addValue("allowedChannels", String.join(",", channels))
                .addValue("allowedIps", normalizeCsv(request.allowedIps(), "*"))
                .addValue("rateLimitPerMinute", Math.max(1, request.rateLimitPerMinute() == null ? 600 : request.rateLimitPerMinute()))
                .addValue("callbackUrl", blankToNull(request.callbackUrl()))
                .addValue("callbackSecretCiphertext", encryptedCallbackSecret)
                .addValue("status", textOrDefault(request.status(), "ENABLED").toUpperCase(Locale.ROOT))
                .addValue("now", now);
        jdbc.update("""
                INSERT INTO im_application (
                    app_code,app_name,tenant_id,app_secret_ciphertext,allowed_channels,allowed_ips,
                    rate_limit_per_minute,callback_url,callback_secret_ciphertext,status,created_time,updated_time
                ) VALUES (
                    :appCode,:appName,:tenantId,:appSecretCiphertext,:allowedChannels,:allowedIps,
                    :rateLimitPerMinute,:callbackUrl,:callbackSecretCiphertext,:status,:now,:now
                )
                ON DUPLICATE KEY UPDATE
                    app_name=VALUES(app_name),tenant_id=VALUES(tenant_id),
                    app_secret_ciphertext=VALUES(app_secret_ciphertext),
                    allowed_channels=VALUES(allowed_channels),allowed_ips=VALUES(allowed_ips),
                    rate_limit_per_minute=VALUES(rate_limit_per_minute),
                    callback_url=VALUES(callback_url),
                    callback_secret_ciphertext=VALUES(callback_secret_ciphertext),
                    status=VALUES(status),updated_time=VALUES(updated_time)
                """, params);
        return new ApplicationSecretResponse(appCode, generatedSecret, generatedSecret != null);
    }

    public ApplicationSecretResponse rotateSecret(String appCode) {
        ApplicationRow row = findRow(appCode).orElseThrow(() -> new ImBusinessException("IM 应用不存在"));
        String secret = generateSecret();
        String encrypted = secretCodec.encrypt(secret);
        jdbc.update("""
                UPDATE im_application
                SET app_secret_ciphertext=:secret,
                    callback_secret_ciphertext=:secret,
                    updated_time=:now
                WHERE app_code=:appCode
                """, Map.of("appCode", row.appCode(), "secret", encrypted, "now", LocalDateTime.now()));
        return new ApplicationSecretResponse(row.appCode(), secret, true);
    }

    public List<ApplicationView> list() {
        return jdbc.query("""
                SELECT app_code,app_name,tenant_id,allowed_channels,allowed_ips,rate_limit_per_minute,
                       callback_url,status,created_time,updated_time
                FROM im_application
                ORDER BY app_code
                """, (rs, rowNum) -> new ApplicationView(
                rs.getString("app_code"),
                rs.getString("app_name"),
                rs.getString("tenant_id"),
                split(rs.getString("allowed_channels")),
                split(rs.getString("allowed_ips")),
                rs.getInt("rate_limit_per_minute"),
                rs.getString("callback_url"),
                rs.getString("status"),
                rs.getTimestamp("created_time").toLocalDateTime(),
                rs.getTimestamp("updated_time").toLocalDateTime()
        ));
    }

    private java.util.Optional<ApplicationRow> findRow(String appCode) {
        try {
            ApplicationRow row = jdbc.queryForObject("""
                    SELECT app_code,app_name,tenant_id,app_secret_ciphertext,allowed_channels,allowed_ips,
                           rate_limit_per_minute,callback_url,callback_secret_ciphertext,status
                    FROM im_application
                    WHERE app_code=:appCode
                    LIMIT 1
                    """, Map.of("appCode", appCode), (rs, rowNum) -> new ApplicationRow(
                    rs.getString("app_code"),
                    rs.getString("app_name"),
                    rs.getString("tenant_id"),
                    rs.getString("app_secret_ciphertext"),
                    rs.getString("allowed_channels"),
                    rs.getString("allowed_ips"),
                    rs.getInt("rate_limit_per_minute"),
                    rs.getString("callback_url"),
                    rs.getString("callback_secret_ciphertext"),
                    rs.getString("status")
            ));
            return java.util.Optional.ofNullable(row);
        } catch (EmptyResultDataAccessException e) {
            return java.util.Optional.empty();
        }
    }

    private void applyRateLimit(AuthenticatedApplication application) {
        long minute = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond() / 60;
        String key = "im:open-api:rate:" + application.appCode() + ":" + minute;
        Long value = redis.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        if (value != null && value > application.rateLimitPerMinute()) {
            throw new ApplicationAuthenticationException("调用频率超过应用限额");
        }
    }

    private boolean ipAllowed(String sourceIp, Set<String> allowedIps) {
        if (allowedIps.isEmpty() || allowedIps.contains("*")) {
            return true;
        }
        if (!StringUtils.hasText(sourceIp)) {
            return false;
        }
        String ip = sourceIp.trim();
        return allowedIps.stream().anyMatch(rule -> matchesIpRule(ip, rule));
    }

    private boolean matchesIpRule(String ip, String rule) {
        if (ip.equals(rule)) {
            return true;
        }
        if (!rule.contains("/")) {
            return false;
        }
        try {
            String[] parts = rule.split("/", 2);
            byte[] address = InetAddress.getByName(ip).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            if (address.length != network.length || prefix < 0 || prefix > address.length * 8) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<String> normalizeChannels(Set<String> channels) {
        if (channels == null) {
            return Set.of();
        }
        return channels.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeCsv(Set<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return values.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.joining(","));
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record ApplicationRow(
            String appCode,
            String appName,
            String tenantId,
            String appSecretCiphertext,
            String allowedChannels,
            String allowedIps,
            int rateLimitPerMinute,
            String callbackUrl,
            String callbackSecretCiphertext,
            String status
    ) {
    }

    public record AuthenticatedApplication(
            String appCode,
            String appName,
            String tenantId,
            String appSecret,
            Set<String> allowedChannels,
            Set<String> allowedIps,
            int rateLimitPerMinute,
            String callbackUrl,
            String callbackSecret,
            boolean configurationFallback
    ) {
    }

    public record ApplicationUpsertRequest(
            String appCode,
            String appName,
            String tenantId,
            String appSecret,
            Set<String> allowedChannels,
            Set<String> allowedIps,
            Integer rateLimitPerMinute,
            String callbackUrl,
            String callbackSecret,
            String status
    ) {
    }

    public record ApplicationSecretResponse(
            String appCode,
            String secret,
            boolean secretChanged
    ) {
    }

    public record ApplicationView(
            String appCode,
            String appName,
            String tenantId,
            Set<String> allowedChannels,
            Set<String> allowedIps,
            int rateLimitPerMinute,
            String callbackUrl,
            String status,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }

    public static class ApplicationAuthenticationException extends RuntimeException {
        public ApplicationAuthenticationException(String message) {
            super(message);
        }
    }
}
