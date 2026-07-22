package single.cjj.scheduler.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class SchedulerInternalAuthFilter extends OncePerRequestFilter {

    private final byte[] expectedToken;

    public SchedulerInternalAuthFilter(
            @Value("${matrix.scheduler.internal-token}") String internalToken) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("matrix.scheduler.internal-token 不能为空");
        }
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !(path.equals("/scheduler/executors/register")
                || path.equals("/scheduler/executors/heartbeat")
                || path.startsWith("/scheduler/callback/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String actual = request.getHeader("X-Scheduler-Internal-Token");
        boolean valid = actual != null && MessageDigest.isEqual(
                expectedToken,
                actual.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":401,\"message\":\"调度内部凭证无效\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
