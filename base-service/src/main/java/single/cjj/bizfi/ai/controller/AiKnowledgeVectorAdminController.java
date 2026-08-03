package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeVectorIndexService;
import single.cjj.bizfi.entity.ApiResponse;

@RestController
@RequestMapping("/ai/admin/knowledge/vector-store")
public class AiKnowledgeVectorAdminController {

    private final AiKnowledgeVectorIndexService vectorIndexService;

    public AiKnowledgeVectorAdminController(AiKnowledgeVectorIndexService vectorIndexService) {
        this.vectorIndexService = vectorIndexService;
    }

    @GetMapping("/status")
    public ApiResponse<AiKnowledgeVectorIndexService.VectorStoreStatus> status() {
        return ApiResponse.success(vectorIndexService.vectorStoreStatus());
    }

    @PostMapping("/migrations/pgvector")
    public ApiResponse<AiKnowledgeVectorIndexService.BulkIndexResult> migrateToPgVector() {
        return ApiResponse.success(vectorIndexService.migrateAllToPgVector());
    }
}
