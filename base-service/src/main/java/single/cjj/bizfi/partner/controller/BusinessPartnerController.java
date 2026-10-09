package single.cjj.bizfi.partner.controller;

import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.BusinessPartnerDetail;
import single.cjj.bizfi.partner.service.BusinessPartnerService;

@RestController
@RequestMapping("/business-partners")
public class BusinessPartnerController {

    private final BusinessPartnerService service;

    public BusinessPartnerController(BusinessPartnerService service) {
        this.service = service;
    }

    @GetMapping("/{fid}")
    public ApiResponse<BusinessPartnerDetail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping("/resolve")
    public ApiResponse<BusinessPartnerDetail> resolve(
            @RequestParam String tenantId,
            @RequestParam String code
    ) {
        return ApiResponse.success(service.resolveByCode(tenantId, code));
    }
}
