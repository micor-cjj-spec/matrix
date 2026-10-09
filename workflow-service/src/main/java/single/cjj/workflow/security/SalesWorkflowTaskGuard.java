package single.cjj.workflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.workflow.api.WorkflowContracts;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

/**
 * Workflow task actions for ERP sales require a real signed and live SALES_APPROVER.
 * Never trust caller-supplied X-User-Roles or operatorId for sales.
 */
@Component
@ConditionalOnProperty(prefix="matrix.sales.workflow",name="internal-enabled",havingValue="true")
public class SalesWorkflowTaskGuard {
    private final Key key;
    private final RedisTemplate<String,String> sessions;
    private final StringRedisTemplate revisions;

    public SalesWorkflowTaskGuard(@Value("${security.jwt.secret:}") String secret,
                                  RedisTemplate<String,String> sessions,
                                  StringRedisTemplate revisions) {
        if(!StringUtils.hasText(secret)
           || secret.getBytes(StandardCharsets.UTF_8).length<32)
            throw new IllegalStateException("Signed sales Workflow needs auth-service JWT secret");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.sessions=sessions;this.revisions=revisions;
    }

    public Set<String> authorize(String bearer,WorkflowContracts.InstanceResponse instance,
                                 String requestedOperator) {
        if(!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sales workflow requires JWT");
        final Claims claims;
        try {
            claims=Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(bearer.substring(7)).getBody();
        }catch(JwtException|IllegalArgumentException ex){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"JWT invalid");
        }
        final Long operator;
        final Long orgId;
        try {
            operator=Long.valueOf(String.valueOf(claims.get("id")));
            Object orgs=claims.get("organizationIds");
            if(!(orgs instanceof Collection<?> values)||values.size()!=1)
                throw new IllegalArgumentException("single org required");
            orgId=Long.valueOf(String.valueOf(values.iterator().next()));
            if(operator<=0||orgId<=0)throw new IllegalArgumentException("positive IDs required");
        }catch(RuntimeException ex){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"JWT missing identity scope");
        }
        if(!operator.toString().equals(requestedOperator)
                || !instance.tenantId().equals(claims.get("tenantId"))
                || !orgId.toString().equals(String.valueOf(instance.variables().get("organizationId")))
                || operator.toString().equals(instance.initiatorId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sales workflow actor scope invalid");
        }
        Set<String> roles=new HashSet<>();
        Object rawRoles=claims.get("roles");
        if(rawRoles instanceof Collection<?> values) {
            for(Object value:values) {
                String role=String.valueOf(value).trim().toUpperCase(Locale.ROOT);
                roles.add(role.startsWith("ROLE_")?role.substring(5):role);
            }
        }
        if(!roles.contains("SALES_APPROVER")&&!roles.contains("SALES_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sales approval role required");
        }
        Object version=claims.get("salesGrantRevision");
        if(!(version instanceof Number number)||number.longValue()<0
                ||number.longValue()%2!=0)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sales authorization revision missing");
        try {
            String currentSession=sessions.opsForValue().get("token:"+bearer.substring(7));
            if(!operator.toString().equals(currentSession))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session revoked");
            String tenantKey=Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(instance.tenantId().getBytes(StandardCharsets.UTF_8));
            String revisionKey="sales:acl:revision:"+tenantKey+":"+orgId+":"+operator;
            String currentVersion=revisions.opsForValue().get(revisionKey);
            if(currentVersion==null || Long.parseLong(currentVersion)!=number.longValue())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sales grant revoked");
        }catch(ResponseStatusException ex){throw ex;}
        catch(RuntimeException ex){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cannot verify sales grant");
        }
        return Set.copyOf(roles);
    }
}
