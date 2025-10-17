package single.cjj.share.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.share.model.Comment;
import single.cjj.share.model.CreateCommentRequest;
import single.cjj.share.model.CreateTaskRequest;
import single.cjj.share.model.ExportJob;
import single.cjj.share.model.ReportExportRequest;
import single.cjj.share.model.ReportSummary;
import single.cjj.share.model.SlaSummary;
import single.cjj.share.model.Task;
import single.cjj.share.model.TaskPage;
import single.cjj.share.model.TaskQuery;
import single.cjj.share.model.TaskTransitionRequest;
import single.cjj.share.model.UserSummary;
import single.cjj.share.service.ReferenceService;
import single.cjj.share.service.TaskService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shared-cloud")
public class TaskController {

    private final TaskService taskService;
    private final ReferenceService referenceService;
    public TaskController(TaskService taskService, ReferenceService referenceService) {
        this.taskService = taskService;
        this.referenceService = referenceService;
    }

    @GetMapping("/tasks")
    public Map<String, Object> listTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "labels", required = false) List<String> labels,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sort) {
        TaskQuery query = new TaskQuery();
        if (status != null) {
            query.setStatus(single.cjj.share.model.TaskStatus.fromValue(status));
        }
        query.setModule(module);
        if (priority != null) {
            query.setPriority(single.cjj.share.model.Priority.fromValue(priority));
        }
        query.setAssignee(assignee);
        query.setLabels(labels);
        query.setSearch(search);
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setSort(sort);
        TaskPage pageResult = taskService.listTasks(query);
        Map<String, Object> response = new HashMap<>();
        response.put("data", pageResult.getData());
        response.put("pagination", pageResult.getPagination());
        return response;
    }

    @PostMapping("/tasks")
    public Map<String, Object> createTask(@RequestBody @Valid CreateTaskRequest request) {
        Task task = taskService.createTask(request, currentUser());
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("createdAt", task.getCreatedAt());
        data.put("status", task.getStatus());
        data.put("slaClass", task.getSlaClass());
        data.put("firstResponseAt", task.getFirstResponseAt());
        response.put("data", data);
        return response;
    }

    @GetMapping("/tasks/{id}")
    public Map<String, Object> getTask(@PathVariable String id) {
        Task task = taskService.getTask(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", task);
        return response;
    }

    @PatchMapping("/tasks/{id}")
    public Map<String, Object> updateTask(@PathVariable String id, @RequestBody ObjectNode patch) {
        Task task = taskService.updateTask(id, patch, currentUser());
        Map<String, Object> response = new HashMap<>();
        response.put("data", task);
        return response;
    }

    @DeleteMapping("/tasks/{id}")
    public void deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
    }

    @PostMapping("/tasks/{id}/comments")
    public Map<String, Object> addComment(@PathVariable String id, @RequestBody @Valid CreateCommentRequest request) {
        Comment comment = taskService.addComment(id, request, currentUser());
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("createdAt", comment.getCreatedAt());
        response.put("data", data);
        return response;
    }

    @GetMapping("/tasks/{id}/comments")
    public Map<String, Object> listComments(@PathVariable String id,
                                            @RequestParam(value = "page", defaultValue = "1") int page,
                                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                                            @RequestParam(value = "sort", defaultValue = "createdAt") String sort) {
        boolean desc = "-createdAt".equals(sort);
        List<Comment> comments = taskService.listComments(id, page, pageSize, desc);
        Map<String, Object> response = new HashMap<>();
        response.put("data", comments);
        return response;
    }

    @PostMapping("/tasks/{id}/comments/{commentId}/ack")
    public void ackComment(@PathVariable String id, @PathVariable String commentId) {
        taskService.acknowledgeComment(id, commentId);
    }

    @PostMapping("/tasks/{id}/transitions")
    public Map<String, Object> transition(@PathVariable String id, @RequestBody @Valid TaskTransitionRequest request) {
        Task task = taskService.transition(id, request, currentUser());
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", task.getEventLog().isEmpty() ? null : task.getEventLog().get(task.getEventLog().size() - 1).getId());
        data.put("timestamp", task.getEventLog().isEmpty() ? null : task.getEventLog().get(task.getEventLog().size() - 1).getTimestamp());
        response.put("data", data);
        return response;
    }

    @GetMapping("/tasks/{id}/sla")
    public Map<String, Object> getSla(@PathVariable String id) {
        SlaSummary summary = taskService.getSla(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", summary);
        return response;
    }

    @GetMapping("/report/summary")
    public Map<String, Object> summary(@RequestParam(value = "from", required = false) String from,
                                       @RequestParam(value = "to", required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        ReportSummary summary = taskService.getSummary(fromDate, toDate);
        Map<String, Object> response = new HashMap<>();
        response.put("data", summary);
        return response;
    }

    @PostMapping("/report/export")
    public Map<String, Object> export(@RequestBody ReportExportRequest request) {
        ExportJob job = taskService.createExportJob(request);
        Map<String, Object> response = new HashMap<>();
        response.put("data", job);
        return response;
    }

    @GetMapping("/report/export/{jobId}")
    public Map<String, Object> exportStatus(@PathVariable String jobId) {
        ExportJob job = taskService.getExportJob(jobId);
        Map<String, Object> response = new HashMap<>();
        response.put("data", job);
        return response;
    }

    private UserSummary currentUser() {
        return referenceService.searchUsers(null).get(0);
    }
}
