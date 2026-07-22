package single.cjj.openapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import single.cjj.openapi.dto.VoucherWriteCreateRequest;
import single.cjj.openapi.dto.VoucherWriteLineRequest;
import single.cjj.openapi.dto.VoucherWriteStatusResponse;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiDefinition;
import single.cjj.openapi.entity.OpenApiGrant;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.exception.OpenApiCallException;
import single.cjj.openapi.mapper.OpenApiWriteRequestLineMapper;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;
import single.cjj.openapi.mapper.OpenApiWriteStatusLogMapper;
import single.cjj.openapi.security.OpenApiContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OpenApiVoucherWriteServiceTest {

    private OpenApiWriteRequestMapper requestMapper;
    private OpenApiPermissionService permissionService;
    private OpenApiVoucherWriteService service;
    private OpenApiContext context;

    @BeforeEach
    void setUp() {
        requestMapper = Mockito.mock(OpenApiWriteRequestMapper.class);
        OpenApiWriteRequestLineMapper lineMapper = Mockito.mock(OpenApiWriteRequestLineMapper.class);
        OpenApiWriteStatusLogMapper logMapper = Mockito.mock(OpenApiWriteStatusLogMapper.class);
        permissionService = Mockito.mock(OpenApiPermissionService.class);
        OpenApiWriteStateService stateService = Mockito.mock(OpenApiWriteStateService.class);
        service = new OpenApiVoucherWriteService(
                requestMapper, lineMapper, logMapper, permissionService, stateService
        );

        OpenApiApp app = new OpenApiApp();
        app.setId(10L);
        app.setAppId("app-test");
        app.setTenantId("tenant-a");
        OpenApiGrant grant = new OpenApiGrant();
        context = new OpenApiContext("req-test", app, new OpenApiDefinition(), grant);
        when(permissionService.resolveVoucherWritePermission(app, grant)).thenReturn(
                new OpenApiPermissionService.VoucherWritePermission(
                        "tenant-a", Set.of("ORG-1"), Set.of("BOOK-1"), 200, 10000
                )
        );
    }

    @Test
    void shouldReturnExistingRequestForSameIdempotencyBody() {
        OpenApiWriteRequest existing = new OpenApiWriteRequest();
        existing.setRequestId("vwr-existing");
        existing.setRequestBodyHash("abc123");
        existing.setStatus(OpenApiWriteStateService.ACCEPTED);
        existing.setCreatedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        when(requestMapper.selectOne(any())).thenReturn(existing);

        VoucherWriteStatusResponse response = service.accept(context, validRequest(), "abc123");

        assertEquals("vwr-existing", response.getRequestId());
        assertEquals(OpenApiWriteStateService.ACCEPTED, response.getStatus());
    }

    @Test
    void shouldRejectReusedIdempotencyKeyWithDifferentBody() {
        OpenApiWriteRequest existing = new OpenApiWriteRequest();
        existing.setRequestId("vwr-existing");
        existing.setRequestBodyHash("first-body");
        existing.setStatus(OpenApiWriteStateService.ACCEPTED);
        when(requestMapper.selectOne(any())).thenReturn(existing);

        OpenApiCallException exception = assertThrows(
                OpenApiCallException.class,
                () -> service.accept(context, validRequest(), "different-body")
        );

        assertEquals("OPENAPI_VOUCHER_40901", exception.getCode());
    }

    @Test
    void shouldRejectUnbalancedVoucher() {
        VoucherWriteCreateRequest request = validRequest();
        request.getLines().get(1).setCreditAmount(new BigDecimal("90.00"));

        OpenApiCallException exception = assertThrows(
                OpenApiCallException.class,
                () -> service.accept(context, request, "body-hash")
        );

        assertEquals("OPENAPI_VOUCHER_40001", exception.getCode());
    }

    private VoucherWriteCreateRequest validRequest() {
        VoucherWriteLineRequest debit = new VoucherWriteLineRequest();
        debit.setLineNo(1);
        debit.setAccountCode("660201");
        debit.setSummary("差旅费");
        debit.setDebitAmount(new BigDecimal("100.00"));
        debit.setCreditAmount(BigDecimal.ZERO);

        VoucherWriteLineRequest credit = new VoucherWriteLineRequest();
        credit.setLineNo(2);
        credit.setAccountCode("224101");
        credit.setSummary("应付员工款");
        credit.setDebitAmount(BigDecimal.ZERO);
        credit.setCreditAmount(new BigDecimal("100.00"));

        VoucherWriteCreateRequest request = new VoucherWriteCreateRequest();
        request.setExternalBizNo("EXP-001");
        request.setIdempotencyKey("expense:EXP-001");
        request.setOrganizationId("ORG-1");
        request.setBookId("BOOK-1");
        request.setVoucherDate(LocalDate.of(2026, 7, 22));
        request.setSummary("员工差旅报销");
        request.setLines(List.of(debit, credit));
        return request;
    }
}
