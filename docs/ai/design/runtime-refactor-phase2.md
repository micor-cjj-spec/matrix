# AI 运行时改造 Phase 2

## 目标

在 Phase 1 完成 API 与模型 Bean 收口后，本阶段进一步统一同步聊天和 SSE 流式聊天的应用编排，确保两种传输方式共用同一套会话、历史消息、知识检索、模型路由与结果持久化逻辑。

## 改造前的问题

1. `AiChatServiceImpl` 负责同步聊天，但 `AiAssistantStreamController` 自己实现了另一套模型 HTTP 调用、Prompt 拼装、历史处理和知识上下文拼装。
2. 同步路径会把刚保存的当前问题放入历史记录，模型适配器末尾又追加一次当前问题，导致同一句问题重复发送。
3. `maxHistoryMessages`、`maxKnowledgeChunks`、`knowledgeEnabled` 和 `enabled` 等运行时配置没有完整进入主调用链。
4. `AiChatEnhancedServiceImpl` 与 `AiChatServiceRoutingConfig` 形成第二套聊天服务和 Primary 路由，增加了运行时歧义。
5. SSE Controller 同时承担 HTTP 协议、模型供应商协议和业务编排职责，难以测试和迁移。

## 当前调用链

```text
AiAssistantFacadeController ─┐
                             ├─> AiChatServiceImpl
AiAssistantStreamController ─┘       ├─ 会话校验/创建
                                     ├─ 用户消息持久化
                                     ├─ 历史窗口构建
                                     ├─ 知识检索与 Top-K
                                     ├─ AiModelFacade
                                     │    └─ RoutingAiModelFacade
                                     │         └─ 具体模型适配器
                                     └─ AI 回复与 usage 持久化
```

同步调用使用 `AiChatService.chat`，流式调用使用 `AiChatService.stream`。两者都由 `AiChatServiceImpl` 的统一准备和完成流程处理。

## 流式协议边界

新增 `AiChatStreamObserver`：

- `onStart`：输出会话 ID、知识引用和当前模型配置。
- `onDelta`：输出模型增量文本。

`AiAssistantStreamController` 只负责：

- 获取当前用户；
- 将 Observer 事件转换为 SSE；
- 输出 `start`、`delta`、`done`、`error` 事件；
- 管理 SSE 超时和执行线程。

Controller 不再直接读取 `AI_API_KEY`、拼装 Prompt、访问知识库或解析模型供应商响应。

## 模型流式能力

`AiModelFacade` 增加统一的 `stream` 方法：

- 原生支持流式的适配器可以逐段调用 delta consumer；
- 不支持原生流式的适配器使用默认实现，将完整回答作为单个 delta 输出；
- `RoutingAiModelFacade` 对同步与流式调用使用同一个适配器选择规则。

当前默认 `prompt-http` 适配器：

- OpenAI-compatible 接口：解析原生 SSE 数据流；
- Gemini Native：当前仍为完整响应后输出单个 delta；
- fallback：输出单个降级回答。

流式 delta 保留前导空格，避免逐段 trim 后英文单词粘连。

## 配置生效

统一编排现在使用：

- `bizfi.ai.enabled`：AI 总开关；
- `bizfi.ai.knowledge-enabled`：知识检索开关；
- `bizfi.ai.max-history-messages`：发送给模型的最大历史消息数；
- `bizfi.ai.max-knowledge-chunks`：知识检索 Top-K；
- `bizfi.ai.request-timeout-seconds`：模型请求和 SSE 超时。

## 兼容性

- `/ai/chat` 与 `/ai/chat/stream` 路径不变；
- 同步与流式返回字段保持原有语义；
- 流式接口现在允许不传 `conversationId`，行为与同步接口一致，会自动创建“快速提问”会话；
- 默认模型适配器仍为 `prompt-http`；
- 数据库结构不变。

## 验证

新增测试覆盖：

1. 当前问题不会重复进入历史上下文；
2. 历史窗口和知识 Top-K 配置生效；
3. 流式调用发送 start/delta 事件；
4. 流式和同步调用共用助手消息持久化；
5. 模型路由正确委托流式调用。

CI 使用：

```bash
mvn -pl base-service -am test
```

## 后续阶段

1. 将 AI 运行时拆分到独立 `ai-service`。
2. 用 Spring AI `ChatClient` 替换手写模型 HTTP 实现。
3. 引入 Advisor 链统一认证上下文、Prompt 版本、RAG、审计与成本控制。
4. 建立 PostgreSQL 全文检索 + PGVector 的混合 RAG。
5. 实现月结检查等只读财务工具与人工确认机制。
