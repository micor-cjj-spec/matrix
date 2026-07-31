package single.cjj.bizfi.ai.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.ai.config.AiProperties;
import single.cjj.bizfi.ai.dto.AiChatRequest;
import single.cjj.bizfi.ai.dto.AiChatResponse;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiMessageResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;
import single.cjj.bizfi.ai.entity.BizfiAiConversation;
import single.cjj.bizfi.ai.entity.BizfiAiMessage;
import single.cjj.bizfi.ai.service.AiChatStreamObserver;
import single.cjj.bizfi.ai.service.AiConversationService;
import single.cjj.bizfi.ai.service.AiKnowledgeService;
import single.cjj.bizfi.ai.service.AiModelFacade;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiConversationService conversationService;

    @Mock
    private AiKnowledgeService knowledgeService;

    @Mock
    private AiModelFacade modelFacade;

    private AiProperties aiProperties;
    private AiChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        chatService = new AiChatServiceImpl(
                conversationService,
                knowledgeService,
                modelFacade,
                aiProperties
        );
    }

    @Test
    void shouldExcludeCurrentQuestionAndApplyConfiguredLimits() {
        aiProperties.setMaxHistoryMessages(2);
        aiProperties.setMaxKnowledgeChunks(1);

        AiChatRequest request = new AiChatRequest();
        request.setConversationId("c_1");
        request.setUserMessage("当前问题");
        request.setKbIds(List.of("finance"));
        request.setTaskType(" financial-analysis ");

        when(conversationService.listMessages(7L, "c_1")).thenReturn(List.of(
                message("user", "较早问题"),
                message("assistant", "较早回答"),
                message("user", "上一个问题"),
                message("assistant", "上一个回答"),
                message("user", "当前问题")
        ));
        List<AiCitationResponse> citations = List.of(
                new AiCitationResponse("doc_1", "月结规则", "chunk_1", "结账前检查未过账凭证")
        );
        when(knowledgeService.retrieve("当前问题", List.of("finance"), 1)).thenReturn(citations);
        when(modelFacade.chat(any(AiModelRequest.class))).thenReturn(modelResult("模型回答"));

        AiChatResponse response = chatService.chat(7L, request);

        ArgumentCaptor<AiModelRequest> modelRequestCaptor = ArgumentCaptor.forClass(AiModelRequest.class);
        verify(modelFacade).chat(modelRequestCaptor.capture());
        AiModelRequest modelRequest = modelRequestCaptor.getValue();

        assertEquals(2, modelRequest.getHistoryMessages().size());
        assertEquals("上一个问题", modelRequest.getHistoryMessages().get(0).getContent());
        assertEquals("上一个回答", modelRequest.getHistoryMessages().get(1).getContent());
        assertFalse(modelRequest.getHistoryMessages().stream()
                .map(AiMessageResponse::getContent)
                .anyMatch("当前问题"::equals));
        assertEquals(List.of("结账前检查未过账凭证"), modelRequest.getKnowledgeSnippets());
        assertEquals("financial-analysis", modelRequest.getTaskType());
        assertEquals("模型回答", response.getAnswer());

        verify(conversationService).saveAssistantMessage(
                "c_1",
                "模型回答",
                "test-model",
                "real-model",
                "trace_test",
                10,
                5,
                15
        );
    }

    @Test
    void shouldUseSameOrchestrationForStreamingChat() {
        aiProperties.setKnowledgeEnabled(false);

        AiChatRequest request = new AiChatRequest();
        request.setUserMessage("流式问题");

        BizfiAiConversation conversation = new BizfiAiConversation();
        conversation.setFconversationid("c_stream");
        when(conversationService.createConversation(9L, "快速提问", "quick_chat"))
                .thenReturn(conversation);
        when(conversationService.listMessages(9L, "c_stream"))
                .thenReturn(List.of(message("user", "流式问题")));

        AiConfigStatusResponse status = new AiConfigStatusResponse(true, "test-model", "real-model");
        when(modelFacade.configStatus()).thenReturn(status);
        when(modelFacade.stream(any(AiModelRequest.class), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> deltaConsumer = invocation.getArgument(1, Consumer.class);
            deltaConsumer.accept("第一段");
            deltaConsumer.accept("第二段");
            return modelResult("第一段第二段");
        });

        AiChatStreamObserver observer = mock(AiChatStreamObserver.class);
        AiChatResponse response = chatService.stream(9L, request, observer);

        verify(observer).onStart(eq("c_stream"), eq(List.of()), eq(status));
        verify(observer).onDelta("第一段");
        verify(observer).onDelta("第二段");
        assertEquals("第一段第二段", response.getAnswer());

        ArgumentCaptor<AiModelRequest> modelRequestCaptor = ArgumentCaptor.forClass(AiModelRequest.class);
        verify(modelFacade).stream(modelRequestCaptor.capture(), any());
        assertEquals(List.of(), modelRequestCaptor.getValue().getHistoryMessages());
        assertEquals(List.of(), modelRequestCaptor.getValue().getKnowledgeSnippets());

        verify(conversationService).saveAssistantMessage(
                "c_stream",
                "第一段第二段",
                "test-model",
                "real-model",
                "trace_test",
                10,
                5,
                15
        );
    }

    private BizfiAiMessage message(String role, String content) {
        BizfiAiMessage message = new BizfiAiMessage();
        message.setFrole(role);
        message.setFcontent(content);
        return message;
    }

    private AiModelResult modelResult(String answer) {
        return new AiModelResult(
                answer,
                "test-model",
                "real-model",
                "trace_test",
                10,
                5,
                15,
                0.01
        );
    }
}
