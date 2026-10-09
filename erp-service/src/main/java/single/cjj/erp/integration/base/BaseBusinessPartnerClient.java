package single.cjj.erp.integration.base;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.integration.base.BaseBusinessPartnerContracts.BusinessPartnerDetail;

@FeignClient(name = "base-service")
public interface BaseBusinessPartnerClient {

    @GetMapping("/business-partners/{fid}")
    ApiResponse<BusinessPartnerDetail> detail(
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    );
}
