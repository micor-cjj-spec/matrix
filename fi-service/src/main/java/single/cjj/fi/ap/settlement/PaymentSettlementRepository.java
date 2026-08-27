package single.cjj.fi.ap.settlement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PaymentSettlementRepository {

    private final JdbcTemplate jdbc;

    public PaymentSettlementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SettlementRow findByPaymentOrder(Long paymentOrderId, String tenantId) {
        List<SettlementRow> rows = jdbc.query(baseSettlementSelect() + """
                 WHERE fpayment_order_id = ?
                   AND ftenant_id = ?
                   AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapSettlement, paymentOrderId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public SettlementRow findSettlement(Long settlementId, String tenantId) {
        List<SettlementRow> rows = jdbc.query(baseSettlementSelect() + """
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, this::mapSettlement, settlementId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PaymentOrderRow lockPaymentOrder(Long paymentOrderId, String tenantId) {
        List<PaymentOrderRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fpayer_bank_account_id,
                       fstatus, fapproval_status, fchannel_status, fbank_match_status,
                       freconciliation_batch_id, freconciliation_case_id,
                       fsettlement_id, fversion
                  FROM matrix_fi_payment_order
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapPaymentOrder, paymentOrderId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BankTransactionRow lockMatchedBankTransaction(
            Long paymentOrderId,
            String tenantId
    ) {
        List<BankTransactionRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id,
                       fbank_account_id, fbank_transaction_no,
                       ftransaction_date, ftransaction_time,
                       fdirection, fcurrency_code, famount,
                       fmatch_status, fstatus,
                       fmatched_payment_order_id,
                       freconciliation_batch_id, freconciliation_case_id,
                       fversion
                  FROM matrix_fi_bank_transaction
                 WHERE fmatched_payment_order_id = ?
                   AND ftenant_id = ?
                   AND fmatch_status = 'MATCHED'
                   AND fstatus = 'CONFIRMED'
                   AND fdelete_flag = 0
                 FOR UPDATE
                """, this::mapBankTransaction, paymentOrderId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<OrderAllocationRow> lockOrderAllocations(
            Long paymentOrderId,
            String tenantId
    ) {
        return jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fpayment_order_id,
                       fpayment_application_id, famount, fstatus, fversion
                  FROM matrix_fi_payment_order_allocation
                 WHERE fpayment_order_id = ?
                   AND ftenant_id = ?
                   AND fdelete_flag = 0
                 ORDER BY fpayment_application_id, fid
                 FOR UPDATE
                """, (rs, n) -> new OrderAllocationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fpayment_order_id"), rs.getLong("fpayment_application_id"),
                rs.getBigDecimal("famount"), rs.getString("fstatus"), rs.getInt("fversion")
        ), paymentOrderId, tenantId);
    }

    public PaymentApplicationRow lockApplication(
            Long applicationId,
            String tenantId
    ) {
        List<PaymentApplicationRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber,
                       famount, fordered_amount,
                       fbusiness_partner_id, fcurrency_code,
                       fstatus, fapproval_status, fexecution_status, fversion
                  FROM matrix_fi_payment_application
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, (rs, n) -> new PaymentApplicationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getBigDecimal("famount"),
                rs.getBigDecimal("fordered_amount"),
                rs.getString("fbusiness_partner_id"), rs.getString("fcurrency_code"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("fexecution_status"), rs.getInt("fversion")
        ), applicationId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ApplicationAllocationRow> lockAvailableApplicationAllocations(
            Long applicationId,
            String tenantId
    ) {
        return jdbc.query("""
                SELECT a.fid, a.ftenant_id, a.forg_id,
                       a.fpayment_application_id, a.fpayable_id,
                       p.fnumber AS payable_number,
                       a.fapplied_amount, a.freserved_amount, a.fconsumed_amount,
                       a.fstatus, a.fversion
                  FROM matrix_fi_payment_application_allocation a
                  JOIN matrix_fi_ap_payable p
                    ON p.fid = a.fpayable_id
                   AND p.fdelete_flag = 0
                 WHERE a.fpayment_application_id = ?
                   AND a.ftenant_id = ?
                   AND a.fdelete_flag = 0
                   AND a.fstatus = 'RESERVED'
                   AND a.freserved_amount > a.fconsumed_amount
                 ORDER BY a.fid
                 FOR UPDATE
                """, (rs, n) -> new ApplicationAllocationRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fpayment_application_id"), rs.getLong("fpayable_id"),
                rs.getString("payable_number"),
                rs.getBigDecimal("fapplied_amount"),
                rs.getBigDecimal("freserved_amount"),
                rs.getBigDecimal("fconsumed_amount"),
                rs.getString("fstatus"), rs.getInt("fversion")
        ), applicationId, tenantId);
    }

    public PayableRow lockPayable(Long payableId, String tenantId) {
        List<PayableRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, ftype,
                       fbusiness_partner_id, fcurrency_code,
                       famount, fopen_amount, fsettled_amount, freserved_amount,
                       fstatus, fapproval_status, faccounting_status, fversion
                  FROM matrix_fi_ap_payable
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, (rs, n) -> new PayableRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getString("ftype"),
                rs.getString("fbusiness_partner_id"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getBigDecimal("fopen_amount"),
                rs.getBigDecimal("fsettled_amount"), rs.getBigDecimal("freserved_amount"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("faccounting_status"), rs.getInt("fversion")
        ), payableId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertSettlement(SettlementRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_ap_settlement
                (fid, ftenant_id, forg_id, fnumber,
                 fpayment_order_id, fbank_transaction_id,
                 fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, fstatus, fsettlement_date,
                 fbusiness_event_id, faccounting_event_id, fvoucher_id, fvoucher_number,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'COMPLETED', ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.number(),
                row.paymentOrderId(), row.bankTransactionId(),
                row.businessPartnerId(), row.businessPartnerCode(), row.businessPartnerName(),
                row.currencyCode(), row.amount(), Date.valueOf(row.settlementDate()),
                row.businessEventId(), row.accountingEventId(), row.voucherId(), row.voucherNumber(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public void insertSettlementEntry(SettlementEntryRow row) {
        jdbc.update("""
                INSERT INTO matrix_fi_ap_settlement_entry
                (fid, ftenant_id, forg_id, fsettlement_id,
                 fpayable_id, fpayable_number,
                 fpayment_application_id, fpayment_application_allocation_id,
                 fpayment_order_allocation_id,
                 fsettled_amount,
                 foriginal_open_amount, fremaining_open_amount,
                 foriginal_reserved_amount, fremaining_reserved_amount,
                 fstatus, fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'COMPLETED', ?, ?, ?, ?, 0, 0)
                """,
                row.id(), row.tenantId(), row.orgId(), row.settlementId(),
                row.payableId(), row.payableNumber(),
                row.paymentApplicationId(), row.paymentApplicationAllocationId(),
                row.paymentOrderAllocationId(), row.settledAmount(),
                row.originalOpenAmount(), row.remainingOpenAmount(),
                row.originalReservedAmount(), row.remainingReservedAmount(),
                row.createBy(), ts(row.createTime()), row.createBy(), ts(row.createTime()));
    }

    public List<SettlementEntryRow> findSettlementEntries(
            Long settlementId,
            String tenantId
    ) {
        return jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fsettlement_id,
                       fpayable_id, fpayable_number,
                       fpayment_application_id, fpayment_application_allocation_id,
                       fpayment_order_allocation_id, fsettled_amount,
                       foriginal_open_amount, fremaining_open_amount,
                       foriginal_reserved_amount, fremaining_reserved_amount,
                       fstatus, fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_ap_settlement_entry
                 WHERE fsettlement_id = ?
                   AND ftenant_id = ?
                   AND fdelete_flag = 0
                 ORDER BY fid
                """, (rs, n) -> new SettlementEntryRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getLong("fsettlement_id"), rs.getLong("fpayable_id"),
                rs.getString("fpayable_number"), rs.getLong("fpayment_application_id"),
                rs.getLong("fpayment_application_allocation_id"),
                rs.getLong("fpayment_order_allocation_id"),
                rs.getBigDecimal("fsettled_amount"),
                rs.getBigDecimal("foriginal_open_amount"),
                rs.getBigDecimal("fremaining_open_amount"),
                rs.getBigDecimal("foriginal_reserved_amount"),
                rs.getBigDecimal("fremaining_reserved_amount"),
                rs.getString("fstatus"), rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(), rs.getInt("fversion")
        ), settlementId, tenantId);
    }

    public void updatePayableBalances(
            Long payableId,
            String tenantId,
            BigDecimal settledAmount,
            BigDecimal openAmount,
            BigDecimal reservedAmount,
            String status,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET fsettled_amount = ?,
                       fopen_amount = ?,
                       freserved_amount = ?,
                       fstatus = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                settledAmount, openAmount, reservedAmount, status,
                operatorId, ts(LocalDateTime.now()), payableId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("payable settlement update failed: " + payableId);
        }
    }

    public void updateApplicationAllocationConsumed(
            Long allocationId,
            String tenantId,
            BigDecimal consumedAmount,
            String status,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_application_allocation
                   SET fconsumed_amount = ?,
                       fstatus = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                consumedAmount, status, operatorId, ts(LocalDateTime.now()),
                allocationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "payment application allocation consume failed: " + allocationId);
        }
    }

    public void markOrderAllocationConsumed(
            Long allocationId,
            String tenantId,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order_allocation
                   SET fstatus = 'CONSUMED',
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ?
                   AND fstatus = 'ORDERED'
                   AND fdelete_flag = 0
                """,
                operatorId, ts(LocalDateTime.now()), allocationId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "payment order allocation consume failed: " + allocationId);
        }
    }

    public void markPaymentOrderPaid(
            Long paymentOrderId,
            String tenantId,
            Long settlementId,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        int updated = jdbc.update("""
                UPDATE matrix_fi_payment_order
                   SET fstatus = 'PAID',
                       fchannel_status = 'CONFIRMED',
                       fpaid_time = ?,
                       fsettlement_id = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ?
                   AND fbank_match_status = 'MATCHED'
                   AND fstatus IN ('AUDITED','PAYING')
                   AND fdelete_flag = 0
                """,
                ts(now), settlementId, operatorId, ts(now),
                paymentOrderId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "payment order paid transition failed: " + paymentOrderId);
        }
    }

    public void updateSettlementBusinessEvent(
            Long settlementId,
            String tenantId,
            String businessEventId,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_settlement
                   SET fbusiness_event_id = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                businessEventId, operatorId, ts(LocalDateTime.now()),
                settlementId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "settlement event update failed: " + settlementId);
        }
    }

    public void updateSettlementAccounting(
            Long settlementId,
            String tenantId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_settlement
                   SET faccounting_event_id = ?,
                       fvoucher_id = ?,
                       fvoucher_number = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                """,
                accountingEventId, voucherId, voucherNumber,
                ts(LocalDateTime.now()), settlementId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException(
                    "settlement accounting update failed: " + settlementId);
        }
    }

    public void insertPaymentAccountingTrace(
            Long id,
            String tenantId,
            Long orgId,
            String businessEventId,
            String businessEventType,
            String sourceDocumentId,
            String sourceDocumentNo,
            Long settlementId,
            Long paymentOrderId,
            Long bankTransactionId,
            String accountingEventId,
            String ruleCode,
            int ruleVersion,
            Long voucherId,
            String voucherNumber
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_trace
                (fid, ftenant_id, forg_id,
                 fbusiness_event_id, fbusiness_event_type,
                 fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_document_no,
                 fpayable_id, fsettlement_id, fpayment_order_id, fbank_transaction_id,
                 faccounting_event_id, frule_code, frule_version,
                 fvoucher_id, fvoucher_number, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?,
                        'MATRIX', 'FI_PAYMENT_ORDER', ?, ?,
                        NULL, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, 0)
                """,
                id, tenantId, orgId, businessEventId, businessEventType,
                sourceDocumentId, sourceDocumentNo,
                settlementId, paymentOrderId, bankTransactionId,
                accountingEventId, ruleCode, ruleVersion,
                voucherId, voucherNumber, ts(LocalDateTime.now()));
    }

    private String baseSettlementSelect() {
        return """
                SELECT fid, ftenant_id, forg_id, fnumber,
                       fpayment_order_id, fbank_transaction_id,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fstatus, fsettlement_date,
                       fbusiness_event_id, faccounting_event_id,
                       fvoucher_id, fvoucher_number,
                       fcreate_by, fcreate_time, fversion
                  FROM matrix_fi_ap_settlement
                """;
    }

    private SettlementRow mapSettlement(ResultSet rs, int n) throws SQLException {
        return new SettlementRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getLong("fpayment_order_id"),
                rs.getLong("fbank_transaction_id"),
                rs.getString("fbusiness_partner_id"),
                rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"),
                rs.getString("fcurrency_code"), rs.getBigDecimal("famount"),
                rs.getString("fstatus"), rs.getDate("fsettlement_date").toLocalDate(),
                rs.getString("fbusiness_event_id"), rs.getString("faccounting_event_id"),
                rs.getObject("fvoucher_id", Long.class), rs.getString("fvoucher_number"),
                rs.getObject("fcreate_by", Long.class),
                rs.getTimestamp("fcreate_time").toLocalDateTime(), rs.getInt("fversion")
        );
    }

    private PaymentOrderRow mapPaymentOrder(ResultSet rs, int n) throws SQLException {
        return new PaymentOrderRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fnumber"), rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"),
                rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"),
                rs.getString("fcurrency_code"), rs.getBigDecimal("famount"),
                rs.getString("fpayer_bank_account_id"),
                rs.getString("fstatus"), rs.getString("fapproval_status"),
                rs.getString("fchannel_status"), rs.getString("fbank_match_status"),
                rs.getObject("freconciliation_batch_id", Long.class),
                rs.getObject("freconciliation_case_id", Long.class),
                rs.getObject("fsettlement_id", Long.class), rs.getInt("fversion")
        );
    }

    private BankTransactionRow mapBankTransaction(ResultSet rs, int n) throws SQLException {
        return new BankTransactionRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fbank_account_id"), rs.getString("fbank_transaction_no"),
                rs.getDate("ftransaction_date").toLocalDate(),
                nullableTime(rs.getTimestamp("ftransaction_time")),
                rs.getString("fdirection"), rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"), rs.getString("fmatch_status"),
                rs.getString("fstatus"), rs.getObject("fmatched_payment_order_id", Long.class),
                rs.getObject("freconciliation_batch_id", Long.class),
                rs.getObject("freconciliation_case_id", Long.class), rs.getInt("fversion")
        );
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime nullableTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record SettlementRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            Long paymentOrderId,
            Long bankTransactionId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            String status,
            LocalDate settlementDate,
            String businessEventId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber,
            Long createBy,
            LocalDateTime createTime,
            Integer version
    ) {
    }

    public record SettlementEntryRow(
            Long id,
            String tenantId,
            Long orgId,
            Long settlementId,
            Long payableId,
            String payableNumber,
            Long paymentApplicationId,
            Long paymentApplicationAllocationId,
            Long paymentOrderAllocationId,
            BigDecimal settledAmount,
            BigDecimal originalOpenAmount,
            BigDecimal remainingOpenAmount,
            BigDecimal originalReservedAmount,
            BigDecimal remainingReservedAmount,
            String status,
            Long createBy,
            LocalDateTime createTime,
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
            String payerBankAccountId,
            String status,
            String approvalStatus,
            String channelStatus,
            String bankMatchStatus,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            Long settlementId,
            Integer version
    ) {
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
            String matchStatus,
            String status,
            Long matchedPaymentOrderId,
            Long reconciliationBatchId,
            Long reconciliationCaseId,
            Integer version
    ) {
    }

    public record OrderAllocationRow(
            Long id,
            String tenantId,
            Long orgId,
            Long paymentOrderId,
            Long paymentApplicationId,
            BigDecimal amount,
            String status,
            Integer version
    ) {
    }

    public record PaymentApplicationRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            BigDecimal amount,
            BigDecimal orderedAmount,
            String businessPartnerId,
            String currencyCode,
            String status,
            String approvalStatus,
            String executionStatus,
            Integer version
    ) {
    }

    public record ApplicationAllocationRow(
            Long id,
            String tenantId,
            Long orgId,
            Long paymentApplicationId,
            Long payableId,
            String payableNumber,
            BigDecimal appliedAmount,
            BigDecimal reservedAmount,
            BigDecimal consumedAmount,
            String status,
            Integer version
    ) {
    }

    public record PayableRow(
            Long id,
            String tenantId,
            Long orgId,
            String number,
            String type,
            String businessPartnerId,
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
}
