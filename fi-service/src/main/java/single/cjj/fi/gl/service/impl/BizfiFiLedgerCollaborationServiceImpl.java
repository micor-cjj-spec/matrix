package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.mapper.BizfiFiArapDocMapper;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherLine;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherLineMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiLedgerCollaborationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BizfiFiLedgerCollaborationServiceImpl implements BizfiFiLedgerCollaborationService {

    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_REVERSED = "REVERSED";
    private static final Set<String> HISTORY_VOUCHER_STATUSES = Set.of(STATUS_POSTED, STATUS_REVERSED);
    private static final Pattern REVERSE_TARGET_PATTERN = Pattern.compile("已冲销到凭证:([^；\\s]+)");
    private static final Pattern REVERSE_SOURCE_ID_PATTERN = Pattern.compile("冲销原凭证ID=([0-9]+)");

    private static final List<RuleSpec> RULE_SPECS = List.of(
            new RuleSpec("AR", "AR", "应收单据转凭证", "1122", "6001", "单据转凭证"),
            new RuleSpec("AR", "AR_ESTIMATE", "暂估应收转凭证", "1122", "2202", "单据转凭证"),
            new RuleSpec("AR", "AR_SETTLEMENT", "应收结算转凭证", "1002", "1122", "单据转凭证"),
            new RuleSpec("AP", "AP", "应付单据转凭证", "6001", "2202", "单据转凭证"),
            new RuleSpec("AP", "AP_ESTIMATE", "暂估应付转凭证", "1405", "2202", "单据转凭证"),
            new RuleSpec("AP", "AP_PAYMENT_APPLY", "付款申请转凭证", "2202", "2241", "单据转凭证"),
            new RuleSpec("AP", "AP_PAYMENT_PROCESS", "付款处理转凭证", "2241", "1002", "单据转凭证")
    );

    @Autowired
    private BizfiFiVoucherMapper voucherMapper;

    @Autowired
    private BizfiFiVoucherLineMapper voucherLineMapper;

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Autowired
    private BizfiFiArapDocMapper arapDocMapper;

    @Autowired
    private BizfiFiAccountMapper accountMapper;

    @Override
    public Map<String, Object> voucherRules(String docTypeRoot, String keyword) {
        String root = normalizeDocTypeRoot(docTypeRoot);
        String keywordText = trimToNull(keyword);

        List<BizfiFiArapDoc> docs = listArapDocs(root);
        Map<String, List<BizfiFiArapDoc>> docsByType = docs.stream()
                .filter(item -> StringUtils.hasText(item.getFdoctype()))
                .collect(Collectors.groupingBy(item -> item.getFdoctype().trim().toUpperCase(Locale.ROOT), LinkedHashMap::new, Collectors.toList()));

        Set<String> accountCodes = RULE_SPECS.stream()
                .filter(item -> root == null || root.equals(item.docTypeRoot))
                .flatMap(item -> Arrays.stream(new String[]{item.debitAccountCode, item.creditAccountCode}))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (RuleSpec spec : RULE_SPECS) {
            if (root != null && !root.equals(spec.docTypeRoot)) {
                continue;
            }

            List<BizfiFiArapDoc> typeDocs = docsByType.getOrDefault(spec.docType, List.of());
            long auditedCount = typeDocs.stream().filter(this::isAuditedOrBeyond).count();
            long generatedCount = typeDocs.stream().filter(item -> item.getFvoucherId() != null || StringUtils.hasText(item.getFvoucherNumber())).count();
            long pendingCount = Math.max(0, auditedCount - generatedCount);
            LocalDate lastDocDate = typeDocs.stream()
                    .map(BizfiFiArapDoc::getFdate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("docTypeRoot", spec.docTypeRoot);
            row.put("docType", spec.docType);
            row.put("ruleName", spec.ruleName);
            row.put("summaryPrefix", spec.summaryPrefix);
            row.put("source", "BUILTIN");
            row.put("enabled", Boolean.TRUE);
            row.put("debitAccountCode", spec.debitAccountCode);
            row.put("debitAccountName", accountNameOf(spec.debitAccountCode, accountMap));
            row.put("creditAccountCode", spec.creditAccountCode);
            row.put("creditAccountName", accountNameOf(spec.creditAccountCode, accountMap));
            row.put("docCount", typeDocs.size());
            row.put("auditedCount", auditedCount);
            row.put("generatedCount", generatedCount);
            row.put("pendingCount", pendingCount);
            row.put("coverageRate", auditedCount == 0 ? "0.00%" : percent(generatedCount, auditedCount));
            row.put("lastDocDate", formatDate(lastDocDate));
            row.put("note", "当前规则来自往来单据自动生成凭证的内置映射，第一期先提供可视化与覆盖率检查。");

            if (matchesKeyword(row, keywordText)) {
                rows.add(row);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("docTypeRoot", root);
        result.put("ruleCount", rows.size());
        result.put("auditedCount", rows.stream().mapToLong(item -> longValue(item.get("auditedCount"))).sum());
        result.put("generatedCount", rows.stream().mapToLong(item -> longValue(item.get("generatedCount"))).sum());
        result.put("pendingCount", rows.stream().mapToLong(item -> longValue(item.get("pendingCount"))).sum());
        result.put("rows", rows);
        result.put("warnings", rows.isEmpty()
                ? new ArrayList<>(List.of("当前条件下没有可展示的折算规则。"))
                : new ArrayList<>(List.of("一期规则页展示的是系统当前内置的单据转凭证映射，尚未开放在线修改。")));
        return result;
    }

    @Override
    public Map<String, Object> offsetVouchers(String startDate, String endDate, String matchStatus, String keyword) {
        QueryRange range = resolveRange(startDate, endDate);
        String statusFilter = normalizeMatchStatus(matchStatus);
        String keywordText = trimToNull(keyword);

        List<BizfiFiVoucher> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(BizfiFiVoucher::getFdate, range.startDate)
                .le(BizfiFiVoucher::getFdate, range.endDate)
                .orderByDesc(BizfiFiVoucher::getFdate)
                .orderByDesc(BizfiFiVoucher::getFid));

        Map<Long, BizfiFiVoucher> voucherById = vouchers.stream()
                .filter(item -> item.getFid() != null)
                .collect(Collectors.toMap(BizfiFiVoucher::getFid, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, BizfiFiVoucher> voucherByNumber = vouchers.stream()
                .filter(item -> StringUtils.hasText(item.getFnumber()))
                .collect(Collectors.toMap(item -> item.getFnumber().trim(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<Long> handledOriginalIds = new LinkedHashSet<>();

        for (BizfiFiVoucher voucher : vouchers) {
            if (!isReverseVoucher(voucher)) {
                continue;
            }
            BizfiFiVoucher original = findOriginalVoucher(voucher, voucherById, voucherByNumber);
            Map<String, Object> row = toOffsetRow(original, voucher);
            if (matchesMatchStatus(row, statusFilter) && matchesKeyword(row, keywordText)) {
                rows.add(row);
            }
            if (original != null && original.getFid() != null) {
                handledOriginalIds.add(original.getFid());
            }
        }

        for (BizfiFiVoucher voucher : vouchers) {
            if (!STATUS_REVERSED.equalsIgnoreCase(text(voucher.getFstatus())) || handledOriginalIds.contains(voucher.getFid())) {
                continue;
            }
            BizfiFiVoucher reverse = findReverseVoucher(voucher, voucherByNumber);
            Map<String, Object> row = toOffsetRow(voucher, reverse);
            if (matchesMatchStatus(row, statusFilter) && matchesKeyword(row, keywordText)) {
                rows.add(row);
            }
        }

        rows.sort(Comparator.comparing((Map<String, Object> item) -> text(item.get("originalDate")), Comparator.nullsLast(String::compareTo)).reversed()
                .thenComparing((Map<String, Object> item) -> text(item.get("originalNumber")), Comparator.nullsLast(String::compareTo)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", formatDate(range.startDate));
        result.put("endDate", formatDate(range.endDate));
        result.put("pairCount", rows.stream().filter(item -> "PAIRED".equals(item.get("matchStatus"))).count());
        result.put("orphanCount", rows.stream().filter(item -> "ORPHAN".equals(item.get("matchStatus"))).count());
        result.put("originalAmount", scale(sumRowAmount(rows, "originalAmount")));
        result.put("reverseAmount", scale(sumRowAmount(rows, "reverseAmount")));
        result.put("rows", rows);
        result.put("warnings", rows.isEmpty()
                ? new ArrayList<>(List.of("当前期间内没有找到对冲凭证记录。"))
                : new ArrayList<>(List.of("对冲凭证页依据冲销摘要和备注里的关联信息进行配对，若历史数据不完整会显示孤儿记录。")));
        return result;
    }

    @Override
    public Map<String, Object> voucherChecks(String startDate, String endDate, String issueCode, String severity, String status, Boolean onlyIssue) {
        QueryRange range = resolveRange(startDate, endDate);
        String issueFilter = normalizeUpper(issueCode);
        String severityFilter = normalizeUpper(severity);
        String statusFilter = normalizeUpper(status);
        boolean issueOnly = Boolean.TRUE.equals(onlyIssue);

        List<BizfiFiVoucher> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(BizfiFiVoucher::getFdate, range.startDate)
                .le(BizfiFiVoucher::getFdate, range.endDate)
                .orderByDesc(BizfiFiVoucher::getFdate)
                .orderByDesc(BizfiFiVoucher::getFid));

        if (StringUtils.hasText(statusFilter) && !"ALL".equals(statusFilter)) {
            vouchers = vouchers.stream()
                    .filter(item -> statusFilter.equalsIgnoreCase(text(item.getFstatus())))
                    .toList();
        }

        List<Long> voucherIds = vouchers.stream()
                .map(BizfiFiVoucher::getFid)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<BizfiFiVoucherLine>> linesByVoucherId = listVoucherLines(voucherIds).stream()
                .collect(Collectors.groupingBy(BizfiFiVoucherLine::getFvoucherId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<BizfiFiGlEntry>> entriesByVoucherId = listGlEntries(voucherIds).stream()
                .collect(Collectors.groupingBy(BizfiFiGlEntry::getFvoucherId, LinkedHashMap::new, Collectors.toList()));
        Map<String, Long> duplicateCountByNumber = vouchers.stream()
                .filter(item -> StringUtils.hasText(item.getFnumber()))
                .collect(Collectors.groupingBy(item -> item.getFnumber().trim(), LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (BizfiFiVoucher voucher : vouchers) {
            List<Map<String, Object>> issues = buildVoucherIssues(
                    voucher,
                    linesByVoucherId.getOrDefault(voucher.getFid(), List.of()),
                    entriesByVoucherId.getOrDefault(voucher.getFid(), List.of()),
                    duplicateCountByNumber.getOrDefault(text(voucher.getFnumber()), 0L)
            );

            if (issues.isEmpty() && !issueOnly) {
                rows.add(toHealthyVoucherRow(voucher,
                        linesByVoucherId.getOrDefault(voucher.getFid(), List.of()),
                        entriesByVoucherId.getOrDefault(voucher.getFid(), List.of())));
                continue;
            }

            for (Map<String, Object> issue : issues) {
                if (StringUtils.hasText(issueFilter) && !"ALL".equals(issueFilter) && !issueFilter.equals(issue.get("issueCode"))) {
                    continue;
                }
                if (StringUtils.hasText(severityFilter) && !"ALL".equals(severityFilter) && !severityFilter.equals(issue.get("severity"))) {
                    continue;
                }
                rows.add(issue);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", formatDate(range.startDate));
        result.put("endDate", formatDate(range.endDate));
        result.put("voucherCount", vouchers.size());
        result.put("issueCount", rows.stream().filter(item -> !"OK".equals(item.get("issueCode"))).count());
        result.put("issueVoucherCount", rows.stream()
                .filter(item -> !"OK".equals(item.get("issueCode")))
                .map(item -> item.get("voucherId"))
                .filter(Objects::nonNull)
                .distinct()
                .count());
        result.put("highCount", rows.stream().filter(item -> "HIGH".equals(item.get("severity"))).count());
        result.put("healthyCount", rows.stream().filter(item -> "OK".equals(item.get("issueCode"))).count());
        result.put("rows", rows);
        result.put("warnings", rows.isEmpty()
                ? new ArrayList<>(List.of("当前条件下没有发现协同检查结果。"))
                : new ArrayList<>(List.of("协同检查一期重点校验表头金额、分录借贷平衡、过账分录和重复凭证号。")));
        return result;
    }

    @Override
    public Map<String, Object> subjectBalanceCompare(String startDate, String endDate, String accountCode, Boolean diffOnly) {
        QueryRange range = resolveRange(startDate, endDate);
        String accountFilter = trimToNull(accountCode);
        boolean onlyDiff = Boolean.TRUE.equals(diffOnly);

        VoucherMovementAggregate voucherOpening = aggregateVoucherMovementsBefore(range.startDate, accountFilter);
        VoucherMovementAggregate voucherPeriod = aggregateVoucherMovementsInRange(range.startDate, range.endDate, accountFilter);
        GlMovementAggregate glOpening = aggregateGlMovementsBefore(range.startDate, accountFilter);
        GlMovementAggregate glPeriod = aggregateGlMovementsInRange(range.startDate, range.endDate, accountFilter);

        Set<String> accountCodes = new LinkedHashSet<>();
        accountCodes.addAll(voucherOpening.netByAccount.keySet());
        accountCodes.addAll(voucherPeriod.debitByAccount.keySet());
        accountCodes.addAll(voucherPeriod.creditByAccount.keySet());
        accountCodes.addAll(glOpening.netByAccount.keySet());
        accountCodes.addAll(glPeriod.debitByAccount.keySet());
        accountCodes.addAll(glPeriod.creditByAccount.keySet());
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String code : accountCodes.stream().sorted().toList()) {
            BigDecimal voucherOpeningNet = scale(voucherOpening.netByAccount.get(code));
            BigDecimal voucherPeriodDebit = scale(voucherPeriod.debitByAccount.get(code));
            BigDecimal voucherPeriodCredit = scale(voucherPeriod.creditByAccount.get(code));
            BigDecimal voucherClosingNet = scale(voucherOpeningNet.add(voucherPeriodDebit).subtract(voucherPeriodCredit));

            BigDecimal glOpeningNet = scale(glOpening.netByAccount.get(code));
            BigDecimal glPeriodDebit = scale(glPeriod.debitByAccount.get(code));
            BigDecimal glPeriodCredit = scale(glPeriod.creditByAccount.get(code));
            BigDecimal glClosingNet = scale(glOpeningNet.add(glPeriodDebit).subtract(glPeriodCredit));

            BigDecimal openingDiff = scale(voucherOpeningNet.subtract(glOpeningNet));
            BigDecimal debitDiff = scale(voucherPeriodDebit.subtract(glPeriodDebit));
            BigDecimal creditDiff = scale(voucherPeriodCredit.subtract(glPeriodCredit));
            BigDecimal closingDiff = scale(voucherClosingNet.subtract(glClosingNet));
            boolean hasDiff = isNonZero(openingDiff) || isNonZero(debitDiff) || isNonZero(creditDiff) || isNonZero(closingDiff);
            if (onlyDiff && !hasDiff) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accountCode", code);
            row.put("accountName", accountNameOf(code, accountMap));
            row.put("voucherOpeningBalance", voucherOpeningNet);
            row.put("glOpeningBalance", glOpeningNet);
            row.put("openingDiff", openingDiff);
            row.put("voucherPeriodDebit", voucherPeriodDebit);
            row.put("glPeriodDebit", glPeriodDebit);
            row.put("periodDebitDiff", debitDiff);
            row.put("voucherPeriodCredit", voucherPeriodCredit);
            row.put("glPeriodCredit", glPeriodCredit);
            row.put("periodCreditDiff", creditDiff);
            row.put("voucherClosingBalance", voucherClosingNet);
            row.put("glClosingBalance", glClosingNet);
            row.put("closingDiff", closingDiff);
            row.put("voucherLineCount", voucherPeriod.countByAccount.getOrDefault(code, 0L));
            row.put("glEntryCount", glPeriod.countByAccount.getOrDefault(code, 0L));
            row.put("matchStatus", hasDiff ? "DIFF" : "MATCH");
            row.put("differenceReason", hasDiff
                    ? "凭证分录汇总与总账分录汇总存在差异，请重点检查过账、冲销和历史修正。"
                    : "当前期间凭证分录与总账分录保持一致。");
            rows.add(row);
        }

        rows.sort(Comparator.comparing(item -> text(item.get("accountCode")), Comparator.nullsLast(String::compareTo)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", formatDate(range.startDate));
        result.put("endDate", formatDate(range.endDate));
        result.put("accountCount", rows.size());
        result.put("diffAccountCount", rows.stream().filter(item -> "DIFF".equals(item.get("matchStatus"))).count());
        result.put("voucherDebitTotal", scale(sumRowAmount(rows, "voucherPeriodDebit")));
        result.put("glDebitTotal", scale(sumRowAmount(rows, "glPeriodDebit")));
        result.put("voucherCreditTotal", scale(sumRowAmount(rows, "voucherPeriodCredit")));
        result.put("glCreditTotal", scale(sumRowAmount(rows, "glPeriodCredit")));
        result.put("rows", rows);
        List<String> warnings = new ArrayList<>();
        if (rows.isEmpty()) {
            warnings.add("当前条件下没有科目余额对照数据。");
        } else {
            warnings.add("一期对照页按凭证分录与总账分录两套口径做比对，尚未引入组织和账簿维度隔离。");
        }
        result.put("warnings", warnings);
        return result;
    }

    private List<BizfiFiArapDoc> listArapDocs(String root) {
        LambdaQueryWrapper<BizfiFiArapDoc> wrapper = new LambdaQueryWrapper<>();
        if ("AR".equals(root)) {
            wrapper.likeRight(BizfiFiArapDoc::getFdoctype, "AR");
        } else if ("AP".equals(root)) {
            wrapper.likeRight(BizfiFiArapDoc::getFdoctype, "AP");
        }
        wrapper.orderByDesc(BizfiFiArapDoc::getFdate).orderByDesc(BizfiFiArapDoc::getFid);
        return arapDocMapper.selectList(wrapper);
    }

    private boolean isAuditedOrBeyond(BizfiFiArapDoc doc) {
        String status = text(doc.getFstatus());
        return Set.of("AUDITED", STATUS_POSTED, STATUS_REVERSED).contains(status);
    }

    private boolean isReverseVoucher(BizfiFiVoucher voucher) {
        return startsWith(text(voucher.getFnumber()), "RV-")
                || startsWith(text(voucher.getFsummary()), "冲销:")
                || REVERSE_SOURCE_ID_PATTERN.matcher(text(voucher.getFremark())).find();
    }

    private BizfiFiVoucher findOriginalVoucher(BizfiFiVoucher reverseVoucher,
                                               Map<Long, BizfiFiVoucher> voucherById,
                                               Map<String, BizfiFiVoucher> voucherByNumber) {
        Matcher idMatcher = REVERSE_SOURCE_ID_PATTERN.matcher(text(reverseVoucher.getFremark()));
        if (idMatcher.find()) {
            Long originalId = longNullable(idMatcher.group(1));
            if (originalId != null && voucherById.containsKey(originalId)) {
                return voucherById.get(originalId);
            }
        }

        String reverseNumber = text(reverseVoucher.getFnumber());
        if (startsWith(reverseNumber, "RV-")) {
            String originalNumber = reverseNumber.substring(3);
            if (voucherByNumber.containsKey(originalNumber)) {
                return voucherByNumber.get(originalNumber);
            }
        }
        return null;
    }

    private BizfiFiVoucher findReverseVoucher(BizfiFiVoucher originalVoucher, Map<String, BizfiFiVoucher> voucherByNumber) {
        Matcher matcher = REVERSE_TARGET_PATTERN.matcher(text(originalVoucher.getFremark()));
        if (matcher.find()) {
            String reverseNumber = matcher.group(1);
            if (voucherByNumber.containsKey(reverseNumber)) {
                return voucherByNumber.get(reverseNumber);
            }
        }
        String generatedNumber = "RV-" + text(originalVoucher.getFnumber());
        return voucherByNumber.get(generatedNumber);
    }

    private Map<String, Object> toOffsetRow(BizfiFiVoucher original, BizfiFiVoucher reverse) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("matchStatus", original != null && reverse != null ? "PAIRED" : "ORPHAN");
        row.put("originalId", original == null ? null : original.getFid());
        row.put("originalNumber", original == null ? null : original.getFnumber());
        row.put("originalDate", original == null ? null : formatDate(original.getFdate()));
        row.put("originalStatus", original == null ? null : original.getFstatus());
        row.put("originalAmount", original == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(original.getFamount()));
        row.put("reverseId", reverse == null ? null : reverse.getFid());
        row.put("reverseNumber", reverse == null ? null : reverse.getFnumber());
        row.put("reverseDate", reverse == null ? null : formatDate(reverse.getFdate()));
        row.put("reverseStatus", reverse == null ? null : reverse.getFstatus());
        row.put("reverseAmount", reverse == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(reverse.getFamount()));
        row.put("amountDiff", scale(decimal(row.get("originalAmount")).subtract(decimal(row.get("reverseAmount")))));
        row.put("remark", reverse != null ? reverse.getFremark() : original == null ? null : original.getFremark());
        row.put("message", original != null && reverse != null
                ? "原凭证与对冲凭证已配对，可追踪完整冲销链路。"
                : "当前记录缺少原凭证或对冲凭证，请检查历史备注和编号链路。");
        return row;
    }

    private boolean matchesMatchStatus(Map<String, Object> row, String statusFilter) {
        return !StringUtils.hasText(statusFilter)
                || "ALL".equals(statusFilter)
                || statusFilter.equals(row.get("matchStatus"));
    }

    private List<Map<String, Object>> buildVoucherIssues(BizfiFiVoucher voucher,
                                                         List<BizfiFiVoucherLine> lines,
                                                         List<BizfiFiGlEntry> entries,
                                                         long duplicateCount) {
        BigDecimal lineDebit = sumVoucherLineAmount(lines, BizfiFiVoucherLine::getFdebitAmount);
        BigDecimal lineCredit = sumVoucherLineAmount(lines, BizfiFiVoucherLine::getFcreditAmount);
        BigDecimal glDebit = sumGlEntryAmount(entries, BizfiFiGlEntry::getFdebitAmount);
        BigDecimal glCredit = sumGlEntryAmount(entries, BizfiFiGlEntry::getFcreditAmount);
        List<Map<String, Object>> issues = new ArrayList<>();

        if (lines.isEmpty()) {
            issues.add(issueRow(voucher, "MISSING_LINES", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                    "凭证缺少分录，不能形成完整账务记录。", "请先补齐分录后再提交、审核或过账。"));
        }

        if (!lines.isEmpty() && isNonZero(lineDebit.subtract(lineCredit))) {
            issues.add(issueRow(voucher, "LINE_NOT_BALANCED", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                    "分录借贷不平衡。", "请检查借贷方向和金额，确保借方合计等于贷方合计。"));
        }

        if (!lines.isEmpty() && isNonZero(scale(safeAmount(voucher.getFamount()).subtract(lineDebit)))) {
            issues.add(issueRow(voucher, "HEADER_LINE_AMOUNT", "MEDIUM", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                    "表头金额与分录借方合计不一致。", "建议以分录平衡结果回填表头金额，避免后续汇总失真。"));
        }

        if (duplicateCount > 1) {
            issues.add(issueRow(voucher, "DUPLICATE_NUMBER", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                    "当前期间存在重复凭证号。", "请核查编号生成规则或手工录入，避免冲销和对照链路混乱。"));
        }

        String status = text(voucher.getFstatus());
        if (HISTORY_VOUCHER_STATUSES.contains(status)) {
            if (entries.isEmpty()) {
                issues.add(issueRow(voucher, "MISSING_GL_ENTRY", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                        "已过账/已冲销凭证缺少总账分录。", "请重新过账或检查总账分录是否被误删。"));
            } else {
                if (entries.size() != lines.size()) {
                    issues.add(issueRow(voucher, "GL_LINE_COUNT_DIFF", "MEDIUM", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                            "总账分录条数与凭证分录条数不一致。", "请检查是否存在重复过账或历史修复造成的残留分录。"));
                }
                if (isNonZero(lineDebit.subtract(glDebit)) || isNonZero(lineCredit.subtract(glCredit))) {
                    issues.add(issueRow(voucher, "GL_LINE_MISMATCH", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                            "总账分录金额与凭证分录金额不一致。", "请核对过账过程和冲销链路，必要时重新过账。"));
                }
            }
        } else if (!entries.isEmpty()) {
            issues.add(issueRow(voucher, "PREMATURE_GL_ENTRY", "HIGH", lines, entries, lineDebit, lineCredit, glDebit, glCredit,
                    "未完成过账的凭证已存在总账分录。", "请检查历史数据修复或异常写入，避免账表提前反映。"));
        }

        return issues;
    }

    private Map<String, Object> issueRow(BizfiFiVoucher voucher,
                                         String issueCode,
                                         String severity,
                                         List<BizfiFiVoucherLine> lines,
                                         List<BizfiFiGlEntry> entries,
                                         BigDecimal lineDebit,
                                         BigDecimal lineCredit,
                                         BigDecimal glDebit,
                                         BigDecimal glCredit,
                                         String message,
                                         String suggestion) {
        Map<String, Object> row = baseVoucherCheckRow(voucher, lines, entries, lineDebit, lineCredit, glDebit, glCredit);
        row.put("issueCode", issueCode);
        row.put("severity", severity);
        row.put("message", message);
        row.put("suggestion", suggestion);
        return row;
    }

    private Map<String, Object> toHealthyVoucherRow(BizfiFiVoucher voucher,
                                                    List<BizfiFiVoucherLine> lines,
                                                    List<BizfiFiGlEntry> entries) {
        BigDecimal lineDebit = sumVoucherLineAmount(lines, BizfiFiVoucherLine::getFdebitAmount);
        BigDecimal lineCredit = sumVoucherLineAmount(lines, BizfiFiVoucherLine::getFcreditAmount);
        BigDecimal glDebit = sumGlEntryAmount(entries, BizfiFiGlEntry::getFdebitAmount);
        BigDecimal glCredit = sumGlEntryAmount(entries, BizfiFiGlEntry::getFcreditAmount);
        Map<String, Object> row = baseVoucherCheckRow(voucher, lines, entries, lineDebit, lineCredit, glDebit, glCredit);
        row.put("issueCode", "OK");
        row.put("severity", "LOW");
        row.put("message", "当前凭证未发现协同异常。");
        row.put("suggestion", "可继续保持现有处理流程。");
        return row;
    }

    private Map<String, Object> baseVoucherCheckRow(BizfiFiVoucher voucher,
                                                    List<BizfiFiVoucherLine> lines,
                                                    List<BizfiFiGlEntry> entries,
                                                    BigDecimal lineDebit,
                                                    BigDecimal lineCredit,
                                                    BigDecimal glDebit,
                                                    BigDecimal glCredit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("voucherId", voucher.getFid());
        row.put("voucherNumber", voucher.getFnumber());
        row.put("voucherDate", formatDate(voucher.getFdate()));
        row.put("status", voucher.getFstatus());
        row.put("summary", voucher.getFsummary());
        row.put("headerAmount", scale(voucher.getFamount()));
        row.put("lineCount", lines.size());
        row.put("entryCount", entries.size());
        row.put("lineDebit", scale(lineDebit));
        row.put("lineCredit", scale(lineCredit));
        row.put("glDebit", scale(glDebit));
        row.put("glCredit", scale(glCredit));
        row.put("remark", voucher.getFremark());
        return row;
    }

    private VoucherMovementAggregate aggregateVoucherMovementsBefore(LocalDate startDate, String accountCode) {
        return aggregateVoucherMovements(voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .lt(BizfiFiVoucher::getFdate, startDate)
                .in(BizfiFiVoucher::getFstatus, HISTORY_VOUCHER_STATUSES)
                .orderByAsc(BizfiFiVoucher::getFdate)
                .orderByAsc(BizfiFiVoucher::getFid)), accountCode, false);
    }

    private VoucherMovementAggregate aggregateVoucherMovementsInRange(LocalDate startDate, LocalDate endDate, String accountCode) {
        return aggregateVoucherMovements(voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(BizfiFiVoucher::getFdate, startDate)
                .le(BizfiFiVoucher::getFdate, endDate)
                .in(BizfiFiVoucher::getFstatus, HISTORY_VOUCHER_STATUSES)
                .orderByAsc(BizfiFiVoucher::getFdate)
                .orderByAsc(BizfiFiVoucher::getFid)), accountCode, true);
    }

    private VoucherMovementAggregate aggregateVoucherMovements(List<BizfiFiVoucher> vouchers, String accountCode, boolean countInPeriod) {
        VoucherMovementAggregate aggregate = new VoucherMovementAggregate();
        List<Long> ids = vouchers.stream()
                .map(BizfiFiVoucher::getFid)
                .filter(Objects::nonNull)
                .toList();
        for (BizfiFiVoucherLine line : listVoucherLines(ids)) {
            String code = text(line.getFaccountCode());
            if (!matchesAccountCode(code, accountCode)) {
                continue;
            }
            BigDecimal debit = scale(line.getFdebitAmount());
            BigDecimal credit = scale(line.getFcreditAmount());
            aggregate.netByAccount.put(code, aggregate.netByAccount.getOrDefault(code, BigDecimal.ZERO).add(debit).subtract(credit));
            if (countInPeriod) {
                aggregate.debitByAccount.put(code, aggregate.debitByAccount.getOrDefault(code, BigDecimal.ZERO).add(debit));
                aggregate.creditByAccount.put(code, aggregate.creditByAccount.getOrDefault(code, BigDecimal.ZERO).add(credit));
                aggregate.countByAccount.put(code, aggregate.countByAccount.getOrDefault(code, 0L) + 1);
            }
        }
        return aggregate;
    }

    private GlMovementAggregate aggregateGlMovementsBefore(LocalDate startDate, String accountCode) {
        return aggregateGlMovements(glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .lt(BizfiFiGlEntry::getFvoucherDate, startDate)
                .likeRight(StringUtils.hasText(accountCode), BizfiFiGlEntry::getFaccountCode, accountCode)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFid)), false);
    }

    private GlMovementAggregate aggregateGlMovementsInRange(LocalDate startDate, LocalDate endDate, String accountCode) {
        return aggregateGlMovements(glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .ge(BizfiFiGlEntry::getFvoucherDate, startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate)
                .likeRight(StringUtils.hasText(accountCode), BizfiFiGlEntry::getFaccountCode, accountCode)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFid)), true);
    }

    private GlMovementAggregate aggregateGlMovements(List<BizfiFiGlEntry> entries, boolean countInPeriod) {
        GlMovementAggregate aggregate = new GlMovementAggregate();
        for (BizfiFiGlEntry entry : entries) {
            String code = text(entry.getFaccountCode());
            BigDecimal debit = scale(entry.getFdebitAmount());
            BigDecimal credit = scale(entry.getFcreditAmount());
            aggregate.netByAccount.put(code, aggregate.netByAccount.getOrDefault(code, BigDecimal.ZERO).add(debit).subtract(credit));
            if (countInPeriod) {
                aggregate.debitByAccount.put(code, aggregate.debitByAccount.getOrDefault(code, BigDecimal.ZERO).add(debit));
                aggregate.creditByAccount.put(code, aggregate.creditByAccount.getOrDefault(code, BigDecimal.ZERO).add(credit));
                aggregate.countByAccount.put(code, aggregate.countByAccount.getOrDefault(code, 0L) + 1);
            }
        }
        return aggregate;
    }

    private List<BizfiFiVoucherLine> listVoucherLines(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return List.of();
        }
        return voucherLineMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucherLine>()
                .in(BizfiFiVoucherLine::getFvoucherId, voucherIds)
                .orderByAsc(BizfiFiVoucherLine::getFvoucherId)
                .orderByAsc(BizfiFiVoucherLine::getFlineNo)
                .orderByAsc(BizfiFiVoucherLine::getFid));
    }

    private List<BizfiFiGlEntry> listGlEntries(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return List.of();
        }
        return glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .in(BizfiFiGlEntry::getFvoucherId, voucherIds)
                .orderByAsc(BizfiFiGlEntry::getFvoucherId)
                .orderByAsc(BizfiFiGlEntry::getFvoucherLineId)
                .orderByAsc(BizfiFiGlEntry::getFid));
    }

    private Map<String, BizfiFiAccount> loadAccountMap(Collection<String> accountCodes) {
        Set<String> codes = accountCodes == null
                ? Set.of()
                : accountCodes.stream().filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
        if (codes.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<BizfiFiAccount>()
                        .in(BizfiFiAccount::getFcode, codes))
                .stream()
                .collect(Collectors.toMap(BizfiFiAccount::getFcode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String accountNameOf(String code, Map<String, BizfiFiAccount> accountMap) {
        BizfiFiAccount account = accountMap.get(code);
        return account == null ? null : account.getFname();
    }

    private QueryRange resolveRange(String startDate, String endDate) {
        LocalDate end = parseDate(endDate, "endDate");
        if (end == null) {
            end = LocalDate.now();
        }
        LocalDate start = parseDate(startDate, "startDate");
        if (start == null) {
            start = YearMonth.from(end).atDay(1);
        }
        if (start.isAfter(end)) {
            throw new BizException("startDate 不能晚于 endDate。");
        }
        return new QueryRange(start, end);
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(fieldName + " 必须使用 yyyy-MM-dd 格式。");
        }
    }

    private String normalizeDocTypeRoot(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String root = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("AR", "AP").contains(root)) {
            throw new BizException("docTypeRoot 仅支持 AR、AP 或 ALL。");
        }
        return root;
    }

    private String normalizeMatchStatus(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PAIRED", "ORPHAN").contains(status)) {
            throw new BizException("matchStatus 仅支持 PAIRED、ORPHAN 或 ALL。");
        }
        return status;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean matchesKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String compare = keyword.trim().toLowerCase(Locale.ROOT);
        for (Object value : row.values()) {
            if (value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(compare)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAccountCode(String accountCode, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }
        return StringUtils.hasText(accountCode) && accountCode.startsWith(filter.trim());
    }

    private boolean startsWith(String value, String prefix) {
        return StringUtils.hasText(value) && value.startsWith(prefix);
    }

    private BigDecimal sumVoucherLineAmount(Collection<BizfiFiVoucherLine> rows, Function<BizfiFiVoucherLine, BigDecimal> mapper) {
        BigDecimal total = BigDecimal.ZERO;
        for (BizfiFiVoucherLine row : rows) {
            total = total.add(scale(mapper.apply(row)));
        }
        return total;
    }

    private BigDecimal sumGlEntryAmount(Collection<BizfiFiGlEntry> rows, Function<BizfiFiGlEntry, BigDecimal> mapper) {
        BigDecimal total = BigDecimal.ZERO;
        for (BizfiFiGlEntry row : rows) {
            total = total.add(scale(mapper.apply(row)));
        }
        return total;
    }

    private BigDecimal sumRowAmount(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(item -> decimal(item.get(key)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safeAmount(value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isNonZero(BigDecimal value) {
        return scale(value).compareTo(BigDecimal.ZERO) != 0;
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
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

    private Long longNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class QueryRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private QueryRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static class RuleSpec {
        private final String docTypeRoot;
        private final String docType;
        private final String ruleName;
        private final String debitAccountCode;
        private final String creditAccountCode;
        private final String summaryPrefix;

        private RuleSpec(String docTypeRoot, String docType, String ruleName, String debitAccountCode, String creditAccountCode, String summaryPrefix) {
            this.docTypeRoot = docTypeRoot;
            this.docType = docType;
            this.ruleName = ruleName;
            this.debitAccountCode = debitAccountCode;
            this.creditAccountCode = creditAccountCode;
            this.summaryPrefix = summaryPrefix;
        }
    }

    private static class VoucherMovementAggregate {
        private final Map<String, BigDecimal> netByAccount = new LinkedHashMap<>();
        private final Map<String, BigDecimal> debitByAccount = new LinkedHashMap<>();
        private final Map<String, BigDecimal> creditByAccount = new LinkedHashMap<>();
        private final Map<String, Long> countByAccount = new LinkedHashMap<>();
    }

    private static class GlMovementAggregate {
        private final Map<String, BigDecimal> netByAccount = new LinkedHashMap<>();
        private final Map<String, BigDecimal> debitByAccount = new LinkedHashMap<>();
        private final Map<String, BigDecimal> creditByAccount = new LinkedHashMap<>();
        private final Map<String, Long> countByAccount = new LinkedHashMap<>();
    }
}
