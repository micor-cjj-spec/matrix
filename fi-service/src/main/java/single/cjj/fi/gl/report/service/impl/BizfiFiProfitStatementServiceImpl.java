package single.cjj.fi.gl.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.report.entity.BizfiFiReportAccountMap;
import single.cjj.fi.gl.report.entity.BizfiFiReportItem;
import single.cjj.fi.gl.report.entity.BizfiFiReportTemplate;
import single.cjj.fi.gl.report.mapper.BizfiFiReportAccountMapMapper;
import single.cjj.fi.gl.report.service.BizfiFiProfitStatementService;
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
public class BizfiFiProfitStatementServiceImpl implements BizfiFiProfitStatementService {

    private static final String CNY = "CNY";

    @Autowired
    private BizfiFiReportTemplateService reportTemplateService;

    @Autowired
    private BizfiFiReportItemService reportItemService;

    @Autowired
    private BizfiFiReportAccountMapMapper reportAccountMapMapper;

    @Autowired
    private BizfiFiAccountMapper accountMapper;

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Override
    public ReportQueryResultVO query(Long orgId, String startPeriod, String endPeriod, String currency, Long templateId, Boolean showZero) {
        ReportQueryResultVO result = new ReportQueryResultVO();
        result.setReportType("PROFIT_STATEMENT");
        result.setOrgId(orgId);
        result.setStartPeriod(startPeriod);
        result.setEndPeriod(endPeriod);
        result.setCurrency(StringUtils.hasText(currency) ? currency : CNY);
        result.setRows(new ArrayList<>());
        result.setChecks(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        BizfiFiReportTemplate template = reportTemplateService.getEnabledTemplate("PROFIT_STATEMENT", templateId);
        if (template == null) {
            result.getWarnings().add("No enabled profit-statement template is configured.");
            result.getChecks().add(new ReportCheckResultVO("TEMPLATE_READY", false, "Enable a profit-statement template first."));
            return result;
        }

        YearMonth endYm = parsePeriod(endPeriod);
        if (endYm == null) {
            result.getWarnings().add("Invalid or missing endPeriod. Use yyyy-MM, for example 2026-03.");
            result.getChecks().add(new ReportCheckResultVO("PERIOD_READY", false, "The requested endPeriod is invalid."));
            return result;
        }

        YearMonth startYm = parsePeriod(startPeriod);
        if (startYm == null) {
            startYm = endYm;
        }
        if (startYm.isAfter(endYm)) {
            YearMonth temp = startYm;
            startYm = endYm;
            endYm = temp;
        }

        result.setStartPeriod(startYm.toString());
        result.setEndPeriod(endYm.toString());
        result.setTemplateId(template.getFid());
        result.setTemplateName(template.getFname());

        List<BizfiFiReportItem> items = reportItemService.listByTemplateId(template.getFid());
        if (items.isEmpty()) {
            result.getWarnings().add("The profit-statement template has no report items yet.");
            result.getChecks().add(new ReportCheckResultVO("TEMPLATE_ITEMS", false, "Add report items before querying."));
            return result;
        }

        List<BizfiFiAccount> accounts = loadAccounts(orgId);
        if (accounts.isEmpty()) {
            result.getWarnings().add("No accounts were found for the requested organization.");
            result.getChecks().add(new ReportCheckResultVO("ACCOUNT_READY", false, "No account subjects are available."));
            result.setRows(toRows(items, Collections.emptyMap(), Collections.emptyMap(), Boolean.TRUE.equals(showZero)));
            return result;
        }

        LocalDate startDate = startYm.atDay(1);
        LocalDate endDate = endYm.atEndOfMonth();
        LocalDate ytdStart = LocalDate.of(endYm.getYear(), 1, 1);

        Set<String> accountCodes = accounts.stream()
                .map(BizfiFiAccount::getFcode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, List<BizfiFiGlEntry>> entriesByCode = loadEntriesByCode(accountCodes, ytdStart, endDate);
        if (entriesByCode.isEmpty()) {
            result.getWarnings().add("No posted ledger entries were found within the requested year-to-date period.");
        }

        Map<Long, Long> explicitItemByAccount = loadExplicitMapping(template.getFid());
        Map<Long, BizfiFiReportItem> itemById = items.stream()
                .collect(Collectors.toMap(BizfiFiReportItem::getFid, item -> item, (left, right) -> left));
        Map<String, BizfiFiReportItem> itemByCode = items.stream()
                .filter(item -> StringUtils.hasText(item.getFcode()))
                .collect(Collectors.toMap(item -> item.getFcode().toUpperCase(Locale.ROOT), item -> item, (left, right) -> left));

        Map<Long, BigDecimal> directCurrentAmount = new HashMap<>();
        Map<Long, BigDecimal> directYtdAmount = new HashMap<>();
        List<String> unmappedAccounts = new ArrayList<>();

        for (BizfiFiAccount account : accounts) {
            List<BizfiFiGlEntry> accountEntries = entriesByCode.getOrDefault(account.getFcode(), Collections.emptyList());
            if (accountEntries.isEmpty()) {
                continue;
            }

            BigDecimal currentDebit = BigDecimal.ZERO;
            BigDecimal currentCredit = BigDecimal.ZERO;
            BigDecimal ytdDebit = BigDecimal.ZERO;
            BigDecimal ytdCredit = BigDecimal.ZERO;

            for (BizfiFiGlEntry entry : accountEntries) {
                LocalDate voucherDate = entry.getFvoucherDate();
                if (voucherDate == null) {
                    continue;
                }

                ytdDebit = ytdDebit.add(safe(entry.getFdebitAmount()));
                ytdCredit = ytdCredit.add(safe(entry.getFcreditAmount()));

                if (!voucherDate.isBefore(startDate) && !voucherDate.isAfter(endDate)) {
                    currentDebit = currentDebit.add(safe(entry.getFdebitAmount()));
                    currentCredit = currentCredit.add(safe(entry.getFcreditAmount()));
                }
            }

            BigDecimal currentAmount = calculateSignedAmount(account, currentDebit, currentCredit);
            BigDecimal ytdAmount = calculateSignedAmount(account, ytdDebit, ytdCredit);
            if (currentAmount.compareTo(BigDecimal.ZERO) == 0 && ytdAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Long itemId = resolveProfitStatementItemId(account, explicitItemByAccount, itemById, itemByCode);
            if (itemId == null) {
                unmappedAccounts.add(account.getFcode() + "-" + account.getFname());
                continue;
            }

            directCurrentAmount.merge(itemId, currentAmount, BigDecimal::add);
            directYtdAmount.merge(itemId, ytdAmount, BigDecimal::add);
        }

        if (!unmappedAccounts.isEmpty()) {
            result.getWarnings().add("Unmapped profit-statement accounts: " + String.join(", ", unmappedAccounts.stream().limit(8).toList()));
        }

        applyNetProfitFormula(items, directCurrentAmount, directYtdAmount);

        Map<Long, BigDecimal> rolledCurrent = rollupAmounts(items, directCurrentAmount);
        Map<Long, BigDecimal> rolledYtd = rollupAmounts(items, directYtdAmount);

        result.setRows(toRows(items, rolledCurrent, rolledYtd, Boolean.TRUE.equals(showZero)));
        result.getChecks().add(new ReportCheckResultVO("PROFIT_QUERY", true, "Profit statement amounts were aggregated from posted general-ledger entries."));
        if (orgId != null) {
            result.getWarnings().add("Profit-statement amounts are currently aggregated by account code. If multiple organizations reuse the same code set, add org-level ledger isolation later.");
        }
        return result;
    }

    private List<BizfiFiAccount> loadAccounts(Long orgId) {
        LambdaQueryWrapper<BizfiFiAccount> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(BizfiFiAccount::getForg, orgId);
        }
        wrapper.orderByAsc(BizfiFiAccount::getFcode);
        return accountMapper.selectList(wrapper);
    }

    private Map<String, List<BizfiFiGlEntry>> loadEntriesByCode(Set<String> accountCodes, LocalDate startDate, LocalDate endDate) {
        if (accountCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BizfiFiGlEntry> entries = glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .in(BizfiFiGlEntry::getFaccountCode, accountCodes)
                .ge(BizfiFiGlEntry::getFvoucherDate, startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFid));
        return entries.stream().collect(Collectors.groupingBy(BizfiFiGlEntry::getFaccountCode));
    }

    private Map<Long, Long> loadExplicitMapping(Long templateId) {
        List<BizfiFiReportAccountMap> mappings = reportAccountMapMapper.selectList(new LambdaQueryWrapper<BizfiFiReportAccountMap>()
                .eq(BizfiFiReportAccountMap::getFtemplateId, templateId)
                .orderByAsc(BizfiFiReportAccountMap::getFid));
        Map<Long, Long> itemByAccount = new HashMap<>();
        for (BizfiFiReportAccountMap mapping : mappings) {
            if (mapping.getFaccountId() != null && mapping.getFitemId() != null) {
                itemByAccount.putIfAbsent(mapping.getFaccountId(), mapping.getFitemId());
            }
        }
        return itemByAccount;
    }

    private Long resolveProfitStatementItemId(
            BizfiFiAccount account,
            Map<Long, Long> explicitItemByAccount,
            Map<Long, BizfiFiReportItem> itemById,
            Map<String, BizfiFiReportItem> itemByCode
    ) {
        if (account.getFid() != null) {
            Long explicitItemId = explicitItemByAccount.get(account.getFid());
            if (explicitItemId != null && itemById.containsKey(explicitItemId)) {
                return explicitItemId;
            }
        }

        if (account.getFreportItem() != null && itemById.containsKey(account.getFreportItem())) {
            return account.getFreportItem();
        }

        String profitLossType = normalize(account.getFpltype());
        String accountType = normalize(account.getFtype());

        if (containsAny(profitLossType, "\u6536\u5165", "revenue", "income")
                || containsAny(accountType, "\u6536\u5165", "revenue", "income")) {
            BizfiFiReportItem revenueItem = itemByCode.get("PL_REVENUE");
            if (revenueItem != null) {
                return revenueItem.getFid();
            }
        }

        if (containsAny(profitLossType, "\u6210\u672c", "\u8d39\u7528", "cost", "expense")
                || containsAny(accountType, "\u6210\u672c", "\u8d39\u7528", "cost", "expense")) {
            BizfiFiReportItem costItem = itemByCode.get("PL_COST");
            if (costItem != null) {
                return costItem.getFid();
            }
        }

        return null;
    }

    private void applyNetProfitFormula(
            List<BizfiFiReportItem> items,
            Map<Long, BigDecimal> currentAmounts,
            Map<Long, BigDecimal> ytdAmounts
    ) {
        Long revenueItemId = findItemIdByCode(items, "PL_REVENUE");
        Long costItemId = findItemIdByCode(items, "PL_COST");
        Long netProfitItemId = findItemIdByCode(items, "PL_NET_PROFIT");
        if (netProfitItemId == null) {
            return;
        }

        BigDecimal revenueCurrent = safe(currentAmounts.get(revenueItemId));
        BigDecimal costCurrent = safe(currentAmounts.get(costItemId));
        BigDecimal revenueYtd = safe(ytdAmounts.get(revenueItemId));
        BigDecimal costYtd = safe(ytdAmounts.get(costItemId));

        currentAmounts.put(netProfitItemId, revenueCurrent.subtract(costCurrent));
        ytdAmounts.put(netProfitItemId, revenueYtd.subtract(costYtd));
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

    private List<ReportRowVO> toRows(
            List<BizfiFiReportItem> items,
            Map<Long, BigDecimal> currentAmountByItem,
            Map<Long, BigDecimal> ytdAmountByItem,
            boolean showZero
    ) {
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
                        null,
                        null,
                        safe(currentAmountByItem.get(item.getFid())),
                        safe(ytdAmountByItem.get(item.getFid())),
                        item.getFdrillable() != null && item.getFdrillable() == 1,
                        Collections.emptyList()
                ))
                .filter(row -> showZero || row.getCurrentAmount().compareTo(BigDecimal.ZERO) != 0 || row.getYtdAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
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

    private BigDecimal calculateSignedAmount(BizfiFiAccount account, BigDecimal debit, BigDecimal credit) {
        return isDebitDirection(account) ? debit.subtract(credit) : credit.subtract(debit);
    }

    private boolean isDebitDirection(BizfiFiAccount account) {
        String direction = normalize(account.getFdirection());
        if (containsAny(direction, "\u501f", "debit")) {
            return true;
        }
        if (containsAny(direction, "\u8d37", "credit")) {
            return false;
        }

        String profitLossType = normalize(account.getFpltype());
        String accountType = normalize(account.getFtype());
        return containsAny(profitLossType, "\u6210\u672c", "\u8d39\u7528", "cost", "expense")
                || containsAny(accountType, "\u6210\u672c", "\u8d39\u7528", "cost", "expense");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
