package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiGlEntry;
import single.cjj.fi.gl.entity.BizfiFiOrgFinanceConfig;
import single.cjj.fi.gl.entity.BizfiFiVoucher;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.mapper.BizfiFiGlEntryMapper;
import single.cjj.fi.gl.mapper.BizfiFiVoucherMapper;
import single.cjj.fi.gl.report.service.BizfiFiBalanceSheetService;
import single.cjj.fi.gl.report.service.BizfiFiCashFlowService;
import single.cjj.fi.gl.report.service.BizfiFiProfitStatementService;
import single.cjj.fi.gl.report.vo.ReportQueryResultVO;
import single.cjj.fi.gl.service.BizfiFiDataHealthCheckService;
import single.cjj.fi.gl.service.BizfiFiOrgFinanceConfigService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckResultVO;
import single.cjj.fi.gl.vo.BizfiFiHealthCheckIssueVO;
import single.cjj.fi.gl.vo.MonthEndCheckItemVO;
import single.cjj.fi.gl.vo.MonthEndStepVO;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;
import single.cjj.fi.gl.vo.PeriodMonitorCenterResultVO;
import single.cjj.fi.gl.vo.PeriodMonitorModuleVO;
import single.cjj.fi.gl.vo.PeriodProcessResultVO;
import single.cjj.fi.gl.vo.VoucherCarryTaskVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
    private BizfiFiGlEntryMapper glEntryMapper;

    @Autowired
    private BizfiFiAccountingPeriodMapper accountingPeriodMapper;

    @Autowired
    private BizfiFiOrgFinanceConfigService orgFinanceConfigService;

    @Autowired
    private BizfiFiDataHealthCheckService dataHealthCheckService;

    @Autowired
    private BizfiFiBalanceSheetService balanceSheetService;

    @Autowired
    private BizfiFiProfitStatementService profitStatementService;

    @Autowired
    private BizfiFiCashFlowService cashFlowService;

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

    @Override
    public MonthEndWorkbenchResultVO monthEndWorkbench(Long forg, String period) {
        PeriodProcessResultVO profitLoss = profitLoss(forg, period);
        PeriodProcessResultVO autoTransfer = autoTransfer(forg, period);
        PeriodProcessResultVO fxRevalue = fxRevalue(forg, period);
        PeriodProcessResultVO amortization = voucherAmortization(forg, period);
        PeriodProcessResultVO closeBooks = closeBooks(forg, period);
        List<PeriodProcessResultVO> modules = List.of(profitLoss, autoTransfer, fxRevalue, amortization, closeBooks);

        MonthEndWorkbenchResultVO result = new MonthEndWorkbenchResultVO();
        result.setForg(closeBooks.getForg());
        result.setPeriod(closeBooks.getPeriod());
        result.setPeriodSource(closeBooks.getPeriodSource());
        result.setBaseCurrency(closeBooks.getBaseCurrency());
        result.setCurrentPeriod(closeBooks.getCurrentPeriod());
        result.setPeriodStatus(closeBooks.getPeriodStatus());
        result.setPeriodVoucherCount(defaultInt(closeBooks.getPeriodVoucherCount()));
        result.setPostedVoucherCount(defaultInt(closeBooks.getPostedVoucherCount()));
        result.setPendingVoucherCount(defaultInt(closeBooks.getPendingVoucherCount()));
        result.setExceptionVoucherCount(defaultInt(closeBooks.getExceptionVoucherCount()));
        result.setCheckedAt(LocalDateTime.now());

        List<MonthEndCheckItemVO> checkItems = new ArrayList<>();
        appendFoundationChecks(checkItems, forg, closeBooks);
        appendPeriodCheck(checkItems, closeBooks);
        appendVoucherCheck(checkItems, closeBooks);
        appendGlBalanceCheck(checkItems, closeBooks.getPeriod());
        appendPeriodModuleChecks(checkItems, modules);
        appendReportCheck(checkItems, result);
        appendCloseDecisionCheck(checkItems, closeBooks);
        result.setCheckItems(checkItems);
        result.setSteps(buildMonthEndSteps(modules, checkItems));

        int blockingCount = countCheckStatus(checkItems, "BLOCKED");
        int warningCount = countCheckStatus(checkItems, "WARNING");
        int pendingCount = countCheckStatus(checkItems, "PENDING");
        int passedCount = countCheckStatus(checkItems, "PASSED");
        result.setBlockingCount(blockingCount);
        result.setWarningCount(warningCount);
        result.setPendingCount(pendingCount);
        result.setPassedCount(passedCount);
        result.setTotalCheckCount(checkItems.size());
        result.setReadinessScore(calculateReadinessScore(checkItems));

        String periodStatus = normalize(closeBooks.getPeriodStatus());
        if ("CLOSED".equals(periodStatus)) {
            result.setCloseStatus("CLOSED");
            result.setCanClose(false);
        } else if (blockingCount > 0) {
            result.setCloseStatus("BLOCKED");
            result.setCanClose(false);
        } else if (warningCount > 0 || pendingCount > 0) {
            result.setCloseStatus("WARNING");
            result.setCanClose(false);
        } else {
            result.setCloseStatus("READY");
            result.setCanClose(true);
        }

        List<String> warnings = new ArrayList<>();
        for (PeriodProcessResultVO module : modules) {
            if (module.getWarnings() != null) {
                module.getWarnings().stream()
                        .filter(StringUtils::hasText)
                        .forEach(warnings::add);
            }
        }
        checkItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getBlocking()))
                .map(item -> item.getName() + ": " + item.getMessage())
                .forEach(warnings::add);
        result.setWarnings(warnings.stream().distinct().toList());
        return result;
    }

    private void appendFoundationChecks(List<MonthEndCheckItemVO> checkItems, Long forg, PeriodProcessResultVO closeBooks) {
        VoucherCarryTaskVO orgTask = findTask(closeBooks, "ORG_CONFIG");
        if (orgTask == null || !StringUtils.hasText(closeBooks.getBaseCurrency())) {
            checkItems.add(checkItem(
                    "ORG_CONFIG",
                    "组织财务参数",
                    "FOUNDATION",
                    "BLOCKED",
                    "HIGH",
                    orgTask == null ? "未识别到组织财务参数。" : orgTask.getMessage(),
                    orgTask == null ? "先维护组织财务参数。" : orgTask.getActionHint(),
                    "/ledger/period-monitor-center",
                    1,
                    true
            ));
        } else {
            checkItems.add(checkItem(
                    "ORG_CONFIG",
                    "组织财务参数",
                    "FOUNDATION",
                    "PASSED",
                    "LOW",
                    orgTask.getMessage(),
                    "可继续执行关账前检查。",
                    "/ledger/period-monitor-center",
                    0,
                    false
            ));
        }

        BizfiFiHealthCheckResultVO healthCheck = forg == null ? null : loadHealthQuietly(forg);
        if (healthCheck == null) {
            checkItems.add(checkItem(
                    "FOUNDATION_HEALTH",
                    "基础资料健康检查",
                    "FOUNDATION",
                    "PENDING",
                    "MEDIUM",
                    "未获取到基础资料健康检查结果。",
                    "建议先执行基础资料健康检查，再推进关账。",
                    "/finance/base-data/account-subject",
                    0,
                    false
            ));
            return;
        }

        int issueCount = defaultInt(healthCheck.getTotalIssueCount());
        boolean highRisk = hasHighHealthIssue(healthCheck);
        boolean healthy = Boolean.TRUE.equals(healthCheck.getHealthy());
        checkItems.add(checkItem(
                "FOUNDATION_HEALTH",
                "基础资料健康检查",
                "FOUNDATION",
                healthy ? "PASSED" : highRisk ? "BLOCKED" : "WARNING",
                highRisk ? "HIGH" : healthy ? "LOW" : "MEDIUM",
                healthy ? "当前检查范围内未发现基础资料缺口。" : "基础资料健康检查发现 " + issueCount + " 条问题。",
                healthy ? "可继续关账前检查。" : "先修复科目、报表映射、期间等主数据问题。",
                "/finance/base-data/account-subject",
                issueCount,
                highRisk
        ));
    }

    private void appendPeriodCheck(List<MonthEndCheckItemVO> checkItems, PeriodProcessResultVO closeBooks) {
        String periodStatus = normalize(closeBooks.getPeriodStatus());
        if (!StringUtils.hasText(periodStatus) || "MISSING".equals(periodStatus)) {
            checkItems.add(checkItem(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "FOUNDATION",
                    "BLOCKED",
                    "HIGH",
                    "当前期间 " + safeText(closeBooks.getPeriod()) + " 没有会计期间档案。",
                    "先补录会计期间，再执行关账前检查。",
                    "/ledger/period-close-books",
                    1,
                    true
            ));
        } else if ("OPEN".equals(periodStatus) || "CLOSED".equals(periodStatus)) {
            checkItems.add(checkItem(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "FOUNDATION",
                    "PASSED",
                    "LOW",
                    "当前期间状态为 " + periodStatus + "。",
                    "可继续关账前检查。",
                    "/ledger/period-close-books",
                    0,
                    false
            ));
        } else {
            checkItems.add(checkItem(
                    "ACCOUNTING_PERIOD",
                    "会计期间",
                    "FOUNDATION",
                    "BLOCKED",
                    "HIGH",
                    "当前期间状态为 " + periodStatus + "，不属于 OPEN 或 CLOSED。",
                    "先确认会计期间状态是否允许月结。",
                    "/ledger/period-close-books",
                    1,
                    true
            ));
        }
    }

    private void appendVoucherCheck(List<MonthEndCheckItemVO> checkItems, PeriodProcessResultVO closeBooks) {
        int total = defaultInt(closeBooks.getPeriodVoucherCount());
        int pending = defaultInt(closeBooks.getPendingVoucherCount());
        int exception = defaultInt(closeBooks.getExceptionVoucherCount());
        if (pending > 0) {
            checkItems.add(checkItem(
                    "VOUCHER_POSTING",
                    "期间凭证过账",
                    "VOUCHER",
                    "BLOCKED",
                    "HIGH",
                    "当前期间仍有 " + pending + " 张凭证未过账。",
                    "先处理未过账凭证，再推进关账。",
                    "/ledger/voucher",
                    pending,
                    true
            ));
        } else if (exception > 0) {
            checkItems.add(checkItem(
                    "VOUCHER_POSTING",
                    "期间凭证过账",
                    "VOUCHER",
                    "WARNING",
                    "MEDIUM",
                    "当前期间存在 " + exception + " 张异常状态凭证。",
                    "建议复核驳回、冲销等异常凭证是否已完成处理。",
                    "/ledger/voucher",
                    exception,
                    false
            ));
        } else if (total == 0) {
            checkItems.add(checkItem(
                    "VOUCHER_POSTING",
                    "期间凭证过账",
                    "VOUCHER",
                    "WARNING",
                    "MEDIUM",
                    "当前期间还没有凭证记录。",
                    "若该期间确实无业务，可人工确认后继续。",
                    "/ledger/voucher",
                    0,
                    false
            ));
        } else {
            checkItems.add(checkItem(
                    "VOUCHER_POSTING",
                    "期间凭证过账",
                    "VOUCHER",
                    "PASSED",
                    "LOW",
                    "当前期间 " + total + " 张凭证已完成过账检查。",
                    "可继续关账前检查。",
                    "/ledger/voucher",
                    total,
                    false
            ));
        }
    }

    private void appendGlBalanceCheck(List<MonthEndCheckItemVO> checkItems, String period) {
        GlBalanceSnapshot snapshot = buildGlBalanceSnapshot(period);
        if (!snapshot.validPeriod) {
            checkItems.add(checkItem(
                    "GL_BALANCE",
                    "总账借贷平衡",
                    "LEDGER",
                    "BLOCKED",
                    "HIGH",
                    "无法解析检查期间。",
                    "先确认期间格式为 yyyy-MM。",
                    "/ledger/general-ledger",
                    1,
                    true
            ));
            return;
        }
        if (snapshot.entryCount == 0) {
            checkItems.add(checkItem(
                    "GL_BALANCE",
                    "总账借贷平衡",
                    "LEDGER",
                    "WARNING",
                    "MEDIUM",
                    "当前期间没有总账分录。",
                    "若该期间已有凭证，请先检查凭证是否完成过账生成总账分录。",
                    "/ledger/general-ledger",
                    0,
                    false
            ));
            return;
        }
        boolean balanced = snapshot.debitAmount.compareTo(snapshot.creditAmount) == 0;
        checkItems.add(checkItem(
                "GL_BALANCE",
                "总账借贷平衡",
                "LEDGER",
                balanced ? "PASSED" : "BLOCKED",
                balanced ? "LOW" : "HIGH",
                balanced
                        ? "当前期间总账分录借贷平衡，借方和贷方均为 " + snapshot.debitAmount + "。"
                        : "当前期间总账借方 " + snapshot.debitAmount + "，贷方 " + snapshot.creditAmount + "，存在差额。",
                balanced ? "可继续关账前检查。" : "先定位总账分录差异，再推进关账。",
                "/ledger/general-ledger",
                snapshot.entryCount,
                !balanced
        ));
    }

    private void appendPeriodModuleChecks(List<MonthEndCheckItemVO> checkItems, List<PeriodProcessResultVO> modules) {
        for (PeriodProcessResultVO module : modules) {
            if ("CL".equals(module.getModuleCode())) {
                continue;
            }
            String status = moduleStatusToCheckStatus(module.getModuleStatus());
            boolean blocking = false;
            checkItems.add(checkItem(
                    "PERIOD_MODULE_" + module.getModuleCode(),
                    module.getModuleName(),
                    "PERIOD_END",
                    status,
                    "PASSED".equals(status) ? "LOW" : "MEDIUM",
                    moduleSummary(module),
                    moduleActionHint(module),
                    moduleRoutePath(module.getModuleCode()),
                    defaultInt(module.getPendingVoucherCount()),
                    blocking
            ));
        }
    }

    private void appendReportCheck(List<MonthEndCheckItemVO> checkItems, MonthEndWorkbenchResultVO result) {
        String currency = StringUtils.hasText(result.getBaseCurrency()) ? result.getBaseCurrency() : "CNY";
        List<ReportProbeResult> probes = List.of(
                probeReport("资产负债表", () -> balanceSheetService.query(result.getForg(), result.getPeriod(), currency, null, true)),
                probeReport("利润表", () -> profitStatementService.query(result.getForg(), firstMonthOfYear(result.getPeriod()), result.getPeriod(), currency, null, true)),
                probeReport("现金流量表", () -> cashFlowService.query(result.getForg(), result.getPeriod(), currency, null, true))
        );
        long failedCount = probes.stream().filter(item -> !item.success).count();
        int warningCount = probes.stream().mapToInt(item -> item.warningCount).sum();
        String message = probes.stream()
                .map(item -> item.message)
                .filter(StringUtils::hasText)
                .distinct()
                .toList()
                .toString();
        String status = failedCount > 0 || warningCount > 0 ? "WARNING" : "PASSED";
        checkItems.add(checkItem(
                "REPORT_GENERATION",
                "报表生成检查",
                "REPORT",
                status,
                failedCount > 0 ? "MEDIUM" : "LOW",
                "PASSED".equals(status) ? "资产负债表、利润表、现金流量表均可生成。" : message,
                "PASSED".equals(status) ? "可继续关账前检查。" : "建议进入报表页面复核模板、映射和现金流项目。",
                "/ledger/balance-sheet",
                warningCount,
                false
        ));
    }

    private void appendCloseDecisionCheck(List<MonthEndCheckItemVO> checkItems, PeriodProcessResultVO closeBooks) {
        String periodStatus = normalize(closeBooks.getPeriodStatus());
        if ("CLOSED".equals(periodStatus)) {
            checkItems.add(checkItem(
                    "CLOSE_DECISION",
                    "关账判断",
                    "CLOSE",
                    "PASSED",
                    "LOW",
                    "当前期间已经关闭，本页面作为关账结果复核。",
                    "如需补做处理，请先确认是否允许反结账。",
                    "/ledger/period-close-books",
                    0,
                    false
            ));
        } else {
            int existingBlockingCount = countCheckStatus(checkItems, "BLOCKED");
            int existingAttentionCount = countCheckStatus(checkItems, "WARNING") + countCheckStatus(checkItems, "PENDING");
            String status = existingBlockingCount > 0 ? "BLOCKED" : existingAttentionCount > 0 ? "WARNING" : "PASSED";
            checkItems.add(checkItem(
                    "CLOSE_DECISION",
                    "关账判断",
                    "CLOSE",
                    status,
                    existingBlockingCount > 0 ? "HIGH" : existingAttentionCount > 0 ? "MEDIUM" : "LOW",
                    existingBlockingCount > 0
                            ? "仍有 " + existingBlockingCount + " 个阻塞项，暂不建议关账。"
                            : existingAttentionCount > 0
                            ? "没有强阻塞，但仍有 " + existingAttentionCount + " 个预警或待确认事项。"
                            : "首版基础关账条件已完成检查。",
                    existingBlockingCount > 0
                            ? "先处理阻塞项，再重新执行关账前检查。"
                            : existingAttentionCount > 0
                            ? "复核预警事项后，再结合制度判断是否进入关账。"
                            : "正式关账动作需结合审批和审计规则另行开放。",
                    "/ledger/period-close-books",
                    existingBlockingCount + existingAttentionCount,
                    existingBlockingCount > 0
            ));
        }
    }

    private List<MonthEndStepVO> buildMonthEndSteps(List<PeriodProcessResultVO> modules, List<MonthEndCheckItemVO> checkItems) {
        List<MonthEndStepVO> steps = new ArrayList<>();
        int orderNo = 1;
        for (PeriodProcessResultVO module : modules) {
            steps.add(new MonthEndStepVO(
                    orderNo++,
                    module.getModuleCode(),
                    module.getModuleName(),
                    module.getModuleStatus(),
                    moduleSummary(module),
                    moduleActionHint(module),
                    moduleRoutePath(module.getModuleCode()),
                    "WARNING".equals(module.getModuleStatus()) ? 1 : 0,
                    "PENDING".equals(module.getModuleStatus()) ? 1 : 0
            ));
        }
        MonthEndCheckItemVO reportCheck = checkItems.stream()
                .filter(item -> "REPORT_GENERATION".equals(item.getCode()))
                .findFirst()
                .orElse(null);
        String reportStatus = reportCheck == null ? "PENDING" : checkStatusToStepStatus(reportCheck.getStatus());
        steps.add(new MonthEndStepVO(
                orderNo,
                "RP",
                "报表生成",
                reportStatus,
                reportCheck == null ? "尚未执行报表生成检查。" : reportCheck.getMessage(),
                reportCheck == null ? "进入报表页面复核。" : reportCheck.getActionHint(),
                "/ledger/balance-sheet",
                reportCheck != null && "BLOCKED".equals(reportCheck.getStatus()) ? 1 : 0,
                reportCheck != null && "WARNING".equals(reportCheck.getStatus()) ? 1 : 0
        ));
        return steps;
    }

    private GlBalanceSnapshot buildGlBalanceSnapshot(String period) {
        YearMonth yearMonth = parsePeriod(period);
        if (yearMonth == null) {
            return new GlBalanceSnapshot(false, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<BizfiFiGlEntry> entries = glEntryMapper.selectList(new LambdaQueryWrapper<BizfiFiGlEntry>()
                .ge(BizfiFiGlEntry::getFvoucherDate, startDate)
                .le(BizfiFiGlEntry::getFvoucherDate, endDate));
        BigDecimal debit = entries.stream()
                .map(BizfiFiGlEntry::getFdebitAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream()
                .map(BizfiFiGlEntry::getFcreditAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GlBalanceSnapshot(true, entries.size(), debit, credit);
    }

    private MonthEndCheckItemVO checkItem(String code,
                                          String name,
                                          String category,
                                          String status,
                                          String severity,
                                          String message,
                                          String actionHint,
                                          String routePath,
                                          Integer relatedCount,
                                          Boolean blocking) {
        return new MonthEndCheckItemVO(code, name, category, status, severity, message, actionHint, routePath, relatedCount, blocking);
    }

    private VoucherCarryTaskVO findTask(PeriodProcessResultVO result, String code) {
        if (result == null || result.getTasks() == null) {
            return null;
        }
        return result.getTasks().stream()
                .filter(item -> code.equals(item.getCode()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasHighHealthIssue(BizfiFiHealthCheckResultVO healthCheck) {
        return healthCheck != null
                && healthCheck.getIssues() != null
                && healthCheck.getIssues().stream()
                .filter(Objects::nonNull)
                .map(BizfiFiHealthCheckIssueVO::getSeverity)
                .anyMatch(severity -> "HIGH".equals(normalize(severity)));
    }

    private String moduleStatusToCheckStatus(String status) {
        String normalized = normalize(status);
        if ("READY".equals(normalized) || "DONE".equals(normalized)) {
            return "PASSED";
        }
        if ("WARNING".equals(normalized)) {
            return "WARNING";
        }
        return "PENDING";
    }

    private String checkStatusToStepStatus(String status) {
        String normalized = normalize(status);
        if ("PASSED".equals(normalized)) {
            return "READY";
        }
        if ("BLOCKED".equals(normalized)) {
            return "WARNING";
        }
        return normalized;
    }

    private String moduleSummary(PeriodProcessResultVO module) {
        if (module.getWarnings() != null && !module.getWarnings().isEmpty()) {
            return module.getWarnings().get(0);
        }
        return safeText(module.getModuleName()) + " 已完成准备度检查。";
    }

    private String moduleActionHint(PeriodProcessResultVO module) {
        if (module.getTasks() != null && !module.getTasks().isEmpty()) {
            return module.getTasks().get(0).getActionHint();
        }
        return "进入模块查看详情。";
    }

    private String moduleRoutePath(String moduleCode) {
        return switch (normalize(moduleCode)) {
            case "PL" -> "/ledger/period-profit-loss";
            case "AT" -> "/ledger/period-auto-transfer";
            case "FX" -> "/ledger/period-fx-revalue";
            case "AM" -> "/ledger/period-voucher-amortization";
            case "CL" -> "/ledger/period-close-books";
            default -> "/ledger/period-monitor-center";
        };
    }

    private int countCheckStatus(List<MonthEndCheckItemVO> items, String status) {
        return (int) items.stream()
                .filter(item -> status.equals(item.getStatus()))
                .count();
    }

    private int calculateReadinessScore(List<MonthEndCheckItemVO> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (MonthEndCheckItemVO item : items) {
            String status = normalize(item.getStatus());
            if ("PASSED".equals(status)) {
                score += 100;
            } else if ("WARNING".equals(status) || "PENDING".equals(status)) {
                score += 50;
            }
        }
        return Math.round((float) score / items.size());
    }

    private ReportProbeResult probeReport(String reportName, ReportSupplier supplier) {
        try {
            ReportQueryResultVO report = supplier.get();
            int warningCount = report == null || report.getWarnings() == null ? 0 : report.getWarnings().size();
            boolean checksPassed = report == null || report.getChecks() == null
                    || report.getChecks().stream().allMatch(check -> check.isPassed());
            if (!checksPassed) {
                warningCount++;
            }
            String message = warningCount == 0
                    ? reportName + " 可生成。"
                    : reportName + " 可生成，但存在 " + warningCount + " 条提示。";
            return new ReportProbeResult(true, warningCount, message);
        } catch (Exception ex) {
            return new ReportProbeResult(false, 1, reportName + " 生成失败。");
        }
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

    private String firstMonthOfYear(String period) {
        YearMonth ym = YearMonth.parse(normalizePeriod(period));
        return YearMonth.of(ym.getYear(), 1).toString();
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(period).toString();
        } catch (DateTimeParseException ex) {
            return YearMonth.now().toString();
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

    private interface ReportSupplier {
        ReportQueryResultVO get();
    }

    private static class ReportProbeResult {
        private final boolean success;
        private final int warningCount;
        private final String message;

        private ReportProbeResult(boolean success, int warningCount, String message) {
            this.success = success;
            this.warningCount = warningCount;
            this.message = message;
        }
    }

    private static class GlBalanceSnapshot {
        private final boolean validPeriod;
        private final int entryCount;
        private final BigDecimal debitAmount;
        private final BigDecimal creditAmount;

        private GlBalanceSnapshot(boolean validPeriod, int entryCount, BigDecimal debitAmount, BigDecimal creditAmount) {
            this.validPeriod = validPeriod;
            this.entryCount = entryCount;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
        }
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
