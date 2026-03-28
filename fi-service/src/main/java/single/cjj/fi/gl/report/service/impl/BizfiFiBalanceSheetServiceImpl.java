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
import single.cjj.fi.gl.report.service.BizfiFiBalanceSheetService;
import single.cjj.fi.gl.report.service.BizfiFiReportItemService;
import single.cjj.fi.gl.report.service.BizfiFiReportTemplateService;
import single.cjj.fi.gl.report.vo.BalanceSheetDrillResultVO;
import single.cjj.fi.gl.report.vo.BalanceSheetDrillRowVO;
import single.cjj.fi.gl.report.util.ReportTextFixer;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BizfiFiBalanceSheetServiceImpl implements BizfiFiBalanceSheetService {

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
    public ReportQueryResultVO query(Long orgId, String period, String currency, Long templateId, Boolean showZero) {
        ReportQueryResultVO result = new ReportQueryResultVO();
        result.setReportType("BALANCE_SHEET");
        result.setOrgId(orgId);
        result.setPeriod(period);
        result.setCurrency(StringUtils.hasText(currency) ? currency : CNY);
        result.setRows(new ArrayList<>());
        result.setChecks(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        BizfiFiReportTemplate template = reportTemplateService.getEnabledTemplate("BALANCE_SHEET", templateId, orgId);
        if (template == null) {
            result.getWarnings().add("No enabled balance-sheet template is configured.");
            result.getChecks().add(new ReportCheckResultVO("TEMPLATE_READY", false, "Enable a balance-sheet template first."));
            return result;
        }

        YearMonth targetPeriod = parsePeriod(period);
        if (targetPeriod == null) {
            result.getWarnings().add("Invalid or missing period. Use yyyy-MM, for example 2026-03.");
            result.getChecks().add(new ReportCheckResultVO("PERIOD_READY", false, "The requested period is invalid."));
            return result;
        }

        result.setPeriod(targetPeriod.toString());
        result.setTemplateId(template.getFid());
        result.setTemplateName(ReportTextFixer.fixTemplateName(template.getFcode(), template.getFname()));
        appendTemplateWarnings(result.getWarnings(), template, orgId, templateId);

        List<BizfiFiReportItem> items = reportItemService.listByTemplateId(template.getFid());
        if (items.isEmpty()) {
            result.getWarnings().add("The balance-sheet template has no report items yet.");
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

        LocalDate yearStart = LocalDate.of(targetPeriod.getYear(), 1, 1);
        LocalDate periodEnd = targetPeriod.atEndOfMonth();

        Set<String> accountCodes = accounts.stream()
                .map(BizfiFiAccount::getFcode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, List<BizfiFiGlEntry>> entriesByCode = loadEntriesByCode(accountCodes, periodEnd);
        Map<Long, Long> explicitItemByAccount = loadExplicitMapping(template.getFid());
        Map<Long, BizfiFiReportItem> itemById = items.stream()
                .collect(Collectors.toMap(BizfiFiReportItem::getFid, item -> item, (a, b) -> a));
        Map<String, BizfiFiReportItem> itemByCode = items.stream()
                .filter(item -> StringUtils.hasText(item.getFcode()))
                .collect(Collectors.toMap(item -> item.getFcode().toUpperCase(Locale.ROOT), item -> item, (a, b) -> a));

        Map<Long, BigDecimal> directBeginAmount = new HashMap<>();
        Map<Long, BigDecimal> directEndAmount = new HashMap<>();
        Map<String, Integer> mappingStats = new LinkedHashMap<>();
        List<String> unmappedAccounts = new ArrayList<>();

        for (BizfiFiAccount account : accounts) {
            List<BizfiFiGlEntry> accountEntries = entriesByCode.getOrDefault(account.getFcode(), Collections.emptyList());
            if (accountEntries.isEmpty()) {
                continue;
            }

            BigDecimal beginDebit = BigDecimal.ZERO;
            BigDecimal beginCredit = BigDecimal.ZERO;
            BigDecimal endDebit = BigDecimal.ZERO;
            BigDecimal endCredit = BigDecimal.ZERO;

            for (BizfiFiGlEntry entry : accountEntries) {
                LocalDate voucherDate = entry.getFvoucherDate();
                if (voucherDate != null && voucherDate.isBefore(yearStart)) {
                    beginDebit = beginDebit.add(safe(entry.getFdebitAmount()));
                    beginCredit = beginCredit.add(safe(entry.getFcreditAmount()));
                }
                endDebit = endDebit.add(safe(entry.getFdebitAmount()));
                endCredit = endCredit.add(safe(entry.getFcreditAmount()));
            }

            boolean debitDirection = isDebitDirection(account);
            BigDecimal beginBalance = calculateNormalBalance(beginDebit, beginCredit, debitDirection);
            BigDecimal endBalance = calculateNormalBalance(endDebit, endCredit, debitDirection);

            if (beginBalance.compareTo(BigDecimal.ZERO) == 0 && endBalance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BalanceSheetMapping mapping = resolveBalanceSheetMapping(account, explicitItemByAccount, itemById, itemByCode);
            if (mapping.getItemId() == null) {
                unmappedAccounts.add(account.getFcode() + "-" + account.getFname());
                continue;
            }

            directBeginAmount.merge(mapping.getItemId(), beginBalance, BigDecimal::add);
            directEndAmount.merge(mapping.getItemId(), endBalance, BigDecimal::add);
            mappingStats.merge(mapping.getSource(), 1, Integer::sum);
        }

        if (!unmappedAccounts.isEmpty()) {
            result.getWarnings().add("Unmapped balance-sheet accounts: " + String.join(", ", unmappedAccounts.stream().limit(8).toList()));
        }

        Map<Long, BigDecimal> rolledBegin = rollupAmounts(items, directBeginAmount);
        Map<Long, BigDecimal> rolledEnd = rollupAmounts(items, directEndAmount);

        result.setRows(toRows(items, rolledEnd, rolledBegin, Boolean.TRUE.equals(showZero)));
        appendMappingWarnings(result.getWarnings(), mappingStats);
        appendBalanceCheck(result.getChecks(), items, rolledEnd, "ENDING_BALANCE_CHECK", "Ending balance");
        appendBalanceCheck(result.getChecks(), items, rolledBegin, "OPENING_BALANCE_CHECK", "Opening balance");
        if (orgId != null) {
            result.getWarnings().add("Amounts are currently aggregated by account code. Add org-level ledger isolation later if multiple organizations share the same code set.");
        }
        return result;
    }

    @Override
    public BalanceSheetDrillResultVO drill(Long orgId, String period, String currency, Long templateId, Long itemId, String itemCode) {
        BalanceSheetDrillResultVO result = new BalanceSheetDrillResultVO();
        result.setOrgId(orgId);
        result.setCurrency(StringUtils.hasText(currency) ? currency : CNY);
        result.setPeriod(period);
        result.setRows(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        BizfiFiReportTemplate template = reportTemplateService.getEnabledTemplate("BALANCE_SHEET", templateId, orgId);
        if (template == null) {
            result.getWarnings().add("No enabled balance-sheet template is configured.");
            return result;
        }

        YearMonth targetPeriod = parsePeriod(period);
        if (targetPeriod == null) {
            result.getWarnings().add("Invalid or missing period. Use yyyy-MM, for example 2026-03.");
            return result;
        }

        result.setTemplateId(template.getFid());
        result.setTemplateName(ReportTextFixer.fixTemplateName(template.getFcode(), template.getFname()));
        result.setPeriod(targetPeriod.toString());
        appendTemplateWarnings(result.getWarnings(), template, orgId, templateId);

        List<BizfiFiReportItem> items = reportItemService.listByTemplateId(template.getFid());
        if (items.isEmpty()) {
            result.getWarnings().add("The balance-sheet template has no report items yet.");
            return result;
        }

        BizfiFiReportItem selectedItem = resolveSelectedItem(items, itemId, itemCode);
        if (selectedItem == null) {
            result.getWarnings().add("The requested report item was not found in the balance-sheet template.");
            return result;
        }

        result.setItemId(selectedItem.getFid());
        result.setItemCode(selectedItem.getFcode());
        result.setItemName(ReportTextFixer.fixItemName(selectedItem.getFcode(), selectedItem.getFname()));
        if (selectedItem.getFdrillable() != null && selectedItem.getFdrillable() != 1) {
            result.getWarnings().add("The selected row is not marked as drillable in the template. Detail is shown for debugging only.");
        }

        List<BizfiFiAccount> accounts = loadAccounts(orgId);
        if (accounts.isEmpty()) {
            result.getWarnings().add("No accounts were found for the requested organization.");
            return result;
        }

        LocalDate yearStart = LocalDate.of(targetPeriod.getYear(), 1, 1);
        LocalDate periodEnd = targetPeriod.atEndOfMonth();

        Set<String> accountCodes = accounts.stream()
                .map(BizfiFiAccount::getFcode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, List<BizfiFiGlEntry>> entriesByCode = loadEntriesByCode(accountCodes, periodEnd);
        Map<Long, Long> explicitItemByAccount = loadExplicitMapping(template.getFid());
        Map<Long, BizfiFiReportItem> itemById = items.stream()
                .collect(Collectors.toMap(BizfiFiReportItem::getFid, item -> item, (a, b) -> a));
        Map<String, BizfiFiReportItem> itemByCode = items.stream()
                .filter(item -> StringUtils.hasText(item.getFcode()))
                .collect(Collectors.toMap(item -> item.getFcode().toUpperCase(Locale.ROOT), item -> item, (a, b) -> a));
        Set<Long> includedItemIds = collectItemIds(items, selectedItem.getFid());

        List<BalanceSheetDrillRowVO> drillRows = new ArrayList<>();
        for (BizfiFiAccount account : accounts) {
            List<BizfiFiGlEntry> accountEntries = entriesByCode.getOrDefault(account.getFcode(), Collections.emptyList());
            if (accountEntries.isEmpty()) {
                continue;
            }

            BigDecimal beginDebit = BigDecimal.ZERO;
            BigDecimal beginCredit = BigDecimal.ZERO;
            BigDecimal endDebit = BigDecimal.ZERO;
            BigDecimal endCredit = BigDecimal.ZERO;

            for (BizfiFiGlEntry entry : accountEntries) {
                LocalDate voucherDate = entry.getFvoucherDate();
                if (voucherDate != null && voucherDate.isBefore(yearStart)) {
                    beginDebit = beginDebit.add(safe(entry.getFdebitAmount()));
                    beginCredit = beginCredit.add(safe(entry.getFcreditAmount()));
                }
                endDebit = endDebit.add(safe(entry.getFdebitAmount()));
                endCredit = endCredit.add(safe(entry.getFcreditAmount()));
            }

            boolean debitDirection = isDebitDirection(account);
            BigDecimal beginBalance = calculateNormalBalance(beginDebit, beginCredit, debitDirection);
            BigDecimal endBalance = calculateNormalBalance(endDebit, endCredit, debitDirection);
            if (beginBalance.compareTo(BigDecimal.ZERO) == 0 && endBalance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BalanceSheetMapping mapping = resolveBalanceSheetMapping(account, explicitItemByAccount, itemById, itemByCode);
            if (mapping.getItemId() == null || !includedItemIds.contains(mapping.getItemId())) {
                continue;
            }

            drillRows.add(new BalanceSheetDrillRowVO(
                    account.getFid(),
                    account.getFcode(),
                    account.getFname(),
                    account.getFtype(),
                    account.getFdirection(),
                    beginBalance,
                    endBalance,
                    mapping.getSource()
            ));
        }

        drillRows.sort(Comparator.comparing(BalanceSheetDrillRowVO::getAccountCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(BalanceSheetDrillRowVO::getAccountId, Comparator.nullsLast(Long::compareTo)));
        result.setRows(drillRows);
        if (drillRows.isEmpty()) {
            result.getWarnings().add("No detail accounts matched the selected balance-sheet item.");
        } else if (includedItemIds.size() > 1) {
            result.getWarnings().add("The selected report row is a summary row. Detail includes all mapped accounts from its child items.");
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

    private Map<String, List<BizfiFiGlEntry>> loadEntriesByCode(Set<String> accountCodes, LocalDate endDate) {
        if (accountCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BizfiFiGlEntry> entries = glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .in(BizfiFiGlEntry::getFaccountCode, accountCodes)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFid));
        return entries.stream().collect(Collectors.groupingBy(BizfiFiGlEntry::getFaccountCode));
    }

    private Map<Long, Long> loadExplicitMapping(Long templateId) {
        List<BizfiFiReportAccountMap> mappings = reportAccountMapMapper.selectList(
                new LambdaQueryWrapper<BizfiFiReportAccountMap>()
                        .eq(BizfiFiReportAccountMap::getFtemplateId, templateId)
                        .orderByAsc(BizfiFiReportAccountMap::getFid)
        );
        Map<Long, Long> itemByAccount = new HashMap<>();
        for (BizfiFiReportAccountMap mapping : mappings) {
            if (mapping.getFaccountId() != null && mapping.getFitemId() != null) {
                itemByAccount.putIfAbsent(mapping.getFaccountId(), mapping.getFitemId());
            }
        }
        return itemByAccount;
    }

    private Long resolveBalanceSheetItemId(
            BizfiFiAccount account,
            Map<Long, Long> explicitItemByAccount,
            Map<Long, BizfiFiReportItem> itemById,
            Map<String, BizfiFiReportItem> itemByCode
    ) {
        return resolveBalanceSheetMapping(account, explicitItemByAccount, itemById, itemByCode).getItemId();
    }

    private BalanceSheetMapping resolveBalanceSheetMapping(
            BizfiFiAccount account,
            Map<Long, Long> explicitItemByAccount,
            Map<Long, BizfiFiReportItem> itemById,
            Map<String, BizfiFiReportItem> itemByCode
    ) {
        if (account.getFid() != null) {
            Long explicitItemId = explicitItemByAccount.get(account.getFid());
            if (explicitItemId != null && itemById.containsKey(explicitItemId)) {
                return new BalanceSheetMapping(explicitItemId, "EXPLICIT");
            }
        }

        if (account.getFreportItem() != null && itemById.containsKey(account.getFreportItem())) {
            return new BalanceSheetMapping(account.getFreportItem(), "ACCOUNT_REPORT_ITEM");
        }

        if (isCashLike(account)) {
            BizfiFiReportItem cashItem = itemByCode.get("BS_CASH");
            if (cashItem != null) {
                return new BalanceSheetMapping(cashItem.getFid(), "AUTO_CASH");
            }
        }

        if (isAssetType(account.getFtype())) {
            BizfiFiReportItem assetItem = itemByCode.get("BS_ASSET");
            if (assetItem != null) {
                return new BalanceSheetMapping(assetItem.getFid(), "AUTO_ASSET");
            }
        }

        if (isLiabilityOrEquityType(account.getFtype())) {
            BizfiFiReportItem liabilityItem = itemByCode.get("BS_LIAB_EQ");
            if (liabilityItem != null) {
                return new BalanceSheetMapping(liabilityItem.getFid(), "AUTO_LIAB_EQ");
            }
        }

        return new BalanceSheetMapping(null, "UNMAPPED");
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
            Map<Long, BigDecimal> amountByItem,
            Map<Long, BigDecimal> beginAmountByItem,
            boolean showZero
    ) {
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
                        safe(beginAmountByItem.get(item.getFid())),
                        null,
                        null,
                        item.getFdrillable() != null && item.getFdrillable() == 1,
                        Collections.emptyList()
                ))
                .filter(row -> showZero || row.getAmount().compareTo(BigDecimal.ZERO) != 0 || row.getBeginAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    private void appendBalanceCheck(
            List<ReportCheckResultVO> checks,
            List<BizfiFiReportItem> items,
            Map<Long, BigDecimal> amountByItem,
            String code,
            String label
    ) {
        BigDecimal assetTotal = findAmountByCode(items, amountByItem, "BS_ASSET");
        BigDecimal liabilityTotal = findAmountByCode(items, amountByItem, "BS_LIAB_EQ");
        if (assetTotal == null || liabilityTotal == null) {
            checks.add(new ReportCheckResultVO(code, false, label + " check cannot run because the required total items are missing."));
            return;
        }

        BigDecimal diff = assetTotal.subtract(liabilityTotal);
        boolean passed = diff.compareTo(BigDecimal.ZERO) == 0;
        String message = passed
                ? label + " matches liabilities plus equity."
                : label + " is not balanced. Difference: " + diff.toPlainString();
        checks.add(new ReportCheckResultVO(code, passed, message));
    }

    private BigDecimal findAmountByCode(List<BizfiFiReportItem> items, Map<Long, BigDecimal> amountByItem, String code) {
        for (BizfiFiReportItem item : items) {
            if (code.equalsIgnoreCase(item.getFcode())) {
                return safe(amountByItem.get(item.getFid()));
            }
        }
        return null;
    }

    private BizfiFiReportItem resolveSelectedItem(List<BizfiFiReportItem> items, Long itemId, String itemCode) {
        if (itemId != null) {
            for (BizfiFiReportItem item : items) {
                if (itemId.equals(item.getFid())) {
                    return item;
                }
            }
        }
        if (StringUtils.hasText(itemCode)) {
            for (BizfiFiReportItem item : items) {
                if (itemCode.trim().equalsIgnoreCase(item.getFcode())) {
                    return item;
                }
            }
        }
        return null;
    }

    private Set<Long> collectItemIds(List<BizfiFiReportItem> items, Long rootItemId) {
        if (rootItemId == null) {
            return Collections.emptySet();
        }
        Map<Long, List<BizfiFiReportItem>> childrenByParent = items.stream()
                .filter(item -> item.getFparentId() != null)
                .collect(Collectors.groupingBy(BizfiFiReportItem::getFparentId));
        Set<Long> collected = new HashSet<>();
        collectChildItemIds(rootItemId, childrenByParent, collected);
        return collected;
    }

    private void collectChildItemIds(Long itemId, Map<Long, List<BizfiFiReportItem>> childrenByParent, Set<Long> collected) {
        if (itemId == null || !collected.add(itemId)) {
            return;
        }
        for (BizfiFiReportItem child : childrenByParent.getOrDefault(itemId, Collections.emptyList())) {
            collectChildItemIds(child.getFid(), childrenByParent, collected);
        }
    }

    private void appendTemplateWarnings(List<String> warnings, BizfiFiReportTemplate template, Long orgId, Long requestedTemplateId) {
        if (template == null) {
            return;
        }
        if (orgId == null) {
            warnings.add("No organization was selected. The current result is based on all accounts visible to the service.");
            return;
        }
        if (template.getForg() == null) {
            warnings.add("Using a global balance-sheet template because no org-specific enabled template was found.");
            return;
        }
        if (!orgId.equals(template.getForg())) {
            if (requestedTemplateId != null) {
                warnings.add("The selected balance-sheet template belongs to organization " + template.getForg() + ".");
            } else {
                warnings.add("No enabled template was found for organization " + orgId + ". Using template from organization " + template.getForg() + " instead.");
            }
        }
    }

    private void appendMappingWarnings(List<String> warnings, Map<String, Integer> mappingStats) {
        if (mappingStats.isEmpty()) {
            return;
        }
        int explicitCount = mappingStats.getOrDefault("EXPLICIT", 0);
        int accountItemCount = mappingStats.getOrDefault("ACCOUNT_REPORT_ITEM", 0);
        int autoCount = mappingStats.getOrDefault("AUTO_CASH", 0)
                + mappingStats.getOrDefault("AUTO_ASSET", 0)
                + mappingStats.getOrDefault("AUTO_LIAB_EQ", 0);
        warnings.add("Mapping summary: explicit=" + explicitCount
                + ", account-item=" + accountItemCount
                + ", auto-classified=" + autoCount + ".");
        if (autoCount > 0) {
            warnings.add("Some accounts are still auto-classified by account type or cash tags. Consider maintaining explicit report-account mappings before go-live.");
        }
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

    private boolean isDebitDirection(BizfiFiAccount account) {
        String direction = normalize(account.getFdirection());
        if (direction.contains("\u501f") || direction.contains("debit")) {
            return true;
        }
        if (direction.contains("\u8d37") || direction.contains("credit")) {
            return false;
        }
        return isAssetType(account.getFtype()) || isExpenseLike(account.getFtype()) || isCostLike(account.getFtype());
    }

    private boolean isAssetType(String type) {
        String value = normalize(type);
        return value.contains("\u8d44\u4ea7") || value.contains("asset");
    }

    private boolean isLiabilityOrEquityType(String type) {
        String value = normalize(type);
        return value.contains("\u8d1f\u503a") || value.contains("liability") || value.contains("\u6743\u76ca") || value.contains("equity");
    }

    private boolean isExpenseLike(String type) {
        String value = normalize(type);
        return value.contains("\u8d39\u7528") || value.contains("expense") || value.contains("\u635f\u76ca");
    }

    private boolean isCostLike(String type) {
        String value = normalize(type);
        return value.contains("\u6210\u672c") || value.contains("cost");
    }

    private boolean isCashLike(BizfiFiAccount account) {
        return isTrue(account.getFcash()) || isTrue(account.getFbank()) || isTrue(account.getFequivalent());
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private BigDecimal calculateNormalBalance(BigDecimal debit, BigDecimal credit, boolean debitDirection) {
        return debitDirection ? debit.subtract(credit) : credit.subtract(debit);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static class BalanceSheetMapping {
        private final Long itemId;
        private final String source;

        private BalanceSheetMapping(Long itemId, String source) {
            this.itemId = itemId;
            this.source = source;
        }

        public Long getItemId() {
            return itemId;
        }

        public String getSource() {
            return source;
        }
    }
}
