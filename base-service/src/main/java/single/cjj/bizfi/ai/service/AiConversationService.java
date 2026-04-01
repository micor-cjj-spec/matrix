package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;

import java.util.List;

public interface AiConversationService {

    BizfiAiConversation createConversation(Long userId, String title, String scene);

    BizfiAiConversation getByConversationId(String conversationId);

    BizfiAiConversation getOwnedConversation(Long userId, String conversationId);

    List<BizfiAiMessage> listMessages(Long userId, String conversationId);

    BizfiAiMessage saveUserMessage(String conversationId, String content);

    BizfiAiMessage saveAssistantMessage(String conversationId, String content, String model, String mode,
                                       String traceId, Integer promptTokens, Integer completionTokens, Integer totalTokens);
}
