package single.cjj.bizfi.partner.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.LegacyPartyRequest;
import single.cjj.bizfi.partner.dto.BusinessPartnerContracts.LegacyPartyResponse;
import single.cjj.bizfi.partner.service.BusinessPartnerService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PartnerCompatibilityController {

    private final BusinessPartnerService service;

    public PartnerCompatibilityController(BusinessPartnerService service) {
        this.service = service;
    }

    @GetMapping({"/customer/list", "/supplier/list"})
    public ApiResponse<Map<String, Object>> list(
            HttpServletRequest servletRequest,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        List<LegacyPartyResponse> records = service.listLegacyRole(
                tenant(tenantId, tenantHeader),
                role(servletRequest));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", records.size());
        return ApiResponse.success(data);
    }

    @GetMapping({"/customer/{fid}", "/supplier/{fid}"})
    public ApiResponse<LegacyPartyResponse> detail(
            HttpServletRequest servletRequest,
            @PathVariable Long fid,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success(service.detailLegacyRole(
                fid, tenant(tenantId, tenantHeader), role(servletRequest)));
    }

    @PostMapping({"/customer", "/supplier"})
    public ApiResponse<LegacyPartyResponse> create(
            HttpServletRequest servletRequest,
            @RequestBody LegacyPartyRequest request,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success("创建成功", service.createLegacyRole(
                tenant(tenantId, tenantHeader), role(servletRequest), request, null));
    }

    @PutMapping({"/customer", "/supplier"})
    public ApiResponse<LegacyPartyResponse> update(
            HttpServletRequest servletRequest,
            @RequestBody LegacyPartyRequest request,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success("更新成功", service.updateLegacyRole(
                tenant(tenantId, tenantHeader), role(servletRequest), request, null));
    }

    @DeleteMapping({"/customer/{fid}", "/supplier/{fid}"})
    public ApiResponse<Boolean> delete(
            HttpServletRequest servletRequest,
            @PathVariable Long fid,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success(service.deleteLegacyRole(
                fid, tenant(tenantId, tenantHeader), role(servletRequest)));
    }

    @PostMapping({"/customer/{fid}/submit", "/supplier/{fid}/submit"})
    public ApiResponse<LegacyPartyResponse> submit(
            HttpServletRequest servletRequest,
            @PathVariable Long fid,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success("提交审核成功", service.submitLegacyRole(
                fid, tenant(tenantId, tenantHeader), role(servletRequest), null));
    }

    @PostMapping({"/customer/{fid}/audit", "/supplier/{fid}/audit"})
    public ApiResponse<LegacyPartyResponse> audit(
            HttpServletRequest servletRequest,
            @PathVariable Long fid,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success("审核通过", service.auditLegacyRole(
                fid, tenant(tenantId, tenantHeader), role(servletRequest), null));
    }

    @PostMapping({"/customer/{fid}/reject", "/supplier/{fid}/reject"})
    public ApiResponse<LegacyPartyResponse> reject(
            HttpServletRequest servletRequest,
            @PathVariable Long fid,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return ApiResponse.success("已驳回", service.rejectLegacyRole(
                fid, tenant(tenantId, tenantHeader), role(servletRequest), null));
    }

    private String tenant(String queryTenant, String headerTenant) {
        if (queryTenant != null && !queryTenant.isBlank()) {
            return queryTenant.trim();
        }
        if (headerTenant != null && !headerTenant.isBlank()) {
            return headerTenant.trim();
        }
        return "default";
    }

    private String role(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/supplier")
                ? BusinessPartnerService.ROLE_SUPPLIER
                : BusinessPartnerService.ROLE_CUSTOMER;
    }
}
