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
import single.cjj.matrix.ai.observability.AiModelMetrics;
import single.cjj.matrix.ai.tool.finance.FinanceMonthEndCloseTool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SpringAiModelGateway {

    private final ChatClient chatClient;
    private final SpringAiPromptFactory promptFactory;
    private final MatrixAiProperties properties;
    private final AiTaskRouter taskRouter;
    private final AiModelMetrics metrics;
    private final FinanceMonthEndCloseTool financeMonthEndCloseTool;

    public SpringAiModelGateway(
            ChatClient.Builder chatClientBuilder,
            SpringAiPromptFactory promptFactory,
            MatrixAiProperties properties,
            AiTaskRouter taskRouter,
            AiModelMetrics metrics,
            FinanceMonthEndCloseTool financeMonthEndCloseTool
    ) {
        this.chatClient = chatClientBuilder.build();
        this.promptFactory = promptFactory;
        this.properties = properties;
        this.taskRouter = taskRouter;
        this.metrics = metrics;
        this.financeMonthEndCloseTool = financeMonthEndCloseTool;
    }

    public ModelContracts.ChatResponse chat(ModelContracts.ChatRequest request) {
        AiTaskRouter.ModelRoute route = route(request);
        long startedAt = metrics.start();
        try {
            Prompt prompt = promptFactory.create(request, route.model());
            var requestSpec = chatClient.prompt(prompt);
            if (toolEnabled(request, route)) {
                requestSpec = requestSpec
                        .tools(financeMonthEndCloseTool)
                        .toolContext(toolContextMap(request.toolContext()));
            }
            ChatResponse response = requestSpec.call().chatResponse();
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
                    : route.model();
            Integer promptTokens = normalizeTokenCount(usage == null ? null : usage.getPromptTokens());
            Integer completionTokens = normalizeTokenCount(usage == null ? null : usage.getCompletionTokens());
            Integer totalTokens = normalizeTokenCount(usage == null ? null : usage.getTotalTokens());

            metrics.recordSuccess("chat", route, startedAt, promptTokens, completionTokens);
            return new ModelContracts.ChatResponse(
                    answer,
                    model,
                    "spring-ai",
                    newTraceId(),
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    0.0
            );
        } catch (RuntimeException error) {
            metrics.recordFailure("chat", route, startedAt, errorType(error));
            throw error;
        }
    }

    public Flux<String> stream(ModelContracts.ChatRequest request) {
        return stream(request, route(request));
    }

    public Flux<String> stream(ModelContracts.ChatRequest request, AiTaskRouter.ModelRoute route) {
        return Flux.defer(() -> {
            long startedAt = metrics.start();
            Prompt prompt = promptFactory.create(request, route.model());
            var requestSpec = chatClient.prompt(prompt);
            if (toolEnabled(request, route)) {
                requestSpec = requestSpec
                        .tools(financeMonthEndCloseTool)
                        .toolContext(toolContextMap(request.toolContext()));
            }
            return requestSpec
                    .stream()
                    .content()
                    .filter(StringUtils::hasLength)
                    .doOnComplete(() -> metrics.recordSuccess("stream", route, startedAt, 0, 0))
                    .doOnError(error -> metrics.recordFailure(
                            "stream",
                            route,
                            startedAt,
                            errorType(error)
                    ));
        });
    }

    public AiTaskRouter.ModelRoute route(ModelContracts.ChatRequest request) {
        return taskRouter.route(request);
    }

    public ModelContracts.StatusResponse status() {
        return new ModelContracts.StatusResponse(true, properties.getModelName(), "spring-ai");
    }

    public String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean toolEnabled(ModelContracts.ChatRequest request, AiTaskRouter.ModelRoute route) {
        if (!AiTaskRouter.TOOL_CALLING.equals(route.taskType())) {
            if (request != null && request.toolContext() != null) {
                throw new IllegalStateException("非 tool-calling 请求不能携带 toolContext");
            }
            return false;
        }
        if (request == null || request.toolContext() == null) {
            throw new IllegalStateException("tool-calling 请求缺少服务端 toolContext");
        }
        if (!FinanceMonthEndCloseTool.CONTEXT_TOOL_NAME.equals(request.toolContext().toolName())) {
            throw new IllegalStateException("不支持的 AI 工具: " + request.toolContext().toolName());
        }
        return true;
    }

    private Map<String, Object> toolContextMap(ModelContracts.ToolContext context) {
        if (context.requestedByUserId() == null
                || context.organizationId() == null
                || !StringUtils.hasText(context.period())
                || !StringUtils.hasText(context.requestId())) {
            throw new IllegalStateException("服务端 toolContext 不完整");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("toolName", context.toolName());
        values.put("requestedByUserId", context.requestedByUserId());
        values.put("organizationId", context.organizationId());
        values.put("period", context.period());
        values.put("requestId", context.requestId());
        return Map.copyOf(values);
    }

    private Integer normalizeTokenCount(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String errorType(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String simpleName = error.getClass().getSimpleName();
        return StringUtils.hasText(simpleName) ? simpleName : "unknown";
    }
}
