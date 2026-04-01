# `SpringAiModelFacade` 正式实现模板

> 用途：在补齐 Spring AI 依赖后，将当前占位类 `SpringAiModelFacade` 改造成正式实现。

## 1. 建议注入的对象

```java
private final ChatClient chatClient;
private final AiProperties aiProperties;
```

或：

```java
private final ChatModel chatModel;
private final AiProperties aiProperties;
```

第一阶段更推荐 `ChatClient`，因为更适合后续继续叠加 prompt、system、advisors、tools。

---

## 2. 建议的正式实现轮廓

```java
@Service("springAiModelFacade")
public class SpringAiModelFacade implements AiModelFacade {

    private final ChatClient chatClient;
    private final AiProperties aiProperties;

    public SpringAiModelFacade(ChatClient.Builder chatClientBuilder, AiProperties aiProperties) {
        this.chatClient = chatClientBuilder.build();
        this.aiProperties = aiProperties;
    }

    @Override
    public AiModelResult chat(AiModelRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserMessage())) {
            throw new IllegalArgumentException("userMessage 不能为空");
        }

        String systemPrompt = buildSystemPrompt();
        String knowledgePrompt = buildKnowledgePrompt(request.getKnowledgeSnippets());
        String historyPrompt = buildHistoryPrompt(request.getHistoryMessages());

        String finalUserPrompt = """
                %s

                %s

                用户当前问题：
                %s
                """.formatted(historyPrompt, knowledgePrompt, request.getUserMessage().trim());

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(finalUserPrompt)
                .call()
                .content();

        return new AiModelResult(
                answer,
                "spring-ai-model",
                "real-model",
                "trace_" + System.currentTimeMillis(),
                0,
                0,
                0,
                0.0
        );
    }

    @Override
    public AiConfigStatusResponse configStatus() {
        return new AiConfigStatusResponse(true, "spring-ai-model", "real-model");
    }

    private String buildSystemPrompt() {
        return "你是 BizFi 业财平台 AI 助手，优先基于业务知识回答，不要编造系统中不存在的功能。";
    }

    private String buildKnowledgePrompt(List<String> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return "当前未召回到业务知识片段。";
        }
        return "业务知识参考：\n" + snippets.stream()
                .filter(StringUtils::hasText)
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
    }

    private String buildHistoryPrompt(List<AiMessageResponse> historyMessages) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return "历史对话：无";
        }
        return "历史对话：\n" + historyMessages.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getRole()) && StringUtils.hasText(item.getContent()))
                .map(item -> item.getRole() + ": " + item.getContent())
                .collect(Collectors.joining("\n"));
    }
}
```

---

## 3. `AiSpringConfig` 建议补充的装配方式

```java
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiSpringConfig {
}
```

如果 starter 自动装配已经可用，第一阶段这里可以不额外写太多 Bean。

如果需要手动暴露 `ChatClient.Builder`，再按你引入的 Spring AI 版本对应 API 补充。

---

## 4. 路由切换点

当前仓库已有：

```java
@Bean
@Primary
public AiModelFacade primaryAiModelFacade(DefaultAiModelFacade defaultAiModelFacade) {
    return defaultAiModelFacade;
}
```

切换到 Spring AI 时，把它改成：

```java
@Bean
@Primary
public AiModelFacade primaryAiModelFacade(SpringAiModelFacade springAiModelFacade) {
    return springAiModelFacade;
}
```

---

## 5. 第一阶段暂不建议加入的内容

- Tool Calling
- Advisors 过度复杂编排
- 多模型路由
- Streaming
- Structured Output

第一阶段只要先完成：

- Spring AI 调模型
- 业务知识片段拼接
- 历史消息拼接
- `/ai/chat` 可用

就已经达标。
