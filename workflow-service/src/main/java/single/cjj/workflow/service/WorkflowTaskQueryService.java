package single.cjj.workflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import single.cjj.workflow.api.WorkflowContracts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowTaskQueryService {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowTaskQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorkflowContracts.TaskResponse> listTasks(String tenantId,
                                                          String assigneeType,
                                                          String assigneeValue,
                                                          String status,
                                                          int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jdbcTemplate.query("""
                SELECT id, instance_id, node_instance_id, node_key, task_name,
                       assignee_type, assignee_value, status, version,
                       created_at, completed_at
                FROM wf_task
                WHERE tenant_id = ? AND assignee_type = ? AND assignee_value = ?
                  AND status = ?
                ORDER BY created_at
                LIMIT ?
                """, this::mapTask, tenantId, assigneeType, assigneeValue, status, safeLimit);
    }

    private WorkflowContracts.TaskResponse mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new WorkflowContracts.TaskResponse(
                rs.getString("id"),
                rs.getString("instance_id"),
                rs.getString("node_instance_id"),
                rs.getString("node_key"),
                rs.getString("task_name"),
                rs.getString("assignee_type"),
                rs.getString("assignee_value"),
                rs.getString("status"),
                rs.getInt("version"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class)
        );
    }
}
