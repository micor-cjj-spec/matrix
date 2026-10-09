package single.cjj.bizfi.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Shared Redis revision contract with erp-service. Use UTF-8 string serializers. */
@Service
public class SalesAclRevision {
    private static final DefaultRedisScript<Long> BEGIN = new DefaultRedisScript<>(
            "local v = tonumber(redis.call('GET', KEYS[1]) or '0'); "
          + "if v % 2 ~= 0 then return -1 end; "
          + "return redis.call('INCR', KEYS[1])", Long.class);

    private final StringRedisTemplate redis;

    public SalesAclRevision(StringRedisTemplate redis) { this.redis = redis; }

    public static String key(String tenantId, Long orgId, Long userId) {
        if (!StringUtils.hasText(tenantId) || orgId == null || userId == null) {
            throw new IllegalArgumentException("Grant scope must include tenant, org and user");
        }
        String tenantKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tenantId.getBytes(StandardCharsets.UTF_8));
        return "sales:acl:revision:" + tenantKey + ":" + orgId + ":" + userId;
    }

    public long current(String tenantId, Long orgId, Long userId) {
        String value = redis.opsForValue().get(key(tenantId, orgId, userId));
        return value == null ? 0L : Long.parseLong(value);
    }

    /** Atomically moves the revision to odd (locked). Return true only after acquiring it. */
    public void beginChange(String tenantId, Long orgId, Long userId) {
        Long result = redis.execute(BEGIN, List.of(key(tenantId, orgId, userId)));
        if (result == null || result < 0 || result % 2 != 1) {
            throw new IllegalStateException("Sales grant revision already changing or Redis unavailable");
        }
    }

    /** Ends the locked phase; Redis failure leaves the revision odd (fail-closed). */
    public void finishChange(String tenantId, Long orgId, Long userId) {
        Long result = redis.opsForValue().increment(key(tenantId, orgId, userId));
        if (result == null || result % 2 != 0) {
            throw new IllegalStateException("Sales role revision cannot be made active");
        }
    }
}
