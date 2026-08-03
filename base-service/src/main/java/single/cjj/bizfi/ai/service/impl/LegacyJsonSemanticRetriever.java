package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeDoc;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeDocMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LegacyJsonSemanticRetriever {

    private static final Set<String> BUILTIN_KB_ALIASES = Set.of("default", "all", "knowledge", "bizfi");
    private static final TypeReference<List<Double>> VECTOR_TYPE = new TypeReference<>() {
    };

    private final BizfiAiKnowledgeChunkMapper chunkMapper;
    private final BizfiAiKnowledgeDocMapper docMapper;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public LegacyJsonSemanticRetriever(
            BizfiAiKnowledgeChunkMapper chunkMapper,
            BizfiAiKnowledgeDocMapper docMapper,
            ObjectMapper objectMapper,
            AiProperties properties
    ) {
        this.chunkMapper = chunkMapper;
        this.docMapper = docMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<AiCitationResponse> retrieve(
            List<Double> queryVector,
            String queryModel,
            List<String> kbIds,
            int topK
    ) {
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }
        Set<String> allowedDocIds = resolveAllowedDocIds(kbIds);
        if (allowedDocIds != null && allowedDocIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(BizfiAiKnowledgeChunk::getFembedding)
                .ne(BizfiAiKnowledgeChunk::getFembedding, "");
        if (allowedDocIds != null) {
            wrapper.in(BizfiAiKnowledgeChunk::getFdocid, allowedDocIds);
        }
        wrapper.orderByDesc(BizfiAiKnowledgeChunk::getFembeddingtime)
                .orderByDesc(BizfiAiKnowledgeChunk::getFid)
                .last("limit " + resolveCandidateLimit());

        List<BizfiAiKnowledgeChunk> chunks = chunkMapper.selectList(wrapper);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> docIds = chunks.stream()
                .map(BizfiAiKnowledgeChunk::getFdocid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, BizfiAiKnowledgeDoc> activeDocs = loadActiveDocs(docIds);
        if (activeDocs.isEmpty()) {
            return List.of();
        }

        double minScore = resolveMinSemanticScore();
        List<SemanticCandidate> candidates = new ArrayList<>();
        for (BizfiAiKnowledgeChunk chunk : chunks) {
            BizfiAiKnowledgeDoc doc = activeDocs.get(chunk.getFdocid());
            if (doc == null || !isCompatibleModel(queryModel, chunk.getFembeddingmodel())) {
                continue;
            }
            List<Double> vector = readVector(chunk.getFembedding());
            if (vector.size() != queryVector.size()) {
                continue;
            }
            double similarity = cosineSimilarity(queryVector, vector);
            if (similarity < minScore) {
                continue;
            }
            candidates.add(new SemanticCandidate(
                    similarity,
                    new AiCitationResponse(
                            doc.getFdocid(),
                            doc.getFtitle(),
                            chunk.getFchunkid(),
                            buildSnippet(chunk.getFcontent())
                    )
            ));
        }

        return candidates.stream()
                .sorted(Comparator.comparingDouble(SemanticCandidate::similarity).reversed())
                .limit(topK)
                .map(SemanticCandidate::citation)
                .toList();
    }

    private Map<String, BizfiAiKnowledgeDoc> loadActiveDocs(Set<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return Map.of();
        }
        return docMapper.selectList(new LambdaQueryWrapper<BizfiAiKnowledgeDoc>()
                        .in(BizfiAiKnowledgeDoc::getFdocid, docIds)
                        .eq(BizfiAiKnowledgeDoc::getFstatus, "ACTIVE"))
                .stream()
                .collect(Collectors.toMap(
                        BizfiAiKnowledgeDoc::getFdocid,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private Set<String> resolveAllowedDocIds(List<String> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return null;
        }
        Set<String> normalized = kbIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(item -> !BUILTIN_KB_ALIASES.contains(item.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Double> readVector(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Double> vector = objectMapper.readValue(json, VECTOR_TYPE);
            return vector == null ? List.of() : vector;
        } catch (Exception failure) {
            if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                throw new IllegalStateException("知识向量解析失败", failure);
            }
            return List.of();
        }
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index) == null ? 0D : left.get(index);
            double rightValue = right.get(index) == null ? 0D : right.get(index);
            if (!Double.isFinite(leftValue) || !Double.isFinite(rightValue)) {
                return -1D;
            }
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return -1D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private boolean isCompatibleModel(String queryModel, String chunkModel) {
        return !StringUtils.hasText(queryModel)
                || !StringUtils.hasText(chunkModel)
                || queryModel.trim().equals(chunkModel.trim());
    }

    private String buildSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 280 ? normalized : normalized.substring(0, 280) + "...";
    }

    private int resolveCandidateLimit() {
        Integer configured = properties.getSemanticCandidateLimit();
        if (configured == null || configured <= 0) {
            return 500;
        }
        return Math.min(configured, 5000);
    }

    private double resolveMinSemanticScore() {
        Double configured = properties.getSemanticMinScore();
        if (configured == null || !Double.isFinite(configured)) {
            return 0.50D;
        }
        return Math.max(-1D, Math.min(configured, 1D));
    }

    private record SemanticCandidate(double similarity, AiCitationResponse citation) {
    }
}
