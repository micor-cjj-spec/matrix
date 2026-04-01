package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.service.BizfiFiDataHealthCheckService;
import single.cjj.fi.gl.service.BizfiFiOrgFinanceConfigService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;
import single.cjj.fi.gl.vo.PeriodMonitorCenterResultVO;
import single.cjj.fi.gl.vo.PeriodMonitorModuleVO;
import single.cjj.fi.gl.vo.PeriodProcessResultVO;
import single.cjj.fi.gl.vo.VoucherCarryTaskVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BizfiFiPeriodProcessServiceImpl implements BizfiFiPeriodProcessService {

    private static final List<String> PROFIT_LOSS_KEYWORDS = List.of("结转损益", "损益结转", "本年利润");
    private static final List<String> AUTO_TRANSFER_KEYWORDS = List.of("转账", "自动转账", "重分类", "内部结转");
    private static final List<String> FX_REVALUE_KEYWORDS = List.of("调汇", "汇兑", "汇兑损益", "期末调汇");
    private static final List<String> AMORTIZATION_KEYWORDS = List.of("摊销", "折旧", "待摊", "预提");
    private static final List<String> UNPOSTED_STATUS = List.of("DRAFT", "SUBMITTED", "AUDITED");

    @Autowired
    private BizfiFiVoucherMapper voucherMapper;

    @Autowired
    private BizfiFiAccountingPeriodMapper accountingPeriodMapper;

    @Autowired
    private BizfiFiOrgFinanceConfigService orgFinanceConfigService;

    @Autowired
    private BizfiFiDataHealthCheckService dataHealthCheckService;

    @Override
    public PeriodProcessResultVO profitLoss(Long forg, String period) {
        return buildModuleResult("PL", "结转损益", forg, period, PROFIT_LOSS_KEYWORDS, "建议先完成损益类凭证复核，再生成本年利润结转凭证。");
    }

    @Override
    public PeriodProcessResultVO autoTransfer(Long forg, String period) {
        return buildModuleResult("AT", "自动转账", forg, period, AUTO_TRANSFER_KEYWORDS, "适合在月末批量检查重分类和内部转账是否已完成。");
    }

    @Override
    public PeriodProcessResultVO fxRevalue(Long forg, String period) {
        PeriodProcessResultVO result = buildModuleResult("FX", "期末调汇", forg, period, FX_REVALUE_KEYWORDS, "先确认本位币与汇率维护，再检查调汇凭证是否已过账。");
        if (!StringUtils.hasText(result.getBaseCurrency())) {
            result.getWarnings().add("当前业务单元还没有本位币配置，无法完成期末调汇检查。");
            result.getTasks().add(new VoucherCarryTaskVO(
                    "BASE_CURRENCY",
                    "本位币设置",
                    "WARNING",
                    "当前业务单元还没有维护本位币或财务参数。",
                    "先维护组织财务参数中的本位币，再继续期末调汇流程。"
            ));
        } else if ("CNY".equalsIgnoreCase(result.getBaseCurrency())) {
            result.getWarnings().add("当前本位币为 CNY，如无外币业务，期末调汇可能无需执行。");
        }
        return refreshModuleStatus(result);
    }

    @Override
    public PeriodProcessResultVO voucherAmortization(Long forg, String period) {
        return buildModuleResult("AM", "凭证摊销", forg, period, AMORTIZATION_KEYWORDS, "适合在月末检查摊销、折旧、预提类凭证是否齐全。");
    }

    @Override
    public PeriodProcessResultVO closeBooks(Long forg, String period) {
        ModuleContext context = buildContext(forg, period);
        PeriodProcessResultVO result = createBaseResult("CL", "期末结账", context);

        List<BizfiFiVoucher> periodVouchers = loadPeriodVouchers(context.resolvedPeriod);
        long unpostedCount = periodVouchers.stream()
                .filter(item -> UNPOSTED_STATUS.contains(normalize(item.getFstatus())))
                .count();
        long postedCount = periodVouchers.stream()
                .filter(item -> "POSTED".equals(normalize(item.getFstatus())))
                .count();

        result.setPeriodVoucherCount(periodVouchers.size());
        result.setMatchedVoucherCount(periodVouchers.size());
        result.setPostedVoucherCount((int) postedCount);
        result.setPendingVoucherCount((int) unpostedCount);
        result.setExceptionVoucherCount((int) periodVouchers.stream()
                .filter(item -> List.of("REJECTED", "REVERSED").contains(normalize(item.getFstatus())))
                .count());
        result.setMatchedAmount(sumAmount(periodVouchers));
        result.setRelatedVouchers(periodVouchers.stream().limit(20).toList());

        addFoundationTasks(result, context);
        addPeriodTask(result, context, true);
        addVoucherPostingTask(result, periodVouchers);
        addCloseBooksTask(result, context, unpostedCount);

        if (periodVouchers.isEmpty()) {
            result.getWarnings().add("当前期间还没有凭证，若直接结账请先确认该期间确实无需处理。");
        }

        return refreshModuleStatus(result);
    }

    @Override
    public PeriodMonitorCenterResultVO monitorCenter(Long forg, String period) {
        PeriodProcessResultVO profitLoss = profitLoss(forg, period);
        PeriodProcessResultVO autoTransfer = autoTransfer(forg, period);
        PeriodProcessResultVO fxRevalue = fxRevalue(forg, period);
        PeriodProcessResultVO amortization = voucherAmortization(forg, period);
        PeriodProcessResultVO closeBooks = closeBooks(forg, period);

        List<PeriodProcessResultVO> modules = List.of(profitLoss, autoTransfer, fxRevalue, amortization, closeBooks);
        PeriodMonitorCenterResultVO result = new PeriodMonitorCenterResultVO();
        result.setForg(profitLoss.getForg());
        result.setPeriod(profitLoss.getPeriod());
        result.setPeriodSource(profitLoss.getPeriodSource());
        result.setBaseCurrency(profitLoss.getBaseCurrency());
        result.setCurrentPeriod(profitLoss.getCurrentPeriod());
        result.setPeriodStatus(profitLoss.getPeriodStatus());
        result.setFoundationHealthy(profitLoss.getFoundationHealthy());
        result.setTotalModules(modules.size());
        result.setReadyModules((int) modules.stream().filter(item -> "READY".equals(item.getModuleStatus()) || "DONE".equals(item.getModuleStatus())).count());
        result.setWarningModules((int) modules.stream().filter(item -> "WARNING".equals(item.getModuleStatus())).count());
        result.setPendingModules((int) modules.stream().filter(item -> "PENDING".equals(item.getModuleStatus())).count());
        result.setPeriodVoucherCount(closeBooks.getPeriodVoucherCount());
        result.setPendingVoucherCount(closeBooks.getPendingVoucherCount());

        List<PeriodMonitorModuleVO> rows = new ArrayList<>();
        for (PeriodProcessResultVO item : modules) {
            String summary = item.getWarnings().isEmpty()
                    ? safeText(item.getModuleName()) + " 已准备就绪。"
                    : item.getWarnings().get(0);
            String actionHint = item.getTasks().isEmpty() ? "进入页面查看详情。" : item.getTasks().get(0).getActionHint();
            rows.add(new PeriodMonitorModuleVO(
                    item.getModuleCode(),
                    item.getModuleName(),
                    item.getModuleStatus(),
                    summary,
                    actionHint,
                    defaultInt(item.getMatchedVoucherCount()),
                    defaultInt(item.getPendingVoucherCount())
            ));
        }
        result.setModules(rows);

        List<String> warnings = new ArrayList<>();
        if (result.getWarningModules() > 0) {
            warnings.add("当前仍有 " + result.getWarningModules() + " 个期末模块存在阻塞或待处理事项。");
        }
        if (Boolean.FALSE.equals(result.getFoundationHealthy())) {
            warnings.add("基础资料健康检查未通过，建议先修复主数据后再推进期末流程。");
        }
        if ("CLOSED".equalsIgnoreCase(result.getPeriodStatus())) {
            warnings.add("当前期间已经关闭，如需补做处理请先确认是否允许反结账。");
        }
        result.setWarnings(warnings);
        return result;
    }

    private PeriodProcessResultVO buildModuleResult(String code,
                                                    String name,
                                                    Long forg,
                                                    String period,
                                                    List<String> keywords,
                                                    String defaultHint) {
        ModuleContext context = buildContext(forg, period);
        PeriodProcessResultVO result = createBaseResult(code, name, context);
        List<BizfiFiVoucher> periodVouchers = loadPeriodVouchers(context.resolvedPeriod);
        List<BizfiFiVoucher> matchedVouchers = filterByKeywords(periodVouchers, keywords);

        result.setPeriodVoucherCount(periodVouchers.size());
        result.setMatchedVoucherCount(matchedVouchers.size());
        result.setPostedVoucherCount((int) matchedVouchers.stream().filter(item -> "POSTED".equals(normalize(item.getFstatus()))).count());
        result.setPendingVoucherCount((int) matchedVouchers.stream().filter(item -> UNPOSTED_STATUS.contains(normalize(item.getFstatus()))).count());
        result.setExceptionVoucherCount((int) matchedVouchers.stream().filter(item -> List.of("REJECTED", "REVERSED").contains(normalize(item.getFstatus()))).count());
        result.setMatchedAmount(sumAmount(matchedVouchers));
        result.setRelatedVouchers(matchedVouchers.stream().limit(20).toList());

        addFoundationTasks(result, context);
        addPeriodTask(result, context, false);
        addModuleVoucherTask(result, matchedVouchers, defaultHint);

        if (matchedVouchers.isEmpty()) {
            result.getWarnings().add(name + " 本期还没有识别到相关凭证。");
        } else if (result.getPendingVoucherCount() > 0) {
            result.getWarnings().add(name + " 相关凭证里还有 " + result.getPendingVoucherCount() + " 张未过账。");
        }

        return refreshModuleStatus(result);
    }

    private ModuleContext buildContext(Long forg, String period) {
        ModuleContext context = new ModuleContext();
        context.forg = forg;
        context.config = forg == null ? null : getConfigQuietly(forg);
        context.resolvedPeriod = resolvePeriod(period, context.config);
        context.periodSource = resolvePeriodSource(period, context.config);
        context.accountingPeriod = loadAccountingPeriod(forg, context.resolvedPeriod);
        context.healthCheck = forg == null ? null : loadHealthQuietly(forg);
        return context;
    }

    private PeriodProcessResultVO createBaseResult(String code, String name, ModuleContext context) {
        PeriodProcessResultVO result = new PeriodProcessResultVO();
        result.setModuleCode(code);
        result.setModuleName(name);
        result.setForg(context.forg);
        result.setPeriod(context.resolvedPeriod);
        result.setPeriodSource(context.periodSource);
        result.setBaseCurrency(context.config == null ? null : context.config.getFbaseCurrency());
        result.setCurrentPeriod(context.config == null ? null : context.config.getFcurrentPeriod());
        result.setPeriodStatus(context.accountingPeriod == null ? "MISSING" : context.accountingPeriod.getFstatus());
        result.setDefaultVoucherType(context.config == null ? null : context.config.getFdefaultVoucherType());
        result.setFoundationHealthy(context.healthCheck == null ? null : context.healthCheck.getHealthy());
        result.setTasks(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        result.setRelatedVouchers(new ArrayList<>());
        result.setMatchedAmount(BigDecimal.ZERO);
        result.setModuleStatus("PENDING");
        return result;
    }

    private void addFoundationTasks(PeriodProcessResultVO result, ModuleContext context) {
        if (context.config == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ORG_CONFIG",
                    "组织财务参数",
                    "WARNING",
                    "当前业务单元还没有维护组织财务参数。",
                    "先维护本位币、当前期间、默认凭证字等财务参数。"
            ));
        } else {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ORG_CONFIG",
                    "组织财务参数",
                    "READY",
                    "已识别组织财务参数，本位币 " + safeText(context.config.getFbaseCurrency()) + "，当前期间 " + safeText(context.config.getFcurrentPeriod()) + "。",
                    "可继续执行期末检查。"
            ));
        }

        if (context.healthCheck == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "FOUNDATION_HEALTH",
                    "基础资料健康检查",
                    "PENDING",
                    "未执行基础资料健康检查。",
                    "建议先选择业务单元并执行主数据健康检查。"
            ));
            return;
        }

        boolean healthy = Boolean.TRUE.equals(context.healthCheck.getHealthy());
        result.getTasks().add(new VoucherCarryTaskVO(
                "FOUNDATION_HEALTH",
                "基础资料健康检查",
                healthy ? "READY" : "WARNING",
                healthy
                        ? "基础资料健康检查通过。"
                        : "健康检查发现 " + defaultInt(context.healthCheck.getTotalIssueCount()) + " 条问题待处理。",
                healthy ? "可继续执行期末流程。" : "建议先修复科目、报表映射、期间等主数据问题。"
        ));

        if (!healthy && context.healthCheck.getNotes() != null) {
            context.healthCheck.getNotes().stream()
                    .filter(StringUtils::hasText)
                    .limit(2)
                    .map(note -> "基础资料检查: " + note.trim())
                    .forEach(result.getWarnings()::add);
        }
    }

    private void addPeriodTask(PeriodProcessResultVO result, ModuleContext context, boolean closeBooksMode) {
        if (!StringUtils.hasText(context.resolvedPeriod)) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "WARNING",
                    "无法识别当前会计期间。",
                    "先从组织财务参数维护当前期间或在页面手动输入。"
            ));
            return;
        }
        if (context.accountingPeriod == null) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "WARNING",
                    "当前期间 " + context.resolvedPeriod + " 没有会计期间档案。",
                    "先补录会计期间，再推进期末处理。"
            ));
            return;
        }
        String status = normalize(context.accountingPeriod.getFstatus());
        String taskStatus;
        String message;
        String actionHint;
        if ("CLOSED".equals(status)) {
            taskStatus = closeBooksMode ? "DONE" : "WARNING";
            message = "当前期间 " + context.accountingPeriod.getFperiod() + " 已关闭。";
            actionHint = closeBooksMode ? "如当前是收尾复核，可直接查看结账结果。" : "如需补做处理，请先确认是否允许反结账。";
        } else {
            taskStatus = "READY";
            message = "当前期间 " + context.accountingPeriod.getFperiod() + " 处于 OPEN 状态。";
            actionHint = closeBooksMode ? "满足条件后可继续推进结账。" : "可继续执行当前模块处理。";
        }
        result.getTasks().add(new VoucherCarryTaskVO(
                "ACCOUNTING_PERIOD",
                "会计期间",
                taskStatus,
                message,
                actionHint
        ));
    }

    private void addModuleVoucherTask(PeriodProcessResultVO result, List<BizfiFiVoucher> matchedVouchers, String defaultHint) {
        if (matchedVouchers.isEmpty()) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "MODULE_VOUCHER",
                    "相关凭证识别",
                    "PENDING",
                    "当前期间还没有识别到本模块相关凭证。",
                    defaultHint
            ));
            return;
        }

        long postedCount = matchedVouchers.stream().filter(item -> "POSTED".equals(normalize(item.getFstatus()))).count();
        long pendingCount = matchedVouchers.stream().filter(item -> UNPOSTED_STATUS.contains(normalize(item.getFstatus()))).count();
        String status = pendingCount == 0 ? "READY" : "WARNING";
        result.getTasks().add(new VoucherCarryTaskVO(
                "MODULE_VOUCHER",
                "相关凭证识别",
                status,
                "已识别 " + matchedVouchers.size() + " 张相关凭证，其中 " + postedCount + " 张已过账，" + pendingCount + " 张待处理。",
                pendingCount == 0 ? "相关凭证已基本到位，可继续执行。" : "建议先把未过账凭证处理完，再推进当前模块。"
        ));
    }

    private void addVoucherPostingTask(PeriodProcessResultVO result, List<BizfiFiVoucher> periodVouchers) {
        if (periodVouchers.isEmpty()) {
            result.getTasks().add(new VoucherCarryTaskVO(
                    "VOUCHER_POSTING",
                    "期间凭证过账",
                    "PENDING",
                    "当前期间还没有凭证记录。",
                    "若该期间已有业务，请先完成制单、审核和过账。"
            ));
            return;
        }
        long unpostedCount = periodVouchers.stream().filter(item -> UNPOSTED_STATUS.contains(normalize(item.getFstatus()))).count();
        long postedCount = periodVouchers.stream().filter(item -> "POSTED".equals(normalize(item.getFstatus()))).count();
        result.getTasks().add(new VoucherCarryTaskVO(
                "VOUCHER_POSTING",
                "期间凭证过账",
                unpostedCount == 0 ? "READY" : "WARNING",
                "当前期间共 " + periodVouchers.size() + " 张凭证，其中 " + postedCount + " 张已过账，" + unpostedCount + " 张未过账。",
                unpostedCount == 0 ? "可继续结账。" : "先处理未过账凭证，再尝试期末结账。"
        ));
    }

    private void addCloseBooksTask(PeriodProcessResultVO result, ModuleContext context, long unpostedCount) {
        String status;
        String message;
        String actionHint;
        if (context.accountingPeriod == null) {
            status = "WARNING";
            message = "当前期间没有会计期间档案，无法执行结账。";
            actionHint = "先补录会计期间档案。";
        } else if ("CLOSED".equals(normalize(context.accountingPeriod.getFstatus()))) {
            status = "DONE";
            message = "当前期间已经关闭，可作为结账结果复核。";
            actionHint = "如需补做处理，请先确认是否允许反结账。";
        } else if (unpostedCount > 0) {
            status = "WARNING";
            message = "当前期间仍有 " + unpostedCount + " 张凭证未过账，暂不建议直接结账。";
            actionHint = "先把剩余凭证处理到 POSTED，再执行结账。";
        } else {
            status = "READY";
            message = "当前期间满足基础结账条件，可继续推进结账。";
            actionHint = "建议先复核结转、调汇、摊销等模块，再正式关闭期间。";
        }
        result.getTasks().add(new VoucherCarryTaskVO("CLOSE_BOOKS", "期末结账判断", status, message, actionHint));
    }

    private PeriodProcessResultVO refreshModuleStatus(PeriodProcessResultVO result) {
        if (result.getTasks().stream().anyMatch(item -> "WARNING".equals(item.getStatus()))) {
            result.setModuleStatus("WARNING");
        } else if (result.getTasks().stream().anyMatch(item -> "PENDING".equals(item.getStatus()))) {
            result.setModuleStatus("PENDING");
        } else if (result.getTasks().stream().allMatch(item -> "DONE".equals(item.getStatus()))) {
            result.setModuleStatus("DONE");
        } else {
            result.setModuleStatus("READY");
        }
        return result;
    }

    private BizfiFiOrgFinanceConfig getConfigQuietly(Long forg) {
        try {
            return orgFinanceConfigService.getByOrg(forg);
        } catch (Exception ex) {
            return null;
        }
    }

    private BizfiFiHealthCheckResultVO loadHealthQuietly(Long forg) {
        try {
            return dataHealthCheckService.check(forg, null, 5);
        } catch (Exception ex) {
            return null;
        }
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

    private List<BizfiFiVoucher> filterByKeywords(List<BizfiFiVoucher> vouchers, List<String> keywords) {
        return vouchers.stream()
                .filter(item -> matchesKeywords(item, keywords))
                .sorted(Comparator.comparing(BizfiFiVoucher::getFdate, Comparator.nullsLast(LocalDate::compareTo)).reversed()
                        .thenComparing(BizfiFiVoucher::getFid, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean matchesKeywords(BizfiFiVoucher voucher, List<String> keywords) {
        String text = (safeText(voucher.getFsummary()) + " " + safeText(voucher.getFremark())).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal sumAmount(List<BizfiFiVoucher> vouchers) {
        return vouchers.stream()
                .map(BizfiFiVoucher::getFamount)
                .filter(item -> item != null)
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

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private static class ModuleContext {
        private Long forg;
        private String resolvedPeriod;
        private String periodSource;
        private BizfiFiOrgFinanceConfig config;
        private BizfiFiAccountingPeriod accountingPeriod;
        private BizfiFiHealthCheckResultVO healthCheck;
    }
}
