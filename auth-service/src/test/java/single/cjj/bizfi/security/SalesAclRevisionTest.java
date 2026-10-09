package single.cjj.bizfi.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesAclRevisionTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SalesAclRevision revisions = new SalesAclRevision(redis);

    @Test
    void firstUseProvisionsRandomPositiveEvenRevision() {
        String key = SalesAclRevision.key("T1", 3L, 77L);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(key)).thenReturn("2048");
        assertEquals(2048L, revisions.currentOrCreate("T1", 3L, 77L));
        verify(values).setIfAbsent(eq(key), argThat(value -> {
            long n = Long.parseLong(value);
            return n > 0 && n % 2 == 0;
        }));
    }

    @Test
    void missingRevisionKeyMustFailClosed() {
        when(redis.opsForValue()).thenReturn(values);
        assertThrows(IllegalStateException.class,
                () -> revisions.current("T1", 3L, 77L));
    }

    @Test
    void grantChangeRequiresAtomicEvenToOddTransition() {
        String key = SalesAclRevision.key("T1", 3L, 77L);
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of(key)))).thenReturn(-1L);
        assertThrows(IllegalStateException.class,
                () -> revisions.beginChange("T1", 3L, 77L));
        verify(redis).execute(any(DefaultRedisScript.class), eq(List.of(key)));
    }

    @Test
    void finishingChangeMustProduceAnEvenVersion() {
        String key = SalesAclRevision.key("T1", 3L, 77L);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(key)).thenReturn(2048L);
        revisions.finishChange("T1", 3L, 77L);
        when(values.increment(key)).thenReturn(2049L);
        assertThrows(IllegalStateException.class,
                () -> revisions.finishChange("T1", 3L, 77L));
    }

    @Test
    void tenantNamesCannotCollideWithOrgAndUserComponents() {
        assertNotEquals(SalesAclRevision.key("a:b", 2L, 3L),
                SalesAclRevision.key("a", 2L, 3L));
    }
}
