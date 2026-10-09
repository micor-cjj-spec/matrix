package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeVectorIndexService;
import single.cjj.bizfi.ai.service.impl.AiVectorStoreDiagnosticsService;
import single.cjj.bizfi.entity.ApiResponse;

@RestController
@RequestMapping("/ai/admin/knowledge/vector-store")
public class AiKnowledgeVectorAdminController {

    private final AiKnowledgeVectorIndexService vectorIndexService;
    private final AiVectorStoreDiagnosticsService diagnosticsService;

    public AiKnowledgeVectorAdminController(
            AiKnowledgeVectorIndexService vectorIndexService,
            AiVectorStoreDiagnosticsService diagnosticsService
    ) {
        this.vectorIndexService = vectorIndexService;
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/status")
    public ApiResponse<AiVectorStoreDiagnosticsService.VectorStoreStatus> status() {
        return ApiResponse.success(diagnosticsService.status());
    }

    @PostMapping("/migrations/pgvector")
    public ApiResponse<AiKnowledgeVectorIndexService.BulkIndexResult> migrateToPgVector() {
        return ApiResponse.success(vectorIndexService.migrateAllToPgVector());
    }
}
