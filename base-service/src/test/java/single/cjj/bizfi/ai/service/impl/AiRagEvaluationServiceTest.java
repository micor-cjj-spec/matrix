package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;
import single.cjj.bizfi.ai.config.KnowledgeEvaluationProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalRunResponse;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalCase;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalRun;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalSet;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalCaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalResultMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalRunMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalSetMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class AiRagEvaluationServiceTest {

    @Mock
    private BizfiAiRagEvalSetMapper setMapper;
    @Mock
    private BizfiAiRagEvalCaseMapper caseMapper;
    @Mock
    private BizfiAiRagEvalRunMapper runMapper;
    @Mock
    private BizfiAiRagEvalResultMapper resultMapper;
    @Mock
    private AiKnowledgeService knowledgeService;
    @Mock
    private AiKnowledgeAclService aclService;

    private KnowledgeEvaluationProperties properties;
    private AiRagEvaluationService service;

    @BeforeEach
    void setUp() {
        properties = new KnowledgeEvaluationProperties();
        properties.setEnabled(true);
        service = new AiRagEvaluationService(
                setMapper,
                caseMapper,
                runMapper,
                resultMapper,
                knowledgeService,
                aclService,
                new AiRagEvaluationMetrics(),
                properties,
                new AiProperties(),
                new AiVectorStoreProperties(),
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotTouchEvaluationTablesWhileFeatureIsDisabled() {
        properties.setEnabled(false);

        service.dispatchPendingRuns();

        verify(runMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(runMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldCreatePendingRunForActiveCases() {
        authenticate("1001");
        BizfiAiRagEvalSet set = evalSet();
        when(setMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(set);
        when(runMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(caseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        AiRagEvalRunResponse response = service.createRun(set.getFsetid());

        assertEquals(AiRagEvaluationService.PENDING, response.status());
        assertEquals(2, response.caseCount());
        assertNotNull(response.runId());
        verify(aclService).assertCanAdmin("finance");
        verify(runMapper).insert(any(BizfiAiRagEvalRun.class));
    }

    @Test
    void shouldExecutePendingRunAndPersistMetrics() {
        BizfiAiRagEvalRun run = new BizfiAiRagEvalRun();
        run.setFid(9L);
        run.setFrunid("evalrun_1");
        run.setFsetid("evalset_1");
        run.setFkbid("finance");
        run.setFstatus(AiRagEvaluationService.PENDING);
        run.setFcreatedby(1001L);
        run.setFcreatetime(LocalDateTime.now());
        run.setFmodifytime(LocalDateTime.now());

        BizfiAiRagEvalCase first = evalCase("case_1", "如何报销？", "[\"doc-a\"]", 5);
        BizfiAiRagEvalCase second = evalCase("case_2", "如何结账？", "[\"doc-b\"]", 5);

        when(runMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(run));
        when(runMapper.update(any(BizfiAiRagEvalRun.class), any(LambdaUpdateWrapper.class)))
                .thenReturn(1);
        when(caseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(first, second));
        when(knowledgeService.retrieve(eq("如何报销？"), eq(List.of("finance")), eq(5)))
                .thenReturn(List.of(new AiCitationResponse("doc-a", "报销", "chunk-a", "snippet")));
        when(knowledgeService.retrieve(eq("如何结账？"), eq(List.of("finance")), eq(5)))
                .thenReturn(List.of(new AiCitationResponse("doc-x", "其他", "chunk-x", "snippet")));

        service.dispatchPendingRuns();

        verify(resultMapper, org.mockito.Mockito.times(2)).insert(any());
        ArgumentCaptor<BizfiAiRagEvalRun> updates = ArgumentCaptor.forClass(BizfiAiRagEvalRun.class);
        verify(runMapper, org.mockito.Mockito.atLeast(4))
                .update(updates.capture(), any(LambdaUpdateWrapper.class));
        BizfiAiRagEvalRun completed = updates.getAllValues().stream()
                .filter(value -> AiRagEvaluationService.SUCCEEDED.equals(value.getFstatus()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, completed.getFcompletedcount());
        assertEquals(1, completed.getFhitcount());
        assertEquals(0.5D, completed.getFhitatk());
        assertEquals(0.5D, completed.getFmrr());
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())
        );
    }

    private BizfiAiRagEvalSet evalSet() {
        BizfiAiRagEvalSet set = new BizfiAiRagEvalSet();
        set.setFid(1L);
        set.setFsetid("evalset_1");
        set.setFkbid("finance");
        set.setFname("财务评测");
        set.setFstatus("ACTIVE");
        set.setFcreatetime(LocalDateTime.now());
        set.setFmodifytime(LocalDateTime.now());
        return set;
    }

    private BizfiAiRagEvalCase evalCase(String id, String question, String expectedDocs, int topK) {
        BizfiAiRagEvalCase item = new BizfiAiRagEvalCase();
        item.setFid((long) id.hashCode());
        item.setFcaseid(id);
        item.setFsetid("evalset_1");
        item.setFquestion(question);
        item.setFexpecteddocids(expectedDocs);
        item.setFexpectedchunkids("[]");
        item.setFtopk(topK);
        item.setFstatus("ACTIVE");
        item.setFcreatetime(LocalDateTime.now());
        item.setFmodifytime(LocalDateTime.now());
        return item;
    }
}
