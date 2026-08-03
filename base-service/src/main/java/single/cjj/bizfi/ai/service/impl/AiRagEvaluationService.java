package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;
import single.cjj.bizfi.ai.config.KnowledgeEvaluationProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalCaseRequest;
import single.cjj.bizfi.ai.dto.AiRagEvalCaseResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalConfigResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalResultResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalRunResponse;
import single.cjj.bizfi.ai.dto.AiRagEvalSetRequest;
import single.cjj.bizfi.ai.dto.AiRagEvalSetResponse;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalCase;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalResult;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalRun;
import single.cjj.bizfi.ai.entity.BizfiAiRagEvalSet;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalCaseMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalResultMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalRunMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiRagEvalSetMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AiRagEvaluationService {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String PARTIAL = "PARTIAL";
    public static final String FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(AiRagEvaluationService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final BizfiAiRagEvalSetMapper setMapper;
    private final BizfiAiRagEvalCaseMapper caseMapper;
    private final BizfiAiRagEvalRunMapper runMapper;
    private final BizfiAiRagEvalResultMapper resultMapper;
    private final AiKnowledgeService knowledgeService;
    private final AiKnowledgeAclService aclService;
    private final AiRagEvaluationMetrics metrics;
    private final KnowledgeEvaluationProperties properties;
    private final AiProperties aiProperties;
    private final AiVectorStoreProperties vectorStoreProperties;
    private final ObjectMapper objectMapper;

    public AiRagEvaluationService(
            BizfiAiRagEvalSetMapper setMapper,
            BizfiAiRagEvalCaseMapper caseMapper,
            BizfiAiRagEvalRunMapper runMapper,
            BizfiAiRagEvalResultMapper resultMapper,
            AiKnowledgeService knowledgeService,
            AiKnowledgeAclService aclService,
            AiRagEvaluationMetrics metrics,
            KnowledgeEvaluationProperties properties,
            AiProperties aiProperties,
            AiVectorStoreProperties vectorStoreProperties,
            ObjectMapper objectMapper
    ) {
        this.setMapper = setMapper;
        this.caseMapper = caseMapper;
        this.runMapper = runMapper;
        this.resultMapper = resultMapper;
        this.knowledgeService = knowledgeService;
        this.aclService = aclService;
        this.metrics = metrics;
        this.properties = properties;
        this.aiProperties = aiProperties;
        this.vectorStoreProperties = vectorStoreProperties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    public AiRagEvalConfigResponse config() {
        return new AiRagEvalConfigResponse(
                isEnabled(),
                maxCasesPerSet(),
                positiveLong(properties.getPollDelayMs(), 10000L),
                "sql/bizfi_ai_rag_evaluation_v7.sql"
        );
    }

    public List<AiRagEvalSetResponse> listSets(String kbId) {
        requireEnabled();
        String normalizedKbId = requireText(kbId, "知识库编号不能为空", 64);
        aclService.assertCanAdmin(normalizedKbId);
        return setMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalSet>()
                        .eq(BizfiAiRagEvalSet::getFkbid, normalizedKbId)
                        .orderByDesc(BizfiAiRagEvalSet::getFmodifytime)
                        .orderByDesc(BizfiAiRagEvalSet::getFid))
                .stream()
                .map(this::toSetResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalSetResponse createSet(String kbId, AiRagEvalSetRequest request) {
        requireEnabled();
        String normalizedKbId = requireText(kbId, "知识库编号不能为空", 64);
        aclService.assertCanAdmin(normalizedKbId);
        if (request == null) {
            throw new BizException("评测集内容不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        BizfiAiRagEvalSet set = new BizfiAiRagEvalSet();
        set.setFsetid(id("evalset_"));
        set.setFkbid(normalizedKbId);
        set.setFname(requireText(request.name(), "评测集名称不能为空", 160));
        set.setFdescription(limitNullable(request.description(), 1000));
        set.setFstatus(normalizeStatus(request.status()));
        set.setFcreatedby(requireCurrentUserId());
        set.setFcreatetime(now);
        set.setFmodifytime(now);
        setMapper.insert(set);
        return toSetResponse(set);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalSetResponse updateSet(String setId, AiRagEvalSetRequest request) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        assertNoActiveRun(set.getFsetid());
        if (request == null) {
            throw new BizException("评测集内容不能为空");
        }
        set.setFname(requireText(request.name(), "评测集名称不能为空", 160));
        set.setFdescription(limitNullable(request.description(), 1000));
        set.setFstatus(normalizeStatus(request.status()));
        set.setFmodifytime(LocalDateTime.now());
        setMapper.updateById(set);
        return toSetResponse(set);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSet(String setId) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        assertNoActiveRun(set.getFsetid());
        List<String> runIds = runMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                        .eq(BizfiAiRagEvalRun::getFsetid, set.getFsetid()))
                .stream()
                .map(BizfiAiRagEvalRun::getFrunid)
                .toList();
        if (!runIds.isEmpty()) {
            resultMapper.delete(new LambdaQueryWrapper<BizfiAiRagEvalResult>()
                    .in(BizfiAiRagEvalResult::getFrunid, runIds));
        }
        runMapper.delete(new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFsetid, set.getFsetid()));
        caseMapper.delete(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                .eq(BizfiAiRagEvalCase::getFsetid, set.getFsetid()));
        return setMapper.deleteById(set.getFid()) > 0;
    }

    public List<AiRagEvalCaseResponse> listCases(String setId) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        return caseMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                        .eq(BizfiAiRagEvalCase::getFsetid, set.getFsetid())
                        .orderByAsc(BizfiAiRagEvalCase::getFid))
                .stream()
                .map(this::toCaseResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalCaseResponse createCase(String setId, AiRagEvalCaseRequest request) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        assertNoActiveRun(set.getFsetid());
        long count = caseMapper.selectCount(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                .eq(BizfiAiRagEvalCase::getFsetid, set.getFsetid()));
        if (count >= maxCasesPerSet()) {
            throw new BizException("单个评测集最多允许 " + maxCasesPerSet() + " 道题");
        }
        CasePayload payload = validateCase(request);
        LocalDateTime now = LocalDateTime.now();
        BizfiAiRagEvalCase item = new BizfiAiRagEvalCase();
        item.setFcaseid(id("evalcase_"));
        item.setFsetid(set.getFsetid());
        item.setFquestion(payload.question());
        item.setFexpecteddocids(writeJson(payload.expectedDocIds()));
        item.setFexpectedchunkids(writeJson(payload.expectedChunkIds()));
        item.setFtopk(payload.topK());
        item.setFstatus(payload.status());
        item.setFcreatetime(now);
        item.setFmodifytime(now);
        caseMapper.insert(item);
        touchSet(set);
        return toCaseResponse(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalCaseResponse updateCase(String setId, String caseId, AiRagEvalCaseRequest request) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        assertNoActiveRun(set.getFsetid());
        BizfiAiRagEvalCase item = requireCase(set.getFsetid(), caseId);
        CasePayload payload = validateCase(request);
        item.setFquestion(payload.question());
        item.setFexpecteddocids(writeJson(payload.expectedDocIds()));
        item.setFexpectedchunkids(writeJson(payload.expectedChunkIds()));
        item.setFtopk(payload.topK());
        item.setFstatus(payload.status());
        item.setFmodifytime(LocalDateTime.now());
        caseMapper.updateById(item);
        touchSet(set);
        return toCaseResponse(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCase(String setId, String caseId) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        assertNoActiveRun(set.getFsetid());
        BizfiAiRagEvalCase item = requireCase(set.getFsetid(), caseId);
        boolean deleted = caseMapper.deleteById(item.getFid()) > 0;
        touchSet(set);
        return deleted;
    }

    public List<AiRagEvalRunResponse> listRuns(String setId, Integer limit) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        return runMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                        .eq(BizfiAiRagEvalRun::getFsetid, set.getFsetid())
                        .orderByDesc(BizfiAiRagEvalRun::getFcreatetime)
                        .orderByDesc(BizfiAiRagEvalRun::getFid)
                        .last("limit " + normalizeLimit(limit, 20, 100)))
                .stream()
                .map(this::toRunResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalRunResponse createRun(String setId) {
        requireEnabled();
        BizfiAiRagEvalSet set = requireSet(setId);
        aclService.assertCanAdmin(set.getFkbid());
        if (!"ACTIVE".equals(set.getFstatus())) {
            throw new BizException("只有启用状态的评测集才能运行");
        }
        assertNoActiveRun(set.getFsetid());
        long caseCount = caseMapper.selectCount(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                .eq(BizfiAiRagEvalCase::getFsetid, set.getFsetid())
                .eq(BizfiAiRagEvalCase::getFstatus, "ACTIVE"));
        if (caseCount <= 0) {
            throw new BizException("评测集没有启用状态的标准问题");
        }
        if (caseCount > maxCasesPerSet()) {
            throw new BizException("评测题目数量超过当前上限 " + maxCasesPerSet());
        }
        LocalDateTime now = LocalDateTime.now();
        BizfiAiRagEvalRun run = new BizfiAiRagEvalRun();
        run.setFrunid(id("evalrun_"));
        run.setFsetid(set.getFsetid());
        run.setFkbid(set.getFkbid());
        run.setFstatus(PENDING);
        run.setFcasecount((int) caseCount);
        run.setFcompletedcount(0);
        run.setFhitcount(0);
        run.setFconfigsnapshot(configSnapshot());
        run.setFcreatedby(requireCurrentUserId());
        run.setFcreatetime(now);
        run.setFmodifytime(now);
        runMapper.insert(run);
        return toRunResponse(run);
    }

    public AiRagEvalRunResponse getRun(String runId) {
        requireEnabled();
        BizfiAiRagEvalRun run = requireRun(runId);
        aclService.assertCanAdmin(run.getFkbid());
        return toRunResponse(run);
    }

    public List<AiRagEvalResultResponse> listResults(String runId) {
        requireEnabled();
        BizfiAiRagEvalRun run = requireRun(runId);
        aclService.assertCanAdmin(run.getFkbid());
        return resultMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalResult>()
                        .eq(BizfiAiRagEvalResult::getFrunid, run.getFrunid())
                        .orderByAsc(BizfiAiRagEvalResult::getFid))
                .stream()
                .map(this::toResultResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiRagEvalRunResponse retryRun(String runId) {
        requireEnabled();
        BizfiAiRagEvalRun run = requireRun(runId);
        aclService.assertCanAdmin(run.getFkbid());
        if (RUNNING.equals(run.getFstatus())) {
            throw new BizException("评测正在运行，不能重复提交");
        }
        assertNoOtherActiveRun(run.getFsetid(), run.getFrunid());
        resultMapper.delete(new LambdaQueryWrapper<BizfiAiRagEvalResult>()
                .eq(BizfiAiRagEvalResult::getFrunid, run.getFrunid()));
        LocalDateTime now = LocalDateTime.now();
        run.setFstatus(PENDING);
        run.setFcompletedcount(0);
        run.setFhitcount(0);
        run.setFhitatk(null);
        run.setFmrr(null);
        run.setFrecallatk(null);
        run.setFavglatencyms(null);
        run.setFp95latencyms(null);
        run.setFerrormessage(null);
        run.setFstarttime(null);
        run.setFfinishtime(null);
        run.setFconfigsnapshot(configSnapshot());
        run.setFmodifytime(now);
        runMapper.updateById(run);
        return toRunResponse(run);
    }

    @Scheduled(fixedDelayString = "${bizfi.ai.knowledge-evaluation.poll-delay-ms:10000}")
    public void dispatchPendingRuns() {
        if (!isEnabled()) {
            return;
        }
        recoverStaleRuns();
        List<BizfiAiRagEvalRun> candidates = runMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFstatus, PENDING)
                .orderByAsc(BizfiAiRagEvalRun::getFid)
                .last("limit " + positive(properties.getBatchSize(), 1)));
        for (BizfiAiRagEvalRun candidate : candidates) {
            BizfiAiRagEvalRun claimed = claim(candidate);
            if (claimed != null) {
                executeRun(claimed);
            }
        }
    }

    private BizfiAiRagEvalRun claim(BizfiAiRagEvalRun candidate) {
        LocalDateTime now = LocalDateTime.now();
        BizfiAiRagEvalRun update = new BizfiAiRagEvalRun();
        update.setFstatus(RUNNING);
        update.setFstarttime(now);
        update.setFmodifytime(now);
        int affected = runMapper.update(update, new LambdaUpdateWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFid, candidate.getFid())
                .eq(BizfiAiRagEvalRun::getFstatus, PENDING));
        if (affected == 0) {
            return null;
        }
        candidate.setFstatus(RUNNING);
        candidate.setFstarttime(now);
        candidate.setFmodifytime(now);
        return candidate;
    }

    private void executeRun(BizfiAiRagEvalRun run) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
        systemContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                String.valueOf(run.getFcreatedby() == null ? 0L : run.getFcreatedby()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        ));
        SecurityContextHolder.setContext(systemContext);
        try {
            resultMapper.delete(new LambdaQueryWrapper<BizfiAiRagEvalResult>()
                    .eq(BizfiAiRagEvalResult::getFrunid, run.getFrunid()));
            List<BizfiAiRagEvalCase> cases = caseMapper.selectList(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                    .eq(BizfiAiRagEvalCase::getFsetid, run.getFsetid())
                    .eq(BizfiAiRagEvalCase::getFstatus, "ACTIVE")
                    .orderByAsc(BizfiAiRagEvalCase::getFid)
                    .last("limit " + maxCasesPerSet()));
            if (cases.isEmpty()) {
                failRun(run, "评测集没有可执行的标准问题");
                return;
            }

            List<AiRagEvaluationMetrics.Observation> observations = new ArrayList<>();
            int errors = 0;
            int completed = 0;
            int hits = 0;
            for (BizfiAiRagEvalCase item : cases) {
                long started = System.nanoTime();
                AiRagEvaluationMetrics.CaseMetrics caseMetrics;
                String errorMessage = null;
                try {
                    List<AiCitationResponse> citations = knowledgeService.retrieve(
                            item.getFquestion(),
                            List.of(run.getFkbid()),
                            normalizeTopK(item.getFtopk())
                    );
                    caseMetrics = metrics.evaluate(
                            readJson(item.getFexpecteddocids()),
                            readJson(item.getFexpectedchunkids()),
                            citations
                    );
                } catch (RuntimeException failure) {
                    errors++;
                    errorMessage = limitNullable(rootMessage(failure), 1000);
                    caseMetrics = metrics.evaluate(
                            readJson(item.getFexpecteddocids()),
                            readJson(item.getFexpectedchunkids()),
                            List.of()
                    );
                    log.warn("RAG evaluation case failed. runId={}, caseId={}",
                            run.getFrunid(), item.getFcaseid(), failure);
                }
                long latencyMs = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                observations.add(new AiRagEvaluationMetrics.Observation(caseMetrics, latencyMs));
                if (caseMetrics.hit()) {
                    hits++;
                }
                completed++;
                insertResult(run, item, caseMetrics, latencyMs, errorMessage);
                updateProgress(run, completed, hits);
            }

            AiRagEvaluationMetrics.RunMetrics summary = metrics.summarize(observations);
            LocalDateTime now = LocalDateTime.now();
            BizfiAiRagEvalRun update = new BizfiAiRagEvalRun();
            update.setFstatus(errors == 0 ? SUCCEEDED : PARTIAL);
            update.setFcasecount(summary.caseCount());
            update.setFcompletedcount(summary.caseCount());
            update.setFhitcount(summary.hitCount());
            update.setFhitatk(summary.hitAtK());
            update.setFmrr(summary.mrr());
            update.setFrecallatk(summary.recallAtK());
            update.setFavglatencyms(summary.averageLatencyMs());
            update.setFp95latencyms(summary.p95LatencyMs());
            update.setFerrormessage(errors == 0 ? null : errors + " 道题执行失败，已按未命中计入指标");
            update.setFfinishtime(now);
            update.setFmodifytime(now);
            runMapper.update(update, new LambdaUpdateWrapper<BizfiAiRagEvalRun>()
                    .eq(BizfiAiRagEvalRun::getFid, run.getFid())
                    .eq(BizfiAiRagEvalRun::getFstatus, RUNNING));
        } catch (RuntimeException failure) {
            log.error("RAG evaluation run failed. runId={}", run.getFrunid(), failure);
            failRun(run, rootMessage(failure));
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void insertResult(
            BizfiAiRagEvalRun run,
            BizfiAiRagEvalCase item,
            AiRagEvaluationMetrics.CaseMetrics caseMetrics,
            long latencyMs,
            String errorMessage
    ) {
        BizfiAiRagEvalResult result = new BizfiAiRagEvalResult();
        result.setFrunid(run.getFrunid());
        result.setFcaseid(item.getFcaseid());
        result.setFquestion(item.getFquestion());
        result.setFexpecteddocids(item.getFexpecteddocids());
        result.setFexpectedchunkids(item.getFexpectedchunkids());
        result.setFretrieveddocids(writeJson(caseMetrics.retrievedDocIds()));
        result.setFretrievedchunkids(writeJson(caseMetrics.retrievedChunkIds()));
        result.setFhit(caseMetrics.hit());
        result.setFfirstrelevantrank(caseMetrics.firstRelevantRank());
        result.setFreciprocalrank(caseMetrics.reciprocalRank());
        result.setFrecallatk(caseMetrics.recallAtK());
        result.setFlatencyms(latencyMs);
        result.setFerrormessage(errorMessage);
        result.setFcreatetime(LocalDateTime.now());
        resultMapper.insert(result);
    }

    private void updateProgress(BizfiAiRagEvalRun run, int completed, int hits) {
        BizfiAiRagEvalRun progress = new BizfiAiRagEvalRun();
        progress.setFcompletedcount(completed);
        progress.setFhitcount(hits);
        progress.setFmodifytime(LocalDateTime.now());
        runMapper.update(progress, new LambdaUpdateWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFid, run.getFid())
                .eq(BizfiAiRagEvalRun::getFstatus, RUNNING));
    }

    private void failRun(BizfiAiRagEvalRun run, String message) {
        LocalDateTime now = LocalDateTime.now();
        BizfiAiRagEvalRun update = new BizfiAiRagEvalRun();
        update.setFstatus(FAILED);
        update.setFerrormessage(limitNullable(message, 1000));
        update.setFfinishtime(now);
        update.setFmodifytime(now);
        runMapper.update(update, new LambdaUpdateWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFid, run.getFid())
                .eq(BizfiAiRagEvalRun::getFstatus, RUNNING));
    }

    private void recoverStaleRuns() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(
                positive(properties.getStaleRunningMinutes(), 30)
        );
        BizfiAiRagEvalRun update = new BizfiAiRagEvalRun();
        update.setFstatus(PENDING);
        update.setFerrormessage("评测运行超时，已重新排队");
        update.setFstarttime(null);
        update.setFmodifytime(LocalDateTime.now());
        runMapper.update(update, new LambdaUpdateWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFstatus, RUNNING)
                .lt(BizfiAiRagEvalRun::getFstarttime, threshold));
    }

    private AiRagEvalSetResponse toSetResponse(BizfiAiRagEvalSet set) {
        long caseCount = caseMapper.selectCount(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                .eq(BizfiAiRagEvalCase::getFsetid, set.getFsetid()));
        return new AiRagEvalSetResponse(
                set.getFsetid(),
                set.getFkbid(),
                set.getFname(),
                set.getFdescription(),
                set.getFstatus(),
                caseCount,
                set.getFcreatetime(),
                set.getFmodifytime()
        );
    }

    private AiRagEvalCaseResponse toCaseResponse(BizfiAiRagEvalCase item) {
        return new AiRagEvalCaseResponse(
                item.getFcaseid(),
                item.getFsetid(),
                item.getFquestion(),
                readJson(item.getFexpecteddocids()),
                readJson(item.getFexpectedchunkids()),
                normalizeTopK(item.getFtopk()),
                item.getFstatus(),
                item.getFcreatetime(),
                item.getFmodifytime()
        );
    }

    private AiRagEvalRunResponse toRunResponse(BizfiAiRagEvalRun run) {
        return new AiRagEvalRunResponse(
                run.getFrunid(),
                run.getFsetid(),
                run.getFkbid(),
                run.getFstatus(),
                value(run.getFcasecount()),
                value(run.getFcompletedcount()),
                value(run.getFhitcount()),
                run.getFhitatk(),
                run.getFmrr(),
                run.getFrecallatk(),
                run.getFavglatencyms(),
                run.getFp95latencyms(),
                run.getFconfigsnapshot(),
                run.getFerrormessage(),
                run.getFstarttime(),
                run.getFfinishtime(),
                run.getFcreatetime(),
                run.getFmodifytime()
        );
    }

    private AiRagEvalResultResponse toResultResponse(BizfiAiRagEvalResult result) {
        return new AiRagEvalResultResponse(
                result.getFrunid(),
                result.getFcaseid(),
                result.getFquestion(),
                readJson(result.getFexpecteddocids()),
                readJson(result.getFexpectedchunkids()),
                readJson(result.getFretrieveddocids()),
                readJson(result.getFretrievedchunkids()),
                Boolean.TRUE.equals(result.getFhit()),
                result.getFfirstrelevantrank(),
                result.getFreciprocalrank() == null ? 0D : result.getFreciprocalrank(),
                result.getFrecallatk() == null ? 0D : result.getFrecallatk(),
                result.getFlatencyms() == null ? 0L : result.getFlatencyms(),
                result.getFerrormessage(),
                result.getFcreatetime()
        );
    }

    private BizfiAiRagEvalSet requireSet(String setId) {
        String normalized = requireText(setId, "评测集编号不能为空", 64);
        BizfiAiRagEvalSet set = setMapper.selectOne(new LambdaQueryWrapper<BizfiAiRagEvalSet>()
                .eq(BizfiAiRagEvalSet::getFsetid, normalized)
                .last("limit 1"));
        if (set == null) {
            throw new BizException("评测集不存在");
        }
        return set;
    }

    private BizfiAiRagEvalCase requireCase(String setId, String caseId) {
        String normalized = requireText(caseId, "评测题目编号不能为空", 64);
        BizfiAiRagEvalCase item = caseMapper.selectOne(new LambdaQueryWrapper<BizfiAiRagEvalCase>()
                .eq(BizfiAiRagEvalCase::getFsetid, setId)
                .eq(BizfiAiRagEvalCase::getFcaseid, normalized)
                .last("limit 1"));
        if (item == null) {
            throw new BizException("评测题目不存在");
        }
        return item;
    }

    private BizfiAiRagEvalRun requireRun(String runId) {
        String normalized = requireText(runId, "评测运行编号不能为空", 64);
        BizfiAiRagEvalRun run = runMapper.selectOne(new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFrunid, normalized)
                .last("limit 1"));
        if (run == null) {
            throw new BizException("评测运行不存在");
        }
        return run;
    }

    private CasePayload validateCase(AiRagEvalCaseRequest request) {
        if (request == null) {
            throw new BizException("评测题目不能为空");
        }
        String question = requireText(request.question(), "评测问题不能为空", 4000);
        List<String> expectedDocs = normalizeIds(request.expectedDocIds());
        List<String> expectedChunks = normalizeIds(request.expectedChunkIds());
        if (expectedDocs.isEmpty() && expectedChunks.isEmpty()) {
            throw new BizException("至少需要配置一个期望文档或期望Chunk");
        }
        return new CasePayload(
                question,
                expectedDocs,
                expectedChunks,
                normalizeTopK(request.topK()),
                normalizeStatus(request.status())
        );
    }

    private void assertNoActiveRun(String setId) {
        assertNoOtherActiveRun(setId, null);
    }

    private void assertNoOtherActiveRun(String setId, String excludedRunId) {
        LambdaQueryWrapper<BizfiAiRagEvalRun> wrapper = new LambdaQueryWrapper<BizfiAiRagEvalRun>()
                .eq(BizfiAiRagEvalRun::getFsetid, setId)
                .in(BizfiAiRagEvalRun::getFstatus, List.of(PENDING, RUNNING));
        if (StringUtils.hasText(excludedRunId)) {
            wrapper.ne(BizfiAiRagEvalRun::getFrunid, excludedRunId);
        }
        if (runMapper.selectCount(wrapper) > 0) {
            throw new BizException("评测集存在等待或运行中的任务，请完成后再修改");
        }
    }

    private void touchSet(BizfiAiRagEvalSet set) {
        set.setFmodifytime(LocalDateTime.now());
        setMapper.updateById(set);
    }

    private String configSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("semanticRetrievalEnabled", aiProperties.getSemanticRetrievalEnabled());
        snapshot.put("semanticFailOpen", aiProperties.getSemanticFailOpen());
        snapshot.put("hybridKeywordWeight", aiProperties.getHybridKeywordWeight());
        snapshot.put("hybridSemanticWeight", aiProperties.getHybridSemanticWeight());
        snapshot.put("hybridRrfK", aiProperties.getHybridRrfK());
        snapshot.put("vectorStoreType", vectorStoreProperties.getType());
        snapshot.put("readFallbackEnabled", vectorStoreProperties.getReadFallbackEnabled());
        snapshot.put("pgVectorEnabled", vectorStoreProperties.getPgvector().getEnabled());
        return writeJsonObject(snapshot);
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new BizException(
                    "RAG评测功能未启用，请先执行 sql/bizfi_ai_rag_evaluation_v7.sql，"
                            + "再设置 AI_KNOWLEDGE_EVALUATION_ENABLED=true"
            );
        }
    }

    private Long requireCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw new BizException("未获取到当前登录用户");
        }
        try {
            long userId = Long.parseLong(principal.toString());
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException failure) {
            throw new BizException("当前登录用户编号无效");
        }
    }

    private List<String> normalizeIds(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .map(value -> value.length() <= 160 ? value : value.substring(0, 160))
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private String normalizeStatus(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if (!Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BizException("状态只支持 ACTIVE 或 DISABLED");
        }
        return normalized;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return 5;
        }
        return Math.min(topK, 20);
    }

    private int maxCasesPerSet() {
        return positive(properties.getMaxCasesPerSet(), 100);
    }

    private int normalizeLimit(Integer configured, int fallback, int maximum) {
        if (configured == null || configured <= 0) {
            return fallback;
        }
        return Math.min(configured, maximum);
    }

    private int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private long positiveLong(Long value, long fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private String requireText(String value, String message, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String limitNullable(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage())
                ? current.getMessage()
                : current.getClass().getSimpleName();
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测数据序列化失败");
        }
    }

    private String writeJsonObject(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            return "{}";
        }
    }

    private List<String> readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return normalizeIds(objectMapper.readValue(json, STRING_LIST));
        } catch (JsonProcessingException failure) {
            log.warn("Invalid RAG evaluation JSON payload, treating as empty list.");
            return List.of();
        }
    }

    private record CasePayload(
            String question,
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            int topK,
            String status
    ) {
    }
}
