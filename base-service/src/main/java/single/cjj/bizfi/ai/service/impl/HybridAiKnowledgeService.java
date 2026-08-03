package single.cjj.bizfi.ai.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.config.AiVectorStoreProperties;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class HybridAiKnowledgeService implements AiKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(HybridAiKnowledgeService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final AiKnowledgeService keywordRetriever;
    private final AiEmbeddingClient embeddingClient;
    private final LegacyJsonSemanticRetriever legacyJsonRetriever;
    private final ObjectProvider<PgVectorSemanticRetriever> pgVectorRetrieverProvider;
    private final AiProperties properties;
    private final AiVectorStoreProperties vectorStoreProperties;

    public HybridAiKnowledgeService(
            @Qualifier("aiKnowledgeManagementService") AiKnowledgeService keywordRetriever,
            AiEmbeddingClient embeddingClient,
            LegacyJsonSemanticRetriever legacyJsonRetriever,
            ObjectProvider<PgVectorSemanticRetriever> pgVectorRetrieverProvider,
            AiProperties properties,
            AiVectorStoreProperties vectorStoreProperties
    ) {
        this.keywordRetriever = keywordRetriever;
        this.embeddingClient = embeddingClient;
        this.legacyJsonRetriever = legacyJsonRetriever;
        this.pgVectorRetrieverProvider = pgVectorRetrieverProvider;
        this.properties = properties;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds) {
        return retrieve(question, kbIds, DEFAULT_TOP_K);
    }

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds, Integer topK) {
        int limit = normalizeTopK(topK);
        int candidateTopK = Math.min(MAX_TOP_K, Math.max(limit * 4, limit));
        List<AiCitationResponse> keywordResults = safeList(
                keywordRetriever.retrieve(question, kbIds, candidateTopK)
        );

        if (!Boolean.TRUE.equals(properties.getSemanticRetrievalEnabled()) || !StringUtils.hasText(question)) {
            return keywordResults.stream().limit(limit).toList();
        }

        List<AiCitationResponse> semanticResults;
        try {
            semanticResults = semanticRetrieve(question.trim(), kbIds, candidateTopK);
        } catch (RuntimeException failure) {
            if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                throw failure;
            }
            log.warn("Semantic knowledge retrieval failed; falling back to keyword retrieval.", failure);
            semanticResults = List.of();
        }

        if (semanticResults.isEmpty()) {
            return keywordResults.stream().limit(limit).toList();
        }
        return fuse(keywordResults, semanticResults, limit);
    }

    private List<AiCitationResponse> semanticRetrieve(String question, List<String> kbIds, int topK) {
        AiEmbeddingClient.EmbeddingBatch queryBatch = embeddingClient.embed(List.of(question));
        if (queryBatch.vectors() == null || queryBatch.vectors().isEmpty()) {
            return List.of();
        }
        List<Double> queryVector = queryBatch.vectors().get(0);
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }

        if (!vectorStoreProperties.usePgVectorForRead()) {
            return legacyJsonRetriever.retrieve(queryVector, queryBatch.model(), kbIds, topK);
        }

        PgVectorSemanticRetriever pgVectorRetriever = pgVectorRetrieverProvider.getIfAvailable();
        if (pgVectorRetriever == null) {
            return fallbackToLegacyOrThrow(
                    queryVector,
                    queryBatch.model(),
                    kbIds,
                    topK,
                    new IllegalStateException("PGVector 检索器未启用")
            );
        }
        try {
            return pgVectorRetriever.retrieve(queryVector, queryBatch.model(), kbIds, topK);
        } catch (RuntimeException failure) {
            return fallbackToLegacyOrThrow(
                    queryVector,
                    queryBatch.model(),
                    kbIds,
                    topK,
                    failure
            );
        }
    }

    private List<AiCitationResponse> fallbackToLegacyOrThrow(
            List<Double> queryVector,
            String queryModel,
            List<String> kbIds,
            int topK,
            RuntimeException failure
    ) {
        if (!Boolean.TRUE.equals(vectorStoreProperties.getReadFallbackEnabled())) {
            throw failure;
        }
        log.warn("PGVector retrieval failed; using MySQL JSON semantic fallback.", failure);
        return legacyJsonRetriever.retrieve(queryVector, queryModel, kbIds, topK);
    }

    private List<AiCitationResponse> fuse(
            List<AiCitationResponse> keywordResults,
            List<AiCitationResponse> semanticResults,
            int limit
    ) {
        Map<String, FusedCandidate> candidates = new LinkedHashMap<>();
        addRanked(candidates, keywordResults, resolveWeight(properties.getHybridKeywordWeight()), "keyword");
        addRanked(candidates, semanticResults, resolveWeight(properties.getHybridSemanticWeight()), "semantic");
        return candidates.values().stream()
                .sorted((left, right) -> {
                    int scoreCompare = Double.compare(right.score(), left.score());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    int docCompare = safe(left.citation().getDocId()).compareTo(safe(right.citation().getDocId()));
                    if (docCompare != 0) {
                        return docCompare;
                    }
                    return safe(left.citation().getChunkId()).compareTo(safe(right.citation().getChunkId()));
                })
                .limit(limit)
                .map(FusedCandidate::citation)
                .toList();
    }

    private void addRanked(
            Map<String, FusedCandidate> candidates,
            List<AiCitationResponse> results,
            double weight,
            String source
    ) {
        int rrfK = resolveRrfK();
        for (int index = 0; index < results.size(); index++) {
            AiCitationResponse citation = results.get(index);
            if (citation == null) {
                continue;
            }
            String key = citationKey(citation, source, index);
            double contribution = weight / (rrfK + index + 1.0D);
            FusedCandidate existing = candidates.get(key);
            if (existing == null) {
                candidates.put(key, new FusedCandidate(citation, contribution));
            } else {
                candidates.put(key, new FusedCandidate(existing.citation(), existing.score() + contribution));
            }
        }
    }

    private String citationKey(AiCitationResponse citation, String source, int index) {
        if (StringUtils.hasText(citation.getChunkId())) {
            return citation.getChunkId().trim();
        }
        if (StringUtils.hasText(citation.getDocId())) {
            return citation.getDocId().trim() + ":" + safe(citation.getSnippet());
        }
        return source + ":" + index;
    }

    private List<AiCitationResponse> safeList(List<AiCitationResponse> results) {
        return results == null ? List.of() : results;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private double resolveWeight(Double configured) {
        if (configured == null || !Double.isFinite(configured) || configured < 0D) {
            return 1D;
        }
        return configured;
    }

    private int resolveRrfK() {
        Integer configured = properties.getHybridRrfK();
        return configured != null && configured > 0 ? configured : 60;
    }

    private record FusedCandidate(AiCitationResponse citation, double score) {
    }
}
