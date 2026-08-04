package single.cjj.bizfi.ai.service.impl;

import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AiEvaluationMetrics {

    private AiEvaluationMetrics() {
    }

    public static QuestionMetrics calculate(
            List<AiCitationResponse> citations,
            Set<String> expectedDocIds,
            Set<String> expectedChunkIds
    ) {
        Set<String> normalizedChunks = normalize(expectedChunkIds);
        Set<String> normalizedDocs = normalize(expectedDocIds);
        boolean evaluateChunks = !normalizedChunks.isEmpty();
        Set<String> expected = evaluateChunks ? normalizedChunks : normalizedDocs;

        if (expected.isEmpty()) {
            throw new IllegalArgumentException("评测问题至少需要一个预期文档或分片");
        }

        List<AiCitationResponse> safeCitations = citations == null ? List.of() : citations;
        Set<String> matched = new HashSet<>();
        Integer firstRelevantRank = null;

        for (int index = 0; index < safeCitations.size(); index++) {
            AiCitationResponse citation = safeCitations.get(index);
            if (citation == null) {
                continue;
            }
            String candidate = evaluateChunks ? citation.getChunkId() : citation.getDocId();
            if (!StringUtils.hasText(candidate) || !expected.contains(candidate.trim())) {
                continue;
            }
            matched.add(candidate.trim());
            if (firstRelevantRank == null) {
                firstRelevantRank = index + 1;
            }
        }

        double recall = (double) matched.size() / expected.size();
        double reciprocalRank = firstRelevantRank == null ? 0D : 1D / firstRelevantRank;
        return new QuestionMetrics(recall, reciprocalRank, firstRelevantRank, matched.size(), expected.size());
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }

    public record QuestionMetrics(
            double recall,
            double reciprocalRank,
            Integer firstRelevantRank,
            int matchedCount,
            int expectedCount
    ) {
    }
}
