package single.cjj.bizfi.ai.dto;

import java.util.List;

public record AiRetrievalTraceResponse(
        String mode,
        String configFingerprint,
        Integer requestedTopK,
        Integer candidateTopK,
        Boolean semanticEnabled,
        Boolean semanticAttempted,
        Boolean semanticSucceeded,
        String configuredVectorStore,
        String actualSemanticBackend,
        Boolean fallbackUsed,
        String fallbackReason,
        String embeddingModel,
        Double keywordWeight,
        Double semanticWeight,
        Integer rrfK,
        List<AiRetrievalCandidateTrace> keywordCandidates,
        List<AiRetrievalCandidateTrace> semanticCandidates,
        List<AiRetrievalCandidateTrace> fusedCandidates
) {

    public AiRetrievalTraceResponse {
        keywordCandidates = keywordCandidates == null ? List.of() : List.copyOf(keywordCandidates);
        semanticCandidates = semanticCandidates == null ? List.of() : List.copyOf(semanticCandidates);
        fusedCandidates = fusedCandidates == null ? List.of() : List.copyOf(fusedCandidates);
    }

    public static AiRetrievalTraceResponse unavailable() {
        return unavailable(null);
    }

    public static AiRetrievalTraceResponse unavailable(String reason) {
        return new AiRetrievalTraceResponse(
                "UNAVAILABLE",
                "unknown",
                0,
                0,
                false,
                false,
                false,
                "unknown",
                "none",
                false,
                reason,
                null,
                0D,
                0D,
                0,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
