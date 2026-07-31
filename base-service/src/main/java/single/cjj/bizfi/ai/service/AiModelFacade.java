package single.cjj.bizfi.ai.service;

import single.cjj.bizfi.ai.dto.AiConfigStatusResponse;
import single.cjj.bizfi.ai.dto.AiModelRequest;
import single.cjj.bizfi.ai.dto.AiModelResult;

import java.util.Objects;
import java.util.function.Consumer;

public interface AiModelFacade {

    AiModelResult chat(AiModelRequest request);

    /**
     * 流式调用模型。
     *
     * <p>不具备原生流式能力的适配器可以沿用默认实现：先完成普通调用，
     * 再把完整答案作为一个 delta 输出。支持原生流式的适配器应覆盖该方法。</p>
     */
    default AiModelResult stream(AiModelRequest request, Consumer<String> deltaConsumer) {
        Objects.requireNonNull(deltaConsumer, "deltaConsumer 不能为空");
        AiModelResult result = chat(request);
        if (result != null && result.getAnswer() != null && !result.getAnswer().isEmpty()) {
            deltaConsumer.accept(result.getAnswer());
        }
        return result;
    }

    AiConfigStatusResponse configStatus();
}
