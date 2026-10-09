package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeRetrievalResponse;
import single.cjj.bizfi.ai.dto.AiRetrievalTraceResponse;

import java.util.List;

public interface AiKnowledgeService {

    List<AiCitationResponse> retrieve(String question, List<String> kbIds);

    /**
     * 带召回数量限制的知识检索。
     *
     * <p>旧实现可以继续使用两参数方法；支持 Top-K 的实现应覆盖该方法。</p>
     */
    default List<AiCitationResponse> retrieve(String question, List<String> kbIds, Integer topK) {
        return retrieve(question, kbIds);
    }

    /**
     * 返回检索结果以及可诊断的候选、融合和降级信息。
     *
     * <p>普通检索实现无需强制支持 Trace；默认实现保持向后兼容。</p>
     */
    default AiKnowledgeRetrievalResponse retrieveWithTrace(
            String question,
            List<String> kbIds,
            Integer topK
    ) {
        return new AiKnowledgeRetrievalResponse(
                retrieve(question, kbIds, topK),
                AiRetrievalTraceResponse.unavailable("当前检索实现未提供 Trace")
        );
    }
}
