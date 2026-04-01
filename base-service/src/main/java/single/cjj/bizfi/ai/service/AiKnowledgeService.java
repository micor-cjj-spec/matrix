package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiCitationResponse;

import java.util.List;

public interface AiKnowledgeService {

    List<AiCitationResponse> retrieve(String question, List<String> kbIds);
}
