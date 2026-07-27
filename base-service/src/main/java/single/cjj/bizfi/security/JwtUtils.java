package single.cjj.bizfi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtils {

    private static volatile Key signingKey;

    public JwtUtils(@Value("${security.jwt.secret}") String secret) {
        signingKey = createSigningKey(secret);
    }

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
