package single.cjj.fi.ap.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PaymentApplicationRepository {

    private final JdbcTemplate jdbc;

    public PaymentApplicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PayableRow lockPayable(Long fid, String tenantId) {
        List<PayableRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fopen_amount, fsettled_amount, freserved_amount,
                       fstatus, fapproval_status, faccounting_status, fversion
                  FROM matrix_fi_ap_payable
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, (rs, n) -> new PayableRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getString("ftype"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getBigDecimal("fopen_amount"),
                rs.getBigDecimal("fsettled_amount"), rs.getBigDecimal("freserved_amount"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("faccounting_status"), rs.getInt("fversion")
        ), fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PayableRow findPayable(Long fid, String tenantId) {
        List<PayableRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fopen_amount, fsettled_amount, freserved_amount,
                       fstatus, fapproval_status, faccounting_status, fversion
                  FROM matrix_fi_ap_payable
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, (rs, n) -> new PayableRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getString("ftype"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getBigDecimal("fopen_amount"),
                rs.getBigDecimal("fsettled_amount"), rs.getBigDecimal("freserved_amount"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("faccounting_status"), rs.getInt("fversion")
        ), fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PayableRow findPayableById(Long fid) {
        List<PayableRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fopen_amount, fsettled_amount, freserved_amount,
                       fstatus, fapproval_status, faccounting_status, fversion
                  FROM matrix_fi_ap_payable
                 WHERE fid = ? AND fdelete_flag = 0
                 LIMIT 1
                """, (rs, n) -> new PayableRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getString("ftype"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getBigDecimal("fopen_amount"),
                rs.getBigDecimal("fsettled_amount"), rs.getBigDecimal("freserved_amount"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("faccounting_status"), rs.getInt("fversion")
        ), fid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void setPayableReserved(Long fid, String tenantId, BigDecimal reserved, Long operatorId) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET freserved_amount = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """, reserved, operatorId, ts(LocalDateTime.now()), fid, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payable reservation update failed: " + fid);
        }
    }

    public BigDecimal sumReservedByPayable(Long payableId, String tenantId) {
        BigDecimal value = jdbc.queryForObject("""
                SELECT COALESCE(SUM(freserved_amount), 0)
                  FROM matrix_fi_payment_application_allocation
                 WHERE ftenant_id = ? AND fpayable_id = ?
                   AND fstatus = 'RESERVED' AND fdelete_flag = 0
                """, BigDecimal.class, tenantId, payableId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public void insertApplication(ApplicationRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_payment_application
                (fid, ftenant_id, forg_id, fnumber, fdate, frequester_id,
                 fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, ffund_plan_id, fplanned_pay_date, fpayment_method,
                 fpayee_bank_account_id, fpayee_account_name, fpayee_bank_name, fpayee_bank_account_no,
                 fevidence_check_status, fbudget_check_status,
                 fstatus, fapproval_status, fexecution_status,
                 fbotp_idempotency_key, fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_execution_id,
                 fremark, fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.number(), Date.valueOf(row.date()), row.requesterId(),
                row.businessPartnerId(), row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), row.fundPlanId(), date(row.plannedPayDate()), row.paymentMethod(),
                row.payeeBankAccountId(), row.payeeAccountName(), row.payeeBankName(), row.payeeBankAccountNo(),
                row.evidenceCheckStatus(), row.budgetCheckStatus(),
                row.status(), row.approvalStatus(), row.executionStatus(),
                row.botpIdempotencyKey(), row.sourceSystemCode(), row.sourceDocumentType(),
                row.sourceDocumentId(), row.sourceExecutionId(),
                row.remark(), row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public ApplicationRow findApplication(Long fid, String tenantId) {
        List<ApplicationRow> rows = jdbc.query(baseApplicationSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapApplication, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ApplicationRow lockApplication(Long fid, String tenantId) {
        List<ApplicationRow> rows = jdbc.query(baseApplicationSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapApplication, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ApplicationRow findApplicationById(Long fid) {
        List<ApplicationRow> rows = jdbc.query(baseApplicationSelect() + """
                 WHERE fid = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapApplication, fid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ApplicationRow findByIdempotency(String tenantId, String key) {
        List<ApplicationRow> rows = jdbc.query(baseApplicationSelect() + """
                 WHERE ftenant_id = ? AND fbotp_idempotency_key = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapApplication, tenantId, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ApplicationRow> listApplications(
            String tenantId, Long orgId, String status, int limit
    ) {
        StringBuilder sql = new StringBuilder(baseApplicationSelect())
                .append(" WHERE ftenant_id = ? AND fdelete_flag = 0");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
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
        return jdbc.query(sql.toString(), this::mapApplication, args.toArray());
    }

    public void insertAllocation(AllocationRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_payment_application_allocation
                (fid, ftenant_id, forg_id, fpayment_application_id, fpayable_id,
                 fapplied_amount, freserved_amount, fstatus,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.applicationId(), row.payableId(),
                row.appliedAmount(), row.reservedAmount(), row.status(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public List<AllocationRow> findAllocations(Long applicationId, String tenantId) {
        return jdbc.query("""
                SELECT a.fid, a.ftenant_id, a.forg_id, a.fpayment_application_id,
                       a.fpayable_id, p.fnumber AS payable_number,
                       a.fapplied_amount, a.freserved_amount, a.fstatus,
                       a.fcreate_by, a.fcreate_time, a.fversion
                  FROM matrix_fi_payment_application_allocation a
                  JOIN matrix_fi_ap_payable p ON p.fid = a.fpayable_id AND p.fdelete_flag = 0
                 WHERE a.fpayment_application_id = ? AND a.ftenant_id = ?
                   AND a.fdelete_flag = 0
                 ORDER BY a.fpayable_id, a.fid
                """, (rs, n) -> new AllocationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fpayment_application_id"), rs.getLong("fpayable_id"),
                rs.getString("payable_number"), rs.getBigDecimal("fapplied_amount"),
                rs.getBigDecimal("freserved_amount"), rs.getString("fstatus"),
                rs.getObject("fcreate_by", Long.class), rs.getTimestamp("fcreate_time").toLocalDateTime(),
                rs.getInt("fversion")
        ), applicationId, tenantId);
    }

    public void updateAllocationReservation(
            Long allocationId, String tenantId, BigDecimal reservedAmount, String status, Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application_allocation
                   SET freserved_amount = ?, fstatus = ?,
                       fmodify_by = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """, reservedAmount, status, operatorId, ts(LocalDateTime.now()), allocationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment allocation update failed: " + allocationId);
        }
    }

    public void insertEvidence(EvidenceRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_payment_application_evidence
                (fid, ftenant_id, forg_id, fpayment_application_id,
                 fevidence_type, fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_document_no, frequired,
                 fverification_status, fremark,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.applicationId(),
                row.evidenceType(), row.sourceSystemCode(), row.sourceDocumentType(),
                row.sourceDocumentId(), row.sourceDocumentNo(), row.required() ? 1 : 0,
                row.verificationStatus(), row.remark(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public List<EvidenceRow> findEvidence(Long applicationId, String tenantId) {
        return jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fpayment_application_id,
                       fevidence_type, fsource_system_code, fsource_document_type,
                       fsource_document_id, fsource_document_no, frequired,
                       fverification_status, fremark, fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_payment_application_evidence
                 WHERE fpayment_application_id = ? AND ftenant_id = ?
                   AND fdelete_flag = 0
                 ORDER BY fid
                """, (rs, n) -> new EvidenceRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fpayment_application_id"), rs.getString("fevidence_type"),
                rs.getString("fsource_system_code"), rs.getString("fsource_document_type"),
                rs.getString("fsource_document_id"), rs.getString("fsource_document_no"),
                rs.getBoolean("frequired"), rs.getString("fverification_status"),
                rs.getString("fremark"), rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(), rs.getInt("fversion")
        ), applicationId, tenantId);
    }

    public void verifyEvidence(
            Long evidenceId, Long applicationId, String tenantId, String status, String remark, Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application_evidence
                   SET fverification_status = ?, fremark = COALESCE(?, fremark),
                       fmodify_by = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ? AND fpayment_application_id = ? AND ftenant_id = ?
                   AND fdelete_flag = 0
                """, status, remark, operatorId, ts(LocalDateTime.now()),
                evidenceId, applicationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment evidence update failed: " + evidenceId);
        }
    }

    public void setEvidenceCheckStatus(Long applicationId, String tenantId, String status, Long operatorId) {
        jdbc.update("""
                UPDATE matrix_fi_payment_application
                   SET fevidence_check_status = ?, fmodify_by = ?, fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """, status, operatorId, ts(LocalDateTime.now()), applicationId, tenantId);
    }

    public void updateBudgetCheck(
            Long applicationId,
            String tenantId,
            String status,
            String checkId,
            String fundPlanId,
            BigDecimal availableAmount,
            String message,
            String snapshotJson,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application
                   SET fbudget_check_status = ?, fbudget_check_id = ?,
                       ffund_plan_id = COALESCE(?, ffund_plan_id),
                       fbudget_available_amount = ?, fbudget_check_message = ?,
                       fbudget_snapshot_json = CASE WHEN ? IS NULL THEN fbudget_snapshot_json ELSE CAST(? AS JSON) END,
                       fmodify_by = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                status, checkId, fundPlanId, availableAmount, message,
                snapshotJson, snapshotJson,
                operatorId, ts(LocalDateTime.now()), applicationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("budget check update failed: " + applicationId);
        }
    }

    public void updateApplicationState(
            Long applicationId,
            String tenantId,
            String status,
            String approvalStatus,
            String rejectReason,
            Long approvedBy,
            LocalDateTime approvedTime,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application
                   SET fstatus = ?, fapproval_status = ?, freject_reason = ?,
                       fapproved_by = ?, fapproved_time = ?,
                       fmodify_by = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                status, approvalStatus, rejectReason, approvedBy, ts(approvedTime),
                operatorId, ts(LocalDateTime.now()), applicationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payment application state update failed: " + applicationId);
        }
    }

    private String baseApplicationSelect() {
        return """
                SELECT fid, ftenant_id, forg_id, fnumber, fdate, frequester_id,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, ffund_plan_id, fplanned_pay_date, fpayment_method,
                       fpayee_bank_account_id, fpayee_account_name, fpayee_bank_name, fpayee_bank_account_no,
                       fevidence_check_status, fbudget_check_status, fbudget_check_id,
                       fbudget_available_amount, fbudget_check_message,
                       fstatus, fapproval_status, fexecution_status,
                       fbotp_idempotency_key, fsource_system_code, fsource_document_type,
                       fsource_document_id, fsource_execution_id,
                       fremark, freject_reason, fapproved_by, fapproved_time,
                       fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_payment_application
                """;
    }

    private ApplicationRow mapApplication(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new ApplicationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getDate("fdate").toLocalDate(),
                rs.getObject("frequester_id", Long.class),
                rs.getString("fbusiness_partner_id"), rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getString("ffund_plan_id"),
                nullableDate(rs.getDate("fplanned_pay_date")), rs.getString("fpayment_method"),
                rs.getString("fpayee_bank_account_id"), rs.getString("fpayee_account_name"),
                rs.getString("fpayee_bank_name"), rs.getString("fpayee_bank_account_no"),
                rs.getString("fevidence_check_status"), rs.getString("fbudget_check_status"),
                rs.getString("fbudget_check_id"), rs.getBigDecimal("fbudget_available_amount"),
                rs.getString("fbudget_check_message"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("fexecution_status"), rs.getString("fbotp_idempotency_key"),
                rs.getString("fsource_system_code"), rs.getString("fsource_document_type"),
                rs.getString("fsource_document_id"), rs.getString("fsource_execution_id"),
                rs.getString("fremark"), rs.getString("freject_reason"),
                rs.getObject("fapproved_by", Long.class),
                nullableTime(rs.getTimestamp("fapproved_time")),
                rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(),
                rs.getInt("fversion")
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

    public record PayableRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            String type,
            LocalDate date,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            BigDecimal openAmount,
            BigDecimal settledAmount,
            BigDecimal reservedAmount,
            String status,
            String approvalStatus,
            String accountingStatus,
            Integer version
    ) {
    }

    public record ApplicationRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            Long requesterId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            String fundPlanId,
            LocalDate plannedPayDate,
            String paymentMethod,
            String payeeBankAccountId,
            String payeeAccountName,
            String payeeBankName,
            String payeeBankAccountNo,
            String evidenceCheckStatus,
            String budgetCheckStatus,
            String budgetCheckId,
            BigDecimal budgetAvailableAmount,
            String budgetCheckMessage,
            String status,
            String approvalStatus,
            String executionStatus,
            String botpIdempotencyKey,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String remark,
            String rejectReason,
            Long approvedBy,
            LocalDateTime approvedTime,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }

    public record AllocationRow(
            Long id,
            String tenantId,
            Long orgId,
            Long applicationId,
            Long payableId,
            String payableNumber,
            BigDecimal appliedAmount,
            BigDecimal reservedAmount,
            String status,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }

    public record EvidenceRow(
            Long id,
            String tenantId,
            Long orgId,
            Long applicationId,
            String evidenceType,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            boolean required,
            String verificationStatus,
            String remark,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }
}
