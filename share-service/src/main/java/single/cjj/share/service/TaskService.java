package single.cjj.share.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.share.exception.ResourceNotFoundException;
import single.cjj.share.exception.ValidationException;
import single.cjj.share.model.Attachment;
import single.cjj.share.model.Comment;
import single.cjj.share.model.CommentThread;
import single.cjj.share.model.CreateCommentRequest;
import single.cjj.share.model.CreateTaskRequest;
import single.cjj.share.model.EnvironmentType;
import single.cjj.share.model.Event;
import single.cjj.share.model.EventType;
import single.cjj.share.model.ExportJob;
import single.cjj.share.model.ModuleOption;
import single.cjj.share.model.Pagination;
import single.cjj.share.model.Priority;
import single.cjj.share.model.ReportExportRequest;
import single.cjj.share.model.ReportSummary;
import single.cjj.share.model.SlaBreach;
import single.cjj.share.model.SlaSummary;
import single.cjj.share.model.Subtask;
import single.cjj.share.model.Task;
import single.cjj.share.model.TaskPage;
import single.cjj.share.model.TaskQuery;
import single.cjj.share.model.TaskStatus;
import single.cjj.share.model.TaskTransitionRequest;
import single.cjj.share.model.TaskType;
import single.cjj.share.model.TicketSource;
import single.cjj.share.model.UserSummary;
import single.cjj.share.validation.TaskValidator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final ReferenceService referenceService;
    private final ObjectMapper objectMapper;
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, ExportJob> exportJobs = new ConcurrentHashMap<>();
    private final AtomicLong commentSequence = new AtomicLong(1);
    private final AtomicLong eventSequence = new AtomicLong(1);
    private final AtomicLong threadSequence = new AtomicLong(1);

    private final UserSummary systemActor = new UserSummary("system", "系统", "system@matrix.local");

    public TaskService(ReferenceService referenceService, ObjectMapper objectMapper) {
        this.referenceService = referenceService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initData() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("支付-退款失败-重复扣款");
        request.setDescription("详细描述复现场景，包含输入、处理、输出等至少20个字符的内容");
        request.setPriority(Priority.P1);
        request.setTaskType(TaskType.DEFECT);
        request.setModule("payment");
        request.setStatus(TaskStatus.READY_FOR_ACCEPTANCE);
        request.setRiskLevel(single.cjj.share.model.RiskLevel.MEDIUM);
        request.setAcceptanceCriteria(List.of("输入 → 复现", "处理 → 观察退款触发", "输出 → 金额校验"));
        request.setImpactScope("≤1%");
        request.setLabels(List.of("支付", "退款"));
        request.setEstimate(new single.cjj.share.model.Estimate(12.0, single.cjj.share.model.EstimateUnit.HOURS));
        request.setDependencies(List.of("FIN-1234"));
        request.setOwnerTeam("共享运营-支付保障");
        request.setEnv(EnvironmentType.PRODUCTION);
        request.setRollbackPlan("若变更失败，回滚退款服务版本至v1.8");
        request.setDorCheck(List.of(new single.cjj.share.model.ChecklistItem("dor1", "场景描述清晰", true, null)));
        createTask(request, referenceService.searchUsers("李雷").get(0));
    }

    public Task createTask(CreateTaskRequest request, UserSummary requester) {
        Instant now = Instant.now();
        TaskValidator.validateCreateRequest(request, now);
        Task task = new Task();
        task.setId("task_" + UUID.randomUUID());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setTaskType(request.getTaskType());
        task.setModule(request.getModule());
        task.setStatus(request.getStatus());
        task.setRiskLevel(request.getRiskLevel());
        task.setRequester(requester);
        task.setAssignee(null);
        task.setCreatedAt(now);
        task.setSlaClass(resolveSlaClass(request.getPriority(), request.getOverrideSlaClass(), request.getSlaClass()));
        task.setOverrideSlaClass(Boolean.TRUE.equals(request.getOverrideSlaClass()));
        task.setDueDate(request.getDueDate());
        task.setLabels(copyList(request.getLabels()));
        task.setAcceptanceCriteria(copyList(request.getAcceptanceCriteria()));
        task.setImpactScope(request.getImpactScope());
        task.setTicketSource(request.getTicketSource());
        task.setEstimate(request.getEstimate());
        task.setSubtasks(copySubtasks(request.getSubtasks()));
        task.setDependencies(copyList(request.getDependencies()));
        task.setMilestone(request.getMilestone());
        task.setQuarter(request.getQuarter());
        task.setImpactScopeDetail(request.getImpactScopeDetail());
        task.setOwnerTeam(request.getOwnerTeam());
        task.setCoOwners(copyUsers(request.getCoOwners()));
        task.setEnv(request.getEnv());
        task.setRollbackPlan(request.getRollbackPlan());
        task.setTestEvidence(copyAttachments(request.getTestEvidence()));
        task.setAttachments(copyAttachments(request.getAttachments()));
        task.setRelatedLinks(copyList(request.getRelatedLinks()));
        task.setDorCheck(copyChecklist(request.getDorCheck()));
        task.setExpectedValue(request.getExpectedValue());
        task.setPostmortem(request.getPostmortem());
        task.setCsat(request.getCsat());
        task.setDuplicateOf(request.getDuplicateOf());
        task.setSecurityLevel(request.getSecurityLevel() == null ? single.cjj.share.model.SecurityLevel.INTERNAL : request.getSecurityLevel());
        task.setComments(new CommentThread("thread_" + threadSequence.getAndIncrement()));
        task.setEventLog(new ArrayList<>());
        task.setRollbackPlanRequired(isRollbackPlanRequired(task));
        tasks.put(task.getId(), task);
        recordEvent(task, EventType.STATUS_CHANGED, null, task.getStatus().getValue(), now, systemActor);
        return task;
    }

    private String resolveSlaClass(Priority priority, Boolean override, String overrideValue) {
        if (Boolean.TRUE.equals(override) && StringUtils.hasText(overrideValue)) {
            return overrideValue;
        }
        return priority != null ? priority.getDefaultSla() : null;
    }

    private List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private List<Attachment> copyAttachments(List<Attachment> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private List<Subtask> copySubtasks(List<Subtask> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private List<single.cjj.share.model.ChecklistItem> copyChecklist(List<single.cjj.share.model.ChecklistItem> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private List<UserSummary> copyUsers(List<UserSummary> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    public TaskPage listTasks(TaskQuery query) {
        List<Task> filtered = tasks.values().stream()
                .filter(task -> !task.isDeleted())
                .filter(task -> query.getStatus() == null || task.getStatus() == query.getStatus())
                .filter(task -> query.getModule() == null || Objects.equals(task.getModule(), query.getModule()))
                .filter(task -> query.getPriority() == null || task.getPriority() == query.getPriority())
                .filter(task -> query.getAssignee() == null || (task.getAssignee() != null && Objects.equals(task.getAssignee().getId(), query.getAssignee())))
                .filter(task -> query.getLabels() == null || task.getLabels().containsAll(query.getLabels()))
                .filter(task -> {
                    if (!StringUtils.hasText(query.getSearch())) {
                        return true;
                    }
                    String lower = query.getSearch().toLowerCase(Locale.ROOT);
                    return (task.getTitle() != null && task.getTitle().toLowerCase(Locale.ROOT).contains(lower))
                            || (task.getDescription() != null && task.getDescription().toLowerCase(Locale.ROOT).contains(lower));
                })
                .sorted(resolveComparator(query.getSort()))
                .collect(Collectors.toList());

        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.max(Math.min(query.getPageSize(), 100), 1);
        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Task> data = filtered.subList(fromIndex, toIndex);
        Pagination pagination = new Pagination(page, pageSize, filtered.size());
        return new TaskPage(new ArrayList<>(data), pagination);
    }

    private Comparator<Task> resolveComparator(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Comparator.comparing(Task::getCreatedAt).reversed();
        }
        boolean desc = sort.startsWith("-");
        String field = desc ? sort.substring(1) : sort;
        Comparator<Task> comparator;
        switch (field) {
            case "createdAt" -> comparator = Comparator.comparing(Task::getCreatedAt);
            case "dueDate" -> comparator = Comparator.comparing(task -> task.getDueDate() == null ? LocalDate.MAX : task.getDueDate());
            case "priority" -> comparator = Comparator.comparing(task -> task.getPriority() == null ? 99 : task.getPriority().ordinal());
            default -> comparator = Comparator.comparing(Task::getCreatedAt);
        }
        if (desc) {
            return comparator.reversed();
        }
        return comparator;
    }

    public Task getTask(String id) {
        Task task = tasks.get(id);
        if (task == null || task.isDeleted()) {
            throw new ResourceNotFoundException("未找到任务: " + id);
        }
        return task;
    }

    public Task updateTask(String id, ObjectNode patch, UserSummary actor) {
        Task task = getTask(id);
        synchronized (task) {
            TaskStatus beforeStatus = task.getStatus();
            UserSummary beforeAssignee = task.getAssignee();
            List<String> beforeLabels = new ArrayList<>(task.getLabels());
            applyPatch(task, patch);
            task.setRollbackPlanRequired(isRollbackPlanRequired(task));
            TaskValidator.validateUpdate(task);
            Instant now = Instant.now();
            if (task.getStatus() != beforeStatus) {
                handleStatusChange(task, beforeStatus, patch.path("statusTransitionNote").asText(null), now, actor);
            }
            if (!Objects.equals(beforeAssignee, task.getAssignee())) {
                recordEvent(task, EventType.ASSIGNEE_CHANGED,
                        beforeAssignee == null ? null : beforeAssignee.getId(),
                        task.getAssignee() == null ? null : task.getAssignee().getId(), now, actor);
            }
            if (!Objects.equals(beforeLabels, task.getLabels())) {
                recordEvent(task, EventType.LABELS_UPDATED, beforeLabels, task.getLabels(), now, actor);
            }
            return task;
        }
    }

    private void applyPatch(Task task, ObjectNode patch) {
        if (patch.has("title")) {
            JsonNode titleNode = patch.get("title");
            if (titleNode.isNull() || !StringUtils.hasText(titleNode.asText())) {
                throw new ValidationException("标题不能为空").addError("title", "标题不能为空");
            }
            if (titleNode.asText().length() > 120) {
                throw new ValidationException("标题过长").addError("title", "标题不能超过120字符");
            }
            task.setTitle(titleNode.asText());
        }
        if (patch.has("description")) {
            JsonNode descriptionNode = patch.get("description");
            if (descriptionNode.isNull() || descriptionNode.asText().trim().length() < 20) {
                throw new ValidationException("描述不合法").addError("description", "描述至少20个字符");
            }
            task.setDescription(descriptionNode.asText());
        }
        if (patch.has("priority")) {
            task.setPriority(Priority.fromValue(patch.get("priority").asText()));
            task.setSlaClass(resolveSlaClass(task.getPriority(), task.isOverrideSlaClass(), task.getSlaClass()));
        }
        if (patch.has("slaClass")) {
            task.setSlaClass(patch.get("slaClass").asText());
            task.setOverrideSlaClass(true);
        }
        if (patch.has("overrideSlaClass")) {
            task.setOverrideSlaClass(patch.get("overrideSlaClass").asBoolean());
            task.setSlaClass(resolveSlaClass(task.getPriority(), task.isOverrideSlaClass(), patch.has("slaClass") ? patch.get("slaClass").asText() : task.getSlaClass()));
        }
        if (patch.has("dueDate")) {
            task.setDueDate(patch.get("dueDate").isNull() ? null : LocalDate.parse(patch.get("dueDate").asText()));
        }
        if (patch.has("labels")) {
            task.setLabels(objectMapper.convertValue(patch.get("labels"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        if (patch.has("acceptanceCriteria")) {
            task.setAcceptanceCriteria(objectMapper.convertValue(patch.get("acceptanceCriteria"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        if (patch.has("impactScope")) {
            task.setImpactScope(patch.get("impactScope").isNull() ? null : patch.get("impactScope").asText());
        }
        if (patch.has("ticketSource")) {
            task.setTicketSource(patch.get("ticketSource").isNull() ? null : objectMapper.convertValue(patch.get("ticketSource"), TicketSource.class));
        }
        if (patch.has("estimate")) {
            task.setEstimate(patch.get("estimate").isNull() ? null : objectMapper.convertValue(patch.get("estimate"), single.cjj.share.model.Estimate.class));
        }
        if (patch.has("subtasks")) {
            task.setSubtasks(patch.get("subtasks").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("subtasks"), objectMapper.getTypeFactory().constructCollectionType(List.class, Subtask.class)));
        }
        if (patch.has("dependencies")) {
            task.setDependencies(patch.get("dependencies").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("dependencies"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        if (patch.has("milestone")) {
            task.setMilestone(patch.get("milestone").isNull() ? null : patch.get("milestone").asText());
        }
        if (patch.has("quarter")) {
            task.setQuarter(patch.get("quarter").isNull() ? null : patch.get("quarter").asText());
        }
        if (patch.has("impactScopeDetail")) {
            task.setImpactScopeDetail(patch.get("impactScopeDetail").isNull() ? null : patch.get("impactScopeDetail").asText());
        }
        if (patch.has("ownerTeam")) {
            task.setOwnerTeam(patch.get("ownerTeam").isNull() ? null : patch.get("ownerTeam").asText());
        }
        if (patch.has("coOwners")) {
            task.setCoOwners(patch.get("coOwners").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("coOwners"), objectMapper.getTypeFactory().constructCollectionType(List.class, UserSummary.class)));
        }
        if (patch.has("env")) {
            task.setEnv(patch.get("env").isNull() ? null : EnvironmentType.fromValue(patch.get("env").asText()));
        }
        if (patch.has("rollbackPlan")) {
            task.setRollbackPlan(patch.get("rollbackPlan").isNull() ? null : patch.get("rollbackPlan").asText());
        }
        if (patch.has("testEvidence")) {
            task.setTestEvidence(patch.get("testEvidence").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("testEvidence"), objectMapper.getTypeFactory().constructCollectionType(List.class, Attachment.class)));
        }
        if (patch.has("attachments")) {
            task.setAttachments(patch.get("attachments").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("attachments"), objectMapper.getTypeFactory().constructCollectionType(List.class, Attachment.class)));
        }
        if (patch.has("relatedLinks")) {
            task.setRelatedLinks(patch.get("relatedLinks").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("relatedLinks"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        if (patch.has("dorCheck")) {
            task.setDorCheck(patch.get("dorCheck").isNull() ? new ArrayList<>() : objectMapper.convertValue(patch.get("dorCheck"), objectMapper.getTypeFactory().constructCollectionType(List.class, single.cjj.share.model.ChecklistItem.class)));
        }
        if (patch.has("expectedValue")) {
            task.setExpectedValue(patch.get("expectedValue"));
        }
        if (patch.has("postmortem")) {
            task.setPostmortem(patch.get("postmortem").isNull() ? null : patch.get("postmortem").asText());
        }
        if (patch.has("csat")) {
            task.setCsat(patch.get("csat").isNull() ? null : patch.get("csat").asInt());
        }
        if (patch.has("duplicateOf")) {
            String duplicateOf = patch.get("duplicateOf").isNull() ? null : patch.get("duplicateOf").asText();
            task.setDuplicateOf(duplicateOf);
            if (duplicateOf != null) {
                TaskStatus prev = task.getStatus();
                task.setStatus(TaskStatus.COMPLETED);
                Instant now = Instant.now();
                handleStatusChange(task, prev, patch.path("statusTransitionNote").asText("标记重复"), now, systemActor);
                recordEvent(task, EventType.DUPLICATE_MARKED, null, duplicateOf, now, systemActor);
            }
        }
        if (patch.has("securityLevel")) {
            task.setSecurityLevel(patch.get("securityLevel").isNull() ? null : single.cjj.share.model.SecurityLevel.fromValue(patch.get("securityLevel").asText()));
        }
        if (patch.has("assignee")) {
            JsonNode assigneeNode = patch.get("assignee");
            if (assigneeNode.isNull()) {
                task.setAssignee(null);
            } else {
                task.setAssignee(objectMapper.convertValue(assigneeNode, UserSummary.class));
            }
        }
        if (patch.has("status")) {
            TaskStatus newStatus = TaskStatus.fromValue(patch.get("status").asText());
            task.setStatus(newStatus);
        }
    }

    private void handleStatusChange(Task task, TaskStatus before, String note, Instant when, UserSummary actor) {
        if (!StringUtils.hasText(note)) {
            throw new ValidationException("状态变更备注必填").addError("statusTransitionNote", "状态变更时需要备注");
        }
        if (before == TaskStatus.TRIAGE && task.getStatus() != TaskStatus.TRIAGE && task.getFirstResponseAt() == null) {
            task.setFirstResponseAt(when);
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(when);
        } else if (before == TaskStatus.COMPLETED && task.getStatus() != TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
            task.setReopenCount(task.getReopenCount() + 1);
        }
        recordEvent(task, EventType.STATUS_CHANGED, before == null ? null : before.getValue(), task.getStatus().getValue(), when, actor);
    }

    private void recordEvent(Task task, EventType type, Object from, Object to, Instant timestamp, UserSummary actor) {
        Event event = new Event("evt_" + eventSequence.getAndIncrement(), type, from, to, timestamp, actor);
        task.getEventLog().add(event);
    }

    private boolean isRollbackPlanRequired(Task task) {
        TaskType type = task.getTaskType();
        EnvironmentType env = task.getEnv();
        if (type == null) {
            return false;
        }
        boolean changeType = type == TaskType.REQUIREMENT || type == TaskType.AUTOMATION;
        if (!changeType || env == null) {
            return false;
        }
        return env == EnvironmentType.PRODUCTION || env == EnvironmentType.PRE_RELEASE;
    }

    public Comment addComment(String taskId, CreateCommentRequest request, UserSummary actor) {
        Task task = getTask(taskId);
        ValidationException ex = new ValidationException("评论不合法");
        TaskValidator.ensureCommentAttachmentsValid(request.getAttachments(), ex);
        if (!ex.getErrors().isEmpty()) {
            throw ex;
        }
        Comment comment = new Comment();
        comment.setId("c_" + commentSequence.getAndIncrement());
        comment.setBody(request.getBody());
        comment.setAuthor(actor);
        comment.setCreatedAt(Instant.now());
        if (request.getMentions() != null && !request.getMentions().isEmpty()) {
            List<UserSummary> mentions = request.getMentions().stream()
                    .map(this::findUserById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            comment.setMentions(mentions);
        }
        comment.setAttachments(copyAttachments(request.getAttachments()));
        synchronized (task) {
            if (task.getComments() == null) {
                task.setComments(new CommentThread("thread_" + threadSequence.getAndIncrement()));
            }
            task.getComments().addComment(comment);
            recordEvent(task, EventType.COMMENT_ADDED, null, comment.getId(), Instant.now(), actor);
        }
        return comment;
    }

    private UserSummary findUserById(String id) {
        return referenceService.searchUsers("").stream()
                .filter(user -> Objects.equals(user.getId(), id))
                .findFirst()
                .orElse(null);
    }

    public List<Comment> listComments(String taskId, int page, int pageSize, boolean desc) {
        Task task = getTask(taskId);
        if (task.getComments() == null) {
            return List.of();
        }
        List<Comment> items = new ArrayList<>(task.getComments().getItems());
        items.sort(Comparator.comparing(Comment::getCreatedAt));
        if (desc) {
            items.sort(Comparator.comparing(Comment::getCreatedAt).reversed());
        }
        int from = Math.min(Math.max(page - 1, 0) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return items.subList(from, to);
    }

    public void acknowledgeComment(String taskId, String commentId) {
        getTask(taskId); // ensure exists
        // no-op for in-memory implementation
    }

    public Task transition(String id, TaskTransitionRequest request, UserSummary actor) {
        Task task = getTask(id);
        synchronized (task) {
            TaskStatus before = task.getStatus();
            task.setStatus(request.getTargetStatus());
            handleStatusChange(task, before, request.getNote(), request.getEffectiveAt() == null ? Instant.now() : request.getEffectiveAt(), actor);
            return task;
        }
    }

    public SlaSummary getSla(String id) {
        Task task = getTask(id);
        double target = resolveTargetHours(task.getPriority());
        double elapsed = Duration.between(task.getCreatedAt(), Instant.now()).toHours() + (Duration.between(task.getCreatedAt(), Instant.now()).toMinutesPart() / 60.0);
        List<SlaBreach> breaches = new ArrayList<>();
        if (target > 0 && elapsed > target) {
            breaches.add(new SlaBreach(task.getStatus().getValue(), task.getCreatedAt().plus(Duration.ofHours((long) target)), elapsed - target));
        }
        return new SlaSummary(task.getStatus().getValue(), target, elapsed, breaches);
    }

    private double resolveTargetHours(Priority priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority) {
            case P0 -> 8;
            case P1 -> 24;
            case P2 -> 72;
            case P3 -> 120;
        };
    }

    public ReportSummary getSummary(LocalDate from, LocalDate to) {
        List<Task> list = tasks.values().stream()
                .filter(task -> !task.isDeleted())
                .filter(task -> {
                    LocalDate created = LocalDate.ofInstant(task.getCreatedAt(), ZoneId.systemDefault());
                    if (from != null && created.isBefore(from)) {
                        return false;
                    }
                    if (to != null && created.isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        ReportSummary summary = new ReportSummary();
        summary.setTotalCreated(list.size());
        summary.setCompleted((int) list.stream().filter(Task::isCompleted).count());
        summary.setOverdue((int) list.stream().filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())).count());
        double avgAge = list.stream().mapToLong(Task::getAgeDays).average().orElse(0);
        summary.setAvgAgeDays(Math.round(avgAge * 10.0) / 10.0);
        Map<String, ReportSummary.PriorityReport> priorityStats = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            List<Task> byPriority = list.stream().filter(task -> task.getPriority() == priority).collect(Collectors.toList());
            if (byPriority.isEmpty()) {
                continue;
            }
            double avgResponse = byPriority.stream()
                    .filter(task -> task.getFirstResponseAt() != null)
                    .mapToDouble(task -> Duration.between(task.getCreatedAt(), task.getFirstResponseAt()).toHours())
                    .average()
                    .orElse(0);
            priorityStats.put(priority.getValue(), new ReportSummary.PriorityReport(byPriority.size(), Math.round(avgResponse * 10.0) / 10.0));
        }
        summary.setByPriority(priorityStats);
        Map<String, Long> byModule = list.stream().collect(Collectors.groupingBy(Task::getModule, Collectors.counting()));
        List<ReportSummary.ModuleReport> modules = byModule.entrySet().stream()
                .map(entry -> new ReportSummary.ModuleReport(resolveModuleName(entry.getKey()), entry.getValue().intValue()))
                .collect(Collectors.toList());
        summary.setByModule(modules);
        return summary;
    }

    private String resolveModuleName(String moduleId) {
        return referenceService.getModules(null).stream()
                .filter(option -> Objects.equals(option.getId(), moduleId))
                .map(ModuleOption::getName)
                .findFirst()
                .orElse(moduleId);
    }

    public ExportJob createExportJob(ReportExportRequest request) {
        String jobId = "job_" + UUID.randomUUID();
        ExportJob job = new ExportJob(jobId, null, null, Instant.now());
        exportJobs.put(jobId, job);
        return job;
    }

    public ExportJob getExportJob(String jobId) {
        ExportJob job = exportJobs.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("未找到导出任务: " + jobId);
        }
        if (job.getDownloadUrl() == null) {
            job.setDownloadUrl("https://downloads.example.com/export/" + jobId + ".xlsx");
            job.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        }
        return job;
    }

    public void deleteTask(String id) {
        Task task = getTask(id);
        task.setDeleted(true);
    }

    public boolean removeAttachmentReferences(String attachmentId) {
        boolean removed = false;
        for (Task task : tasks.values()) {
            if (task.getAttachments() != null) {
                removed |= task.getAttachments().removeIf(att -> Objects.equals(att.getId(), attachmentId));
            }
            if (task.getTestEvidence() != null) {
                removed |= task.getTestEvidence().removeIf(att -> Objects.equals(att.getId(), attachmentId));
            }
            if (task.getComments() != null && task.getComments().getItems() != null) {
                task.getComments().getItems().forEach(comment -> {
                    if (comment.getAttachments() != null) {
                        comment.getAttachments().removeIf(att -> Objects.equals(att.getId(), attachmentId));
                    }
                });
            }
        }
        return removed;
    }
}
