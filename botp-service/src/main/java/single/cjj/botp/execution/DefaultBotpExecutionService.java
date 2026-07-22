package single.cjj.botp.execution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.ExecutionDetails;
import single.cjj.botp.domain.BotpContracts.ExecutionMode;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionResult;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.PreviewResult;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.TaskStatus;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.engine.BotpMappingEngine;
import single.cjj.botp.relation.BotpRelationRepository;
import single.cjj.botp.relation.InMemoryBotpRelationRepository;
import single.cjj.botp.rule.BotpRuleRepository;
import single.cjj.botp.writeback.BotpWritebackService;
import single.cjj.botp.writeback.InMemoryBotpWritebackTaskRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DefaultBotpExecutionService implements BotpExecutionService {

    private final BotpRuleRepository ruleRepository;
    private final BotpAdapterRegistry adapterRegistry;
    private final BotpMappingEngine mappingEngine;
    private final BotpExecutionStore executionStore;
    private final BotpRelationRepository relationRepository;
    private final BotpExecutionLogRepository logRepository;
    private final BotpWritebackService writebackService;

    @Autowired
    public DefaultBotpExecutionService(
            BotpRuleRepository ruleRepository,
            BotpAdapterRegistry adapterRegistry,
            BotpMappingEngine mappingEngine,
            BotpExecutionStore executionStore,
            BotpRelationRepository relationRepository,
            BotpExecutionLogRepository logRepository,
            BotpWritebackService writebackService
    ) {
        this.ruleRepository = ruleRepository;
        this.adapterRegistry = adapterRegistry;
        this.mappingEngine = mappingEngine;
        this.executionStore = executionStore;
        this.relationRepository = relationRepository;
        this.logRepository = logRepository;
        this.writebackService = writebackService;
    }

    /** 保留给纯单元测试和嵌入式调用。 */
    public DefaultBotpExecutionService(
            BotpRuleRepository ruleRepository,
            BotpAdapterRegistry adapterRegistry,
            BotpMappingEngine mappingEngine
    ) {
        this(ruleRepository, adapterRegistry, mappingEngine, createTestDependencies(adapterRegistry));
    }

    private DefaultBotpExecutionService(
            BotpRuleRepository ruleRepository,
            BotpAdapterRegistry adapterRegistry,
            BotpMappingEngine mappingEngine,
            TestDependencies dependencies
    ) {
        this(
                ruleRepository, adapterRegistry, mappingEngine,
                dependencies.executionStore(), dependencies.relationRepository(),
                dependencies.logRepository(), dependencies.writebackService()
        );
    }

    @Override
    public PreviewResult preview(ExecutionRequest request) {
        RuleDefinition rule = requirePublishedRule(request.ruleCode());
        List<SourceAndDraft> transformed = transformSources(request, rule, "PREVIEW");
        List<TargetDraft> drafts = transformed.stream().map(SourceAndDraft::targetDraft).toList();
        List<String> warnings = drafts.size() > 1
                ? List.of("V3 仍按源单逐张生成目标草稿，尚未启用多源合并")
                : List.of();
        return new PreviewResult(rule.ruleCode(), rule.version(), drafts, warnings);
    }

    @Override
    public synchronized ExecutionResult execute(ExecutionRequest request) {
        if (request.executionMode() == ExecutionMode.ASYNC) {
            throw new BizException("BOTP V3 当前仅开放 SYNC 创建，失败补偿由异步任务执行");
        }

        ExecutionDetails existing = executionStore.findByRequest(
                request.tenantId(), request.sourceSystem(), request.requestId()).orElse(null);
        if (existing != null) {
            return existing.toResult();
        }

        RuleDefinition rule = requirePublishedRule(request.ruleCode());
        String executionId = newExecutionId();
        try {
            save(request, rule, executionId, ExecutionStatus.CREATED, List.of(), null);
        } catch (DuplicateKeyException duplicateKeyException) {
            return executionStore.findByRequest(request.tenantId(), request.sourceSystem(), request.requestId())
                    .map(ExecutionDetails::toResult)
                    .orElseThrow(() -> duplicateKeyException);
        }

        List<TargetResult> targets = new ArrayList<>();
        try {
            save(request, rule, executionId, ExecutionStatus.VALIDATING, targets, null);
            save(request, rule, executionId, ExecutionStatus.SOURCE_LOADING, targets, null);
            List<SourceAndDraft> transformed = transformSources(request, rule, executionId);
            save(request, rule, executionId, ExecutionStatus.TRANSFORMING, targets, null);

            BotpDocumentAdapter targetAdapter = adapterRegistry.require(
                    rule.targetSystemCode(), rule.targetDocumentType());

            for (int index = 0; index < transformed.size(); index++) {
                SourceAndDraft sourceAndDraft = transformed.get(index);
                save(request, rule, executionId, ExecutionStatus.TARGET_CREATING, targets, null);
                String targetIdempotencyKey = buildTargetIdempotencyKey(request.tenantId(), executionId, index);
                TargetResult target = targetAdapter.findByIdempotencyKey(targetIdempotencyKey)
                        .orElseGet(() -> targetAdapter.createTarget(sourceAndDraft.targetDraft(), targetIdempotencyKey));
                targets.add(target);
                save(request, rule, executionId, ExecutionStatus.TARGET_CREATED, targets, null);

                save(request, rule, executionId, ExecutionStatus.RELATION_SAVING, targets, null);
                DocumentRelation relation = relationRepository.saveActive(
                        request.tenantId(), executionId, rule,
                        sourceAndDraft.sourceDocument().reference(), target, sourceAndDraft.allocatedAmount());
                save(request, rule, executionId, ExecutionStatus.RELATION_SAVED, targets, null);

                BigDecimal activeAmount = relationRepository.sumActiveAmount(
                        request.tenantId(), sourceAndDraft.sourceDocument().reference());
                Map<String, Object> writebackContext = new LinkedHashMap<>(sourceAndDraft.context());
                writebackContext.put("activeAllocatedAmount", activeAmount);
                writebackContext.put("releaseReservedAmount", sourceAndDraft.allocatedAmount());

                try {
                    save(request, rule, executionId, ExecutionStatus.WRITEBACK_PROCESSING, targets, null);
                    sourceAndDraft.sourceAdapter().applyWriteback(new WritebackCommand(
                            executionId,
                            sourceAndDraft.sourceDocument().reference(),
                            target,
                            rule.writebackMappings(),
                            writebackContext
                    ));
                } catch (RuntimeException writebackException) {
                    writebackService.enqueueForward(
                            request.tenantId(), executionId, relation, activeAmount, sourceAndDraft.allocatedAmount());
                    return save(
                            request, rule, executionId, ExecutionStatus.WRITEBACK_PENDING,
                            targets, safeMessage(writebackException));
                }
            }

            return save(request, rule, executionId, ExecutionStatus.SUCCEEDED, targets, null);
        } catch (RuntimeException exception) {
            return save(
                    request, rule, executionId, ExecutionStatus.FAILED,
                    targets, safeMessage(exception));
        }
    }

    @Override
    public ExecutionResult getById(String executionId) {
        return executionStore.findById(executionId)
                .map(ExecutionDetails::toResult)
                .orElseThrow(() -> new BizException("BOTP 执行任务不存在: " + executionId));
    }

    @Override
    public List<ExecutionDetails> list(int limit) {
        return executionStore.list(limit);
    }

    private List<SourceAndDraft> transformSources(ExecutionRequest request, RuleDefinition rule, String executionId) {
        if (request.sourceDocuments().isEmpty()) {
            throw new BizException("BOTP 至少需要一个源单引用");
        }
        List<SourceAndDraft> transformed = new ArrayList<>();
        for (DocumentRef sourceRef : request.sourceDocuments()) {
            validateSourceRef(rule, sourceRef);
            BotpDocumentAdapter sourceAdapter = adapterRegistry.require(
                    sourceRef.systemCode(), sourceRef.documentType());
            DocumentData sourceDocument = sourceAdapter.load(sourceRef);
            Map<String, Object> context = enrichContext(request, sourceRef, executionId);
            sourceAdapter.validateSource(sourceDocument, context);
            TargetDraft targetDraft = mappingEngine.transform(rule, sourceDocument, context);
            transformed.add(new SourceAndDraft(
                    sourceAdapter, sourceDocument, targetDraft, context,
                    resolveAllocatedAmount(context, targetDraft)
            ));
        }
        return transformed;
    }

    private Map<String, Object> enrichContext(ExecutionRequest request, DocumentRef sourceRef, String executionId) {
        Map<String, Object> context = new LinkedHashMap<>(request.parameters());
        context.put("executionId", executionId);
        context.put("sourceSystemCode", sourceRef.systemCode());
        context.put("sourceDocumentType", sourceRef.documentType());
        context.put("sourceDocumentId", sourceRef.documentId());
        context.putIfAbsent("operator", context.get("operatorId"));
        return context;
    }

    private BigDecimal resolveAllocatedAmount(Map<String, Object> context, TargetDraft targetDraft) {
        Object value = context.get("pushAmount");
        if (value == null) {
            value = context.get("allocatedAmount");
        }
        if (value == null) {
            value = targetDraft.header().get("amount");
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount;
        if (value instanceof BigDecimal decimal) {
            amount = decimal;
        } else if (value instanceof Number number) {
            amount = new BigDecimal(number.toString());
        } else {
            try {
                amount = new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException exception) {
                throw new BizException("下推分配金额格式错误: " + value);
            }
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("下推分配金额不能小于0");
        }
        return amount;
    }

    private RuleDefinition requirePublishedRule(String ruleCode) {
        return ruleRepository.findPublishedByCode(ruleCode)
                .orElseThrow(() -> new BizException("BOTP 已发布规则不存在: " + ruleCode));
    }

    private void validateSourceRef(RuleDefinition rule, DocumentRef sourceRef) {
        if (!rule.sourceSystemCode().equals(sourceRef.systemCode())
                || !rule.sourceDocumentType().equals(sourceRef.documentType())) {
            throw new BizException(
                    "源单类型与规则不匹配: expected="
                            + rule.sourceSystemCode() + "/" + rule.sourceDocumentType()
                            + ", actual=" + sourceRef.systemCode() + "/" + sourceRef.documentType()
            );
        }
    }

    private String buildTargetIdempotencyKey(String tenantId, String executionId, int targetIndex) {
        return "botp:" + tenantId + ":" + executionId + ":" + targetIndex;
    }

    private String newExecutionId() {
        return "BOTP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private ExecutionResult save(
            ExecutionRequest request,
            RuleDefinition rule,
            String executionId,
            ExecutionStatus status,
            List<TargetResult> targets,
            String errorMessage
    ) {
        ExecutionDetails details = executionStore.save(
                request, rule, executionId, status, targets, errorMessage);
        logRepository.append(
                executionId,
                status.name(),
                logStatus(status),
                errorMessage == null ? "执行阶段完成: " + status.name() : errorMessage,
                status == ExecutionStatus.CREATED ? request.toString() : null,
                targets.isEmpty() ? null : targets.toString(),
                errorMessage == null ? null : new RuntimeException(errorMessage)
        );
        return details.toResult();
    }

    private TaskStatus logStatus(ExecutionStatus status) {
        if (status == ExecutionStatus.SUCCEEDED || status == ExecutionStatus.REVERSED) {
            return TaskStatus.SUCCEEDED;
        }
        if (status == ExecutionStatus.FAILED || status == ExecutionStatus.WRITEBACK_PENDING) {
            return TaskStatus.FAILED;
        }
        return TaskStatus.PROCESSING;
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }

    private static TestDependencies createTestDependencies(BotpAdapterRegistry adapterRegistry) {
        InMemoryBotpExecutionStore executionStore = new InMemoryBotpExecutionStore();
        InMemoryBotpRelationRepository relationRepository = new InMemoryBotpRelationRepository();
        InMemoryBotpExecutionLogRepository logRepository = new InMemoryBotpExecutionLogRepository();
        BotpWritebackService writebackService = new BotpWritebackService(
                new InMemoryBotpWritebackTaskRepository(), adapterRegistry, relationRepository, logRepository);
        return new TestDependencies(executionStore, relationRepository, logRepository, writebackService);
    }

    private record SourceAndDraft(
            BotpDocumentAdapter sourceAdapter,
            DocumentData sourceDocument,
            TargetDraft targetDraft,
            Map<String, Object> context,
            BigDecimal allocatedAmount
    ) {
    }

    private record TestDependencies(
            BotpExecutionStore executionStore,
            BotpRelationRepository relationRepository,
            BotpExecutionLogRepository logRepository,
            BotpWritebackService writebackService
    ) {
    }
}
