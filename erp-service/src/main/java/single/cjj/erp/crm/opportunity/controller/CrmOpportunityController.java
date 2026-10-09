package single.cjj.erp.crm.opportunity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.CreateRequest;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.LoseRequest;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.StageRequest;
import single.cjj.erp.crm.opportunity.dto.CrmOpportunityContracts.UpdateRequest;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;
import single.cjj.erp.crm.opportunity.service.CrmOpportunityService;

@RestController
@RequestMapping("/crm/opportunities")
public class CrmOpportunityController {

    private final CrmOpportunityService service;

    public CrmOpportunityController(CrmOpportunityService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CrmOpportunityEntity> create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @PutMapping("/{fid}")
    public ApiResponse<CrmOpportunityEntity> update(
            @PathVariable Long fid,
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.update(fid, request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<CrmOpportunityEntity> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<IPage<CrmOpportunityEntity>> page(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long businessPartnerId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(service.page(
                tenantId, orgId, page, size, number, name,
                businessPartnerId, ownerId, stage, status));
    }

    @PostMapping("/{fid}/stage")
    public ApiResponse<CrmOpportunityEntity> stage(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody StageRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.changeStage(
                fid, tenantId, request.stage(),
                request.probability(), request.nextActionDate(),
                operatorId));
    }

    @PostMapping("/{fid}/win")
    public ApiResponse<CrmOpportunityEntity> win(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.win(
                fid, tenantId, operatorId));
    }

    @PostMapping("/{fid}/lose")
    public ApiResponse<CrmOpportunityEntity> lose(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody LoseRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.lose(
                fid, tenantId, request.reason(), operatorId));
    }
}
