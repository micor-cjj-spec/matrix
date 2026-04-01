package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.report.entity.BizfiFiCashflowItem;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;
import single.cjj.fi.gl.report.entity.BizfiFiReportTemplate;
import single.cjj.fi.gl.report.mapper.BizfiFiCashflowItemMapper;
import single.cjj.fi.gl.report.service.BizfiFiCashFlowService;
import single.cjj.fi.gl.report.service.BizfiFiReportItemService;
import single.cjj.fi.gl.report.service.BizfiFiReportTemplateService;
import single.cjj.fi.gl.report.util.ReportTextFixer;
import single.cjj.fi.gl.report.vo.CashFlowSupplementCategoryVO;
import single.cjj.fi.gl.report.vo.CashFlowSupplementResultVO;
import single.cjj.fi.gl.report.vo.CashFlowSupplementTaskVO;
import single.cjj.fi.gl.report.vo.CashFlowSupplementVoucherVO;
import single.cjj.fi.gl.report.vo.CashFlowTraceResultVO;
import single.cjj.fi.gl.report.vo.CashFlowTraceRowVO;
import single.cjj.fi.gl.report.vo.ReportCheckResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;
import single.cjj.fi.gl.report.vo.ReportRowVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BizfiFiCashFlowServiceImpl implements BizfiFiCashFlowService {

    private static final String CNY = "CNY";
    private static final String OPERATING_CODE = "CF_OPERATING";
    private static final String INVESTING_CODE = "CF_INVESTING";
    private static final String FINANCING_CODE = "CF_FINANCING";
    private static final String NET_INCREASE_CODE = "CF_NET_INCREASE";

    @Autowired
    private BizfiFiReportTemplateService reportTemplateService;

    @Autowired
    private BizfiFiReportItemService reportItemService;

    @Autowired
    private BizfiFiCashflowItemMapper cashflowItemMapper;

    @Autowired
    private BizfiFiAccountMapper accountMapper;

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Override
    public ReportQueryResultVO query(Long orgId, String period, String currency, Long templateId, Boolean showZero) {
        ReportQueryResultVO result = new ReportQueryResultVO();
        result.setReportType("CASH_FLOW");
        result.setOrgId(orgId);
        result.setPeriod(period);
        result.setCurrency(StringUtils.hasText(currency) ? currency : CNY);
        result.setRows(new ArrayList<>());
        result.setChecks(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        YearMonth targetPeriod = parsePeriod(period);
        if (targetPeriod == null) {
            result.getWarnings().add("Invalid or missing period. Use yyyy-MM, for example 2026-03.");
            result.getChecks().add(new ReportCheckResultVO("PERIOD_READY", false, "The requested period is invalid."));
            return result;
        }
        result.setPeriod(targetPeriod.toString());

        BizfiFiReportTemplate template = resolveTemplate(templateId);
        if (template == null) {
            result.getWarnings().add("No cash-flow template was found. Create or import a cash-flow template first.");
            result.getChecks().add(new ReportCheckResultVO("TEMPLATE_READY", false, "Cash-flow template is not ready."));
            return result;
        }
        result.setTemplateId(template.getFid());
        result.setTemplateName(ReportTextFixer.fixTemplateName(template.getFcode(), template.getFname()));

        List<BizfiFiReportItem> items = reportItemService.listByTemplateId(template.getFid());
        if (items.isEmpty()) {
            items = buildDefaultItems(template.getFid());
            result.getWarnings().add("No cash-flow report items are configured yet. Showing scaffold rows first.");
        }

        List<BizfiFiAccount> accounts = loadAccounts(orgId);
        if (accounts.isEmpty()) {
            result.getWarnings().add("No accounts were found for the requested organization.");
            result.getChecks().add(new ReportCheckResultVO("ACCOUNT_READY", false, "No account subjects are available."));
            result.setRows(toRows(items, Collections.emptyMap(), Boolean.TRUE.equals(showZero)));
            return result;
        }

        Map<String, BizfiFiAccount> accountByCode = accounts.stream()
                .filter(account -> StringUtils.hasText(account.getFcode()))
                .collect(Collectors.toMap(
                        account -> account.getFcode().toUpperCase(Locale.ROOT),
                        account -> account,
                        (left, right) -> left
                ));
        Set<String> accountCodes = accountByCode.keySet();
        if (accountCodes.isEmpty()) {
            result.getWarnings().add("No valid account codes were found for the requested organization.");
            result.setRows(toRows(items, Collections.emptyMap(), Boolean.TRUE.equals(showZero)));
            return result;
        }

        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();
        List<BizfiFiGlEntry> entries = glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .in(BizfiFiGlEntry::getFaccountCode, accountCodes)
                .ge(BizfiFiGlEntry::getFvoucherDate, startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherId)
                .orderByAsc(BizfiFiGlEntry::getFid));
        if (entries.isEmpty()) {
            result.getWarnings().add("No posted ledger entries were found for the requested period.");
            result.setRows(toRows(items, Collections.emptyMap(), Boolean.TRUE.equals(showZero)));
            result.getChecks().add(new ReportCheckResultVO("ENTRY_READY", false, "Post vouchers before querying the cash-flow statement."));
            return result;
        }

        Map<String, BizfiFiReportItem> itemByCode = items.stream()
                .filter(item -> StringUtils.hasText(item.getFcode()))
                .collect(Collectors.toMap(
                        item -> item.getFcode().toUpperCase(Locale.ROOT),
                        item -> item,
                        (left, right) -> left
                ));
        Map<String, BizfiFiCashflowItem> cashflowItemByCode = loadCashflowItemByCode();
        Map<Long, BigDecimal> directAmounts = new HashMap<>();
        List<String> missingCashflowCodes = new ArrayList<>();
        int cashTransferCount = 0;
        int heuristicCount = 0;

        Map<Long, List<BizfiFiGlEntry>> entriesByVoucher = entries.stream()
                .filter(entry -> entry.getFvoucherId() != null)
                .collect(Collectors.groupingBy(BizfiFiGlEntry::getFvoucherId, java.util.LinkedHashMap::new, Collectors.toList()));

        for (List<BizfiFiGlEntry> voucherEntries : entriesByVoucher.values()) {
            List<BizfiFiGlEntry> cashEntries = voucherEntries.stream()
                    .filter(entry -> isCashLike(accountByCode.get(normalizeCode(entry.getFaccountCode()))))
                    .toList();
            if (cashEntries.isEmpty()) {
                continue;
            }

            List<BizfiFiGlEntry> nonCashEntries = voucherEntries.stream()
                    .filter(entry -> !isCashLike(accountByCode.get(normalizeCode(entry.getFaccountCode()))))
                    .toList();
            if (nonCashEntries.isEmpty()) {
                cashTransferCount++;
                continue;
            }

            Resolution resolution = resolveVoucherCategory(voucherEntries, cashEntries, nonCashEntries, cashflowItemByCode, itemByCode, accountByCode);
            if (resolution.itemId == null) {
                if (StringUtils.hasText(resolution.rawCode)) {
                    missingCashflowCodes.add(resolution.rawCode);
                }
                continue;
            }

            if (resolution.heuristic) {
                heuristicCount++;
            }

            BigDecimal voucherAmount = cashEntries.stream()
                    .map(entry -> safe(entry.getFdebitAmount()).subtract(safe(entry.getFcreditAmount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (voucherAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            directAmounts.merge(resolution.itemId, voucherAmount, BigDecimal::add);
        }

        applyNetIncreaseFormula(items, directAmounts);
        Map<Long, BigDecimal> rolledAmounts = rollupAmounts(items, directAmounts);
        result.setRows(toRows(items, rolledAmounts, Boolean.TRUE.equals(showZero)));

        if (cashTransferCount > 0) {
            result.getWarnings().add("Skipped " + cashTransferCount + " pure cash-transfer vouchers to avoid overstating external cash flow.");
        }
        if (heuristicCount > 0) {
            result.getWarnings().add("Used account-type heuristics for " + heuristicCount + " vouchers because no cash-flow item code was provided.");
        }
        if (!missingCashflowCodes.isEmpty()) {
            result.getWarnings().add("Unknown cash-flow item codes: " + String.join(", ", missingCashflowCodes.stream().distinct().limit(8).toList()));
        }
        if (orgId != null) {
            result.getWarnings().add("Cash-flow amounts are currently aggregated by account code. If multiple organizations reuse the same code set, add org-level ledger isolation later.");
        }

        result.getChecks().add(new ReportCheckResultVO("CASHFLOW_QUERY", true, "Cash-flow statement amounts were aggregated from posted ledger entries and cash-flow tags."));
        BigDecimal netIncrease = findAmountByCode(items, rolledAmounts, NET_INCREASE_CODE);
        if (netIncrease != null) {
            result.getChecks().add(new ReportCheckResultVO("NET_INCREASE_READY", true, "Net cash increase for the selected period is " + netIncrease.toPlainString() + "."));
        }
        return result;
    }

    @Override
    public CashFlowTraceResultVO trace(
            Long orgId,
            String period,
            String currency,
            String cashflowItemCode,
            String categoryCode,
            String sourceType,
            String accountCode,
            String keyword
    ) {
        AnalysisContext context = analyzeCashVouchers(orgId, period, currency);
        CashFlowTraceResultVO result = initTraceResult(context);
        if (context.targetPeriod == null) {
            return result;
        }

        List<VoucherFlowRecord> filteredRecords = context.records.stream()
                .filter(record -> matchesCashflowItem(record, cashflowItemCode))
                .filter(record -> matchesCategory(record, categoryCode))
                .filter(record -> matchesSourceType(record, sourceType))
                .filter(record -> matchesAccount(record, accountCode))
                .filter(record -> matchesKeyword(record, keyword))
                .toList();

        result.setRecords(filteredRecords.stream().map(this::toTraceRow).toList());
        result.setCashVoucherCount((long) filteredRecords.size());
        result.setDirectCount(countBySource(filteredRecords, "DIRECT"));
        result.setHeuristicCount(countBySource(filteredRecords, "HEURISTIC"));
        result.setUnknownCount(countBySource(filteredRecords, "UNKNOWN_ITEM"));
        result.setMixedCount(countBySource(filteredRecords, "MIXED_ITEM"));
        result.setTransferCount(countBySource(filteredRecords, "CASH_TRANSFER"));
        result.setCashInAmount(scale(sumTraceAmount(filteredRecords, true)));
        result.setCashOutAmount(scale(sumTraceAmount(filteredRecords, false)));
        result.setNetAmount(scale(sumTraceNet(filteredRecords)));

        if (context.records.isEmpty()) {
            result.getWarnings().add("The selected period has no cash-related posted vouchers.");
        } else if (filteredRecords.isEmpty()) {
            result.getWarnings().add("No cash-flow trace records matched the current filters.");
        }
        return result;
    }

    @Override
    public CashFlowSupplementResultVO supplement(Long orgId, String period, String currency) {
        AnalysisContext context = analyzeCashVouchers(orgId, period, currency);
        CashFlowSupplementResultVO result = initSupplementResult(context);
        result.setTasks(buildSupplementTasks(context));
        result.setCategories(buildSupplementCategories(context.records));
        result.setPendingVouchers(buildPendingVouchers(context.records));
        return result;
    }

    private BizfiFiReportTemplate resolveTemplate(Long templateId) {
        if (templateId != null) {
            return reportTemplateService.get(templateId);
        }

        BizfiFiReportTemplate enabledTemplate = reportTemplateService.getEnabledTemplate("CASH_FLOW", null);
        if (enabledTemplate != null) {
            return enabledTemplate;
        }

        List<BizfiFiReportTemplate> templates = reportTemplateService.list(new LambdaQueryWrapper<BizfiFiReportTemplate>()
                .eq(BizfiFiReportTemplate::getFtype, "CASH_FLOW")
                .orderByDesc(BizfiFiReportTemplate::getFupdatetime)
                .orderByDesc(BizfiFiReportTemplate::getFid)
                .last("limit 1"));
        return templates.isEmpty() ? null : templates.get(0);
    }

    private List<BizfiFiAccount> loadAccounts(Long orgId) {
        LambdaQueryWrapper<BizfiFiAccount> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(BizfiFiAccount::getForg, orgId);
        }
        wrapper.orderByAsc(BizfiFiAccount::getFcode);
        return accountMapper.selectList(wrapper);
    }

    private Map<String, BizfiFiCashflowItem> loadCashflowItemByCode() {
        return cashflowItemMapper.selectList(new LambdaQueryWrapper<BizfiFiCashflowItem>()
                        .orderByAsc(BizfiFiCashflowItem::getFsort)
                        .orderByAsc(BizfiFiCashflowItem::getFid))
                .stream()
                .filter(item -> StringUtils.hasText(item.getFcode()))
                .collect(Collectors.toMap(
                        item -> item.getFcode().toUpperCase(Locale.ROOT),
                        item -> item,
                        (left, right) -> left
                ));
    }

    private AnalysisContext analyzeCashVouchers(Long orgId, String period, String currency) {
        AnalysisContext context = new AnalysisContext();
        context.orgId = orgId;
        context.currency = StringUtils.hasText(currency) ? currency.trim() : CNY;
        context.records = new ArrayList<>();
        context.warnings = new ArrayList<>();

        YearMonth targetPeriod = parsePeriod(period);
        if (targetPeriod == null) {
            context.warnings.add("Invalid or missing period. Use yyyy-MM, for example 2026-03.");
            return context;
        }
        context.targetPeriod = targetPeriod;
        context.period = targetPeriod.toString();
        context.startDate = targetPeriod.atDay(1);
        context.endDate = targetPeriod.atEndOfMonth();

        List<BizfiFiAccount> accounts = loadAccounts(orgId);
        context.cashAccountCount = (int) accounts.stream().filter(this::isCashLike).count();
        if (accounts.isEmpty()) {
            context.warnings.add("No accounts were found for the requested organization.");
            return context;
        }

        context.accountByCode = accounts.stream()
                .filter(account -> StringUtils.hasText(account.getFcode()))
                .collect(Collectors.toMap(
                        account -> account.getFcode().toUpperCase(Locale.ROOT),
                        account -> account,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (context.accountByCode.isEmpty()) {
            context.warnings.add("No valid account codes were found for the requested organization.");
            return context;
        }

        context.cashflowItemByCode = loadCashflowItemByCode();
        context.cashflowItemCount = context.cashflowItemByCode.size();

        context.entries = glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .in(BizfiFiGlEntry::getFaccountCode, context.accountByCode.keySet())
                .ge(BizfiFiGlEntry::getFvoucherDate, context.startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, context.endDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherId)
                .orderByAsc(BizfiFiGlEntry::getFvoucherLineId)
                .orderByAsc(BizfiFiGlEntry::getFid));
        if (context.entries.isEmpty()) {
            context.warnings.add("No posted ledger entries were found for the requested period.");
            return context;
        }

        context.postedVoucherCount = context.entries.stream()
                .map(this::voucherGroupKey)
                .distinct()
                .count();
        context.records = buildVoucherFlowRecords(context.entries, context.accountByCode, context.cashflowItemByCode);

        long heuristicCount = countBySource(context.records, "HEURISTIC");
        long unknownCount = countBySource(context.records, "UNKNOWN_ITEM");
        long mixedCount = countBySource(context.records, "MIXED_ITEM");
        long transferCount = countBySource(context.records, "CASH_TRANSFER");
        if (heuristicCount > 0) {
            context.warnings.add("Some vouchers still rely on account-type heuristics because no cash-flow item was entered.");
        }
        if (unknownCount > 0) {
            context.warnings.add("Some vouchers use unknown cash-flow item codes and are not included in the statement until they are corrected.");
        }
        if (mixedCount > 0) {
            context.warnings.add("Some vouchers carry multiple cash-flow item codes and should be reviewed before final reporting.");
        }
        if (transferCount > 0) {
            context.warnings.add("Pure cash-transfer vouchers are separated for review and are skipped by the statement.");
        }
        if (orgId != null) {
            context.warnings.add("Cash-flow amounts are currently aggregated by account code. If multiple organizations reuse the same code set, add org-level ledger isolation later.");
        }
        return context;
    }

    private List<VoucherFlowRecord> buildVoucherFlowRecords(
            List<BizfiFiGlEntry> entries,
            Map<String, BizfiFiAccount> accountByCode,
            Map<String, BizfiFiCashflowItem> cashflowItemByCode
    ) {
        Map<String, List<BizfiFiGlEntry>> entriesByVoucher = entries.stream()
                .collect(Collectors.groupingBy(this::voucherGroupKey, LinkedHashMap::new, Collectors.toList()));

        List<VoucherFlowRecord> records = new ArrayList<>();
        for (List<BizfiFiGlEntry> voucherEntries : entriesByVoucher.values()) {
            List<BizfiFiGlEntry> cashEntries = voucherEntries.stream()
                    .filter(entry -> isCashLike(accountByCode.get(normalizeCode(entry.getFaccountCode()))))
                    .toList();
            if (cashEntries.isEmpty()) {
                continue;
            }

            List<BizfiFiGlEntry> nonCashEntries = voucherEntries.stream()
                    .filter(entry -> !isCashLike(accountByCode.get(normalizeCode(entry.getFaccountCode()))))
                    .toList();

            VoucherFlowRecord record = new VoucherFlowRecord();
            BizfiFiGlEntry firstEntry = voucherEntries.get(0);
            record.voucherId = firstEntry.getFvoucherId();
            record.voucherNumber = firstNonBlank(voucherEntries.stream().map(BizfiFiGlEntry::getFvoucherNumber).toList());
            record.voucherDate = firstEntry.getFvoucherDate();
            record.summary = firstNonBlank(voucherEntries.stream().map(BizfiFiGlEntry::getFsummary).toList());
            record.cashEntryCount = cashEntries.size();
            record.counterpartyEntryCount = nonCashEntries.size();
            record.cashInAmount = scale(cashEntries.stream().map(BizfiFiGlEntry::getFdebitAmount).reduce(BigDecimal.ZERO, this::safeAdd));
            record.cashOutAmount = scale(cashEntries.stream().map(BizfiFiGlEntry::getFcreditAmount).reduce(BigDecimal.ZERO, this::safeAdd));
            record.netAmount = scale(record.cashInAmount.subtract(record.cashOutAmount));
            record.cashAccountCodes = joinCodes(cashEntries.stream().map(BizfiFiGlEntry::getFaccountCode).toList());
            record.counterpartyAccountCodes = joinCodes(nonCashEntries.stream().map(BizfiFiGlEntry::getFaccountCode).toList());
            record.postedBy = firstNonBlank(voucherEntries.stream().map(BizfiFiGlEntry::getFpostedBy).toList());
            record.postedTime = voucherEntries.stream()
                    .map(BizfiFiGlEntry::getFpostedTime)
                    .filter(item -> item != null)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            List<String> explicitCodes = voucherEntries.stream()
                    .map(BizfiFiGlEntry::getFcashflowItem)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
            record.cashflowItemCode = explicitCodes.isEmpty() ? null : explicitCodes.get(0);

            if (nonCashEntries.isEmpty()) {
                record.sourceType = "CASH_TRANSFER";
                record.categoryCode = null;
                record.categoryName = "Cash transfer";
                record.reason = "Only cash-like accounts were found in the voucher, so the statement skips it to avoid overstating external cash flow.";
            } else if (explicitCodes.size() > 1) {
                ExplicitResolution resolution = resolveExplicitCode(explicitCodes.get(0), cashflowItemByCode);
                applyResolution(record, resolution);
                record.sourceType = "MIXED_ITEM";
                record.reason = "Multiple cash-flow item codes were found in one voucher. Review the voucher lines before final reporting.";
            } else if (!explicitCodes.isEmpty()) {
                ExplicitResolution resolution = resolveExplicitCode(explicitCodes.get(0), cashflowItemByCode);
                applyResolution(record, resolution);
                if (resolution.recognized) {
                    record.sourceType = "DIRECT";
                    record.reason = "The voucher carries an explicit cash-flow item code.";
                } else {
                    record.sourceType = "UNKNOWN_ITEM";
                    record.categoryCode = null;
                    record.categoryName = "Unknown code";
                    record.reason = "The cash-flow item code is unknown and the statement does not include this voucher until the code is corrected.";
                }
            } else {
                record.sourceType = "HEURISTIC";
                record.categoryCode = inferCategoryFromNonCashAccounts(nonCashEntries, accountByCode);
                record.categoryName = categoryNameOf(record.categoryCode);
                record.reason = "No cash-flow item code was entered, so the voucher is classified by counterparty account type.";
            }

            records.add(record);
        }

        return records.stream()
                .sorted(Comparator.comparing((VoucherFlowRecord item) -> item.voucherDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(item -> item.voucherNumber, Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> item.voucherId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private void applyResolution(VoucherFlowRecord record, ExplicitResolution resolution) {
        record.cashflowItemCode = resolution.code;
        record.cashflowItemName = resolution.name;
        record.categoryCode = resolution.categoryCode;
        record.categoryName = resolution.categoryName;
    }

    private ExplicitResolution resolveExplicitCode(String rawCode, Map<String, BizfiFiCashflowItem> cashflowItemByCode) {
        String code = trim(rawCode);
        if (!StringUtils.hasText(code)) {
            return new ExplicitResolution(null, null, null, null, false);
        }

        String normalizedCode = normalizeCode(code);
        if (OPERATING_CODE.equals(normalizedCode) || INVESTING_CODE.equals(normalizedCode) || FINANCING_CODE.equals(normalizedCode)) {
            return new ExplicitResolution(code, categoryNameOf(normalizedCode), normalizedCode, categoryNameOf(normalizedCode), true);
        }

        BizfiFiCashflowItem item = cashflowItemByCode.get(normalizedCode);
        if (item == null) {
            return new ExplicitResolution(code, code, null, null, false);
        }

        String categoryCode = mapCategoryToReportCode(item.getFcategory());
        return new ExplicitResolution(
                item.getFcode(),
                ReportTextFixer.fix(item.getFname()),
                categoryCode,
                categoryNameOf(categoryCode),
                true
        );
    }

    private CashFlowTraceResultVO initTraceResult(AnalysisContext context) {
        CashFlowTraceResultVO result = new CashFlowTraceResultVO();
        result.setOrgId(context.orgId);
        result.setPeriod(context.period);
        result.setStartDate(context.startDate == null ? null : context.startDate.toString());
        result.setEndDate(context.endDate == null ? null : context.endDate.toString());
        result.setCurrency(context.currency);
        result.setPostedVoucherCount(context.postedVoucherCount);
        result.setCashVoucherCount(0L);
        result.setDirectCount(0L);
        result.setHeuristicCount(0L);
        result.setUnknownCount(0L);
        result.setMixedCount(0L);
        result.setTransferCount(0L);
        result.setCashInAmount(scale(BigDecimal.ZERO));
        result.setCashOutAmount(scale(BigDecimal.ZERO));
        result.setNetAmount(scale(BigDecimal.ZERO));
        result.setRecords(new ArrayList<>());
        result.setWarnings(new ArrayList<>(context.warnings));
        return result;
    }

    private CashFlowSupplementResultVO initSupplementResult(AnalysisContext context) {
        CashFlowSupplementResultVO result = new CashFlowSupplementResultVO();
        result.setOrgId(context.orgId);
        result.setPeriod(context.period);
        result.setStartDate(context.startDate == null ? null : context.startDate.toString());
        result.setEndDate(context.endDate == null ? null : context.endDate.toString());
        result.setCurrency(context.currency);
        result.setPostedVoucherCount(context.postedVoucherCount);
        result.setCashVoucherCount((long) context.records.size());
        result.setDirectCount(countBySource(context.records, "DIRECT"));
        result.setHeuristicCount(countBySource(context.records, "HEURISTIC"));
        result.setUnknownCount(countBySource(context.records, "UNKNOWN_ITEM"));
        result.setMixedCount(countBySource(context.records, "MIXED_ITEM"));
        result.setTransferCount(countBySource(context.records, "CASH_TRANSFER"));
        result.setCashAccountCount(context.cashAccountCount);
        result.setCashflowItemCount(context.cashflowItemCount);
        result.setCashInAmount(scale(sumTraceAmount(context.records, true)));
        result.setCashOutAmount(scale(sumTraceAmount(context.records, false)));
        result.setNetAmount(scale(sumTraceNet(context.records)));
        result.setTasks(new ArrayList<>());
        result.setCategories(new ArrayList<>());
        result.setPendingVouchers(new ArrayList<>());
        result.setWarnings(new ArrayList<>(context.warnings));
        return result;
    }

    private List<CashFlowSupplementTaskVO> buildSupplementTasks(AnalysisContext context) {
        List<CashFlowSupplementTaskVO> tasks = new ArrayList<>();
        long directCount = countBySource(context.records, "DIRECT");
        long heuristicCount = countBySource(context.records, "HEURISTIC");
        long unknownCount = countBySource(context.records, "UNKNOWN_ITEM");
        long mixedCount = countBySource(context.records, "MIXED_ITEM");
        long transferCount = countBySource(context.records, "CASH_TRANSFER");
        long pendingCount = heuristicCount + unknownCount + mixedCount + transferCount;

        tasks.add(new CashFlowSupplementTaskVO(
                "CASH_ACCOUNT_READY",
                "Cash account setup",
                context.cashAccountCount > 0 ? "READY" : "WARNING",
                context.cashAccountCount > 0
                        ? "Detected " + context.cashAccountCount + " cash-like accounts that can participate in the cash-flow statement."
                        : "No cash-like accounts are configured, so the system cannot identify cash-flow vouchers correctly.",
                "Maintain cash/bank/equivalent flags in account subjects."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "CASHFLOW_ITEM_READY",
                "Cashflow item master",
                context.cashflowItemCount > 0 ? "READY" : "WARNING",
                context.cashflowItemCount > 0
                        ? "Detected " + context.cashflowItemCount + " cash-flow items for voucher tagging and statement review."
                        : "No cash-flow item master data is configured yet.",
                "Complete cash-flow item maintenance before large-scale voucher tagging."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "DIRECT_COVERAGE",
                "Direct tagging coverage",
                pendingCount == 0 ? "READY" : "WARNING",
                "Directly tagged vouchers: " + directCount + ", heuristic vouchers: " + heuristicCount + ", pending review vouchers: " + (unknownCount + mixedCount + transferCount) + ".",
                "Prefer explicit cash-flow item tagging on voucher lines to reduce heuristic classification."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "UNKNOWN_CODE_REVIEW",
                "Unknown code review",
                unknownCount == 0 ? "READY" : "WARNING",
                unknownCount == 0
                        ? "No unknown cash-flow item codes were found in the selected period."
                        : "Found " + unknownCount + " vouchers with unknown cash-flow item codes.",
                "Correct invalid cash-flow item codes or align the master data before final reporting."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "MIXED_CODE_REVIEW",
                "Mixed-code voucher review",
                mixedCount == 0 ? "READY" : "WARNING",
                mixedCount == 0
                        ? "No vouchers carry multiple cash-flow item codes."
                        : "Found " + mixedCount + " vouchers that carry multiple cash-flow item codes.",
                "Split or correct voucher lines so one voucher follows a clear cash-flow tagging rule."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "TRANSFER_REVIEW",
                "Cash transfer review",
                transferCount == 0 ? "READY" : "INFO",
                transferCount == 0
                        ? "No pure cash-transfer vouchers were detected in the selected period."
                        : "Detected " + transferCount + " pure cash-transfer vouchers that are excluded from the statement.",
                "Review whether internal transfers need separate disclosure or supporting notes."
        ));
        tasks.add(new CashFlowSupplementTaskVO(
                "ORG_ISOLATION",
                "Org isolation note",
                context.orgId == null ? "INFO" : "WARNING",
                context.orgId == null
                        ? "The current query is not narrowed to one organization."
                        : "The current implementation still aggregates by account code and does not isolate ledger entries by organization.",
                "When multiple business units reuse the same account code set, add org-level dimensions to general-ledger entries."
        ));
        return tasks;
    }

    private List<CashFlowSupplementCategoryVO> buildSupplementCategories(List<VoucherFlowRecord> records) {
        Map<String, SupplementCategoryAccumulator> grouped = new LinkedHashMap<>();
        for (VoucherFlowRecord record : records) {
            String key = categoryBucketKey(record);
            grouped.computeIfAbsent(key, ignore -> new SupplementCategoryAccumulator(categoryBucketCode(record), categoryBucketName(record)));
            grouped.get(key).add(record);
        }
        return grouped.values().stream()
                .map(SupplementCategoryAccumulator::toVO)
                .sorted(Comparator.comparing(CashFlowSupplementCategoryVO::getCategoryCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<CashFlowSupplementVoucherVO> buildPendingVouchers(List<VoucherFlowRecord> records) {
        return records.stream()
                .filter(record -> !"DIRECT".equals(record.sourceType))
                .sorted(Comparator.comparingInt((VoucherFlowRecord item) -> sourcePriority(item.sourceType))
                        .thenComparing(item -> item.voucherDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(item -> item.voucherNumber, Comparator.nullsLast(String::compareTo)))
                .map(record -> new CashFlowSupplementVoucherVO(
                        record.voucherId,
                        record.voucherNumber,
                        record.voucherDate == null ? null : record.voucherDate.toString(),
                        record.summary,
                        record.sourceType,
                        record.cashflowItemCode,
                        record.cashflowItemName,
                        categoryBucketName(record),
                        scale(record.netAmount),
                        pendingIssue(record.sourceType),
                        pendingSuggestion(record.sourceType)
                ))
                .toList();
    }

    private CashFlowTraceRowVO toTraceRow(VoucherFlowRecord record) {
        return new CashFlowTraceRowVO(
                record.voucherId,
                record.voucherNumber,
                record.voucherDate == null ? null : record.voucherDate.toString(),
                record.summary,
                record.cashAccountCodes,
                record.counterpartyAccountCodes,
                record.cashflowItemCode,
                record.cashflowItemName,
                record.categoryCode,
                record.categoryName,
                record.sourceType,
                scale(record.cashInAmount),
                scale(record.cashOutAmount),
                scale(record.netAmount),
                record.cashEntryCount,
                record.counterpartyEntryCount,
                record.postedBy,
                record.postedTime == null ? null : record.postedTime.toString(),
                record.reason
        );
    }

    private Resolution resolveVoucherCategory(
            List<BizfiFiGlEntry> voucherEntries,
            List<BizfiFiGlEntry> cashEntries,
            List<BizfiFiGlEntry> nonCashEntries,
            Map<String, BizfiFiCashflowItem> cashflowItemByCode,
            Map<String, BizfiFiReportItem> itemByCode,
            Map<String, BizfiFiAccount> accountByCode
    ) {
        List<BizfiFiGlEntry> prioritizedEntries = new ArrayList<>();
        prioritizedEntries.addAll(cashEntries);
        prioritizedEntries.addAll(nonCashEntries);
        prioritizedEntries.addAll(voucherEntries);

        for (BizfiFiGlEntry entry : prioritizedEntries) {
            String rawCode = entry.getFcashflowItem();
            if (!StringUtils.hasText(rawCode)) {
                continue;
            }

            Long itemId = resolveItemIdFromCashflowCode(rawCode, cashflowItemByCode, itemByCode);
            if (itemId != null) {
                return new Resolution(itemId, false, rawCode);
            }
            return new Resolution(null, false, rawCode);
        }

        String fallbackCode = inferCategoryFromNonCashAccounts(nonCashEntries, accountByCode);
        BizfiFiReportItem item = itemByCode.get(fallbackCode);
        return new Resolution(item == null ? null : item.getFid(), true, fallbackCode);
    }

    private Long resolveItemIdFromCashflowCode(
            String rawCode,
            Map<String, BizfiFiCashflowItem> cashflowItemByCode,
            Map<String, BizfiFiReportItem> itemByCode
    ) {
        String normalizedCode = normalizeCode(rawCode);
        BizfiFiReportItem directItem = itemByCode.get(normalizedCode);
        if (directItem != null) {
            return directItem.getFid();
        }

        BizfiFiCashflowItem cashflowItem = cashflowItemByCode.get(normalizedCode);
        if (cashflowItem == null) {
            return null;
        }

        String categoryCode = mapCategoryToReportCode(cashflowItem.getFcategory());
        BizfiFiReportItem categoryItem = itemByCode.get(categoryCode);
        return categoryItem == null ? null : categoryItem.getFid();
    }

    private String inferCategoryFromNonCashAccounts(List<BizfiFiGlEntry> nonCashEntries, Map<String, BizfiFiAccount> accountByCode) {
        for (BizfiFiGlEntry entry : nonCashEntries) {
            BizfiFiAccount account = accountByCode.get(normalizeCode(entry.getFaccountCode()));
            if (account == null) {
                continue;
            }
            String type = normalize(account.getFtype());
            if (containsAny(type, "\u6295\u8d44", "\u957f\u671f", "\u56fa\u5b9a\u8d44\u4ea7", "\u65e0\u5f62\u8d44\u4ea7", "invest", "fixed asset", "intangible")) {
                return INVESTING_CODE;
            }
            if (containsAny(type, "\u8d1f\u503a", "\u6743\u76ca", "\u501f\u6b3e", "\u80a1\u672c", "\u8d44\u672c", "liability", "equity", "loan", "capital")) {
                return FINANCING_CODE;
            }
        }
        return OPERATING_CODE;
    }

    private void applyNetIncreaseFormula(List<BizfiFiReportItem> items, Map<Long, BigDecimal> directAmounts) {
        Long operatingId = findItemIdByCode(items, OPERATING_CODE);
        Long investingId = findItemIdByCode(items, INVESTING_CODE);
        Long financingId = findItemIdByCode(items, FINANCING_CODE);
        Long netIncreaseId = findItemIdByCode(items, NET_INCREASE_CODE);
        if (netIncreaseId == null) {
            return;
        }

        BigDecimal total = safe(directAmounts.get(operatingId))
                .add(safe(directAmounts.get(investingId)))
                .add(safe(directAmounts.get(financingId)));
        directAmounts.put(netIncreaseId, total);
    }

    private Long findItemIdByCode(List<BizfiFiReportItem> items, String code) {
        for (BizfiFiReportItem item : items) {
            if (code.equalsIgnoreCase(item.getFcode())) {
                return item.getFid();
            }
        }
        return null;
    }

    private Map<Long, BigDecimal> rollupAmounts(List<BizfiFiReportItem> items, Map<Long, BigDecimal> directAmounts) {
        Map<Long, List<BizfiFiReportItem>> childrenByParent = items.stream()
                .filter(item -> item.getFparentId() != null)
                .collect(Collectors.groupingBy(BizfiFiReportItem::getFparentId));
        Map<Long, BigDecimal> memo = new HashMap<>();
        for (BizfiFiReportItem item : items) {
            computeItemAmount(item.getFid(), childrenByParent, directAmounts, memo, new HashSet<>());
        }
        return memo;
    }

    private BigDecimal computeItemAmount(
            Long itemId,
            Map<Long, List<BizfiFiReportItem>> childrenByParent,
            Map<Long, BigDecimal> directAmounts,
            Map<Long, BigDecimal> memo,
            Set<Long> path
    ) {
        if (itemId == null) {
            return BigDecimal.ZERO;
        }
        if (memo.containsKey(itemId)) {
            return memo.get(itemId);
        }
        if (!path.add(itemId)) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = safe(directAmounts.get(itemId));
        for (BizfiFiReportItem child : childrenByParent.getOrDefault(itemId, Collections.emptyList())) {
            total = total.add(computeItemAmount(child.getFid(), childrenByParent, directAmounts, memo, path));
        }
        path.remove(itemId);
        memo.put(itemId, total);
        return total;
    }

    private BigDecimal findAmountByCode(List<BizfiFiReportItem> items, Map<Long, BigDecimal> amountByItem, String code) {
        for (BizfiFiReportItem item : items) {
            if (code.equalsIgnoreCase(item.getFcode())) {
                return safe(amountByItem.get(item.getFid()));
            }
        }
        return null;
    }

    private List<ReportRowVO> toRows(List<BizfiFiReportItem> items, Map<Long, BigDecimal> amountByItem, boolean showZero) {
        return items.stream()
                .sorted(Comparator.comparing(BizfiFiReportItem::getFsort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BizfiFiReportItem::getFid))
                .map(item -> new ReportRowVO(
                        item.getFid(),
                        item.getFcode(),
                        ReportTextFixer.fixItemName(item.getFcode(), item.getFname()),
                        item.getFrowNo(),
                        item.getFlevel(),
                        item.getFlineType(),
                        safe(amountByItem.get(item.getFid())),
                        null,
                        null,
                        null,
                        item.getFdrillable() != null && item.getFdrillable() == 1,
                        Collections.emptyList()
                ))
                .filter(row -> showZero || row.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    private List<BizfiFiReportItem> buildDefaultItems(Long templateId) {
        List<BizfiFiReportItem> items = new ArrayList<>();
        items.add(buildItem(-1301L, templateId, null, OPERATING_CODE, "\u7ecf\u8425\u6d3b\u52a8\u73b0\u91d1\u6d41\u91cf\u51c0\u989d", "1", 1, "DETAIL", 10));
        items.add(buildItem(-1302L, templateId, null, INVESTING_CODE, "\u6295\u8d44\u6d3b\u52a8\u73b0\u91d1\u6d41\u91cf\u51c0\u989d", "2", 1, "DETAIL", 20));
        items.add(buildItem(-1303L, templateId, null, FINANCING_CODE, "\u7b79\u8d44\u6d3b\u52a8\u73b0\u91d1\u6d41\u91cf\u51c0\u989d", "3", 1, "DETAIL", 30));
        items.add(buildItem(-1304L, templateId, null, NET_INCREASE_CODE, "\u73b0\u91d1\u53ca\u73b0\u91d1\u7b49\u4ef7\u7269\u51c0\u589e\u52a0\u989d", "4", 1, "FORMULA", 40));
        return items;
    }

    private BizfiFiReportItem buildItem(
            Long id,
            Long templateId,
            Long parentId,
            String code,
            String name,
            String rowNo,
            Integer level,
            String lineType,
            Integer sort
    ) {
        BizfiFiReportItem item = new BizfiFiReportItem();
        item.setFid(id);
        item.setFtemplateId(templateId);
        item.setFparentId(parentId);
        item.setFcode(code);
        item.setFname(name);
        item.setFrowNo(rowNo);
        item.setFlevel(level);
        item.setFlineType(lineType);
        item.setFperiodMode("CURRENT");
        item.setFsignRule("RAW");
        item.setFdrillable(0);
        item.setFeditableAdjustment(0);
        item.setFsort(sort);
        return item;
    }

    private boolean matchesCashflowItem(VoucherFlowRecord record, String cashflowItemCode) {
        return !StringUtils.hasText(cashflowItemCode)
                || normalizeCode(cashflowItemCode).equals(normalizeCode(record.cashflowItemCode));
    }

    private boolean matchesCategory(VoucherFlowRecord record, String categoryCode) {
        return !StringUtils.hasText(categoryCode)
                || normalizeCode(categoryCode).equals(normalizeCode(record.categoryCode));
    }

    private boolean matchesSourceType(VoucherFlowRecord record, String sourceType) {
        return !StringUtils.hasText(sourceType)
                || normalizeCode(sourceType).equals(normalizeCode(record.sourceType));
    }

    private boolean matchesAccount(VoucherFlowRecord record, String accountCode) {
        if (!StringUtils.hasText(accountCode)) {
            return true;
        }
        String keyword = normalizeCode(accountCode);
        return normalizeCode(record.cashAccountCodes).contains(keyword)
                || normalizeCode(record.counterpartyAccountCodes).contains(keyword);
    }

    private boolean matchesKeyword(VoucherFlowRecord record, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return containsText(record.voucherNumber, value)
                || containsText(record.summary, value)
                || containsText(record.cashflowItemCode, value)
                || containsText(record.cashflowItemName, value);
    }

    private boolean containsText(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private long countBySource(List<VoucherFlowRecord> records, String sourceType) {
        return records.stream().filter(item -> sourceType.equals(item.sourceType)).count();
    }

    private BigDecimal sumTraceAmount(List<VoucherFlowRecord> records, boolean inbound) {
        return records.stream()
                .map(item -> inbound ? item.cashInAmount : item.cashOutAmount)
                .reduce(BigDecimal.ZERO, this::safeAdd);
    }

    private BigDecimal sumTraceNet(List<VoucherFlowRecord> records) {
        return records.stream()
                .map(item -> item.netAmount)
                .reduce(BigDecimal.ZERO, this::safeAdd);
    }

    private BigDecimal safeAdd(BigDecimal left, BigDecimal right) {
        return safe(left).add(safe(right));
    }

    private String joinCodes(List<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String firstNonBlank(List<String> values) {
        return values.stream().filter(StringUtils::hasText).map(String::trim).findFirst().orElse(null);
    }

    private String voucherGroupKey(BizfiFiGlEntry entry) {
        if (entry == null) {
            return "ENTRY-NULL";
        }
        if (entry.getFvoucherId() != null) {
            return "V-" + entry.getFvoucherId();
        }
        return "E-" + entry.getFid();
    }

    private String categoryNameOf(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return null;
        }
        String normalizedCode = normalizeCode(categoryCode);
        if (OPERATING_CODE.equals(normalizedCode)) {
            return "Operating";
        }
        if (INVESTING_CODE.equals(normalizedCode)) {
            return "Investing";
        }
        if (FINANCING_CODE.equals(normalizedCode)) {
            return "Financing";
        }
        return categoryCode;
    }

    private String categoryBucketKey(VoucherFlowRecord record) {
        if (StringUtils.hasText(record.categoryCode)) {
            return normalizeCode(record.categoryCode);
        }
        if ("CASH_TRANSFER".equals(record.sourceType)) {
            return "ZZ_CASH_TRANSFER";
        }
        if ("MIXED_ITEM".equals(record.sourceType)) {
            return "ZY_MIXED_ITEM";
        }
        if ("UNKNOWN_ITEM".equals(record.sourceType)) {
            return "ZX_UNKNOWN_ITEM";
        }
        return "ZW_PENDING";
    }

    private String categoryBucketCode(VoucherFlowRecord record) {
        if (StringUtils.hasText(record.categoryCode)) {
            return record.categoryCode;
        }
        if ("CASH_TRANSFER".equals(record.sourceType)) {
            return "CASH_TRANSFER";
        }
        if ("MIXED_ITEM".equals(record.sourceType)) {
            return "MIXED_ITEM";
        }
        if ("UNKNOWN_ITEM".equals(record.sourceType)) {
            return "UNKNOWN_ITEM";
        }
        return "PENDING";
    }

    private String categoryBucketName(VoucherFlowRecord record) {
        if (StringUtils.hasText(record.categoryName)) {
            return record.categoryName;
        }
        if ("CASH_TRANSFER".equals(record.sourceType)) {
            return "Cash transfer";
        }
        if ("MIXED_ITEM".equals(record.sourceType)) {
            return "Mixed-code review";
        }
        if ("UNKNOWN_ITEM".equals(record.sourceType)) {
            return "Unknown code";
        }
        return "Pending supplement";
    }

    private int sourcePriority(String sourceType) {
        if ("UNKNOWN_ITEM".equals(sourceType)) {
            return 0;
        }
        if ("MIXED_ITEM".equals(sourceType)) {
            return 1;
        }
        if ("HEURISTIC".equals(sourceType)) {
            return 2;
        }
        if ("CASH_TRANSFER".equals(sourceType)) {
            return 3;
        }
        return 9;
    }

    private String pendingIssue(String sourceType) {
        if ("UNKNOWN_ITEM".equals(sourceType)) {
            return "Unknown cashflow item code";
        }
        if ("MIXED_ITEM".equals(sourceType)) {
            return "Multiple cashflow item codes in one voucher";
        }
        if ("HEURISTIC".equals(sourceType)) {
            return "No explicit tag; classified by account-type heuristic";
        }
        if ("CASH_TRANSFER".equals(sourceType)) {
            return "Transfer between cash-like accounts; skipped by statement";
        }
        return "Needs review";
    }

    private String pendingSuggestion(String sourceType) {
        if ("UNKNOWN_ITEM".equals(sourceType)) {
            return "Correct the cashflow item code or complete the master data.";
        }
        if ("MIXED_ITEM".equals(sourceType)) {
            return "Review voucher lines and keep one clear cashflow tagging rule.";
        }
        if ("HEURISTIC".equals(sourceType)) {
            return "Add an explicit cashflow item to the voucher lines.";
        }
        if ("CASH_TRANSFER".equals(sourceType)) {
            return "Decide whether internal transfers need separate disclosure.";
        }
        return "Review with business context and provide supporting notes.";
    }

    private String mapCategoryToReportCode(String category) {
        String value = normalize(category);
        if (containsAny(value, "\u6295\u8d44", "invest")) {
            return INVESTING_CODE;
        }
        if (containsAny(value, "\u7b79\u8d44", "financ")) {
            return FINANCING_CODE;
        }
        return OPERATING_CODE;
    }

    private boolean isCashLike(BizfiFiAccount account) {
        return account != null && (isTrue(account.getFcash()) || isTrue(account.getFbank()) || isTrue(account.getFequivalent()));
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private YearMonth parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static class Resolution {
        private final Long itemId;
        private final boolean heuristic;
        private final String rawCode;

        private Resolution(Long itemId, boolean heuristic, String rawCode) {
            this.itemId = itemId;
            this.heuristic = heuristic;
            this.rawCode = rawCode;
        }
    }

    private static class AnalysisContext {
        private Long orgId;
        private String period;
        private String currency;
        private YearMonth targetPeriod;
        private LocalDate startDate;
        private LocalDate endDate;
        private long postedVoucherCount;
        private int cashAccountCount;
        private int cashflowItemCount;
        private List<BizfiFiGlEntry> entries = new ArrayList<>();
        private Map<String, BizfiFiAccount> accountByCode = Collections.emptyMap();
        private Map<String, BizfiFiCashflowItem> cashflowItemByCode = Collections.emptyMap();
        private List<VoucherFlowRecord> records = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
    }

    private static class VoucherFlowRecord {
        private Long voucherId;
        private String voucherNumber;
        private LocalDate voucherDate;
        private String summary;
        private String cashAccountCodes;
        private String counterpartyAccountCodes;
        private String cashflowItemCode;
        private String cashflowItemName;
        private String categoryCode;
        private String categoryName;
        private String sourceType;
        private BigDecimal cashInAmount = BigDecimal.ZERO;
        private BigDecimal cashOutAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
        private int cashEntryCount;
        private int counterpartyEntryCount;
        private String postedBy;
        private LocalDateTime postedTime;
        private String reason;
    }

    private static class ExplicitResolution {
        private final String code;
        private final String name;
        private final String categoryCode;
        private final String categoryName;
        private final boolean recognized;

        private ExplicitResolution(String code, String name, String categoryCode, String categoryName, boolean recognized) {
            this.code = code;
            this.name = name;
            this.categoryCode = categoryCode;
            this.categoryName = categoryName;
            this.recognized = recognized;
        }
    }

    private static class SupplementCategoryAccumulator {
        private final String categoryCode;
        private final String categoryName;
        private int voucherCount;
        private BigDecimal cashInAmount = BigDecimal.ZERO;
        private BigDecimal cashOutAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
        private int directCount;
        private int heuristicCount;
        private int pendingCount;

        private SupplementCategoryAccumulator(String categoryCode, String categoryName) {
            this.categoryCode = categoryCode;
            this.categoryName = categoryName;
        }

        private void add(VoucherFlowRecord record) {
            voucherCount += 1;
            cashInAmount = cashInAmount.add(record.cashInAmount == null ? BigDecimal.ZERO : record.cashInAmount);
            cashOutAmount = cashOutAmount.add(record.cashOutAmount == null ? BigDecimal.ZERO : record.cashOutAmount);
            netAmount = netAmount.add(record.netAmount == null ? BigDecimal.ZERO : record.netAmount);
            if ("DIRECT".equals(record.sourceType)) {
                directCount += 1;
            } else if ("HEURISTIC".equals(record.sourceType)) {
                heuristicCount += 1;
                pendingCount += 1;
            } else {
                pendingCount += 1;
            }
        }

        private CashFlowSupplementCategoryVO toVO() {
            return new CashFlowSupplementCategoryVO(
                    categoryCode,
                    categoryName,
                    voucherCount,
                    cashInAmount.setScale(2, RoundingMode.HALF_UP),
                    cashOutAmount.setScale(2, RoundingMode.HALF_UP),
                    netAmount.setScale(2, RoundingMode.HALF_UP),
                    directCount,
                    heuristicCount,
                    pendingCount
            );
        }
    }
}
