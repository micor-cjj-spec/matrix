package single.cjj.workflow.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.workflow.api.WorkflowContracts;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesWorkflowTaskGuardTest {
    private static final String SECRET="matrix-test-signing-key-32-bytes-minimum-only";
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String,String> sessions=mock(RedisTemplate.class);
    private final StringRedisTemplate revisions=mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String,String> sessionValues=mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String,String> revisionValues=mock(ValueOperations.class);
    private final SalesWorkflowTaskGuard guard=new SalesWorkflowTaskGuard(SECRET,sessions,revisions);

    private WorkflowContracts.InstanceResponse instance() {
        return new WorkflowContracts.InstanceResponse("wf-1","T1","sales_quote_v1",1,
              "MATRIX_ERP","SALES_QUOTE","100","77","review", "RUNNING",0,
              Map.of("organizationId","3"),null,null);
    }

    private String token(String role) {
        String raw=Jwts.builder().claim("id",99L).claim("tenantId","T1")
            .claim("organizationIds",List.of(3L)).claim("roles",List.of(role))
            .claim("salesGrantRevision",2048L)
            .setExpiration(new Date(System.currentTimeMillis()+600_000L))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                    SignatureAlgorithm.HS256).compact();
        when(sessions.opsForValue()).thenReturn(sessionValues);
        when(sessionValues.get("token:"+raw)).thenReturn("99");
        when(revisions.opsForValue()).thenReturn(revisionValues);
        when(revisionValues.get("sales:acl:revision:VDE:3:99")).thenReturn("2048");
        return "Bearer "+raw;
    }

    @Test
    void activeApproverMayCompleteTaskAsSelf() {
        assertTrue(guard.authorize(token("SALES_APPROVER"),instance(),"99")
                .contains("SALES_APPROVER"));
    }

    @Test
    void editorCannotApproveEvenWhenRequestHeaderClaimsOtherwise() {
        assertEquals(403,code(()->guard.authorize(token("SALES_EDITOR"),instance(),"99")));
    }

    @Test
    void spoofedOperatorIsRejected() {
        assertEquals(403,code(()->guard.authorize(token("SALES_APPROVER"),instance(),"77")));
    }

    @Test
    void lostRedisSessionFailsClosed() {
        String token=token("SALES_APPROVER");
        when(sessionValues.get("token:"+token.substring(7))).thenReturn(null);
        assertEquals(401,code(()->guard.authorize(token,instance(),"99")));
    }

    @Test
    void revokedRevisionFailsClosed() {
        String token=token("SALES_APPROVER");
        when(revisionValues.get("sales:acl:revision:VDE:3:99")).thenReturn("2050");
        assertEquals(401,code(()->guard.authorize(token,instance(),"99")));
    }

    private int code(Runnable action) {
        return assertThrows(ResponseStatusException.class,action::run).getStatusCode().value();
    }
}
