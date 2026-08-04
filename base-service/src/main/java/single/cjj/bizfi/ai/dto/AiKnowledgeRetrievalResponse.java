package single.cjj.bizfi.ai.dto;

import java.util.List;

public record AiKnowledgeRetrievalResponse(
        List<AiCitationResponse> citations,
        AiRetrievalTraceResponse trace
) {

    public AiKnowledgeRetrievalResponse {
        citations = citations == null ? List.of() : List.copyOf(citations);
        trace = trace == null ? AiRetrievalTraceResponse.unavailable() : trace;
    }
}
