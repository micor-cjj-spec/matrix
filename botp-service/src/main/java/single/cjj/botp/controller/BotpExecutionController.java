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
import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionResult;
import single.cjj.botp.domain.BotpContracts.PreviewResult;
import single.cjj.botp.execution.BotpExecutionService;

import java.util.List;

@RestController
@RequestMapping("/botp/executions")
public class BotpExecutionController {

    private final BotpExecutionService executionService;

    public BotpExecutionController(BotpExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/preview")
    public ApiResponse<PreviewResult> preview(@Valid @RequestBody ExecutionRequest request) {
        return ApiResponse.success(executionService.preview(request));
    }

    @PostMapping
    public ApiResponse<ExecutionResult> execute(@Valid @RequestBody ExecutionRequest request) {
        return ApiResponse.success(executionService.execute(request));
    }

    @GetMapping
    public ApiResponse<List<ExecutionDetails>> list(
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.success(executionService.list(limit));
    }

    @GetMapping("/{executionId}")
    public ApiResponse<ExecutionResult> getExecution(
            @PathVariable("executionId") String executionId
    ) {
        return ApiResponse.success(executionService.getById(executionId));
    }
}
