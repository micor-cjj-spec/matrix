package single.cjj.fi.fund.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PaymentOrderRepository {

    private final JdbcTemplate jdbc;

    public PaymentOrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PaymentApplicationRow lockApplication(Long fid, String tenantId) {
        List<PaymentApplicationRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fordered_amount,
                       ffund_plan_id, fplanned_pay_date, fpayment_method,
                       fpayee_bank_account_id, fpayee_account_name, fpayee_bank_name,
                       fpayee_bank_account_no,
                       fstatus, fapproval_status, fexecution_status, fversion
                  FROM matrix_fi_payment_application
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapApplication, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentApplicationRow findApplication(Long fid, String tenantId) {
        List<PaymentApplicationRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fordered_amount,
                       ffund_plan_id, fplanned_pay_date, fpayment_method,
                       fpayee_bank_account_id, fpayee_account_name, fpayee_bank_name,
                       fpayee_bank_account_no,
                       fstatus, fapproval_status, fexecution_status, fversion
                  FROM matrix_fi_payment_application
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapApplication, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentApplicationRow findApplicationById(Long fid) {
        List<PaymentApplicationRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fordered_amount,
                       ffund_plan_id, fplanned_pay_date, fpayment_method,
                       fpayee_bank_account_id, fpayee_account_name, fpayee_bank_name,
                       fpayee_bank_account_no,
                       fstatus, fapproval_status, fexecution_status, fversion
                  FROM matrix_fi_payment_application
                 WHERE fid = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapApplication, fid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateApplicationOrdered(
            Long fid,
            String tenantId,
            BigDecimal orderedAmount,
            String executionStatus,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application
                   SET fordered_amount = ?,
                       fexecution_status = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """, orderedAmount, executionStatus, operatorId, ts(LocalDateTime.now()), fid, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment application ordered amount update failed: " + fid);
        }
    }

    public BigDecimal sumOrderedByApplication(Long applicationId, String tenantId) {
        BigDecimal value = jdbc.queryForObject("""
                SELECT COALESCE(SUM(famount), 0)
                  FROM matrix_fi_payment_order_allocation
                 WHERE ftenant_id = ?
                   AND fpayment_application_id = ?
                   AND fstatus IN ('ORDERED', 'CONSUMED')
                   AND fdelete_flag = 0
                """, BigDecimal.class, tenantId, applicationId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public void insertOrder(PaymentOrderRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_payment_order
                (fid, ftenant_id, forg_id, fnumber, fdate,
                 fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, fpayment_method,
                 fpayer_bank_account_id, fpayee_bank_account_id,
                 fpayee_account_name, fpayee_bank_name, fpayee_bank_account_no,
                 ffund_plan_id, fplanned_pay_date,
                 fliquidity_check_status,
                 fstatus, fapproval_status,
                 fchannel_status, fbank_match_status,
                 fbotp_idempotency_key, fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_execution_id, fremark,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?,
                        'PENDING',
                        'DRAFT', 'DRAFT',
                        'NOT_SENT', 'UNMATCHED',
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?,
                        0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.number(), Date.valueOf(row.date()),
                row.businessPartnerId(), row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), row.paymentMethod(),
                row.payerBankAccountId(), row.payeeBankAccountId(),
                row.payeeAccountName(), row.payeeBankName(), row.payeeBankAccountNo(),
                row.fundPlanId(), date(row.plannedPayDate()),
                row.botpIdempotencyKey(), row.sourceSystemCode(), row.sourceDocumentType(),
                row.sourceDocumentId(), row.sourceExecutionId(), row.remark(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public PaymentOrderRow findOrder(Long fid, String tenantId) {
        List<PaymentOrderRow> rows = jdbc.query(baseOrderSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapOrder, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentOrderRow findOrderById(Long fid) {
        List<PaymentOrderRow> rows = jdbc.query(baseOrderSelect() + """
                 WHERE fid = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapOrder, fid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentOrderRow lockOrder(Long fid, String tenantId) {
        List<PaymentOrderRow> rows = jdbc.query(baseOrderSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapOrder, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentOrderRow findByIdempotency(String tenantId, String key) {
        List<PaymentOrderRow> rows = jdbc.query(baseOrderSelect() + """
                 WHERE ftenant_id = ? AND fbotp_idempotency_key = ?
                   AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapOrder, tenantId, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<PaymentOrderRow> listOrders(
            String tenantId, Long orgId, String status, int limit
    ) {
        StringBuilder sql = new StringBuilder(baseOrderSelect())
                .append(" WHERE ftenant_id = ? AND fdelete_flag = 0");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (orgId != null) {
            sql.append(" AND forg_id = ?");
            args.add(orgId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND fstatus = ?");
            args.add(status);
        }
        sql.append(" ORDER BY fid DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 500)));
        return jdbc.query(sql.toString(), this::mapOrder, args.toArray());
    }

    public void insertAllocation(PaymentOrderAllocationRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_payment_order_allocation
                (fid, ftenant_id, forg_id, fpayment_order_id,
                 fpayment_application_id, famount, fstatus,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.paymentOrderId(),
                row.paymentApplicationId(), row.amount(), row.status(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public List<PaymentOrderAllocationRow> findAllocations(Long orderId, String tenantId) {
        return jdbc.query("""
                SELECT a.fid, a.ftenant_id, a.forg_id, a.fpayment_order_id,
                       a.fpayment_application_id,
                       p.fnumber AS application_number,
                       a.famount, a.fstatus,
                       a.fcreate_by, a.fcreate_time, a.fversion
                  FROM matrix_fi_payment_order_allocation a
                  JOIN matrix_fi_payment_application p
                    ON p.fid = a.fpayment_application_id
                   AND p.fdelete_flag = 0
                 WHERE a.fpayment_order_id = ?
                   AND a.ftenant_id = ?
                   AND a.fdelete_flag = 0
                 ORDER BY a.fpayment_application_id, a.fid
                """, (rs, n) -> new PaymentOrderAllocationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fpayment_order_id"), rs.getLong("fpayment_application_id"),
                rs.getString("application_number"), rs.getBigDecimal("famount"),
                rs.getString("fstatus"), rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(), rs.getInt("fversion")
        ), orderId, tenantId);
    }

    public void updateAllocationStatus(
            Long allocationId,
            String tenantId,
            String status,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order_allocation
                   SET fstatus = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """, status, operatorId, ts(LocalDateTime.now()), allocationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order allocation update failed: " + allocationId);
        }
    }

    public void updateLiquidity(
            Long orderId,
            String tenantId,
            String status,
            String checkId,
            BigDecimal availableAmount,
            String message,
            String snapshotJson,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fliquidity_check_status = ?,
                       fliquidity_check_id = ?,
                       fliquidity_available_amount = ?,
                       fliquidity_check_message = ?,
                       fliquidity_snapshot_json =
                         CASE WHEN ? IS NULL
                              THEN fliquidity_snapshot_json
                              ELSE CAST(? AS JSON)
                          END,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                status, checkId, availableAmount, message,
                snapshotJson, snapshotJson,
                operatorId, ts(LocalDateTime.now()), orderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order liquidity update failed: " + orderId);
        }
    }

    public void updateOrderState(
            Long orderId,
            String tenantId,
            String status,
            String approvalStatus,
            String rejectReason,
            Long auditBy,
            LocalDateTime auditTime,
            LocalDateTime submittedTime,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fstatus = ?,
                       fapproval_status = ?,
                       freject_reason = ?,
                       faudit_by = COALESCE(?, faudit_by),
                       faudit_time = COALESCE(?, faudit_time),
                       fsubmitted_time = COALESCE(?, fsubmitted_time),
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                status, approvalStatus, rejectReason,
                auditBy, ts(auditTime), ts(submittedTime),
                operatorId, ts(LocalDateTime.now()), orderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order state update failed: " + orderId);
        }
    }

    public void markPaying(
            Long orderId,
            String tenantId,
            String channelCode,
            String channelRequestId,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fstatus = 'PAYING',
                       fchannel_code = ?,
                       fchannel_request_id = ?,
                       fchannel_status = 'SUBMITTED',
                       fchannel_error = NULL,
                       fpaying_time = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ?
                   AND fstatus = 'AUDITED'
                   AND fdelete_flag = 0
                """,
                channelCode, channelRequestId, ts(LocalDateTime.now()),
                operatorId, ts(LocalDateTime.now()), orderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order submit-to-bank state changed: " + orderId);
        }
    }

    public void markChannelFailed(
            Long orderId,
            String tenantId,
            String error,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fstatus = 'FAILED',
                       fchannel_status = 'FAILED',
                       fchannel_error = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ?
                   AND fstatus = 'PAYING'
                   AND fdelete_flag = 0
                """, abbreviate(error, 1000), operatorId, ts(LocalDateTime.now()), orderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order channel failure state changed: " + orderId);
        }
    }

    public void updateOrderBankMatch(
            Long orderId,
            String tenantId,
            String matchStatus,
            Long batchId,
            Long caseId,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fbank_match_status = ?,
                       freconciliation_batch_id = ?,
                       freconciliation_case_id = ?,
                       fchannel_status =
                         CASE WHEN ? = 'MATCHED' THEN 'CONFIRMED' ELSE fchannel_status END,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                matchStatus, batchId, caseId, matchStatus,
                operatorId, ts(LocalDateTime.now()), orderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment order bank match update failed: " + orderId);
        }
    }

    private String baseOrderSelect() {
        return """
                SELECT fid, ftenant_id, forg_id, fnumber, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fpayment_method,
                       fpayer_bank_account_id, fpayee_bank_account_id,
                       fpayee_account_name, fpayee_bank_name, fpayee_bank_account_no,
                       ffund_plan_id, fplanned_pay_date,
                       fliquidity_check_status, fliquidity_check_id,
                       fliquidity_available_amount, fliquidity_check_message,
                       fstatus, fapproval_status,
                       fchannel_code, fchannel_request_id, fchannel_status, fchannel_error,
                       fsubmitted_time, faudit_by, faudit_time, fpaying_time,
                       fbank_match_status, freconciliation_batch_id, freconciliation_case_id,
                       fbotp_idempotency_key, fsource_system_code, fsource_document_type,
                       fsource_document_id, fsource_execution_id,
                       fremark, freject_reason, fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_payment_order
                """;
    }

    private PaymentApplicationRow mapApplication(ResultSet rs, int n) throws SQLException {
        return new PaymentApplicationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getBigDecimal("fordered_amount"),
                rs.getString("ffund_plan_id"), nullableDate(rs.getDate("fplanned_pay_date")),
                rs.getString("fpayment_method"), rs.getString("fpayee_bank_account_id"),
                rs.getString("fpayee_account_name"), rs.getString("fpayee_bank_name"),
                rs.getString("fpayee_bank_account_no"), rs.getString("fstatus"),
                rs.getString("fapproval_status"), rs.getString("fexecution_status"),
                rs.getInt("fversion")
        );
    }

    private PaymentOrderRow mapOrder(ResultSet rs, int n) throws SQLException {
        return new PaymentOrderRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getString("fpayment_method"),
                rs.getString("fpayer_bank_account_id"), rs.getString("fpayee_bank_account_id"),
                rs.getString("fpayee_account_name"), rs.getString("fpayee_bank_name"),
                rs.getString("fpayee_bank_account_no"), rs.getString("ffund_plan_id"),
                nullableDate(rs.getDate("fplanned_pay_date")),
                rs.getString("fliquidity_check_status"), rs.getString("fliquidity_check_id"),
                rs.getBigDecimal("fliquidity_available_amount"),
                rs.getString("fliquidity_check_message"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("fchannel_code"), rs.getString("fchannel_request_id"),
                rs.getString("fchannel_status"), rs.getString("fchannel_error"),
                nullableTime(rs.getTimestamp("fsubmitted_time")),
                rs.getObject("faudit_by", Long.class), nullableTime(rs.getTimestamp("faudit_time")),
                nullableTime(rs.getTimestamp("fpaying_time")),
                rs.getString("fbank_match_status"),
                rs.getObject("freconciliation_batch_id", Long.class),
                rs.getObject("freconciliation_case_id", Long.class),
                rs.getString("fbotp_idempotency_key"), rs.getString("fsource_system_code"),
                rs.getString("fsource_document_type"), rs.getString("fsource_document_id"),
                rs.getString("fsource_execution_id"), rs.getString("fremark"),
                rs.getString("freject_reason"), rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(), rs.getInt("fversion")
        );
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private LocalDate nullableDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime nullableTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private String abbreviate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public record PaymentApplicationRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            BigDecimal orderedAmount,
            String fundPlanId,
            LocalDate plannedPayDate,
            String paymentMethod,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String status,
            String approvalStatus,
            String executionStatus,
            Integer version
    ) {
    }

    public record PaymentOrderRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            String paymentMethod,
            String payerBankAccountId,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String fundPlanId,
            LocalDate plannedPayDate,
            String liquidityCheckStatus,
            String liquidityCheckId,
            BigDecimal liquidityAvailableAmount,
            String liquidityCheckMessage,
            String status,
            String approvalStatus,
            String channelCode,
            String channelRequestId,
            String channelStatus,
            String channelError,
            LocalDateTime submittedTime,
            Long auditBy,
            LocalDateTime auditTime,
            LocalDateTime payingTime,
            String bankMatchStatus,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            String botpIdempotencyKey,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String remark,
            String rejectReason,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }

    public record PaymentOrderAllocationRow(
            Long id,
            String tenantId,
            Long orgId,
            Long paymentOrderId,
            Long paymentApplicationId,
            String paymentApplicationNumber,
            BigDecimal amount,
            String status,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }
}
