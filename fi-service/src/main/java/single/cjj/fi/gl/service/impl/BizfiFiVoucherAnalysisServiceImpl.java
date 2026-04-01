package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.entity.BizfiFiVoucherType;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherTypeMapper;
import single.cjj.fi.gl.service.BizfiFiDataHealthCheckService;
import single.cjj.fi.gl.service.BizfiFiOrgFinanceConfigService;
import single.cjj.fi.gl.service.BizfiFiVoucherAnalysisService;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;
import single.cjj.fi.gl.vo.VoucherCarryListResultVO;
import single.cjj.fi.gl.vo.VoucherCarryTaskVO;
import single.cjj.fi.gl.vo.VoucherSummaryResultVO;
import single.cjj.fi.gl.vo.VoucherSummaryRowVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BizfiFiVoucherAnalysisServiceImpl implements BizfiFiVoucherAnalysisService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUDITED = "AUDITED";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_REVERSED = "REVERSED";

    private static final List<String> CARRY_KEYWORDS = List.of(
            "结转", "损益", "期末", "汇兑", "摊销", "转账", "结账"
    );

    @Autowired
    private BizfiFiVoucherMapper voucherMapper;

    @Autowired
    private BizfiFiAccountingPeriodMapper accountingPeriodMapper;

    @Autowired
    private BizfiFiVoucherTypeMapper voucherTypeMapper;

    @Autowired
    private BizfiFiOrgFinanceConfigService orgFinanceConfigService;

    @Autowired
    private BizfiFiDataHealthCheckService dataHealthCheckService;

    @Override
    public VoucherSummaryResultVO summary(String startDate, String endDate, String status, String summaryKeyword) {
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException("startDate 不能晚于 endDate。");
        }

        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = trimToNull(summaryKeyword);

        List<BizfiFiVoucher> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(start != null, BizfiFiVoucher::getFdate, start)
                .le(end != null, BizfiFiVoucher::getFdate, end)
                .eq(StringUtils.hasText(normalizedStatus), BizfiFiVoucher::getFstatus, normalizedStatus)
                .like(StringUtils.hasText(normalizedKeyword), BizfiFiVoucher::getFsummary, normalizedKeyword)
                .orderByDesc(BizfiFiVoucher::getFdate)
                .orderByDesc(BizfiFiVoucher::getFid));

        VoucherSummaryResultVO result = new VoucherSummaryResultVO();
        result.setStartDate(start != null ? start.toString() : null);
        result.setEndDate(end != null ? end.toString() : null);
        result.setStatus(normalizedStatus);
        result.setSummaryKeyword(normalizedKeyword);
        result.setTotalCount(vouchers.size());
        result.setTotalAmount(sumAmount(vouchers));
        result.setDraftCount(countStatus(vouchers, STATUS_DRAFT));
        result.setSubmittedCount(countStatus(vouchers, STATUS_SUBMITTED));
        result.setAuditedCount(countStatus(vouchers, STATUS_AUDITED));
        result.setPostedCount(countStatus(vouchers, STATUS_POSTED));
        result.setRejectedCount(countStatus(vouchers, STATUS_REJECTED));
        result.setReversedCount(countStatus(vouchers, STATUS_REVERSED));
        result.setPostedAmount(sumAmountByStatus(vouchers, STATUS_POSTED));
        result.setWarnings(new ArrayList<>());

        if (vouchers.isEmpty()) {
            result.getWarnings().add("当前筛选条件下没有匹配的凭证。");
            result.setRows(new ArrayList<>());
            return result;
        }

        Map<LocalDate, List<BizfiFiVoucher>> grouped = new LinkedHashMap<>();
        for (BizfiFiVoucher voucher : vouchers) {
            LocalDate bizDate = voucher.getFdate();
            if (bizDate == null) {
                continue;
            }
            grouped.computeIfAbsent(bizDate, key -> new ArrayList<>()).add(voucher);
        }

        List<VoucherSummaryRowVO> rows = grouped.entrySet().stream()
                .map(entry -> toSummaryRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(VoucherSummaryRowVO::getBizDate, Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .toList();
        result.setRows(rows);

        if (!StringUtils.hasText(normalizedStatus)) {
            result.getWarnings().add("当前表格中的状态分布，是按筛选范围内所有状态一起汇总出来的。");
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            result.getWarnings().add("摘要关键字只对凭证头摘要做模糊匹配，不会额外匹配备注和分录摘要。");
        }
        return result;
    }

    @Override
    public VoucherCarryListResultVO carryList(Long forg, String period) {
        VoucherCarryListResultVO result = new VoucherCarryListResultVO();
        result.setForg(forg);
        result.setTasks(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        result.setRelatedVouchers(new ArrayList<>());

        BizfiFiOrgFinanceConfig config = forg == null ? null : orgFinanceConfigService.getByOrg(forg);
        if (config != null) {
            result.setBaseCurrency(config.getFbaseCurrency());
            result.setCurrentPeriod(config.getFcurrentPeriod());
            result.setDefaultVoucherType(config.getFdefaultVoucherType());
        }

        if (StringUtils.hasText(period) && parsePeriod(period) == null) {
            result.getWarnings().add("期间格式无效，已忽略页面输入并回退到组织参数或系统当前月份，正确格式应为 yyyy-MM。");
        }

        String resolvedPeriod = resolvePeriod(period, config);
        result.setPeriod(resolvedPeriod);
        result.setPeriodSource(resolvePeriodSource(period, config));

        if (forg == null) {
            result.getWarnings().add("结转清单是按业务单元管理的，建议先选择业务单元后再检查。");
        } else {
            result.getWarnings().add("当前凭证头还没有组织维度，结转清单中的凭证数量仍按所选期间做全局统计。");
        }

        addConfigTask(result, config);
        BizfiFiAccountingPeriod accountingPeriod = loadAccountingPeriod(forg, resolvedPeriod);
        addPeriodTask(result, accountingPeriod, resolvedPeriod);
        addVoucherTypeTask(result, forg, config);

        BizfiFiHealthCheckResultVO healthCheck = forg == null ? null : dataHealthCheckService.check(forg, null, 5);
        result.setFoundationHealthy(healthCheck == null ? null : Boolean.TRUE.equals(healthCheck.getHealthy()));
        addHealthCheckTask(result, healthCheck);
        appendHealthWarnings(result, healthCheck);

        List<BizfiFiVoucher> periodVouchers = loadPeriodVouchers(resolvedPeriod);
        result.setPeriodVoucherCount(periodVouchers.size());
        result.setPeriodVoucherAmount(sumAmount(periodVouchers));

        List<BizfiFiVoucher> relatedVouchers = periodVouchers.stream()
                .filter(this::isCarryVoucher)
                .sorted(Comparator.comparing(BizfiFiVoucher::getFdate, Comparator.nullsLast(LocalDate::compareTo)).reversed()
                        .thenComparing(BizfiFiVoucher::getFid, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        result.setRelatedVouchers(relatedVouchers);
        result.setCarryVoucherCount(relatedVouchers.size());
        addCarryVoucherTask(result, relatedVouchers);
        return result;
    }

    private VoucherSummaryRowVO toSummaryRow(LocalDate bizDate, List<BizfiFiVoucher> vouchers) {
        return new VoucherSummaryRowVO(
                bizDate,
                vouchers.size(),
                sumAmount(vouchers),
                countStatus(vouchers, STATUS_DRAFT),
                countStatus(vouchers, STATUS_SUBMITTED),
                countStatus(vouchers, STATUS_AUDITED),
                countStatus(vouchers, STATUS_POSTED),
                countStatus(vouchers, STATUS_REJECTED),
                countStatus(vouchers, STATUS_REVERSED),
                sumAmountByStatus(vouchers, STATUS_POSTED)
        );
    }

    private void addConfigTask(VoucherCarryListResultVO result, BizfiFiOrgFinanceConfig config) {
        if (config == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ORG_CONFIG",
                    "组织财务参数",
                    "PENDING",
                    "当前业务单元还没有组织财务参数，无法确认当前期间、本位币和默认凭证字。",
                    "先维护组织财务参数，再继续期末结转检查。"
            ));
            return;
        }

        String status = "ENABLED".equalsIgnoreCase(config.getFstatus()) ? "READY" : "WARNING";
        String message = "组织财务参数已存在，本位币 "
                + safeText(config.getFbaseCurrency(), "CNY")
                + "，当前期间 "
                + safeText(config.getFcurrentPeriod(), "未设置")
                + "。";
        if (!"READY".equals(status)) {
            message = "组织财务参数已存在，但当前状态不是 ENABLED。";
        }
        result.getTasks().add(new VoucherCarryTaskVO(
                "ORG_CONFIG",
                "组织财务参数",
                status,
                message,
                "确认当前期间、期间控制模式和默认凭证字是否已经维护正确。"
        ));
    }

    private void addPeriodTask(VoucherCarryListResultVO result, BizfiFiAccountingPeriod accountingPeriod, String resolvedPeriod) {
        if (!StringUtils.hasText(resolvedPeriod)) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "PENDING",
                    "未能解析当前期间，无法判断结转窗口。",
                    "优先从组织财务参数维护当前期间，或手工输入期间。"
            ));
            return;
        }

        if (accountingPeriod == null) {
            result.setPeriodStatus("MISSING");
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "WARNING",
                    "当前期间 " + resolvedPeriod + " 没有对应的会计期间档案。",
                    "先补录会计期间，再进行期末处理。"
            ));
            return;
        }

        result.setPeriodStatus(accountingPeriod.getFstatus());
        String taskStatus = "OPEN".equalsIgnoreCase(accountingPeriod.getFstatus()) ? "READY" : "DONE";
        String message = "期间 " + accountingPeriod.getFperiod() + " 的状态为 " + accountingPeriod.getFstatus() + "。";
        if ("DONE".equals(taskStatus)) {
            message = "期间 " + accountingPeriod.getFperiod() + " 已关账，如需补做请先反开期间。";
        }
        result.getTasks().add(new VoucherCarryTaskVO(
                "ACCOUNTING_PERIOD",
                "会计期间",
                taskStatus,
                message,
                "OPEN 状态可继续结转，CLOSED 状态需要先确认是否允许反开。"
        ));
    }

    private void addVoucherTypeTask(VoucherCarryListResultVO result, Long forg, BizfiFiOrgFinanceConfig config) {
        String defaultVoucherType = config == null ? null : trimToNull(config.getFdefaultVoucherType());
        if (forg == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "DEFAULT_VOUCHER_TYPE",
                    "默认凭证字",
                    "PENDING",
                    "未选择业务单元，无法检查默认凭证字。",
                    "选择业务单元后重新检查。"
            ));
            return;
        }
        if (!StringUtils.hasText(defaultVoucherType)) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "DEFAULT_VOUCHER_TYPE",
                    "默认凭证字",
                    "WARNING",
                    "组织财务参数里还没有设置默认凭证字。",
                    "为该业务单元维护默认凭证字。"
            ));
            return;
        }

        BizfiFiVoucherType voucherType = voucherTypeMapper.selectOne(new LambdaQueryWrapper<BizfiFiVoucherType>()
                .eq(BizfiFiVoucherType::getForg, forg)
                .eq(BizfiFiVoucherType::getFcode, defaultVoucherType)
                .last("limit 1"));
        if (voucherType == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "DEFAULT_VOUCHER_TYPE",
                    "默认凭证字",
                    "WARNING",
                    "默认凭证字 " + defaultVoucherType + " 在当前业务单元下不存在。",
                    "补录凭证类型，或修正组织财务参数中的默认凭证字。"
            ));
            return;
        }

        String taskStatus = "ENABLED".equalsIgnoreCase(voucherType.getFstatus()) ? "READY" : "WARNING";
        String message = "默认凭证字 " + voucherType.getFcode() + " - " + safeText(voucherType.getFname(), "-") + " 可用于结转凭证。";
        if (!"READY".equals(taskStatus)) {
            message = "默认凭证字 " + voucherType.getFcode() + " 已存在，但状态不是 ENABLED。";
        }
        result.getTasks().add(new VoucherCarryTaskVO(
                "DEFAULT_VOUCHER_TYPE",
                "默认凭证字",
                taskStatus,
                message,
                "建议结转类凭证统一使用组织默认凭证字，便于编号治理和查询。"
        ));
    }

    private void addHealthCheckTask(VoucherCarryListResultVO result, BizfiFiHealthCheckResultVO healthCheck) {
        if (healthCheck == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "FOUNDATION_HEALTH",
                    "基础资料检查",
                    "PENDING",
                    "未执行基础资料健康检查。",
                    "选择业务单元后执行财务基础资料健康检查。"
            ));
            return;
        }

        boolean healthy = Boolean.TRUE.equals(healthCheck.getHealthy());
        String status = healthy ? "READY" : "WARNING";
        String message = healthy
                ? "基础资料健康检查通过，没有发现关键缺口。"
                : "基础资料检查发现 " + safeNumber(healthCheck.getIssueTypeCount()) + " 类问题，共 "
                + safeNumber(healthCheck.getTotalIssueCount()) + " 条待处理。";
        result.getTasks().add(new VoucherCarryTaskVO(
                "FOUNDATION_HEALTH",
                "基础资料检查",
                status,
                message,
                healthy ? "可以继续执行结转和关账动作。" : "先修复科目、报表映射、期间等基础资料缺口。"
        ));
    }

    private void addCarryVoucherTask(VoucherCarryListResultVO result, List<BizfiFiVoucher> relatedVouchers) {
        if (relatedVouchers.isEmpty()) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "CARRY_VOUCHERS",
                    "结转相关凭证",
                    "PENDING",
                    "当前期间还没有识别到结转或期末类凭证。",
                    "如果已完成期末处理，请确认摘要或备注是否包含结转关键字；如果尚未处理，可以继续执行期末流程。"
            ));
            return;
        }

        long postedCount = relatedVouchers.stream()
                .filter(voucher -> STATUS_POSTED.equalsIgnoreCase(voucher.getFstatus()))
                .count();
        String status = postedCount == relatedVouchers.size() ? "DONE" : "WARNING";
        String message = "当前期间识别到 " + relatedVouchers.size() + " 张结转相关凭证，其中 "
                + postedCount + " 张已过账。";
        result.getTasks().add(new VoucherCarryTaskVO(
                "CARRY_VOUCHERS",
                "结转相关凭证",
                status,
                message,
                "建议逐张核对摘要、日期和状态，再决定是否进入关账。"
        ));
    }

    private void appendHealthWarnings(VoucherCarryListResultVO result, BizfiFiHealthCheckResultVO healthCheck) {
        if (healthCheck == null || healthCheck.getNotes() == null) {
            return;
        }
        healthCheck.getNotes().stream()
                .filter(StringUtils::hasText)
                .limit(3)
                .map(note -> "基础资料检查: " + note.trim())
                .forEach(result.getWarnings()::add);
    }

    private BizfiFiAccountingPeriod loadAccountingPeriod(Long forg, String period) {
        if (forg == null || !StringUtils.hasText(period)) {
            return null;
        }
        return accountingPeriodMapper.selectOne(new LambdaQueryWrapper<BizfiFiAccountingPeriod>()
                .eq(BizfiFiAccountingPeriod::getForg, forg)
                .eq(BizfiFiAccountingPeriod::getFperiod, period)
                .last("limit 1"));
    }

    private List<BizfiFiVoucher> loadPeriodVouchers(String period) {
        YearMonth yearMonth = parsePeriod(period);
        if (yearMonth == null) {
            return List.of();
        }
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        return voucherMapper.selectList(new LambdaQueryWrapper<BizfiFiVoucher>()
                .ge(BizfiFiVoucher::getFdate, startDate)
                .le(BizfiFiVoucher::getFdate, endDate)
                .orderByDesc(BizfiFiVoucher::getFdate)
                .orderByDesc(BizfiFiVoucher::getFid));
    }

    private boolean isCarryVoucher(BizfiFiVoucher voucher) {
        String text = (safeText(voucher.getFsummary(), "") + " " + safeText(voucher.getFremark(), ""))
                .toLowerCase(Locale.ROOT);
        for (String keyword : CARRY_KEYWORDS) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Integer countStatus(List<BizfiFiVoucher> vouchers, String status) {
        return (int) vouchers.stream()
                .filter(voucher -> status.equalsIgnoreCase(safeText(voucher.getFstatus(), "")))
                .count();
    }

    private BigDecimal sumAmount(List<BizfiFiVoucher> vouchers) {
        return vouchers.stream()
                .map(BizfiFiVoucher::getFamount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmountByStatus(List<BizfiFiVoucher> vouchers, String status) {
        return vouchers.stream()
                .filter(voucher -> status.equalsIgnoreCase(safeText(voucher.getFstatus(), "")))
                .map(BizfiFiVoucher::getFamount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolvePeriod(String period, BizfiFiOrgFinanceConfig config) {
        YearMonth fromParam = parsePeriod(period);
        if (fromParam != null) {
            return fromParam.toString();
        }
        YearMonth fromConfig = config == null ? null : parsePeriod(config.getFcurrentPeriod());
        if (fromConfig != null) {
            return fromConfig.toString();
        }
        return YearMonth.now().toString();
    }

    private String resolvePeriodSource(String period, BizfiFiOrgFinanceConfig config) {
        if (parsePeriod(period) != null) {
            return "PARAM";
        }
        if (config != null && parsePeriod(config.getFcurrentPeriod()) != null) {
            return "ORG_CONFIG";
        }
        return "SYSTEM";
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
