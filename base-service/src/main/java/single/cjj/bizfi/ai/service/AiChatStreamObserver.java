package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;

import java.util.List;

/**
 * AI 聊天流式事件观察者。
 *
 * <p>应用服务负责会话、知识检索和持久化，传输层只需要把这里的
 * start/delta 事件转换为 SSE、WebSocket 或其他协议。</p>
 */
public interface AiChatStreamObserver {

    void onStart(
            String conversationId,
            List<AiCitationResponse> citations,
            AiConfigStatusResponse configStatus
    );

    void onDelta(String delta);
}
