package single.cjj.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureDefinition(String tenantId,
                                 String definitionKey,
                                 String definitionName,
                                 String createdBy) {
        jdbcTemplate.update("""
                INSERT INTO wf_definition
                    (tenant_id, definition_key, definition_name, latest_version, status, created_by)
                VALUES (?, ?, ?, 0, 'ACTIVE', ?)
                ON DUPLICATE KEY UPDATE definition_name = VALUES(definition_name)
                """, tenantId, definitionKey, definitionName, createdBy);
    }

    public int nextDefinitionVersion(String tenantId, String definitionKey) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version), 0) + 1
                FROM wf_definition_version
                WHERE tenant_id = ? AND definition_key = ?
                """, Integer.class, tenantId, definitionKey);
        return value == null ? 1 : value;
    }

    public void insertDefinitionVersion(String tenantId,
                                        String definitionKey,
                                        String definitionName,
                                        int version,
                                        String definitionJson,
                                        String createdBy) {
        jdbcTemplate.update("""
                INSERT INTO wf_definition_version
                    (tenant_id, definition_key, definition_name, version,
                     definition_json, status, created_by)
                VALUES (?, ?, ?, ?, CAST(? AS JSON), 'DRAFT', ?)
                """, tenantId, definitionKey, definitionName, version, definitionJson, createdBy);
        jdbcTemplate.update("""
                UPDATE wf_definition
                SET latest_version = GREATEST(latest_version, ?)
                WHERE tenant_id = ? AND definition_key = ?
                """, version, tenantId, definitionKey);
    }

    public int publishDefinition(String tenantId, String definitionKey, int version) {
        jdbcTemplate.update("""
                UPDATE wf_definition_version
                SET status = 'RETIRED'
                WHERE tenant_id = ? AND definition_key = ? AND status = 'PUBLISHED'
                """, tenantId, definitionKey);
        return jdbcTemplate.update("""
                UPDATE wf_definition_version
                SET status = 'PUBLISHED', published_at = NOW()
                WHERE tenant_id = ? AND definition_key = ? AND version = ? AND status = 'DRAFT'
                """, tenantId, definitionKey, version);
    }

    public Optional<DefinitionVersionRow> findDefinition(String tenantId,
                                                         String definitionKey,
                                                         int version) {
        return first(jdbcTemplate.query("""
                SELECT tenant_id, definition_key, definition_name, version,
                       definition_json, status, created_at, published_at
                FROM wf_definition_version
                WHERE tenant_id = ? AND definition_key = ? AND version = ?
                """, this::mapDefinition, tenantId, definitionKey, version));
    }

    public Optional<DefinitionVersionRow> findPublishedDefinition(String tenantId,
                                                                  String definitionKey) {
        return first(jdbcTemplate.query("""
                SELECT tenant_id, definition_key, definition_name, version,
                       definition_json, status, created_at, published_at
                FROM wf_definition_version
                WHERE tenant_id = ? AND definition_key = ? AND status = 'PUBLISHED'
                ORDER BY version DESC
                LIMIT 1
                """, this::mapDefinition, tenantId, definitionKey));
    }

    public Optional<InstanceRow> findByIdempotency(String tenantId,
                                                   String sourceSystem,
                                                   String idempotencyKey) {
        return first(jdbcTemplate.query("""
                SELECT * FROM wf_instance
                WHERE tenant_id = ? AND source_system = ? AND idempotency_key = ?
                LIMIT 1
                """, this::mapInstance, tenantId, sourceSystem, idempotencyKey));
    }

    public void insertInstance(InstanceRow row) {
        jdbcTemplate.update("""
                INSERT INTO wf_instance
                    (id, tenant_id, definition_key, definition_version,
                     source_system, business_type, business_id, business_key,
                     idempotency_key, initiator_id, current_node_key, status,
                     variables_json, callback_url, version, started_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, 0, ?)
                """, row.id(), row.tenantId(), row.definitionKey(), row.definitionVersion(),
                row.sourceSystem(), row.businessType(), row.businessId(), row.businessKey(),
                row.idempotencyKey(), row.initiatorId(), row.currentNodeKey(), row.status(),
                row.variablesJson(), row.callbackUrl(), row.startedAt());
    }

    public Optional<InstanceRow> findInstance(String instanceId) {
        return first(jdbcTemplate.query("SELECT * FROM wf_instance WHERE id = ?",
                this::mapInstance, instanceId));
    }

    public Optional<InstanceRow> findLatestBusinessInstance(String tenantId,
                                                            String sourceSystem,
                                                            String businessType,
                                                            String businessId) {
        return first(jdbcTemplate.query("""
                SELECT * FROM wf_instance
                WHERE tenant_id = ? AND source_system = ?
                  AND business_type = ? AND business_id = ?
                ORDER BY started_at DESC
                LIMIT 1
                """, this::mapInstance, tenantId, sourceSystem, businessType, businessId));
    }

    public void insertNodeInstance(NodeInstanceRow row) {
        jdbcTemplate.update("""
                INSERT INTO wf_node_instance
                    (id, instance_id, node_key, node_name, node_type, status,
                     handler_key, input_json, started_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                """, row.id(), row.instanceId(), row.nodeKey(), row.nodeName(), row.nodeType(),
                row.status(), row.handlerKey(), row.inputJson(), row.startedAt());
    }

    public void completeNode(String nodeInstanceId, String outputJson) {
        jdbcTemplate.update("""
                UPDATE wf_node_instance
                SET status = 'COMPLETED', output_json = CAST(? AS JSON), ended_at = NOW()
                WHERE id = ? AND status = 'ACTIVE'
                """, outputJson, nodeInstanceId);
    }

    public void insertTask(TaskRow row) {
        jdbcTemplate.update("""
                INSERT INTO wf_task
                    (id, tenant_id, instance_id, node_instance_id, node_key,
                     task_name, assignee_type, assignee_value, status, version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
                """, row.id(), row.tenantId(), row.instanceId(), row.nodeInstanceId(),
                row.nodeKey(), row.taskName(), row.assigneeType(), row.assigneeValue(),
                row.status(), row.createdAt());
    }

    public Optional<TaskRow> findTask(String taskId) {
        return first(jdbcTemplate.query("SELECT * FROM wf_task WHERE id = ?",
                this::mapTask, taskId));
    }

    public int completeTask(String taskId, int expectedVersion, String terminalStatus) {
        return jdbcTemplate.update("""
                UPDATE wf_task
                SET status = ?, version = version + 1, completed_at = NOW()
                WHERE id = ? AND status IN ('PENDING', 'CLAIMED') AND version = ?
                """, terminalStatus, taskId, expectedVersion);
    }

    public void updateInstancePosition(String instanceId,
                                       String currentNodeKey,
                                       String variablesJson) {
        jdbcTemplate.update("""
                UPDATE wf_instance
                SET current_node_key = ?, variables_json = CAST(? AS JSON), version = version + 1
                WHERE id = ? AND status = 'RUNNING'
                """, currentNodeKey, variablesJson, instanceId);
    }

    public int finishInstance(String instanceId,
                              String terminalStatus,
                              String variablesJson) {
        return jdbcTemplate.update("""
                UPDATE wf_instance
                SET status = ?, current_node_key = NULL,
                    variables_json = CAST(? AS JSON), ended_at = NOW(), version = version + 1
                WHERE id = ? AND status = 'RUNNING'
                """, terminalStatus, variablesJson, instanceId);
    }

    public void insertActionLog(String id,
                                String instanceId,
                                String taskId,
                                String action,
                                String operatorId,
                                String comment,
                                String beforeStatus,
                                String afterStatus,
                                String requestId) {
        jdbcTemplate.update("""
                INSERT INTO wf_action_log
                    (id, instance_id, task_id, action, operator_id, comment_text,
                     before_status, after_status, request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, instanceId, taskId, action, operatorId, comment,
                beforeStatus, afterStatus, requestId);
    }

    public void insertOutbox(String id,
                             String eventId,
                             String instanceId,
                             String eventType,
                             String payloadJson) {
        jdbcTemplate.update("""
                INSERT INTO wf_event_outbox
                    (id, event_id, instance_id, event_type, payload_json, status)
                VALUES (?, ?, ?, ?, CAST(? AS JSON), 'PENDING')
                """, id, eventId, instanceId, eventType, payloadJson);
    }

    private DefinitionVersionRow mapDefinition(ResultSet rs, int rowNum) throws SQLException {
        return new DefinitionVersionRow(
                rs.getString("tenant_id"),
                rs.getString("definition_key"),
                rs.getString("definition_name"),
                rs.getInt("version"),
                rs.getString("definition_json"),
                rs.getString("status"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("published_at", LocalDateTime.class)
        );
    }

    private InstanceRow mapInstance(ResultSet rs, int rowNum) throws SQLException {
        return new InstanceRow(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("definition_key"),
                rs.getInt("definition_version"),
                rs.getString("source_system"),
                rs.getString("business_type"),
                rs.getString("business_id"),
                rs.getString("business_key"),
                rs.getString("idempotency_key"),
                rs.getString("initiator_id"),
                rs.getString("current_node_key"),
                rs.getString("status"),
                rs.getString("variables_json"),
                rs.getString("callback_url"),
                rs.getInt("version"),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("ended_at", LocalDateTime.class)
        );
    }

    private TaskRow mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new TaskRow(
                rs.getString("id"),
                rs.getString("tenant_id"),
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

    private <T> Optional<T> first(List<T> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public record DefinitionVersionRow(
            String tenantId,
            String definitionKey,
            String definitionName,
            int version,
            String definitionJson,
            String status,
            LocalDateTime createdAt,
            LocalDateTime publishedAt
    ) {
    }

    public record InstanceRow(
            String id,
            String tenantId,
            String definitionKey,
            int definitionVersion,
            String sourceSystem,
            String businessType,
            String businessId,
            String businessKey,
            String idempotencyKey,
            String initiatorId,
            String currentNodeKey,
            String status,
            String variablesJson,
            String callbackUrl,
            int version,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
    }

    public record NodeInstanceRow(
            String id,
            String instanceId,
            String nodeKey,
            String nodeName,
            String nodeType,
            String status,
            String handlerKey,
            String inputJson,
            LocalDateTime startedAt
    ) {
    }

    public record TaskRow(
            String id,
            String tenantId,
            String instanceId,
            String nodeInstanceId,
            String nodeKey,
            String taskName,
            String assigneeType,
            String assigneeValue,
            String status,
            int version,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
    }
}
