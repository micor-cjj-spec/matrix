package single.cjj.workflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import single.cjj.workflow.api.WorkflowContracts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorkflowContracts.TaskResponse> listTasks(String instanceId) {
        return jdbcTemplate.query("""
                SELECT id, instance_id, node_instance_id, node_key, task_name,
                       assignee_type, assignee_value, status, version,
                       created_at, completed_at
                FROM wf_task
                WHERE instance_id = ?
                ORDER BY created_at, id
                """, this::mapTask, instanceId);
    }

    public List<WorkflowContracts.TimelineResponse> listTimeline(String instanceId) {
        return jdbcTemplate.query("""
                SELECT l.id action_id, l.instance_id, l.task_id,
                       COALESCE(t.node_key, n.node_key) node_key,
                       COALESCE(t.task_name, n.node_name) node_name,
                       l.action, l.operator_id, l.comment_text,
                       l.before_status, l.after_status, l.request_id, l.created_at
                FROM wf_action_log l
                LEFT JOIN wf_task t ON t.id = l.task_id
                LEFT JOIN wf_node_instance n ON n.id = t.node_instance_id
                WHERE l.instance_id = ?
                ORDER BY l.created_at, l.id
                """, this::mapTimeline, instanceId);
    }

    private WorkflowContracts.TaskResponse mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new WorkflowContracts.TaskResponse(
                rs.getString("id"), rs.getString("instance_id"),
                rs.getString("node_instance_id"), rs.getString("node_key"),
                rs.getString("task_name"), rs.getString("assignee_type"),
                rs.getString("assignee_value"), rs.getString("status"),
                rs.getInt("version"), rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class)
        );
    }

    private WorkflowContracts.TimelineResponse mapTimeline(ResultSet rs, int rowNum) throws SQLException {
        return new WorkflowContracts.TimelineResponse(
                rs.getString("action_id"), rs.getString("instance_id"),
                rs.getString("task_id"), rs.getString("node_key"),
                rs.getString("node_name"), rs.getString("action"),
                rs.getString("operator_id"), rs.getString("comment_text"),
                rs.getString("before_status"), rs.getString("after_status"),
                rs.getString("request_id"), rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
