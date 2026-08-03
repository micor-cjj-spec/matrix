package single.cjj.bizfi.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = JwtUtils.parseToken(token);
                String userId = String.valueOf(claims.get("id"));
                String cached = redisTemplate.opsForValue().get("token:" + token);
                if (StringUtils.hasText(cached) && cached.equals(userId)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, resolveAuthorities(claims));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> resolveAuthorities(Claims claims) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addAuthorityValues(values, claims.get("authorities"), false);
        addAuthorityValues(values, claims.get("permissions"), false);
        addAuthorityValues(values, claims.get("roles"), true);
        addAuthorityValues(values, claims.get("role"), true);
        addOrganizationValues(values, claims.get("organizationIds"));
        addOrganizationValues(values, claims.get("organizationId"));
        addOrganizationValues(values, claims.get("orgIds"));
        addOrganizationValues(values, claims.get("orgId"));
        return values.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
    }

    private void addAuthorityValues(Set<String> target, Object raw, boolean roleClaim) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Collection<?> collection) {
            collection.forEach(item -> addAuthorityValues(target, item, roleClaim));
            return;
        }
        String value = raw.toString();
        if (!StringUtils.hasText(value)) {
            return;
        }
        for (String item : value.split("[,\\s]+")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String authority = item.trim();
            target.add(authority);
            if (roleClaim && !authority.toUpperCase(Locale.ROOT).startsWith("ROLE_")) {
                target.add("ROLE_" + authority.toUpperCase(Locale.ROOT));
            }
        }
    }

    private void addOrganizationValues(Set<String> target, Object raw) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Collection<?> collection) {
            collection.forEach(item -> addOrganizationValues(target, item));
            return;
        }
        String value = raw.toString();
        if (!StringUtils.hasText(value)) {
            return;
        }
        for (String item : value.split("[,\\s]+")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String organizationId = item.trim();
            target.add("org:" + organizationId);
            target.add("organization:" + organizationId);
        }
    }
}
