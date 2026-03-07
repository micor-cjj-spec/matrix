package single.cjj.share.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class CreateTaskRequest {
    @NotBlank(message = "标题必填")
    @Size(max = 120, message = "标题不能超过120字符")
    private String title;

    @NotBlank(message = "描述必填")
    private String description;

    @NotNull(message = "优先级必填")
    private Priority priority;

    @NotNull(message = "任务类型必填")
    private TaskType taskType;

    @NotBlank(message = "模块必填")
    private String module;

    @NotNull(message = "状态必填")
    private TaskStatus status;

    @NotNull(message = "风险等级必填")
    private RiskLevel riskLevel;

    private Boolean overrideSlaClass;
    private String slaClass;
    private LocalDate dueDate;
    private List<String> labels;

    @NotEmpty(message = "至少一条验收标准")
    private List<String> acceptanceCriteria;

    private String impactScope;
    private TicketSource ticketSource;
    private Estimate estimate;
    private List<Subtask> subtasks;
    private List<String> dependencies;
    private String milestone;
    private String quarter;
    private String impactScopeDetail;
    private String ownerTeam;
    private List<UserSummary> coOwners;
    private EnvironmentType env;
    private String rollbackPlan;
    private List<Attachment> testEvidence;
    private List<Attachment> attachments;
    private List<String> relatedLinks;
    private List<ChecklistItem> dorCheck;
    private JsonNode expectedValue;
    private String postmortem;
    private Integer csat;
    private String duplicateOf;
    private SecurityLevel securityLevel;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getOverrideSlaClass() {
        return overrideSlaClass;
    }

    public void setOverrideSlaClass(Boolean overrideSlaClass) {
        this.overrideSlaClass = overrideSlaClass;
    }

    public String getSlaClass() {
        return slaClass;
    }

    public void setSlaClass(String slaClass) {
        this.slaClass = slaClass;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(List<String> acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public String getImpactScope() {
        return impactScope;
    }

    public void setImpactScope(String impactScope) {
        this.impactScope = impactScope;
    }

    public TicketSource getTicketSource() {
        return ticketSource;
    }

    public void setTicketSource(TicketSource ticketSource) {
        this.ticketSource = ticketSource;
    }

    public Estimate getEstimate() {
        return estimate;
    }

    public void setEstimate(Estimate estimate) {
        this.estimate = estimate;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getQuarter() {
        return quarter;
    }

    public void setQuarter(String quarter) {
        this.quarter = quarter;
    }

    public String getImpactScopeDetail() {
        return impactScopeDetail;
    }

    public void setImpactScopeDetail(String impactScopeDetail) {
        this.impactScopeDetail = impactScopeDetail;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public List<UserSummary> getCoOwners() {
        return coOwners;
    }

    public void setCoOwners(List<UserSummary> coOwners) {
        this.coOwners = coOwners;
    }

    public EnvironmentType getEnv() {
        return env;
    }

    public void setEnv(EnvironmentType env) {
        this.env = env;
    }

    public String getRollbackPlan() {
        return rollbackPlan;
    }

    public void setRollbackPlan(String rollbackPlan) {
        this.rollbackPlan = rollbackPlan;
    }

    public List<Attachment> getTestEvidence() {
        return testEvidence;
    }

    public void setTestEvidence(List<Attachment> testEvidence) {
        this.testEvidence = testEvidence;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<String> getRelatedLinks() {
        return relatedLinks;
    }

    public void setRelatedLinks(List<String> relatedLinks) {
        this.relatedLinks = relatedLinks;
    }

    public List<ChecklistItem> getDorCheck() {
        return dorCheck;
    }

    public void setDorCheck(List<ChecklistItem> dorCheck) {
        this.dorCheck = dorCheck;
    }

    public JsonNode getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(JsonNode expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getPostmortem() {
        return postmortem;
    }

    public void setPostmortem(String postmortem) {
        this.postmortem = postmortem;
    }

    public Integer getCsat() {
        return csat;
    }

    public void setCsat(Integer csat) {
        this.csat = csat;
    }

    public String getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(String duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

}
