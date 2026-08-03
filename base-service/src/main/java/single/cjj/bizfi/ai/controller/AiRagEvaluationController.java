package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.dto.AiRagEvalCaseRequest;
import single.cjj.bizfi.ai.dto.AiRagEvalCaseResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalConfigResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalResultResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalRunResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalSetRequest;
import single.cjj.bizfi.ai.dto.AiRagEvalSetResponse;
import single.cjj.bizfi.ai.service.impl.AiRagEvaluationService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/knowledge/evaluation")
public class AiRagEvaluationController {

    private final AiRagEvaluationService evaluationService;

    public AiRagEvaluationController(AiRagEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/config")
    public ApiResponse<AiRagEvalConfigResponse> config() {
        return ApiResponse.success(evaluationService.config());
    }

    @GetMapping("/sets")
    public ApiResponse<List<AiRagEvalSetResponse>> listSets(
            @RequestParam("kbId") String kbId
    ) {
        return ApiResponse.success(evaluationService.listSets(kbId));
    }

    @PostMapping("/sets")
    public ApiResponse<AiRagEvalSetResponse> createSet(
            @RequestParam("kbId") String kbId,
            @RequestBody AiRagEvalSetRequest request
    ) {
        return ApiResponse.success(evaluationService.createSet(kbId, request));
    }

    @PutMapping("/sets/{setId}")
    public ApiResponse<AiRagEvalSetResponse> updateSet(
            @PathVariable("setId") String setId,
            @RequestBody AiRagEvalSetRequest request
    ) {
        return ApiResponse.success(evaluationService.updateSet(setId, request));
    }

    @DeleteMapping("/sets/{setId}")
    public ApiResponse<Boolean> deleteSet(@PathVariable("setId") String setId) {
        return ApiResponse.success(evaluationService.deleteSet(setId));
    }

    @GetMapping("/sets/{setId}/cases")
    public ApiResponse<List<AiRagEvalCaseResponse>> listCases(@PathVariable("setId") String setId) {
        return ApiResponse.success(evaluationService.listCases(setId));
    }

    @PostMapping("/sets/{setId}/cases")
    public ApiResponse<AiRagEvalCaseResponse> createCase(
            @PathVariable("setId") String setId,
            @RequestBody AiRagEvalCaseRequest request
    ) {
        return ApiResponse.success(evaluationService.createCase(setId, request));
    }

    @PutMapping("/sets/{setId}/cases/{caseId}")
    public ApiResponse<AiRagEvalCaseResponse> updateCase(
            @PathVariable("setId") String setId,
            @PathVariable("caseId") String caseId,
            @RequestBody AiRagEvalCaseRequest request
    ) {
        return ApiResponse.success(evaluationService.updateCase(setId, caseId, request));
    }

    @DeleteMapping("/sets/{setId}/cases/{caseId}")
    public ApiResponse<Boolean> deleteCase(
            @PathVariable("setId") String setId,
            @PathVariable("caseId") String caseId
    ) {
        return ApiResponse.success(evaluationService.deleteCase(setId, caseId));
    }

    @GetMapping("/sets/{setId}/runs")
    public ApiResponse<List<AiRagEvalRunResponse>> listRuns(
            @PathVariable("setId") String setId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(evaluationService.listRuns(setId, limit));
    }

    @PostMapping("/sets/{setId}/runs")
    public ApiResponse<AiRagEvalRunResponse> createRun(@PathVariable("setId") String setId) {
        return ApiResponse.success(evaluationService.createRun(setId));
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AiRagEvalRunResponse> getRun(@PathVariable("runId") String runId) {
        return ApiResponse.success(evaluationService.getRun(runId));
    }

    @GetMapping("/runs/{runId}/results")
    public ApiResponse<List<AiRagEvalResultResponse>> listResults(@PathVariable("runId") String runId) {
        return ApiResponse.success(evaluationService.listResults(runId));
    }

    @PostMapping("/runs/{runId}/retry")
    public ApiResponse<AiRagEvalRunResponse> retryRun(@PathVariable("runId") String runId) {
        return ApiResponse.success(evaluationService.retryRun(runId));
    }
}
