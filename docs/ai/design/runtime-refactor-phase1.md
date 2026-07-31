# AI 运行时改造 Phase 1

## 目标

本阶段先收口当前 `base-service` 中的 AI 运行时，消除重复入口和 Bean 覆盖技巧，为后续拆分独立 `ai-service`、接入 Spring AI、向量检索与工具调用建立稳定基线。

## 当前问题

1. `AiAssistantController` 与 `AiAssistantFacadeController` 暴露重复的 `/ai` 接口，旧实现仍使用内存会话和演示引用。
2. `AiModelFacadeRoutingConfig` 与 `AiPromptDrivenModelRoutingConfig` 同时声明 `@Primary AiModelFacade`。
3. `AiModelFacadeBeanOverrideConfig` 通过删除 BeanDefinition 规避冲突，运行行为不直观，也不利于测试和后续扩展。
4. 模型适配器的选择没有形成明确配置契约。

## 本阶段改造

### 1. 统一 API 入口

保留：

- `AiAssistantFacadeController`：同步会话、聊天、配置状态与反馈。
- `AiAssistantStreamController`：SSE 流式聊天。
- `AiKnowledgeController`：知识库管理与检索。

删除旧的 `AiAssistantController`，避免重复映射和内存会话实现继续参与运行。

### 2. 配置化模型路由

新增 `RoutingAiModelFacade` 作为唯一 `@Primary` 模型入口，根据 `bizfi.ai.model-adapter` 选择具体实现：

- `prompt-http`：默认实现，加载文件系统 Prompt，并通过 Java HTTP Client 调用模型。
- `legacy-http`：兼容旧的无 Prompt HTTP 实现，仅用于回退与迁移验证。
- `spring-ai`：Spring AI 适配器占位，未完成依赖接入前会明确返回不可用状态。

环境变量：

```text
AI_MODEL_ADAPTER=prompt-http
```

### 3. 删除 Bean 覆盖技巧

删除两个旧路由配置和 `BeanDefinitionRegistryPostProcessor`。所有调用方只依赖 `AiModelFacade`，由 `RoutingAiModelFacade` 提供稳定入口。

## 兼容性

- 默认适配器仍为 `prompt-http`，保留现有 Prompt、OpenAI-compatible、Gemini Native 和 fallback 行为。
- API 路径不变。
- 数据库表结构不变。
- 本阶段不引入 Spring AI 依赖，不改变现有 Spring Boot 版本。

## 已完成的后续阶段

同步与 SSE 流式模型调用已经在 [Phase 2](./runtime-refactor-phase2.md) 中统一到同一应用服务和模型网关，并清理了遗留的增强聊天服务与 Primary 路由。

## 后续阶段

1. 拆分独立 `ai-service`。
2. 引入 Spring AI `ChatClient`、Advisor 与结构化输出。
3. 将关键词 RAG 升级为 PostgreSQL 全文检索 + PGVector 混合检索。
4. 增加财务只读工具、人工确认与 AI Run 审计模型。
