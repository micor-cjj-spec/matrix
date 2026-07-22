package single.cjj.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.scheduler.dto.SchedulerJobRequest;
import single.cjj.scheduler.entity.MatrixSchedulerJob;
import single.cjj.scheduler.mapper.MatrixSchedulerJobMapper;
import single.cjj.scheduler.quartz.QuartzJobManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SchedulerJobService {

    private final MatrixSchedulerJobMapper jobMapper;
    private final QuartzJobManager quartzJobManager;

    public SchedulerJobService(MatrixSchedulerJobMapper jobMapper,
                               QuartzJobManager quartzJobManager) {
        this.jobMapper = jobMapper;
        this.quartzJobManager = quartzJobManager;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerJob create(SchedulerJobRequest request,
                                     String sourceType,
                                     String sourceService,
                                     String idempotencyKey) {
        validate(request);
        if (StringUtils.hasText(sourceService) && StringUtils.hasText(idempotencyKey)) {
            MatrixSchedulerJob existing = jobMapper.selectOne(new LambdaQueryWrapper<MatrixSchedulerJob>()
                    .eq(MatrixSchedulerJob::getFsourceService, sourceService)
                    .eq(MatrixSchedulerJob::getFidempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }

        MatrixSchedulerJob job = new MatrixSchedulerJob();
        job.setFid(IdWorker.getId());
        job.setFjobCode(StringUtils.hasText(request.getJobCode())
                ? request.getJobCode().trim()
                : "JOB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        ensureJobCodeUnique(job.getFjobCode(), null);
        apply(job, request);
        job.setFsourceType(defaultText(sourceType, "PLATFORM"));
        job.setFsourceService(defaultText(sourceService, "matrix-web"));
        job.setFidempotencyKey(idempotencyKey);
        job.setFversion(0);
        job.setFcreateTime(LocalDateTime.now());
        job.setFupdateTime(LocalDateTime.now());
        jobMapper.insert(job);
        quartzJobManager.upsert(job);
        return job;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerJob update(Long jobId, SchedulerJobRequest request) {
        validate(request);
        MatrixSchedulerJob job = mustGet(jobId);
        if (StringUtils.hasText(request.getJobCode())) {
            ensureJobCodeUnique(request.getJobCode(), jobId);
            job.setFjobCode(request.getJobCode().trim());
        }
        apply(job, request);
        job.setFversion(job.getFversion() == null ? 1 : job.getFversion() + 1);
        job.setFupdateTime(LocalDateTime.now());
        jobMapper.updateById(job);
        quartzJobManager.upsert(job);
        return jobMapper.selectById(jobId);
    }

    public MatrixSchedulerJob get(Long jobId) {
        return mustGet(jobId);
    }

    public IPage<MatrixSchedulerJob> list(int page, int size, String keyword,
                                          String status, String sourceService) {
        LambdaQueryWrapper<MatrixSchedulerJob> wrapper = new LambdaQueryWrapper<MatrixSchedulerJob>()
                .ne(MatrixSchedulerJob::getFstatus, "DELETED")
                .and(StringUtils.hasText(keyword), w -> w
                        .like(MatrixSchedulerJob::getFjobCode, keyword)
                        .or()
                        .like(MatrixSchedulerJob::getFjobName, keyword))
                .eq(StringUtils.hasText(status), MatrixSchedulerJob::getFstatus, status)
                .eq(StringUtils.hasText(sourceService), MatrixSchedulerJob::getFsourceService, sourceService)
                .orderByDesc(MatrixSchedulerJob::getFupdateTime);
        return jobMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerJob pause(Long jobId) {
        MatrixSchedulerJob job = mustGet(jobId);
        job.setFstatus("PAUSED");
        touch(job);
        jobMapper.updateById(job);
        quartzJobManager.pause(jobId);
        return job;
    }

    @Transactional(rollbackFor = Exception.class)
    public MatrixSchedulerJob resume(Long jobId) {
        MatrixSchedulerJob job = mustGet(jobId);
        job.setFstatus("ENABLED");
        job.setFnextFireTime(quartzJobManager.nextFireTime(job.getFcronExpression(), job.getFtimezone()));
        touch(job);
        jobMapper.updateById(job);
        quartzJobManager.resume(jobId);
        return job;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long jobId) {
        MatrixSchedulerJob job = mustGet(jobId);
        job.setFstatus("DELETED");
        touch(job);
        jobMapper.updateById(job);
        quartzJobManager.delete(jobId);
        return true;
    }

    public Boolean runNow(Long jobId) {
        mustGet(jobId);
        quartzJobManager.runNow(jobId);
        return true;
    }

    public List<LocalDateTime> preview(String cronExpression, String timezone, Integer count) {
        int safeCount = count == null ? 5 : Math.max(1, Math.min(count, 20));
        return quartzJobManager.preview(cronExpression, defaultText(timezone, "Asia/Shanghai"), safeCount);
    }

    private void apply(MatrixSchedulerJob job, SchedulerJobRequest request) {
        job.setFjobName(request.getJobName().trim());
        job.setFtenantId(defaultText(request.getTenantId(), "default"));
        job.setFscheduleType("CRON");
        job.setFcronExpression(request.getCronExpression().trim());
        job.setFtimezone(defaultText(request.getTimezone(), "Asia/Shanghai"));
        job.setFexecuteType(defaultText(request.getExecuteType(), "MQ").toUpperCase());
        job.setFexecutorCode(request.getExecutorCode().trim());
        job.setFhandlerCode(request.getHandlerCode().trim());
        job.setFexecuteParameters(defaultText(request.getExecuteParameters(), "{}"));
        job.setFstatus(Boolean.FALSE.equals(request.getEnabled()) ? "PAUSED" : "ENABLED");
        job.setFconcurrencyPolicy(defaultText(request.getConcurrencyPolicy(), "SKIP").toUpperCase());
        job.setFmisfirePolicy(defaultText(request.getMisfirePolicy(), "FIRE_ONCE_NOW").toUpperCase());
        job.setFtimeoutSeconds(request.getTimeoutSeconds() == null ? 300 : request.getTimeoutSeconds());
        job.setFretryCount(request.getRetryCount() == null ? 0 : Math.max(0, request.getRetryCount()));
        job.setFretryIntervalSeconds(request.getRetryIntervalSeconds() == null
                ? 60 : Math.max(1, request.getRetryIntervalSeconds()));
        job.setFnextFireTime(quartzJobManager.nextFireTime(job.getFcronExpression(), job.getFtimezone()));
    }

    private void validate(SchedulerJobRequest request) {
        if (!CronExpression.isValidExpression(request.getCronExpression())) {
            throw new IllegalArgumentException("Cron 表达式无效");
        }
        String policy = defaultText(request.getConcurrencyPolicy(), "SKIP").toUpperCase();
        if (!List.of("SKIP", "SERIAL", "PARALLEL").contains(policy)) {
            throw new IllegalArgumentException("不支持的并发策略: " + policy);
        }
        String misfire = defaultText(request.getMisfirePolicy(), "FIRE_ONCE_NOW").toUpperCase();
        if (!List.of("DO_NOTHING", "FIRE_ONCE_NOW", "FIRE_ALL").contains(misfire)) {
            throw new IllegalArgumentException("不支持的 Misfire 策略: " + misfire);
        }
    }

    private void ensureJobCodeUnique(String jobCode, Long excludeId) {
        Long count = jobMapper.selectCount(new LambdaQueryWrapper<MatrixSchedulerJob>()
                .eq(MatrixSchedulerJob::getFjobCode, jobCode)
                .ne(excludeId != null, MatrixSchedulerJob::getFid, excludeId));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("任务编码已存在: " + jobCode);
        }
    }

    private MatrixSchedulerJob mustGet(Long jobId) {
        MatrixSchedulerJob job = jobMapper.selectById(jobId);
        if (job == null || "DELETED".equals(job.getFstatus())) {
            throw new IllegalArgumentException("调度任务不存在");
        }
        return job;
    }

    private void touch(MatrixSchedulerJob job) {
        job.setFversion(job.getFversion() == null ? 1 : job.getFversion() + 1);
        job.setFupdateTime(LocalDateTime.now());
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
