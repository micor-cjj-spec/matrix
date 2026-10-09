package single.cjj.fi.gl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.exception.BizException;
import single.cjj.fi.gl.entity.BizfiFiAccountingPeriod;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCheckBatch;
import single.cjj.fi.gl.entity.BizfiFiMonthEndCloseExecution;
import single.cjj.fi.gl.mapper.BizfiFiAccountingPeriodMapper;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCheckBatchMapper;
import single.cjj.fi.gl.mapper.BizfiFiMonthEndCloseExecutionMapper;
import single.cjj.fi.gl.service.BizfiFiAccountingPeriodService;
import single.cjj.fi.gl.service.BizfiFiMonthEndCheckBatchService;
import single.cjj.fi.gl.service.BizfiFiPeriodProcessService;
import single.cjj.fi.gl.vo.MonthEndBatchActionRequestVO;
import single.cjj.fi.gl.vo.MonthEndBatchCreateRequestVO;
import single.cjj.fi.gl.vo.MonthEndCloseExecuteRequestVO;
import single.cjj.fi.gl.vo.MonthEndCloseExecutionResultVO;
import single.cjj.fi.gl.vo.MonthEndWorkbenchResultVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BizfiFiMonthEndCheckBatchServiceImpl
        extends ServiceImpl<BizfiFiMonthEndCheckBatchMapper, BizfiFiMonthEndCheckBatch>
        implements BizfiFiMonthEndCheckBatchService {

    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String APPROVED = "APPROVED";
    private static final String CLOSED = "CLOSED";
    private static final String SUCCESS = "SUCCESS";
    private static final DateTimeFormatter BATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private BizfiFiPeriodProcessService periodProcessService;

    @Autowired
    private BizfiFiAccountingPeriodService accountingPeriodService;

    @Autowired
    private BizfiFiAccountingPeriodMapper accountingPeriodMapper;

    @Autowired
    private BizfiFiMonthEndCloseExecutionMapper closeExecutionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public BizfiFiMonthEndCheckBatch createBatch(MonthEndBatchCreateRequestVO request) {
        MonthEndBatchCreateRequestVO safeRequest = request == null ? new MonthEndBatchCreateRequestVO() : request;
        MonthEndWorkbenchResultVO snapshot = periodProcessService.monthEndWorkbench(safeRequest.getForg(), safeRequest.getPeriod());
        LocalDateTime now = LocalDateTime.now();

        BizfiFiMonthEndCheckBatch batch = new BizfiFiMonthEndCheckBatch();
        batch.setFbatchNo(buildBatchNo(snapshot, now));
        batch.setForg(snapshot.getForg());
        batch.setFperiod(snapshot.getPeriod());
        batch.setFperiodSource(snapshot.getPeriodSource());
        batch.setFbaseCurrency(snapshot.getBaseCurrency());
        batch.setFcurrentPeriod(snapshot.getCurrentPeriod());
        batch.setFperiodStatus(snapshot.getPeriodStatus());
        batch.setFcloseStatus(snapshot.getCloseStatus());
        batch.setFreadinessScore(defaultInt(snapshot.getReadinessScore()));
        batch.setFcanClose(Boolean.TRUE.equals(snapshot.getCanClose()));
        batch.setFblockingCount(defaultInt(snapshot.getBlockingCount()));
        batch.setFwarningCount(defaultInt(snapshot.getWarningCount()));
        batch.setFpendingCount(defaultInt(snapshot.getPendingCount()));
        batch.setFpassedCount(defaultInt(snapshot.getPassedCount()));
        batch.setFtotalCheckCount(defaultInt(snapshot.getTotalCheckCount()));
        batch.setFperiodVoucherCount(defaultInt(snapshot.getPeriodVoucherCount()));
        batch.setFpostedVoucherCount(defaultInt(snapshot.getPostedVoucherCount()));
        batch.setFpendingVoucherCount(defaultInt(snapshot.getPendingVoucherCount()));
        batch.setFexceptionVoucherCount(defaultInt(snapshot.getExceptionVoucherCount()));
        batch.setFapplicationStatus(DRAFT);
        batch.setFsnapshotJson(toSnapshotJson(snapshot));
        batch.setFremark(trimToNull(safeRequest.getRemark()));
        batch.setFcreatedBy(operatorOrDefault(safeRequest.getCreatedBy()));
        batch.setFcreatedTime(now);
        batch.setFupdateTime(now);
        baseMapper.insert(batch);
        return batch;
    }

    @Override
    public IPage<BizfiFiMonthEndCheckBatch> list(int page, int size, Map<String, Object> query) {
        LambdaQueryWrapper<BizfiFiMonthEndCheckBatch> wrapper = new LambdaQueryWrapper<>();
        if (query != null && query.get("forg") instanceof Number number) {
            wrapper.eq(BizfiFiMonthEndCheckBatch::getForg, number.longValue());
        }
        if (query != null && StringUtils.hasText((String) query.get("period"))) {
            wrapper.eq(BizfiFiMonthEndCheckBatch::getFperiod, query.get("period").toString().trim());
        }
        if (query != null && StringUtils.hasText((String) query.get("applicationStatus"))) {
            wrapper.eq(BizfiFiMonthEndCheckBatch::getFapplicationStatus,
                    query.get("applicationStatus").toString().trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(BizfiFiMonthEndCheckBatch::getFcreatedTime)
                .orderByDesc(BizfiFiMonthEndCheckBatch::getFid);
        return baseMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
    }

    @Override
    public BizfiFiMonthEndCheckBatch get(Long fid) {
        BizfiFiMonthEndCheckBatch batch = baseMapper.selectById(fid);
        if (batch == null) {
            throw new BizException("月结检查批次不存在");
        }
        return batch;
    }

    @Override
    public BizfiFiMonthEndCheckBatch submit(Long fid, MonthEndBatchActionRequestVO request) {
        BizfiFiMonthEndCheckBatch batch = get(fid);
        if (!DRAFT.equals(normalize(batch.getFapplicationStatus()))) {
            throw new BizException("只有草稿状态的月结检查批次可以提交");
        }
        if (defaultInt(batch.getFblockingCount()) > 0) {
            throw new BizException("存在阻塞项的月结检查批次不能提交关账申请");
        }
        LocalDateTime now = LocalDateTime.now();
        batch.setFapplicationStatus(SUBMITTED);
        batch.setFsubmittedBy(operatorOrDefault(request == null ? null : request.getOperator()));
        batch.setFsubmittedTime(now);
        batch.setFupdateTime(now);
        mergeRemark(batch, request == null ? null : request.getRemark());
        baseMapper.updateById(batch);
        return get(fid);
    }

    @Override
    public BizfiFiMonthEndCheckBatch approve(Long fid, MonthEndBatchActionRequestVO request) {
        BizfiFiMonthEndCheckBatch batch = get(fid);
        if (!SUBMITTED.equals(normalize(batch.getFapplicationStatus()))) {
            throw new BizException("只有已提交的月结检查批次可以批准");
        }
        LocalDateTime now = LocalDateTime.now();
        batch.setFapplicationStatus(APPROVED);
        batch.setFapprovedBy(operatorOrDefault(request == null ? null : request.getOperator()));
        batch.setFapprovedTime(now);
        batch.setFupdateTime(now);
        mergeRemark(batch, request == null ? null : request.getRemark());
        baseMapper.updateById(batch);
        return get(fid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthEndCloseExecutionResultVO executeClose(Long fid, MonthEndCloseExecuteRequestVO request) {
        BizfiFiMonthEndCheckBatch batch = get(fid);
        String status = normalize(batch.getFapplicationStatus());
        if (CLOSED.equals(status)) {
            throw new BizException("该月结检查批次已经完成关账执行");
        }
        if (!APPROVED.equals(status)) {
            throw new BizException("只有已批准的月结检查批次可以执行关账");
        }
        if (closeExecutionMapper.selectCount(new LambdaQueryWrapper<BizfiFiMonthEndCloseExecution>()
                .eq(BizfiFiMonthEndCloseExecution::getFbatchId, fid)) > 0) {
            throw new BizException("该月结检查批次已有执行记录，不能重复关账");
        }

        MonthEndWorkbenchResultVO snapshot = periodProcessService.monthEndWorkbench(batch.getForg(), batch.getFperiod());
        if (!Boolean.TRUE.equals(snapshot.getCanClose())) {
            throw new BizException("实时关账前检查未通过，不能执行关账");
        }

        BizfiFiAccountingPeriod period = loadAccountingPeriod(batch.getForg(), batch.getFperiod());
        if (period == null) {
            throw new BizException("未找到会计期间档案，无法执行关账");
        }
        String beforeStatus = normalize(period.getFstatus());
        if (!"OPEN".equals(beforeStatus)) {
            throw new BizException("只有OPEN状态的会计期间可以执行关账");
        }

        String operator = operatorOrDefault(request == null ? null : request.getOperator());
        BizfiFiAccountingPeriod closedPeriod = accountingPeriodService.close(period.getFid(), operator);
        LocalDateTime now = LocalDateTime.now();

        BizfiFiMonthEndCloseExecution execution = new BizfiFiMonthEndCloseExecution();
        execution.setFexecutionNo(buildExecutionNo(now));
        execution.setFbatchId(batch.getFid());
        execution.setFbatchNo(batch.getFbatchNo());
        execution.setForg(batch.getForg());
        execution.setFperiod(batch.getFperiod());
        execution.setFperiodId(period.getFid());
        execution.setFbeforeStatus(beforeStatus);
        execution.setFafterStatus(normalize(closedPeriod.getFstatus()));
        execution.setFexecutionStatus(SUCCESS);
        execution.setFcheckSnapshotJson(toSnapshotJson(snapshot));
        execution.setFoperator(operator);
        execution.setFremark(trimToNull(request == null ? null : request.getRemark()));
        execution.setFexecutedTime(now);
        execution.setFcreatedTime(now);
        closeExecutionMapper.insert(execution);

        batch.setFapplicationStatus(CLOSED);
        batch.setFperiodStatus(closedPeriod.getFstatus());
        batch.setFcloseStatus(CLOSED);
        batch.setFupdateTime(now);
        mergeRemark(batch, request == null ? null : request.getRemark());
        baseMapper.updateById(batch);

        return new MonthEndCloseExecutionResultVO(execution, get(fid), closedPeriod, snapshot);
    }

    private String buildBatchNo(MonthEndWorkbenchResultVO snapshot, LocalDateTime time) {
        String orgPart = snapshot.getForg() == null ? "ALL" : snapshot.getForg().toString();
        String periodPart = StringUtils.hasText(snapshot.getPeriod()) ? snapshot.getPeriod().replace("-", "") : "NOPERIOD";
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "MEC-" + BATCH_TIME_FORMATTER.format(time) + "-" + randomPart + "-" + orgPart + "-" + periodPart;
    }

    private String buildExecutionNo(LocalDateTime time) {
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "MECLOSE-" + BATCH_TIME_FORMATTER.format(time) + "-" + randomPart;
    }

    private String toSnapshotJson(MonthEndWorkbenchResultVO snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new BizException("月结检查快照序列化失败");
        }
    }

    private void mergeRemark(BizfiFiMonthEndCheckBatch batch, String remark) {
        if (!StringUtils.hasText(remark)) {
            return;
        }
        String trimmed = remark.trim();
        if (!StringUtils.hasText(batch.getFremark())) {
            batch.setFremark(trimmed);
        } else if (!batch.getFremark().contains(trimmed)) {
            batch.setFremark(batch.getFremark() + "；" + trimmed);
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

    private String operatorOrDefault(String operator) {
        return StringUtils.hasText(operator) ? operator.trim() : "SYSTEM";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
