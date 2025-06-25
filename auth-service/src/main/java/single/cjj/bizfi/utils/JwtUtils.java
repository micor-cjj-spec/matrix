package single.cjj.bizfi.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;

import java.util.Date;

public class JwtUtils {
    private static final String SECRET = "secret123456";
    /**
     * Token 过期时间：1 小时
     */
    public static final long EXPIRE = 3600 * 1000;

    public static String generateToken(Long userId, Long username) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("username", username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET)
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
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
