package single.cjj.fi.ar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.entity.BizfiFiArapWriteoffLink;
import single.cjj.fi.ar.entity.BizfiFiArapWriteoffLog;
import single.cjj.fi.ar.entity.BizfiFiCounterpartyCredit;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.ar.mapper.BizfiFiArapWriteoffLinkMapper;
import single.cjj.fi.ar.mapper.BizfiFiArapWriteoffLogMapper;
import single.cjj.fi.ar.mapper.BizfiFiCounterpartyCreditMapper;
import single.cjj.fi.ar.service.BizfiFiArapManageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BizfiFiArapManageServiceImpl implements BizfiFiArapManageService {

    private static final Set<String> OPEN_STATUSES = Set.of("SUBMITTED", "AUDITED");
    private static final Set<String> AUDITED_STATUS = Set.of("AUDITED");
    private static final Map<String, Set<String>> SOURCE_TYPE_MAP = new HashMap<>();
    private static final Map<String, Set<String>> TARGET_TYPE_MAP = new HashMap<>();

    static {
        SOURCE_TYPE_MAP.put("AR", Set.of("AR", "AR_ESTIMATE"));
        SOURCE_TYPE_MAP.put("AP", Set.of("AP", "AP_ESTIMATE"));
        TARGET_TYPE_MAP.put("AR", Set.of("AR_SETTLEMENT"));
        TARGET_TYPE_MAP.put("AP", Set.of("AP_PAYMENT_PROCESS"));
    }

    @Autowired
    private BizfiFiArapDocMapper docMapper;

    @Autowired
    private BizfiFiArapWriteoffLinkMapper writeoffLinkMapper;

    @Autowired
    private BizfiFiArapWriteoffLogMapper writeoffLogMapper;

    @Autowired
    private BizfiFiCounterpartyCreditMapper creditMapper;

    @Override
    public Map<String, Object> plan(String docTypeRoot, String counterparty, LocalDate asOfDate, Boolean auditedOnly) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        boolean onlyAudited = Boolean.TRUE.equals(auditedOnly);

        ManageContext context = buildContext(root, trim(counterparty), date, onlyAudited ? AUDITED_STATUS : OPEN_STATUSES, true);
        List<PlanMatch> matches = buildPlanMatches(context.docs);
        BigDecimal suggestedAmount = matches.stream().map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = baseResult(root, date);
        result.put("counterparty", trim(counterparty));
        result.put("auditedOnly", onlyAudited);
        result.put("sourceDocCount", countRole(context.docs, "SOURCE"));
        result.put("targetDocCount", countRole(context.docs, "TARGET"));
        result.put("counterpartyCount", context.docs.stream().map(item -> item.counterparty).filter(StringUtils::hasText).distinct().count());
        result.put("sourceOpenAmount", scale(sumOpenAmount(context.docs, "SOURCE")));
        result.put("targetOpenAmount", scale(sumOpenAmount(context.docs, "TARGET")));
        result.put("suggestedAmount", scale(suggestedAmount));
        result.put("planCount", matches.size());
        result.put("remainingSourceAmount", scale(sumOpenAmount(context.docs, "SOURCE").subtract(suggestedAmount)));
        result.put("remainingTargetAmount", scale(sumOpenAmount(context.docs, "TARGET").subtract(suggestedAmount)));
        result.put("warnings", context.warnings);
        result.put("records", matches.stream().map(this::toPlanRow).toList());
        if (matches.isEmpty()) {
            context.warnings.add("No writeoff plan candidates were found under the current conditions.");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoWriteoff(String docTypeRoot, String counterparty, LocalDate asOfDate, String operator) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        String targetCounterparty = trim(counterparty);
        String operateBy = StringUtils.hasText(operator) ? operator.trim() : "system";

        ManageContext context = buildContext(root, targetCounterparty, date, AUDITED_STATUS, true);
        List<PlanMatch> matches = buildPlanMatches(context.docs);
        Map<String, Object> result = baseResult(root, date);
        result.put("counterparty", targetCounterparty);
        result.put("warnings", context.warnings);

        if (matches.isEmpty()) {
            result.put("message", "No writeoff candidates were found for automatic execution.");
            result.put("linkCount", 0);
            result.put("totalAmount", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("records", Collections.emptyList());
            return result;
        }

        String planCode = root + "-WO-" + System.currentTimeMillis();
        BigDecimal totalAmount = matches.stream().map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BizfiFiArapWriteoffLog log = new BizfiFiArapWriteoffLog();
        log.setFplanCode(planCode);
        log.setFdocTypeRoot(root);
        log.setFcounterparty(targetCounterparty);
        log.setFmode("AUTO");
        log.setFsourceDocCount((int) matches.stream().map(item -> item.sourceDoc.fid).distinct().count());
        log.setFtargetDocCount((int) matches.stream().map(item -> item.targetDoc.fid).distinct().count());
        log.setFlinkCount(matches.size());
        log.setFtotalAmount(scale(totalAmount));
        log.setFstatus("DONE");
        log.setFmessage("Auto writeoff completed by oldest-source to oldest-target matching.");
        log.setFoperator(operateBy);
        log.setFoperateTime(LocalDateTime.now());
        writeoffLogMapper.insert(log);

        for (PlanMatch match : matches) {
            BizfiFiArapWriteoffLink link = new BizfiFiArapWriteoffLink();
            link.setFlogId(log.getFid());
            link.setFplanCode(planCode);
            link.setFdocTypeRoot(root);
            link.setFcounterparty(match.counterparty);
            link.setFsourceDocId(match.sourceDoc.fid);
            link.setFsourceDocNumber(match.sourceDoc.number);
            link.setFsourceDocType(match.sourceDoc.docType);
            link.setFtargetDocId(match.targetDoc.fid);
            link.setFtargetDocNumber(match.targetDoc.number);
            link.setFtargetDocType(match.targetDoc.docType);
            link.setFamount(scale(match.amount));
            link.setFautoFlag(1);
            link.setFoperator(operateBy);
            link.setFoperateTime(log.getFoperateTime());
            link.setFremark("AUTO_GREEDY_MATCH");
            writeoffLinkMapper.insert(link);
        }

        result.put("planCode", planCode);
        result.put("logId", log.getFid());
        result.put("message", "Auto writeoff finished successfully.");
        result.put("sourceDocCount", log.getFsourceDocCount());
        result.put("targetDocCount", log.getFtargetDocCount());
        result.put("linkCount", log.getFlinkCount());
        result.put("totalAmount", log.getFtotalAmount());
        result.put("records", matches.stream().map(this::toPlanRow).toList());
        return result;
    }

    @Override
    public Map<String, Object> statement(String docTypeRoot, String counterparty, LocalDate asOfDate, Boolean openOnly) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        String targetCounterparty = trim(counterparty);
        boolean onlyOpen = Boolean.TRUE.equals(openOnly);

        ManageContext context = buildContext(root, targetCounterparty, date, OPEN_STATUSES, true);
        List<ManageDoc> docs = context.docs.stream()
                .filter(item -> isReconcilableRole(item.role))
                .filter(item -> !onlyOpen || item.openAmount.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing((ManageDoc item) -> safeText(item.counterparty))
                        .thenComparing(item -> item.docDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(item -> item.number, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<BizfiFiArapWriteoffLog> logs = listLogs(root, targetCounterparty, null, null, null).stream().limit(20).toList();

        Map<String, Object> result = baseResult(root, date);
        result.put("counterparty", targetCounterparty);
        result.put("openOnly", onlyOpen);
        result.put("docCount", docs.size());
        result.put("counterpartyCount", docs.stream().map(item -> item.counterparty).filter(StringUtils::hasText).distinct().count());
        result.put("totalAmount", scale(docs.stream().map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("writtenOffAmount", scale(docs.stream().map(item -> item.writtenOffAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("openAmount", scale(docs.stream().map(item -> item.openAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("openDocCount", docs.stream().filter(item -> item.openAmount.compareTo(BigDecimal.ZERO) > 0).count());
        result.put("recentWriteoffCount", logs.size());
        result.put("warnings", context.warnings);
        result.put("rows", docs.stream().map(this::toDocRow).toList());
        result.put("recentLogs", logs.stream().map(this::toLogRow).toList());
        if (!StringUtils.hasText(targetCounterparty)) {
            context.warnings.add("Statement rows include every counterparty because no exact counterparty filter was provided.");
        }
        return result;
    }

    @Override
    public Map<String, Object> accountQuery(String docTypeRoot, String counterparty, String docType, String status, Boolean openOnly, String keyword, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        String targetCounterparty = trim(counterparty);
        String targetDocType = trim(docType);
        String targetStatus = trim(status);
        String targetKeyword = trim(keyword);
        boolean onlyOpen = Boolean.TRUE.equals(openOnly);

        ManageContext context = buildContext(root, targetCounterparty, date, Collections.emptySet(), false);
        List<ManageDoc> docs = context.docs.stream()
                .filter(item -> !StringUtils.hasText(targetDocType) || targetDocType.equalsIgnoreCase(item.docType))
                .filter(item -> !StringUtils.hasText(targetStatus) || targetStatus.equalsIgnoreCase(item.status))
                .filter(item -> !onlyOpen || item.openAmount.compareTo(BigDecimal.ZERO) > 0)
                .filter(item -> matchesKeyword(item, targetKeyword))
                .sorted(Comparator.comparing((ManageDoc item) -> item.docDate, Comparator.nullsLast(LocalDate::compareTo)).reversed()
                        .thenComparing(item -> item.number, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<String, Object> result = baseResult(root, date);
        result.put("counterparty", targetCounterparty);
        result.put("docType", targetDocType);
        result.put("status", targetStatus);
        result.put("keyword", targetKeyword);
        result.put("openOnly", onlyOpen);
        result.put("docCount", docs.size());
        result.put("totalAmount", scale(docs.stream().map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("writtenOffAmount", scale(docs.stream().map(item -> item.writtenOffAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("openAmount", scale(docs.stream().map(item -> item.openAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("openDocCount", docs.stream().filter(item -> item.openAmount.compareTo(BigDecimal.ZERO) > 0).count());
        result.put("warnings", context.warnings);
        result.put("records", docs.stream().map(this::toDocRow).toList());
        return result;
    }

    @Override
    public Map<String, Object> writeoffLog(String docTypeRoot, String counterparty, String planCode, LocalDate startDate, LocalDate endDate) {
        String root = normalizeRoot(docTypeRoot);
        String targetCounterparty = trim(counterparty);
        String targetPlanCode = trim(planCode);
        List<BizfiFiArapWriteoffLog> logs = listLogs(root, targetCounterparty, targetPlanCode, startDate, endDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("docTypeRoot", root);
        result.put("counterparty", targetCounterparty);
        result.put("planCode", targetPlanCode);
        result.put("startDate", startDate == null ? null : startDate.toString());
        result.put("endDate", endDate == null ? null : endDate.toString());
        result.put("logCount", logs.size());
        result.put("linkCount", logs.stream().map(BizfiFiArapWriteoffLog::getFlinkCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
        result.put("totalAmount", scale(logs.stream().map(BizfiFiArapWriteoffLog::getFtotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("records", logs.stream().map(this::toLogRow).toList());
        result.put("warnings", new ArrayList<>(List.of("Writeoff logs are generated only after auto writeoff is executed successfully.")));
        if (StringUtils.hasText(targetPlanCode)) {
            List<BizfiFiArapWriteoffLink> links = listLinks(root, targetCounterparty, targetPlanCode);
            result.put("linkDetails", links.stream().map(this::toLinkRow).toList());
        }
        return result;
    }

    @Override
    public Map<String, Object> agingAnalysis(String docTypeRoot, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        ManageContext context = buildContext(root, null, date, OPEN_STATUSES, true);

        Map<String, BizfiFiCounterpartyCredit> creditMap = creditMapper.selectList(new LambdaQueryWrapper<BizfiFiCounterpartyCredit>()
                        .eq(BizfiFiCounterpartyCredit::getFdocTypeRoot, root)
                        .eq(BizfiFiCounterpartyCredit::getFenabled, 1))
                .stream()
                .collect(Collectors.toMap(BizfiFiCounterpartyCredit::getFcounterparty, item -> item, (left, right) -> left));

        Map<String, AgingAccumulator> grouped = new LinkedHashMap<>();
        for (ManageDoc doc : context.docs) {
            if (!"SOURCE".equals(doc.role) || doc.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String key = safeText(doc.counterparty);
            grouped.computeIfAbsent(key, AgingAccumulator::new).add(doc);
        }

        List<Map<String, Object>> rows = grouped.values().stream()
                .map(item -> item.toRow(creditMap.get(item.counterparty)))
                .sorted(Comparator.comparing((Map<String, Object> item) -> (BigDecimal) item.get("openAmount")).reversed())
                .toList();

        long warningCount = rows.stream().filter(item -> Boolean.TRUE.equals(item.get("riskFlag"))).count();
        Map<String, Object> result = baseResult(root, date);
        result.put("counterpartyCount", rows.size());
        result.put("warningCount", warningCount);
        result.put("totalOpenAmount", scale(rows.stream()
                .map(item -> (BigDecimal) item.get("openAmount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("rows", rows);
        result.put("warnings", context.warnings);
        context.warnings.add("Aging buckets use remaining open amount after deducting persisted writeoff links.");
        return result;
    }

    @Override
    public Map<String, Object> multiAnalysis(String docTypeRoot, String groupDimension, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        String dimension = normalizeGroupDimension(groupDimension);
        ManageContext context = buildContext(root, null, date, OPEN_STATUSES, false);

        Map<String, MultiAccumulator> grouped = new LinkedHashMap<>();
        for (ManageDoc doc : context.docs) {
            String key = groupKeyOf(doc, dimension);
            String name = groupNameOf(doc, dimension);
            grouped.computeIfAbsent(key, ignore -> new MultiAccumulator(key, name)).add(doc);
        }

        List<Map<String, Object>> rows = grouped.values().stream()
                .map(MultiAccumulator::toRow)
                .sorted(Comparator.comparing((Map<String, Object> item) -> (BigDecimal) item.get("openAmount")).reversed())
                .toList();

        Map<String, Object> result = baseResult(root, date);
        result.put("groupDimension", dimension);
        result.put("groupCount", rows.size());
        result.put("totalAmount", scale(rows.stream()
                .map(item -> (BigDecimal) item.get("amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("writtenOffAmount", scale(rows.stream()
                .map(item -> (BigDecimal) item.get("writtenOffAmount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("openAmount", scale(rows.stream()
                .map(item -> (BigDecimal) item.get("openAmount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        result.put("rows", rows);
        result.put("warnings", context.warnings);
        return result;
    }

    private ManageContext buildContext(String root, String counterparty, LocalDate asOfDate, Set<String> statuses, boolean reconcilableOnly) {
        ManageContext context = new ManageContext();
        context.root = root;
        context.counterparty = counterparty;
        context.asOfDate = asOfDate;
        context.warnings = new ArrayList<>();

        List<BizfiFiArapDoc> docs = listDocs(root, counterparty, asOfDate, statuses);
        if (docs.isEmpty()) {
            context.warnings.add("No AR/AP documents were found for the requested scope.");
            context.docs = Collections.emptyList();
            return context;
        }

        List<BizfiFiArapWriteoffLink> links = listLinks(root, counterparty, null);
        Map<Long, BigDecimal> usedAmountMap = buildUsedAmountMap(links);
        context.docs = docs.stream()
                .map(doc -> toManageDoc(root, doc, usedAmountMap, asOfDate))
                .filter(item -> !reconcilableOnly || isReconcilableRole(item.role))
                .toList();
        context.warnings.add("Open amount is calculated from original document amount minus persisted writeoff links.");
        if (reconcilableOnly) {
            context.warnings.add("Only configured source/target document types participate in writeoff planning.");
        }
        return context;
    }

    private List<BizfiFiArapDoc> listDocs(String root, String counterparty, LocalDate asOfDate, Set<String> statuses) {
        LambdaQueryWrapper<BizfiFiArapDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(BizfiFiArapDoc::getFdoctype, root);
        if (StringUtils.hasText(counterparty)) {
            wrapper.eq(BizfiFiArapDoc::getFcounterparty, counterparty);
        }
        if (asOfDate != null) {
            wrapper.le(BizfiFiArapDoc::getFdate, asOfDate);
        }
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(BizfiFiArapDoc::getFstatus, statuses);
        }
        wrapper.orderByAsc(BizfiFiArapDoc::getFcounterparty)
                .orderByAsc(BizfiFiArapDoc::getFdate)
                .orderByAsc(BizfiFiArapDoc::getFid);
        return docMapper.selectList(wrapper);
    }

    private List<BizfiFiArapWriteoffLink> listLinks(String root, String counterparty, String planCode) {
        LambdaQueryWrapper<BizfiFiArapWriteoffLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiArapWriteoffLink::getFdocTypeRoot, root);
        if (StringUtils.hasText(counterparty)) {
            wrapper.eq(BizfiFiArapWriteoffLink::getFcounterparty, counterparty);
        }
        if (StringUtils.hasText(planCode)) {
            wrapper.eq(BizfiFiArapWriteoffLink::getFplanCode, planCode);
        }
        wrapper.orderByDesc(BizfiFiArapWriteoffLink::getFoperateTime).orderByDesc(BizfiFiArapWriteoffLink::getFid);
        return writeoffLinkMapper.selectList(wrapper);
    }

    private List<BizfiFiArapWriteoffLog> listLogs(String root, String counterparty, String planCode, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BizfiFiArapWriteoffLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiArapWriteoffLog::getFdocTypeRoot, root);
        if (StringUtils.hasText(counterparty)) {
            wrapper.eq(BizfiFiArapWriteoffLog::getFcounterparty, counterparty);
        }
        if (StringUtils.hasText(planCode)) {
            wrapper.eq(BizfiFiArapWriteoffLog::getFplanCode, planCode);
        }
        if (startDate != null) {
            wrapper.ge(BizfiFiArapWriteoffLog::getFoperateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(BizfiFiArapWriteoffLog::getFoperateTime, endDate.plusDays(1).atStartOfDay().minusNanos(1));
        }
        wrapper.orderByDesc(BizfiFiArapWriteoffLog::getFoperateTime).orderByDesc(BizfiFiArapWriteoffLog::getFid);
        return writeoffLogMapper.selectList(wrapper);
    }

    private Map<Long, BigDecimal> buildUsedAmountMap(List<BizfiFiArapWriteoffLink> links) {
        Map<Long, BigDecimal> usedAmountMap = new HashMap<>();
        for (BizfiFiArapWriteoffLink link : links) {
            BigDecimal amount = nz(link.getFamount());
            usedAmountMap.merge(link.getFsourceDocId(), amount, BigDecimal::add);
            usedAmountMap.merge(link.getFtargetDocId(), amount, BigDecimal::add);
        }
        return usedAmountMap;
    }

    private ManageDoc toManageDoc(String root, BizfiFiArapDoc doc, Map<Long, BigDecimal> usedAmountMap, LocalDate asOfDate) {
        ManageDoc item = new ManageDoc();
        item.fid = doc.getFid();
        item.counterparty = doc.getFcounterparty();
        item.docTypeRoot = root;
        item.docType = trim(doc.getFdoctype());
        item.number = doc.getFnumber();
        item.docDate = doc.getFdate();
        item.status = trim(doc.getFstatus());
        item.amount = scale(nz(doc.getFamount()));
        item.writtenOffAmount = scale(min(item.amount, nz(usedAmountMap.get(doc.getFid()))));
        item.openAmount = scale(max(BigDecimal.ZERO, item.amount.subtract(item.writtenOffAmount)));
        item.role = roleOf(root, item.docType);
        item.ageDays = item.docDate == null ? 0L : Math.max(0L, ChronoUnit.DAYS.between(item.docDate, asOfDate == null ? LocalDate.now() : asOfDate));
        item.voucherNumber = trim(doc.getFvoucherNumber());
        item.remark = trim(doc.getFremark());
        return item;
    }

    private List<PlanMatch> buildPlanMatches(List<ManageDoc> docs) {
        Map<String, List<ManageDoc>> sourceByCounterparty = docs.stream()
                .filter(item -> "SOURCE".equals(item.role) && item.openAmount.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(item -> safeText(item.counterparty), LinkedHashMap::new, Collectors.toList()));
        Map<String, List<ManageDoc>> targetByCounterparty = docs.stream()
                .filter(item -> "TARGET".equals(item.role) && item.openAmount.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(item -> safeText(item.counterparty), LinkedHashMap::new, Collectors.toList()));

        Set<String> counterparties = new LinkedHashSet<>();
        counterparties.addAll(sourceByCounterparty.keySet());
        counterparties.addAll(targetByCounterparty.keySet());

        List<PlanMatch> matches = new ArrayList<>();
        for (String counterparty : counterparties) {
            List<ManageDoc> sources = cloneDocs(sourceByCounterparty.get(counterparty));
            List<ManageDoc> targets = cloneDocs(targetByCounterparty.get(counterparty));
            if (sources.isEmpty() || targets.isEmpty()) {
                continue;
            }

            int sourceIndex = 0;
            int targetIndex = 0;
            while (sourceIndex < sources.size() && targetIndex < targets.size()) {
                ManageDoc source = sources.get(sourceIndex);
                ManageDoc target = targets.get(targetIndex);
                BigDecimal matchAmount = min(source.openAmount, target.openAmount);
                if (matchAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    if (source.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        sourceIndex++;
                    }
                    if (target.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        targetIndex++;
                    }
                    continue;
                }

                PlanMatch match = new PlanMatch();
                match.counterparty = counterparty;
                match.sourceDoc = source.copy();
                match.targetDoc = target.copy();
                match.amount = scale(matchAmount);
                matches.add(match);

                source.openAmount = scale(source.openAmount.subtract(matchAmount));
                target.openAmount = scale(target.openAmount.subtract(matchAmount));
                if (source.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    sourceIndex++;
                }
                if (target.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    targetIndex++;
                }
            }
        }
        return matches;
    }

    private List<ManageDoc> cloneDocs(List<ManageDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        return docs.stream()
                .sorted(Comparator.comparing((ManageDoc item) -> item.docDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(item -> item.number, Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> item.fid, Comparator.nullsLast(Long::compareTo)))
                .map(ManageDoc::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> baseResult(String root, LocalDate asOfDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("docTypeRoot", root);
        result.put("asOfDate", asOfDate == null ? null : asOfDate.toString());
        return result;
    }

    private Map<String, Object> toPlanRow(PlanMatch match) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("counterparty", match.counterparty);
        row.put("sourceDocId", match.sourceDoc.fid);
        row.put("sourceNumber", match.sourceDoc.number);
        row.put("sourceType", match.sourceDoc.docType);
        row.put("sourceDate", match.sourceDoc.docDate == null ? null : match.sourceDoc.docDate.toString());
        row.put("sourceOpenAmount", match.sourceDoc.openAmount);
        row.put("sourceAgeDays", match.sourceDoc.ageDays);
        row.put("targetDocId", match.targetDoc.fid);
        row.put("targetNumber", match.targetDoc.number);
        row.put("targetType", match.targetDoc.docType);
        row.put("targetDate", match.targetDoc.docDate == null ? null : match.targetDoc.docDate.toString());
        row.put("targetOpenAmount", match.targetDoc.openAmount);
        row.put("targetAgeDays", match.targetDoc.ageDays);
        row.put("suggestedAmount", match.amount);
        return row;
    }

    private Map<String, Object> toDocRow(ManageDoc doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fid", doc.fid);
        row.put("counterparty", doc.counterparty);
        row.put("docTypeRoot", doc.docTypeRoot);
        row.put("docType", doc.docType);
        row.put("number", doc.number);
        row.put("docDate", doc.docDate == null ? null : doc.docDate.toString());
        row.put("status", doc.status);
        row.put("role", doc.role);
        row.put("amount", doc.amount);
        row.put("writtenOffAmount", doc.writtenOffAmount);
        row.put("openAmount", doc.openAmount);
        row.put("writeoffStatus", writeoffStatusOf(doc));
        row.put("ageDays", doc.ageDays);
        row.put("voucherNumber", doc.voucherNumber);
        row.put("remark", doc.remark);
        return row;
    }

    private Map<String, Object> toLogRow(BizfiFiArapWriteoffLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fid", log.getFid());
        row.put("planCode", log.getFplanCode());
        row.put("docTypeRoot", log.getFdocTypeRoot());
        row.put("counterparty", log.getFcounterparty());
        row.put("mode", log.getFmode());
        row.put("sourceDocCount", log.getFsourceDocCount());
        row.put("targetDocCount", log.getFtargetDocCount());
        row.put("linkCount", log.getFlinkCount());
        row.put("totalAmount", log.getFtotalAmount());
        row.put("status", log.getFstatus());
        row.put("message", log.getFmessage());
        row.put("operator", log.getFoperator());
        row.put("operateTime", log.getFoperateTime() == null ? null : log.getFoperateTime().toString());
        return row;
    }

    private Map<String, Object> toLinkRow(BizfiFiArapWriteoffLink link) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fid", link.getFid());
        row.put("planCode", link.getFplanCode());
        row.put("counterparty", link.getFcounterparty());
        row.put("sourceNumber", link.getFsourceDocNumber());
        row.put("sourceType", link.getFsourceDocType());
        row.put("targetNumber", link.getFtargetDocNumber());
        row.put("targetType", link.getFtargetDocType());
        row.put("amount", link.getFamount());
        row.put("operator", link.getFoperator());
        row.put("operateTime", link.getFoperateTime() == null ? null : link.getFoperateTime().toString());
        row.put("remark", link.getFremark());
        return row;
    }

    private String normalizeRoot(String docTypeRoot) {
        String root = StringUtils.hasText(docTypeRoot) ? docTypeRoot.trim().toUpperCase(Locale.ROOT) : "AR";
        if (!SOURCE_TYPE_MAP.containsKey(root)) {
            throw new BizException("docTypeRoot only supports AR or AP");
        }
        return root;
    }

    private String normalizeGroupDimension(String value) {
        String dimension = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "COUNTERPARTY";
        Set<String> supported = Set.of("COUNTERPARTY", "DOCTYPE", "STATUS", "COUNTERPARTY_DOCTYPE", "ROLE");
        if (!supported.contains(dimension)) {
            throw new BizException("groupDimension is not supported");
        }
        return dimension;
    }

    private String roleOf(String root, String docType) {
        if (SOURCE_TYPE_MAP.getOrDefault(root, Collections.emptySet()).contains(docType)) {
            return "SOURCE";
        }
        if (TARGET_TYPE_MAP.getOrDefault(root, Collections.emptySet()).contains(docType)) {
            return "TARGET";
        }
        return "OTHER";
    }

    private boolean isReconcilableRole(String role) {
        return "SOURCE".equals(role) || "TARGET".equals(role);
    }

    private long countRole(List<ManageDoc> docs, String role) {
        return docs.stream().filter(item -> role.equals(item.role)).count();
    }

    private BigDecimal sumOpenAmount(List<ManageDoc> docs, String role) {
        return docs.stream()
                .filter(item -> role.equals(item.role))
                .map(item -> item.openAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String writeoffStatusOf(ManageDoc doc) {
        if (doc.writtenOffAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNWRITTEN";
        }
        if (doc.openAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "FULL";
        }
        return "PARTIAL";
    }

    private String groupKeyOf(ManageDoc doc, String dimension) {
        if ("DOCTYPE".equals(dimension)) {
            return safeText(doc.docType);
        }
        if ("STATUS".equals(dimension)) {
            return safeText(doc.status);
        }
        if ("COUNTERPARTY_DOCTYPE".equals(dimension)) {
            return safeText(doc.counterparty) + "|" + safeText(doc.docType);
        }
        if ("ROLE".equals(dimension)) {
            return safeText(doc.role);
        }
        return safeText(doc.counterparty);
    }

    private String groupNameOf(ManageDoc doc, String dimension) {
        if ("COUNTERPARTY_DOCTYPE".equals(dimension)) {
            return safeText(doc.counterparty) + " / " + safeText(doc.docType);
        }
        return groupKeyOf(doc, dimension);
    }

    private boolean matchesKeyword(ManageDoc doc, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(doc.number, value)
                || containsIgnoreCase(doc.counterparty, value)
                || containsIgnoreCase(doc.docType, value)
                || containsIgnoreCase(doc.status, value)
                || containsIgnoreCase(doc.voucherNumber, value)
                || containsIgnoreCase(doc.remark, value);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal min(BigDecimal left, BigDecimal right) {
        return nz(left).min(nz(right));
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        return nz(left).max(nz(right));
    }

    private static class ManageContext {
        private String root;
        private String counterparty;
        private LocalDate asOfDate;
        private List<ManageDoc> docs = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
    }

    private static class ManageDoc {
        private Long fid;
        private String counterparty;
        private String docTypeRoot;
        private String docType;
        private String number;
        private LocalDate docDate;
        private String status;
        private String role;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal writtenOffAmount = BigDecimal.ZERO;
        private BigDecimal openAmount = BigDecimal.ZERO;
        private long ageDays;
        private String voucherNumber;
        private String remark;

        private ManageDoc copy() {
            ManageDoc item = new ManageDoc();
            item.fid = fid;
            item.counterparty = counterparty;
            item.docTypeRoot = docTypeRoot;
            item.docType = docType;
            item.number = number;
            item.docDate = docDate;
            item.status = status;
            item.role = role;
            item.amount = amount;
            item.writtenOffAmount = writtenOffAmount;
            item.openAmount = openAmount;
            item.ageDays = ageDays;
            item.voucherNumber = voucherNumber;
            item.remark = remark;
            return item;
        }
    }

    private static class PlanMatch {
        private String counterparty;
        private ManageDoc sourceDoc;
        private ManageDoc targetDoc;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    private static class AgingAccumulator {
        private final String counterparty;
        private BigDecimal bucket0_30 = BigDecimal.ZERO;
        private BigDecimal bucket31_60 = BigDecimal.ZERO;
        private BigDecimal bucket61_90 = BigDecimal.ZERO;
        private BigDecimal bucket91Plus = BigDecimal.ZERO;
        private BigDecimal openAmount = BigDecimal.ZERO;
        private BigDecimal writtenOffAmount = BigDecimal.ZERO;
        private long maxAgeDays;
        private int docCount;

        private AgingAccumulator(String counterparty) {
            this.counterparty = counterparty;
        }

        private void add(ManageDoc doc) {
            BigDecimal amount = doc.openAmount;
            openAmount = openAmount.add(amount);
            writtenOffAmount = writtenOffAmount.add(doc.writtenOffAmount);
            maxAgeDays = Math.max(maxAgeDays, doc.ageDays);
            docCount += 1;
            if (doc.ageDays <= 30) {
                bucket0_30 = bucket0_30.add(amount);
            } else if (doc.ageDays <= 60) {
                bucket31_60 = bucket31_60.add(amount);
            } else if (doc.ageDays <= 90) {
                bucket61_90 = bucket61_90.add(amount);
            } else {
                bucket91Plus = bucket91Plus.add(amount);
            }
        }

        private Map<String, Object> toRow(BizfiFiCounterpartyCredit credit) {
            boolean overLimit = credit != null && credit.getFcreditLimit() != null && openAmount.compareTo(credit.getFcreditLimit()) > 0;
            int overdueThreshold = credit == null || credit.getFoverdueDaysThreshold() == null ? 30 : credit.getFoverdueDaysThreshold();
            boolean overdue = maxAgeDays > overdueThreshold;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("counterparty", counterparty);
            row.put("docCount", docCount);
            row.put("openAmount", openAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("writtenOffAmount", writtenOffAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("bucket0_30", bucket0_30.setScale(2, RoundingMode.HALF_UP));
            row.put("bucket31_60", bucket31_60.setScale(2, RoundingMode.HALF_UP));
            row.put("bucket61_90", bucket61_90.setScale(2, RoundingMode.HALF_UP));
            row.put("bucket91Plus", bucket91Plus.setScale(2, RoundingMode.HALF_UP));
            row.put("maxAgeDays", maxAgeDays);
            row.put("creditLimit", credit == null ? null : credit.getFcreditLimit());
            row.put("riskFlag", overLimit || overdue);
            row.put("riskReason", buildRiskReason(overLimit, overdue));
            return row;
        }

        private String buildRiskReason(boolean overLimit, boolean overdue) {
            List<String> reasons = new ArrayList<>();
            if (overLimit) {
                reasons.add("OVER_LIMIT");
            }
            if (overdue) {
                reasons.add("OVERDUE");
            }
            return String.join(", ", reasons);
        }
    }

    private static class MultiAccumulator {
        private final String groupKey;
        private final String groupName;
        private int docCount;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal writtenOffAmount = BigDecimal.ZERO;
        private BigDecimal openAmount = BigDecimal.ZERO;
        private long totalAgeDays;
        private LocalDate latestDate;

        private MultiAccumulator(String groupKey, String groupName) {
            this.groupKey = groupKey;
            this.groupName = groupName;
        }

        private void add(ManageDoc doc) {
            docCount += 1;
            amount = amount.add(doc.amount);
            writtenOffAmount = writtenOffAmount.add(doc.writtenOffAmount);
            openAmount = openAmount.add(doc.openAmount);
            totalAgeDays += doc.ageDays;
            if (doc.docDate != null && (latestDate == null || doc.docDate.isAfter(latestDate))) {
                latestDate = doc.docDate;
            }
        }

        private Map<String, Object> toRow() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("groupKey", groupKey);
            row.put("groupName", groupName);
            row.put("docCount", docCount);
            row.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
            row.put("writtenOffAmount", writtenOffAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("openAmount", openAmount.setScale(2, RoundingMode.HALF_UP));
            row.put("avgAgeDays", docCount == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(totalAgeDays).divide(BigDecimal.valueOf(docCount), 2, RoundingMode.HALF_UP));
            row.put("latestDate", latestDate == null ? null : latestDate.toString());
            return row;
        }
    }
}
