package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationDataset;
import single.cjj.bizfi.ai.entity.BizfiAiEvaluationQuestion;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationDatasetMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiEvaluationQuestionMapper;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiEvaluationQuestionDraftService {

    private static final int MAX_BULK_QUESTIONS = 200;

    private final BizfiAiEvaluationDatasetMapper datasetMapper;
    private final BizfiAiEvaluationQuestionMapper questionMapper;
    private final AiKnowledgeAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public AiEvaluationQuestionDraftService(
            BizfiAiEvaluationDatasetMapper datasetMapper,
            BizfiAiEvaluationQuestionMapper questionMapper,
            AiKnowledgeAccessGuard accessGuard,
            ObjectMapper objectMapper
    ) {
        this.datasetMapper = datasetMapper;
        this.questionMapper = questionMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public BulkImportView bulkImport(String datasetId, List<QuestionCommand> commands) {
        assertEvaluationAdmin();
        BizfiAiEvaluationDataset dataset = requireActiveDataset(datasetId);
        if (commands == null || commands.isEmpty()) {
            throw new BizException("批量导入问题不能为空");
        }
        if (commands.size() > MAX_BULK_QUESTIONS) {
            throw new BizException("单次最多导入 " + MAX_BULK_QUESTIONS + " 条评测问题");
        }

        Set<String> existingQuestions = questionMapper.selectList(
                        new LambdaQueryWrapper<BizfiAiEvaluationQuestion>()
                                .eq(BizfiAiEvaluationQuestion::getFdatasetid, dataset.getFdatasetid())
                ).stream()
                .map(BizfiAiEvaluationQuestion::getFquestion)
                .filter(StringUtils::hasText)
                .map(this::questionKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int imported = 0;
        int skipped = 0;
        int rejected = 0;
        List<BulkImportItem> items = new ArrayList<>();

        for (int index = 0; index < commands.size(); index++) {
            QuestionCommand command = commands.get(index);
            try {
                PreparedQuestion prepared = prepare(command, null);
                String key = questionKey(prepared.question());
                if (!existingQuestions.add(key)) {
                    skipped++;
                    items.add(new BulkImportItem(index + 1, null, prepared.question(), "SKIPPED", "评测问题已存在"));
                    continue;
                }

                BizfiAiEvaluationQuestion question = newQuestion(dataset.getFdatasetid(), prepared);
                questionMapper.insert(question);
                imported++;
                items.add(new BulkImportItem(index + 1, question.getFquestionid(), question.getFquestion(), "IMPORTED", null));
            } catch (RuntimeException failure) {
                rejected++;
                items.add(new BulkImportItem(
                        index + 1,
                        null,
                        command == null ? null : command.question(),
                        "REJECTED",
                        resolveErrorMessage(failure)
                ));
            }
        }

        return new BulkImportView(commands.size(), imported, skipped, rejected, items);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionView updateQuestion(String datasetId, String questionId, QuestionCommand command) {
        assertEvaluationAdmin();
        BizfiAiEvaluationDataset dataset = requireActiveDataset(datasetId);
        BizfiAiEvaluationQuestion current = requireQuestion(dataset.getFdatasetid(), questionId);
        PreparedQuestion prepared = prepare(command, current);

        current.setFquestion(prepared.question());
        current.setFkbids(writeJson(prepared.kbIds()));
        current.setFexpecteddocids(writeJson(prepared.expectedDocIds()));
        current.setFexpectedchunkids(writeJson(prepared.expectedChunkIds()));
        current.setFexpectedanswer(prepared.expectedAnswer());
        current.setFstatus(prepared.status());
        current.setFmodifytime(LocalDateTime.now());
        questionMapper.updateById(current);
        return toView(current);
    }

    private PreparedQuestion prepare(QuestionCommand command, BizfiAiEvaluationQuestion current) {
        if (command == null) {
            throw new BizException("评测问题不能为空");
        }

        String question = StringUtils.hasText(command.question())
                ? limit(command.question().trim(), 2000)
                : current == null ? null : current.getFquestion();
        if (!StringUtils.hasText(question)) {
            throw new BizException("评测问题不能为空");
        }

        List<String> kbIds = command.kbIds() == null && current != null
                ? readStringList(current.getFkbids())
                : defaultKnowledgeScope(command.kbIds());
        List<String> expectedDocIds = command.expectedDocIds() == null && current != null
                ? readStringList(current.getFexpecteddocids())
                : normalizeList(command.expectedDocIds());
        List<String> expectedChunkIds = command.expectedChunkIds() == null && current != null
                ? readStringList(current.getFexpectedchunkids())
                : normalizeList(command.expectedChunkIds());
        String expectedAnswer = command.expectedAnswer() == null && current != null
                ? current.getFexpectedanswer()
                : normalizeOptional(command.expectedAnswer(), 20000);

        boolean hasGroundTruth = !expectedDocIds.isEmpty() || !expectedChunkIds.isEmpty();
        String requestedStatus = command.status() == null && current != null ? current.getFstatus() : command.status();
        String status = resolveQuestionStatus(requestedStatus, hasGroundTruth);

        return new PreparedQuestion(
                question,
                kbIds,
                expectedDocIds,
                expectedChunkIds,
                expectedAnswer,
                status
        );
    }

    static String resolveQuestionStatus(String requestedStatus, boolean hasGroundTruth) {
        if (!StringUtils.hasText(requestedStatus)) {
            return hasGroundTruth ? "ACTIVE" : "INACTIVE";
        }
        String normalized = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "INACTIVE").contains(normalized)) {
            throw new BizException("评测状态仅支持 ACTIVE 或 INACTIVE");
        }
        if ("ACTIVE".equals(normalized) && !hasGroundTruth) {
            throw new BizException("激活评测问题前必须绑定预期文档或预期分片");
        }
        return normalized;
    }

    private BizfiAiEvaluationQuestion newQuestion(String datasetId, PreparedQuestion prepared) {
        LocalDateTime now = LocalDateTime.now();
        BizfiAiEvaluationQuestion question = new BizfiAiEvaluationQuestion();
        question.setFquestionid(generateId("eval_q_"));
        question.setFdatasetid(datasetId);
        question.setFquestion(prepared.question());
        question.setFkbids(writeJson(prepared.kbIds()));
        question.setFexpecteddocids(writeJson(prepared.expectedDocIds()));
        question.setFexpectedchunkids(writeJson(prepared.expectedChunkIds()));
        question.setFexpectedanswer(prepared.expectedAnswer());
        question.setFstatus(prepared.status());
        question.setFcreatetime(now);
        question.setFmodifytime(now);
        return question;
    }

    private BizfiAiEvaluationDataset requireActiveDataset(String datasetId) {
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
        if (!"ACTIVE".equals(dataset.getFstatus())) {
            throw new BizException("停用的评测集不能维护问题");
        }
        return dataset;
    }

    private BizfiAiEvaluationQuestion requireQuestion(String datasetId, String questionId) {
        if (!StringUtils.hasText(questionId)) {
            throw new BizException("评测问题编号不能为空");
        }
        BizfiAiEvaluationQuestion question = questionMapper.selectOne(
                new LambdaQueryWrapper<BizfiAiEvaluationQuestion>()
                        .eq(BizfiAiEvaluationQuestion::getFdatasetid, datasetId)
                        .eq(BizfiAiEvaluationQuestion::getFquestionid, questionId.trim())
                        .last("limit 1")
        );
        if (question == null) {
            throw new BizException("评测问题不存在");
        }
        return question;
    }

    private QuestionView toView(BizfiAiEvaluationQuestion question) {
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

    private String normalizeOptional(String value, int maxLength) {
        return StringUtils.hasText(value) ? limit(value.trim(), maxLength) : null;
    }

    private String questionKey(String question) {
        return question.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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

    private record PreparedQuestion(
            String question,
            List<String> kbIds,
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            String expectedAnswer,
            String status
    ) {
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

    public record BulkImportView(
            int total,
            int imported,
            int skipped,
            int rejected,
            List<BulkImportItem> items
    ) {
    }

    public record BulkImportItem(
            int row,
            String questionId,
            String question,
            String status,
            String message
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
}
