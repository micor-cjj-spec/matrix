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
import single.cjj.fi.gl.report.vo.ReportCheckResultVO;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;
import single.cjj.fi.gl.report.vo.ReportRowVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
        result.setTemplateName(template.getFname());

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
                        item.getFname(),
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

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
}
