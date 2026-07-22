package single.cjj.fi.expense.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;

@Service
public class ExpenseWorkflowCallbackService {

    private final ExpenseWorkflowRepository repository;

    public ExpenseWorkflowCallbackService(ExpenseWorkflowRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean process(ExpenseWorkflowContracts.WorkflowEventRequest event) {
        if (!ExpenseWorkflowService.SOURCE_SYSTEM.equals(event.sourceSystem())) {
            throw new BizException("工作流事件来源系统不匹配");
        }
        if (!ExpenseWorkflowService.BUSINESS_TYPE.equals(event.businessType())) {
            throw new BizException("工作流事件业务类型不匹配");
        }

        int inserted = repository.insertWorkflowEventIfAbsent(
                new ExpenseWorkflowRepository.WorkflowEventRow(
                        event.eventId(), event.eventType(), event.instanceId(),
                        event.businessType(), event.businessId(), LocalDateTime.now()
                ));
        if (inserted == 0) {
            return false;
        }

        StatusMapping mapping = map(event.eventType());
        int affected = repository.applyWorkflowEvent(
                event.tenantId(), event.businessId(), event.instanceId(),
                mapping.businessStatus(), mapping.workflowStatus(), mapping.terminal());
        if (affected != 1) {
            throw new BizException("工作流事件对应的报销单不存在或流程实例不匹配");
        }
        repository.markWorkflowEventProcessed(event.eventId());
        return true;
    }

    private StatusMapping map(String eventType) {
        return switch (eventType) {
            case "INSTANCE_RETURNED" -> new StatusMapping("RETURNED", "WAITING_RESUBMIT", false);
            case "INSTANCE_RESUBMITTED" -> new StatusMapping("APPROVING", "RUNNING", false);
            case "INSTANCE_COMPLETED" -> new StatusMapping("APPROVED", "COMPLETED", true);
            case "INSTANCE_REJECTED" -> new StatusMapping("REJECTED", "REJECTED", true);
            case "INSTANCE_CANCELLED" -> new StatusMapping("CANCELLED", "CANCELLED", true);
            default -> throw new BizException("不支持的工作流事件类型: " + eventType);
        };
    }

    private record StatusMapping(String businessStatus, String workflowStatus, boolean terminal) {
    }
}
