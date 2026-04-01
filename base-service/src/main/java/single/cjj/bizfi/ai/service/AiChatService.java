package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiChatResponse;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;

public interface AiChatService {

    AiChatResponse chat(Long userId, AiChatRequest request);

    AiConfigStatusResponse configStatus();
}
