package single.cjj.fi.expense.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseWorkflowService {

    public static final String BUSINESS_TYPE = "EXPENSE_REIMBURSEMENT";
    public static final String SOURCE_SYSTEM = "fi-service";
    public static final String EVENT_START = "WORKFLOW_START_REQUESTED";
    public static final String EVENT_RESUBMIT = "WORKFLOW_RESUBMIT_REQUESTED";
    public static final String EVENT_CANCEL = "WORKFLOW_CANCEL_REQUESTED";

    private final ExpenseWorkflowRepository repository;
    private final ExpenseWorkflowGateway gateway;
    private final ObjectMapper objectMapper;
    private final String callbackUrl;

    public ExpenseWorkflowService(
            ExpenseWorkflowRepository repository,
            ExpenseWorkflowGateway gateway,
            ObjectMapper objectMapper,
            @Value("${fi.workflow.callback-url:http://localhost:10003/api/fi/expense/workflow/events}")
            String callbackUrl) {
        this.repository = repository;
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.callbackUrl = callbackUrl;
    }

    @Transactional
    public ExpenseWorkflowContracts.ExpenseResponse create(
            ExpenseWorkflowContracts.CreateExpenseRequest request) {
        String expenseId = newId();
        LocalDateTime now = LocalDateTime.now();
        ExpenseWorkflowRepository.ExpenseRow row = new ExpenseWorkflowRepository.ExpenseRow(
                expenseId,
                request.tenantId(),
                "ER-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "-" + expenseId.substring(0, 6).toUpperCase(),
                request.applicantId(),
                request.departmentCode(),
                request.amount(),
                request.safeCurrency(),
                request.description(),
                "DRAFT",
                null,
                0,
                now,
                null,
                null
        );
        repository.insertExpense(row);
        return toResponse(row);
    }

    @Transactional
    public ExpenseWorkflowContracts.ExpenseResponse update(
            String expenseId,
            ExpenseWorkflowContracts.UpdateExpenseRequest request) {
        ExpenseWorkflowRepository.ExpenseRow expense = requireExpense(expenseId);
        validateApplicant(expense, request.operatorId());
        if (!("DRAFT".equals(expense.status()) || "RETURNED".equals(expense.status()))) {
            throw new BizException("只有草稿或已退回的报销单可以修改");
        }
        int updated = repository.updateEditable(
                expense.id(), request.operatorId(), request.version(), request.departmentCode(),
                request.amount(), request.safeCurrency(), request.description());
        if (updated != 1) {
            throw new BizException("报销单状态或版本已变化，请刷新后重试");
        }
        return toResponse(requireExpense(expenseId));
    }

    public ExpenseWorkflowContracts.ExpenseResponse get(String expenseId) {
        return toResponse(requireExpense(expenseId));
    }

    public ExpenseWorkflowContracts.ApprovalDetailResponse approvalDetail(String expenseId) {
        ExpenseWorkflowRepository.ExpenseRow expense = requireExpense(expenseId);
        ExpenseWorkflowRepository.BindingRow binding = repository
                .findBinding(expense.tenantId(), BUSINESS_TYPE, expense.id())
                .orElse(null);
        JsonNode attachments = gateway.listAttachments(expense.tenantId(), expense.id());
        if (binding == null || !StringUtils.hasText(binding.workflowInstanceId())) {
            return new ExpenseWorkflowContracts.ApprovalDetailResponse(
                    toResponse(expense), bindingMap(binding), null,
                    attachments, objectMapper.createArrayNode(), objectMapper.createArrayNode());
        }
        return new ExpenseWorkflowContracts.ApprovalDetailResponse(
                toResponse(expense), bindingMap(binding),
                gateway.getInstance(binding.workflowInstanceId()), attachments,
                gateway.listTasks(binding.workflowInstanceId()),
                gateway.listTimeline(binding.workflowInstanceId()));
    }

    @Transactional
    public ExpenseWorkflowContracts.ExpenseResponse submit(
            String expenseId,
            ExpenseWorkflowContracts.SubmitExpenseRequest request,
            String requestId) {
        ExpenseWorkflowRepository.ExpenseRow expense = requireExpense(expenseId);
        validateApplicant(expense, request.operatorId());
        if (!("DRAFT".equals(expense.status()) || "RETURNED".equals(expense.status()))) {
            throw new BizException("只有草稿或已退回的报销单可以提交");
        }

        gateway.requireUploadedCategory(expense.tenantId(), expense.id(), "INVOICE", 1);

        String effectiveRequestId = StringUtils.hasText(requestId) ? requestId.trim() : newId();
        ExpenseWorkflowRepository.BindingRow binding = repository
                .findBinding(expense.tenantId(), BUSINESS_TYPE, expense.id())
                .orElse(null);
        boolean resubmit = "RETURNED".equals(expense.status())
                && binding != null
                && StringUtils.hasText(binding.workflowInstanceId());

        int updated = repository.markApproving(
                expense.id(), expense.version(), binding == null ? null : binding.workflowInstanceId());
        if (updated != 1) {
            throw new BizException("报销单状态已变化，请刷新后重试");
        }

        String definitionKey = request.safeDefinitionKey();
        String bindingId = binding == null ? newId() : binding.id();
        String workflowStatus = resubmit ? "RESUBMIT_REQUESTED" : "START_REQUESTED";
        repository.upsertBinding(new ExpenseWorkflowRepository.BindingRow(
                bindingId,
                expense.tenantId(),
                BUSINESS_TYPE,
                expense.id(),
                binding == null ? null : binding.workflowInstanceId(),
                definitionKey,
                workflowStatus,
                "APPROVING",
                effectiveRequestId,
                callbackUrl,
                request.operatorId()
        ));

        String eventId = newId();
        String eventType = resubmit ? EVENT_RESUBMIT : EVENT_START;
        String idempotencyKey = resubmit
                ? "expense-resubmit:" + expense.id() + ":" + effectiveRequestId
                : "expense-start:" + expense.id();
        Map<String, Object> variables = buildVariables(expense);
        Object payload = resubmit
                ? new ExpenseWorkflowContracts.WorkflowResubmitPayload(
                        request.operatorId(), request.comment(), variables)
                : new ExpenseWorkflowContracts.WorkflowStartPayload(
                        expense.tenantId(), definitionKey, SOURCE_SYSTEM, BUSINESS_TYPE,
                        expense.id(), request.operatorId(), variables, callbackUrl);
        repository.insertOutbox(new ExpenseWorkflowRepository.OutboxRow(
                newId(), eventId, BUSINESS_TYPE, expense.id(), eventType,
                writeJson(payload), idempotencyKey, "PENDING", 0, LocalDateTime.now()
        ));
        return toResponse(requireExpense(expense.id()));
    }

    @Transactional
    public ExpenseWorkflowContracts.ExpenseResponse cancel(
            String expenseId,
            ExpenseWorkflowContracts.CancelExpenseRequest request,
            String requestId) {
        ExpenseWorkflowRepository.ExpenseRow expense = requireExpense(expenseId);
        validateApplicant(expense, request.operatorId());
        if (!("APPROVING".equals(expense.status()) || "RETURNED".equals(expense.status()))) {
            throw new BizException("只有审批中或已退回的报销单可以撤销");
        }
        ExpenseWorkflowRepository.BindingRow binding = repository
                .findBinding(expense.tenantId(), BUSINESS_TYPE, expense.id())
                .orElseThrow(() -> new BizException("报销单缺少流程绑定"));
        if (!StringUtils.hasText(binding.workflowInstanceId())) {
            throw new BizException("流程实例尚未创建，暂时不能撤销");
        }
        if ("CANCEL_REQUESTED".equals(binding.workflowStatus())) {
            return toResponse(expense);
        }

        String effectiveRequestId = StringUtils.hasText(requestId) ? requestId.trim() : newId();
        if (repository.markCancelRequested(expense.tenantId(), expense.id(), binding.workflowInstanceId()) != 1) {
            throw new BizException("流程绑定状态已变化，请刷新后重试");
        }
        repository.insertOutbox(new ExpenseWorkflowRepository.OutboxRow(
                newId(), newId(), BUSINESS_TYPE, expense.id(), EVENT_CANCEL,
                writeJson(new ExpenseWorkflowContracts.WorkflowCancelPayload(
                        request.operatorId(), request.reason())),
                "expense-cancel:" + expense.id() + ":" + effectiveRequestId,
                "PENDING", 0, LocalDateTime.now()
        ));
        return toResponse(expense);
    }

    private Map<String, Object> buildVariables(ExpenseWorkflowRepository.ExpenseRow expense) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("documentNumber", expense.documentNumber());
        variables.put("applicantId", expense.applicantId());
        variables.put("departmentCode", expense.departmentCode());
        variables.put("amount", expense.amount());
        variables.put("currency", expense.currency());
        variables.put("description", expense.description());
        return variables;
    }

    private Map<String, Object> bindingMap(ExpenseWorkflowRepository.BindingRow binding) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (binding == null) {
            return result;
        }
        result.put("id", binding.id());
        result.put("workflowInstanceId", binding.workflowInstanceId());
        result.put("workflowDefinitionKey", binding.workflowDefinitionKey());
        result.put("workflowStatus", binding.workflowStatus());
        result.put("businessStatus", binding.businessStatus());
        result.put("submittedBy", binding.submittedBy());
        return result;
    }

    private void validateApplicant(ExpenseWorkflowRepository.ExpenseRow expense, String operatorId) {
        if (!expense.applicantId().equals(operatorId)) {
            throw new BizException("只有报销单申请人可以执行该操作");
        }
    }

    private ExpenseWorkflowRepository.ExpenseRow requireExpense(String expenseId) {
        return repository.findExpense(expenseId)
                .orElseThrow(() -> new BizException("报销单不存在"));
    }

    private ExpenseWorkflowContracts.ExpenseResponse toResponse(
            ExpenseWorkflowRepository.ExpenseRow row) {
        return new ExpenseWorkflowContracts.ExpenseResponse(
                row.id(), row.tenantId(), row.documentNumber(), row.applicantId(),
                row.departmentCode(), row.amount(), row.currency(), row.description(),
                row.status(), row.workflowInstanceId(), row.version(), row.createdAt(),
                row.submittedAt(), row.completedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException("报销工作流事件无法序列化: " + ex.getOriginalMessage());
        }
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
