package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.KnowledgeIngestionProperties;
import single.cjj.bizfi.ai.dto.AiKnowledgeIndexJobResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeIndexJob;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeIndexJobMapper;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiKnowledgeIndexJobService {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String PARTIAL = "PARTIAL";
    public static final String SKIPPED = "SKIPPED";
    public static final String FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeIndexJobService.class);

    private final BizfiAiKnowledgeIndexJobMapper jobMapper;
    private final BizfiAiKnowledgeDocMapper docMapper;
    private final AiKnowledgeVectorIndexService vectorIndexService;
    private final KnowledgeIngestionProperties properties;

    public AiKnowledgeIndexJobService(
            BizfiAiKnowledgeIndexJobMapper jobMapper,
            BizfiAiKnowledgeDocMapper docMapper,
            AiKnowledgeVectorIndexService vectorIndexService,
            KnowledgeIngestionProperties properties
    ) {
        this.jobMapper = jobMapper;
        this.docMapper = docMapper;
        this.vectorIndexService = vectorIndexService;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeIndexJobResponse createJob(
            String kbId,
            String docId,
            String fileName,
            String mediaType,
            long fileSize,
            String contentHash
    ) {
        requireEnabled();
        if (!StringUtils.hasText(docId)) {
            throw new BizException("知识文档编号不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeIndexJob job = new BizfiAiKnowledgeIndexJob();
        job.setFjobid("idx_" + UUID.randomUUID().toString().replace("-", ""));
        job.setFkbid(normalize(kbId, "default"));
        job.setFdocid(docId.trim());
        job.setFfilename(limit(normalize(fileName, docId), 255));
        job.setFmediatype(limit(normalize(mediaType, "application/octet-stream"), 128));
        job.setFfilesize(Math.max(0L, fileSize));
        job.setFcontenthash(limit(normalize(contentHash, "manual"), 64));
        job.setFstatus(PENDING);
        job.setFattempts(0);
        job.setFmaxattempts(resolveMaxAttempts());
        job.setFnextretrytime(now);
        job.setFcreatetime(now);
        job.setFmodifytime(now);
        jobMapper.insert(job);
        return toResponse(job);
    }

    public List<AiKnowledgeIndexJobResponse> listJobs(String docId, String status, Integer limit) {
        if (!isEnabled()) {
            return List.of();
        }
        LambdaQueryWrapper<BizfiAiKnowledgeIndexJob> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(docId)) {
            wrapper.eq(BizfiAiKnowledgeIndexJob::getFdocid, docId.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizfiAiKnowledgeIndexJob::getFstatus, status.trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(BizfiAiKnowledgeIndexJob::getFcreatetime)
                .orderByDesc(BizfiAiKnowledgeIndexJob::getFid)
                .last("limit " + normalizeLimit(limit));
        return jobMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeIndexJobResponse retry(String jobId) {
        requireEnabled();
        BizfiAiKnowledgeIndexJob job = requireJob(jobId);
        if (RUNNING.equals(job.getFstatus())) {
            throw new BizException("索引任务正在执行，不能重复提交");
        }
        LocalDateTime now = LocalDateTime.now();
        job.setFstatus(PENDING);
        job.setFattempts(0);
        job.setFerrormessage(null);
        job.setFnextretrytime(now);
        job.setFstarttime(null);
        job.setFfinishtime(null);
        job.setFmodifytime(now);
        jobMapper.updateById(job);
        return toResponse(job);
    }

    public AiKnowledgeIndexJobResponse createReindexJob(String docId) {
        requireEnabled();
        BizfiAiKnowledgeDoc doc = docMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                .eq(BizfiAiKnowledgeDoc::getFdocid, docId)
                .last("limit 1"));
        if (doc == null) {
            throw new BizException("知识文档不存在");
        }
        return createJob(
                doc.getFkbid(),
                doc.getFdocid(),
                StringUtils.hasText(doc.getFsourcepath()) ? doc.getFsourcepath() : doc.getFtitle(),
                "text/plain",
                doc.getFcontent() == null ? 0 : doc.getFcontent().length(),
                "manual"
        );
    }

    @Scheduled(fixedDelayString = "${bizfi.ai.knowledge-ingestion.poll-delay-ms:5000}")
    public void dispatchPendingJobs() {
        if (!isEnabled()) {
            return;
        }
        recoverStaleJobs();
        LocalDateTime now = LocalDateTime.now();
        List<BizfiAiKnowledgeIndexJob> candidates = jobMapper.selectList(
                new LambdaQueryWrapper<BizfiAiKnowledgeIndexJob>()
                        .eq(BizfiAiKnowledgeIndexJob::getFstatus, PENDING)
                        .and(query -> query.isNull(BizfiAiKnowledgeIndexJob::getFnextretrytime)
                                .or().le(BizfiAiKnowledgeIndexJob::getFnextretrytime, now))
                        .orderByAsc(BizfiAiKnowledgeIndexJob::getFid)
                        .last("limit " + resolveBatchSize())
        );
        for (BizfiAiKnowledgeIndexJob candidate : candidates) {
            BizfiAiKnowledgeIndexJob claimed = claim(candidate, now);
            if (claimed != null) {
                execute(claimed);
            }
        }
    }

    private BizfiAiKnowledgeIndexJob claim(BizfiAiKnowledgeIndexJob candidate, LocalDateTime now) {
        int nextAttempt = (candidate.getFattempts() == null ? 0 : candidate.getFattempts()) + 1;
        BizfiAiKnowledgeIndexJob update = new BizfiAiKnowledgeIndexJob();
        update.setFstatus(RUNNING);
        update.setFattempts(nextAttempt);
        update.setFstarttime(now);
        update.setFmodifytime(now);
        int affected = jobMapper.update(update, new LambdaUpdateWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFid, candidate.getFid())
                .eq(BizfiAiKnowledgeIndexJob::getFstatus, PENDING));
        if (affected == 0) {
            return null;
        }
        candidate.setFstatus(RUNNING);
        candidate.setFattempts(nextAttempt);
        candidate.setFstarttime(now);
        candidate.setFmodifytime(now);
        return candidate;
    }

    private void execute(BizfiAiKnowledgeIndexJob job) {
        try {
            AiKnowledgeVectorIndexService.IndexResult result = vectorIndexService.indexDocumentIfEnabled(job.getFdocid());
            String resultStatus = result == null ? FAILED : result.status();
            switch (resultStatus) {
                case "INDEXED", "EMPTY" -> complete(job, SUCCEEDED, null);
                case "PARTIAL" -> complete(job, PARTIAL, "PGVector 写入部分失败，关键词检索仍可用");
                case "DISABLED" -> complete(job, SKIPPED, "语义检索未启用，已保留关键词索引");
                case "FAILED" -> failOrRetry(job, new IllegalStateException("Embedding 索引失败"));
                default -> failOrRetry(job, new IllegalStateException("未知索引状态: " + resultStatus));
            }
        } catch (RuntimeException failure) {
            failOrRetry(job, failure);
        }
    }

    private void complete(BizfiAiKnowledgeIndexJob job, String status, String message) {
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeIndexJob update = new BizfiAiKnowledgeIndexJob();
        update.setFstatus(status);
        update.setFerrormessage(limitNullable(message, 1000));
        update.setFnextretrytime(null);
        update.setFfinishtime(now);
        update.setFmodifytime(now);
        jobMapper.update(update, new LambdaUpdateWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFid, job.getFid())
                .eq(BizfiAiKnowledgeIndexJob::getFstatus, RUNNING));
    }

    private void failOrRetry(BizfiAiKnowledgeIndexJob job, RuntimeException failure) {
        int attempts = job.getFattempts() == null ? 1 : job.getFattempts();
        int maxAttempts = job.getFmaxattempts() == null ? resolveMaxAttempts() : job.getFmaxattempts();
        boolean retry = attempts < maxAttempts;
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeIndexJob update = new BizfiAiKnowledgeIndexJob();
        update.setFstatus(retry ? PENDING : FAILED);
        update.setFerrormessage(limitNullable(rootMessage(failure), 1000));
        update.setFnextretrytime(retry ? now.plusSeconds(retryDelaySeconds(attempts)) : null);
        update.setFfinishtime(retry ? null : now);
        update.setFmodifytime(now);
        jobMapper.update(update, new LambdaUpdateWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFid, job.getFid())
                .eq(BizfiAiKnowledgeIndexJob::getFstatus, RUNNING));
        if (retry) {
            log.warn("Knowledge indexing will retry. jobId={}, docId={}, attempts={}/{}",
                    job.getFjobid(), job.getFdocid(), attempts, maxAttempts, failure);
        } else {
            log.error("Knowledge indexing failed permanently. jobId={}, docId={}, attempts={}",
                    job.getFjobid(), job.getFdocid(), attempts, failure);
        }
    }

    private void recoverStaleJobs() {
        int staleMinutes = positive(properties.getStaleRunningMinutes(), 15);
        LocalDateTime now = LocalDateTime.now();
        BizfiAiKnowledgeIndexJob update = new BizfiAiKnowledgeIndexJob();
        update.setFstatus(PENDING);
        update.setFerrormessage("任务执行超时，已重新排队");
        update.setFnextretrytime(now);
        update.setFmodifytime(now);
        jobMapper.update(update, new LambdaUpdateWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFstatus, RUNNING)
                .lt(BizfiAiKnowledgeIndexJob::getFstarttime, now.minusMinutes(staleMinutes))
                .apply("fattempts < fmaxattempts"));

        BizfiAiKnowledgeIndexJob failed = new BizfiAiKnowledgeIndexJob();
        failed.setFstatus(FAILED);
        failed.setFerrormessage("任务执行超时且已达到最大重试次数");
        failed.setFnextretrytime(null);
        failed.setFfinishtime(now);
        failed.setFmodifytime(now);
        jobMapper.update(failed, new LambdaUpdateWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFstatus, RUNNING)
                .lt(BizfiAiKnowledgeIndexJob::getFstarttime, now.minusMinutes(staleMinutes))
                .apply("fattempts >= fmaxattempts"));
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new BizException("知识文件导入和异步索引功能未启用");
        }
    }

    private BizfiAiKnowledgeIndexJob requireJob(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            throw new BizException("索引任务编号不能为空");
        }
        BizfiAiKnowledgeIndexJob job = jobMapper.selectOne(new LambdaQueryWrapper<BizfiAiKnowledgeIndexJob>()
                .eq(BizfiAiKnowledgeIndexJob::getFjobid, jobId.trim())
                .last("limit 1"));
        if (job == null) {
            throw new BizException("索引任务不存在");
        }
        return job;
    }

    private AiKnowledgeIndexJobResponse toResponse(BizfiAiKnowledgeIndexJob job) {
        return new AiKnowledgeIndexJobResponse(
                job.getFjobid(),
                job.getFkbid(),
                job.getFdocid(),
                job.getFfilename(),
                job.getFmediatype(),
                job.getFfilesize() == null ? 0L : job.getFfilesize(),
                job.getFcontenthash(),
                job.getFstatus(),
                job.getFattempts() == null ? 0 : job.getFattempts(),
                job.getFmaxattempts() == null ? resolveMaxAttempts() : job.getFmaxattempts(),
                job.getFerrormessage(),
                job.getFnextretrytime(),
                job.getFstarttime(),
                job.getFfinishtime(),
                job.getFcreatetime(),
                job.getFmodifytime()
        );
    }

    private int resolveMaxAttempts() {
        return Math.min(10, positive(properties.getMaxAttempts(), 3));
    }

    private int resolveBatchSize() {
        return Math.min(20, positive(properties.getBatchSize(), 5));
    }

    private int normalizeLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return 50;
        }
        return Math.min(200, requested);
    }

    private long retryDelaySeconds(int attempts) {
        return Math.min(60L, 5L * (1L << Math.min(4, Math.max(0, attempts - 1))));
    }

    private int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage()) ? current.getMessage() : current.getClass().getSimpleName();
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String limitNullable(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return limit(value.trim(), maxLength);
    }
}
