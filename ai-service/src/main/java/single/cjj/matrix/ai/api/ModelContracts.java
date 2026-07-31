package single.cjj.matrix.ai.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class ModelContracts {

    private ModelContracts() {
    }

    public record Message(
            @NotBlank String role,
            @NotBlank String content
    ) {
    }

    public record ToolContext(
            String toolName,
            Long requestedByUserId,
            Long organizationId,
            String period,
            String requestId
    ) {
    }

    public record ChatRequest(
            @NotBlank String userMessage,
            List<@Valid Message> historyMessages,
            List<String> knowledgeSnippets,
            String taskType,
            ToolContext toolContext
    ) {
        public ChatRequest(
                String userMessage,
                List<Message> historyMessages,
                List<String> knowledgeSnippets
        ) {
            this(userMessage, historyMessages, knowledgeSnippets, null, null);
        }

        public ChatRequest(
                String userMessage,
                List<Message> historyMessages,
                List<String> knowledgeSnippets,
                String taskType
        ) {
            this(userMessage, historyMessages, knowledgeSnippets, taskType, null);
        }
    }

    public record ChatResponse(
            String answer,
            String model,
            String mode,
            String traceId,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Double estimatedCost
    ) {
    }

    public record StatusResponse(
            Boolean configured,
            String model,
            String mode
    ) {
    }

    public record StreamEvent(
            String type,
            String delta,
            ChatResponse result,
            String message
    ) {
        public static StreamEvent start() {
            return new StreamEvent("start", null, null, null);
        }

        public static StreamEvent delta(String value) {
            return new StreamEvent("delta", value, null, null);
        }

        public static StreamEvent done(ChatResponse result) {
            return new StreamEvent("done", null, result, null);
        }

        public static StreamEvent error(String message) {
            return new StreamEvent("error", null, null, message);
        }
    }
}
