package single.cjj.share.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class JwtUtils {
    private static final String SECRET = "8U0n8cxtkN2yeFfYRCFNL97kUbwQlm3Qw5XKyr0oyYg=@";

    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
