package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationDataset;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationQuestion;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationResult;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationRun;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationDatasetMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationQuestionMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationResultMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationRunMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AiKnowledgeEvaluationService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final BizfiAiEvaluationDatasetMapper datasetMapper;
    private final BizfiAiEvaluationQuestionMapper questionMapper;
    private final BizfiAiEvaluationRunMapper runMapper;
    private final BizfiAiEvaluationResultMapper resultMapper;
    private final AiKnowledgeService knowledgeService;
    private final AiKnowledgeAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public AiKnowledgeEvaluationService(
            BizfiAiEvaluationDatasetMapper datasetMapper,
            BizfiAiEvaluationQuestionMapper questionMapper,
            BizfiAiEvaluationRunMapper runMapper,
            BizfiAiEvaluationResultMapper resultMapper,
            AiKnowledgeService knowledgeService,
            AiKnowledgeAccessGuard accessGuard,
            ObjectMapper objectMapper
    ) {
        this.datasetMapper = datasetMapper;
        this.questionMapper = questionMapper;
        this.runMapper = runMapper;
        this.resultMapper = resultMapper;
        this.knowledgeService = knowledgeService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public DatasetView createDataset(DatasetCommand command) {
        assertEvaluationAdmin();
        if (command == null || !StringUtils.hasText(command.name())) {
            throw new BizException("评测集名称不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        BizfiAiEvaluationDataset dataset = new BizfiAiEvaluationDataset();
        dataset.setFdatasetid(generateId("eval_ds_"));
        dataset.setFname(limit(command.name().trim(), 255));
        dataset.setFdescription(normalizeOptional(command.description(), 4000));
        dataset.setFstatus(normalizeStatus(command.status()));
        dataset.setFcreatetime(now);
        dataset.setFmodifytime(now);
        datasetMapper.insert(dataset);
        return toDatasetView(dataset);
    }

    public List<DatasetView> listDatasets() {
        assertEvaluationAdmin();
        return datasetMapper.selectList(new LambdaQueryWrapper<BizfiAiEvaluationDataset>()
                        .orderByDesc(BizfiAiEvaluationDataset::getFmodifytime)
                        .orderByDesc(BizfiAiEvaluationDataset::getFid))
                .stream()
                .map(this::toDatasetView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionView addQuestion(String datasetId, QuestionCommand command) {
        assertEvaluationAdmin();
        BizfiAiEvaluationDataset dataset = requireDataset(datasetId);
        if (!"ACTIVE".equals(dataset.getFstatus())) {
            throw new BizException("停用的评测集不能新增问题");
        }
        if (command == null || !StringUtils.hasText(command.question())) {
            throw new BizException("评测问题不能为空");
        }

        List<String> expectedDocIds = normalizeList(command.expectedDocIds());
        List<String> expectedChunkIds = normalizeList(command.expectedChunkIds());
        if (expectedDocIds.isEmpty() && expectedChunkIds.isEmpty()) {
            throw new BizException("至少需要配置一个预期文档或预期分片");
        }

        LocalDateTime now = LocalDateTime.now();
        BizfiAiEvaluationQuestion question = new BizfiAiEvaluationQuestion();
        question.setFquestionid(generateId("eval_q_"));
        question.setFdatasetid(dataset.getFdatasetid());
        question.setFquestion(command.question().trim());
        question.setFkbids(writeJson(defaultKnowledgeScope(command.kbIds())));
        question.setFexpecteddocids(writeJson(expectedDocIds));
        question.setFexpectedchunkids(writeJson(expectedChunkIds));
        question.setFexpectedanswer(normalizeOptional(command.expectedAnswer(), 20000));
        question.setFstatus(normalizeStatus(command.status()));
        question.setFcreatetime(now);
        question.setFmodifytime(now);
        questionMapper.insert(question);
        return toQuestionView(question);
    }

    public List<QuestionView> listQuestions(String datasetId) {
        assertEvaluationAdmin();
        requireDataset(datasetId);
        return questionMapper.selectList(new LambdaQueryWrapper<BizfiAiEvaluationQuestion>()
                        .eq(BizfiAiEvaluationQuestion::getFdatasetid, datasetId.trim())
                        .orderByAsc(BizfiAiEvaluationQuestion::getFid))
                .stream()
                .map(this::toQuestionView)
                .toList();
    }

    public RunView run(String datasetId, Integer requestedTopK) {
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
            AiEvaluationMetrics.QuestionMetrics metrics = null;
            String errorMessage = null;

            try {
                citations = knowledgeService.retrieve(
                        question.getFquestion(),
                        readStringList(question.getFkbids()),
                        topK
                );
                metrics = AiEvaluationMetrics.calculate(
                        citations,
                        new LinkedHashSet<>(readStringList(question.getFexpecteddocids())),
                        new LinkedHashSet<>(readStringList(question.getFexpectedchunkids()))
                );
            } catch (RuntimeException failure) {
                failureCount++;
                errorMessage = limit(resolveErrorMessage(failure), 1000);
                failures.add(question.getFquestionid() + ": " + errorMessage);
            }

            long latencyMs = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
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

            persistResult(run.getFrunid(), question, citations, metrics, latencyMs, errorMessage);
            run.setFcompletedquestions(run.getFcompletedquestions() + 1);
            run.setFmodifytime(LocalDateTime.now());
            runMapper.updateById(run);
        }

        int total = questions.size();
        run.setFrecallatk(recallSum / total);
        run.setFmrr(reciprocalRankSum / total);
        run.setFzerohitrate((double) zeroHitCount / total);
        run.setFavglatencyms(latencySum / total);
        run.setFstatus(failureCount == 0 ? "SUCCEEDED" : failureCount == total ? "FAILED" : "PARTIAL");
        run.setFerrormessage(failures.isEmpty() ? null : limit(String.join(" | ", failures), 1000));
        run.setFfinishtime(LocalDateTime.now());
        run.setFmodifytime(run.getFfinishtime());
        runMapper.updateById(run);
        return toRunView(run);
    }

    public RunView getRun(String runId) {
        assertEvaluationAdmin();
        return toRunView(requireRun(runId));
    }

    public List<ResultView> listResults(String runId) {
        assertEvaluationAdmin();
        requireRun(runId);
        return resultMapper.selectList(new LambdaQueryWrapper<BizfiAiEvaluationResult>()
                        .eq(BizfiAiEvaluationResult::getFrunid, runId.trim())
                        .orderByAsc(BizfiAiEvaluationResult::getFid))
                .stream()
                .map(this::toResultView)
                .toList();
    }

    private void persistResult(
            String runId,
            BizfiAiEvaluationQuestion question,
            List<AiCitationResponse> citations,
            AiEvaluationMetrics.QuestionMetrics metrics,
            long latencyMs,
            String errorMessage
    ) {
        BizfiAiEvaluationResult result = new BizfiAiEvaluationResult();
        result.setFresultid(generateId("eval_result_"));
        result.setFrunid(runId);
        result.setFquestionid(question.getFquestionid());
        result.setFcitationsjson(writeJson(citations == null ? List.of() : citations));
        result.setFfirstrelevantrank(metrics == null ? null : metrics.firstRelevantRank());
        result.setFrecall(metrics == null ? 0D : metrics.recall());
        result.setFreciprocalrank(metrics == null ? 0D : metrics.reciprocalRank());
        result.setFlatencyms(latencyMs);
        result.setFerrormessage(errorMessage);
        result.setFcreatetime(LocalDateTime.now());
        resultMapper.insert(result);
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

    private DatasetView toDatasetView(BizfiAiEvaluationDataset dataset) {
        return new DatasetView(
                dataset.getFdatasetid(),
                dataset.getFname(),
                dataset.getFdescription(),
                dataset.getFstatus(),
                dataset.getFcreatetime(),
                dataset.getFmodifytime()
        );
    }

    private QuestionView toQuestionView(BizfiAiEvaluationQuestion question) {
        return new QuestionView(
                question.getFquestionid(),
                question.getFdatasetid(),
                question.getFquestion(),
                readStringList(question.getFkbids()),
                readStringList(question.getFexpecteddocids()),
                readStringList(question.getFexpectedchunkids()),
                question.getFexpectedanswer(),
                question.getFstatus(),
                question.getFcreatetime(),
                question.getFmodifytime()
        );
    }

    private RunView toRunView(BizfiAiEvaluationRun run) {
        return new RunView(
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
                run.getFerrormessage(),
                run.getFstarttime(),
                run.getFfinishtime()
        );
    }

    private ResultView toResultView(BizfiAiEvaluationResult result) {
        return new ResultView(
                result.getFresultid(),
                result.getFrunid(),
                result.getFquestionid(),
                readCitations(result.getFcitationsjson()),
                result.getFfirstrelevantrank(),
                result.getFrecall(),
                result.getFreciprocalrank(),
                result.getFlatencyms(),
                result.getFerrormessage(),
                result.getFcreatetime()
        );
    }

    private List<String> defaultKnowledgeScope(List<String> kbIds) {
        List<String> normalized = normalizeList(kbIds);
        return normalized.isEmpty() ? List.of("all") : normalized;
    }

    private List<String> normalizeList(List<String> values) {
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
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, MAX_TOP_K));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "INACTIVE").contains(normalized)) {
            throw new BizException("评测状态仅支持 ACTIVE 或 INACTIVE");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        return StringUtils.hasText(value) ? limit(value.trim(), maxLength) : null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测数据序列化失败");
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return normalizeList(values);
        } catch (JsonProcessingException failure) {
            throw new BizException("评测数据格式损坏");
        }
    }

    private List<AiCitationResponse> readCitations(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiCitationResponse>>() {
            });
        } catch (JsonProcessingException failure) {
            throw new BizException("评测结果格式损坏");
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

    public record DatasetCommand(String name, String description, String status) {
    }

    public record QuestionCommand(
            String question,
            List<String> kbIds,
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            String expectedAnswer,
            String status
    ) {
    }

    public record DatasetView(
            String datasetId,
            String name,
            String description,
            String status,
            LocalDateTime createTime,
            LocalDateTime modifyTime
    ) {
    }

    public record QuestionView(
            String questionId,
            String datasetId,
            String question,
            List<String> kbIds,
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            String expectedAnswer,
            String status,
            LocalDateTime createTime,
            LocalDateTime modifyTime
    ) {
    }

    public record RunView(
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
            String errorMessage,
            LocalDateTime startTime,
            LocalDateTime finishTime
    ) {
    }

    public record ResultView(
            String resultId,
            String runId,
            String questionId,
            List<AiCitationResponse> citations,
            Integer firstRelevantRank,
            Double recall,
            Double reciprocalRank,
            Long latencyMs,
            String errorMessage,
            LocalDateTime createTime
    ) {
    }
}
