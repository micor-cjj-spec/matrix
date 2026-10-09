package single.cjj.bizfi.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import single.cjj.bizfi.entity.BizfiBaseUser;
import single.cjj.bizfi.mapper.SalesRoleGrantMapper;
import single.cjj.bizfi.security.SalesAclRevision;
import single.cjj.bizfi.service.impl.BizfiAuthLoginServiceImpl;
import single.cjj.bizfi.utils.JwtUtils;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesRoleIssuerTest {
    private static final String SECRET = "matrix-test-signing-key-32-bytes-minimum-only";

    private BizfiBaseUser user() {
        BizfiBaseUser u = new BizfiBaseUser();
        u.setFid(77L);
        u.setFtid(300L);
        u.setFdptid(900L);
        return u;
    }

    private String issue(BizfiAuthLoginServiceImpl svc, BizfiBaseUser user) {
        return ReflectionTestUtils.invokeMethod(svc, "issueToken", user);
    }

    @Test
    void disabledIssuerRetainsLegacyClaimsWithoutGrantLookup() {
        new JwtUtils(SECRET);
        SalesRoleGrantMapper grants = mock(SalesRoleGrantMapper.class);
        BizfiAuthLoginServiceImpl svc = new BizfiAuthLoginServiceImpl();
        ReflectionTestUtils.setField(svc, "salesRoleGrantMapper", grants);
        ReflectionTestUtils.setField(svc, "salesRoleIssuerEnabled", false);
        Claims claims = JwtUtils.parseToken(issue(svc, user()));
        assertEquals("77", claims.get("id").toString());
        assertNull(claims.get("tenantId"));
        assertNull(claims.get("roles"));
        verifyNoInteractions(grants);
    }

    @Test
    void enabledIssuerOnlySignsPersistedAllowlistedRolesForUserOrgAndTenant() {
        new JwtUtils(SECRET);
        SalesRoleGrantMapper grants = mock(SalesRoleGrantMapper.class);
        when(grants.activeRoles(77L, "T1", 300L)).thenReturn(
                List.of("SALES_EDITOR", "SALES_EDITOR", "SUPERUSER", "SALES_VIEWER"));
        BizfiAuthLoginServiceImpl svc = new BizfiAuthLoginServiceImpl();
        ReflectionTestUtils.setField(svc, "salesRoleGrantMapper", grants);
        SalesAclRevision revision = mock(SalesAclRevision.class);
        when(revision.current( "T1", 300L, 77L)).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "salesAclRevision", revision);
        ReflectionTestUtils.setField(svc, "salesRoleIssuerEnabled", true);
        ReflectionTestUtils.setField(svc, "salesRoleTenantId", "T1");
        Claims claims = JwtUtils.parseToken(issue(svc, user()));
        assertEquals("T1", claims.get("tenantId"));
        assertEquals(List.of("SALES_EDITOR", "SALES_VIEWER"), claims.get("roles"));
        assertEquals(0, ((Number) claims.get("salesGrantRevision")).intValue());
        assertEquals(List.of(300), claims.get("organizationIds"));
        verify(grants).activeRoles(77L, "T1", 300L);
    }

    @Test
    void userWithoutOrganizationHasNoElevatedGrant() {
        new JwtUtils(SECRET);
        SalesRoleGrantMapper grants = mock(SalesRoleGrantMapper.class);
        BizfiAuthLoginServiceImpl svc = new BizfiAuthLoginServiceImpl();
        ReflectionTestUtils.setField(svc, "salesRoleGrantMapper", grants);
        ReflectionTestUtils.setField(svc, "salesRoleIssuerEnabled", true);
        ReflectionTestUtils.setField(svc, "salesRoleTenantId", "T1");
        BizfiBaseUser u = user();
        u.setFtid(null);
        Claims claims = JwtUtils.parseToken(issue(svc, u));
        assertNull(claims.get("roles"));
        verifyNoInteractions(grants);
    }
}
