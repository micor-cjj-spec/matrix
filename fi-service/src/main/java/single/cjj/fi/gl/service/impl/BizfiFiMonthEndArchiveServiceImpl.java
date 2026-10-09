package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.entity.BizfiFiPeriodRollover;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCheckBatchMapper;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCloseExecutionMapper;
import single.cjj.fi.gl.mapper.BizfiFiPeriodRolloverMapper;
import single.cjj.fi.gl.service.BizfiFiMonthEndArchiveService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndArchiveMilestoneVO;
import single.cjj.fi.gl.vo.MonthEndArchivePackageVO;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BizfiFiMonthEndArchiveServiceImpl implements BizfiFiMonthEndArchiveService {

    private static final String DONE = "DONE";
    private static final String PENDING = "PENDING";

    @Autowired
    private BizfiFiMonthEndCheckBatchMapper checkBatchMapper;

    @Autowired
    private BizfiFiMonthEndCloseExecutionMapper closeExecutionMapper;

    @Autowired
    private BizfiFiPeriodRolloverMapper periodRolloverMapper;

    @Autowired
    private BizfiFiPeriodProcessService periodProcessService;

    @Override
    public MonthEndArchivePackageVO getPackage(Long forg, String period) {
        String resolvedPeriod = resolvePeriod(period);
        MonthEndArchivePackageVO result = new MonthEndArchivePackageVO();
        result.setForg(forg);
        result.setPeriod(resolvedPeriod);
        result.setGeneratedAt(LocalDateTime.now());

        BizfiFiMonthEndCheckBatch batch = loadLatestBatch(forg, resolvedPeriod);
        BizfiFiMonthEndCloseExecution execution = loadLatestCloseExecution(forg, resolvedPeriod);
        BizfiFiPeriodRollover rollover = loadLatestRollover(forg, resolvedPeriod);
        MonthEndWorkbenchResultVO workbench = loadWorkbenchQuietly(forg, resolvedPeriod, result.getWarnings());

        result.setCheckBatch(batch);
        result.setCloseExecution(execution);
        result.setPeriodRollover(rollover);
        result.setWorkbench(workbench);
        applyMetrics(result, batch, workbench);
        result.setCloseExecuted(execution != null);
        result.setPeriodRolled(rollover != null);
        result.setMilestones(buildMilestones(batch, execution, rollover));
        applyArchiveStatusAndConclusion(result, batch, execution, rollover);
        appendWarnings(result, batch, workbench);
        result.setHasWarnings(!result.getWarnings().isEmpty());
        return result;
    }

    private BizfiFiMonthEndCheckBatch loadLatestBatch(Long forg, String period) {
        LambdaQueryWrapper<BizfiFiMonthEndCheckBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(period), BizfiFiMonthEndCheckBatch::getFperiod, period);
        wrapper.eq(forg != null, BizfiFiMonthEndCheckBatch::getForg, forg);
        wrapper.orderByDesc(BizfiFiMonthEndCheckBatch::getFcreatedTime)
                .orderByDesc(BizfiFiMonthEndCheckBatch::getFid)
                .last("limit 1");
        return checkBatchMapper.selectOne(wrapper);
    }

    private BizfiFiMonthEndCloseExecution loadLatestCloseExecution(Long forg, String period) {
        LambdaQueryWrapper<BizfiFiMonthEndCloseExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(period), BizfiFiMonthEndCloseExecution::getFperiod, period);
        wrapper.eq(forg != null, BizfiFiMonthEndCloseExecution::getForg, forg);
        wrapper.orderByDesc(BizfiFiMonthEndCloseExecution::getFexecutedTime)
                .orderByDesc(BizfiFiMonthEndCloseExecution::getFid)
                .last("limit 1");
        return closeExecutionMapper.selectOne(wrapper);
    }

    private BizfiFiPeriodRollover loadLatestRollover(Long forg, String period) {
        LambdaQueryWrapper<BizfiFiPeriodRollover> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(period), BizfiFiPeriodRollover::getFfromPeriod, period);
        wrapper.eq(forg != null, BizfiFiPeriodRollover::getForg, forg);
        wrapper.orderByDesc(BizfiFiPeriodRollover::getFrolledTime)
                .orderByDesc(BizfiFiPeriodRollover::getFid)
                .last("limit 1");
        return periodRolloverMapper.selectOne(wrapper);
    }

    private MonthEndWorkbenchResultVO loadWorkbenchQuietly(Long forg, String period, List<String> warnings) {
        try {
            return periodProcessService.monthEndWorkbench(forg, period);
        } catch (Exception ex) {
            warnings.add("实时月结工作台检查结果暂不可用。");
            return null;
        }
    }

    private void applyMetrics(MonthEndArchivePackageVO result,
                              BizfiFiMonthEndCheckBatch batch,
                              MonthEndWorkbenchResultVO workbench) {
        result.setReadinessScore(firstInt(batch == null ? null : batch.getFreadinessScore(),
                workbench == null ? null : workbench.getReadinessScore()));
        result.setBlockingCount(firstInt(batch == null ? null : batch.getFblockingCount(),
                workbench == null ? null : workbench.getBlockingCount()));
        result.setWarningCount(firstInt(batch == null ? null : batch.getFwarningCount(),
                workbench == null ? null : workbench.getWarningCount()));
        result.setPendingCount(firstInt(batch == null ? null : batch.getFpendingCount(),
                workbench == null ? null : workbench.getPendingCount()));
        result.setPassedCount(firstInt(batch == null ? null : batch.getFpassedCount(),
                workbench == null ? null : workbench.getPassedCount()));
        result.setTotalCheckCount(firstInt(batch == null ? null : batch.getFtotalCheckCount(),
                workbench == null ? null : workbench.getTotalCheckCount()));
        result.setPeriodVoucherCount(firstInt(batch == null ? null : batch.getFperiodVoucherCount(),
                workbench == null ? null : workbench.getPeriodVoucherCount()));
        result.setPostedVoucherCount(firstInt(batch == null ? null : batch.getFpostedVoucherCount(),
                workbench == null ? null : workbench.getPostedVoucherCount()));
        result.setPendingVoucherCount(firstInt(batch == null ? null : batch.getFpendingVoucherCount(),
                workbench == null ? null : workbench.getPendingVoucherCount()));
        result.setExceptionVoucherCount(firstInt(batch == null ? null : batch.getFexceptionVoucherCount(),
                workbench == null ? null : workbench.getExceptionVoucherCount()));
    }

    private List<MonthEndArchiveMilestoneVO> buildMilestones(BizfiFiMonthEndCheckBatch batch,
                                                             BizfiFiMonthEndCloseExecution execution,
                                                             BizfiFiPeriodRollover rollover) {
        List<MonthEndArchiveMilestoneVO> rows = new ArrayList<>();
        rows.add(new MonthEndArchiveMilestoneVO(
                "CHECK_BATCH",
                "生成检查批次",
                batch == null ? PENDING : DONE,
                batch == null ? null : batch.getFcreatedTime(),
                batch == null ? null : batch.getFcreatedBy(),
                batch == null ? "尚未生成月结检查批次。" : "检查批次 " + safe(batch.getFbatchNo()) + " 已生成。"
        ));
        rows.add(new MonthEndArchiveMilestoneVO(
                "SUBMIT_APPLICATION",
                "提交关账申请",
                batch != null && batch.getFsubmittedTime() != null ? DONE : PENDING,
                batch == null ? null : batch.getFsubmittedTime(),
                batch == null ? null : batch.getFsubmittedBy(),
                batch != null && batch.getFsubmittedTime() != null ? "关账申请已提交。" : "尚未提交关账申请。"
        ));
        rows.add(new MonthEndArchiveMilestoneVO(
                "APPROVE_APPLICATION",
                "批准关账申请",
                batch != null && batch.getFapprovedTime() != null ? DONE : PENDING,
                batch == null ? null : batch.getFapprovedTime(),
                batch == null ? null : batch.getFapprovedBy(),
                batch != null && batch.getFapprovedTime() != null ? "关账申请已批准。" : "尚未批准关账申请。"
        ));
        rows.add(new MonthEndArchiveMilestoneVO(
                "CLOSE_EXECUTION",
                "执行关账",
                execution == null ? PENDING : DONE,
                execution == null ? null : execution.getFexecutedTime(),
                execution == null ? null : execution.getFoperator(),
                execution == null ? "尚未执行正式关账。" : "关账执行 " + safe(execution.getFexecutionNo()) + " 已完成。"
        ));
        rows.add(new MonthEndArchiveMilestoneVO(
                "PERIOD_ROLLOVER",
                "启用下一期间",
                rollover == null ? PENDING : DONE,
                rollover == null ? null : rollover.getFrolledTime(),
                rollover == null ? null : rollover.getFoperator(),
                rollover == null ? "尚未启用下一期间。" : "已启用下一期间 " + safe(rollover.getFtoPeriod()) + "。"
        ));
        return rows;
    }

    private void applyArchiveStatusAndConclusion(MonthEndArchivePackageVO result,
                                                 BizfiFiMonthEndCheckBatch batch,
                                                 BizfiFiMonthEndCloseExecution execution,
                                                 BizfiFiPeriodRollover rollover) {
        if (rollover != null) {
            result.setArchiveStatus("ROLLED");
            result.setConclusion("本期已完成关账并启用下一期间 " + safe(rollover.getFtoPeriod()) + "。");
            return;
        }
        if (execution != null) {
            result.setArchiveStatus("CLOSED");
            result.setConclusion("本期已完成正式关账，尚未启用下一期间。");
            return;
        }
        if (batch == null) {
            result.setArchiveStatus("NOT_STARTED");
            result.setConclusion("本期尚未形成月结检查批次。");
            return;
        }
        if (defaultInt(batch.getFblockingCount()) > 0) {
            result.setArchiveStatus("BLOCKED");
            result.setConclusion("本期存在阻塞项，暂不能归档关账。");
            return;
        }
        if ("APPROVED".equals(normalize(batch.getFapplicationStatus()))) {
            result.setArchiveStatus("APPROVED_PENDING_CLOSE");
            result.setConclusion("本期关账申请已批准，等待执行正式关账。");
            return;
        }
        result.setArchiveStatus("CHECKED");
        result.setConclusion("本期已生成检查批次，仍需完成提交、批准和关账执行。");
    }

    private void appendWarnings(MonthEndArchivePackageVO result,
                                BizfiFiMonthEndCheckBatch batch,
                                MonthEndWorkbenchResultVO workbench) {
        if (defaultInt(result.getBlockingCount()) > 0) {
            result.getWarnings().add("本期仍存在 " + result.getBlockingCount() + " 个阻塞项。");
        }
        if (defaultInt(result.getWarningCount()) > 0) {
            result.getWarnings().add("本期仍存在 " + result.getWarningCount() + " 个预警项。");
        }
        if (batch == null) {
            result.getWarnings().add("尚未生成月结检查批次。");
        }
        if (Boolean.FALSE.equals(result.getCloseExecuted())) {
            result.getWarnings().add("尚未执行正式关账。");
        }
        if (workbench != null && workbench.getWarnings() != null) {
            workbench.getWarnings().stream()
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .forEach(result.getWarnings()::add);
        }
    }

    private String resolvePeriod(String period) {
        YearMonth yearMonth = parsePeriod(period);
        return yearMonth == null ? YearMonth.now().toString() : yearMonth.toString();
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

    private Integer firstInt(Integer first, Integer second) {
        return first != null ? first : defaultInt(second);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
