package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiRagEvaluationMetrics {

    public CaseMetrics evaluate(
            List<String> expectedDocIds,
            List<String> expectedChunkIds,
            List<AiCitationResponse> retrieved
    ) {
        List<String> expectedDocs = normalize(expectedDocIds);
        List<String> expectedChunks = normalize(expectedChunkIds);
        boolean matchChunks = !expectedChunks.isEmpty();
        Set<String> expected = new LinkedHashSet<>(matchChunks ? expectedChunks : expectedDocs);

        List<String> retrievedDocs = new ArrayList<>();
        List<String> retrievedChunks = new ArrayList<>();
        Set<String> matched = new LinkedHashSet<>();
        Integer firstRank = null;

        List<AiCitationResponse> safeRetrieved = retrieved == null ? List.of() : retrieved;
        for (int index = 0; index < safeRetrieved.size(); index++) {
            AiCitationResponse citation = safeRetrieved.get(index);
            if (citation == null) {
                continue;
            }
            addDistinct(retrievedDocs, citation.getDocId());
            addDistinct(retrievedChunks, citation.getChunkId());
            String candidate = matchChunks ? normalizeValue(citation.getChunkId()) : normalizeValue(citation.getDocId());
            if (candidate != null && expected.contains(candidate)) {
                matched.add(candidate);
                if (firstRank == null) {
                    firstRank = index + 1;
                }
            }
        }

        boolean hit = firstRank != null;
        double reciprocalRank = hit ? 1.0D / firstRank : 0.0D;
        double recallAtK = expected.isEmpty() ? 0.0D : (double) matched.size() / expected.size();
        return new CaseMetrics(
                hit,
                firstRank,
                reciprocalRank,
                recallAtK,
                List.copyOf(retrievedDocs),
                List.copyOf(retrievedChunks)
        );
    }

    public RunMetrics summarize(List<Observation> observations) {
        List<Observation> safe = observations == null ? List.of() : observations;
        if (safe.isEmpty()) {
            return new RunMetrics(0, 0, 0D, 0D, 0D, 0D, 0L);
        }
        int hits = 0;
        double reciprocalRank = 0D;
        double recall = 0D;
        double latency = 0D;
        List<Long> latencies = new ArrayList<>();
        for (Observation observation : safe) {
            if (observation == null || observation.metrics() == null) {
                continue;
            }
            if (observation.metrics().hit()) {
                hits++;
            }
            reciprocalRank += observation.metrics().reciprocalRank();
            recall += observation.metrics().recallAtK();
            long caseLatency = Math.max(0L, observation.latencyMs());
            latency += caseLatency;
            latencies.add(caseLatency);
        }
        int count = latencies.size();
        if (count == 0) {
            return new RunMetrics(0, 0, 0D, 0D, 0D, 0D, 0L);
        }
        latencies.sort(Long::compareTo);
        int p95Index = Math.max(0, (int) Math.ceil(count * 0.95D) - 1);
        return new RunMetrics(
                count,
                hits,
                round((double) hits / count),
                round(reciprocalRank / count),
                round(recall / count),
                round(latency / count),
                latencies.get(p95Index)
        );
    }

    private List<String> normalize(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream().map(this::normalizeValue).filter(value -> value != null).forEach(normalized::add);
        }
        return List.copyOf(normalized);
    }

    private String normalizeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void addDistinct(List<String> target, String value) {
        String normalized = normalizeValue(value);
        if (normalized != null && !target.contains(normalized)) {
            target.add(normalized);
        }
    }

    private double round(double value) {
        return Math.round(value * 1_000_000D) / 1_000_000D;
    }

    public record CaseMetrics(
            boolean hit,
            Integer firstRelevantRank,
            double reciprocalRank,
            double recallAtK,
            List<String> retrievedDocIds,
            List<String> retrievedChunkIds
    ) {
    }

    public record Observation(CaseMetrics metrics, long latencyMs) {
    }

    public record RunMetrics(
            int caseCount,
            int hitCount,
            double hitAtK,
            double mrr,
            double recallAtK,
            double averageLatencyMs,
            long p95LatencyMs
    ) {
    }
}
