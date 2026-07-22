package single.cjj.botp.execution;

import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.adapter.BotpAdapterRegistry;
import single.cjj.botp.adapter.BotpDocumentAdapter;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.ExecutionMode;
import single.cjj.botp.domain.BotpContracts.ExecutionRequest;
import single.cjj.botp.domain.BotpContracts.ExecutionResult;
import single.cjj.botp.domain.BotpContracts.ExecutionStatus;
import single.cjj.botp.domain.BotpContracts.PreviewResult;
import single.cjj.botp.domain.BotpContracts.RuleDefinition;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.engine.BotpMappingEngine;
import single.cjj.botp.rule.BotpRuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultBotpExecutionService implements BotpExecutionService {

    private final BotpRuleRepository ruleRepository;
    private final BotpAdapterRegistry adapterRegistry;
    private final BotpMappingEngine mappingEngine;
    private final Map<String, ExecutionResult> executionById = new ConcurrentHashMap<>();
    private final Map<String, String> executionIdByRequestKey = new ConcurrentHashMap<>();

    public DefaultBotpExecutionService(
            BotpRuleRepository ruleRepository,
            BotpAdapterRegistry adapterRegistry,
            BotpMappingEngine mappingEngine
    ) {
        this.ruleRepository = ruleRepository;
        this.adapterRegistry = adapterRegistry;
        this.mappingEngine = mappingEngine;
    }

    @Override
    public PreviewResult preview(ExecutionRequest request) {
        RuleDefinition rule = requirePublishedRule(request.ruleCode());
        List<SourceAndDraft> transformed = transformSources(request, rule);
        List<TargetDraft> drafts = transformed.stream()
                .map(SourceAndDraft::targetDraft)
                .toList();
        List<String> warnings = drafts.size() > 1
                ? List.of("V1 按源单逐张生成目标草稿，尚未启用多源合并")
                : List.of();
        return new PreviewResult(rule.ruleCode(), rule.version(), drafts, warnings);
    }

    @Override
    public synchronized ExecutionResult execute(ExecutionRequest request) {
        if (request.executionMode() == ExecutionMode.ASYNC) {
            throw new BizException("BOTP V1 暂未开放 ASYNC 执行，请使用 SYNC");
        }

        String requestKey = buildRequestKey(request);
        String existingExecutionId = executionIdByRequestKey.get(requestKey);
        if (existingExecutionId != null) {
            return getById(existingExecutionId);
        }

        RuleDefinition rule = requirePublishedRule(request.ruleCode());
        String executionId = newExecutionId();
        executionIdByRequestKey.put(requestKey, executionId);
        save(executionId, rule, ExecutionStatus.CREATED, List.of(), null);

        try {
            List<SourceAndDraft> transformed = transformSources(request, rule);
            save(executionId, rule, ExecutionStatus.TRANSFORMING, List.of(), null);

            BotpDocumentAdapter targetAdapter = adapterRegistry.require(
                    rule.targetSystemCode(),
                    rule.targetDocumentType()
            );
            List<TargetResult> targets = new ArrayList<>();

            for (int index = 0; index < transformed.size(); index++) {
                save(executionId, rule, ExecutionStatus.TARGET_CREATING, targets, null);
                String targetIdempotencyKey = buildTargetIdempotencyKey(request.tenantId(), executionId, index);
                TargetDraft draft = transformed.get(index).targetDraft();
                TargetResult target = targetAdapter.findByIdempotencyKey(targetIdempotencyKey)
                        .orElseGet(() -> targetAdapter.createTarget(draft, targetIdempotencyKey));
                targets.add(target);
                save(executionId, rule, ExecutionStatus.TARGET_CREATED, targets, null);
            }

            save(executionId, rule, ExecutionStatus.RELATION_SAVED, targets, null);

            try {
                for (int index = 0; index < transformed.size(); index++) {
                    SourceAndDraft sourceAndDraft = transformed.get(index);
                    TargetResult target = targets.get(index);
                    sourceAndDraft.sourceAdapter().applyWriteback(new WritebackCommand(
                            executionId,
                            sourceAndDraft.sourceDocument().reference(),
                            target,
                            rule.writebackMappings()
                    ));
                }
            } catch (RuntimeException writebackException) {
                return save(
                        executionId,
                        rule,
                        ExecutionStatus.WRITEBACK_PENDING,
                        targets,
                        safeMessage(writebackException)
                );
            }

            return save(executionId, rule, ExecutionStatus.SUCCEEDED, targets, null);
        } catch (RuntimeException exception) {
            return save(
                    executionId,
                    rule,
                    ExecutionStatus.FAILED,
                    List.of(),
                    safeMessage(exception)
            );
        }
    }

    @Override
    public ExecutionResult getById(String executionId) {
        ExecutionResult result = executionById.get(executionId);
        if (result == null) {
            throw new BizException("BOTP 执行任务不存在: " + executionId);
        }
        return result;
    }

    private List<SourceAndDraft> transformSources(ExecutionRequest request, RuleDefinition rule) {
        if (request.sourceDocuments().isEmpty()) {
            throw new BizException("BOTP 至少需要一个源单引用");
        }

        List<SourceAndDraft> transformed = new ArrayList<>();
        for (DocumentRef sourceRef : request.sourceDocuments()) {
            validateSourceRef(rule, sourceRef);
            BotpDocumentAdapter sourceAdapter = adapterRegistry.require(
                    sourceRef.systemCode(),
                    sourceRef.documentType()
            );
            DocumentData sourceDocument = sourceAdapter.load(sourceRef);
            sourceAdapter.validateSource(sourceDocument, request.parameters());
            TargetDraft targetDraft = mappingEngine.transform(rule, sourceDocument, request.parameters());
            transformed.add(new SourceAndDraft(sourceAdapter, sourceDocument, targetDraft));
        }
        return transformed;
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

    private String buildRequestKey(ExecutionRequest request) {
        return request.tenantId() + "|" + request.sourceSystem() + "|" + request.requestId();
    }

    private String buildTargetIdempotencyKey(String tenantId, String executionId, int targetIndex) {
        return "botp:" + tenantId + ":" + executionId + ":" + targetIndex;
    }

    private String newExecutionId() {
        return "BOTP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private ExecutionResult save(
            String executionId,
            RuleDefinition rule,
            ExecutionStatus status,
            List<TargetResult> targets,
            String errorMessage
    ) {
        ExecutionResult result = new ExecutionResult(
                executionId,
                rule.ruleCode(),
                rule.version(),
                status,
                targets,
                errorMessage
        );
        executionById.put(executionId, result);
        return result;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private record SourceAndDraft(
            BotpDocumentAdapter sourceAdapter,
            DocumentData sourceDocument,
            TargetDraft targetDraft
    ) {
    }
}
