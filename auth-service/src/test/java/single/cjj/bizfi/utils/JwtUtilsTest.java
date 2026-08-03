package single.cjj.bizfi.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JwtUtilsTest {

    private static final String SECRET = "matrix-phase3c-jwt-test-secret-key-32-bytes-minimum";

    @BeforeAll
    static void initializeSigningKey() {
        new JwtUtils(SECRET);
    }

    @Test
    void shouldIncludeTeamAndDepartmentContextWithoutPollutingRoles() {
        String token = JwtUtils.generateToken(1001L, 1001L, 88L, 9L);

        Claims claims = JwtUtils.parseToken(token);
        List<?> organizationIds = claims.get("organizationIds", List.class);
        List<?> departmentIds = claims.get("departmentIds", List.class);

        assertEquals("1001", String.valueOf(claims.get("id")));
        assertEquals(List.of("88"), organizationIds.stream().map(String::valueOf).toList());
        assertEquals(List.of("9"), departmentIds.stream().map(String::valueOf).toList());
        assertFalse(claims.containsKey("authorities"));
        assertFalse(claims.containsKey("roles"));
    }

    @Test
    void shouldRemainCompatibleWithLegacyTokenMethod() {
        String token = JwtUtils.generateToken(1001L, 1001L);

        Claims claims = JwtUtils.parseToken(token);

        assertEquals("1001", String.valueOf(claims.get("id")));
        assertFalse(claims.containsKey("organizationIds"));
        assertFalse(claims.containsKey("departmentIds"));
        assertFalse(claims.containsKey("authorities"));
    }
}
