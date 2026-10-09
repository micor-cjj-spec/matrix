package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiToolContext;

public interface AiToolPolicyService {

    AiToolContext prepareContext(Long userId, AiChatRequest request);
}
