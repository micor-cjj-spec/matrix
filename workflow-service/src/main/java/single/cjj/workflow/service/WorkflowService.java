package single.cjj.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.workflow.api.WorkflowContracts;
import single.cjj.workflow.engine.WorkflowConditionEvaluator;
import single.cjj.workflow.engine.WorkflowNodeHandler;
import single.cjj.workflow.engine.WorkflowNodeHandlerRegistry;
import single.cjj.workflow.model.WorkflowDefinition;
import single.cjj.workflow.repository.WorkflowRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkflowService {

    private static final String INSTANCE_RUNNING = "RUNNING";
    private static final String INSTANCE_COMPLETED = "COMPLETED";
    private static final String INSTANCE_REJECTED = "REJECTED";

    private final WorkflowRepository repository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final WorkflowNodeHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository repository,
                           WorkflowConditionEvaluator conditionEvaluator,
                           WorkflowNodeHandlerRegistry handlerRegistry,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.conditionEvaluator = conditionEvaluator;
        this.handlerRegistry = handlerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowContracts.DefinitionResponse createDefinition(
            WorkflowContracts.DefinitionCreateRequest request) {
        validateDefinition(request.definition());
        repository.ensureDefinition(
                request.tenantId(),
                request.definitionKey(),
                request.definitionName(),
                request.createdBy()
        );
        int version = repository.nextDefinitionVersion(request.tenantId(), request.definitionKey());
        repository.insertDefinitionVersion(
                request.tenantId(),
                request.definitionKey(),
                request.definitionName(),
                version,
                writeJson(request.definition()),
                request.createdBy()
        );
        return getDefinition(request.tenantId(), request.definitionKey(), version);
    }

    @Transactional
    public WorkflowContracts.DefinitionResponse publishDefinition(String tenantId,
                                                                  String definitionKey,
                                                                  int version) {
        WorkflowRepository.DefinitionVersionRow row = repository
                .findDefinition(tenantId, definitionKey, version)
                .orElseThrow(() -> new BizException("流程定义版本不存在"));
        validateDefinition(readDefinition(row.definitionJson()));
        int affected = repository.publishDefinition(tenantId, definitionKey, version);
        if (affected != 1) {
            throw new BizException("只有草稿版本可以发布");
        }
        return getDefinition(tenantId, definitionKey, version);
    }

    public WorkflowContracts.DefinitionResponse getPublishedDefinition(String tenantId,
                                                                       String definitionKey) {
        WorkflowRepository.DefinitionVersionRow row = repository
                .findPublishedDefinition(tenantId, definitionKey)
                .orElseThrow(() -> new BizException("没有已发布的流程定义"));
        return toDefinitionResponse(row);
    }

    public WorkflowContracts.DefinitionResponse getDefinition(String tenantId,
                                                              String definitionKey,
                                                              int version) {
        WorkflowRepository.DefinitionVersionRow row = repository
                .findDefinition(tenantId, definitionKey, version)
                .orElseThrow(() -> new BizException("流程定义版本不存在"));
        return toDefinitionResponse(row);
    }

    @Transactional
    public WorkflowContracts.InstanceResponse startWorkflow(
            WorkflowContracts.StartWorkflowRequest request,
            String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BizException("缺少 Idempotency-Key 请求头");
        }

        WorkflowRepository.InstanceRow existing = repository
                .findByIdempotency(request.tenantId(), request.sourceSystem(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return toInstanceResponse(existing);
        }

        WorkflowRepository.DefinitionVersionRow definitionRow = repository
                .findPublishedDefinition(request.tenantId(), request.definitionKey())
                .orElseThrow(() -> new BizException("没有可启动的已发布流程定义"));
        WorkflowDefinition definition = readDefinition(definitionRow.definitionJson());
        validateDefinition(definition);

        String instanceId = newId();
        Map<String, Object> variables = request.safeVariables();
        variables.putIfAbsent("initiatorId", request.initiatorId());
        String businessKey = request.sourceSystem() + ":" + request.businessType() + ":" + request.businessId();
        LocalDateTime now = LocalDateTime.now();

        WorkflowRepository.InstanceRow instance = new WorkflowRepository.InstanceRow(
                instanceId,
                request.tenantId(),
                request.definitionKey(),
                definitionRow.version(),
                request.sourceSystem(),
                request.businessType(),
                request.businessId(),
                businessKey,
                idempotencyKey,
                request.initiatorId(),
                null,
                INSTANCE_RUNNING,
                writeJson(variables),
                request.callbackUrl(),
                0,
                now,
                null
        );
        repository.insertInstance(instance);
        repository.insertActionLog(
                newId(), instanceId, null, "START", request.initiatorId(),
                "启动流程", null, INSTANCE_RUNNING, idempotencyKey
        );

        WorkflowDefinition.Node startNode = definition.requireStartNode();
        String startNodeInstanceId = createNodeInstance(instanceId, startNode, variables);
        repository.completeNode(startNodeInstanceId, "{}");
        advanceFrom(instance, definition, startNode.getKey(), variables);
        return getInstance(instanceId);
    }

    @Transactional
    public WorkflowContracts.InstanceResponse actOnTask(String taskId,
                                                        WorkflowContracts.TaskActionRequest request,
                                                        String requestId) {
        WorkflowRepository.TaskRow task = repository.findTask(taskId)
                .orElseThrow(() -> new BizException("待办任务不存在"));
        if (!("PENDING".equals(task.status()) || "CLAIMED".equals(task.status()))) {
            throw new BizException("待办任务已经处理");
        }
        validateOperator(task, request.operatorId());

        WorkflowRepository.InstanceRow instance = repository.findInstance(task.instanceId())
                .orElseThrow(() -> new BizException("流程实例不存在"));
        if (!INSTANCE_RUNNING.equals(instance.status())) {
            throw new BizException("流程实例不在运行中");
        }

        WorkflowRepository.DefinitionVersionRow definitionRow = repository
                .findDefinition(instance.tenantId(), instance.definitionKey(), instance.definitionVersion())
                .orElseThrow(() -> new BizException("流程实例对应的定义版本不存在"));
        WorkflowDefinition definition = readDefinition(definitionRow.definitionJson());

        Map<String, Object> variables = readMap(instance.variablesJson());
        variables.putAll(request.safeVariables());
        String taskTerminalStatus = request.action() == WorkflowContracts.TaskAction.APPROVE
                ? "APPROVED" : "REJECTED";
        int affected = repository.completeTask(taskId, task.version(), taskTerminalStatus);
        if (affected != 1) {
            throw new BizException("任务已被其他请求处理，请刷新后重试");
        }
        repository.completeNode(task.nodeInstanceId(), writeJson(Map.of(
                "action", request.action().name(),
                "operatorId", request.operatorId()
        )));
        repository.insertActionLog(
                newId(), instance.id(), task.id(), request.action().name(),
                request.operatorId(), request.comment(), task.status(), taskTerminalStatus, requestId
        );

        if (request.action() == WorkflowContracts.TaskAction.REJECT) {
            int finished = repository.finishInstance(instance.id(), INSTANCE_REJECTED, writeJson(variables));
            if (finished != 1) {
                throw new BizException("流程实例状态已变化，请刷新后重试");
            }
            emitTerminalEvent(instance, INSTANCE_REJECTED, variables);
        } else {
            advanceFrom(instance, definition, task.nodeKey(), variables);
        }
        return getInstance(instance.id());
    }

    public WorkflowContracts.InstanceResponse getInstance(String instanceId) {
        return repository.findInstance(instanceId)
                .map(this::toInstanceResponse)
                .orElseThrow(() -> new BizException("流程实例不存在"));
    }

    public WorkflowContracts.InstanceResponse getBusinessInstance(String tenantId,
                                                                  String sourceSystem,
                                                                  String businessType,
                                                                  String businessId) {
        return repository.findLatestBusinessInstance(tenantId, sourceSystem, businessType, businessId)
                .map(this::toInstanceResponse)
                .orElseThrow(() -> new BizException("业务对象没有流程实例"));
    }

    public WorkflowContracts.TaskResponse getTask(String taskId) {
        return repository.findTask(taskId)
                .map(this::toTaskResponse)
                .orElseThrow(() -> new BizException("待办任务不存在"));
    }

    private void advanceFrom(WorkflowRepository.InstanceRow instance,
                             WorkflowDefinition definition,
                             String completedNodeKey,
                             Map<String, Object> variables) {
        String nextNodeKey = conditionEvaluator.resolveNextNode(definition, completedNodeKey, variables);
        int guard = Math.max(definition.getNodes().size() * 2, 10);

        while (guard-- > 0) {
            WorkflowDefinition.Node node = definition.requireNode(nextNodeKey);
            switch (node.getType()) {
                case START -> throw new BizException("流程不能回到 START 节点");
                case USER_TASK -> {
                    activateUserTask(instance, node, variables);
                    return;
                }
                case SERVICE_TASK -> {
                    String nodeInstanceId = createNodeInstance(instance.id(), node, variables);
                    repository.updateInstancePosition(instance.id(), node.getKey(), writeJson(variables));
                    WorkflowNodeHandler.ExecutionResult result = handlerRegistry
                            .require(node.getHandlerKey())
                            .execute(new WorkflowNodeHandler.ExecutionContext(
                                    instance.id(), node, Map.copyOf(variables)
                            ));
                    if (!result.success()) {
                        throw new BizException(result.message());
                    }
                    if (result.output() != null && !result.output().isEmpty()) {
                        variables.putAll(result.output());
                    }
                    repository.completeNode(nodeInstanceId, writeJson(result.output()));
                    nextNodeKey = conditionEvaluator.resolveNextNode(definition, node.getKey(), variables);
                }
                case EXCLUSIVE_GATEWAY -> {
                    String nodeInstanceId = createNodeInstance(instance.id(), node, variables);
                    repository.completeNode(nodeInstanceId, "{}");
                    nextNodeKey = conditionEvaluator.resolveNextNode(definition, node.getKey(), variables);
                }
                case END -> {
                    String nodeInstanceId = createNodeInstance(instance.id(), node, variables);
                    repository.completeNode(nodeInstanceId, "{}");
                    int finished = repository.finishInstance(
                            instance.id(), INSTANCE_COMPLETED, writeJson(variables)
                    );
                    if (finished != 1) {
                        throw new BizException("流程实例状态已变化，请刷新后重试");
                    }
                    emitTerminalEvent(instance, INSTANCE_COMPLETED, variables);
                    return;
                }
            }
        }
        throw new BizException("流程节点跳转次数超过安全限制，可能存在死循环");
    }

    private void activateUserTask(WorkflowRepository.InstanceRow instance,
                                  WorkflowDefinition.Node node,
                                  Map<String, Object> variables) {
        Assignee assignee = resolveAssignee(node, variables);
        String nodeInstanceId = createNodeInstance(instance.id(), node, variables);
        String taskId = newId();
        repository.insertTask(new WorkflowRepository.TaskRow(
                taskId,
                instance.tenantId(),
                instance.id(),
                nodeInstanceId,
                node.getKey(),
                node.getName(),
                assignee.type(),
                assignee.value(),
                "PENDING",
                0,
                LocalDateTime.now(),
                null
        ));
        repository.updateInstancePosition(instance.id(), node.getKey(), writeJson(variables));
    }

    private String createNodeInstance(String instanceId,
                                      WorkflowDefinition.Node node,
                                      Map<String, Object> variables) {
        String nodeInstanceId = newId();
        repository.insertNodeInstance(new WorkflowRepository.NodeInstanceRow(
                nodeInstanceId,
                instanceId,
                node.getKey(),
                StringUtils.hasText(node.getName()) ? node.getName() : node.getKey(),
                node.getType().name(),
                "ACTIVE",
                node.getHandlerKey(),
                writeJson(variables),
                LocalDateTime.now()
        ));
        return nodeInstanceId;
    }

    private Assignee resolveAssignee(WorkflowDefinition.Node node,
                                     Map<String, Object> variables) {
        WorkflowDefinition.AssigneeRule rule = node.getAssigneeRule();
        if (rule == null || rule.getType() == null || !StringUtils.hasText(rule.getValue())) {
            throw new BizException("人工节点未配置审批人规则: " + node.getKey());
        }
        return switch (rule.getType()) {
            case USER -> new Assignee("USER", rule.getValue());
            case ROLE -> new Assignee("ROLE", rule.getValue());
            case VARIABLE -> {
                Object value = variables.get(rule.getValue());
                if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                    throw new BizException("审批人变量不存在: " + rule.getValue());
                }
                yield new Assignee("USER", String.valueOf(value));
            }
        };
    }

    private void validateOperator(WorkflowRepository.TaskRow task, String operatorId) {
        if ("USER".equals(task.assigneeType()) && !task.assigneeValue().equals(operatorId)) {
            throw new BizException("当前用户不是该任务的处理人");
        }
        // ROLE 类型由网关或权限服务校验角色成员关系；工作流服务保留最终任务并发校验。
    }

    private void emitTerminalEvent(WorkflowRepository.InstanceRow instance,
                                   String status,
                                   Map<String, Object> variables) {
        String eventId = newId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", "INSTANCE_" + status);
        payload.put("instanceId", instance.id());
        payload.put("tenantId", instance.tenantId());
        payload.put("sourceSystem", instance.sourceSystem());
        payload.put("businessType", instance.businessType());
        payload.put("businessId", instance.businessId());
        payload.put("callbackUrl", instance.callbackUrl());
        payload.put("variables", variables);
        payload.put("occurredAt", LocalDateTime.now().toString());
        repository.insertOutbox(newId(), eventId, instance.id(), "INSTANCE_" + status, writeJson(payload));
    }

    private void validateDefinition(WorkflowDefinition definition) {
        if (definition == null || definition.getNodes() == null || definition.getNodes().isEmpty()) {
            throw new BizException("流程定义至少需要一个节点");
        }
        if (definition.getTransitions() == null) {
            definition.setTransitions(new ArrayList<>());
        }

        Set<String> nodeKeys = new HashSet<>();
        long startCount = 0;
        long endCount = 0;
        for (WorkflowDefinition.Node node : definition.getNodes()) {
            if (node == null || !StringUtils.hasText(node.getKey()) || node.getType() == null) {
                throw new BizException("流程节点 key 和 type 不能为空");
            }
            if (!nodeKeys.add(node.getKey())) {
                throw new BizException("流程节点 key 重复: " + node.getKey());
            }
            if (node.getType() == WorkflowDefinition.NodeType.START) {
                startCount++;
            }
            if (node.getType() == WorkflowDefinition.NodeType.END) {
                endCount++;
            }
            if (node.getType() == WorkflowDefinition.NodeType.USER_TASK
                    && node.getAssigneeRule() == null) {
                throw new BizException("人工节点必须配置审批人规则: " + node.getKey());
            }
            if (node.getType() == WorkflowDefinition.NodeType.SERVICE_TASK
                    && !StringUtils.hasText(node.getHandlerKey())) {
                throw new BizException("服务节点必须配置 handlerKey: " + node.getKey());
            }
        }
        if (startCount != 1) {
            throw new BizException("流程定义必须且只能有一个 START 节点");
        }
        if (endCount < 1) {
            throw new BizException("流程定义至少需要一个 END 节点");
        }
        for (WorkflowDefinition.Transition transition : definition.getTransitions()) {
            if (transition == null
                    || !nodeKeys.contains(transition.getFrom())
                    || !nodeKeys.contains(transition.getTo())) {
                throw new BizException("流程连线引用了不存在的节点");
            }
        }
    }

    private WorkflowContracts.DefinitionResponse toDefinitionResponse(
            WorkflowRepository.DefinitionVersionRow row) {
        return new WorkflowContracts.DefinitionResponse(
                row.tenantId(), row.definitionKey(), row.definitionName(), row.version(),
                row.status(), readDefinition(row.definitionJson()), row.createdAt(), row.publishedAt()
        );
    }

    private WorkflowContracts.InstanceResponse toInstanceResponse(
            WorkflowRepository.InstanceRow row) {
        return new WorkflowContracts.InstanceResponse(
                row.id(), row.tenantId(), row.definitionKey(), row.definitionVersion(),
                row.sourceSystem(), row.businessType(), row.businessId(), row.initiatorId(),
                row.currentNodeKey(), row.status(), readMap(row.variablesJson()),
                row.startedAt(), row.endedAt()
        );
    }

    private WorkflowContracts.TaskResponse toTaskResponse(WorkflowRepository.TaskRow row) {
        return new WorkflowContracts.TaskResponse(
                row.id(), row.instanceId(), row.nodeInstanceId(), row.nodeKey(),
                row.taskName(), row.assigneeType(), row.assigneeValue(), row.status(),
                row.version(), row.createdAt(), row.completedAt()
        );
    }

    private WorkflowDefinition readDefinition(String json) {
        try {
            return objectMapper.readValue(json, WorkflowDefinition.class);
        } catch (JsonProcessingException ex) {
            throw new BizException("流程定义 JSON 无法解析: " + ex.getOriginalMessage());
        }
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BizException("流程变量 JSON 无法解析: " + ex.getOriginalMessage());
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException("工作流数据无法序列化: " + ex.getOriginalMessage());
        }
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record Assignee(String type, String value) {
    }
}
