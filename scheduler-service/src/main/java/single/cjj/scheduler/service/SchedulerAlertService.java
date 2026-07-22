package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.entity.MatrixSchedulerExecutor;
import single.cjj.scheduler.mapper.MatrixSchedulerAlertRecordMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutorMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerAlertService {

    private final MatrixSchedulerAlertRecordMapper alertMapper;
    private final MatrixSchedulerExecutionMapper executionMapper;
    private final MatrixSchedulerExecutorMapper executorMapper;

    public SchedulerAlertService(MatrixSchedulerAlertRecordMapper alertMapper,
                                 MatrixSchedulerExecutionMapper executionMapper,
                                 MatrixSchedulerExecutorMapper executorMapper) {
        this.alertMapper = alertMapper;
        this.executionMapper = executionMapper;
        this.executorMapper = executorMapper;
    }

    @Scheduled(fixedDelayString = "${matrix.scheduler.alert.scan-ms:30000}")
    public void scan() {
        scanExecutionFailures();
        scanOfflineExecutors();
    }

    public IPage<MatrixSchedulerAlertRecord> list(int page, int size, String status, String level) {
        return alertMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MatrixSchedulerAlertRecord>()
                        .eq(StringUtils.hasText(status), MatrixSchedulerAlertRecord::getFstatus, status)
                        .eq(StringUtils.hasText(level), MatrixSchedulerAlertRecord::getFlevel, level)
                        .orderByDesc(MatrixSchedulerAlertRecord::getFcreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerAlertRecord acknowledge(Long alertId, String operatorId) {
        MatrixSchedulerAlertRecord alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("告警记录不存在");
        }
        if (!"ACKED".equals(alert.getFstatus())) {
            LocalDateTime now = LocalDateTime.now();
            alert.setFstatus("ACKED");
            alert.setFackBy(StringUtils.hasText(operatorId) ? operatorId : "system");
            alert.setFackTime(now);
            alert.setFupdateTime(now);
            alertMapper.updateById(alert);
        }
        return alert;
    }

    private void scanExecutionFailures() {
        List<MatrixSchedulerExecution> failures = executionMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .in(MatrixSchedulerExecution::getFstatus, "FAILED", "TIMEOUT", "DEAD")
                        .ge(MatrixSchedulerExecution::getFupdateTime, LocalDateTime.now().minusDays(7))
                        .orderByDesc(MatrixSchedulerExecution::getFupdateTime)
                        .last("LIMIT 500"));
        for (MatrixSchedulerExecution execution : failures) {
            String level = "DEAD".equals(execution.getFstatus()) ? "CRITICAL" : "ERROR";
            String key = "EXECUTION:" + execution.getFexecutionNo() + ":" + execution.getFstatus();
            createIfAbsent(key,
                    execution.getFexecutionNo(),
                    execution.getFjobId(),
                    execution.getFexecutorCode(),
                    "EXECUTION_" + execution.getFstatus(),
                    level,
                    "调度任务执行" + execution.getFstatus(),
                    buildExecutionContent(execution));
        }
    }

    private void scanOfflineExecutors() {
        List<MatrixSchedulerExecutor> executors = executorMapper.selectList(
                new LambdaQueryWrapper<MatrixSchedulerExecutor>()
                        .eq(MatrixSchedulerExecutor::getFstatus, "OFFLINE"));
        for (MatrixSchedulerExecutor executor : executors) {
            String key = "EXECUTOR:" + executor.getFexecutorCode() + ":OFFLINE:"
                    + String.valueOf(executor.getFlastHeartbeatTime());
            createIfAbsent(key,
                    null,
                    null,
                    executor.getFexecutorCode(),
                    "EXECUTOR_OFFLINE",
                    "WARN",
                    "执行器离线：" + executor.getFexecutorCode(),
                    "执行器最后心跳时间：" + executor.getFlastHeartbeatTime());
        }
    }

    private void createIfAbsent(String dedupeKey,
                                String executionNo,
                                Long jobId,
                                String executorCode,
                                String alertType,
                                String level,
                                String title,
                                String content) {
        Long count = alertMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerAlertRecord>()
                .eq(MatrixSchedulerAlertRecord::getFdedupeKey, dedupeKey));
        if (count != null && count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MatrixSchedulerAlertRecord alert = new MatrixSchedulerAlertRecord();
        alert.setFid(IdWorker.getId());
        alert.setFdedupeKey(dedupeKey);
        alert.setFexecutionNo(executionNo);
        alert.setFjobId(jobId);
        alert.setFexecutorCode(executorCode);
        alert.setFalertType(alertType);
        alert.setFlevel(level);
        alert.setFtitle(trim(title, 200));
        alert.setFcontent(trim(content, 2000));
        alert.setFstatus("PENDING");
        alert.setFcreateTime(now);
        alert.setFupdateTime(now);
        try {
            alertMapper.insert(alert);
        } catch (DuplicateKeyException ignored) {
            // 多实例扫描时由唯一键完成最终去重。
        }
    }

    private String buildExecutionContent(MatrixSchedulerExecution execution) {
        return "executionNo=" + execution.getFexecutionNo()
                + ", jobCode=" + execution.getFjobCode()
                + ", executor=" + execution.getFexecutorCode()
                + ", handler=" + execution.getFhandlerCode()
                + ", attempt=" + execution.getFattemptNo()
                + ", errorCode=" + execution.getFerrorCode()
                + ", errorMessage=" + execution.getFerrorMessage();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
