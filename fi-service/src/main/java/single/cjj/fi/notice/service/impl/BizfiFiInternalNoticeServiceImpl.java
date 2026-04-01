package single.cjj.fi.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.service.BizfiFiArapManageService;
import single.cjj.fi.gl.report.service.BizfiFiCashFlowService;
import single.cjj.fi.gl.report.vo.CashFlowSupplementResultVO;
import single.cjj.fi.gl.report.vo.CashFlowSupplementVoucherVO;
import single.cjj.fi.notice.entity.BizfiFiInternalNotice;
import single.cjj.fi.notice.mapper.BizfiFiInternalNoticeMapper;
import single.cjj.fi.notice.service.BizfiFiInternalNoticeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BizfiFiInternalNoticeServiceImpl implements BizfiFiInternalNoticeService {

    private static final String NOTICE_COUNTERPARTY = "COUNTERPARTY";
    private static final String NOTICE_CASHFLOW = "CASHFLOW";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_RESOLVED = "RESOLVED";

    @Autowired
    private BizfiFiInternalNoticeMapper noticeMapper;

    @Autowired
    private BizfiFiArapManageService arapManageService;

    @Autowired
    private BizfiFiCashFlowService cashFlowService;

    @Override
    public Map<String, Object> queryCounterpartyNotices(String docTypeRoot, String status, String severity, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        List<BizfiFiInternalNotice> notices = listCounterpartyNotices(root, status, severity, asOfDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeType", NOTICE_COUNTERPARTY);
        result.put("docTypeRoot", root);
        result.put("asOfDate", formatDate(asOfDate));
        result.put("noticeCount", notices.size());
        result.put("openCount", notices.stream().filter(item -> STATUS_OPEN.equals(item.getFstatus())).count());
        result.put("resolvedCount", notices.stream().filter(item -> STATUS_RESOLVED.equals(item.getFstatus())).count());
        result.put("highCount", notices.stream().filter(item -> "HIGH".equals(item.getFseverity())).count());
        result.put("amount", scale(sumAmount(notices, BizfiFiInternalNotice::getFamount)));
        result.put("openAmount", scale(sumAmount(notices, BizfiFiInternalNotice::getFopenAmount)));
        result.put("rows", notices.stream().map(this::toCounterpartyNoticeRow).toList());
        result.put("warnings", buildEmptyWarning(notices, "No counterparty notices were found. Generate notice sheets from the latest AR/AP risk scan first."));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateCounterpartyNotices(String docTypeRoot, LocalDate asOfDate, String operator) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate targetDate = asOfDate == null ? LocalDate.now() : asOfDate;
        String operateBy = StringUtils.hasText(operator) ? operator.trim() : "system";

        List<NoticeCandidate> candidates = buildCounterpartyCandidates(root, targetDate);
        List<BizfiFiInternalNotice> existingNotices = listCounterpartyNotices(root, null, null, null);
        SyncResult syncResult = syncNotices(NOTICE_COUNTERPARTY, existingNotices, candidates, operateBy, root, null, YearMonth.from(targetDate).toString());

        Map<String, Object> result = queryCounterpartyNotices(root, null, null, targetDate);
        result.put("generatedCount", candidates.size());
        result.put("insertedCount", syncResult.insertedCount);
        result.put("updatedCount", syncResult.updatedCount);
        result.put("resolvedAutoCount", syncResult.resolvedCount);
        result.put("message", candidates.isEmpty()
                ? "No current counterparty risk items require notice generation."
                : "Counterparty notices were generated from the latest aging and risk analysis.");
        return result;
    }

    @Override
    public Map<String, Object> reconcileCounterpartyNotices(String docTypeRoot, String status, LocalDate asOfDate) {
        String root = normalizeRoot(docTypeRoot);
        LocalDate targetDate = asOfDate == null ? LocalDate.now() : asOfDate;
        List<BizfiFiInternalNotice> notices = listCounterpartyNotices(root, status, null, null);
        Map<String, NoticeCandidate> currentMap = buildCounterpartyCandidates(root, targetDate).stream()
                .collect(Collectors.toMap(item -> item.referenceKey, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> rows = notices.stream()
                .map(notice -> toCounterpartyReconcileRow(notice, currentMap.get(notice.getFreferenceKey())))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeType", NOTICE_COUNTERPARTY);
        result.put("docTypeRoot", root);
        result.put("asOfDate", formatDate(targetDate));
        result.put("noticeCount", rows.size());
        result.put("ongoingCount", rows.stream().filter(item -> "ONGOING".equals(item.get("matchStatus"))).count());
        result.put("resolvedCount", rows.stream().filter(item -> "RESOLVED".equals(item.get("matchStatus"))).count());
        result.put("snapshotOpenAmount", scale(sumRowAmount(rows, "snapshotOpenAmount")));
        result.put("currentOpenAmount", scale(sumRowAmount(rows, "currentOpenAmount")));
        result.put("rows", rows);
        result.put("warnings", buildEmptyWarning(notices, "No counterparty notices are available for reconciliation."));
        return result;
    }

    @Override
    public Map<String, Object> queryCashflowNotices(Long orgId, String period, String status, String severity, String sourceCode, String currency) {
        String targetPeriod = normalizePeriod(period);
        List<BizfiFiInternalNotice> notices = listCashflowNotices(orgId, targetPeriod, status, severity, sourceCode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeType", NOTICE_CASHFLOW);
        result.put("orgId", orgId);
        result.put("period", targetPeriod);
        result.put("currency", StringUtils.hasText(currency) ? currency.trim() : "CNY");
        result.put("noticeCount", notices.size());
        result.put("openCount", notices.stream().filter(item -> STATUS_OPEN.equals(item.getFstatus())).count());
        result.put("resolvedCount", notices.stream().filter(item -> STATUS_RESOLVED.equals(item.getFstatus())).count());
        result.put("highCount", notices.stream().filter(item -> "HIGH".equals(item.getFseverity())).count());
        result.put("amount", scale(sumAmount(notices, BizfiFiInternalNotice::getFamount)));
        result.put("rows", notices.stream().map(this::toCashflowNoticeRow).toList());
        result.put("warnings", buildEmptyWarning(notices, "No cash-flow notices were found. Generate them from pending supplement or review items first."));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateCashflowNotices(Long orgId, String period, String currency, String operator) {
        String targetPeriod = normalizePeriod(period);
        String operateBy = StringUtils.hasText(operator) ? operator.trim() : "system";
        List<NoticeCandidate> candidates = buildCashflowCandidates(orgId, targetPeriod, currency);
        List<BizfiFiInternalNotice> existingNotices = listCashflowNotices(orgId, targetPeriod, null, null, null);
        SyncResult syncResult = syncNotices(NOTICE_CASHFLOW, existingNotices, candidates, operateBy, null, orgId, targetPeriod);

        Map<String, Object> result = queryCashflowNotices(orgId, targetPeriod, null, null, null, currency);
        result.put("generatedCount", candidates.size());
        result.put("insertedCount", syncResult.insertedCount);
        result.put("updatedCount", syncResult.updatedCount);
        result.put("resolvedAutoCount", syncResult.resolvedCount);
        result.put("message", candidates.isEmpty()
                ? "No current cash-flow review items require notice generation."
                : "Cash-flow notices were generated from pending supplement and review items.");
        return result;
    }

    @Override
    public Map<String, Object> reconcileCashflowNotices(Long orgId, String period, String status, String currency) {
        String targetPeriod = normalizePeriod(period);
        List<BizfiFiInternalNotice> notices = listCashflowNotices(orgId, targetPeriod, status, null, null);
        Map<String, NoticeCandidate> currentMap = buildCashflowCandidates(orgId, targetPeriod, currency).stream()
                .collect(Collectors.toMap(item -> item.referenceKey, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> rows = notices.stream()
                .map(notice -> toCashflowReconcileRow(notice, currentMap.get(notice.getFreferenceKey())))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeType", NOTICE_CASHFLOW);
        result.put("orgId", orgId);
        result.put("period", targetPeriod);
        result.put("currency", StringUtils.hasText(currency) ? currency.trim() : "CNY");
        result.put("noticeCount", rows.size());
        result.put("ongoingCount", rows.stream().filter(item -> "ONGOING".equals(item.get("matchStatus"))).count());
        result.put("resolvedCount", rows.stream().filter(item -> "RESOLVED".equals(item.get("matchStatus"))).count());
        result.put("snapshotAmount", scale(sumRowAmount(rows, "snapshotAmount")));
        result.put("currentAmount", scale(sumRowAmount(rows, "currentAmount")));
        result.put("rows", rows);
        result.put("warnings", buildEmptyWarning(notices, "No cash-flow notices are available for reconciliation."));
        return result;
    }

    private List<BizfiFiInternalNotice> listCounterpartyNotices(String docTypeRoot, String status, String severity, LocalDate asOfDate) {
        LambdaQueryWrapper<BizfiFiInternalNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiInternalNotice::getFnoticeType, NOTICE_COUNTERPARTY);
        if (StringUtils.hasText(docTypeRoot)) {
            wrapper.eq(BizfiFiInternalNotice::getFdocTypeRoot, normalizeRoot(docTypeRoot));
        }
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(BizfiFiInternalNotice::getFstatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(BizfiFiInternalNotice::getFseverity, severity.trim().toUpperCase(Locale.ROOT));
        }
        if (asOfDate != null) {
            wrapper.le(BizfiFiInternalNotice::getFsourceDate, asOfDate);
        }
        wrapper.orderByDesc(BizfiFiInternalNotice::getFupdateTime).orderByDesc(BizfiFiInternalNotice::getFid);
        return sortNotices(noticeMapper.selectList(wrapper));
    }

    private List<BizfiFiInternalNotice> listCashflowNotices(Long orgId, String period, String status, String severity, String sourceCode) {
        LambdaQueryWrapper<BizfiFiInternalNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizfiFiInternalNotice::getFnoticeType, NOTICE_CASHFLOW);
        if (orgId != null) {
            wrapper.eq(BizfiFiInternalNotice::getForgId, orgId);
        }
        if (StringUtils.hasText(period)) {
            wrapper.eq(BizfiFiInternalNotice::getFperiod, period.trim());
        }
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(BizfiFiInternalNotice::getFstatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(BizfiFiInternalNotice::getFseverity, severity.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(sourceCode)) {
            wrapper.eq(BizfiFiInternalNotice::getFsourceCode, sourceCode.trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(BizfiFiInternalNotice::getFupdateTime).orderByDesc(BizfiFiInternalNotice::getFid);
        return sortNotices(noticeMapper.selectList(wrapper));
    }

    private List<NoticeCandidate> buildCounterpartyCandidates(String docTypeRoot, LocalDate asOfDate) {
        Map<String, Object> analysis = arapManageService.agingAnalysis(docTypeRoot, asOfDate);
        List<Map<String, Object>> rows = toMapList(analysis.get("rows"));
        List<NoticeCandidate> candidates = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String counterparty = text(row.get("counterparty"));
            if (!StringUtils.hasText(counterparty)) {
                continue;
            }

            BigDecimal openAmount = decimal(row.get("openAmount"));
            long maxAgeDays = longValue(row.get("maxAgeDays"));
            Set<String> reasons = splitReasons(text(row.get("riskReason")));
            BigDecimal creditLimit = decimalNullable(row.get("creditLimit"));

            if (reasons.contains("OVER_LIMIT")) {
                candidates.add(buildCounterpartyCandidate(docTypeRoot, asOfDate, counterparty, openAmount, maxAgeDays, creditLimit, "OVER_LIMIT"));
            }
            if (reasons.contains("OVERDUE")) {
                candidates.add(buildCounterpartyCandidate(docTypeRoot, asOfDate, counterparty, openAmount, maxAgeDays, creditLimit, "OVERDUE"));
            }
            if (reasons.isEmpty() && openAmount.compareTo(BigDecimal.ZERO) > 0 && maxAgeDays > 60) {
                candidates.add(buildCounterpartyCandidate(docTypeRoot, asOfDate, counterparty, openAmount, maxAgeDays, creditLimit, "OPEN_AGING"));
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparing((NoticeCandidate item) -> severityRank(item.severity))
                        .thenComparing(item -> safe(item.amount), Comparator.reverseOrder())
                        .thenComparing(item -> item.referenceKey))
                .toList();
    }

    private NoticeCandidate buildCounterpartyCandidate(
            String docTypeRoot,
            LocalDate asOfDate,
            String counterparty,
            BigDecimal openAmount,
            long maxAgeDays,
            BigDecimal creditLimit,
            String sourceCode
    ) {
        NoticeCandidate candidate = new NoticeCandidate();
        candidate.noticeType = NOTICE_COUNTERPARTY;
        candidate.docTypeRoot = normalizeRoot(docTypeRoot);
        candidate.period = asOfDate == null ? null : YearMonth.from(asOfDate).toString();
        candidate.referenceType = "COUNTERPARTY_RISK";
        candidate.referenceKey = "COUNTERPARTY|" + candidate.docTypeRoot + "|" + counterparty + "|" + sourceCode;
        candidate.sourceCode = sourceCode;
        candidate.counterparty = counterparty;
        candidate.amount = scale(openAmount);
        candidate.openAmount = scale(openAmount);
        candidate.sourceDate = asOfDate;
        candidate.dueDate = asOfDate == null ? null : asOfDate.plusDays("HIGH".equals(counterpartySeverity(sourceCode, maxAgeDays)) ? 3 : 7);
        candidate.noticeTime = LocalDateTime.now();
        candidate.severity = counterpartySeverity(sourceCode, maxAgeDays);
        candidate.title = counterpartyTitle(candidate.docTypeRoot, sourceCode);
        candidate.message = counterpartyMessage(candidate.docTypeRoot, counterparty, openAmount, maxAgeDays, creditLimit, sourceCode);
        candidate.suggestion = counterpartySuggestion(candidate.docTypeRoot, sourceCode);
        candidate.owner = "counterparty-manager";
        return candidate;
    }

    private List<NoticeCandidate> buildCashflowCandidates(Long orgId, String period, String currency) {
        CashFlowSupplementResultVO supplement = cashFlowService.supplement(orgId, period, currency);
        List<CashFlowSupplementVoucherVO> pendingVouchers = supplement.getPendingVouchers() == null
                ? Collections.emptyList()
                : supplement.getPendingVouchers();
        List<NoticeCandidate> candidates = new ArrayList<>();
        for (CashFlowSupplementVoucherVO voucher : pendingVouchers) {
            String sourceCode = normalizeSourceCode(voucher.getSourceType());
            if (!StringUtils.hasText(sourceCode)) {
                continue;
            }

            NoticeCandidate candidate = new NoticeCandidate();
            candidate.noticeType = NOTICE_CASHFLOW;
            candidate.orgId = orgId;
            candidate.period = normalizePeriod(period);
            candidate.referenceType = "CASHFLOW_VOUCHER";
            candidate.referenceKey = "CASHFLOW|" + text(voucher.getVoucherId()) + "|" + sourceCode;
            candidate.sourceCode = sourceCode;
            candidate.categoryCode = text(voucher.getCategoryName());
            candidate.voucherId = voucher.getVoucherId();
            candidate.voucherNumber = voucher.getVoucherNumber();
            candidate.amount = scale(safe(voucher.getNetAmount()).abs());
            candidate.sourceDate = parseDate(voucher.getVoucherDate());
            candidate.dueDate = candidate.sourceDate == null ? null : candidate.sourceDate.plusDays("HIGH".equals(cashflowSeverity(sourceCode)) ? 1 : 3);
            candidate.noticeTime = LocalDateTime.now();
            candidate.severity = cashflowSeverity(sourceCode);
            candidate.title = cashflowTitle(sourceCode);
            candidate.message = cashflowMessage(voucher);
            candidate.suggestion = text(voucher.getSuggestion());
            candidate.owner = "cashflow-review";
            candidates.add(candidate);
        }

        return candidates.stream()
                .sorted(Comparator.comparing((NoticeCandidate item) -> severityRank(item.severity))
                        .thenComparing(item -> safe(item.amount), Comparator.reverseOrder())
                        .thenComparing(item -> text(item.voucherNumber)))
                .toList();
    }

    private SyncResult syncNotices(
            String noticeType,
            List<BizfiFiInternalNotice> existingNotices,
            List<NoticeCandidate> candidates,
            String operator,
            String docTypeRoot,
            Long orgId,
            String period
    ) {
        Map<String, BizfiFiInternalNotice> existingByRef = existingNotices.stream()
                .collect(Collectors.toMap(BizfiFiInternalNotice::getFreferenceKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<String> activeReferenceKeys = new LinkedHashSet<>();
        SyncResult result = new SyncResult();
        LocalDateTime now = LocalDateTime.now();

        for (NoticeCandidate candidate : candidates) {
            activeReferenceKeys.add(candidate.referenceKey);
            BizfiFiInternalNotice notice = existingByRef.get(candidate.referenceKey);
            if (notice == null) {
                notice = new BizfiFiInternalNotice();
                notice.setFnoticeType(noticeType);
                notice.setFnoticeTime(now);
                result.insertedCount += 1;
            } else {
                result.updatedCount += 1;
            }

            notice.setFdocTypeRoot(docTypeRoot);
            notice.setForgId(orgId);
            notice.setFperiod(period);
            notice.setFstatus(STATUS_OPEN);
            notice.setFseverity(candidate.severity);
            notice.setFreferenceKey(candidate.referenceKey);
            notice.setFreferenceType(candidate.referenceType);
            notice.setFsourceCode(candidate.sourceCode);
            notice.setFcategoryCode(candidate.categoryCode);
            notice.setFcounterparty(candidate.counterparty);
            notice.setFvoucherId(candidate.voucherId);
            notice.setFvoucherNumber(candidate.voucherNumber);
            notice.setFtitle(candidate.title);
            notice.setFmessage(candidate.message);
            notice.setFsuggestion(candidate.suggestion);
            notice.setFamount(scale(candidate.amount));
            notice.setFopenAmount(scale(candidate.openAmount));
            notice.setFsourceDate(candidate.sourceDate);
            notice.setFdueDate(candidate.dueDate);
            notice.setFowner(candidate.owner);
            notice.setFupdateTime(now);
            notice.setFresolvedTime(null);
            notice.setFresolveNote(null);

            if (notice.getFid() == null) {
                noticeMapper.insert(notice);
            } else {
                noticeMapper.updateById(notice);
            }
        }

        for (BizfiFiInternalNotice notice : existingNotices) {
            if (!STATUS_OPEN.equals(notice.getFstatus()) || activeReferenceKeys.contains(notice.getFreferenceKey())) {
                continue;
            }
            notice.setFstatus(STATUS_RESOLVED);
            notice.setFresolvedTime(now);
            notice.setFresolveNote("Auto-resolved by the latest notice generation scan. Operator: " + operator);
            notice.setFupdateTime(now);
            noticeMapper.updateById(notice);
            result.resolvedCount += 1;
        }
        return result;
    }

    private Map<String, Object> toCounterpartyNoticeRow(BizfiFiInternalNotice notice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fid", notice.getFid());
        row.put("status", notice.getFstatus());
        row.put("severity", notice.getFseverity());
        row.put("docTypeRoot", notice.getFdocTypeRoot());
        row.put("sourceCode", notice.getFsourceCode());
        row.put("counterparty", notice.getFcounterparty());
        row.put("title", notice.getFtitle());
        row.put("message", notice.getFmessage());
        row.put("suggestion", notice.getFsuggestion());
        row.put("amount", notice.getFamount());
        row.put("openAmount", notice.getFopenAmount());
        row.put("sourceDate", formatDate(notice.getFsourceDate()));
        row.put("dueDate", formatDate(notice.getFdueDate()));
        row.put("noticeTime", formatDateTime(notice.getFnoticeTime()));
        row.put("updateTime", formatDateTime(notice.getFupdateTime()));
        row.put("resolvedTime", formatDateTime(notice.getFresolvedTime()));
        row.put("resolveNote", notice.getFresolveNote());
        return row;
    }

    private Map<String, Object> toCounterpartyReconcileRow(BizfiFiInternalNotice notice, NoticeCandidate current) {
        BigDecimal snapshotOpenAmount = scale(notice.getFopenAmount());
        BigDecimal currentOpenAmount = current == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(current.openAmount);
        Map<String, Object> row = toCounterpartyNoticeRow(notice);
        row.put("matchStatus", current == null ? "RESOLVED" : "ONGOING");
        row.put("snapshotOpenAmount", snapshotOpenAmount);
        row.put("currentOpenAmount", currentOpenAmount);
        row.put("improvementAmount", scale(snapshotOpenAmount.subtract(currentOpenAmount)));
        row.put("currentSeverity", current == null ? null : current.severity);
        row.put("currentMessage", current == null ? "Current scan no longer finds this issue." : current.message);
        row.put("currentSuggestion", current == null ? "Regenerate notices to auto-close this issue." : current.suggestion);
        return row;
    }

    private Map<String, Object> toCashflowNoticeRow(BizfiFiInternalNotice notice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fid", notice.getFid());
        row.put("status", notice.getFstatus());
        row.put("severity", notice.getFseverity());
        row.put("orgId", notice.getForgId());
        row.put("period", notice.getFperiod());
        row.put("sourceCode", notice.getFsourceCode());
        row.put("categoryCode", notice.getFcategoryCode());
        row.put("voucherId", notice.getFvoucherId());
        row.put("voucherNumber", notice.getFvoucherNumber());
        row.put("title", notice.getFtitle());
        row.put("message", notice.getFmessage());
        row.put("suggestion", notice.getFsuggestion());
        row.put("amount", notice.getFamount());
        row.put("sourceDate", formatDate(notice.getFsourceDate()));
        row.put("dueDate", formatDate(notice.getFdueDate()));
        row.put("noticeTime", formatDateTime(notice.getFnoticeTime()));
        row.put("updateTime", formatDateTime(notice.getFupdateTime()));
        row.put("resolvedTime", formatDateTime(notice.getFresolvedTime()));
        row.put("resolveNote", notice.getFresolveNote());
        return row;
    }

    private Map<String, Object> toCashflowReconcileRow(BizfiFiInternalNotice notice, NoticeCandidate current) {
        BigDecimal snapshotAmount = scale(notice.getFamount());
        BigDecimal currentAmount = current == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(current.amount);
        Map<String, Object> row = toCashflowNoticeRow(notice);
        row.put("matchStatus", current == null ? "RESOLVED" : "ONGOING");
        row.put("snapshotAmount", snapshotAmount);
        row.put("currentAmount", currentAmount);
        row.put("currentSourceCode", current == null ? null : current.sourceCode);
        row.put("currentCategoryCode", current == null ? null : current.categoryCode);
        row.put("currentMessage", current == null ? "Current scan no longer finds this pending cash-flow issue." : current.message);
        row.put("currentSuggestion", current == null ? "Regenerate notices to auto-close this issue." : current.suggestion);
        return row;
    }

    private String normalizeRoot(String docTypeRoot) {
        String root = StringUtils.hasText(docTypeRoot) ? docTypeRoot.trim().toUpperCase(Locale.ROOT) : "AR";
        if (!Set.of("AR", "AP").contains(root)) {
            throw new BizException("docTypeRoot only supports AR or AP");
        }
        return root;
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(period.trim()).toString();
        } catch (DateTimeParseException ex) {
            throw new BizException("period must use yyyy-MM, for example 2026-03");
        }
    }

    private String normalizeSourceCode(String sourceCode) {
        return StringUtils.hasText(sourceCode) ? sourceCode.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String counterpartyTitle(String root, String sourceCode) {
        if ("OVER_LIMIT".equals(sourceCode)) {
            return rootLabel(root) + "超额度通知单";
        }
        if ("OVERDUE".equals(sourceCode)) {
            return rootLabel(root) + "超期通知单";
        }
        return rootLabel(root) + "长期未清通知单";
    }

    private String counterpartyMessage(String root, String counterparty, BigDecimal openAmount, long maxAgeDays, BigDecimal creditLimit, String sourceCode) {
        if ("OVER_LIMIT".equals(sourceCode)) {
            return rootLabel(root) + "往来方 " + counterparty + " 当前未清金额 " + scale(openAmount).toPlainString()
                    + "，已超过信用/付款控制额度 " + (creditLimit == null ? "-" : scale(creditLimit).toPlainString()) + "。";
        }
        if ("OVERDUE".equals(sourceCode)) {
            return rootLabel(root) + "往来方 " + counterparty + " 当前最大账龄 " + maxAgeDays + " 天，存在超期未清项。";
        }
        return rootLabel(root) + "往来方 " + counterparty + " 当前未清金额 " + scale(openAmount).toPlainString()
                + "，最大账龄 " + maxAgeDays + " 天，建议优先清理长期未清项目。";
    }

    private String counterpartySuggestion(String root, String sourceCode) {
        if ("OVER_LIMIT".equals(sourceCode)) {
            return "请结合" + rootLabel(root) + "对账单核实差异，并安排优先回款/付款计划。";
        }
        if ("OVERDUE".equals(sourceCode)) {
            return "请核查未清项目、补充结算单据或执行往来自动核销。";
        }
        return "请关注长期未清往来，并确认是否需要新增结算、付款或核销处理。";
    }

    private String counterpartySeverity(String sourceCode, long maxAgeDays) {
        if ("OVER_LIMIT".equals(sourceCode)) {
            return "HIGH";
        }
        if ("OVERDUE".equals(sourceCode)) {
            return maxAgeDays > 90 ? "HIGH" : "MEDIUM";
        }
        return "MEDIUM";
    }

    private String cashflowTitle(String sourceCode) {
        if ("UNKNOWN_ITEM".equals(sourceCode)) {
            return "现金流未知编码通知单";
        }
        if ("MIXED_ITEM".equals(sourceCode)) {
            return "现金流多编码复核通知单";
        }
        if ("HEURISTIC".equals(sourceCode)) {
            return "现金流规则推断复核通知单";
        }
        return "现金流内部划转通知单";
    }

    private String cashflowMessage(CashFlowSupplementVoucherVO voucher) {
        return "凭证 " + text(voucher.getVoucherNumber()) + " / " + text(voucher.getSummary()) + "，问题：" + text(voucher.getIssue());
    }

    private String cashflowSeverity(String sourceCode) {
        if ("UNKNOWN_ITEM".equals(sourceCode) || "MIXED_ITEM".equals(sourceCode)) {
            return "HIGH";
        }
        if ("HEURISTIC".equals(sourceCode)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String rootLabel(String root) {
        return "AP".equals(root) ? "应付" : "应收";
    }

    private List<BizfiFiInternalNotice> sortNotices(List<BizfiFiInternalNotice> notices) {
        return notices.stream()
                .sorted(Comparator.comparing((BizfiFiInternalNotice item) -> statusRank(item.getFstatus()))
                        .thenComparing(item -> severityRank(item.getFseverity()))
                        .thenComparing(item -> item.getFupdateTime(), Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .toList();
    }

    private int statusRank(String status) {
        return STATUS_OPEN.equals(status) ? 2 : 1;
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) {
            return 3;
        }
        if ("MEDIUM".equals(severity)) {
            return 2;
        }
        return 1;
    }

    private BigDecimal sumAmount(Collection<BizfiFiInternalNotice> notices, java.util.function.Function<BizfiFiInternalNotice, BigDecimal> mapper) {
        return notices.stream().map(mapper).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRowAmount(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(item -> decimal(item.get(key))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<String> buildEmptyWarning(List<?> rows, String message) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>(List.of(message));
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> toMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((key, mapValue) -> row.put(String.valueOf(key), mapValue));
                rows.add(row);
            }
        }
        return rows;
    }

    private Set<String> splitReasons(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptySet();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text.trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimalNullable(Object value) {
        return value == null ? null : decimal(value);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private static class NoticeCandidate {
        private String noticeType;
        private String docTypeRoot;
        private Long orgId;
        private String period;
        private String referenceKey;
        private String referenceType;
        private String sourceCode;
        private String categoryCode;
        private String counterparty;
        private Long voucherId;
        private String voucherNumber;
        private String severity;
        private String title;
        private String message;
        private String suggestion;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal openAmount = BigDecimal.ZERO;
        private LocalDate sourceDate;
        private LocalDate dueDate;
        private LocalDateTime noticeTime;
        private String owner;
    }

    private static class SyncResult {
        private int insertedCount;
        private int updatedCount;
        private int resolvedCount;
    }
}
