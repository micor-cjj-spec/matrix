package single.cjj.bizfi.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtils {

    /**
     * Token 过期时间：1 小时
     */
    public static final long EXPIRE = 3600 * 1000;

    private static volatile Key signingKey;

    public JwtUtils(@Value("${security.jwt.secret}") String secret) {
        signingKey = createSigningKey(secret);
    }

    public static String generateToken(Long userId, Long username) {
        return generateToken(userId, username, null, null);
    }

    public static String generateToken(
            Long userId,
            Long username,
            Long organizationId,
            Long departmentId
    ) {
        JwtBuilder builder = Jwts.builder()
                .claim("id", userId)
                .claim("username", username);

        if (organizationId != null && organizationId > 0) {
            builder.claim("organizationIds", List.of(organizationId));
        }

        Set<String> authorities = new LinkedHashSet<>();
        if (organizationId != null && organizationId > 0) {
            authorities.add("team:" + organizationId);
        }
        if (departmentId != null && departmentId > 0) {
            authorities.add("department:" + departmentId);
        }
        if (!authorities.isEmpty()) {
            builder.claim("authorities", new ArrayList<>(authorities));
        }

        return builder
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(requireSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 Token
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws JwtException token 无效或已过期
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(requireSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static Key createSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.jwt.secret must not be blank");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must contain at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static Key requireSigningKey() {
        Key key = signingKey;
        if (key == null) {
            throw new IllegalStateException("JWT signing key has not been initialized");
        }
        return key;
    }
}
