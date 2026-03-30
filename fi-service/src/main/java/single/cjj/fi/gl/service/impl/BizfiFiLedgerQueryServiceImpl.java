package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiAccount;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.mapper.BizfiFiAccountMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.report.entity.BizfiFiCashflowItem;
import single.cjj.fi.gl.report.mapper.BizfiFiCashflowItemMapper;
import single.cjj.fi.gl.service.BizfiFiLedgerQueryService;
import single.cjj.fi.gl.vo.LedgerBalanceRowVO;
import single.cjj.fi.gl.vo.LedgerBookRowVO;
import single.cjj.fi.gl.vo.LedgerDailyRowVO;
import single.cjj.fi.gl.vo.LedgerDimensionBalanceRowVO;
import single.cjj.fi.gl.vo.LedgerQueryResultVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
public class BizfiFiLedgerQueryServiceImpl implements BizfiFiLedgerQueryService {

    private static final String DIMENSION_UNASSIGNED = "__UNASSIGNED__";

    @Autowired
    private BizfiFiGlEntryMapper glEntryMapper;

    @Autowired
    private BizfiFiAccountMapper accountMapper;

    @Autowired
    private BizfiFiCashflowItemMapper cashflowItemMapper;

    @Override
    public LedgerQueryResultVO subjectBalance(String startDate, String endDate, String accountCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, null);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, null);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BalanceAccumulator> grouped = new LinkedHashMap<>();
        accumulateByAccount(grouped, openingEntries, periodEntries, accountMap);

        List<LedgerBalanceRowVO> records = grouped.values().stream()
                .map(BalanceAccumulator::toBalanceRow)
                .sorted(Comparator.comparing(LedgerBalanceRowVO::getAccountCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        if (!StringUtils.hasText(accountCode)) {
            result.getWarnings().add("未指定科目编码时，将展示当前期间全部已过账科目余额。");
        }
        return result;
    }

    @Override
    public LedgerQueryResultVO generalLedger(String startDate, String endDate, String accountCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, null);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, null);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BigDecimal> openingNet = computeOpeningNetByAccount(openingEntries);

        List<LedgerBookRowVO> records = buildGeneralLedgerRows(periodEntries, accountMap, openingNet);
        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        if (!StringUtils.hasText(accountCode)) {
            result.getWarnings().add("未指定科目编码时，总分类账将按全部科目流水混合展示，建议按科目查询。");
        }
        return result;
    }

    @Override
    public LedgerQueryResultVO detailLedger(String startDate, String endDate, String accountCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, null);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, null);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BigDecimal> openingNet = computeOpeningNetByAccount(openingEntries);

        List<LedgerBookRowVO> records = buildDetailLedgerRows(periodEntries, accountMap, openingNet);
        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        if (!StringUtils.hasText(accountCode)) {
            result.getWarnings().add("未指定科目编码时，明细分类账会展示全部科目的分录明细。");
        }
        return result;
    }

    @Override
    public LedgerQueryResultVO dailyReport(String startDate, String endDate, String accountCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, null);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, null);

        BigDecimal openingNet = sumNet(openingEntries);
        Map<LocalDate, List<BizfiFiGlEntry>> grouped = periodEntries.stream()
                .collect(Collectors.groupingBy(BizfiFiGlEntry::getFvoucherDate, LinkedHashMap::new, Collectors.toList()));

        List<LedgerDailyRowVO> records = new ArrayList<>();
        BigDecimal runningNet = openingNet;
        for (Map.Entry<LocalDate, List<BizfiFiGlEntry>> entry : grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            LocalDate date = entry.getKey();
            List<BizfiFiGlEntry> rows = entry.getValue();
            BigDecimal debit = sumDebit(rows);
            BigDecimal credit = sumCredit(rows);
            LedgerAmount openingAmount = toAmount(runningNet);
            runningNet = runningNet.add(debit).subtract(credit);
            LedgerAmount closingAmount = toAmount(runningNet);
            int voucherCount = (int) rows.stream()
                    .map(BizfiFiGlEntry::getFvoucherId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            records.add(new LedgerDailyRowVO(
                    date,
                    voucherCount,
                    openingAmount.amount,
                    openingAmount.direction,
                    normalize(debit),
                    normalize(credit),
                    closingAmount.amount,
                    closingAmount.direction
            ));
        }

        LedgerQueryResultVO result = buildResult(records, periodEntries);
        result.getSummary().put("voucherCount", periodEntries.stream()
                .map(BizfiFiGlEntry::getFvoucherId)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        appendBaseWarnings(result, range);
        return result;
    }

    @Override
    public LedgerQueryResultVO dimensionBalance(String startDate, String endDate, String accountCode, String dimensionCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, dimensionCode);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, dimensionCode);

        Set<String> dimensionCodes = collectDimensionCodes(openingEntries, periodEntries);
        Map<String, BizfiFiCashflowItem> dimensionMap = loadDimensionMap(dimensionCodes);
        Map<String, DimensionAccumulator> grouped = new LinkedHashMap<>();

        for (BizfiFiGlEntry entry : openingEntries) {
            String key = normalizeDimensionCode(entry.getFcashflowItem());
            grouped.computeIfAbsent(key, code -> new DimensionAccumulator(code, dimensionNameOf(code, dimensionMap)));
            grouped.get(key).addOpening(entry);
        }
        for (BizfiFiGlEntry entry : periodEntries) {
            String key = normalizeDimensionCode(entry.getFcashflowItem());
            grouped.computeIfAbsent(key, code -> new DimensionAccumulator(code, dimensionNameOf(code, dimensionMap)));
            grouped.get(key).addPeriod(entry, null, null);
        }

        List<LedgerDimensionBalanceRowVO> records = grouped.values().stream()
                .map(acc -> acc.toRow(false))
                .sorted(Comparator.comparing(LedgerDimensionBalanceRowVO::getDimensionCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        appendDimensionWarnings(result);
        return result;
    }

    @Override
    public LedgerQueryResultVO auxDimensionBalance(String startDate, String endDate, String accountCode, String dimensionCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, dimensionCode);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, dimensionCode);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Set<String> dimensionCodes = collectDimensionCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BizfiFiCashflowItem> dimensionMap = loadDimensionMap(dimensionCodes);
        Map<String, DimensionAccumulator> grouped = new LinkedHashMap<>();

        for (BizfiFiGlEntry entry : openingEntries) {
            String dimension = normalizeDimensionCode(entry.getFcashflowItem());
            String key = dimension + "|" + safeText(entry.getFaccountCode(), "-");
            grouped.computeIfAbsent(key, ignore -> new DimensionAccumulator(
                    dimension,
                    dimensionNameOf(dimension, dimensionMap),
                    entry.getFaccountCode(),
                    accountNameOf(entry.getFaccountCode(), accountMap)
            ));
            grouped.get(key).addOpening(entry);
        }
        for (BizfiFiGlEntry entry : periodEntries) {
            String dimension = normalizeDimensionCode(entry.getFcashflowItem());
            String key = dimension + "|" + safeText(entry.getFaccountCode(), "-");
            grouped.computeIfAbsent(key, ignore -> new DimensionAccumulator(
                    dimension,
                    dimensionNameOf(dimension, dimensionMap),
                    entry.getFaccountCode(),
                    accountNameOf(entry.getFaccountCode(), accountMap)
            ));
            grouped.get(key).addPeriod(entry, entry.getFaccountCode(), accountNameOf(entry.getFaccountCode(), accountMap));
        }

        List<LedgerDimensionBalanceRowVO> records = grouped.values().stream()
                .map(acc -> acc.toRow(true))
                .sorted(Comparator.comparing(LedgerDimensionBalanceRowVO::getDimensionCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(LedgerDimensionBalanceRowVO::getAccountCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        appendDimensionWarnings(result);
        return result;
    }

    @Override
    public LedgerQueryResultVO auxGeneralLedger(String startDate, String endDate, String accountCode, String dimensionCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, dimensionCode);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, dimensionCode);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Set<String> dimensionCodes = collectDimensionCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BizfiFiCashflowItem> dimensionMap = loadDimensionMap(dimensionCodes);
        Map<String, BigDecimal> openingNet = computeOpeningNetByAccountDimension(openingEntries);

        List<LedgerBookRowVO> records = buildAuxGeneralLedgerRows(periodEntries, accountMap, dimensionMap, openingNet);
        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        appendDimensionWarnings(result);
        if (!StringUtils.hasText(accountCode)) {
            result.getWarnings().add("未指定科目编码时，辅助总账会按现金流项目和科目组合展示全部流水。");
        }
        return result;
    }

    @Override
    public LedgerQueryResultVO auxDetailLedger(String startDate, String endDate, String accountCode, String dimensionCode) {
        QueryRange range = resolveRange(startDate, endDate);
        List<BizfiFiGlEntry> openingEntries = listEntriesBefore(range.startDate, accountCode, dimensionCode);
        List<BizfiFiGlEntry> periodEntries = listEntriesInRange(range.startDate, range.endDate, accountCode, dimensionCode);

        Set<String> accountCodes = collectAccountCodes(openingEntries, periodEntries);
        Set<String> dimensionCodes = collectDimensionCodes(openingEntries, periodEntries);
        Map<String, BizfiFiAccount> accountMap = loadAccountMap(accountCodes);
        Map<String, BizfiFiCashflowItem> dimensionMap = loadDimensionMap(dimensionCodes);
        Map<String, BigDecimal> openingNet = computeOpeningNetByAccountDimension(openingEntries);

        List<LedgerBookRowVO> records = buildAuxDetailLedgerRows(periodEntries, accountMap, dimensionMap, openingNet);
        LedgerQueryResultVO result = buildResult(records, periodEntries);
        appendBaseWarnings(result, range);
        appendDimensionWarnings(result);
        if (!StringUtils.hasText(accountCode)) {
            result.getWarnings().add("未指定科目编码时，辅助明细账会展示全部科目与现金流项目组合的分录明细。");
        }
        return result;
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

    private List<BizfiFiGlEntry> listEntriesBefore(LocalDate startDate, String accountCode, String dimensionCode) {
        return glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .lt(BizfiFiGlEntry::getFvoucherDate, startDate)
                .likeRight(StringUtils.hasText(accountCode), BizfiFiGlEntry::getFaccountCode, trimToNull(accountCode))
                .eq(StringUtils.hasText(dimensionCode), BizfiFiGlEntry::getFcashflowItem, trimToNull(dimensionCode))
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherId)
                .orderByAsc(BizfiFiGlEntry::getFvoucherLineId)
                .orderByAsc(BizfiFiGlEntry::getFid));
    }

    private List<BizfiFiGlEntry> listEntriesInRange(LocalDate startDate, LocalDate endDate, String accountCode, String dimensionCode) {
        return glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .ge(BizfiFiGlEntry::getFvoucherDate, startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate)
                .likeRight(StringUtils.hasText(accountCode), BizfiFiGlEntry::getFaccountCode, trimToNull(accountCode))
                .eq(StringUtils.hasText(dimensionCode), BizfiFiGlEntry::getFcashflowItem, trimToNull(dimensionCode))
                .orderByAsc(BizfiFiGlEntry::getFaccountCode)
                .orderByAsc(BizfiFiGlEntry::getFvoucherDate)
                .orderByAsc(BizfiFiGlEntry::getFvoucherNumber)
                .orderByAsc(BizfiFiGlEntry::getFvoucherLineId)
                .orderByAsc(BizfiFiGlEntry::getFid));
    }

    private Set<String> collectAccountCodes(List<BizfiFiGlEntry>... sources) {
        Set<String> codes = new LinkedHashSet<>();
        for (List<BizfiFiGlEntry> source : sources) {
            source.stream()
                    .map(BizfiFiGlEntry::getFaccountCode)
                    .filter(StringUtils::hasText)
                    .forEach(codes::add);
        }
        return codes;
    }

    private Set<String> collectDimensionCodes(List<BizfiFiGlEntry>... sources) {
        Set<String> codes = new LinkedHashSet<>();
        for (List<BizfiFiGlEntry> source : sources) {
            source.stream()
                    .map(this::normalizeDimensionCode)
                    .forEach(codes::add);
        }
        return codes;
    }

    private Map<String, BizfiFiAccount> loadAccountMap(Set<String> accountCodes) {
        if (accountCodes.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<BizfiFiAccount>()
                        .in(BizfiFiAccount::getFcode, accountCodes))
                .stream()
                .collect(Collectors.toMap(BizfiFiAccount::getFcode, item -> item, (left, right) -> left));
    }

    private Map<String, BizfiFiCashflowItem> loadDimensionMap(Set<String> dimensionCodes) {
        Set<String> codes = dimensionCodes.stream()
                .filter(code -> !DIMENSION_UNASSIGNED.equals(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (codes.isEmpty()) {
            return Map.of();
        }
        return cashflowItemMapper.selectList(new LambdaQueryWrapper<BizfiFiCashflowItem>()
                        .in(BizfiFiCashflowItem::getFcode, codes))
                .stream()
                .collect(Collectors.toMap(BizfiFiCashflowItem::getFcode, item -> item, (left, right) -> left));
    }

    private void accumulateByAccount(Map<String, BalanceAccumulator> grouped,
                                     List<BizfiFiGlEntry> openingEntries,
                                     List<BizfiFiGlEntry> periodEntries,
                                     Map<String, BizfiFiAccount> accountMap) {
        for (BizfiFiGlEntry entry : openingEntries) {
            String code = safeText(entry.getFaccountCode(), "-");
            grouped.computeIfAbsent(code, key -> new BalanceAccumulator(key, accountNameOf(key, accountMap), normalDirectionOf(key, accountMap)));
            grouped.get(code).addOpening(entry);
        }
        for (BizfiFiGlEntry entry : periodEntries) {
            String code = safeText(entry.getFaccountCode(), "-");
            grouped.computeIfAbsent(code, key -> new BalanceAccumulator(key, accountNameOf(key, accountMap), normalDirectionOf(key, accountMap)));
            grouped.get(code).addPeriod(entry);
        }
    }

    private Map<String, BigDecimal> computeOpeningNetByAccount(List<BizfiFiGlEntry> openingEntries) {
        Map<String, BigDecimal> openingNet = new LinkedHashMap<>();
        for (BizfiFiGlEntry entry : openingEntries) {
            String code = safeText(entry.getFaccountCode(), "-");
            openingNet.put(code, openingNet.getOrDefault(code, BigDecimal.ZERO)
                    .add(safeAmount(entry.getFdebitAmount()))
                    .subtract(safeAmount(entry.getFcreditAmount())));
        }
        return openingNet;
    }

    private Map<String, BigDecimal> computeOpeningNetByAccountDimension(List<BizfiFiGlEntry> openingEntries) {
        Map<String, BigDecimal> openingNet = new LinkedHashMap<>();
        for (BizfiFiGlEntry entry : openingEntries) {
            String key = bookBalanceKey(entry.getFaccountCode(), entry.getFcashflowItem());
            openingNet.put(key, openingNet.getOrDefault(key, BigDecimal.ZERO)
                    .add(safeAmount(entry.getFdebitAmount()))
                    .subtract(safeAmount(entry.getFcreditAmount())));
        }
        return openingNet;
    }

    private List<LedgerBookRowVO> buildGeneralLedgerRows(List<BizfiFiGlEntry> periodEntries,
                                                         Map<String, BizfiFiAccount> accountMap,
                                                         Map<String, BigDecimal> openingNet) {
        Map<String, GeneralLedgerAccumulator> grouped = new LinkedHashMap<>();
        for (BizfiFiGlEntry entry : periodEntries) {
            String key = safeText(entry.getFaccountCode(), "-") + "|"
                    + safeText(entry.getFvoucherDate() == null ? null : entry.getFvoucherDate().toString(), "-") + "|"
                    + safeText(entry.getFvoucherNumber(), "-") + "|"
                    + safeText(entry.getFsummary(), "-");
            grouped.computeIfAbsent(key, ignore -> new GeneralLedgerAccumulator(entry, accountNameOf(entry.getFaccountCode(), accountMap)));
            grouped.get(key).add(entry);
        }

        Map<String, BigDecimal> runningNet = new LinkedHashMap<>(openingNet);
        List<LedgerBookRowVO> rows = new ArrayList<>();
        for (GeneralLedgerAccumulator acc : grouped.values().stream()
                .sorted(Comparator.comparing(GeneralLedgerAccumulator::sortKey))
                .toList()) {
            String accountCode = safeText(acc.accountCode, "-");
            BigDecimal currentNet = runningNet.getOrDefault(accountCode, BigDecimal.ZERO)
                    .add(acc.debitAmount)
                    .subtract(acc.creditAmount);
            runningNet.put(accountCode, currentNet);
            LedgerAmount balanceAmount = toAmount(currentNet);
            rows.add(new LedgerBookRowVO(
                    null,
                    null,
                    accountCode,
                    acc.accountName,
                    acc.voucherDate,
                    acc.voucherNumber,
                    acc.summary,
                    normalize(acc.debitAmount),
                    normalize(acc.creditAmount),
                    balanceAmount.amount,
                    balanceAmount.direction
            ));
        }
        return rows;
    }

    private List<LedgerBookRowVO> buildDetailLedgerRows(List<BizfiFiGlEntry> periodEntries,
                                                        Map<String, BizfiFiAccount> accountMap,
                                                        Map<String, BigDecimal> openingNet) {
        Map<String, BigDecimal> runningNet = new LinkedHashMap<>(openingNet);
        List<LedgerBookRowVO> rows = new ArrayList<>();
        for (BizfiFiGlEntry entry : periodEntries.stream().sorted(entryComparator()).toList()) {
            String accountCode = safeText(entry.getFaccountCode(), "-");
            BigDecimal currentNet = runningNet.getOrDefault(accountCode, BigDecimal.ZERO)
                    .add(safeAmount(entry.getFdebitAmount()))
                    .subtract(safeAmount(entry.getFcreditAmount()));
            runningNet.put(accountCode, currentNet);
            LedgerAmount balanceAmount = toAmount(currentNet);
            rows.add(new LedgerBookRowVO(
                    null,
                    null,
                    accountCode,
                    accountNameOf(accountCode, accountMap),
                    entry.getFvoucherDate(),
                    entry.getFvoucherNumber(),
                    safeText(entry.getFsummary(), "-"),
                    normalize(entry.getFdebitAmount()),
                    normalize(entry.getFcreditAmount()),
                    balanceAmount.amount,
                    balanceAmount.direction
            ));
        }
        return rows;
    }

    private List<LedgerBookRowVO> buildAuxGeneralLedgerRows(List<BizfiFiGlEntry> periodEntries,
                                                            Map<String, BizfiFiAccount> accountMap,
                                                            Map<String, BizfiFiCashflowItem> dimensionMap,
                                                            Map<String, BigDecimal> openingNet) {
        Map<String, GeneralLedgerAccumulator> grouped = new LinkedHashMap<>();
        for (BizfiFiGlEntry entry : periodEntries) {
            String dimension = normalizeDimensionCode(entry.getFcashflowItem());
            String key = dimension + "|"
                    + safeText(entry.getFaccountCode(), "-") + "|"
                    + safeText(entry.getFvoucherDate() == null ? null : entry.getFvoucherDate().toString(), "-") + "|"
                    + safeText(entry.getFvoucherNumber(), "-") + "|"
                    + safeText(entry.getFsummary(), "-");
            grouped.computeIfAbsent(key, ignore -> new GeneralLedgerAccumulator(
                    entry,
                    accountNameOf(entry.getFaccountCode(), accountMap),
                    dimension,
                    dimensionNameOf(dimension, dimensionMap)
            ));
            grouped.get(key).add(entry);
        }

        Map<String, BigDecimal> runningNet = new LinkedHashMap<>(openingNet);
        List<LedgerBookRowVO> rows = new ArrayList<>();
        for (GeneralLedgerAccumulator acc : grouped.values().stream()
                .sorted(Comparator.comparing(GeneralLedgerAccumulator::sortKey))
                .toList()) {
            String balanceKey = bookBalanceKey(acc.accountCode, acc.dimensionCode);
            BigDecimal currentNet = runningNet.getOrDefault(balanceKey, BigDecimal.ZERO)
                    .add(acc.debitAmount)
                    .subtract(acc.creditAmount);
            runningNet.put(balanceKey, currentNet);
            LedgerAmount balanceAmount = toAmount(currentNet);
            rows.add(new LedgerBookRowVO(
                    acc.dimensionCode,
                    acc.dimensionName,
                    acc.accountCode,
                    acc.accountName,
                    acc.voucherDate,
                    acc.voucherNumber,
                    acc.summary,
                    normalize(acc.debitAmount),
                    normalize(acc.creditAmount),
                    balanceAmount.amount,
                    balanceAmount.direction
            ));
        }
        return rows;
    }

    private List<LedgerBookRowVO> buildAuxDetailLedgerRows(List<BizfiFiGlEntry> periodEntries,
                                                           Map<String, BizfiFiAccount> accountMap,
                                                           Map<String, BizfiFiCashflowItem> dimensionMap,
                                                           Map<String, BigDecimal> openingNet) {
        Map<String, BigDecimal> runningNet = new LinkedHashMap<>(openingNet);
        List<LedgerBookRowVO> rows = new ArrayList<>();
        for (BizfiFiGlEntry entry : periodEntries.stream().sorted(entryComparator()).toList()) {
            String accountCode = safeText(entry.getFaccountCode(), "-");
            String dimensionCode = normalizeDimensionCode(entry.getFcashflowItem());
            String balanceKey = bookBalanceKey(accountCode, dimensionCode);
            BigDecimal currentNet = runningNet.getOrDefault(balanceKey, BigDecimal.ZERO)
                    .add(safeAmount(entry.getFdebitAmount()))
                    .subtract(safeAmount(entry.getFcreditAmount()));
            runningNet.put(balanceKey, currentNet);
            LedgerAmount balanceAmount = toAmount(currentNet);
            rows.add(new LedgerBookRowVO(
                    dimensionCode,
                    dimensionNameOf(dimensionCode, dimensionMap),
                    accountCode,
                    accountNameOf(accountCode, accountMap),
                    entry.getFvoucherDate(),
                    entry.getFvoucherNumber(),
                    safeText(entry.getFsummary(), "-"),
                    normalize(entry.getFdebitAmount()),
                    normalize(entry.getFcreditAmount()),
                    balanceAmount.amount,
                    balanceAmount.direction
            ));
        }
        return rows;
    }

    private Comparator<BizfiFiGlEntry> entryComparator() {
        return Comparator.comparing(BizfiFiGlEntry::getFaccountCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(BizfiFiGlEntry::getFcashflowItem, Comparator.nullsLast(String::compareTo))
                .thenComparing(BizfiFiGlEntry::getFvoucherDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(BizfiFiGlEntry::getFvoucherNumber, Comparator.nullsLast(String::compareTo))
                .thenComparing(BizfiFiGlEntry::getFvoucherLineId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(BizfiFiGlEntry::getFid, Comparator.nullsLast(Long::compareTo));
    }

    private LedgerQueryResultVO buildResult(List<?> records, List<BizfiFiGlEntry> periodEntries) {
        LedgerQueryResultVO result = new LedgerQueryResultVO();
        result.setRecords(records);
        result.getSummary().put("recordCount", records.size());
        result.getSummary().put("totalDebit", normalize(sumDebit(periodEntries)));
        result.getSummary().put("totalCredit", normalize(sumCredit(periodEntries)));
        return result;
    }

    private void appendBaseWarnings(LedgerQueryResultVO result, QueryRange range) {
        result.getWarnings().add("当前账表查询直接基于已过账总账分录计算，尚未切换到期间余额表。");
        result.getWarnings().add("当前总账分录尚未记录业务单元维度，结果暂未按业务单元隔离。");
        result.getSummary().put("startDate", range.startDate.toString());
        result.getSummary().put("endDate", range.endDate.toString());
    }

    private void appendDimensionWarnings(LedgerQueryResultVO result) {
        result.getWarnings().add("当前系统仅支持按现金流项目作为维度进行查询，部门、客户、供应商等辅助维度待后续扩展。");
    }

    private String normalizeDimensionCode(BizfiFiGlEntry entry) {
        return normalizeDimensionCode(entry == null ? null : entry.getFcashflowItem());
    }

    private String normalizeDimensionCode(String dimensionCode) {
        return StringUtils.hasText(dimensionCode) ? dimensionCode.trim() : DIMENSION_UNASSIGNED;
    }

    private String dimensionNameOf(String dimensionCode, Map<String, BizfiFiCashflowItem> dimensionMap) {
        if (DIMENSION_UNASSIGNED.equals(dimensionCode)) {
            return "未指定现金流项目";
        }
        BizfiFiCashflowItem item = dimensionMap.get(dimensionCode);
        return item == null ? dimensionCode : safeText(item.getFname(), dimensionCode);
    }

    private String accountNameOf(String accountCode, Map<String, BizfiFiAccount> accountMap) {
        BizfiFiAccount account = accountMap.get(accountCode);
        return account == null ? accountCode : safeText(account.getFname(), accountCode);
    }

    private String normalDirectionOf(String accountCode, Map<String, BizfiFiAccount> accountMap) {
        BizfiFiAccount account = accountMap.get(accountCode);
        if (account == null || !StringUtils.hasText(account.getFdirection())) {
            return "-";
        }
        return directionLabel(account.getFdirection());
    }

    private String directionLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("DEBIT") || "借".equals(value)) {
            return "借";
        }
        if (normalized.contains("CREDIT") || "贷".equals(value)) {
            return "贷";
        }
        return value;
    }

    private String bookBalanceKey(String accountCode, String dimensionCode) {
        return safeText(accountCode, "-") + "|" + normalizeDimensionCode(dimensionCode);
    }

    private BigDecimal sumDebit(List<BizfiFiGlEntry> entries) {
        return entries.stream()
                .map(BizfiFiGlEntry::getFdebitAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCredit(List<BizfiFiGlEntry> entries) {
        return entries.stream()
                .map(BizfiFiGlEntry::getFcreditAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumNet(List<BizfiFiGlEntry> entries) {
        return sumDebit(entries).subtract(sumCredit(entries));
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return safeAmount(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private LedgerAmount toAmount(BigDecimal netAmount) {
        BigDecimal normalized = safeAmount(netAmount);
        if (normalized.compareTo(BigDecimal.ZERO) > 0) {
            return new LedgerAmount(normalize(normalized), "借");
        }
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return new LedgerAmount(normalize(normalized.abs()), "贷");
        }
        return new LedgerAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), "平");
    }

    private static class QueryRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private QueryRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static class LedgerAmount {
        private final BigDecimal amount;
        private final String direction;

        private LedgerAmount(BigDecimal amount, String direction) {
            this.amount = amount;
            this.direction = direction;
        }
    }

    private class BalanceAccumulator {
        private final String accountCode;
        private final String accountName;
        private final String normalDirection;
        private BigDecimal openingNet = BigDecimal.ZERO;
        private BigDecimal periodDebit = BigDecimal.ZERO;
        private BigDecimal periodCredit = BigDecimal.ZERO;
        private int entryCount = 0;

        private BalanceAccumulator(String accountCode, String accountName, String normalDirection) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.normalDirection = normalDirection;
        }

        private void addOpening(BizfiFiGlEntry entry) {
            openingNet = openingNet.add(safeAmount(entry.getFdebitAmount())).subtract(safeAmount(entry.getFcreditAmount()));
        }

        private void addPeriod(BizfiFiGlEntry entry) {
            periodDebit = periodDebit.add(safeAmount(entry.getFdebitAmount()));
            periodCredit = periodCredit.add(safeAmount(entry.getFcreditAmount()));
            entryCount += 1;
        }

        private LedgerBalanceRowVO toBalanceRow() {
            LedgerAmount opening = toAmount(openingNet);
            LedgerAmount closing = toAmount(openingNet.add(periodDebit).subtract(periodCredit));
            return new LedgerBalanceRowVO(
                    accountCode,
                    accountName,
                    normalDirection,
                    opening.amount,
                    opening.direction,
                    normalize(periodDebit),
                    normalize(periodCredit),
                    closing.amount,
                    closing.direction,
                    entryCount
            );
        }
    }

    private class DimensionAccumulator {
        private final String dimensionCode;
        private final String dimensionName;
        private String accountCode;
        private String accountName;
        private BigDecimal openingNet = BigDecimal.ZERO;
        private BigDecimal periodDebit = BigDecimal.ZERO;
        private BigDecimal periodCredit = BigDecimal.ZERO;
        private int entryCount = 0;

        private DimensionAccumulator(String dimensionCode, String dimensionName) {
            this.dimensionCode = dimensionCode;
            this.dimensionName = dimensionName;
        }

        private DimensionAccumulator(String dimensionCode, String dimensionName, String accountCode, String accountName) {
            this.dimensionCode = dimensionCode;
            this.dimensionName = dimensionName;
            this.accountCode = accountCode;
            this.accountName = accountName;
        }

        private void addOpening(BizfiFiGlEntry entry) {
            openingNet = openingNet.add(safeAmount(entry.getFdebitAmount())).subtract(safeAmount(entry.getFcreditAmount()));
        }

        private void addPeriod(BizfiFiGlEntry entry, String accountCode, String accountName) {
            if (StringUtils.hasText(accountCode)) {
                this.accountCode = accountCode;
            }
            if (StringUtils.hasText(accountName)) {
                this.accountName = accountName;
            }
            periodDebit = periodDebit.add(safeAmount(entry.getFdebitAmount()));
            periodCredit = periodCredit.add(safeAmount(entry.getFcreditAmount()));
            entryCount += 1;
        }

        private LedgerDimensionBalanceRowVO toRow(boolean includeAccount) {
            LedgerAmount opening = toAmount(openingNet);
            LedgerAmount closing = toAmount(openingNet.add(periodDebit).subtract(periodCredit));
            return new LedgerDimensionBalanceRowVO(
                    dimensionCode,
                    dimensionName,
                    includeAccount ? safeText(accountCode, "-") : null,
                    includeAccount ? safeText(accountName, safeText(accountCode, "-")) : null,
                    opening.amount,
                    opening.direction,
                    normalize(periodDebit),
                    normalize(periodCredit),
                    closing.amount,
                    closing.direction,
                    entryCount
            );
        }
    }

    private class GeneralLedgerAccumulator {
        private final String dimensionCode;
        private final String dimensionName;
        private final String accountCode;
        private final String accountName;
        private final LocalDate voucherDate;
        private final String voucherNumber;
        private final String summary;
        private BigDecimal debitAmount = BigDecimal.ZERO;
        private BigDecimal creditAmount = BigDecimal.ZERO;

        private GeneralLedgerAccumulator(BizfiFiGlEntry entry, String accountName) {
            this(entry, accountName, null, null);
        }

        private GeneralLedgerAccumulator(BizfiFiGlEntry entry, String accountName, String dimensionCode, String dimensionName) {
            this.dimensionCode = dimensionCode;
            this.dimensionName = dimensionName;
            this.accountCode = safeText(entry.getFaccountCode(), "-");
            this.accountName = accountName;
            this.voucherDate = entry.getFvoucherDate();
            this.voucherNumber = safeText(entry.getFvoucherNumber(), "-");
            this.summary = safeText(entry.getFsummary(), "-");
        }

        private void add(BizfiFiGlEntry entry) {
            debitAmount = debitAmount.add(safeAmount(entry.getFdebitAmount()));
            creditAmount = creditAmount.add(safeAmount(entry.getFcreditAmount()));
        }

        private String sortKey() {
            return safeText(accountCode, "-") + "|"
                    + safeText(dimensionCode, "-") + "|"
                    + safeText(voucherDate == null ? null : voucherDate.toString(), "-") + "|"
                    + safeText(voucherNumber, "-") + "|"
                    + safeText(summary, "-");
        }
    }
}
