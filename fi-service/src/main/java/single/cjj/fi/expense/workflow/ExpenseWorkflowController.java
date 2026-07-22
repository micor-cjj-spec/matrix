package single.cjj.fi.expense.workflow;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;

@RestController
@RequestMapping("/fi/expense-reimbursements")
public class ExpenseWorkflowController {

    private final ExpenseWorkflowService service;
    private final ExpenseWorkflowReconciliationService reconciliationService;

    public ExpenseWorkflowController(ExpenseWorkflowService service,
                                     ExpenseWorkflowReconciliationService reconciliationService) {
        this.service = service;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    public ApiResponse<ExpenseWorkflowContracts.ExpenseResponse> create(
            @Valid @RequestBody ExpenseWorkflowContracts.CreateExpenseRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseWorkflowContracts.ExpenseResponse> update(
            @PathVariable("expenseId") String expenseId,
            @Valid @RequestBody ExpenseWorkflowContracts.UpdateExpenseRequest request) {
        return ApiResponse.success(service.update(expenseId, request));
    }

    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseWorkflowContracts.ExpenseResponse> get(
            @PathVariable("expenseId") String expenseId) {
        return ApiResponse.success(service.get(expenseId));
    }

    @GetMapping("/{expenseId}/approval-detail")
    public ApiResponse<ExpenseWorkflowContracts.ApprovalDetailResponse> approvalDetail(
            @PathVariable("expenseId") String expenseId) {
        return ApiResponse.success(service.approvalDetail(expenseId));
    }

    @PostMapping("/{expenseId}/submit")
    public ApiResponse<ExpenseWorkflowContracts.ExpenseResponse> submit(
            @PathVariable("expenseId") String expenseId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody ExpenseWorkflowContracts.SubmitExpenseRequest request) {
        return ApiResponse.success(service.submit(expenseId, request, requestId));
    }

    @PostMapping("/{expenseId}/cancel")
    public ApiResponse<ExpenseWorkflowContracts.ExpenseResponse> cancel(
            @PathVariable("expenseId") String expenseId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody ExpenseWorkflowContracts.CancelExpenseRequest request) {
        return ApiResponse.success(service.cancel(expenseId, request, requestId));
    }

    @PostMapping("/admin/reconcile")
    public ApiResponse<ExpenseWorkflowContracts.ReconciliationResponse> reconcile(
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ApiResponse.success(reconciliationService.reconcile(limit));
    }
}
