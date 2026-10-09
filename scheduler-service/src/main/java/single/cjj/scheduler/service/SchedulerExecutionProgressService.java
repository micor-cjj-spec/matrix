package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.scheduler.dto.ExecutionProgressRequest;
import single.cjj.scheduler.entity.MatrixSchedulerExecution;
import single.cjj.scheduler.mapper.MatrixSchedulerExecutionMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerExecutionProgressService {

    private final MatrixSchedulerExecutionMapper executionMapper;

    public SchedulerExecutionProgressService(MatrixSchedulerExecutionMapper executionMapper) {
        this.executionMapper = executionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerExecution report(String executionNo,
                                           String executorCode,
                                           ExecutionProgressRequest request) {
        MatrixSchedulerExecution execution = requiredExecution(executionNo);
        if (!executorCode.equals(execution.getFexecutorCode())) {
            throw new IllegalArgumentException("执行器无权上报该执行实例进度");
        }
        if (!List.of("QUEUED", "RUNNING").contains(execution.getFstatus())) {
            return execution;
        }

        int progress = request.progress() == null ? 0 : request.progress();
        int current = execution.getFprogress() == null ? 0 : execution.getFprogress();
        if (progress < current) {
            return execution;
        }

        LocalDateTime now = LocalDateTime.now();
        execution.setFprogress(progress);
        execution.setFcurrentStage(trim(request.stage(), 64));
        execution.setFprogressMessage(trim(request.message(), 500));
        execution.setFexecutorInstance(request.executorInstance());
        execution.setFlastProgressTime(now);
        execution.setFupdateTime(now);
        executionMapper.updateById(execution);
        return execution;
    }

    private MatrixSchedulerExecution requiredExecution(String executionNo) {
        MatrixSchedulerExecution execution = executionMapper.selectOne(
                new LambdaQueryWrapper<MatrixSchedulerExecution>()
                        .eq(MatrixSchedulerExecution::getFexecutionNo, executionNo)
                        .last("LIMIT 1"));
        if (execution == null) {
            throw new IllegalArgumentException("执行实例不存在");
        }
        return execution;
    }

    private String trim(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
