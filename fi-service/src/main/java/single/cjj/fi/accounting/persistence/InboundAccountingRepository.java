package single.cjj.fi.accounting.persistence;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import single.cjj.fi.accounting.integration.BusinessEventEnvelope;
import single.cjj.fi.accounting.model.AccountingModels.AccountMappingCandidate;
import single.cjj.fi.accounting.model.AccountingModels.AccountingLine;
import single.cjj.fi.accounting.model.AccountingModels.ProcessingResult;
import single.cjj.fi.accounting.model.AccountingModels.RuleDimension;
import single.cjj.fi.accounting.model.AccountingModels.RuleEntry;
import single.cjj.fi.accounting.model.AccountingModels.RuleHeader;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class InboundAccountingRepository {

    private final JdbcTemplate jdbc;

    public InboundAccountingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String findInboxStatus(String consumerCode, String eventId) {
        try {
            return jdbc.queryForObject("""
                    SELECT fstatus FROM matrix_fi_inbox_event
                     WHERE fconsumer_code = ? AND fevent_id = ? AND fdelete_flag = 0
                    """, String.class, consumerCode, eventId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    public ProcessingResult findInboxResult(String consumerCode, String eventId) {
        List<ProcessingResult> rows = jdbc.query("""
                SELECT fpayable_id, faccounting_event_id, fvoucher_id, fvoucher_number
                  FROM matrix_fi_inbox_event
                 WHERE fconsumer_code = ? AND fevent_id = ? AND fdelete_flag = 0
                """, (rs, rowNum) -> new ProcessingResult(
                true,
                rs.getObject("fpayable_id", Long.class),
                rs.getString("faccounting_event_id"),
                rs.getObject("fvoucher_id", Long.class),
                rs.getString("fvoucher_number")
        ), consumerCode, eventId);
        return rows.isEmpty() ? new ProcessingResult(true, null, null, null, null) : rows.get(0);
    }

    public int insertInbox(Long id, String consumerCode, BusinessEventEnvelope event) {
        LocalDateTime now = LocalDateTime.now();
        return jdbc.update("""
                INSERT IGNORE INTO matrix_fi_inbox_event
                (fid, ftenant_id, forg_id, fconsumer_code, fevent_id, fevent_type, fevent_version,
                 fproducer_service, fsource_document_type, fsource_document_id, fpayload_json,
                 fstatus, freceived_time, fcreate_time, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON),
                        'PROCESSING', ?, ?, ?, 0, 0)
                """,
                id, event.tenantId(), event.orgId(), consumerCode, event.eventId(), event.eventType(),
                event.eventVersion(), event.producerService(), event.sourceDocumentType(), event.sourceDocumentId(),
                event.rawJson(), ts(now), ts(now), ts(now));
    }

    public void resetInboxProcessing(String consumerCode, String eventId) {
        jdbc.update("""
                UPDATE matrix_fi_inbox_event
                   SET fstatus = 'PROCESSING', ferror_message = NULL, fmodify_time = ?, fversion = fversion + 1
                 WHERE fconsumer_code = ? AND fevent_id = ? AND fstatus = 'FAILED'
                """, ts(LocalDateTime.now()), consumerCode, eventId);
    }

    public void markInboxProcessed(
            String consumerCode,
            String eventId,
            Long payableId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE matrix_fi_inbox_event
                   SET fstatus = 'PROCESSED', fprocessed_time = ?, fpayable_id = ?,
                       faccounting_event_id = ?, fvoucher_id = ?, fvoucher_number = ?,
                       ferror_message = NULL, fmodify_time = ?, fversion = fversion + 1
                 WHERE fconsumer_code = ? AND fevent_id = ?
                """, ts(now), payableId, accountingEventId, voucherId, voucherNumber, ts(now), consumerCode, eventId);
    }

    public void recordInboxFailure(
            Long id,
            String consumerCode,
            String eventId,
            String tenantId,
            Long orgId,
            String eventType,
            String rawJson,
            String error
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_inbox_event
                (fid, ftenant_id, forg_id, fconsumer_code, fevent_id, fevent_type, fevent_version,
                 fproducer_service, fpayload_json, fstatus, ferror_message,
                 freceived_time, fcreate_time, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, 1, 'erp-service', CAST(? AS JSON), 'FAILED', ?, ?, ?, ?, 0, 0)
                ON DUPLICATE KEY UPDATE
                    fstatus = IF(fstatus = 'PROCESSED', fstatus, 'FAILED'),
                    ferror_message = IF(fstatus = 'PROCESSED', ferror_message, VALUES(ferror_message)),
                    fmodify_time = VALUES(fmodify_time),
                    fversion = fversion + 1
                """, id, tenantId, orgId, consumerCode, eventId, eventType,
                rawJson == null || rawJson.isBlank() ? "{}" : rawJson, truncate(error, 1000),
                ts(now), ts(now), ts(now));
    }

    public Long findPayableIdByEvent(String tenantId, String businessEventId) {
        List<Long> rows = jdbc.query("""
                SELECT fid FROM matrix_fi_ap_payable
                 WHERE ftenant_id = ? AND fbusiness_event_id = ? AND fdelete_flag = 0
                """, (rs, n) -> rs.getLong(1), tenantId, businessEventId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertPayable(
            Long id,
            BusinessEventEnvelope event,
            String number,
            String partnerId,
            String partnerCode,
            String partnerName,
            String currencyCode,
            BigDecimal amount
    ) {
        LocalDate date = event.businessDate() == null ? LocalDate.now() : event.businessDate();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_payable
                (fid, ftenant_id, forg_id, fnumber, ftype, fdate,
                 fbusiness_partner_id, fbusiness_partner_code, fbusiness_partner_name,
                 fcurrency_code, famount, fopen_amount, fsettled_amount,
                 fstatus, fapproval_status, faccounting_status,
                 fsource_system_code, fsource_document_type, fsource_document_id, fsource_document_no,
                 fbusiness_event_id, fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, 'ESTIMATE', ?, ?, ?, ?, ?, ?, ?, 0,
                        'OPEN', 'AUDITED', 'PENDING', ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, event.tenantId(), event.orgId(), number, Date.valueOf(date),
                partnerId, partnerCode, partnerName, currencyCode, amount, amount,
                event.sourceSystemCode(), event.sourceDocumentType(), event.sourceDocumentId(), event.sourceDocumentNo(),
                event.eventId(), event.operatorId(), ts(now), event.operatorId(), ts(now));
    }

    public void insertPayableEntry(
            Long id, Long payableId, BusinessEventEnvelope event, int lineNo,
            String inboundEntryId, String purchaseOrderId, String purchaseOrderEntryId,
            String materialId, String materialCode, String materialName,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount,
            String warehouseId, String projectId, String costCenterId
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_ap_payable_entry
                (fid, ftenant_id, forg_id, fpayable_id, fline_no,
                 fsource_entry_id, fpurchase_order_id, fpurchase_order_entry_id,
                 fmaterial_id, fmaterial_code, fmaterial_name,
                 fquantity, funit_price, famount, fwarehouse_id, fproject_id, fcost_center_id,
                 fcreate_by, fcreate_time, fmodify_by, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """,
                id, event.tenantId(), event.orgId(), payableId, lineNo,
                inboundEntryId, purchaseOrderId, purchaseOrderEntryId, materialId, materialCode, materialName,
                quantity, unitPrice, amount, warehouseId, projectId, costCenterId,
                event.operatorId(), ts(now), event.operatorId(), ts(now));
    }

    public void insertAccountingEvent(
            Long id, String accountingEventId, String accountingEventType,
            BusinessEventEnvelope event, String bookId, String factsJson
    ) {
        LocalDate accountingDate = event.businessDate() == null ? LocalDate.now() : event.businessDate();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_event
                (fid, ftenant_id, forg_id, faccounting_org_id, faccounting_event_id, faccounting_event_type,
                 fbusiness_event_id, fbusiness_event_type, fsequence_no, fbook_id,
                 fsource_system_code, fsource_document_type, fsource_document_id, fsource_document_no,
                 fbusiness_date, faccounting_date, fcorrelation_id, fcausation_id,
                 fstatus, fstage, ffacts_json, fcreate_time, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        'PROCESSING', 'VALIDATION', CAST(? AS JSON), ?, ?, 0, 0)
                """,
                id, event.tenantId(), event.orgId(), event.orgId(), accountingEventId, accountingEventType,
                event.eventId(), event.eventType(), bookId,
                event.sourceSystemCode(), event.sourceDocumentType(), event.sourceDocumentId(), event.sourceDocumentNo(),
                event.businessDate() == null ? null : Date.valueOf(event.businessDate()), Date.valueOf(accountingDate),
                event.correlationId(), event.causationId(), factsJson, ts(now), ts(now));
    }

    public void markAccountingReady(
            Long id, RuleHeader rule, String sourcePayloadHash
    ) {
        jdbc.update("""
                UPDATE matrix_fi_accounting_event
                   SET frule_code = ?, frule_version = ?, fstatus = 'READY', fstage = 'VOUCHER_GENERATION',
                       fsource_payload_hash = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ?
                """, rule.ruleCode(), rule.versionNo(), sourcePayloadHash, ts(LocalDateTime.now()), id);
    }

    public void insertAccountingEntry(Long id, Long accountingEventPk, AccountingLine line) {
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_event_entry
                (fid, faccounting_event_id, fline_no, fsource_entry_id, fdirection, faccount_key,
                 fresolved_account_code, fsummary, fdebit_amount, fcredit_amount,
                 fcurrency_code, frate, foriginal_amount, frule_entry_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """, id, accountingEventPk, line.lineNo(), line.sourceEntryId(), line.direction(), line.accountKey(),
                line.accountCode(), line.summary(), line.debitAmount(), line.creditAmount(),
                line.currencyCode(), line.originalAmount(), line.ruleEntryId());
    }

    public void insertAccountingDimension(
            Long id, Long accountingEventPk, Long accountingEventEntryId,
            String code, String valueId, String valueCode, String valueName
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_event_dimension
                (fid, faccounting_event_id, faccounting_event_entry_id,
                 fdimension_code, fdimension_value_id, fdimension_value_code, fdimension_value_name)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, accountingEventPk, accountingEventEntryId, code, valueId, valueCode, valueName);
    }

    public List<RuleHeader> findPublishedRules(
            String tenantId, Long orgId, String eventType, String bookId, LocalDate date
    ) {
        return jdbc.query("""
                SELECT r.fid AS rule_id, v.fid AS version_id, r.frule_code, v.fversion_no,
                       r.fpriority,
                       ((r.ftenant_id IS NOT NULL) + (r.forg_id IS NOT NULL) + (v.fbook_id IS NOT NULL)) AS specificity,
                       r.fsource, v.fbook_id
                  FROM matrix_fi_accounting_rule r
                  JOIN matrix_fi_accounting_rule_version v ON v.frule_id = r.fid
                 WHERE r.fstatus = 'PUBLISHED' AND v.fstatus = 'PUBLISHED'
                   AND r.fevent_type = ?
                   AND (r.ftenant_id IS NULL OR r.ftenant_id = ?)
                   AND (r.forg_id IS NULL OR r.forg_id = ?)
                   AND (v.fbook_id IS NULL OR v.fbook_id = ?)
                   AND (r.feffective_date IS NULL OR r.feffective_date <= ?)
                   AND (r.fexpiry_date IS NULL OR r.fexpiry_date >= ?)
                 ORDER BY r.fpriority DESC, specificity DESC, r.fid
                """, (rs, n) -> new RuleHeader(
                rs.getLong("rule_id"), rs.getLong("version_id"), rs.getString("frule_code"),
                rs.getInt("fversion_no"), rs.getInt("fpriority"), rs.getInt("specificity"),
                rs.getString("fsource"), rs.getString("fbook_id")
        ), eventType, tenantId, orgId, bookId, Date.valueOf(date), Date.valueOf(date));
    }

    public List<RuleEntry> findRuleEntries(Long versionId) {
        return jdbc.query("""
                SELECT fid, fline_no, fscope, fdirection, faccount_source_type, faccount_key, faccount_code,
                       famount_expression, fskip_zero_amount, fsummary_template, fcurrency_expression
                  FROM matrix_fi_accounting_rule_entry
                 WHERE frule_version_id = ? AND fstatus = 'ACTIVE' AND fdelete_flag = 0
                 ORDER BY fline_no, fid
                """, (rs, n) -> new RuleEntry(
                rs.getLong("fid"), rs.getInt("fline_no"), rs.getString("fscope"), rs.getString("fdirection"),
                rs.getString("faccount_source_type"), rs.getString("faccount_key"), rs.getString("faccount_code"),
                rs.getString("famount_expression"), rs.getBoolean("fskip_zero_amount"),
                rs.getString("fsummary_template"), rs.getString("fcurrency_expression")
        ), versionId);
    }

    public List<RuleDimension> findRuleDimensions(Long versionId) {
        return jdbc.query("""
                SELECT fid, frule_entry_id, fdimension_code, fsource_path, frequired
                  FROM matrix_fi_accounting_rule_dimension
                 WHERE frule_version_id = ? AND fstatus = 'ACTIVE' AND fdelete_flag = 0
                 ORDER BY fid
                """, (rs, n) -> new RuleDimension(
                rs.getLong("fid"), rs.getLong("frule_entry_id"), rs.getString("fdimension_code"),
                rs.getString("fsource_path"), rs.getBoolean("frequired")
        ), versionId);
    }

    public List<AccountMappingCandidate> findAccountMappings(
            String tenantId, Long orgId, String bookId, String accountKey, LocalDate date
    ) {
        return jdbc.query("""
                SELECT fid, faccount_code, fpriority,
                       ((ftenant_id IS NOT NULL) + (forg_id IS NOT NULL) + (fbook_id IS NOT NULL)) AS specificity,
                       fsource
                  FROM matrix_fi_account_mapping
                 WHERE faccount_key = ? AND fstatus = 'ACTIVE' AND fdelete_flag = 0
                   AND (ftenant_id IS NULL OR ftenant_id = ?)
                   AND (forg_id IS NULL OR forg_id = ?)
                   AND (fbook_id IS NULL OR fbook_id = ?)
                   AND (feffective_date IS NULL OR feffective_date <= ?)
                   AND (fexpiry_date IS NULL OR fexpiry_date >= ?)
                 ORDER BY fpriority DESC, specificity DESC, fid
                """, (rs, n) -> new AccountMappingCandidate(
                rs.getLong("fid"), rs.getString("faccount_code"), rs.getInt("fpriority"),
                rs.getInt("specificity"), rs.getString("fsource")
        ), accountKey, tenantId, orgId, bookId, Date.valueOf(date), Date.valueOf(date));
    }

    public void markAccountingVoucherGenerated(Long accountingEventPk, Long voucherId, String voucherNumber) {
        jdbc.update("""
                UPDATE matrix_fi_accounting_event
                   SET fstatus = 'VOUCHER_GENERATED', fstage = 'VOUCHER_GENERATION',
                       fvoucher_id = ?, fvoucher_number = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ?
                """, voucherId, voucherNumber, ts(LocalDateTime.now()), accountingEventPk);
    }

    public void updatePayableAccounting(
            Long payableId, String accountingEventId, Long voucherId, String voucherNumber
    ) {
        jdbc.update("""
                UPDATE matrix_fi_ap_payable
                   SET faccounting_event_id = ?, faccounting_status = 'VOUCHER_GENERATED',
                       fvoucher_id = ?, fvoucher_number = ?, fmodify_time = ?, fversion = fversion + 1
                 WHERE fid = ?
                """, accountingEventId, voucherId, voucherNumber, ts(LocalDateTime.now()), payableId);
    }

    public void insertVoucherLineDimension(
            Long id, Long voucherId, Long voucherLineId,
            String code, String valueId, String valueCode, String valueName
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_voucher_line_dimension
                (fid, fvoucher_id, fvoucher_line_id, fdimension_code,
                 fdimension_value_id, fdimension_value_code, fdimension_value_name,
                 fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, id, voucherId, voucherLineId, code, valueId, valueCode, valueName, ts(LocalDateTime.now()));
    }

    public void insertTrace(
            Long id, BusinessEventEnvelope event, Long payableId, String accountingEventId,
            String ruleCode, int ruleVersion, Long voucherId, String voucherNumber
    ) {
        jdbc.update("""
                INSERT INTO matrix_fi_accounting_trace
                (fid, ftenant_id, forg_id, fbusiness_event_id, fbusiness_event_type,
                 fsource_system_code, fsource_document_type, fsource_document_id, fsource_document_no,
                 fpayable_id, faccounting_event_id, frule_code, frule_version,
                 fvoucher_id, fvoucher_number, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id, event.tenantId(), event.orgId(), event.eventId(), event.eventType(),
                event.sourceSystemCode(), event.sourceDocumentType(), event.sourceDocumentId(), event.sourceDocumentNo(),
                payableId, accountingEventId, ruleCode, ruleVersion, voucherId, voucherNumber, ts(LocalDateTime.now()));
    }

    private Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
