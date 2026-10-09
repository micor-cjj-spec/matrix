package single.cjj.erp.sales.commercial.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.erp.sales.commercial.dto.SalesCommercialContracts.*;
import single.cjj.erp.sales.commercial.entity.SalesQuoteEntity;
import single.cjj.erp.sales.commercial.security.SalesAccessGuard;
import single.cjj.erp.sales.commercial.security.SalesAccessGuard.Permission;
import single.cjj.erp.sales.commercial.service.SalesCommercialService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesCommercialControllerTest {
    @Mock SalesCommercialService service;
    @Mock SalesAccessGuard guard;

    private SalesCommercialController controller() {
        return new SalesCommercialController(service, guard);
    }

    private CreateQuote createQuote() {
        return new CreateQuote("T1", 3L, null, 99L, 100L, "CNY",
                "QUOTE", null, LocalDate.now().plusDays(7), "DDP", "NET30",
                List.of(new QuoteLine("服务", null, BigDecimal.ONE, new BigDecimal("10"), BigDecimal.ZERO)));
    }

    @Test
    void invalidSignedIdentityMustFailBeforeQuoteCreation() {
        when(guard.authorize("Bearer invalid", "T1", 3L, Permission.WRITE))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        assertThrows(ResponseStatusException.class,
                () -> controller().createQuote("Bearer invalid", createQuote()));
        verifyNoInteractions(service);
    }

    @Test
    void identityMustBeVerifiedBeforeDatabaseRead() {
        when(guard.authorizeTenant("Bearer token", "T1", Permission.READ))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class,
                () -> controller().quote("Bearer token", 42L, "T1"));
        verifyNoInteractions(service);
    }

    @Test
    void quoteApprovalMustRequireApproverPermissionOnDocumentOrganization() {
        SalesQuoteEntity quote = new SalesQuoteEntity();
        quote.setFid(42L); quote.setFtenantId("T1"); quote.setForgId(3L);
        when(service.quoteDetail(42L, "T1")).thenReturn(new QuoteDetail(quote, List.of()));
        when(guard.authorize("Bearer token", "T1", 3L, Permission.APPROVE))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class,
                () -> controller().changeQuote("Bearer token", 42L, "approve", "T1"));
        verify(service, never()).transitionQuote(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void contractCreationMustRequireWriteAccessOnSourceQuoteOrganization() {
        SalesQuoteEntity q = new SalesQuoteEntity();
        q.setFid(42L); q.setFtenantId("T1"); q.setForgId(3L);
        when(service.quoteDetail(42L, "T1")).thenReturn(new QuoteDetail(q, List.of()));
        when(guard.authorize("Bearer token", "T1", 3L, Permission.WRITE))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        CreateContract request = new CreateContract("T1", null, 42L, "合同", LocalDate.now(), LocalDate.now());
        assertThrows(ResponseStatusException.class,
                () -> controller().createContract("Bearer token", request));
        verify(service, never()).createContract(any(), any());
    }
}
