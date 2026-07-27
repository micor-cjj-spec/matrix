package single.cjj.bizfi.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    /**
     * Token 过期时间：1 小时
     */
    public static final long EXPIRE = 3600 * 1000;

    private final Key signingKey;

    public JwtUtils(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = createSigningKey(secret);
    }

    public String generateToken(Long userId, Long username) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("username", username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 Token
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws JwtException token 无效或已过期
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key createSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.jwt.secret must not be blank");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must contain at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
