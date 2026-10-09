package single.cjj.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.service.WorkflowService;

@RestController
@RequestMapping("/workflow/definitions")
public class WorkflowDefinitionController {

    private final WorkflowService workflowService;

    public WorkflowDefinitionController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ApiResponse<WorkflowContracts.DefinitionResponse> create(
            @Valid @RequestBody WorkflowContracts.DefinitionCreateRequest request) {
        return ApiResponse.success(workflowService.createDefinition(request));
    }

    @PostMapping("/{definitionKey}/versions/{version}/publish")
    public ApiResponse<WorkflowContracts.DefinitionResponse> publish(
            @PathVariable("definitionKey") String definitionKey,
            @PathVariable("version") int version,
            @RequestParam("tenantId") String tenantId) {
        return ApiResponse.success(
                workflowService.publishDefinition(tenantId, definitionKey, version)
        );
    }

    @GetMapping("/{definitionKey}/published")
    public ApiResponse<WorkflowContracts.DefinitionResponse> published(
            @PathVariable("definitionKey") String definitionKey,
            @RequestParam("tenantId") String tenantId) {
        return ApiResponse.success(
                workflowService.getPublishedDefinition(tenantId, definitionKey)
        );
    }
}
