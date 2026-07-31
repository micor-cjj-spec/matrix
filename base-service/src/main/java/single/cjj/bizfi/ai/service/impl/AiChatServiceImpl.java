package single.cjj.bizfi.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiChatResponse;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiMessageResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.dto.AiUsageResponse;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.service.AiChatService;
import single.cjj.bizfi.ai.service.AiChatStreamObserver;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.AiModelFacade;
import single.cjj.bizfi.exception.BizException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final AiConversationService conversationService;
    private final AiKnowledgeService knowledgeService;
    private final AiModelFacade modelFacade;
    private final AiProperties aiProperties;

    public AiChatServiceImpl(
            AiConversationService conversationService,
            AiKnowledgeService knowledgeService,
            AiModelFacade modelFacade,
            AiProperties aiProperties
    ) {
        this.conversationService = conversationService;
        this.knowledgeService = knowledgeService;
        this.modelFacade = modelFacade;
        this.aiProperties = aiProperties;
    }

    @Override
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        PreparedChat prepared = prepareChat(userId, request);
        AiModelResult modelResult = modelFacade.chat(prepared.modelRequest());
        return completeChat(prepared, modelResult);
    }

    @Override
    public AiChatResponse stream(Long userId, AiChatRequest request, AiChatStreamObserver observer) {
        Objects.requireNonNull(observer, "stream observer 不能为空");
        PreparedChat prepared = prepareChat(userId, request);
        observer.onStart(prepared.conversationId(), prepared.citations(), modelFacade.configStatus());
        AiModelResult modelResult = modelFacade.stream(prepared.modelRequest(), observer::onDelta);
        return completeChat(prepared, modelResult);
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return modelFacade.configStatus();
    }

    private PreparedChat prepareChat(Long userId, AiChatRequest request) {
        if (Boolean.FALSE.equals(aiProperties.getEnabled())) {
            throw new BizException("AI 能力当前未启用");
        }
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new BizException("userMessage 不能为空");
        }

        String userMessage = request.getUserMessage().trim();
        String conversationId = resolveConversationId(userId, request.getConversationId());

        conversationService.saveUserMessage(conversationId, userMessage);
        List<AiMessageResponse> historyMessages = loadHistoryMessages(userId, conversationId, userMessage);
        List<AiCitationResponse> citations = retrieveKnowledge(userMessage, request.getKbIds());
        List<String> knowledgeSnippets = citations.stream()
                .map(AiCitationResponse::getSnippet)
                .filter(StringUtils::hasText)
                .toList();

        return new PreparedChat(
                conversationId,
                citations,
                new AiModelRequest(userMessage, historyMessages, knowledgeSnippets)
        );
    }

    private String resolveConversationId(Long userId, String requestedConversationId) {
        if (!StringUtils.hasText(requestedConversationId)) {
            BizfiAiConversation conversation = conversationService.createConversation(
                    userId,
                    "快速提问",
                    "quick_chat"
            );
            return conversation.getFconversationid();
        }

        String conversationId = requestedConversationId.trim();
        conversationService.getOwnedConversation(userId, conversationId);
        return conversationId;
    }

    private List<AiMessageResponse> loadHistoryMessages(
            Long userId,
            String conversationId,
            String currentQuestion
    ) {
        List<BizfiAiMessage> dbMessages = conversationService.listMessages(userId, conversationId);
        if (dbMessages == null || dbMessages.isEmpty()) {
            return List.of();
        }

        List<AiMessageResponse> history = dbMessages.stream()
                .map(item -> new AiMessageResponse(item.getFrole(), item.getFcontent()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // 用户消息已在本次调用开始时落库；模型请求末尾还会追加当前问题，
        // 因此需要从历史中移除刚保存的最后一条，避免同一句问题重复发送。
        if (!history.isEmpty()) {
            AiMessageResponse last = history.get(history.size() - 1);
            if ("user".equals(last.getRole()) && currentQuestion.equals(last.getContent())) {
                history.remove(history.size() - 1);
            }
        }

        int maxHistory = normalizePositive(aiProperties.getMaxHistoryMessages(), 20);
        if (history.size() <= maxHistory) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - maxHistory, history.size()));
    }

    private List<AiCitationResponse> retrieveKnowledge(String question, List<String> kbIds) {
        if (Boolean.FALSE.equals(aiProperties.getKnowledgeEnabled())) {
            return List.of();
        }
        int topK = normalizePositive(aiProperties.getMaxKnowledgeChunks(), 5);
        List<AiCitationResponse> citations = knowledgeService.retrieve(question, kbIds, topK);
        return citations == null ? List.of() : citations;
    }

    private AiChatResponse completeChat(PreparedChat prepared, AiModelResult modelResult) {
        if (modelResult == null || !StringUtils.hasText(modelResult.getAnswer())) {
            throw new BizException("AI 模型返回为空");
        }

        conversationService.saveAssistantMessage(
                prepared.conversationId(),
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
                prepared.conversationId(),
                modelResult.getAnswer(),
                prepared.citations(),
                usage,
                modelResult.getTraceId(),
                modelResult.getMode(),
                modelResult.getModel()
        );
    }

    private int normalizePositive(Integer configured, int defaultValue) {
        return configured != null && configured > 0 ? configured : defaultValue;
    }

    private record PreparedChat(
            String conversationId,
            List<AiCitationResponse> citations,
            AiModelRequest modelRequest
    ) {
    }
}
