package single.cjj.bizfi.partner.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.LegacyPartyRequest;
import single.cjj.bizfi.partner.entity.BusinessPartnerEntity;
import single.cjj.bizfi.partner.entity.BusinessPartnerRoleEntity;
import single.cjj.bizfi.partner.mapper.BusinessPartnerMapper;
import single.cjj.bizfi.partner.mapper.BusinessPartnerRoleMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerServiceTest {

    @Mock
    private BusinessPartnerMapper partnerMapper;
    @Mock
    private BusinessPartnerRoleMapper roleMapper;

    @Test
    void createCustomerShouldPersistPartnerAndCustomerRole() {
        when(partnerMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(partnerMapper.insert(any(BusinessPartnerEntity.class))).thenReturn(1);
        when(roleMapper.insert(any(BusinessPartnerRoleEntity.class))).thenReturn(1);

        BusinessPartnerService service =
                new BusinessPartnerService(partnerMapper, roleMapper);

        var response = service.createLegacyRole(
                "T1",
                BusinessPartnerService.ROLE_CUSTOMER,
                new LegacyPartyRequest(
                        null, "客户A", "BP001", null),
                100L);

        assertEquals("BP001", response.fcode());
        assertEquals("DRAFT", response.fstatus());
        assertEquals("CUSTOMER", response.roleType());
        assertEquals(response.fid(), response.businessPartnerId());

        ArgumentCaptor<BusinessPartnerEntity> partnerCaptor =
                ArgumentCaptor.forClass(BusinessPartnerEntity.class);
        verify(partnerMapper).insert(partnerCaptor.capture());
        assertEquals("DRAFT", partnerCaptor.getValue().getFstatus());
        assertEquals("DRAFT", partnerCaptor.getValue().getFapprovalStatus());

        ArgumentCaptor<BusinessPartnerRoleEntity> roleCaptor =
                ArgumentCaptor.forClass(BusinessPartnerRoleEntity.class);
        verify(roleMapper).insert(roleCaptor.capture());
        assertEquals("CUSTOMER", roleCaptor.getValue().getFroleType());
        assertEquals(
                partnerCaptor.getValue().getFid(),
                roleCaptor.getValue().getFbusinessPartnerId());
    }

    @Test
    void addingSupplierWithSameCodeShouldReuseBusinessPartner() {
        BusinessPartnerEntity existing = partner(
                10L, "T1", "BP001", "统一法人", "AUDITED", "ACTIVE");

        when(partnerMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(roleMapper.insert(any(BusinessPartnerRoleEntity.class))).thenReturn(1);

        BusinessPartnerService service =
                new BusinessPartnerService(partnerMapper, roleMapper);

        var response = service.createLegacyRole(
                "T1",
                BusinessPartnerService.ROLE_SUPPLIER,
                new LegacyPartyRequest(
                        null, "统一法人", "BP001", null),
                100L);

        assertEquals(10L, response.businessPartnerId());
        assertEquals("SUPPLIER", response.roleType());
        assertEquals("AUDITED", response.fstatus());
        verify(partnerMapper, never()).insert(any(BusinessPartnerEntity.class));
        verify(roleMapper).insert(any(BusinessPartnerRoleEntity.class));
    }

    @Test
    void auditShouldActivateLifecycleButKeepLegacyAuditedStatus() {
        BusinessPartnerEntity existing = partner(
                20L, "T1", "BP002", "客户B", "SUBMITTED", "DRAFT");
        BusinessPartnerRoleEntity role = new BusinessPartnerRoleEntity();
        role.setFid(21L);
        role.setFtenantId("T1");
        role.setFbusinessPartnerId(20L);
        role.setFroleType("CUSTOMER");
        role.setFstatus("ENABLED");

        when(partnerMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(partnerMapper.updateById(existing)).thenReturn(1);

        BusinessPartnerService service =
                new BusinessPartnerService(partnerMapper, roleMapper);

        var response = service.auditLegacyRole(
                20L, "T1", "CUSTOMER", 100L);

        assertEquals("AUDITED", response.fstatus());
        assertEquals("ACTIVE", response.lifecycleStatus());
        assertEquals("AUDITED", existing.getFapprovalStatus());
        assertEquals("ACTIVE", existing.getFstatus());
    }

    private BusinessPartnerEntity partner(
            Long id,
            String tenant,
            String code,
            String name,
            String approval,
            String lifecycle
    ) {
        BusinessPartnerEntity entity = new BusinessPartnerEntity();
        entity.setFid(id);
        entity.setFtenantId(tenant);
        entity.setFcode(code);
        entity.setFname(name);
        entity.setFpartnerType("ORGANIZATION");
        entity.setFapprovalStatus(approval);
        entity.setFstatus(lifecycle);
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        return entity;
    }
}
