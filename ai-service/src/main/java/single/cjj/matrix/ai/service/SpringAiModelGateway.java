package single.cjj.matrix.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.UUID;

@Service
public class SpringAiModelGateway {

    private final ChatClient chatClient;
    private final SpringAiPromptFactory promptFactory;
    private final MatrixAiProperties properties;

    public SpringAiModelGateway(
            ChatClient.Builder chatClientBuilder,
            SpringAiPromptFactory promptFactory,
            MatrixAiProperties properties
    ) {
        this.chatClient = chatClientBuilder.build();
        this.promptFactory = promptFactory;
        this.properties = properties;
    }

    public ModelContracts.ChatResponse chat(ModelContracts.ChatRequest request) {
        Prompt prompt = promptFactory.create(request);
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("Spring AI 模型返回为空");
        }

        String answer = response.getResult().getOutput().getText();
        if (!StringUtils.hasText(answer)) {
            throw new IllegalStateException("Spring AI 模型回答为空");
        }

        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        String model = response.getMetadata() != null && StringUtils.hasText(response.getMetadata().getModel())
                ? response.getMetadata().getModel()
                : properties.getModelName();

        return new ModelContracts.ChatResponse(
                answer,
                model,
                "spring-ai",
                newTraceId(),
                normalizeTokenCount(usage == null ? null : usage.getPromptTokens()),
                normalizeTokenCount(usage == null ? null : usage.getCompletionTokens()),
                normalizeTokenCount(usage == null ? null : usage.getTotalTokens()),
                0.0
        );
    }

    public Flux<String> stream(ModelContracts.ChatRequest request) {
        Prompt prompt = promptFactory.create(request);
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .filter(StringUtils::hasLength);
    }

    public ModelContracts.StatusResponse status() {
        return new ModelContracts.StatusResponse(true, properties.getModelName(), "spring-ai");
    }

    public String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Integer normalizeTokenCount(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }
}
