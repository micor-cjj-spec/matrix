package single.cjj.fi.accounting.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import single.cjj.fi.accounting.integration.BusinessEventEnvelope;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SupplierInvoiceAccountingRepository {

    private final JdbcTemplate jdbc;

    public SupplierInvoiceAccountingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EstimateEntry> findOpenEstimateEntriesForUpdate(
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
                       p.famount AS payable_amount,
                       p.fopen_amount AS payable_open_amount,
                       p.faccounting_event_id AS accounting_event_id,
                       p.fsource_system_code,
                       p.fsource_document_type,
                       p.fsource_document_id,
                       p.fsource_document_no,
                       p.fbusiness_event_id AS estimate_business_event_id,
                       e.fid AS entry_id,
                       e.fline_no,
                       e.fsource_entry_id,
                       e.fpurchase_order_id,
                       e.fpurchase_order_entry_id,
                       e.fmaterial_id,
                       e.fmaterial_code,
                       e.fmaterial_name,
                       e.fquantity,
                       e.funit_price,
                       e.famount,
                       e.fwarehouse_id,
                       e.fproject_id,
                       e.fcost_center_id
                  FROM matrix_fi_ap_payable p
                  JOIN matrix_fi_ap_payable_entry e
                    ON e.fpayable_id = p.fid AND e.fdelete_flag = 0
                 WHERE p.ftenant_id = ?
                   AND p.forg_id = ?
                   AND p.ftype = 'ESTIMATE'
                   AND p.fbusiness_partner_id = ?
                   AND p.fcurrency_code = ?
                   AND p.fstatus = 'OPEN'
                   AND p.fopen_amount > 0
                   AND p.faccounting_status = 'VOUCHER_GENERATED'
                   AND p.fdelete_flag = 0
                   AND e.fpurchase_order_entry_id = ?
                 ORDER BY p.fdate, p.fid, e.fline_no, e.fid
                 FOR UPDATE
                """, (rs, n) -> new EstimateEntry(
                rs.getLong("payable_id"),
                rs.getString("payable_number"),
                rs.getDate("payable_date").toLocalDate(),
                rs.getBigDecimal("payable_amount"),
                rs.getBigDecimal("payable_open_amount"),
                rs.getString("accounting_event_id"),
                rs.getString("fsource_system_code"),
                rs.getString("fsource_document_type"),
                rs.getString("fsource_document_id"),
                rs.getString("fsource_document_no"),
                rs.getString("estimate_business_event_id"),
                rs.getLong("entry_id"),
                rs.getInt("fline_no"),
                rs.getString("fsource_entry_id"),
                rs.getString("fpurchase_order_id"),
                rs.getString("fpurchase_order_entry_id"),
                rs.getString("fmaterial_id"),
                rs.getString("fmaterial_code"),
                rs.getString("fmaterial_name"),
                rs.getBigDecimal("fquantity"),
                rs.getBigDecimal("funit_price"),
                rs.getBigDecimal("famount"),
                rs.getString("fwarehouse_id"),
                rs.getString("fproject_id"),
                rs.getString("fcost_center_id")
        ), tenantId, orgId, businessPartnerId, currencyCode, purchaseOrderEntryId);
    }

    public PayableHeader findPayableForUpdate(Long payableId, String tenantId) {
        List<PayableHeader> values = jdbc.query("""
                SELECT fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                       fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                       fcurrency_code, famount, fopen_amount, fstatus, fapproval_status,
                       faccounting_status, fsource_system_code, fsource_document_type,
                       fsource_document_id, fsource_document_no, fbusiness_event_id,
                       faccounting_event_id, fvoucher_id, fvoucher_number
                  FROM matrix_fi_ap_payable
                 WHERE fid = ? AND ftenant_id = ? AND fdelete_flag = 0
                 FOR UPDATE
                """, (rs, n) -> new PayableHeader(
                rs.getLong("fid"),
                rs.getString("ftenant_id"),
                rs.getLong("forg_id"),
                rs.getString("fnumber"),
                rs.getString("ftype"),
                rs.getDate("fdate").toLocalDate(),
                rs.getString("fbusiness_partner_id"),
                rs.getString("fbusiness_partner_code"),
                rs.getString("fbusiness_partner_name"),
                rs.getString("fcurrency_code"),
                rs.getBigDecimal("famount"),
                rs.getBigDecimal("fopen_amount"),
                rs.getString("fstatus"),
                rs.getString("fapproval_status"),
                rs.getString("faccounting_status"),
                rs.getString("fsource_system_code"),
                rs.getString("fsource_document_type"),
                rs.getString("fsource_document_id"),
                rs.getString("fsource_document_no"),
                rs.getString("fbusiness_event_id"),
                rs.getString("faccounting_event_id"),
                rs.getObject("fvoucher_id", Long.class),
                rs.getString("fvoucher_number")
        ), payableId, tenantId);
        return values.isEmpty() ? null : values.get(0);
    }

    public List<PayableEntry> findPayableEntries(Long payableId) {
        return jdbc.query("""
                SELECT fid, fpayable_id, fline_no, fsource_entry_id,
                       fpurchase_order_id, fpurchase_order_entry_id,
                       fmaterial_id, fmaterial_code, fmaterial_name,
                       fquantity, funit_price, famount,
                       fwarehouse_id, fproject_id, fcost_center_id
                  FROM matrix_fi_ap_payable_entry
                 WHERE fpayable_id = ? AND fdelete_flag = 0
                 ORDER BY fline_no, fid
                """, (rs, n) -> new PayableEntry(
                rs.getLong("fid"),
                rs.getLong("fpayable_id"),
                rs.getInt("fline_no"),
                rs.getString("fsource_entry_id"),
                rs.getString("fpurchase_order_id"),
                rs.getString("fpurchase_order_entry_id"),
                rs.getString("fmaterial_id"),
                rs.getString("fmaterial_code"),
                rs.getString("fmaterial_name"),
                rs.getBigDecimal("fquantity"),
                rs.getBigDecimal("funit_price"),
                rs.getBigDecimal("famount"),
                rs.getString("fwarehouse_id"),
                rs.getString("fproject_id"),
                rs.getString("fcost_center_id")
        ), payableId);
    }

    public OriginalAccountingEvent findOriginalAccountingEvent(String accountingEventId) {
        List<OriginalAccountingEvent> values = jdbc.query("""
                SELECT fid, faccounting_event_id, faccounting_event_type,
                       frule_code, frule_version, fbook_id, fvoucher_id, fvoucher_number
                  FROM matrix_fi_accounting_event
                 WHERE faccounting_event_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, (rs, n) -> new OriginalAccountingEvent(
                rs.getLong("fid"),
                rs.getString("faccounting_event_id"),
                rs.getString("faccounting_event_type"),
                rs.getString("frule_code"),
                rs.getInt("frule_version"),
                rs.getString("fbook_id"),
                rs.getObject("fvoucher_id", Long.class),
                rs.getString("fvoucher_number")
        ), accountingEventId);
        return values.isEmpty() ? null : values.get(0);
    }

    public List<OriginalAccountingLine> findOriginalAccountingLines(Long accountingEventPk) {
        return jdbc.query("""
                SELECT fid, fline_no, fsource_entry_id, fdirection, faccount_key,
                       fresolved_account_code, fsummary, fdebit_amount, fcredit_amount,
                       fcurrency_code, foriginal_amount, frule_entry_id
                  FROM matrix_fi_accounting_event_entry
                 WHERE faccounting_event_id = ?
                 ORDER BY fline_no, fid
                """, (rs, n) -> new OriginalAccountingLine(
                rs.getLong("fid"),
                rs.getInt("fline_no"),
                rs.getString("fsource_entry_id"),
                rs.getString("fdirection"),
                rs.getString("faccount_key"),
                rs.getString("fresolved_account_code"),
                rs.getString("fsummary"),
                rs.getBigDecimal("fdebit_amount"),
                rs.getBigDecimal("fcredit_amount"),
                rs.getString("fcurrency_code"),
                rs.getBigDecimal("foriginal_amount"),
                rs.getObject("frule_entry_id", Long.class)
        ), accountingEventPk);
    }

    public List<OriginalDimension> findOriginalAccountingDimensions(Long accountingEventPk) {
        return jdbc.query("""
                SELECT d.faccounting_event_entry_id, d.fdimension_code,
                       d.fdimension_value_id, d.fdimension_value_code, d.fdimension_value_name
                  FROM matrix_fi_accounting_event_dimension d
                 WHERE d.faccounting_event_id = ?
                 ORDER BY d.faccounting_event_entry_id, d.fid
                """, (rs, n) -> new OriginalDimension(
                rs.getLong("faccounting_event_entry_id"),
                rs.getString("fdimension_code"),
                rs.getString("fdimension_value_id"),
                rs.getString("fdimension_value_code"),
                rs.getString("fdimension_value_name")
        ), accountingEventPk);
    }

    public void insertPayable(
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
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            String businessEventId,
            Long originalPayableId,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_payable
                (fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                 fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, fopen_amount, fsettled_amount,
                 fstatus, fapproval_status, faccounting_status,
                 fsource_system_code, fsource_document_type, fsource_document_id, fsource_document_no,
                 fbusiness_event_id, foriginal_payable_id,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0,
                        'OPEN', 'AUDITED', 'PENDING',
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, tenantId, orgId, number, type, Date.valueOf(date),
                businessPartnerId, businessPartnerCode, businessPartnerName,
                currencyCode, amount, amount,
                sourceSystemCode, sourceDocumentType, sourceDocumentId, sourceDocumentNo,
                businessEventId, originalPayableId,
                operatorId, ts(now), operatorId, ts(now));
    }

    public void insertPayableEntry(
            Long id,
            String tenantId,
            Long orgId,
            Long payableId,
            int lineNo,
            String sourceEntryId,
            String purchaseOrderId,
            String purchaseOrderEntryId,
            String materialId,
            String materialCode,
            String materialName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            String warehouseId,
            String projectId,
            String costCenterId,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_payable_entry
                (fid, ftenant_id, forg_id, fpayable_id, fline_no, fsource_entry_id,
                 fpurchase_order_id, fpurchase_order_entry_id,
                 fmaterial_id, fmaterial_code, fmaterial_name,
                 fquantity, funit_price, famount,
                 fnet_amount, ftax_rate, ftax_amount, fgross_amount,
                 fwarehouse_id, fproject_id, fcost_center_id,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, tenantId, orgId, payableId, lineNo, sourceEntryId,
                purchaseOrderId, purchaseOrderEntryId,
                materialId, materialCode, materialName,
                quantity, unitPrice, amount,
                netAmount, taxRate, taxAmount, grossAmount,
                warehouseId, projectId, costCenterId,
                operatorId, ts(now), operatorId, ts(now));
    }

    public void markEstimateReversed(Long payableId, Long operatorId) {
        int updated = jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET fopen_amount = 0,
                       fstatus = 'REVERSED',
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ? AND ftype = 'ESTIMATE' AND fstatus = 'OPEN' AND fdelete_flag = 0
                """, operatorId, ts(LocalDateTime.now()), payableId);
        if (updated != 1) {
            throw new IllegalStateException("estimate payable state changed: " + payableId);
        }
    }

    public void insertEstimateReversal(
            Long id,
            BusinessEventEnvelope event,
            String supplierInvoiceId,
            String supplierInvoiceNo,
            Long estimatePayableId,
            Long formalPayableId,
            BigDecimal reversalAmount,
            Long residualPayableId,
            BigDecimal residualAmount,
            Long operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_estimate_reversal
                (fid, ftenant_id, forg_id, fbusiness_event_id,
                 fsupplier_invoice_id, fsupplier_invoice_no,
                 festimate_payable_id, fformal_payable_id,
                 freversal_amount, fresidual_payable_id, fresidual_amount,
                 fstatus, fcreate_by, fcreate_time, fmodify_by, fmodify_time,
                 fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROCESSING',
                        ?, ?, ?, ?, 0, 0)
                """,
                id, event.tenantId(), event.orgId(), event.eventId(),
                supplierInvoiceId, supplierInvoiceNo,
                estimatePayableId, formalPayableId,
                reversalAmount, residualPayableId, residualAmount,
                operatorId, ts(now), operatorId, ts(now));
    }

    public void insertEstimateReversalAllocation(
            Long id,
            BusinessEventEnvelope event,
            Long reversalId,
            String supplierInvoiceEntryId,
            Long estimatePayableEntryId,
            BigDecimal quantity,
            BigDecimal amount,
            Long operatorId
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_ap_estimate_reversal_allocation
                (fid, ftenant_id, forg_id, freversal_id, fbusiness_event_id,
                 fsupplier_invoice_entry_id, festimate_payable_entry_id,
                 fmatched_quantity, fmatched_amount,
                 fcreate_by, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id, event.tenantId(), event.orgId(), reversalId, event.eventId(),
                supplierInvoiceEntryId, estimatePayableEntryId,
                quantity, amount, operatorId, ts(LocalDateTime.now()));
    }

    public void completeEstimateReversal(
            Long reversalId,
            String reversalAccountingEventId,
            String residualAccountingEventId,
            Long operatorId
    ) {
        jdbc.update("""
                UPDATE matrix_fi_ap_estimate_reversal
                   SET freversal_accounting_event_id = ?,
                       fresidual_accounting_event_id = ?,
                       fstatus = 'COMPLETED',
                       fmodify_by = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ?
                """,
                reversalAccountingEventId, residualAccountingEventId,
                operatorId, ts(LocalDateTime.now()), reversalId);
    }

    public void insertSnapshotAccountingEvent(
            Long pk,
            String accountingEventId,
            String accountingEventType,
            int sequenceNo,
            BusinessEventEnvelope event,
            String bookId,
            String factsJson,
            String ruleCode,
            int ruleVersion,
            String sourcePayloadHash
    ) {
        LocalDate accountingDate = event.businessDate() == null ? LocalDate.now() : event.businessDate();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_event
                (fid, ftenant_id, forg_id, faccounting_org_id,
                 faccounting_event_id, faccounting_event_type,
                 fbusiness_event_id, fbusiness_event_type, fsequence_no, fbook_id,
                 fsource_system_code, fsource_document_type, fsource_document_id, fsource_document_no,
                 fbusiness_date, faccounting_date,
                 fcorrelation_id, fcausation_id,
                 frule_code, frule_version, fsource_payload_hash,
                 fstatus, fstage, ffacts_json,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'READY', 'VOUCHER_GENERATION', CAST(? AS JSON),
                        ?, ?, ?, ?, 0, 0)
                """,
                pk, event.tenantId(), event.orgId(), event.orgId(),
                accountingEventId, accountingEventType,
                event.eventId(), event.eventType(), sequenceNo, bookId,
                event.sourceSystemCode(), event.sourceDocumentType(), event.sourceDocumentId(), event.sourceDocumentNo(),
                event.businessDate() == null ? null : Date.valueOf(event.businessDate()), Date.valueOf(accountingDate),
                event.correlationId(), event.eventId(),
                ruleCode, ruleVersion, sourcePayloadHash,
                factsJson,
                event.operatorId(), ts(now), event.operatorId(), ts(now));
    }

    public void updatePayableAccounting(
            Long payableId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
        jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET faccounting_event_id = ?,
                       faccounting_status = 'VOUCHER_GENERATED',
                       fvoucher_id = ?,
                       fvoucher_number = ?,
                       fmodify_time = ?,
                       fversion = fversion + 1
                 WHERE fid = ?
                """,
                accountingEventId, voucherId, voucherNumber, ts(LocalDateTime.now()), payableId);
    }

    public record EstimateEntry(
            Long payableId,
            String payableNumber,
            LocalDate payableDate,
            BigDecimal payableAmount,
            BigDecimal payableOpenAmount,
            String accountingEventId,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            String estimateBusinessEventId,
            Long entryId,
            int lineNo,
            String sourceEntryId,
            String purchaseOrderId,
            String purchaseOrderEntryId,
            String materialId,
            String materialCode,
            String materialName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String warehouseId,
            String projectId,
            String costCenterId
    ) {
    }

    public record PayableHeader(
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
            String status,
            String approvalStatus,
            String accountingStatus,
            String sourceSystemCode,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceDocumentNo,
            String businessEventId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
    }

    public record PayableEntry(
            Long id,
            Long payableId,
            int lineNo,
            String sourceEntryId,
            String purchaseOrderId,
            String purchaseOrderEntryId,
            String materialId,
            String materialCode,
            String materialName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String warehouseId,
            String projectId,
            String costCenterId
    ) {
    }

    public record OriginalAccountingEvent(
            Long pk,
            String accountingEventId,
            String accountingEventType,
            String ruleCode,
            int ruleVersion,
            String bookId,
            Long voucherId,
            String voucherNumber
    ) {
    }

    public record OriginalAccountingLine(
            Long id,
            int lineNo,
            String sourceEntryId,
            String direction,
            String accountKey,
            String accountCode,
            String summary,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String currencyCode,
            BigDecimal originalAmount,
            Long ruleEntryId
    ) {
    }

    public record OriginalDimension(
            Long accountingEntryId,
            String code,
            String valueId,
            String valueCode,
            String valueName
    ) {
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
