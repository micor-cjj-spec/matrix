package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.ai.dto.AiCitationResponse;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiEvaluationMetricsTest {

    @Test
    void shouldPreferChunkLevelGroundTruthAndCalculateRecallAndMrr() {
        List<AiCitationResponse> citations = List.of(
                new AiCitationResponse("doc-a", "A", "chunk-x", "x"),
                new AiCitationResponse("doc-b", "B", "chunk-2", "two"),
                new AiCitationResponse("doc-a", "A", "chunk-1", "one")
        );

        AiEvaluationMetrics.QuestionMetrics metrics = AiEvaluationMetrics.calculate(
                citations,
                Set.of("doc-a"),
                Set.of("chunk-1", "chunk-2")
        );

        assertEquals(1D, metrics.recall());
        assertEquals(0.5D, metrics.reciprocalRank());
        assertEquals(2, metrics.firstRelevantRank());
        assertEquals(2, metrics.matchedCount());
        assertEquals(2, metrics.expectedCount());
    }

    @Test
    void shouldEvaluateDocumentsWhenChunkGroundTruthIsAbsent() {
        List<AiCitationResponse> citations = List.of(
                new AiCitationResponse("doc-x", "X", "chunk-x", "x"),
                new AiCitationResponse("doc-a", "A", "chunk-a", "a")
        );

        AiEvaluationMetrics.QuestionMetrics metrics = AiEvaluationMetrics.calculate(
                citations,
                Set.of("doc-a", "doc-b"),
                Set.of()
        );

        assertEquals(0.5D, metrics.recall());
        assertEquals(0.5D, metrics.reciprocalRank());
        assertEquals(2, metrics.firstRelevantRank());
    }

    @Test
    void shouldReportZeroHitWithoutRelevantCitation() {
        AiEvaluationMetrics.QuestionMetrics metrics = AiEvaluationMetrics.calculate(
                List.of(new AiCitationResponse("doc-x", "X", "chunk-x", "x")),
                Set.of("doc-a"),
                Set.of()
        );

        assertEquals(0D, metrics.recall());
        assertEquals(0D, metrics.reciprocalRank());
        assertNull(metrics.firstRelevantRank());
    }

    @Test
    void shouldRejectQuestionWithoutGroundTruth() {
        assertThrows(IllegalArgumentException.class, () -> AiEvaluationMetrics.calculate(
                List.of(),
                Set.of(),
                Set.of()
        ));
    }
}
