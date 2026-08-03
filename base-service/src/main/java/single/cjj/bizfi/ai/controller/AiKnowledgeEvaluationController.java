package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeEvaluationService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/admin/knowledge/evaluations")
public class AiKnowledgeEvaluationController {

    private final AiKnowledgeEvaluationService evaluationService;

    public AiKnowledgeEvaluationController(AiKnowledgeEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/datasets")
    public ApiResponse<List<AiKnowledgeEvaluationService.DatasetView>> listDatasets() {
        return ApiResponse.success(evaluationService.listDatasets());
    }

    @PostMapping("/datasets")
    public ApiResponse<AiKnowledgeEvaluationService.DatasetView> createDataset(
            @RequestBody DatasetRequest request
    ) {
        return ApiResponse.success(evaluationService.createDataset(
                new AiKnowledgeEvaluationService.DatasetCommand(
                        request == null ? null : request.name(),
                        request == null ? null : request.description(),
                        request == null ? null : request.status()
                )
        ));
    }

    @GetMapping("/datasets/{datasetId}/questions")
    public ApiResponse<List<AiKnowledgeEvaluationService.QuestionView>> listQuestions(
            @PathVariable("datasetId") String datasetId
    ) {
        return ApiResponse.success(evaluationService.listQuestions(datasetId));
    }

    @PostMapping("/datasets/{datasetId}/questions")
    public ApiResponse<AiKnowledgeEvaluationService.QuestionView> addQuestion(
            @PathVariable("datasetId") String datasetId,
            @RequestBody QuestionRequest request
    ) {
        return ApiResponse.success(evaluationService.addQuestion(
                datasetId,
                new AiKnowledgeEvaluationService.QuestionCommand(
                        request == null ? null : request.question(),
                        request == null ? null : request.kbIds(),
                        request == null ? null : request.expectedDocIds(),
                        request == null ? null : request.expectedChunkIds(),
                        request == null ? null : request.expectedAnswer(),
                        request == null ? null : request.status()
                )
        ));
    }

    @PostMapping("/datasets/{datasetId}/runs")
    public ApiResponse<AiKnowledgeEvaluationService.RunView> run(
            @PathVariable("datasetId") String datasetId,
            @RequestParam(value = "topK", defaultValue = "5") Integer topK
    ) {
        return ApiResponse.success(evaluationService.run(datasetId, topK));
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AiKnowledgeEvaluationService.RunView> getRun(
            @PathVariable("runId") String runId
    ) {
        return ApiResponse.success(evaluationService.getRun(runId));
    }

    @GetMapping("/runs/{runId}/results")
    public ApiResponse<List<AiKnowledgeEvaluationService.ResultView>> listResults(
            @PathVariable("runId") String runId
    ) {
        return ApiResponse.success(evaluationService.listResults(runId));
    }

    public record DatasetRequest(String name, String description, String status) {
    }

    public record QuestionRequest(
            String question,
            List<String> kbIds,
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            String expectedAnswer,
            String status
    ) {
    }
}
