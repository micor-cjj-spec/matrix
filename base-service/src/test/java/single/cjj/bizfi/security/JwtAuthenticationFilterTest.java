package single.cjj.bizfi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void shouldMapOrganizationDepartmentAndRoleClaimsWithoutCollisions() {
        Claims claims = Jwts.claims();
        claims.put("organizationIds", List.of(88L));
        claims.put("departmentIds", List.of(9L));
        claims.put("roles", List.of("finance_admin"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(StringRedisTemplate.class));
        Set<String> authorities = filter.resolveAuthorities(claims).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "org:88",
                "organization:88",
                "team:88",
                "department:9",
                "finance_admin",
                "ROLE_FINANCE_ADMIN"
        ), authorities);
    }
}
