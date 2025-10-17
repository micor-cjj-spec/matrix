package single.cjj.share.validation;

import com.fasterxml.jackson.databind.JsonNode;
import single.cjj.share.exception.ValidationException;
import single.cjj.share.model.Attachment;
import single.cjj.share.model.ChecklistItem;
import single.cjj.share.model.CreateTaskRequest;
import single.cjj.share.model.EnvironmentType;
import single.cjj.share.model.Estimate;
import single.cjj.share.model.Task;
import single.cjj.share.model.TaskStatus;
import single.cjj.share.model.TaskType;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TaskValidator {

    private TaskValidator() {
    }

    public static void validateCreateRequest(CreateTaskRequest request, Instant now) {
        ValidationException ex = new ValidationException("任务创建参数不合法");

        if (request.getDescription() == null || request.getDescription().trim().length() < 20) {
            ex.addError("description", "描述至少需要20个字符");
        }

        validateLabels(request.getLabels(), ex);
        validateAcceptanceCriteria(request.getAcceptanceCriteria(), ex);
        validateEstimate(request.getEstimate(), ex);
        validateSubtasks(request.getSubtasks(), ex);
        validateDorCheck(request.getDorCheck(), ex);
        validateRelatedLinks(request.getRelatedLinks(), ex);
        validateRollbackPlan(request.getTaskType(), request.getEnv(), request.getRollbackPlan(), ex);
        validateOwnerTeam(request.getTaskType(), request.getOwnerTeam(), ex);
        validateEnvironmentRequirement(request.getTaskType(), request.getEnv(), ex);
        validateDueDate(request.getDueDate(), now, ex);

        if (!ex.getErrors().isEmpty()) {
            throw ex;
        }
    }

    public static void validateUpdate(Task task) {
        ValidationException ex = new ValidationException("任务更新参数不合法");
        validateLabels(task.getLabels(), ex);
        validateAcceptanceCriteria(task.getAcceptanceCriteria(), ex);
        validateEstimate(task.getEstimate(), ex);
        validateSubtasks(task.getSubtasks(), ex);
        validateDorCheck(task.getDorCheck(), ex);
        validateRelatedLinks(task.getRelatedLinks(), ex);
        validateRollbackPlan(task.getTaskType(), task.getEnv(), task.getRollbackPlan(), ex);
        validateOwnerTeam(task.getTaskType(), task.getOwnerTeam(), ex);
        if (!ex.getErrors().isEmpty()) {
            throw ex;
        }
    }

    private static void validateLabels(List<String> labels, ValidationException ex) {
        if (labels == null) {
            return;
        }
        if (labels.size() > 3) {
            ex.addError("labels", "最多3个标签");
            return;
        }
        Set<String> set = new HashSet<>(labels);
        if (set.size() != labels.size()) {
            ex.addError("labels", "标签不能重复");
        }
    }

    private static void validateAcceptanceCriteria(List<String> criteria, ValidationException ex) {
        if (criteria == null || criteria.isEmpty()) {
            ex.addError("acceptanceCriteria", "请至少提供一条验收标准");
        }
    }

    private static void validateEstimate(Estimate estimate, ValidationException ex) {
        if (estimate == null) {
            return;
        }
        if (estimate.getValue() == null || estimate.getValue() < 0) {
            ex.addError("estimate.value", "预估值必须大于等于0");
        }
    }

    private static void validateSubtasks(List<?> subtasks, ValidationException ex) {
        if (subtasks == null) {
            return;
        }
        if (subtasks.isEmpty()) {
            ex.addError("subtasks", "子任务至少需要一项");
        }
    }

    private static void validateDorCheck(List<ChecklistItem> items, ValidationException ex) {
        if (items == null || items.isEmpty()) {
            ex.addError("dorCheck", "请维护至少一条就绪检查项");
        }
    }

    private static void validateRelatedLinks(List<String> links, ValidationException ex) {
        if (links == null) {
            return;
        }
        for (int i = 0; i < links.size(); i++) {
            String link = links.get(i);
            if (link == null || link.isBlank()) {
                ex.addError("relatedLinks[" + i + "]", "链接不能为空");
                continue;
            }
            try {
                URI uri = new URI(link);
                if (uri.getScheme() == null || uri.getHost() == null) {
                    ex.addError("relatedLinks[" + i + "]", "链接格式不合法");
                }
            } catch (URISyntaxException e) {
                ex.addError("relatedLinks[" + i + "]", "链接格式不合法");
            }
        }
    }

    private static void validateRollbackPlan(TaskType taskType, EnvironmentType env, String rollbackPlan, ValidationException ex) {
        if (taskType == null) {
            return;
        }
        boolean require = isChangeType(taskType) && env != null && (env == EnvironmentType.PRODUCTION || env == EnvironmentType.PRE_RELEASE);
        if (require && (rollbackPlan == null || rollbackPlan.isBlank())) {
            ex.addError("rollbackPlan", "生产或预发变更类任务必须提供回滚方案");
        }
    }

    private static void validateOwnerTeam(TaskType taskType, String ownerTeam, ValidationException ex) {
        if (taskType != null && taskType != TaskType.DEFECT && (ownerTeam == null || ownerTeam.isBlank())) {
            ex.addError("ownerTeam", "非缺陷类任务必须指定负责团队");
        }
    }

    private static void validateEnvironmentRequirement(TaskType taskType, EnvironmentType env, ValidationException ex) {
        if (taskType != null && isChangeType(taskType) && env == null) {
            ex.addError("env", "变更类任务必须指定环境");
        }
    }

    private static boolean isChangeType(TaskType taskType) {
        return taskType == TaskType.REQUIREMENT || taskType == TaskType.AUTOMATION;
    }

    private static void validateDueDate(LocalDate dueDate, Instant now, ValidationException ex) {
        if (dueDate == null) {
            return;
        }
        LocalDate today = LocalDate.ofInstant(now, java.time.ZoneId.systemDefault());
        if (dueDate.isBefore(today)) {
            ex.addError("dueDate", "截止日期不能早于创建日期");
        }
    }

    public static void ensureCommentAttachmentsValid(List<Attachment> attachments, ValidationException ex) {
        if (attachments == null) {
            return;
        }
        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);
            if (attachment.getId() == null || attachment.getId().isBlank()) {
                ex.addError("attachments[" + i + "]", "附件ID不能为空");
            }
            if (attachment.getType() == null) {
                ex.addError("attachments[" + i + "]", "附件类型不能为空");
            }
        }
    }

    public static void ensureStatusTransitionValid(Task task, TaskStatus target, ValidationException ex) {
        if (task.getStatus() == TaskStatus.COMPLETED && target == TaskStatus.COMPLETED) {
            ex.addError("status", "任务已完成");
        }
        if (!ex.getErrors().isEmpty()) {
            throw ex;
        }
    }

    public static void ensurePatchFieldLength(JsonNode node, String field, int min, ValidationException ex) {
        if (node == null || !node.has(field)) {
            return;
        }
        JsonNode value = node.get(field);
        if (value.isTextual() && value.asText().trim().length() < min) {
            ex.addError(field, "字段长度至少" + min + "个字符");
        }
    }
}
