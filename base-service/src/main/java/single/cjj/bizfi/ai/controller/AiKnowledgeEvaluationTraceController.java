package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeEvaluationTraceRunService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/admin/knowledge/evaluations")
public class AiKnowledgeEvaluationTraceController {

    private final AiKnowledgeEvaluationTraceRunService traceRunService;

    public AiKnowledgeEvaluationTraceController(
            AiKnowledgeEvaluationTraceRunService traceRunService
    ) {
        this.traceRunService = traceRunService;
    }

    @PostMapping("/datasets/{datasetId}/trace-runs")
    public ApiResponse<AiKnowledgeEvaluationTraceRunService.TraceRunView> runWithTrace(
            @PathVariable("datasetId") String datasetId,
            @RequestParam(value = "topK", defaultValue = "5") Integer topK
    ) {
        return ApiResponse.success(traceRunService.run(datasetId, topK));
    }

    @GetMapping("/runs/{runId}/traces")
    public ApiResponse<List<AiKnowledgeEvaluationTraceRunService.TraceView>> listTraces(
            @PathVariable("runId") String runId
    ) {
        return ApiResponse.success(traceRunService.listTraces(runId));
    }
}
