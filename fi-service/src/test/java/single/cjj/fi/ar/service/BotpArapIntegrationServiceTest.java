package single.cjj.fi.ar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.fi.ar.dto.BotpArapContracts.ArapWritebackRequest;
import single.cjj.fi.ar.dto.BotpArapContracts.PaymentApplicationCreateRequest;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.mapper.BotpArapIntegrationMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotpArapIntegrationServiceTest {

    @Mock
    private BizfiFiArapDocMapper arapDocMapper;
    @Mock
    private BotpArapIntegrationMapper lockMapper;

    @Test
    void shouldReserveThenConvertReservationToAppliedAmount() {
        BizfiFiArapDoc source = new BizfiFiArapDoc();
        source.setFid(1L);
        source.setFdoctype("AP");
        source.setFnumber("AP-001");
        source.setFdate(LocalDate.of(2026, 7, 22));
        source.setFcounterparty("SUPPLIER-1");
        source.setFamount(new BigDecimal("1000"));
        source.setFstatus("AUDITED");
        source.setFappliedAmount(new BigDecimal("100"));
        source.setFreservedAmount(new BigDecimal("100"));
        source.setFremainingAmount(new BigDecimal("800"));
        source.setFversion(0);

        when(arapDocMapper.selectOne(any())).thenReturn(null);
        when(lockMapper.selectByIdForUpdate(1L)).thenReturn(source);
        when(arapDocMapper.updateById(any())).thenReturn(1);
        when(arapDocMapper.insert(any())).thenAnswer(invocation -> {
            BizfiFiArapDoc target = invocation.getArgument(0);
            target.setFid(2L);
            return 1;
        });
        when(arapDocMapper.selectById(1L)).thenReturn(source);

        BotpArapIntegrationService service = new BotpArapIntegrationService(arapDocMapper, lockMapper);
        BizfiFiArapDoc target = service.createPaymentApplication(new PaymentApplicationCreateRequest(
                "botp:default:BOTP-1:0",
                "MATRIX",
                "FI_AP_DOC",
                "1",
                "BOTP-1",
                "AP-001",
                "SUPPLIER-1",
                new BigDecimal("600"),
                "BANK",
                LocalDate.of(2026, 7, 30),
                "admin"
        ));

        assertEquals(new BigDecimal("700"), source.getFreservedAmount());
        assertEquals(new BigDecimal("200"), source.getFremainingAmount());
        assertEquals("PARTIAL", source.getFpushStatus());
        assertEquals("AP_PAYMENT_APPLY", target.getFdoctype());
        assertEquals("botp:default:BOTP-1:0", target.getFbotpIdempotencyKey());

        BizfiFiArapDoc writtenBack = service.recomputeWriteback(
                1L,
                new ArapWritebackRequest(new BigDecimal("700"), new BigDecimal("600"), "BOTP-1")
        );

        assertEquals(new BigDecimal("700"), writtenBack.getFappliedAmount());
        assertEquals(new BigDecimal("100"), writtenBack.getFreservedAmount());
        assertEquals(new BigDecimal("200"), writtenBack.getFremainingAmount());
        assertEquals("PARTIAL", writtenBack.getFpushStatus());
    }
}
