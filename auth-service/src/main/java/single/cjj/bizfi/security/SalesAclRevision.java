package single.cjj.bizfi.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/** Shared Redis revision contract with erp-service. Use UTF-8 string serializers. */
@Service
public class SalesAclRevision {
    private static final DefaultRedisScript<Long> BEGIN = new DefaultRedisScript<>(
            "local raw = redis.call('GET', KEYS[1]); "
          + "if not raw then return -1 end; "
          + "local v = tonumber(raw); "
          + "if not v or v % 2 ~= 0 then return -1 end; "
          + "return redis.call('INCR', KEYS[1])", Long.class);

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

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
        if (value == null) throw new IllegalStateException("Sales grant revision missing");
        return Long.parseLong(value);
    }

    /** A missing Redis key must never silently turn into version zero.
     *  Provision a fresh random even value once, so pre-restart tokens remain invalid.
     */
    public long currentOrCreate(String tenantId, Long orgId, Long userId) {
        String scopedKey = key(tenantId, orgId, userId);
        long initial = (random.nextLong(1L << 51) + 1L) * 2L;
        redis.opsForValue().setIfAbsent(scopedKey, Long.toString(initial));
        return current(tenantId, orgId, userId);
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
