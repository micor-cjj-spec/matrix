package single.cjj.bizfi.ai.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.*;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.service.AiChatService;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.AiModelFacade;
import single.cjj.bizfi.exception.BizException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiConversationService conversationService;

    @Autowired
    private AiKnowledgeService knowledgeService;

    @Autowired
    private AiModelFacade modelFacade;

    @Override
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new BizException("userMessage 不能为空");
        }

        String conversationId = request.getConversationId();
        if (!StringUtils.hasText(conversationId)) {
            BizfiAiConversation conversation = conversationService.createConversation(userId, "快速提问", "quick_chat");
            conversationId = conversation.getFconversationid();
        } else {
            conversationService.getOwnedConversation(userId, conversationId.trim());
            conversationId = conversationId.trim();
        }

        conversationService.saveUserMessage(conversationId, request.getUserMessage().trim());
        List<BizfiAiMessage> dbMessages = conversationService.listMessages(userId, conversationId);
        List<AiMessageResponse> historyMessages = dbMessages.stream()
                .map(item -> new AiMessageResponse(item.getFrole(), item.getFcontent()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<AiCitationResponse> citations = knowledgeService.retrieve(request.getUserMessage().trim(), request.getKbIds());
        List<String> knowledgeSnippets = citations.stream()
                .map(AiCitationResponse::getSnippet)
                .filter(StringUtils::hasText)
                .toList();

        AiModelResult modelResult = modelFacade.chat(new AiModelRequest(
                request.getUserMessage().trim(),
                historyMessages,
                knowledgeSnippets
        ));

        conversationService.saveAssistantMessage(
                conversationId,
                modelResult.getAnswer(),
                modelResult.getModel(),
                modelResult.getMode(),
                modelResult.getTraceId(),
                modelResult.getPromptTokens(),
                modelResult.getCompletionTokens(),
                modelResult.getTotalTokens()
        );

        AiUsageResponse usage = new AiUsageResponse(
                modelResult.getPromptTokens(),
                modelResult.getCompletionTokens(),
                modelResult.getTotalTokens(),
                modelResult.getEstimatedCost()
        );
        return new AiChatResponse(
                conversationId,
                modelResult.getAnswer(),
                citations,
                usage,
                modelResult.getTraceId(),
                modelResult.getMode(),
                modelResult.getModel()
        );
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return modelFacade.configStatus();
    }
}
