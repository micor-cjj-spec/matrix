package single.cjj.workflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.workflow.api.WorkflowContracts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkflowTaskCenterService {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowTaskCenterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WorkflowContracts.TaskCenterPage query(String tenantId,
                                                   String userId,
                                                   Set<String> roleCodes,
                                                   WorkflowContracts.TaskCenterView view,
                                                   String businessType,
                                                   String keyword,
                                                   int page,
                                                   int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        QueryPlan plan = buildPlan(
                tenantId, userId,
                roleCodes == null ? Set.of() : new LinkedHashSet<>(roleCodes),
                view == null ? WorkflowContracts.TaskCenterView.TODO : view,
                businessType, keyword);

        Long totalValue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (" + plan.sql() + ") task_center_count",
                Long.class,
                plan.arguments().toArray());
        long total = totalValue == null ? 0L : totalValue;

        List<Object> pageArguments = new ArrayList<>(plan.arguments());
        pageArguments.add(safeSize);
        pageArguments.add((safePage - 1) * safeSize);
        List<WorkflowContracts.TaskCenterItem> items = jdbcTemplate.query(
                plan.sql() + " ORDER BY item_created_at DESC LIMIT ? OFFSET ?",
                this::mapItem,
                pageArguments.toArray());
        return new WorkflowContracts.TaskCenterPage(items, total, safePage, safeSize);
    }

    private QueryPlan buildPlan(String tenantId,
                                String userId,
                                Set<String> roles,
                                WorkflowContracts.TaskCenterView view,
                                String businessType,
                                String keyword) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();
        switch (view) {
            case TODO -> appendTodo(sql, args, tenantId, userId, roles);
            case DONE -> appendDone(sql, args, tenantId, userId);
            case INITIATED -> appendInitiated(sql, args, tenantId, userId);
        }
        if (StringUtils.hasText(businessType)) {
            sql.append(" AND i.business_type = ?");
            args.add(businessType.trim());
        }
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (i.business_id LIKE ? OR i.definition_key LIKE ? OR COALESCE(t.task_name, '') LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        return new QueryPlan(sql.toString(), args);
    }

    private void appendTodo(StringBuilder sql,
                            List<Object> args,
                            String tenantId,
                            String userId,
                            Set<String> roles) {
        sql.append(baseSelect("NULL", "t.created_at"));
        sql.append(" FROM wf_task t JOIN wf_instance i ON i.id = t.instance_id")
                .append(" WHERE t.tenant_id = ? AND t.status IN ('PENDING', 'CLAIMED') AND (")
                .append("(t.assignee_type = 'USER' AND t.assignee_value = ?)")
                .append(" OR EXISTS (SELECT 1 FROM wf_task_candidate c")
                .append(" WHERE c.task_id = t.id AND c.candidate_type = 'USER' AND c.candidate_value = ?)");
        args.add(tenantId);
        args.add(userId);
        args.add(userId);
        if (!roles.isEmpty()) {
            String placeholders = placeholders(roles.size());
            sql.append(" OR (t.assignee_type = 'ROLE' AND t.assignee_value IN (")
                    .append(placeholders).append("))")
                    .append(" OR EXISTS (SELECT 1 FROM wf_task_candidate c")
                    .append(" WHERE c.task_id = t.id AND c.candidate_type = 'ROLE'")
                    .append(" AND c.candidate_value IN (").append(placeholders).append("))");
            args.addAll(roles);
            args.addAll(roles);
        }
        sql.append(")");
    }

    private void appendDone(StringBuilder sql,
                            List<Object> args,
                            String tenantId,
                            String userId) {
        sql.append(baseSelect("a.action", "a.created_at"));
        sql.append(" FROM wf_action_log a")
                .append(" JOIN wf_task t ON t.id = a.task_id")
                .append(" JOIN wf_instance i ON i.id = t.instance_id")
                .append(" WHERE t.tenant_id = ? AND a.operator_id = ?")
                .append(" AND a.action IN ('APPROVE','REJECT','RETURN_TO_INITIATOR','RESUBMIT')");
        args.add(tenantId);
        args.add(userId);
    }

    private void appendInitiated(StringBuilder sql,
                                 List<Object> args,
                                 String tenantId,
                                 String userId) {
        sql.append(baseSelect("NULL", "COALESCE(t.created_at, i.started_at)"));
        sql.append(" FROM wf_instance i")
                .append(" LEFT JOIN wf_task t ON t.id = (")
                .append("SELECT t2.id FROM wf_task t2 WHERE t2.instance_id = i.id")
                .append(" ORDER BY t2.created_at DESC LIMIT 1)")
                .append(" WHERE i.tenant_id = ? AND i.initiator_id = ?");
        args.add(tenantId);
        args.add(userId);
    }

    private String baseSelect(String actionExpression, String createdExpression) {
        return "SELECT t.id AS task_id, i.id AS instance_id, i.definition_key,"
                + " i.source_system, i.business_type, i.business_id, i.initiator_id,"
                + " i.current_node_key, t.task_name, t.assignee_type, t.assignee_value,"
                + " t.status AS task_status, i.status AS instance_status,"
                + " " + actionExpression + " AS action_name,"
                + " " + createdExpression + " AS item_created_at, t.completed_at";
    }

    private WorkflowContracts.TaskCenterItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new WorkflowContracts.TaskCenterItem(
                rs.getString("task_id"),
                rs.getString("instance_id"),
                rs.getString("definition_key"),
                rs.getString("source_system"),
                rs.getString("business_type"),
                rs.getString("business_id"),
                rs.getString("initiator_id"),
                rs.getString("current_node_key"),
                rs.getString("task_name"),
                rs.getString("assignee_type"),
                rs.getString("assignee_value"),
                rs.getString("task_status"),
                rs.getString("instance_status"),
                rs.getString("action_name"),
                rs.getObject("item_created_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class)
        );
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private record QueryPlan(String sql, List<Object> arguments) {
    }
}
