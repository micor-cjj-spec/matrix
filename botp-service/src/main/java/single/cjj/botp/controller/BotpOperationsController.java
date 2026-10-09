package single.cjj.botp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.botp.domain.BotpContracts.ReconciliationActionRequest;
import single.cjj.botp.domain.BotpContracts.ReconciliationIssue;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.reconciliation.BotpReconciliationService;
import single.cjj.botp.writeback.BotpWritebackService;

import java.util.List;

@RestController
@RequestMapping("/botp/operations")
public class BotpOperationsController {

    private final BotpWritebackService writebackService;
    private final BotpReconciliationService reconciliationService;

    public BotpOperationsController(
            BotpWritebackService writebackService,
            BotpReconciliationService reconciliationService
    ) {
        this.writebackService = writebackService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/writeback-tasks")
    public ApiResponse<List<WritebackTask>> writebackTasks(
            @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ApiResponse.success(writebackService.list(limit));
    }

    @PostMapping("/writeback-tasks/{taskId}/retry")
    public ApiResponse<WritebackTask> retryWriteback(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(writebackService.retryNow(taskId));
    }

    @GetMapping("/reconciliation-issues")
    public ApiResponse<List<ReconciliationIssue>> reconciliationIssues(
            @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ApiResponse.success(reconciliationService.list(limit));
    }

    @PostMapping("/reconciliation/run")
    public ApiResponse<List<ReconciliationIssue>> runReconciliation(
            @RequestParam(value = "limit", defaultValue = "500") int limit,
            @RequestParam(value = "autoFix", defaultValue = "true") boolean autoFix
    ) {
        return ApiResponse.success(reconciliationService.runNow(limit, autoFix));
    }

    @PostMapping("/reconciliation-issues/{issueId}/fix")
    public ApiResponse<ReconciliationIssue> fixIssue(
            @PathVariable("issueId") Long issueId,
            @RequestBody(required = false) ReconciliationActionRequest request
    ) {
        return ApiResponse.success(reconciliationService.fix(issueId, request));
    }

    @PostMapping("/reconciliation-issues/{issueId}/ignore")
    public ApiResponse<ReconciliationIssue> ignoreIssue(
            @PathVariable("issueId") Long issueId,
            @RequestBody(required = false) ReconciliationActionRequest request
    ) {
        return ApiResponse.success(reconciliationService.ignore(issueId, request));
    }
}
