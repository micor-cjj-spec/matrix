package single.cjj.fi.expense.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ExpenseWorkflowOutboxDispatcher {

    private final ExpenseWorkflowRepository repository;
    private final ExpenseWorkflowGateway gateway;
    private final int batchSize;

    public ExpenseWorkflowOutboxDispatcher(
            ExpenseWorkflowRepository repository,
            ExpenseWorkflowGateway gateway,
            @Value("${fi.workflow.outbox.batch-size:20}") int batchSize) {
        this.repository = repository;
        this.gateway = gateway;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${fi.workflow.outbox.dispatch-delay-ms:5000}")
    public void dispatch() {
        List<ExpenseWorkflowRepository.OutboxRow> rows = repository.findDispatchable(batchSize);
        for (ExpenseWorkflowRepository.OutboxRow row : rows) {
            if (!repository.claimOutbox(row.id())) {
                continue;
            }
            dispatchOne(row);
        }
    }

    private void dispatchOne(ExpenseWorkflowRepository.OutboxRow row) {
        try {
            ExpenseWorkflowRepository.ExpenseRow expense = repository.findExpense(row.aggregateId())
                    .orElseThrow(() -> new IllegalStateException("报销单不存在: " + row.aggregateId()));
            ExpenseWorkflowRepository.BindingRow binding = repository
                    .findBinding(expense.tenantId(), ExpenseWorkflowService.BUSINESS_TYPE, expense.id())
                    .orElseThrow(() -> new IllegalStateException("报销单缺少流程绑定"));

            ExpenseWorkflowGateway.WorkflowResult result;
            if (ExpenseWorkflowService.EVENT_START.equals(row.eventType())) {
                result = gateway.start(row.payloadJson(), row.idempotencyKey());
            } else if (ExpenseWorkflowService.EVENT_RESUBMIT.equals(row.eventType())) {
                if (!StringUtils.hasText(binding.workflowInstanceId())) {
                    throw new IllegalStateException("重新提交缺少 workflowInstanceId");
                }
                result = gateway.resubmit(
                        binding.workflowInstanceId(), row.payloadJson(), row.idempotencyKey());
            } else {
                throw new IllegalStateException("不支持的业务 Outbox 事件: " + row.eventType());
            }

            repository.bindWorkflowInstance(
                    expense.tenantId(), ExpenseWorkflowService.BUSINESS_TYPE,
                    expense.id(), result.instanceId(), result.status());
            repository.markOutboxSent(row.id());
        } catch (Exception ex) {
            long delaySeconds = Math.min(3600L, 5L * (1L << Math.min(row.retryCount(), 9)));
            LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);
            repository.markOutboxFailed(row.id(), nextRetry, ex.getMessage());
            log.warn("expense workflow outbox failed, eventId={}, nextRetry={}",
                    row.eventId(), nextRetry, ex);
        }
    }
}
