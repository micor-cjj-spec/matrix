package single.cjj.erp.crm.lead.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.CreateRequest;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.DisqualifyRequest;
import single.cjj.erp.crm.lead.dto.CrmLeadContracts.UpdateRequest;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;
import single.cjj.erp.crm.lead.service.CrmLeadService;

@RestController
@RequestMapping("/crm/leads")
public class CrmLeadController {

    private final CrmLeadService service;

    public CrmLeadController(CrmLeadService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CrmLeadEntity> create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<CrmLeadEntity> update(
            @PathVariable Long fid,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<CrmLeadEntity> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<CrmLeadEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(service.page(
                tenantId, orgId, page, size, number,
                name, source, ownerId, status));
    }

    @PostMapping("/{fid}/start-qualification")
    public ApiResponse<CrmLeadEntity> startQualification(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.startQualification(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/qualify")
    public ApiResponse<CrmLeadEntity> qualify(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.qualify(fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/disqualify")
    public ApiResponse<CrmLeadEntity> disqualify(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody DisqualifyRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.disqualify(
                fid, tenantId, request.reason(), operatorId));
    }

    @DeleteMapping("/{fid}")
    public ApiResponse<Boolean> delete(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.delete(fid, tenantId));
    }
}
