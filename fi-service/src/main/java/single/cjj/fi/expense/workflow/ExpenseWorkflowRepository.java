package single.cjj.fi.expense.workflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseWorkflowRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseWorkflowRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertExpense(ExpenseRow row) {
        jdbcTemplate.update("""
                INSERT INTO fi_expense_reimbursement
                    (id, tenant_id, document_number, applicant_id, department_code,
                     amount, currency_code, description_text, status, version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 0, ?)
                """, row.id(), row.tenantId(), row.documentNumber(), row.applicantId(),
                row.departmentCode(), row.amount(), row.currency(), row.description(), row.createdAt());
    }

    public Optional<ExpenseRow> findExpense(String expenseId) {
        return first(jdbcTemplate.query(
                "SELECT * FROM fi_expense_reimbursement WHERE id = ?",
                this::mapExpense,
                expenseId
        ));
    }

    public int markApproving(String expenseId, int expectedVersion, String workflowInstanceId) {
        return jdbcTemplate.update("""
                UPDATE fi_expense_reimbursement
                SET status = 'APPROVING', workflow_instance_id = COALESCE(?, workflow_instance_id),
                    submitted_at = COALESCE(submitted_at, NOW()), version = version + 1,
                    updated_at = NOW()
                WHERE id = ? AND version = ? AND status IN ('DRAFT', 'RETURNED')
                """, workflowInstanceId, expenseId, expectedVersion);
    }

    public void upsertBinding(BindingRow row) {
        jdbcTemplate.update("""
                INSERT INTO fi_workflow_binding
                    (id, tenant_id, business_type, business_id, workflow_instance_id,
                     workflow_definition_key, workflow_status, business_status,
                     submit_request_id, callback_url, submitted_by, submitted_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'APPROVING', ?, ?, ?, NOW(), 0)
                ON DUPLICATE KEY UPDATE
                    workflow_instance_id = COALESCE(VALUES(workflow_instance_id), workflow_instance_id),
                    workflow_definition_key = VALUES(workflow_definition_key),
                    workflow_status = VALUES(workflow_status),
                    business_status = 'APPROVING',
                    submit_request_id = VALUES(submit_request_id),
                    callback_url = VALUES(callback_url),
                    submitted_by = VALUES(submitted_by),
                    submitted_at = NOW(),
                    version = version + 1
                """, row.id(), row.tenantId(), row.businessType(), row.businessId(),
                row.workflowInstanceId(), row.workflowDefinitionKey(), row.workflowStatus(),
                row.submitRequestId(), row.callbackUrl(), row.submittedBy());
    }

    public Optional<BindingRow> findBinding(String tenantId, String businessType, String businessId) {
        return first(jdbcTemplate.query("""
                SELECT * FROM fi_workflow_binding
                WHERE tenant_id = ? AND business_type = ? AND business_id = ?
                LIMIT 1
                """, this::mapBinding, tenantId, businessType, businessId));
    }

    public void insertOutbox(OutboxRow row) {
        jdbcTemplate.update("""
                INSERT INTO fi_event_outbox
                    (id, event_id, aggregate_type, aggregate_id, event_type,
                     payload_json, idempotency_key, status, retry_count, created_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, 'PENDING', 0, ?)
                """, row.id(), row.eventId(), row.aggregateType(), row.aggregateId(),
                row.eventType(), row.payloadJson(), row.idempotencyKey(), row.createdAt());
    }

    public List<OutboxRow> findDispatchable(int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM fi_event_outbox
                WHERE status IN ('PENDING', 'FAILED')
                  AND (next_retry_time IS NULL OR next_retry_time <= NOW())
                ORDER BY created_at
                LIMIT ?
                """, this::mapOutbox, limit);
    }

    public boolean claimOutbox(String id) {
        return jdbcTemplate.update("""
                UPDATE fi_event_outbox
                SET status = 'SENDING'
                WHERE id = ? AND status IN ('PENDING', 'FAILED')
                """, id) == 1;
    }

    public void markOutboxSent(String id) {
        jdbcTemplate.update("""
                UPDATE fi_event_outbox
                SET status = 'SUCCESS', sent_at = NOW(), last_error = NULL
                WHERE id = ? AND status = 'SENDING'
                """, id);
    }

    public void markOutboxFailed(String id, LocalDateTime nextRetryTime, String error) {
        jdbcTemplate.update("""
                UPDATE fi_event_outbox
                SET status = 'FAILED', retry_count = retry_count + 1,
                    next_retry_time = ?, last_error = ?
                WHERE id = ? AND status = 'SENDING'
                """, nextRetryTime, abbreviate(error, 1000), id);
    }

    public void bindWorkflowInstance(String tenantId,
                                     String businessType,
                                     String businessId,
                                     String workflowInstanceId,
                                     String workflowStatus) {
        jdbcTemplate.update("""
                UPDATE fi_workflow_binding
                SET workflow_instance_id = ?, workflow_status = ?, version = version + 1
                WHERE tenant_id = ? AND business_type = ? AND business_id = ?
                """, workflowInstanceId, workflowStatus, tenantId, businessType, businessId);
        jdbcTemplate.update("""
                UPDATE fi_expense_reimbursement
                SET workflow_instance_id = ?, updated_at = NOW()
                WHERE id = ? AND tenant_id = ?
                """, workflowInstanceId, businessId, tenantId);
    }

    public int insertWorkflowEventIfAbsent(WorkflowEventRow row) {
        return jdbcTemplate.update("""
                INSERT IGNORE INTO fi_workflow_event_log
                    (event_id, event_type, workflow_instance_id, business_type,
                     business_id, processed_status, created_at)
                VALUES (?, ?, ?, ?, ?, 'PROCESSING', ?)
                """, row.eventId(), row.eventType(), row.workflowInstanceId(),
                row.businessType(), row.businessId(), row.createdAt());
    }

    public int applyWorkflowEvent(String tenantId,
                                  String businessId,
                                  String workflowInstanceId,
                                  String businessStatus,
                                  String workflowStatus,
                                  boolean terminal) {
        int affected = jdbcTemplate.update("""
                UPDATE fi_expense_reimbursement
                SET status = ?, workflow_instance_id = ?,
                    completed_at = CASE WHEN ? THEN NOW() ELSE completed_at END,
                    version = version + 1, updated_at = NOW()
                WHERE id = ? AND tenant_id = ?
                  AND (workflow_instance_id IS NULL OR workflow_instance_id = ?)
                """, businessStatus, workflowInstanceId, terminal,
                businessId, tenantId, workflowInstanceId);
        jdbcTemplate.update("""
                UPDATE fi_workflow_binding
                SET workflow_instance_id = ?, workflow_status = ?, business_status = ?,
                    completed_at = CASE WHEN ? THEN NOW() ELSE completed_at END,
                    version = version + 1
                WHERE tenant_id = ? AND business_type = 'EXPENSE_REIMBURSEMENT'
                  AND business_id = ?
                  AND (workflow_instance_id IS NULL OR workflow_instance_id = ?)
                """, workflowInstanceId, workflowStatus, businessStatus, terminal,
                tenantId, businessId, workflowInstanceId);
        return affected;
    }

    public void markWorkflowEventProcessed(String eventId) {
        jdbcTemplate.update("""
                UPDATE fi_workflow_event_log
                SET processed_status = 'SUCCESS', processed_at = NOW(), error_message = NULL
                WHERE event_id = ?
                """, eventId);
    }

    private ExpenseRow mapExpense(ResultSet rs, int rowNum) throws SQLException {
        return new ExpenseRow(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("document_number"),
                rs.getString("applicant_id"), rs.getString("department_code"),
                rs.getBigDecimal("amount"), rs.getString("currency_code"),
                rs.getString("description_text"), rs.getString("status"),
                rs.getString("workflow_instance_id"), rs.getInt("version"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("submitted_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class)
        );
    }

    private BindingRow mapBinding(ResultSet rs, int rowNum) throws SQLException {
        return new BindingRow(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("business_type"),
                rs.getString("business_id"), rs.getString("workflow_instance_id"),
                rs.getString("workflow_definition_key"), rs.getString("workflow_status"),
                rs.getString("business_status"), rs.getString("submit_request_id"),
                rs.getString("callback_url"), rs.getString("submitted_by")
        );
    }

    private OutboxRow mapOutbox(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxRow(
                rs.getString("id"), rs.getString("event_id"), rs.getString("aggregate_type"),
                rs.getString("aggregate_id"), rs.getString("event_type"),
                rs.getString("payload_json"), rs.getString("idempotency_key"),
                rs.getString("status"), rs.getInt("retry_count"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    private <T> Optional<T> first(List<T> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private String abbreviate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

    public record ExpenseRow(
            String id, String tenantId, String documentNumber, String applicantId,
            String departmentCode, BigDecimal amount, String currency, String description,
            String status, String workflowInstanceId, int version, LocalDateTime createdAt,
            LocalDateTime submittedAt, LocalDateTime completedAt
    ) {
    }

    public record BindingRow(
            String id, String tenantId, String businessType, String businessId,
            String workflowInstanceId, String workflowDefinitionKey, String workflowStatus,
            String businessStatus, String submitRequestId, String callbackUrl, String submittedBy
    ) {
    }

    public record OutboxRow(
            String id, String eventId, String aggregateType, String aggregateId,
            String eventType, String payloadJson, String idempotencyKey,
            String status, int retryCount, LocalDateTime createdAt
    ) {
    }

    public record WorkflowEventRow(
            String eventId, String eventType, String workflowInstanceId,
            String businessType, String businessId, LocalDateTime createdAt
    ) {
    }
}
