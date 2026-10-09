package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeRetrievalResponse;
import single.cjj.bizfi.ai.dto.AiRetrievalTraceResponse;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationDataset;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationQuestion;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationResult;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationRun;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationTrace;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationDatasetMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationQuestionMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationResultMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationRunMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationTraceMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AiKnowledgeEvaluationTraceRunService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final BizfiAiEvaluationDatasetMapper datasetMapper;
    private final BizfiAiEvaluationQuestionMapper questionMapper;
    private final BizfiAiEvaluationRunMapper runMapper;
    private final BizfiAiEvaluationResultMapper resultMapper;
    private final BizfiAiEvaluationTraceMapper traceMapper;
    private final AiKnowledgeService knowledgeService;
    private final AiKnowledgeAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public AiKnowledgeEvaluationTraceRunService(
            BizfiAiEvaluationDatasetMapper datasetMapper,
            BizfiAiEvaluationQuestionMapper questionMapper,
            BizfiAiEvaluationRunMapper runMapper,
            BizfiAiEvaluationResultMapper resultMapper,
            BizfiAiEvaluationTraceMapper traceMapper,
            AiKnowledgeService knowledgeService,
            AiKnowledgeAccessGuard accessGuard,
            ObjectMapper objectMapper
    ) {
        this.datasetMapper = datasetMapper;
        this.questionMapper = questionMapper;
        this.runMapper = runMapper;
        this.resultMapper = resultMapper;
        this.traceMapper = traceMapper;
        this.knowledgeService = knowledgeService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    public TraceRunView run(String datasetId, Integer requestedTopK) {
        assertEvaluationAdmin();
        BizfiAiEvaluationDataset dataset = requireDataset(datasetId);
        if (!"ACTIVE".equals(dataset.getFstatus())) {
            throw new BizException("停用的评测集不能执行");
        }

        List<BizfiAiEvaluationQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<BizfiAiEvaluationQuestion>()
                        .eq(BizfiAiEvaluationQuestion::getFdatasetid, dataset.getFdatasetid())
                        .eq(BizfiAiEvaluationQuestion::getFstatus, "ACTIVE")
                        .orderByAsc(BizfiAiEvaluationQuestion::getFid)
        );
        if (questions.isEmpty()) {
            throw new BizException("评测集没有可执行的标准问题");
        }

        int topK = normalizeTopK(requestedTopK);
        LocalDateTime now = LocalDateTime.now();
        BizfiAiEvaluationRun run = new BizfiAiEvaluationRun();
        run.setFrunid(generateId("eval_run_"));
        run.setFdatasetid(dataset.getFdatasetid());
        run.setFstatus("RUNNING");
        run.setFtopk(topK);
        run.setFtotalquestions(questions.size());
        run.setFcompletedquestions(0);
        run.setFstarttime(now);
        run.setFcreatetime(now);
        run.setFmodifytime(now);
        runMapper.insert(run);

        double recallSum = 0D;
        double reciprocalRankSum = 0D;
        long latencySum = 0L;
        int zeroHitCount = 0;
        int failureCount = 0;
        List<String> failures = new ArrayList<>();

        for (BizfiAiEvaluationQuestion question : questions) {
            long startedNanos = System.nanoTime();
            List<AiCitationResponse> citations = List.of();
            AiRetrievalTraceResponse trace = AiRetrievalTraceResponse.unavailable();
            AiEvaluationMetrics.QuestionMetrics metrics = null;
            String errorMessage = null;

            try {
                AiKnowledgeRetrievalResponse retrieval = knowledgeService.retrieveWithTrace(
                        question.getFquestion(),
                        readStringList(question.getFkbids()),
                        topK
                );
                citations = retrieval.citations();
                trace = retrieval.trace();
                metrics = AiEvaluationMetrics.calculate(
                        citations,
                        new LinkedHashSet<>(readStringList(question.getFexpecteddocids())),
                        new LinkedHashSet<>(readStringList(question.getFexpectedchunkids()))
                );
            } catch (RuntimeException failure) {
                failureCount++;
                errorMessage = limit(resolveErrorMessage(failure), 1000);
                trace = AiRetrievalTraceResponse.unavailable(errorMessage);
                failures.add(question.getFquestionid() + ": " + errorMessage);
            }

            long latencyMs = Math.max(
                    0L,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)
            );
            latencySum += latencyMs;
            if (metrics != null) {
                recallSum += metrics.recall();
                reciprocalRankSum += metrics.reciprocalRank();
                if (metrics.firstRelevantRank() == null) {
                    zeroHitCount++;
                }
            } else {
                zeroHitCount++;
            }

            persistResultAndTrace(
                    run.getFrunid(),
                    question,
                    citations,
                    trace,
                    metrics,
                    latencyMs,
                    errorMessage
            );
            run.setFcompletedquestions(run.getFcompletedquestions() + 1);
            run.setFmodifytime(LocalDateTime.now());
            runMapper.updateById(run);
        }

        int total = questions.size();
        run.setFrecallatk(recallSum / total);
        run.setFmrr(reciprocalRankSum / total);
        run.setFzerohitrate((double) zeroHitCount / total);
        run.setFavglatencyms(latencySum / total);
        run.setFstatus(
                failureCount == 0
                        ? "SUCCEEDED"
                        : failureCount == total ? "FAILED" : "PARTIAL"
        );
        run.setFerrormessage(
                failures.isEmpty() ? null : limit(String.join(" | ", failures), 1000)
        );
        run.setFfinishtime(LocalDateTime.now());
        run.setFmodifytime(run.getFfinishtime());
        runMapper.updateById(run);
        return toRunView(run, total);
    }

    public List<TraceView> listTraces(String runId) {
        assertEvaluationAdmin();
        requireRun(runId);
        return traceMapper.selectList(
                        new LambdaQueryWrapper<BizfiAiEvaluationTrace>()
                                .eq(BizfiAiEvaluationTrace::getFrunid, runId.trim())
                                .orderByAsc(BizfiAiEvaluationTrace::getFid)
                )
                .stream()
                .map(this::toTraceView)
                .toList();
    }

    private void persistResultAndTrace(
            String runId,
            BizfiAiEvaluationQuestion question,
            List<AiCitationResponse> citations,
            AiRetrievalTraceResponse trace,
            AiEvaluationMetrics.QuestionMetrics metrics,
            long latencyMs,
            String errorMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        String resultId = generateId("eval_result_");

        BizfiAiEvaluationResult result = new BizfiAiEvaluationResult();
        result.setFresultid(resultId);
        result.setFrunid(runId);
        result.setFquestionid(question.getFquestionid());
        result.setFcitationsjson(writeJson(citations == null ? List.of() : citations));
        result.setFfirstrelevantrank(metrics == null ? null : metrics.firstRelevantRank());
        result.setFrecall(metrics == null ? 0D : metrics.recall());
        result.setFreciprocalrank(metrics == null ? 0D : metrics.reciprocalRank());
        result.setFlatencyms(latencyMs);
        result.setFerrormessage(errorMessage);
        result.setFcreatetime(now);
        resultMapper.insert(result);

        AiRetrievalTraceResponse safeTrace = trace == null
                ? AiRetrievalTraceResponse.unavailable()
                : trace;
        BizfiAiEvaluationTrace traceEntity = new BizfiAiEvaluationTrace();
        traceEntity.setFtraceid(generateId("eval_trace_"));
        traceEntity.setFrunid(runId);
        traceEntity.setFresultid(resultId);
        traceEntity.setFquestionid(question.getFquestionid());
        traceEntity.setFconfigfingerprint(
                normalizeTraceText(safeTrace.configFingerprint(), "unknown", 64)
        );
        traceEntity.setFmode(normalizeTraceText(safeTrace.mode(), "UNAVAILABLE", 32));
        traceEntity.setFtracejson(writeJson(safeTrace));
        traceEntity.setFcreatetime(now);
        traceMapper.insert(traceEntity);
    }

    private BizfiAiEvaluationDataset requireDataset(String datasetId) {
        if (!StringUtils.hasText(datasetId)) {
            throw new BizException("评测集编号不能为空");
        }
        BizfiAiEvaluationDataset dataset = datasetMapper.selectOne(
                new LambdaQueryWrapper<BizfiAiEvaluationDataset>()
                        .eq(BizfiAiEvaluationDataset::getFdatasetid, datasetId.trim())
                        .last("limit 1")
        );
        if (dataset == null) {
            throw new BizException("评测集不存在");
        }
        return dataset;
    }

    private BizfiAiEvaluationRun requireRun(String runId) {
        if (!StringUtils.hasText(runId)) {
            throw new BizException("评测运行编号不能为空");
        }
        BizfiAiEvaluationRun run = runMapper.selectOne(
                new LambdaQueryWrapper<BizfiAiEvaluationRun>()
                        .eq(BizfiAiEvaluationRun::getFrunid, runId.trim())
                        .last("limit 1")
        );
        if (run == null) {
            throw new BizException("评测运行不存在");
        }
        return run;
    }

    private TraceRunView toRunView(BizfiAiEvaluationRun run, int traceCount) {
        return new TraceRunView(
                run.getFrunid(),
                run.getFdatasetid(),
                run.getFstatus(),
                run.getFtopk(),
                run.getFtotalquestions(),
                run.getFcompletedquestions(),
                run.getFrecallatk(),
                run.getFmrr(),
                run.getFzerohitrate(),
                run.getFavglatencyms(),
                traceCount,
                run.getFerrormessage(),
                run.getFstarttime(),
                run.getFfinishtime()
        );
    }

    private TraceView toTraceView(BizfiAiEvaluationTrace trace) {
        return new TraceView(
                trace.getFtraceid(),
                trace.getFrunid(),
                trace.getFresultid(),
                trace.getFquestionid(),
                trace.getFconfigfingerprint(),
                trace.getFmode(),
                readTrace(trace.getFtracejson()),
                trace.getFcreatetime()
        );
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, MAX_TOP_K));
    }

    private String normalizeTraceText(String value, String fallback, int maxLength) {
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        return limit(normalized, maxLength);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测 Trace 序列化失败");
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    normalized.add(value.trim());
                }
            }
            return List.copyOf(normalized);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测数据格式损坏");
        }
    }

    private AiRetrievalTraceResponse readTrace(String json) {
        if (!StringUtils.hasText(json)) {
            return AiRetrievalTraceResponse.unavailable("Trace 内容为空");
        }
        try {
            return objectMapper.readValue(json, AiRetrievalTraceResponse.class);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测 Trace 格式损坏");
        }
    }

    private String resolveErrorMessage(RuntimeException failure) {
        return StringUtils.hasText(failure.getMessage())
                ? failure.getMessage().trim()
                : failure.getClass().getSimpleName();
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void assertEvaluationAdmin() {
        accessGuard.assertCanRunGlobalKnowledgeOperation();
    }

    public record TraceRunView(
            String runId,
            String datasetId,
            String status,
            Integer topK,
            Integer totalQuestions,
            Integer completedQuestions,
            Double recallAtK,
            Double mrr,
            Double zeroHitRate,
            Long averageLatencyMs,
            Integer traceCount,
            String errorMessage,
            LocalDateTime startTime,
            LocalDateTime finishTime
    ) {
    }

    public record TraceView(
            String traceId,
            String runId,
            String resultId,
            String questionId,
            String configFingerprint,
            String mode,
            AiRetrievalTraceResponse trace,
            LocalDateTime createTime
    ) {
    }
}
