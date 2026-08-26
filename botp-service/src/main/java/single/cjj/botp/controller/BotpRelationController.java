package single.cjj.botp.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.DocumentRelationEntry;
import single.cjj.botp.domain.BotpContracts.RelationInvalidateRequest;
import single.cjj.botp.domain.BotpContracts.TargetStatusEvent;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.relation.BotpRelationLifecycleService;
import single.cjj.botp.relation.BotpRelationRepository;

import java.util.List;

@RestController
@RequestMapping("/botp/relations")
public class BotpRelationController {

    private final BotpRelationRepository repository;
    private final BotpRelationLifecycleService lifecycleService;

    public BotpRelationController(
            BotpRelationRepository repository,
            BotpRelationLifecycleService lifecycleService
    ) {
        this.repository = repository;
        this.lifecycleService = lifecycleService;
    }

    @GetMapping
    public ApiResponse<List<DocumentRelation>> list(
            @RequestParam(value = "tenantId", defaultValue = "default") String tenantId,
            @RequestParam(value = "sourceDocumentId", required = false) String sourceDocumentId,
            @RequestParam(value = "targetDocumentId", required = false) String targetDocumentId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.success(repository.find(tenantId, sourceDocumentId, targetDocumentId, limit));
    }

    @GetMapping("/{relationId}")
    public ApiResponse<DocumentRelation> detail(@PathVariable("relationId") Long relationId) {
        return repository.findById(relationId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error("BOTP 单据关系不存在: " + relationId));
    }

    @GetMapping("/{relationId}/entries")
    public ApiResponse<List<DocumentRelationEntry>> entries(@PathVariable("relationId") Long relationId) {
        return ApiResponse.success(repository.findEntries(relationId));
    }

    @PostMapping("/target-events")
    public ApiResponse<List<DocumentRelation>> targetStatusEvent(@Valid @RequestBody TargetStatusEvent event) {
        return ApiResponse.success(lifecycleService.handleTargetStatusEvent(event));
    }

    @PostMapping("/{relationId}/invalidate")
    public ApiResponse<DocumentRelation> invalidate(
            @PathVariable("relationId") Long relationId,
            @Valid @RequestBody RelationInvalidateRequest request
    ) {
        return ApiResponse.success(lifecycleService.invalidateManually(relationId, request));
    }

    @PostMapping("/{relationId}/recompute")
    public ApiResponse<WritebackTask> recompute(@PathVariable("relationId") Long relationId) {
        return ApiResponse.success(lifecycleService.recompute(relationId));
    }
}
