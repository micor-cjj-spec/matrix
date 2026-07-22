package single.cjj.fi.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.scheduler.client.annotation.MatrixJobHandler;
import single.cjj.scheduler.client.core.JobContext;
import single.cjj.scheduler.client.core.JobResult;
import single.cjj.scheduler.client.core.MatrixJob;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@MatrixJobHandler(value = "financial-report-generate", name = "财务报表生成")
public class FinancialReportGenerateJob implements MatrixJob {

    private final BizfiFiVoucherMapper voucherMapper;
    private final BizfiFiGlEntryMapper glEntryMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FinancialReportGenerateJob(BizfiFiVoucherMapper voucherMapper,
                                      BizfiFiGlEntryMapper glEntryMapper,
                                      JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper) {
        this.voucherMapper = voucherMapper;
        this.glEntryMapper = glEntryMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobResult execute(JobContext context) throws Exception {
        YearMonth period = YearMonth.parse(context.getRequiredString("period"));
        String reportType = defaultText(context.getString("reportType"), "TRIAL_BALANCE");
        String bookId = context.getString("bookId");
        context.reportProgress(10, "VALIDATING", "正在校验报表参数");

        List<Long> voucherIds = loadPostedVoucherIds(period, bookId);
        context.reportProgress(30, "LOADING_GL", "正在加载已过账总账分录");
        List<BizfiFiGlEntry> entries = loadEntries(period, voucherIds, StringUtils.hasText(bookId));

        context.reportProgress(60, "AGGREGATING", "正在按科目汇总借贷发生额");
        Map<String, AmountAggregate> aggregateMap = new LinkedHashMap<>();
        for (BizfiFiGlEntry entry : entries) {
            String accountCode = defaultText(entry.getFaccountCode(), "UNMAPPED");
            AmountAggregate aggregate = aggregateMap.computeIfAbsent(accountCode, key -> new AmountAggregate());
            aggregate.debit = aggregate.debit.add(zero(entry.getFdebitAmount()));
            aggregate.credit = aggregate.credit.add(zero(entry.getFcreditAmount()));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        aggregateMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("accountCode", item.getKey());
                    row.put("debitAmount", scale(item.getValue().debit));
                    row.put("creditAmount", scale(item.getValue().credit));
                    row.put("netAmount", scale(item.getValue().debit.subtract(item.getValue().credit)));
                    rows.add(row);
                });

        BigDecimal totalDebit = rows.stream()
                .map(row -> (BigDecimal) row.get("debitAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = rows.stream()
                .map(row -> (BigDecimal) row.get("creditAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportType", reportType);
        result.put("period", period.toString());
        result.put("bookId", bookId);
        result.put("voucherCount", voucherIds.size());
        result.put("entryCount", entries.size());
        result.put("accountCount", rows.size());
        result.put("totalDebit", scale(totalDebit));
        result.put("totalCredit", scale(totalCredit));
        result.put("balanced", totalDebit.compareTo(totalCredit) == 0);
        result.put("rows", rows);

        context.reportProgress(85, "PERSISTING", "正在保存报表快照");
        persistSnapshot(context.getExecutionNo(), reportType, period.toString(), bookId, result);
        context.reportProgress(95, "FINALIZING", "正在生成执行结果");
        return JobResult.success(result);
    }

    private List<Long> loadPostedVoucherIds(YearMonth period, String bookId) {
        LambdaQueryWrapper<BizfiFiVoucher> wrapper = new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(BizfiFiVoucher::getFdate, period.atDay(1))
                .le(BizfiFiVoucher::getFdate, period.atEndOfMonth())
                .eq(BizfiFiVoucher::getFstatus, "POSTED")
                .eq(StringUtils.hasText(bookId), BizfiFiVoucher::getBookId, bookId);
        return voucherMapper.selectList(wrapper).stream()
                .map(BizfiFiVoucher::getFid)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<BizfiFiGlEntry> loadEntries(YearMonth period,
                                             List<Long> voucherIds,
                                             boolean bookScoped) {
        if (bookScoped && voucherIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<BizfiFiGlEntry> wrapper = new LambdaQueryWrapper<BizfiFiGlEntry>()
                .ge(BizfiFiGlEntry::getFvoucherDate, period.atDay(1))
                .le(BizfiFiGlEntry::getFvoucherDate, period.atEndOfMonth())
                .in(bookScoped, BizfiFiGlEntry::getFvoucherId, voucherIds)
                .orderByAsc(BizfiFiGlEntry::getFaccountCode);
        return glEntryMapper.selectList(wrapper);
    }

    private void persistSnapshot(String executionNo,
                                 String reportType,
                                 String period,
                                 String bookId,
                                 Map<String, Object> result) throws Exception {
        String json = objectMapper.writeValueAsString(result);
        jdbcTemplate.update("""
                        INSERT INTO matrix_fi_scheduler_report_snapshot
                        (fid, fexecution_no, freport_type, fperiod, fbook_id, fstatus, fsummary_json, fcreated_time)
                        VALUES (?, ?, ?, ?, ?, 'SUCCESS', ?, ?)
                        ON DUPLICATE KEY UPDATE
                        freport_type = VALUES(freport_type),
                        fperiod = VALUES(fperiod),
                        fbook_id = VALUES(fbook_id),
                        fstatus = VALUES(fstatus),
                        fsummary_json = VALUES(fsummary_json)
                        """,
                IdWorker.getId(), executionNo, reportType, period, bookId, json, LocalDateTime.now());
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static class AmountAggregate {
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
    }
}
