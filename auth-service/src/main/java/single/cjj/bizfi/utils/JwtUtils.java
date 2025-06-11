package single.cjj.bizfi.utils;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtils {
    private static final String SECRET = "secret123456";
    private static final long EXPIRE = 3600 * 1000;

    public static String generateToken(Long userId, Long username) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("username", username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
}
