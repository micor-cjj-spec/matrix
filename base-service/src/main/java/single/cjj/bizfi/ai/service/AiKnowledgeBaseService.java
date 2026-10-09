package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiKnowledgeBaseRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeBaseResponse;

import java.util.List;

public interface AiKnowledgeBaseService {
    List<AiKnowledgeBaseResponse> listBases(String status);

    AiKnowledgeBaseResponse createBase(AiKnowledgeBaseRequest request);

    AiKnowledgeBaseResponse updateBase(String kbId, AiKnowledgeBaseRequest request);

    boolean deleteBase(String kbId);
}
