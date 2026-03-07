package single.cjj.share.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
    private String id;
    private String title;
    private String description;
    private Priority priority;
    private String slaClass;
    private boolean overrideSlaClass;
    private UserSummary requester;
    private UserSummary assignee;
    private TaskStatus status;
    private LocalDate dueDate;
    private List<String> labels = new ArrayList<>();
    private String module;
    private List<String> acceptanceCriteria = new ArrayList<>();
    private String impactScope;
    private TicketSource ticketSource;
    private TaskType taskType;
    private Estimate estimate;
    private List<Subtask> subtasks = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private String milestone;
    private String quarter;
    private String impactScopeDetail;
    private String ownerTeam;
    private List<UserSummary> coOwners = new ArrayList<>();
    private EnvironmentType env;
    private String rollbackPlan;
    private List<Attachment> testEvidence = new ArrayList<>();
    private List<Attachment> attachments = new ArrayList<>();
    private List<String> relatedLinks = new ArrayList<>();
    private CommentThread comments;
    private List<Event> eventLog = new ArrayList<>();
    private Instant firstResponseAt;
    private Instant createdAt;
    private Instant completedAt;
    private RiskLevel riskLevel;
    private List<ChecklistItem> dorCheck = new ArrayList<>();
    private boolean rollbackPlanRequired;
    private CiStatus ciStatus = CiStatus.UNKNOWN;
    private JsonNode expectedValue;
    private String postmortem;
    private Integer csat;
    private String duplicateOf;
    private int reopenCount;
    private SecurityLevel securityLevel = SecurityLevel.INTERNAL;
    private boolean deleted;

    public Task() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getSlaClass() {
        return slaClass;
    }

    public void setSlaClass(String slaClass) {
        this.slaClass = slaClass;
    }

    public boolean isOverrideSlaClass() {
        return overrideSlaClass;
    }

    public void setOverrideSlaClass(boolean overrideSlaClass) {
        this.overrideSlaClass = overrideSlaClass;
    }

    public UserSummary getRequester() {
        return requester;
    }

    public void setRequester(UserSummary requester) {
        this.requester = requester;
    }

    public UserSummary getAssignee() {
        return assignee;
    }

    public void setAssignee(UserSummary assignee) {
        this.assignee = assignee;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
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

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
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

    public CommentThread getComments() {
        return comments;
    }

    public void setComments(CommentThread comments) {
        this.comments = comments;
    }

    public List<Event> getEventLog() {
        return eventLog;
    }

    public void setEventLog(List<Event> eventLog) {
        this.eventLog = eventLog;
    }

    public Instant getFirstResponseAt() {
        return firstResponseAt;
    }

    public void setFirstResponseAt(Instant firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<ChecklistItem> getDorCheck() {
        return dorCheck;
    }

    public void setDorCheck(List<ChecklistItem> dorCheck) {
        this.dorCheck = dorCheck;
    }

    @JsonProperty("rollbackPlanRequired")
    public boolean isRollbackPlanRequired() {
        return rollbackPlanRequired;
    }

    public void setRollbackPlanRequired(boolean rollbackPlanRequired) {
        this.rollbackPlanRequired = rollbackPlanRequired;
    }

    public CiStatus getCiStatus() {
        return ciStatus;
    }

    public void setCiStatus(CiStatus ciStatus) {
        this.ciStatus = ciStatus;
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

    public int getReopenCount() {
        return reopenCount;
    }

    public void setReopenCount(int reopenCount) {
        this.reopenCount = reopenCount;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("ageDays")
    public long getAgeDays() {
        if (createdAt == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(createdAt.atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
    }

    @JsonIgnore
    public boolean isCompleted() {
        return TaskStatus.COMPLETED.equals(status);
    }
}
