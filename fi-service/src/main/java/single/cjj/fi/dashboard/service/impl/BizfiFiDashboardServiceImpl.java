package single.cjj.fi.dashboard.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.ar.service.BizfiFiArapDocService;
import single.cjj.fi.dashboard.service.BizfiFiDashboardService;
import single.cjj.fi.dashboard.vo.FinanceDashboardOverviewVO;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.service.BizfiFiVoucherAnalysisService;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;
import single.cjj.fi.gl.vo.VoucherSummaryResultVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BizfiFiDashboardServiceImpl implements BizfiFiDashboardService {

    @Autowired
    private BizfiFiVoucherAnalysisService voucherAnalysisService;

    @Autowired
    private BizfiFiPeriodProcessService periodProcessService;

    @Autowired
    private BizfiFiArapDocService arapDocService;

    @Override
    public FinanceDashboardOverviewVO overview(Long forg, String period) {
        YearMonth yearMonth = resolvePeriod(period);
        FinanceDashboardOverviewVO result = new FinanceDashboardOverviewVO();
        result.setForg(forg);
        result.setPeriod(yearMonth.toString());
        result.setGeneratedAt(LocalDateTime.now());

        loadVoucherSummary(result, yearMonth);
        loadMonthEnd(result, forg, yearMonth.toString());
        loadCreditWarnings(result);
        buildFocusItems(result);
        return result;
    }

    private void loadVoucherSummary(FinanceDashboardOverviewVO result, YearMonth yearMonth) {
        try {
            VoucherSummaryResultVO summary = voucherAnalysisService.summary(
                    yearMonth.atDay(1).toString(),
                    yearMonth.atEndOfMonth().toString(),
                    null,
                    null
            );
            if (summary == null) {
                addWarning(result, "凭证汇总暂不可用");
                return;
            }
            result.setMonthVoucherCount(defaultInt(summary.getTotalCount()));
            result.setPostedVoucherCount(defaultInt(summary.getPostedCount()));
            result.setPendingVoucherCount(
                    defaultInt(summary.getDraftCount())
                            + defaultInt(summary.getSubmittedCount())
                            + defaultInt(summary.getAuditedCount())
            );
            result.setExceptionVoucherCount(
                    defaultInt(summary.getRejectedCount())
                            + defaultInt(summary.getReversedCount())
            );
            if (summary.getWarnings() != null) {
                summary.getWarnings().forEach(warning -> addWarning(result, warning));
            }
        } catch (Exception exception) {
            log.warn("finance dashboard voucher summary failed, period={}", yearMonth, exception);
            addWarning(result, "凭证汇总加载失败");
        }
    }

    private void loadMonthEnd(FinanceDashboardOverviewVO result, Long forg, String period) {
        try {
            MonthEndWorkbenchResultVO workbench = periodProcessService.monthEndWorkbench(forg, period);
            if (workbench == null) {
                addWarning(result, "月结工作台暂不可用");
                return;
            }
            int total = defaultInt(workbench.getTotalCheckCount());
            int passed = defaultInt(workbench.getPassedCount());
            result.setMonthCloseTotalCount(total);
            result.setMonthClosePassedCount(passed);
            result.setMonthCloseBlockingCount(defaultInt(workbench.getBlockingCount()));
            result.setMonthCloseProgress(total > 0 ? Math.min(100, Math.max(0, passed * 100 / total)) : null);
            result.setCanClose(Boolean.TRUE.equals(workbench.getCanClose()));

            if (workbench.getPeriodVoucherCount() != null) {
                result.setMonthVoucherCount(defaultInt(workbench.getPeriodVoucherCount()));
            }
            if (workbench.getPostedVoucherCount() != null) {
                result.setPostedVoucherCount(defaultInt(workbench.getPostedVoucherCount()));
            }
            if (workbench.getPendingVoucherCount() != null) {
                result.setPendingVoucherCount(defaultInt(workbench.getPendingVoucherCount()));
            }
            if (workbench.getExceptionVoucherCount() != null) {
                result.setExceptionVoucherCount(defaultInt(workbench.getExceptionVoucherCount()));
            }
            if (workbench.getWarnings() != null) {
                workbench.getWarnings().forEach(warning -> addWarning(result, warning));
            }
        } catch (Exception exception) {
            log.warn("finance dashboard month-end data failed, forg={}, period={}", forg, period, exception);
            addWarning(result, "月结数据加载失败");
        }
    }

    private void loadCreditWarnings(FinanceDashboardOverviewVO result) {
        LocalDate asOfDate = LocalDate.now();
        result.setReceivableWarningCount(loadWarningCount(result, "AR", asOfDate, "应收信用预警"));
        result.setPayableWarningCount(loadWarningCount(result, "AP", asOfDate, "应付信用预警"));
    }

    private int loadWarningCount(FinanceDashboardOverviewVO result,
                                 String docTypeRoot,
                                 LocalDate asOfDate,
                                 String label) {
        try {
            List<Map<String, Object>> warnings = arapDocService.creditWarnings(docTypeRoot, asOfDate);
            return warnings == null ? 0 : warnings.size();
        } catch (Exception exception) {
            log.warn("finance dashboard credit warnings failed, docTypeRoot={}, asOfDate={}", docTypeRoot, asOfDate, exception);
            addWarning(result, label + "加载失败");
            return 0;
        }
    }

    private void buildFocusItems(FinanceDashboardOverviewVO result) {
        List<FinanceDashboardOverviewVO.FocusItem> focusItems = new ArrayList<>();
        result.setFocusItems(focusItems);
        addFocusItem(focusItems, "MONTH_CLOSE_BLOCKING", "月结阻塞项", result.getMonthCloseBlockingCount(), "/ledger/month-end-close-workbench");
        addFocusItem(focusItems, "VOUCHER_PENDING", "待处理凭证", result.getPendingVoucherCount(), "/ledger/voucher");
        addFocusItem(focusItems, "AR_CREDIT_WARNING", "应收信用预警", result.getReceivableWarningCount(), "/receivable/aging-credit");
        addFocusItem(focusItems, "AP_CREDIT_WARNING", "应付信用预警", result.getPayableWarningCount(), "/payable/aging-credit");
    }

    private void addFocusItem(List<FinanceDashboardOverviewVO.FocusItem> target,
                              String code,
                              String name,
                              Integer count,
                              String path) {
        int value = defaultInt(count);
        target.add(new FinanceDashboardOverviewVO.FocusItem(
                code,
                name,
                value > 0 ? "待处理" : "正常",
                value,
                path
        ));
    }

    private void addWarning(FinanceDashboardOverviewVO result, String warning) {
        if (warning == null || warning.isBlank()) {
            return;
        }
        result.getWarnings().add(warning);
    }

    private YearMonth resolvePeriod(String period) {
        if (period == null || period.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (DateTimeParseException exception) {
            throw new BizException("期间格式应为 yyyy-MM");
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
