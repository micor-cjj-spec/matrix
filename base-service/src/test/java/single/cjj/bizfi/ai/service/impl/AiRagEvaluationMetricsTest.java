package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.dto.AiCitationResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRagEvaluationMetricsTest {

    private final AiRagEvaluationMetrics metrics = new AiRagEvaluationMetrics();

    @Test
    void shouldPreferChunkExpectationsAndCalculateRank() {
        AiRagEvaluationMetrics.CaseMetrics result = metrics.evaluate(
                List.of("doc-a"),
                List.of("chunk-b"),
                List.of(
                        citation("doc-a", "chunk-a"),
                        citation("doc-b", "chunk-b")
                )
        );

        assertTrue(result.hit());
        assertEquals(2, result.firstRelevantRank());
        assertEquals(0.5D, result.reciprocalRank());
        assertEquals(1.0D, result.recallAtK());
    }

    @Test
    void shouldCalculateDocumentRecallAcrossMultipleExpectedDocuments() {
        AiRagEvaluationMetrics.CaseMetrics result = metrics.evaluate(
                List.of("doc-a", "doc-b"),
                List.of(),
                List.of(citation("doc-b", "chunk-1"))
        );

        assertTrue(result.hit());
        assertEquals(1, result.firstRelevantRank());
        assertEquals(0.5D, result.recallAtK());
    }

    @Test
    void shouldReturnZeroMetricsWhenNothingMatches() {
        AiRagEvaluationMetrics.CaseMetrics result = metrics.evaluate(
                List.of("doc-a"),
                List.of(),
                List.of(citation("doc-b", "chunk-b"))
        );

        assertFalse(result.hit());
        assertEquals(null, result.firstRelevantRank());
        assertEquals(0D, result.reciprocalRank());
        assertEquals(0D, result.recallAtK());
    }

    @Test
    void shouldSummarizeHitMrrRecallAndP95Latency() {
        List<AiRagEvaluationMetrics.Observation> observations = List.of(
                new AiRagEvaluationMetrics.Observation(
                        metrics.evaluate(List.of("a"), List.of(), List.of(citation("a", "1"))),
                        10L
                ),
                new AiRagEvaluationMetrics.Observation(
                        metrics.evaluate(List.of("b"), List.of(), List.of(citation("x", "2"), citation("b", "3"))),
                        20L
                ),
                new AiRagEvaluationMetrics.Observation(
                        metrics.evaluate(List.of("c"), List.of(), List.of(citation("x", "4"))),
                        100L
                )
        );

        AiRagEvaluationMetrics.RunMetrics summary = metrics.summarize(observations);

        assertEquals(3, summary.caseCount());
        assertEquals(2, summary.hitCount());
        assertEquals(0.666667D, summary.hitAtK());
        assertEquals(0.5D, summary.mrr());
        assertEquals(0.666667D, summary.recallAtK());
        assertEquals(43.333333D, summary.averageLatencyMs());
        assertEquals(100L, summary.p95LatencyMs());
    }

    private AiCitationResponse citation(String docId, String chunkId) {
        return new AiCitationResponse(docId, docId, chunkId, "snippet");
    }
}
