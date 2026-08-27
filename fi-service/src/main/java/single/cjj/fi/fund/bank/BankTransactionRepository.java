package single.cjj.fi.fund.bank;

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
public class BankTransactionRepository {

    private final JdbcTemplate jdbc;

    public BankTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(BankTransactionRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_bank_transaction
                (fid, ftenant_id, forg_id, fbank_account_id, fbank_transaction_no,
                 ftransaction_date, ftransaction_time, fdirection, fcurrency_code, famount,
                 fcounterparty_name, fcounterparty_account, fpurpose, fsummary,
                 fbank_receipt_no, fsource_channel, fmatch_status, fstatus,
                 fraw_payload_hash, fraw_payload_json,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, 'UNMATCHED', 'CONFIRMED',
                        ?, CASE WHEN ? IS NULL THEN NULL ELSE CAST(? AS JSON) END,
                        ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.bankAccountId(),
                row.bankTransactionNo(), Date.valueOf(row.transactionDate()),
                ts(row.transactionTime()), row.direction(), row.currencyCode(), row.amount(),
                row.counterpartyName(), row.counterpartyAccount(), row.purpose(), row.summary(),
                row.bankReceiptNo(), row.sourceChannel(), row.rawPayloadHash(),
                row.rawPayloadJson(), row.rawPayloadJson(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public BankTransactionRow findByNaturalKey(
            String tenantId,
            String bankAccountId,
            String bankTransactionNo
    ) {
        List<BankTransactionRow> rows = jdbc.query(baseSelect() + """
                 WHERE ftenant_id = ?
                   AND fbank_account_id = ?
                   AND fbank_transaction_no = ?
                   AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapRow, tenantId, bankAccountId, bankTransactionNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BankTransactionRow find(Long fid, String tenantId) {
        List<BankTransactionRow> rows = jdbc.query(baseSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapRow, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BankTransactionRow lock(Long fid, String tenantId) {
        List<BankTransactionRow> rows = jdbc.query(baseSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapRow, fid, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<BankTransactionRow> list(
            String tenantId,
            Long orgId,
            String matchStatus,
            int limit
    ) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE ftenant_id = ? AND fdelete_flag = 0");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (orgId != null) {
            sql.append(" AND forg_id = ?");
            args.add(orgId);
        }
        if (matchStatus != null && !matchStatus.isBlank()) {
            sql.append(" AND fmatch_status = ?");
            args.add(matchStatus);
        }
        sql.append(" ORDER BY ftransaction_date DESC, fid DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 500)));
        return jdbc.query(sql.toString(), this::mapRow, args.toArray());
    }

    public void updateMatch(
            Long fid,
            String tenantId,
            String matchStatus,
            Long paymentOrderId,
            Long batchId,
            Long caseId,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_bank_transaction
                   SET fmatch_status = ?,
                       fmatched_payment_order_id = ?,
                       freconciliation_batch_id = ?,
                       freconciliation_case_id = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                matchStatus, paymentOrderId, batchId, caseId,
                operatorId, ts(LocalDateTime.now()), fid, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("bank transaction match update failed: " + fid);
        }
    }

    public void insertReconciliationBatch(ReconciliationBatchRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_reconciliation_batch
                (fid, ftenant_id, forg_id, fbatch_no, fscenario_type,
                 frule_code, frule_version, frequest_id,
                 fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_document_no,
                 fstatus, fresult,
                 ftotal_case_count, fmatched_count, fpartial_count,
                 fdifference_count, funmatched_count,
                 fstart_time, ffinish_time, fcreate_time, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, 'BANK_PAYMENT',
                        'BANK_PAYMENT_MATCH', 1, ?,
                        'MATRIX', 'FI_BANK_TRANSACTION', ?, ?,
                        'COMPLETED', ?,
                        1, ?, 0, ?, 0,
                        ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.batchNo(),
                row.requestId(), String.valueOf(row.bankTransactionId()),
                row.bankTransactionNo(), row.result(),
                "MATCHED".equals(row.result()) ? 1 : 0,
                "DIFFERENCE".equals(row.result()) ? 1 : 0,
                ts(row.startTime()), ts(row.finishTime()),
                ts(row.startTime()), ts(row.finishTime()));
    }

    public void insertReconciliationCase(ReconciliationCaseRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_reconciliation_case
                (fid, ftenant_id, forg_id, fbatch_id, fcase_no, fcase_key,
                 fresult, favailable_quantity, fsnapshot_json, fstatus,
                 fcreate_time, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL,
                        CASE WHEN ? IS NULL THEN NULL ELSE CAST(? AS JSON) END,
                        'CLOSED', ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.batchId(),
                row.caseNo(), row.caseKey(), row.result(),
                row.snapshotJson(), row.snapshotJson(),
                ts(row.createTime()), ts(row.createTime()));
    }

    public void insertParticipant(ReconciliationParticipantRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_reconciliation_participant
                (fid, ftenant_id, forg_id, fcase_id, fparticipant_role,
                 fsystem_code, fdocument_type, fdocument_id, fdocument_no,
                 fentry_id, fbusiness_partner_id, fcurrency_code, fbusiness_date,
                 fsnapshot_json, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, 'MATRIX', ?, ?, ?, NULL,
                        NULL, ?, ?, CAST(? AS JSON), ?, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.caseId(),
                row.participantRole(), row.documentType(), row.documentId(),
                row.documentNo(), row.currencyCode(), date(row.businessDate()),
                row.snapshotJson(), ts(row.createTime()));
    }

    public void insertMatch(ReconciliationMatchRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_reconciliation_match
                (fid, ftenant_id, forg_id, fcase_id, fmatch_type,
                 fmatched_quantity, fexpected_value_json, factual_value_json,
                 fstatus, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, 'BANK_PAYMENT',
                        0, CAST(? AS JSON), CAST(? AS JSON), ?, ?, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.caseId(),
                row.expectedJson(), row.actualJson(), row.status(), ts(row.createTime()));
    }

    public void insertDifference(ReconciliationDifferenceRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_reconciliation_difference
                (fid, ftenant_id, forg_id, fcase_id, fdifference_code,
                 ffield_code, fexpected_value, factual_value, fseverity,
                 fmessage, fstatus, fcreate_time, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'BLOCKING',
                        ?, 'OPEN', ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.caseId(),
                row.code(), row.field(), row.expected(), row.actual(),
                row.message(), ts(row.createTime()), ts(row.createTime()));
    }

    private String baseSelect() {
        return """
                SELECT fid, ftenant_id, forg_id,
                       fbank_account_id, fbank_transaction_no,
                       ftransaction_date, ftransaction_time, fdirection,
                       fcurrency_code, famount, fcounterparty_name,
                       fcounterparty_account, fpurpose, fsummary,
                       fbank_receipt_no, fsource_channel,
                       fmatch_status, fstatus,
                       fmatched_payment_order_id,
                       freconciliation_batch_id, freconciliation_case_id,
                       fraw_payload_hash, fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_bank_transaction
                """;
    }

    private BankTransactionRow mapRow(ResultSet rs, int n) throws SQLException {
        return new BankTransactionRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fbank_account_id"), rs.getString("fbank_transaction_no"),
                rs.getDate("ftransaction_date").toLocalDate(),
                nullableTime(rs.getTimestamp("ftransaction_time")),
                rs.getString("fdirection"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getString("fcounterparty_name"),
                rs.getString("fcounterparty_account"), rs.getString("fpurpose"),
                rs.getString("fsummary"), rs.getString("fbank_receipt_no"),
                rs.getString("fsource_channel"), rs.getString("fmatch_status"),
                rs.getString("fstatus"),
                rs.getObject("fmatched_payment_order_id", Long.class),
                rs.getObject("freconciliation_batch_id", Long.class),
                rs.getObject("freconciliation_case_id", Long.class),
                rs.getString("fraw_payload_hash"),
                null,
                rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(),
                rs.getInt("fversion")
        );
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime nullableTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record BankTransactionRow(
            Long id,
            String tenantId,
            Long orgId,
            String bankAccountId,
            String bankTransactionNo,
            LocalDate transactionDate,
            LocalDateTime transactionTime,
            String direction,
            String currencyCode,
            BigDecimal amount,
            String counterpartyName,
            String counterpartyAccount,
            String purpose,
            String summary,
            String bankReceiptNo,
            String sourceChannel,
            String matchStatus,
            String status,
            Long matchedPaymentOrderId,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            String rawPayloadHash,
            String rawPayloadJson,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }

    public record ReconciliationBatchRow(
            Long id,
            String tenantId,
            Long orgId,
            String batchNo,
            String requestId,
            Long bankTransactionId,
            String bankTransactionNo,
            String result,
            LocalDateTime startTime,
            LocalDateTime finishTime
    ) {
    }

    public record ReconciliationCaseRow(
            Long id,
            String tenantId,
            Long orgId,
            Long batchId,
            String caseNo,
            String caseKey,
            String result,
            String snapshotJson,
            LocalDateTime createTime
    ) {
    }

    public record ReconciliationParticipantRow(
            Long id,
            String tenantId,
            Long orgId,
            Long caseId,
            String participantRole,
            String documentType,
            String documentId,
            String documentNo,
            String currencyCode,
            LocalDate businessDate,
            String snapshotJson,
            LocalDateTime createTime
    ) {
    }

    public record ReconciliationMatchRow(
            Long id,
            String tenantId,
            Long orgId,
            Long caseId,
            String expectedJson,
            String actualJson,
            String status,
            LocalDateTime createTime
    ) {
    }

    public record ReconciliationDifferenceRow(
            Long id,
            String tenantId,
            Long orgId,
            Long caseId,
            String code,
            String field,
            String expected,
            String actual,
            String message,
            LocalDateTime createTime
    ) {
    }
}
