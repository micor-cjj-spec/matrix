package single.cjj.fi.ap.deduction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PurchaseDeductionAccountingRepository {

    private final JdbcTemplate jdbc;

    public PurchaseDeductionAccountingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PayableCandidate> findFormalPayableCandidatesForUpdate(
            String tenantId,
            Long orgId,
            String businessPartnerId,
            String currencyCode,
            String purchaseOrderEntryId
    ) {
        return jdbc.query("""
                SELECT p.fid AS payable_id,
                       p.fnumber AS payable_number,
                       p.fdate AS payable_date,
                       p.fopen_amount,
                       p.freserved_amount,
                       p.fstatus,
                       e.fid AS payable_entry_id,
                       COALESCE(e.fgross_amount, e.famount) AS line_amount,
                       COALESCE((
                           SELECT SUM(a.famount)
                             FROM matrix_fi_ap_deduction_allocation a
                            WHERE a.fpayable_entry_id = e.fid
                              AND a.fdelete_flag = 0
                       ), 0) AS line_deducted_amount
                  FROM matrix_fi_ap_payable p
                  JOIN matrix_fi_ap_payable_entry e
                    ON e.fpayable_id = p.fid
                   AND e.fdelete_flag = 0
                 WHERE p.ftenant_id = ?
                   AND p.forg_id = ?
                   AND p.ftype = 'FORMAL'
                   AND p.fbusiness_partner_id = ?
                   AND p.fcurrency_code = ?
                   AND p.fstatus IN ('OPEN','PARTIAL_SETTLED')
                   AND p.fapproval_status = 'AUDITED'
                   AND p.faccounting_status IN ('VOUCHER_GENERATED','POSTED')
                   AND p.fopen_amount > p.freserved_amount
                   AND p.fdelete_flag = 0
                   AND e.fpurchase_order_entry_id = ?
                 ORDER BY p.fdate, p.fid, e.fline_no, e.fid
                 FOR UPDATE
                """, (rs, n) -> new PayableCandidate(
                rs.getLong("payable_id"),
                rs.getString("payable_number"),
                rs.getDate("payable_date").toLocalDate(),
                rs.getBigDecimal("fopen_amount"),
                rs.getBigDecimal("freserved_amount"),
                rs.getString("fstatus"),
                rs.getLong("payable_entry_id"),
                rs.getBigDecimal("line_amount"),
                rs.getBigDecimal("line_deducted_amount")
        ), tenantId, orgId, businessPartnerId, currencyCode, purchaseOrderEntryId);
    }

    public void insertDeduction(
            Long id,
            String tenantId,
            Long orgId,
            String businessEventId,
            String erpDeductionId,
            String erpDeductionNo,
            String supplierClaimId,
            String purchaseOrderId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_deduction
                (fid, ftenant_id, forg_id, fbusiness_event_id,
                 ferp_deduction_id, ferp_deduction_no, fsupplier_claim_id,
                 fpurchase_order_id, fbusiness_partner_id,
                 fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, fstatus,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROCESSING',
                        ?, ?, ?, ?, 0, 0)
                """,
                id, tenantId, orgId, businessEventId,
                erpDeductionId, erpDeductionNo, supplierClaimId,
                purchaseOrderId, businessPartnerId,
                businessPartnerCode, businessPartnerName,
                currencyCode, amount,
                operatorId, ts(now), operatorId, ts(now));
    }

    public void insertAllocation(
            Long id,
            String tenantId,
            Long orgId,
            Long deductionId,
            String businessEventId,
            String erpDeductionEntryId,
            String purchaseOrderEntryId,
            Long payableId,
            Long payableEntryId,
            BigDecimal amount,
            BigDecimal originalOpen,
            BigDecimal remainingOpen,
            Long operatorId
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_ap_deduction_allocation
                (fid, ftenant_id, forg_id, fdeduction_id, fbusiness_event_id,
                 ferp_deduction_entry_id, fpurchase_order_entry_id,
                 fpayable_id, fpayable_entry_id, famount,
                 foriginal_open_amount, fremaining_open_amount,
                 fcreate_by, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id, tenantId, orgId, deductionId, businessEventId,
                erpDeductionEntryId, purchaseOrderEntryId,
                payableId, payableEntryId, amount,
                originalOpen, remainingOpen,
                operatorId, ts(LocalDateTime.now()));
    }

    public void applyPayableDeduction(
            Long payableId,
            String tenantId,
            BigDecimal deductionAmount,
            BigDecimal remainingOpen,
            String nextStatus,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET fopen_amount = ?,
                       fdeducted_amount = fdeducted_amount + ?,
                       fstatus = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ?
                   AND ftenant_id = ?
                   AND ftype = 'FORMAL'
                   AND fdelete_flag = 0
                """,
                remainingOpen, deductionAmount, nextStatus,
                operatorId, ts(LocalDateTime.now()), payableId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("formal payable deduction update failed: " + payableId);
        }
    }

    public void completeDeduction(
            Long deductionId,
            String tenantId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber,
            Long operatorId
    ) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_deduction
                   SET fstatus = 'COMPLETED',
                       faccounting_event_id = ?,
                       fvoucher_id = ?,
                       fvoucher_number = ?,
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ?
                   AND ftenant_id = ?
                   AND fdelete_flag = 0
                """,
                accountingEventId, voucherId, voucherNumber,
                operatorId, ts(LocalDateTime.now()),
                deductionId, tenantId);
        if (updated != 1) {
            throw new IllegalStateException("AP deduction completion failed: " + deductionId);
        }
    }

    public void insertAccountingTrace(
            Long id,
            String tenantId,
            Long orgId,
            String businessEventId,
            String businessEventType,
            String sourceDocumentId,
            String sourceDocumentNo,
            Long payableId,
            Long deductionId,
            String accountingEventId,
            String ruleCode,
            int ruleVersion,
            Long voucherId,
            String voucherNumber
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_trace
                (fid, ftenant_id, forg_id, fbusiness_event_id, fbusiness_event_type,
                 fsource_system_code, fsource_document_type,
                 fsource_document_id, fsource_document_no,
                 fpayable_id, fdeduction_id, faccounting_event_id,
                 frule_code, frule_version, fvoucher_id, fvoucher_number,
                 fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, 'MATRIX', 'ERP_PURCHASE_DEDUCTION',
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id, tenantId, orgId, businessEventId, businessEventType,
                sourceDocumentId, sourceDocumentNo,
                payableId, deductionId, accountingEventId,
                ruleCode, ruleVersion, voucherId, voucherNumber,
                ts(LocalDateTime.now()));
    }

    public DeductionRow findByBusinessEvent(String tenantId, String eventId) {
        List<DeductionRow> rows = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fbusiness_event_id,
                       ferp_deduction_id, ferp_deduction_no, famount,
                       fstatus, faccounting_event_id, fvoucher_id, fvoucher_number
                  FROM matrix_fi_ap_deduction
                 WHERE ftenant_id = ? AND fbusiness_event_id = ?
                   AND fdelete_flag = 0
                 LIMIT 1
                """, (rs, n) -> new DeductionRow(
                rs.getLong("fid"), rs.getString("ftenant_id"), rs.getLong("forg_id"),
                rs.getString("fbusiness_event_id"), rs.getString("ferp_deduction_id"),
                rs.getString("ferp_deduction_no"), rs.getBigDecimal("famount"),
                rs.getString("fstatus"), rs.getString("faccounting_event_id"),
                rs.getObject("fvoucher_id", Long.class), rs.getString("fvoucher_number")
        ), tenantId, eventId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    public record PayableCandidate(
            Long payableId,
            String payableNumber,
            LocalDate payableDate,
            BigDecimal openAmount,
            BigDecimal reservedAmount,
            String status,
            Long payableEntryId,
            BigDecimal lineAmount,
            BigDecimal lineDeductedAmount
    ) {}

    public record DeductionRow(
            Long id,
            String tenantId,
            Long orgId,
            String businessEventId,
            String erpDeductionId,
            String erpDeductionNo,
            BigDecimal amount,
            String status,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {}
}
