package single.cjj.erp.crm.support;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.integration.base.BaseBusinessPartnerClient;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;

@Component
public class CustomerPartnerValidator {

    private final BaseBusinessPartnerClient client;

    public CustomerPartnerValidator(BaseBusinessPartnerClient client) {
        this.client = client;
    }

    public BusinessPartnerDetail requireActiveCustomer(
            Long partnerId,
            String tenantId
    ) {
        if (partnerId == null) {
            throw new BizException("businessPartnerId 不能为空");
        }
        ApiResponse<BusinessPartnerDetail> response =
                client.detail(partnerId, tenantId);
        if (response == null || response.getCode() != 200
                || response.getData() == null) {
            throw new BizException("客户 BusinessPartner 校验失败: " + partnerId);
        }
        BusinessPartnerDetail partner = response.getData();
        if (!tenantId.equals(partner.ftenantId())) {
            throw new BizException("客户 BusinessPartner 租户不匹配");
        }
        if (!"ACTIVE".equals(partner.fstatus())
                || !"AUDITED".equals(partner.fapprovalStatus())) {
            throw new BizException("只有已审核且生效的客户允许创建商机");
        }
        if (!partner.roles().contains("CUSTOMER")) {
            throw new BizException("BusinessPartner 未启用 CUSTOMER 角色");
        }
        return partner;
    }
}
