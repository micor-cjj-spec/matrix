package single.cjj.bizfi.ai.dto;

public record AiRetrievalCandidateTrace(
        String source,
        String docId,
        String docName,
        String chunkId,
        String snippet,
        Integer rank,
        Double rrfContribution,
        Double fusedScore,
        Boolean selected
) {
}
