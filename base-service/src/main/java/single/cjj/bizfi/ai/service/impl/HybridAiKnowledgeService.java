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
import single.cjj.bizfi.ai.dto.AiKnowledgeRetrievalResponse;
import single.cjj.bizfi.ai.dto.AiRetrievalCandidateTrace;
import single.cjj.bizfi.ai.dto.AiRetrievalTraceResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        return retrieveWithTrace(question, kbIds, topK).citations();
    }

    @Override
    public AiKnowledgeRetrievalResponse retrieveWithTrace(
            String question,
            List<String> kbIds,
            Integer topK
    ) {
        int limit = normalizeTopK(topK);
        int candidateTopK = Math.min(MAX_TOP_K, Math.max(limit * 4, limit));
        double keywordWeight = resolveWeight(properties.getHybridKeywordWeight());
        double semanticWeight = resolveWeight(properties.getHybridSemanticWeight());
        int rrfK = resolveRrfK();
        boolean semanticEnabled = Boolean.TRUE.equals(properties.getSemanticRetrievalEnabled());

        List<AiCitationResponse> keywordResults = safeList(
                keywordRetriever.retrieve(question, kbIds, candidateTopK)
        );

        SemanticOutcome semanticOutcome = SemanticOutcome.notAttempted(configuredVectorStore());
        if (semanticEnabled && StringUtils.hasText(question)) {
            try {
                semanticOutcome = semanticRetrieve(question.trim(), kbIds, candidateTopK);
            } catch (RuntimeException failure) {
                if (!Boolean.TRUE.equals(properties.getSemanticFailOpen())) {
                    throw failure;
                }
                log.warn("Semantic knowledge retrieval failed; falling back to keyword retrieval.", failure);
                semanticOutcome = SemanticOutcome.failed(
                        configuredVectorStore(),
                        resolveFailureMessage(failure)
                );
            }
        }

        FusionOutcome fusion = fuse(
                keywordResults,
                semanticOutcome.results(),
                limit,
                keywordWeight,
                semanticWeight,
                rrfK
        );

        String mode;
        if (!semanticEnabled || !StringUtils.hasText(question)) {
            mode = "KEYWORD_ONLY";
        } else if (!semanticOutcome.succeeded()) {
            mode = "KEYWORD_FALLBACK";
        } else if (semanticOutcome.results().isEmpty()) {
            mode = "KEYWORD_ONLY";
        } else {
            mode = "HYBRID_RRF";
        }

        AiRetrievalTraceResponse trace = new AiRetrievalTraceResponse(
                mode,
                configFingerprint(limit, candidateTopK, keywordWeight, semanticWeight, rrfK),
                limit,
                candidateTopK,
                semanticEnabled,
                semanticOutcome.attempted(),
                semanticOutcome.succeeded(),
                configuredVectorStore(),
                semanticOutcome.actualBackend(),
                semanticOutcome.fallbackUsed(),
                semanticOutcome.fallbackReason(),
                semanticOutcome.embeddingModel(),
                keywordWeight,
                semanticWeight,
                rrfK,
                fusion.keywordCandidates(),
                fusion.semanticCandidates(),
                fusion.fusedCandidates()
        );
        return new AiKnowledgeRetrievalResponse(fusion.citations(), trace);
    }

    private SemanticOutcome semanticRetrieve(String question, List<String> kbIds, int topK) {
        AiEmbeddingClient.EmbeddingBatch queryBatch = embeddingClient.embed(List.of(question));
        String embeddingModel = queryBatch.model();
        if (queryBatch.vectors() == null || queryBatch.vectors().isEmpty()) {
            return SemanticOutcome.succeeded(
                    List.of(),
                    configuredVectorStore(),
                    false,
                    null,
                    embeddingModel
            );
        }
        List<Double> queryVector = queryBatch.vectors().get(0);
        if (queryVector == null || queryVector.isEmpty()) {
            return SemanticOutcome.succeeded(
                    List.of(),
                    configuredVectorStore(),
                    false,
                    null,
                    embeddingModel
            );
        }

        if (!vectorStoreProperties.usePgVectorForRead()) {
            return SemanticOutcome.succeeded(
                    safeList(legacyJsonRetriever.retrieve(queryVector, embeddingModel, kbIds, topK)),
                    AiVectorStoreProperties.MYSQL_JSON,
                    false,
                    null,
                    embeddingModel
            );
        }

        PgVectorSemanticRetriever pgVectorRetriever = pgVectorRetrieverProvider.getIfAvailable();
        if (pgVectorRetriever == null) {
            return fallbackToLegacyOrThrow(
                    queryVector,
                    embeddingModel,
                    kbIds,
                    topK,
                    new IllegalStateException("PGVector 检索器未启用")
            );
        }
        try {
            return SemanticOutcome.succeeded(
                    safeList(pgVectorRetriever.retrieve(queryVector, embeddingModel, kbIds, topK)),
                    AiVectorStoreProperties.PGVECTOR,
                    false,
                    null,
                    embeddingModel
            );
        } catch (RuntimeException failure) {
            return fallbackToLegacyOrThrow(
                    queryVector,
                    embeddingModel,
                    kbIds,
                    topK,
                    failure
            );
        }
    }

    private SemanticOutcome fallbackToLegacyOrThrow(
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
        return SemanticOutcome.succeeded(
                safeList(legacyJsonRetriever.retrieve(queryVector, queryModel, kbIds, topK)),
                AiVectorStoreProperties.MYSQL_JSON,
                true,
                resolveFailureMessage(failure),
                queryModel
        );
    }

    private FusionOutcome fuse(
            List<AiCitationResponse> keywordResults,
            List<AiCitationResponse> semanticResults,
            int limit,
            double keywordWeight,
            double semanticWeight,
            int rrfK
    ) {
        Map<String, MutableCandidate> candidates = new LinkedHashMap<>();
        addRanked(candidates, keywordResults, keywordWeight, rrfK, true);
        addRanked(candidates, semanticResults, semanticWeight, rrfK, false);

        List<MutableCandidate> sorted = candidates.values().stream()
                .sorted((left, right) -> {
                    int scoreCompare = Double.compare(right.score, left.score);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    int docCompare = safe(left.citation.getDocId())
                            .compareTo(safe(right.citation.getDocId()));
                    if (docCompare != 0) {
                        return docCompare;
                    }
                    return safe(left.citation.getChunkId())
                            .compareTo(safe(right.citation.getChunkId()));
                })
                .toList();

        List<MutableCandidate> selected = sorted.stream().limit(limit).toList();
        Set<String> selectedKeys = selected.stream()
                .map(candidate -> candidate.key)
                .collect(Collectors.toSet());

        List<AiRetrievalCandidateTrace> keywordTrace = sourceTrace(
                keywordResults,
                "keyword",
                keywordWeight,
                rrfK,
                candidates,
                selectedKeys
        );
        List<AiRetrievalCandidateTrace> semanticTrace = sourceTrace(
                semanticResults,
                "semantic",
                semanticWeight,
                rrfK,
                candidates,
                selectedKeys
        );
        List<AiRetrievalCandidateTrace> fusedTrace = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            MutableCandidate candidate = sorted.get(index);
            fusedTrace.add(new AiRetrievalCandidateTrace(
                    "fused",
                    candidate.citation.getDocId(),
                    candidate.citation.getDocName(),
                    candidate.citation.getChunkId(),
                    candidate.citation.getSnippet(),
                    index + 1,
                    null,
                    candidate.score,
                    selectedKeys.contains(candidate.key)
            ));
        }

        return new FusionOutcome(
                selected.stream().map(candidate -> candidate.citation).toList(),
                keywordTrace,
                semanticTrace,
                fusedTrace
        );
    }

    private void addRanked(
            Map<String, MutableCandidate> candidates,
            List<AiCitationResponse> results,
            double weight,
            int rrfK,
            boolean keyword
    ) {
        for (int index = 0; index < results.size(); index++) {
            AiCitationResponse citation = results.get(index);
            if (citation == null) {
                continue;
            }
            String key = citationKey(citation, keyword ? "keyword" : "semantic", index);
            double contribution = weight / (rrfK + index + 1.0D);
            MutableCandidate candidate = candidates.computeIfAbsent(
                    key,
                    ignored -> new MutableCandidate(key, citation)
            );
            candidate.score += contribution;
            if (keyword) {
                candidate.keywordRank = index + 1;
            } else {
                candidate.semanticRank = index + 1;
            }
        }
    }

    private List<AiRetrievalCandidateTrace> sourceTrace(
            List<AiCitationResponse> results,
            String source,
            double weight,
            int rrfK,
            Map<String, MutableCandidate> candidates,
            Set<String> selectedKeys
    ) {
        List<AiRetrievalCandidateTrace> traces = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            AiCitationResponse citation = results.get(index);
            if (citation == null) {
                continue;
            }
            String key = citationKey(citation, source, index);
            MutableCandidate candidate = candidates.get(key);
            traces.add(new AiRetrievalCandidateTrace(
                    source,
                    citation.getDocId(),
                    citation.getDocName(),
                    citation.getChunkId(),
                    citation.getSnippet(),
                    index + 1,
                    weight / (rrfK + index + 1.0D),
                    candidate == null ? null : candidate.score,
                    selectedKeys.contains(key)
            ));
        }
        return List.copyOf(traces);
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

    private String configuredVectorStore() {
        return vectorStoreProperties.usePgVectorForRead()
                ? AiVectorStoreProperties.PGVECTOR
                : AiVectorStoreProperties.MYSQL_JSON;
    }

    private String configFingerprint(
            int topK,
            int candidateTopK,
            double keywordWeight,
            double semanticWeight,
            int rrfK
    ) {
        String source = String.join(
                "|",
                "trace-v1",
                "topK=" + topK,
                "candidateTopK=" + candidateTopK,
                "semanticEnabled=" + Boolean.TRUE.equals(properties.getSemanticRetrievalEnabled()),
                "semanticFailOpen=" + Boolean.TRUE.equals(properties.getSemanticFailOpen()),
                "semanticCandidateLimit=" + properties.getSemanticCandidateLimit(),
                "semanticMinScore=" + properties.getSemanticMinScore(),
                "keywordWeight=" + keywordWeight,
                "semanticWeight=" + semanticWeight,
                "rrfK=" + rrfK,
                "vectorStore=" + configuredVectorStore(),
                "readFallback=" + Boolean.TRUE.equals(vectorStoreProperties.getReadFallbackEnabled())
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 不可用", failure);
        }
    }

    private String resolveFailureMessage(RuntimeException failure) {
        if (failure == null) {
            return null;
        }
        return StringUtils.hasText(failure.getMessage())
                ? failure.getMessage().trim()
                : failure.getClass().getSimpleName();
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

    private static final class MutableCandidate {
        private final String key;
        private final AiCitationResponse citation;
        private double score;
        private Integer keywordRank;
        private Integer semanticRank;

        private MutableCandidate(String key, AiCitationResponse citation) {
            this.key = key;
            this.citation = citation;
        }
    }

    private record SemanticOutcome(
            List<AiCitationResponse> results,
            boolean attempted,
            boolean succeeded,
            String actualBackend,
            boolean fallbackUsed,
            String fallbackReason,
            String embeddingModel
    ) {
        private SemanticOutcome {
            results = results == null ? List.of() : List.copyOf(results);
        }

        private static SemanticOutcome notAttempted(String configuredBackend) {
            return new SemanticOutcome(
                    List.of(),
                    false,
                    false,
                    configuredBackend,
                    false,
                    null,
                    null
            );
        }

        private static SemanticOutcome failed(String configuredBackend, String reason) {
            return new SemanticOutcome(
                    List.of(),
                    true,
                    false,
                    configuredBackend,
                    false,
                    reason,
                    null
            );
        }

        private static SemanticOutcome succeeded(
                List<AiCitationResponse> results,
                String actualBackend,
                boolean fallbackUsed,
                String fallbackReason,
                String embeddingModel
        ) {
            return new SemanticOutcome(
                    results,
                    true,
                    true,
                    actualBackend,
                    fallbackUsed,
                    fallbackReason,
                    embeddingModel
            );
        }
    }

    private record FusionOutcome(
            List<AiCitationResponse> citations,
            List<AiRetrievalCandidateTrace> keywordCandidates,
            List<AiRetrievalCandidateTrace> semanticCandidates,
            List<AiRetrievalCandidateTrace> fusedCandidates
    ) {
    }
}
