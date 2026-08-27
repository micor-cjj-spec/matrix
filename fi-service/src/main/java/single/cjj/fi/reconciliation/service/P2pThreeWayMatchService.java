package single.cjj.fi.reconciliation.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.Difference;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InboundSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InvoiceSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.PurchaseOrderSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchLine;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchLineResult;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchRequest;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class P2pThreeWayMatchService {

    private static final String SCENARIO = "P2P_3WAY_MATCH";
    private static final String RULE_CODE = "P2P_3WAY_MATCH";
    private static final String RESULT_MATCHED = "MATCHED";
    private static final String RESULT_PARTIAL = "PARTIAL_MATCHED";
    private static final String RESULT_DIFFERENCE = "DIFFERENCE";
    private static final String RESULT_UNMATCHED = "UNMATCHED";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final P2pThreeWayMatchPolicy matchPolicy;

    public P2pThreeWayMatchService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                                   P2pThreeWayMatchPolicy matchPolicy) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.matchPolicy = matchPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public ThreeWayMatchResponse execute(ThreeWayMatchRequest request) {
        validateRequest(request);
        ThreeWayMatchResponse existing = findByRequestId(request.tenantId(), request.requestId());
        if (existing != null) {
            return existing;
        }
        RuleRef rule = loadPublishedRule(request.tenantId());
        long batchId = IdWorker.getId();
        String batchNo = "REC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.valueOf(batchId).substring(Math.max(0, String.valueOf(batchId).length() - 6));
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbcTemplate.update("""
                    INSERT INTO matrix_fi_reconciliation_batch
                    (fid, ftenant_id, forg_id, fbatch_no, fscenario_type, frule_code, frule_version,
                     frequest_id, fsource_system_code, fsource_document_type, fsource_document_id,
                     fsource_document_no, fstatus, ftotal_case_count, fmatched_count, fpartial_count,
                     fdifference_count, funmatched_count, fstart_time, fcreate_time, fdelete_flag, fversion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'MATRIX', 'ERP_SUPPLIER_INVOICE', ?, ?,
                            'PROCESSING', 0, 0, 0, 0, 0, ?, ?, 0, 0)
                    """,
                    batchId, request.tenantId(), request.orgId(), batchNo, SCENARIO, rule.code(), rule.version(),
                    request.requestId(), String.valueOf(request.invoiceId()), request.invoiceNo(), now, now);
        } catch (DuplicateKeyException duplicate) {
            ThreeWayMatchResponse concurrent = findByRequestId(request.tenantId(), request.requestId());
            if (concurrent != null) return concurrent;
            throw duplicate;
        }

        List<ThreeWayMatchLineResult> lineResults = new ArrayList<>();
        int matched = 0;
        int difference = 0;
        int unmatched = 0;
        for (ThreeWayMatchLine line : request.lines()) {
            ThreeWayMatchLineResult result = evaluateAndPersistLine(batchId, batchNo, request, line, rule, now);
            lineResults.add(result);
            switch (result.result()) {
                case RESULT_MATCHED -> matched++;
                case RESULT_UNMATCHED -> unmatched++;
                default -> difference++;
            }
        }
        int partial = matched > 0 && (difference > 0 || unmatched > 0) ? 1 : 0;
        String overall = matched == request.lines().size()
                ? RESULT_MATCHED
                : matched > 0
                ? RESULT_PARTIAL
                : unmatched == request.lines().size()
                ? RESULT_UNMATCHED
                : RESULT_DIFFERENCE;
        LocalDateTime finished = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE matrix_fi_reconciliation_batch
                   SET fstatus = 'COMPLETED', ftotal_case_count = ?, fmatched_count = ?, fpartial_count = ?,
                       fdifference_count = ?, funmatched_count = ?, fresult = ?, ffinish_time = ?, fmodify_time = ?
                 WHERE fid = ? AND ftenant_id = ?
                """, request.lines().size(), matched, partial, difference, unmatched, overall, finished, finished,
                batchId, request.tenantId());
        return new ThreeWayMatchResponse(batchId, batchNo, request.requestId(), overall,
                matched, partial, difference, unmatched, lineResults);
    }

    private ThreeWayMatchLineResult evaluateAndPersistLine(long batchId, String batchNo, ThreeWayMatchRequest request,
                                                            ThreeWayMatchLine line, RuleRef rule, LocalDateTime now) {
        long caseId = IdWorker.getId();
        PurchaseOrderSnapshot po = line.purchaseOrder();
        InvoiceSnapshot invoice = line.invoice();
        List<InboundSnapshot> inbounds = line.inbounds() == null ? List.of() : line.inbounds();
        P2pThreeWayMatchPolicy.Evaluation evaluation = matchPolicy.evaluate(request, line);
        List<Difference> differences = evaluation.differences();
        BigDecimal available = evaluation.availableInboundQuantity();
        String result = evaluation.result();

        String caseNo = batchNo + "-L" + (line.lineNo() == null ? line.invoiceEntryId() : line.lineNo());
        jdbcTemplate.update("""
                INSERT INTO matrix_fi_reconciliation_case
                (fid, ftenant_id, forg_id, fbatch_id, fcase_no, fcase_key, fresult,
                 favailable_quantity, fsnapshot_json, fstatus, fcreate_time, fmodify_time, fdelete_flag, fversion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?, 0, 0)
                """, caseId, request.tenantId(), request.orgId(), batchId, caseNo,
                String.valueOf(line.invoiceEntryId()), result, available, toJson(line), now, now);

        persistParticipant(caseId, request, "SUPPLIER_INVOICE", "ERP_SUPPLIER_INVOICE", request.invoiceId(),
                request.invoiceNo(), line.invoiceEntryId(), request.businessPartnerId(), request.currencyCode(),
                request.invoiceDate(), invoice);
        if (po != null) {
            persistParticipant(caseId, request, "PURCHASE_ORDER", "ERP_PURCHASE_ORDER", po.purchaseOrderId(),
                    po.purchaseOrderNo(), po.purchaseOrderEntryId(), po.businessPartnerId(), po.currencyCode(), null, po);
        }
        for (InboundSnapshot inbound : inbounds) {
            persistParticipant(caseId, request, "PURCHASE_INBOUND", "ERP_PURCHASE_INBOUND", inbound.inboundId(),
                    inbound.inboundNo(), inbound.inboundEntryId(), inbound.businessPartnerId(), inbound.currencyCode(),
                    null, inbound);
        }

        jdbcTemplate.update("""
                INSERT INTO matrix_fi_reconciliation_match
                (fid, ftenant_id, forg_id, fcase_id, fmatch_type, fmatched_quantity,
                 fexpected_value_json, factual_value_json, fstatus, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, 'P2P_3WAY_MATCH', ?, ?, ?, ?, ?, 0)
                """, IdWorker.getId(), request.tenantId(), request.orgId(), caseId,
                invoice == null ? BigDecimal.ZERO : nvl(invoice.quantity()).min(available),
                toJson(Map.of("availableInboundQuantity", available,
                        "poUnitPrice", po == null ? BigDecimal.ZERO : nvl(po.unitPrice()),
                        "poTaxRate", po == null ? BigDecimal.ZERO : nvl(po.taxRate()))),
                toJson(invoice), result, now);

        for (Difference item : differences) {
            jdbcTemplate.update("""
                    INSERT INTO matrix_fi_reconciliation_difference
                    (fid, ftenant_id, forg_id, fcase_id, fdifference_code, ffield_code,
                     fexpected_value, factual_value, fseverity, fmessage, fstatus,
                     fcreate_time, fmodify_time, fdelete_flag, fversion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, 0, 0)
                    """, IdWorker.getId(), request.tenantId(), request.orgId(), caseId, item.code(), item.field(),
                    item.expectedValue(), item.actualValue(), item.severity(), item.message(), now, now);
        }
        return new ThreeWayMatchLineResult(line.invoiceEntryId(), caseId, result, available, differences);
    }

    private void persistParticipant(long caseId, ThreeWayMatchRequest request, String role, String documentType,
                                    Long documentId, String documentNo, Long entryId, Long partnerId,
                                    String currency, Object businessDate, Object snapshot) {
        jdbcTemplate.update("""
                INSERT INTO matrix_fi_reconciliation_participant
                (fid, ftenant_id, forg_id, fcase_id, fparticipant_role, fsystem_code,
                 fdocument_type, fdocument_id, fdocument_no, fentry_id, fbusiness_partner_id,
                 fcurrency_code, fbusiness_date, fsnapshot_json, fcreate_time, fdelete_flag)
                VALUES (?, ?, ?, ?, ?, 'MATRIX', ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, IdWorker.getId(), request.tenantId(), request.orgId(), caseId, role, documentType,
                documentId == null ? null : String.valueOf(documentId), documentNo,
                entryId == null ? null : String.valueOf(entryId), partnerId, currency,
                businessDate, toJson(snapshot), LocalDateTime.now());
    }

    private RuleRef loadPublishedRule(String tenantId) {
        List<RuleRef> rules = jdbcTemplate.query("""
                SELECT r.fcode, v.fversion_no
                  FROM matrix_fi_reconciliation_rule r
                  JOIN matrix_fi_reconciliation_rule_version v ON v.frule_id = r.fid
                 WHERE r.fcode = ?
                   AND r.fstatus = 'PUBLISHED'
                   AND r.fdelete_flag = 0
                   AND v.fstatus = 'PUBLISHED'
                   AND v.fdelete_flag = 0
                   AND (r.ftenant_id IS NULL OR r.ftenant_id = ?)
                 ORDER BY CASE WHEN r.ftenant_id = ? THEN 1 ELSE 0 END DESC,
                          v.fversion_no DESC
                 LIMIT 2
                """, (rs, rowNum) -> new RuleRef(rs.getString("fcode"), rs.getInt("fversion_no")),
                RULE_CODE, tenantId, tenantId);
        if (rules.isEmpty()) throw new BizException("RECONCILIATION_RULE_NOT_FOUND: " + RULE_CODE);
        return rules.get(0);
    }

    private ThreeWayMatchResponse findByRequestId(String tenantId, String requestId) {
        List<BatchRow> batches = jdbcTemplate.query("""
                SELECT fid, fbatch_no, frequest_id, fresult, fmatched_count, fpartial_count,
                       fdifference_count, funmatched_count, fstatus
                  FROM matrix_fi_reconciliation_batch
                 WHERE ftenant_id = ? AND frequest_id = ? AND fdelete_flag = 0
                 LIMIT 1
                """, (rs, rowNum) -> batchRow(rs), tenantId, requestId);
        if (batches.isEmpty()) return null;
        BatchRow batch = batches.get(0);
        if (!"COMPLETED".equals(batch.status())) {
            throw new BizException("相同 requestId 的三单匹配正在处理中");
        }
        List<ThreeWayMatchLineResult> lines = jdbcTemplate.query("""
                SELECT fid, fcase_key, fresult, favailable_quantity
                  FROM matrix_fi_reconciliation_case
                 WHERE ftenant_id = ? AND fbatch_id = ? AND fdelete_flag = 0
                 ORDER BY fid
                """, (rs, rowNum) -> {
            long caseId = rs.getLong("fid");
            List<Difference> differences = loadDifferences(tenantId, caseId);
            return new ThreeWayMatchLineResult(Long.valueOf(rs.getString("fcase_key")), caseId,
                    rs.getString("fresult"), rs.getBigDecimal("favailable_quantity"), differences);
        }, tenantId, batch.id());
        return new ThreeWayMatchResponse(batch.id(), batch.batchNo(), batch.requestId(), batch.result(),
                batch.matchedCount(), batch.partialCount(), batch.differenceCount(), batch.unmatchedCount(), lines);
    }

    private List<Difference> loadDifferences(String tenantId, long caseId) {
        return jdbcTemplate.query("""
                SELECT fdifference_code, ffield_code, fexpected_value, factual_value, fseverity, fmessage
                  FROM matrix_fi_reconciliation_difference
                 WHERE ftenant_id = ? AND fcase_id = ? AND fdelete_flag = 0
                 ORDER BY fid
                """, (rs, rowNum) -> new Difference(rs.getString("fdifference_code"), rs.getString("ffield_code"),
                rs.getString("fexpected_value"), rs.getString("factual_value"), rs.getString("fseverity"),
                rs.getString("fmessage")), tenantId, caseId);
    }

    private BatchRow batchRow(ResultSet rs) throws SQLException {
        return new BatchRow(rs.getLong("fid"), rs.getString("fbatch_no"), rs.getString("frequest_id"),
                rs.getString("fresult"), rs.getInt("fmatched_count"), rs.getInt("fpartial_count"),
                rs.getInt("fdifference_count"), rs.getInt("funmatched_count"), rs.getString("fstatus"));
    }

    private void validateRequest(ThreeWayMatchRequest request) {
        if (request == null) throw new BizException("三单匹配请求不能为空");
        if (!StringUtils.hasText(request.requestId())) throw new BizException("requestId 不能为空");
        if (!StringUtils.hasText(request.tenantId())) throw new BizException("tenantId 不能为空");
        if (request.orgId() == null) throw new BizException("orgId 不能为空");
        if (request.invoiceId() == null) throw new BizException("invoiceId 不能为空");
        if (request.businessPartnerId() == null) throw new BizException("businessPartnerId 不能为空");
        if (!StringUtils.hasText(request.currencyCode())) throw new BizException("currencyCode 不能为空");
        if (request.lines() == null || request.lines().isEmpty()) throw new BizException("三单匹配至少需要一行发票分录");
    }

    private void compareLong(List<Difference> out, String code, String field, Long expected, Long actual, String message) {
        if (!Objects.equals(expected, actual)) out.add(diff(code, field, text(expected), text(actual), "BLOCKING", message));
    }

    private void compareText(List<Difference> out, String code, String field, String expected, String actual, String message) {
        if (!Objects.equals(normalize(expected), normalize(actual)))
            out.add(diff(code, field, expected, actual, "BLOCKING", message));
    }

    private void compareDecimal(List<Difference> out, String code, String field, BigDecimal expected, BigDecimal actual, String message) {
        if (nvl(expected).compareTo(nvl(actual)) != 0)
            out.add(diff(code, field, nvl(expected).toPlainString(), nvl(actual).toPlainString(), "BLOCKING", message));
    }

    private Difference diff(String code, String field, String expected, String actual, String severity, String message) {
        return new Difference(code, field, expected, actual, severity, message);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException("对账快照序列化失败");
        }
    }

    private record RuleRef(String code, int version) {
    }

    private record BatchRow(long id, String batchNo, String requestId, String result, int matchedCount,
                            int partialCount, int differenceCount, int unmatchedCount, String status) {
    }
}
