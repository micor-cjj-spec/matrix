package single.cjj.matrix.ai.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SpringAiPromptFactory {

    private final MatrixAiProperties properties;

    public SpringAiPromptFactory(MatrixAiProperties properties) {
        this.properties = properties;
    }

    public Prompt create(ModelContracts.ChatRequest request) {
        return create(request, properties.getModelName());
    }

    public Prompt create(ModelContracts.ChatRequest request, String model) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(properties.getSystemPrompt())) {
            messages.add(new SystemMessage(properties.getSystemPrompt().trim()));
        }

        if (request.historyMessages() != null) {
            for (ModelContracts.Message historyMessage : request.historyMessages()) {
                if (historyMessage == null
                        || !StringUtils.hasText(historyMessage.role())
                        || !StringUtils.hasText(historyMessage.content())) {
                    continue;
                }
                messages.add(toSpringMessage(historyMessage));
            }
        }

        String knowledgeContext = buildKnowledgeContext(request.knowledgeSnippets());
        if (StringUtils.hasText(knowledgeContext)) {
            messages.add(new SystemMessage(knowledgeContext));
        }
        messages.add(new UserMessage(request.userMessage().trim()));

        ChatOptions options = ChatOptions.builder()
                .model(StringUtils.hasText(model) ? model.trim() : properties.getModelName())
                .build();
        return new Prompt(messages, options);
    }

    private Message toSpringMessage(ModelContracts.Message message) {
        String role = message.role().trim().toLowerCase(Locale.ROOT);
        String content = message.content().trim();
        return switch (role) {
            case "assistant", "model" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> new UserMessage(content);
        };
    }

    private String buildKnowledgeContext(List<String> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return null;
        }
        StringBuilder context = new StringBuilder("以下是业务知识参考，请优先据此回答：\n");
        for (String snippet : snippets) {
            if (StringUtils.hasText(snippet)) {
                context.append("- ").append(snippet.trim()).append('\n');
            }
        }
        return context.length() == "以下是业务知识参考，请优先据此回答：\n".length()
                ? null
                : context.toString().trim();
    }
}
