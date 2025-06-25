package single.cjj.bizfi.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import single.cjj.bizfi.utils.JwtUtils;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!StringUtils.hasText(token)) {
            response.sendRedirect("/login");
            return false;
        }

        String redisKey = "token:" + token;
        String userId = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(userId)) {
            response.sendRedirect("/login");
            return false;
        }

        try {
            JwtUtils.parseToken(token);
        } catch (JwtException e) {
            redisTemplate.delete(redisKey);
            response.sendRedirect("/login");
            return false;
        }

        // 刷新过期时间
        redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);
        return true;
    }
}
