package single.cjj.botp.relation;

import org.springframework.stereotype.Service;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.RelationInvalidateRequest;
import single.cjj.botp.domain.BotpContracts.TargetStatusEvent;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackTask;
import single.cjj.botp.execution.BotpExecutionLogRepository;
import single.cjj.botp.writeback.BotpWritebackService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BotpRelationLifecycleService {

    private final BotpRelationRepository relationRepository;
    private final BotpWritebackService writebackService;
    private final BotpExecutionLogRepository logRepository;

    public BotpRelationLifecycleService(
            BotpRelationRepository relationRepository,
            BotpWritebackService writebackService,
            BotpExecutionLogRepository logRepository
    ) {
        this.relationRepository = relationRepository;
        this.writebackService = writebackService;
        this.logRepository = logRepository;
    }

    public List<DocumentRelation> handleTargetStatusEvent(TargetStatusEvent event) {
        if (!isInvalidatingStatus(event.targetStatus())) {
            return relationRepository.findByTarget(event.tenantId(), event.targetDocumentId());
        }
        List<DocumentRelation> invalidated = relationRepository.invalidateByTarget(
                event.tenantId(), event.targetDocumentId(), event.eventId(), event.targetStatus(), event.reason());
        for (DocumentRelation relation : invalidated) {
            enqueueAndRunReverse(relation, "目标单状态变更: " + event.targetStatus());
        }
        return invalidated;
    }

    public DocumentRelation invalidateManually(Long relationId, RelationInvalidateRequest request) {
        DocumentRelation relation = relationRepository.invalidateById(relationId, request.eventId(), request.reason());
        if (relation.status().name().equals("INVALID")) {
            enqueueAndRunReverse(relation, "人工失效关系");
        }
        return relationRepository.findById(relationId).orElse(relation);
    }

    public WritebackTask recompute(Long relationId) {
        DocumentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("BOTP 单据关系不存在: " + relationId));
        BigDecimal activeAmount = relationRepository.sumActiveAmount(relation.tenantId(), relation.sourceDocument());
        WritebackTask task = writebackService.enqueueRecompute(
                relation.tenantId(), relation.executionId(), relation, activeAmount);
        writebackService.processTask(task.taskId());
        return writebackService.list(500).stream()
                .filter(item -> item.taskId().equals(task.taskId()))
                .findFirst().orElse(task);
    }

    private void enqueueAndRunReverse(DocumentRelation relation, String reason) {
        BigDecimal activeAmount = relationRepository.sumActiveAmount(relation.tenantId(), relation.sourceDocument());
        logRepository.append(
                relation.executionId(), "REVERSE_PENDING", TaskStatus.PENDING,
                reason + "，有效关联金额重算为 " + activeAmount, null, null, null
        );
        WritebackTask task = writebackService.enqueueReverse(relation, activeAmount);
        writebackService.processTask(task.taskId());
    }

    private boolean isInvalidatingStatus(String status) {
        return "VOID".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)
                || "DELETED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status);
    }
}
