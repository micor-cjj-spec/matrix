# AI 运行时改造 Phase 3：独立 Spring AI 服务

## 目标

在不升级现有业务服务 Spring Boot 基线的前提下，将模型调用能力迁移到独立 `ai-service`，正式使用 Spring AI，并保持现有 `/ai/**` 对外接口、会话数据和知识检索逻辑兼容。

## 版本隔离

现有 Matrix 业务服务仍使用：

```text
Spring Boot 3.2.5
Java 17
```

新 `ai-service` 独立使用：

```text
Spring Boot 3.5.14
Spring AI 1.1.7
Java 17
```

`ai-service` 使用独立 Maven Parent，不继承根项目的 Spring Boot 依赖管理；根 `pom.xml` 只负责将其纳入 Reactor。

## 调用架构

```text
Matrix Web/App
    ↓
base-service /ai/chat 或 /ai/chat/stream
    ↓
AiChatServiceImpl
    ├── 用户与会话校验
    ├── 消息持久化
    ├── 历史窗口
    └── 业务知识检索
            ↓
RoutingAiModelFacade
            ↓ model-adapter=spring-ai
SpringAiModelFacade（远程适配器）
            ↓ 内部 HTTP/SSE + Token
ai-service /internal/model/**
            ↓
SpringAiPromptFactory
            ↓
Spring AI ChatClient
            ↓
OpenAI-compatible Provider
```

## 责任边界

### base-service

- 对外 API
- 用户认证与会话权限
- 会话和消息落库
- 当前关键词 RAG 与引用
- 模型适配器选择
- AI 回复最终持久化

### ai-service

- Spring AI 和供应商客户端
- Prompt 消息转换
- 同步模型调用
- 流式模型调用
- 模型返回元数据和 Token 用量
- 内部模型 API 安全

## 内部协议

### 同步

```text
POST /api/internal/model/chat
```

请求字段与 `AiModelRequest` 对齐：

```text
userMessage
historyMessages[]
knowledgeSnippets[]
```

返回字段与 `AiModelResult` 对齐：

```text
answer
model
mode
traceId
promptTokens
completionTokens
totalTokens
estimatedCost
```

### 流式

```text
POST /api/internal/model/chat/stream
Accept: text/event-stream
```

事件：

```text
start
delta
done
error
```

`base-service` 将 `delta` 转发给前端，并使用 `done.result` 保存最终 AI 消息。

## 安全

内部接口要求：

```text
X-Matrix-Internal-Token
```

服务端使用常量时间比较，避免普通字符串比较带来的时序差异。令牌只从环境变量或配置中心注入，不进入请求正文，也不由模型生成。

## 兼容策略

默认仍为：

```text
AI_MODEL_ADAPTER=prompt-http
```

只有显式配置：

```text
AI_MODEL_ADAPTER=spring-ai
```

才会启用独立服务，因此迁移可以按环境逐步灰度。

## 后续

1. 接入 Nacos 服务发现和内部负载均衡。
2. 增加 Resilience4j 超时、熔断和供应商降级。
3. 将 Prompt 版本、模型用量和调用审计写入 AI Run 模型。
4. 将 RAG 迁入独立服务并升级为全文 + PGVector 混合检索。
5. 增加受控财务工具调用和人工确认。
