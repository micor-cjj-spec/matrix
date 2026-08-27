package single.cjj.erp.crm.lead.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.CreateRequest;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;
import single.cjj.erp.crm.lead.mapper.CrmLeadMapper;
import single.cjj.erp.event.service.BusinessEventOutboxService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmLeadServiceTest {

    @Mock
    private CrmLeadMapper mapper;
    @Mock
    private BusinessEventOutboxService outboxService;

    @Test
    void createLeadShouldNotRequireCustomerBusinessPartner() {
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(CrmLeadEntity.class))).thenReturn(1);

        CrmLeadService service =
                new CrmLeadService(mapper, outboxService);

        CrmLeadEntity result = service.create(
                new CreateRequest(
                        "T1", 1L, null, LocalDate.of(2026, 8, 27),
                        "医院数字化项目线索", "客户公司", "张三",
                        "13800000000", "a@example.com", "REFERRAL",
                        100L, new BigDecimal("500000"), "CNY",
                        LocalDate.of(2026, 9, 1)),
                100L);

        assertEquals(CrmLeadService.STATUS_NEW, result.getFstatus());
        assertEquals("客户公司", result.getFcompanyName());
        assertEquals(new BigDecimal("500000.00"), result.getFestimatedAmount());
        verifyNoInteractions(outboxService);
    }

    @Test
    void qualifyShouldPublishCrmLeadQualifiedEvent() {
        CrmLeadEntity lead = lead(10L, CrmLeadService.STATUS_NEW);
        when(mapper.selectByIdForUpdate(10L, "T1")).thenReturn(lead);
        when(mapper.updateById(lead)).thenReturn(1);

        CrmLeadService service =
                new CrmLeadService(mapper, outboxService);

        CrmLeadEntity result = service.qualify(10L, "T1", 100L);

        assertEquals(CrmLeadService.STATUS_QUALIFIED, result.getFstatus());
        assertNotNull(result.getFqualifiedTime());
        verify(outboxService).append(
                eq("T1"),
                eq(1L),
                eq("CRM"),
                eq("CRM_LEAD_QUALIFIED"),
                eq("CRM_LEAD"),
                eq(10L),
                anyLong(),
                eq("ERP_CRM_LEAD"),
                eq("LEAD-10"),
                eq(LocalDate.of(2026, 8, 27)),
                eq(100L),
                any()
        );
    }

    @Test
    void convertedLeadCannotBeDisqualified() {
        CrmLeadEntity lead = lead(10L, CrmLeadService.STATUS_CONVERTED);
        when(mapper.selectByIdForUpdate(10L, "T1")).thenReturn(lead);

        CrmLeadService service =
                new CrmLeadService(mapper, outboxService);

        BizException error = assertThrows(
                BizException.class,
                () -> service.disqualify(
                        10L, "T1", "无效", 100L));

        assertTrue(error.getMessage().contains("已转换"));
        verify(mapper, never()).updateById(any());
    }

    private CrmLeadEntity lead(Long id, String status) {
        CrmLeadEntity lead = new CrmLeadEntity();
        lead.setFid(id);
        lead.setFtenantId("T1");
        lead.setForgId(1L);
        lead.setFnumber("LEAD-" + id);
        lead.setFdate(LocalDate.of(2026, 8, 27));
        lead.setFname("线索");
        lead.setFstatus(status);
        lead.setFestimatedAmount(BigDecimal.ZERO);
        lead.setFversion(0);
        return lead;
    }
}
