package single.cjj.erp.crm.opportunity.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;
import single.cjj.erp.crm.lead.mapper.CrmLeadMapper;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.CreateRequest;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;
import single.cjj.erp.crm.opportunity.mapper.CrmOpportunityMapper;
import single.cjj.erp.crm.support.CustomerPartnerValidator;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmOpportunityServiceTest {

    @Mock
    private CrmOpportunityMapper mapper;
    @Mock
    private CrmLeadMapper leadMapper;
    @Mock
    private CustomerPartnerValidator partnerValidator;
    @Mock
    private BusinessEventOutboxService outboxService;

    @Test
    void createFromQualifiedLeadShouldSnapshotCustomerAndConvertLead() {
        BusinessPartnerDetail partner = partner();
        when(partnerValidator.requireActiveCustomer(900L, "T1"))
                .thenReturn(partner);

        CrmLeadEntity lead = lead("QUALIFIED");
        when(leadMapper.selectByIdForUpdate(100L, "T1"))
                .thenReturn(lead);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(CrmOpportunityEntity.class))).thenReturn(1);
        when(leadMapper.updateById(lead)).thenReturn(1);

        CrmOpportunityService service = service();

        CrmOpportunityEntity result = service.create(
                new CreateRequest(
                        "T1", 1L, null,
                        LocalDate.of(2026, 8, 27),
                        100L, 900L,
                        "医院数字化升级商机",
                        200L, "CNY",
                        new BigDecimal("1000000"),
                        LocalDate.of(2026, 12, 31),
                        "QUALIFICATION",
                        new BigDecimal("30"),
                        LocalDate.of(2026, 9, 5)),
                200L);

        assertEquals(900L, result.getFbusinessPartnerId());
        assertEquals("C001", result.getFbusinessPartnerCode());
        assertEquals("客户A", result.getFbusinessPartnerName());
        assertEquals(CrmOpportunityService.STATUS_OPEN, result.getFstatus());
        assertEquals("CONVERTED", lead.getFstatus());
        assertEquals(result.getFid(), lead.getFconvertedOpportunityId());
        assertEquals(900L, lead.getFconvertedBusinessPartnerId());

        verify(outboxService, times(2)).append(
                eq("T1"),
                eq(1L),
                eq("CRM"),
                anyString(),
                anyString(),
                anyLong(),
                anyLong(),
                anyString(),
                anyString(),
                any(LocalDate.class),
                eq(200L),
                any()
        );
    }

    @Test
    void unqualifiedLeadCannotBeConverted() {
        when(partnerValidator.requireActiveCustomer(900L, "T1"))
                .thenReturn(partner());
        when(leadMapper.selectByIdForUpdate(100L, "T1"))
                .thenReturn(lead("NEW"));

        CrmOpportunityService service = service();

        BizException error = assertThrows(
                BizException.class,
                () -> service.create(
                        createRequest(100L), 200L));

        assertTrue(error.getMessage().contains("QUALIFIED"));
        verify(mapper, never()).insert(any());
    }

    @Test
    void customerValidationFailureShouldStopOpportunityCreation() {
        when(partnerValidator.requireActiveCustomer(900L, "T1"))
                .thenThrow(new BizException("BusinessPartner 未启用 CUSTOMER 角色"));

        CrmOpportunityService service = service();

        BizException error = assertThrows(
                BizException.class,
                () -> service.create(
                        createRequest(null), 200L));

        assertTrue(error.getMessage().contains("CUSTOMER"));
        verify(mapper, never()).insert(any());
        verifyNoInteractions(leadMapper);
    }

    @Test
    void winShouldCloseOpportunityAndPublishBusinessEvent() {
        CrmOpportunityEntity opportunity = opportunity();
        when(mapper.selectByIdForUpdate(300L, "T1"))
                .thenReturn(opportunity);
        when(mapper.updateById(opportunity)).thenReturn(1);

        CrmOpportunityEntity result =
                service().win(300L, "T1", 200L);

        assertEquals(CrmOpportunityService.STATUS_WON, result.getFstatus());
        assertEquals(CrmOpportunityService.STAGE_WON, result.getFstage());
        assertEquals(new BigDecimal("100.00"), result.getFprobability());
        assertNotNull(result.getFwonTime());

        verify(outboxService).append(
                eq("T1"),
                eq(1L),
                eq("CRM"),
                eq("CRM_OPPORTUNITY_WON"),
                eq("CRM_OPPORTUNITY"),
                eq(300L),
                anyLong(),
                eq("ERP_CRM_OPPORTUNITY"),
                eq("OPP-300"),
                eq(LocalDate.of(2026, 8, 27)),
                eq(200L),
                any()
        );
    }

    private CrmOpportunityService service() {
        return new CrmOpportunityService(
                mapper, leadMapper, partnerValidator, outboxService);
    }

    private CreateRequest createRequest(Long leadId) {
        return new CreateRequest(
                "T1", 1L, null,
                LocalDate.of(2026, 8, 27),
                leadId, 900L, "商机", 200L,
                "CNY", new BigDecimal("1000"),
                LocalDate.of(2026, 10, 1),
                null, null, null);
    }

    private BusinessPartnerDetail partner() {
        return new BusinessPartnerDetail(
                900L, "T1", "C001", "客户A",
                "ORGANIZATION", null,
                "ACTIVE", "AUDITED",
                List.of("CUSTOMER"));
    }

    private CrmLeadEntity lead(String status) {
        CrmLeadEntity lead = new CrmLeadEntity();
        lead.setFid(100L);
        lead.setFtenantId("T1");
        lead.setForgId(1L);
        lead.setFnumber("LEAD-100");
        lead.setFdate(LocalDate.of(2026, 8, 27));
        lead.setFname("线索");
        lead.setFstatus(status);
        lead.setFversion(0);
        return lead;
    }

    private CrmOpportunityEntity opportunity() {
        CrmOpportunityEntity entity = new CrmOpportunityEntity();
        entity.setFid(300L);
        entity.setFtenantId("T1");
        entity.setForgId(1L);
        entity.setFnumber("OPP-300");
        entity.setFdate(LocalDate.of(2026, 8, 27));
        entity.setFbusinessPartnerId(900L);
        entity.setFbusinessPartnerCode("C001");
        entity.setFbusinessPartnerName("客户A");
        entity.setFname("商机");
        entity.setFcurrencyCode("CNY");
        entity.setFexpectedAmount(new BigDecimal("1000.00"));
        entity.setFstage(CrmOpportunityService.STAGE_NEGOTIATION);
        entity.setFprobability(new BigDecimal("75.00"));
        entity.setFstatus(CrmOpportunityService.STATUS_OPEN);
        entity.setFversion(0);
        return entity;
    }
}
