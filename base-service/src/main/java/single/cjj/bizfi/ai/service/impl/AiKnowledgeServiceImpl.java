package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Service;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.util.Collections;
import java.util.List;

@Service
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds) {
        return Collections.emptyList();
    }
}
