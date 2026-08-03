package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiKnowledgeIndexJobResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeIndexJob;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeIndexJobMapper;
import single.cjj.bizfi.ai.security.AiKnowledgePermission;
import single.cjj.bizfi.exception.BizException;

import java.util.List;
import java.util.Set;

@Service
public class AclAwareAiKnowledgeIndexJobService {

    private final AiKnowledgeIndexJobService delegate;
    private final BizfiAiKnowledgeIndexJobMapper jobMapper;
    private final AiKnowledgeAclService aclService;
    private final AiKnowledgeAccessGuard accessGuard;

    public AclAwareAiKnowledgeIndexJobService(
            AiKnowledgeIndexJobService delegate,
            BizfiAiKnowledgeIndexJobMapper jobMapper,
            AiKnowledgeAclService aclService,
            AiKnowledgeAccessGuard accessGuard
    ) {
        this.delegate = delegate;
        this.jobMapper = jobMapper;
        this.aclService = aclService;
        this.accessGuard = accessGuard;
    }

    public List<AiKnowledgeIndexJobResponse> listJobs(String docId, String status, Integer limit) {
        List<AiKnowledgeIndexJobResponse> jobs = delegate.listJobs(docId, status, limit);
        Set<String> accessibleBaseIds = aclService.resolveAccessibleBaseIds(AiKnowledgePermission.VIEWER);
        if (accessibleBaseIds == null) {
            return jobs;
        }
        return jobs.stream()
                .filter(job -> accessibleBaseIds.contains(job.kbId()))
                .toList();
    }

    public AiKnowledgeIndexJobResponse retry(String jobId) {
        BizfiAiKnowledgeIndexJob job = requireJob(jobId);
        aclService.assertCanEdit(job.getFkbid());
        return delegate.retry(jobId);
    }

    public AiKnowledgeIndexJobResponse createReindexJob(String docId) {
        accessGuard.assertCanEditDocument(docId);
        return delegate.createReindexJob(docId);
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
}
