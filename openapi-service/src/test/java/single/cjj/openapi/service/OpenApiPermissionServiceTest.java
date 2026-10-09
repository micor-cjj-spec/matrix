package single.cjj.openapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.exception.OpenApiCallException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiPermissionServiceTest {

    private final OpenApiPermissionService service = new OpenApiPermissionService(new ObjectMapper());

    @Test
    void shouldResolveTenantOrganizationAndBookScopes() {
        OpenApiApp app = new OpenApiApp();
        app.setTenantId("tenant-a");
        OpenApiGrant grant = new OpenApiGrant();
        grant.setDataPermissionJson("""
                {
                  "allowedStatuses": ["POSTED"],
                  "organizationIds": ["ORG-001", "ORG-002"],
                  "bookIds": ["BOOK-001"],
                  "maxHistoryMonths": 18
                }
                """);

        OpenApiPermissionService.VoucherPermission permission =
                service.resolveVoucherPermission(app, grant);

        assertEquals("tenant-a", permission.tenantId());
        assertEquals(18, permission.maxHistoryMonths());
        assertTrue(permission.allowedStatuses().contains("POSTED"));
        assertTrue(permission.allowsOrganization("ORG-002"));
        assertFalse(permission.allowsOrganization("ORG-999"));
        assertTrue(permission.allowsBook("BOOK-001"));
    }

    @Test
    void shouldResolveVoucherWriteLimits() {
        OpenApiApp app = new OpenApiApp();
        app.setTenantId("tenant-write");
        OpenApiGrant grant = new OpenApiGrant();
        grant.setDataPermissionJson("""
                {
                  "organizationIds": ["ORG-W"],
                  "bookIds": ["BOOK-W"],
                  "maxLinesPerVoucher": 80,
                  "dailyWriteQuota": 1200
                }
                """);

        OpenApiPermissionService.VoucherWritePermission permission =
                service.resolveVoucherWritePermission(app, grant);

        assertEquals("tenant-write", permission.tenantId());
        assertEquals(80, permission.maxLinesPerVoucher());
        assertEquals(1200, permission.dailyWriteQuota());
        assertTrue(permission.allowsOrganization("ORG-W"));
        assertFalse(permission.allowsOrganization("ORG-X"));
        assertTrue(permission.allowsBook("BOOK-W"));
    }

    @Test
    void shouldClampVoucherWriteLimits() {
        OpenApiApp app = new OpenApiApp();
        app.setTenantId("default");
        OpenApiGrant grant = new OpenApiGrant();
        grant.setDataPermissionJson("""
                {
                  "maxLinesPerVoucher": 5000,
                  "dailyWriteQuota": 5000000
                }
                """);

        OpenApiPermissionService.VoucherWritePermission permission =
                service.resolveVoucherWritePermission(app, grant);

        assertEquals(500, permission.maxLinesPerVoucher());
        assertEquals(1000000, permission.dailyWriteQuota());
        assertTrue(permission.allowsOrganization("ANY-ORG"));
        assertTrue(permission.allowsBook("ANY-BOOK"));
    }

    @Test
    void shouldTreatMissingOrganizationAndBookScopesAsTenantWildcard() {
        OpenApiApp app = new OpenApiApp();
        app.setTenantId("default");
        OpenApiGrant grant = new OpenApiGrant();
        grant.setDataPermissionJson("{\"allowedStatuses\":[\"POSTED\"]}");

        OpenApiPermissionService.VoucherPermission permission =
                service.resolveVoucherPermission(app, grant);

        assertTrue(permission.allowsOrganization("ANY-ORG"));
        assertTrue(permission.allowsBook("ANY-BOOK"));
    }

    @Test
    void shouldRejectStatusesOutsideSystemMaximum() {
        OpenApiApp app = new OpenApiApp();
        app.setTenantId("default");
        OpenApiGrant grant = new OpenApiGrant();
        grant.setDataPermissionJson("{\"allowedStatuses\":[\"DRAFT\"]}");

        OpenApiCallException exception = assertThrows(
                OpenApiCallException.class,
                () -> service.resolveVoucherPermission(app, grant)
        );
        assertEquals("OPENAPI_40303", exception.getCode());
    }
}
