package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.config.KnowledgeIngestionProperties;
import single.cjj.bizfi.ai.dto.AiKnowledgeIndexJobResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeIndexJob;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeIndexJobMapper;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeIndexJobServiceTest {

    @Mock
    private BizfiAiKnowledgeIndexJobMapper jobMapper;

    @Mock
    private BizfiAiKnowledgeDocMapper docMapper;

    @Mock
    private AiKnowledgeVectorIndexService vectorIndexService;

    private KnowledgeIngestionProperties properties;
    private AiKnowledgeIndexJobService service;

    @BeforeEach
    void setUp() {
        properties = new KnowledgeIngestionProperties();
        properties.setEnabled(true);
        properties.setBatchSize(5);
        properties.setMaxAttempts(3);
        service = new AiKnowledgeIndexJobService(jobMapper, docMapper, vectorIndexService, properties);
    }

    @Test
    void shouldCreatePendingJob() {
        AiKnowledgeIndexJobResponse result = service.createJob(
                "finance",
                "month-end",
                "月结制度.pdf",
                "application/pdf",
                1024L,
                "a".repeat(64)
        );

        assertEquals("finance", result.kbId());
        assertEquals("month-end", result.docId());
        assertEquals(AiKnowledgeIndexJobService.PENDING, result.status());
        assertEquals(0, result.attempts());
        verify(jobMapper).insert(any(BizfiAiKnowledgeIndexJob.class));
    }

    @Test
    void shouldClaimAndCompletePendingJob() {
        BizfiAiKnowledgeIndexJob candidate = pendingJob();
        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(candidate));
        when(jobMapper.update(any(BizfiAiKnowledgeIndexJob.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(vectorIndexService.indexDocumentIfEnabled("month-end"))
                .thenReturn(new AiKnowledgeVectorIndexService.IndexResult(
                        "month-end", 2, "embedding-test", 2, "INDEXED"
                ));

        service.dispatchPendingJobs();

        ArgumentCaptor<BizfiAiKnowledgeIndexJob> updates = ArgumentCaptor.forClass(BizfiAiKnowledgeIndexJob.class);
        verify(jobMapper, atLeast(4)).update(updates.capture(), any(LambdaUpdateWrapper.class));
        assertTrue(updates.getAllValues().stream()
                .anyMatch(item -> AiKnowledgeIndexJobService.RUNNING.equals(item.getFstatus())));
        assertTrue(updates.getAllValues().stream()
                .anyMatch(item -> AiKnowledgeIndexJobService.SUCCEEDED.equals(item.getFstatus())));
    }

    @Test
    void shouldRejectRetryForRunningJob() {
        BizfiAiKnowledgeIndexJob running = pendingJob();
        running.setFstatus(AiKnowledgeIndexJobService.RUNNING);
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(running);

        assertThrows(BizException.class, () -> service.retry(running.getFjobid()));

        verify(jobMapper, never()).updateById(any(BizfiAiKnowledgeIndexJob.class));
    }

    @Test
    void shouldAvoidAllJobStorageWhenIngestionIsDisabled() {
        properties.setEnabled(false);

        service.dispatchPendingJobs();
        assertEquals(List.of(), service.listJobs(null, null, 50));
        assertThrows(BizException.class, () -> service.createJob(
                "default", "doc", "doc.txt", "text/plain", 3L, "manual"
        ));
        assertThrows(BizException.class, () -> service.retry("idx_test"));
        assertThrows(BizException.class, () -> service.createReindexJob("doc"));

        verify(jobMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(jobMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(jobMapper, never()).insert(any(BizfiAiKnowledgeIndexJob.class));
        verify(jobMapper, never()).update(any(BizfiAiKnowledgeIndexJob.class), any(LambdaUpdateWrapper.class));
        verify(docMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    private BizfiAiKnowledgeIndexJob pendingJob() {
        BizfiAiKnowledgeIndexJob job = new BizfiAiKnowledgeIndexJob();
        job.setFid(1L);
        job.setFjobid("idx_test");
        job.setFkbid("finance");
        job.setFdocid("month-end");
        job.setFfilename("月结制度.pdf");
        job.setFmediatype("application/pdf");
        job.setFfilesize(1024L);
        job.setFcontenthash("a".repeat(64));
        job.setFstatus(AiKnowledgeIndexJobService.PENDING);
        job.setFattempts(0);
        job.setFmaxattempts(3);
        job.setFnextretrytime(LocalDateTime.now().minusSeconds(1));
        job.setFcreatetime(LocalDateTime.now());
        job.setFmodifytime(LocalDateTime.now());
        return job;
    }
}
