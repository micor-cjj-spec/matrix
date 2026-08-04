package single.cjj.bizfi.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.ai.service.impl.AiEvaluationQuestionDraftService;
import single.cjj.bizfi.ai.service.impl.AiKnowledgeEvaluationService;
import single.cjj.bizfi.entity.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/ai/admin/knowledge/evaluations")
public class AiKnowledgeEvaluationController {

    private final AiKnowledgeEvaluationService evaluationService;
    private final AiEvaluationQuestionDraftService draftService;

    public AiKnowledgeEvaluationController(
            AiKnowledgeEvaluationService evaluationService,
            AiEvaluationQuestionDraftService draftService
    ) {
        this.evaluationService = evaluationService;
        this.draftService = draftService;
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
                toEvaluationCommand(request)
        ));
    }

    @PostMapping("/datasets/{datasetId}/questions/bulk")
    public ApiResponse<AiEvaluationQuestionDraftService.BulkImportView> bulkImportQuestions(
            @PathVariable("datasetId") String datasetId,
            @RequestBody BulkQuestionRequest request
    ) {
        List<AiEvaluationQuestionDraftService.QuestionCommand> commands = request == null || request.questions() == null
                ? List.of()
                : request.questions().stream().map(this::toDraftCommand).toList();
        return ApiResponse.success(draftService.bulkImport(datasetId, commands));
    }

    @PutMapping("/datasets/{datasetId}/questions/{questionId}")
    public ApiResponse<AiEvaluationQuestionDraftService.QuestionView> updateQuestion(
            @PathVariable("datasetId") String datasetId,
            @PathVariable("questionId") String questionId,
            @RequestBody QuestionRequest request
    ) {
        return ApiResponse.success(draftService.updateQuestion(
                datasetId,
                questionId,
                toDraftCommand(request)
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

    private AiKnowledgeEvaluationService.QuestionCommand toEvaluationCommand(QuestionRequest request) {
        return new AiKnowledgeEvaluationService.QuestionCommand(
                request == null ? null : request.question(),
                request == null ? null : request.kbIds(),
                request == null ? null : request.expectedDocIds(),
                request == null ? null : request.expectedChunkIds(),
                request == null ? null : request.expectedAnswer(),
                request == null ? null : request.status()
        );
    }

    private AiEvaluationQuestionDraftService.QuestionCommand toDraftCommand(QuestionRequest request) {
        return new AiEvaluationQuestionDraftService.QuestionCommand(
                request == null ? null : request.question(),
                request == null ? null : request.kbIds(),
                request == null ? null : request.expectedDocIds(),
                request == null ? null : request.expectedChunkIds(),
                request == null ? null : request.expectedAnswer(),
                request == null ? null : request.status()
        );
    }

    public record DatasetRequest(String name, String description, String status) {
    }

    public record BulkQuestionRequest(List<QuestionRequest> questions) {
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
