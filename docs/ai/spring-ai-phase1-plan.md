# AI 助手 Spring AI 第一阶段接入说明

## 当前状态

当前 AI 模块已经完成以下拆分：

- `AiAssistantFacadeController`：新的 `/ai/**` 门面控制器
- `AiConversationService`：会话与消息持久化骨架
- `AiChatService`：聊天主流程编排
- `AiFeedbackService`：反馈落库骨架
- `AiKnowledgeService`：知识检索接口占位
- `DefaultAiModelFacade`：当前默认模型调用实现（手写 HTTP）
- `SpringAiModelFacade`：Spring AI 正式接入占位类
- `AiModelFacadeRoutingConfig`：当前默认将 `AiModelFacade` 路由到 `DefaultAiModelFacade`

## 为什么当前仍默认使用 DefaultAiModelFacade

因为仓库还未完成以下改动：

1. `pom.xml` / `base-service/pom.xml` 增加 Spring AI BOM 与 starter 依赖
2. `AiSpringConfig` 中正式装配 `ChatModel` / `ChatClient`
3. `SpringAiModelFacade` 改为使用 Spring AI Bean 进行模型调用

在这三步完成前，为了保证现有功能仍可运行，`AiModelFacadeRoutingConfig` 将主实现固定为 `DefaultAiModelFacade`。

## 下一步改造点

### 1. 依赖层
- 根 `pom.xml` 增加 Spring AI BOM 版本属性
- `base-service/pom.xml` 增加 Spring AI starter

### 2. 配置层
- `AiProperties` 增加模型供应商、超时、温度、topK 等配置
- `AiSpringConfig` 正式装配 Spring AI Bean

### 3. 模型层
- `SpringAiModelFacade` 接入 `ChatClient` / `ChatModel`
- 将当前 prompt、历史消息、知识片段映射到 Spring AI 调用

### 4. 路由层
- `AiModelFacadeRoutingConfig` 从默认路由 `DefaultAiModelFacade`
  切换为 `SpringAiModelFacade`

### 5. 知识层
- `AiKnowledgeServiceImpl` 导入 `docs/business/**`
- 第一阶段先做文档切片 + 关键词召回
- 第二阶段再升级为向量检索

## 建议切换顺序

1. 补依赖
2. 补 Spring AI Bean
3. 实现 `SpringAiModelFacade`
4. 切换 `AiModelFacadeRoutingConfig`
5. 联调 `/ai/chat`
6. 再接入知识检索与 citation
