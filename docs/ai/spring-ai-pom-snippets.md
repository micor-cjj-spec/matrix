# Spring AI 依赖与配置片段

> 用途：指导将当前 AI 模块从 `DefaultAiModelFacade` 切换到 `SpringAiModelFacade`
> 说明：以下片段为仓库内手工接入参考，请结合你当前 Maven 版本管理方式落到根 `pom.xml` 与 `base-service/pom.xml`

---

## 1. 根 `pom.xml` 建议补充的 properties

```xml
<properties>
    <!-- Spring AI 版本请按你当前 Spring Boot 3.2.x 兼容版本选择 -->
    <spring.ai.version>填写兼容版本</spring.ai.version>
</properties>
```

---

## 2. 根 `pom.xml` 建议补充的 dependencyManagement

```xml
<dependencyManagement>
    <dependencies>
        <!-- 保留现有 spring-boot / spring-cloud / alibaba BOM -->

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring.ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 3. `base-service/pom.xml` 建议补充的依赖

### OpenAI Compatible 路线（推荐第一阶段优先）

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### 如果后续需要向量检索 / 文档读取，可再按需补充

```xml
<!-- 可选：后续做向量存储时再加 -->
<!--
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
-->
```

---

## 4. `application.yml` / Nacos 建议补充的配置片段

### 建议优先新增你自己的业务配置

```yaml
bizfi:
  ai:
    enabled: true
    knowledge-enabled: true
    fallback-enabled: true
    max-history-messages: 20
    max-knowledge-chunks: 5
```

### Spring AI 模型配置（OpenAI Compatible）

> 说明：具体前缀请按你最终引入的 Spring AI starter 版本校准。
> 当前项目已有 `AI_API_KEY / AI_BASE_URL / AI_CHAT_MODEL` 环境变量习惯，建议继续沿用并映射。

```yaml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:}
      base-url: ${AI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_CHAT_MODEL:gpt-4o-mini}
          temperature: 0.2
```

---

## 5. 接入后建议的代码切换顺序

1. 根 `pom.xml` 增加 `spring.ai.version`
2. 根 `pom.xml` 增加 `spring-ai-bom`
3. `base-service/pom.xml` 增加 `spring-ai-openai-spring-boot-starter`
4. 在 Nacos / `application.yml` 增加 `spring.ai.*` 配置
5. 改造 `AiSpringConfig`，装配 `ChatClient` / `ChatModel`
6. 实现 `SpringAiModelFacade`
7. 修改 `AiModelFacadeRoutingConfig`，将主路由从 `DefaultAiModelFacade` 切到 `SpringAiModelFacade`
8. 联调 `/ai/config/status` 与 `/ai/chat`

---

## 6. 切换完成后的目标状态

切换完成后：

- `/ai/**` 仍由 `AiAssistantFacadeController` 对外暴露
- `AiChatEnhancedServiceImpl` 继续负责编排聊天主流程
- `AiKnowledgeEnhancedServiceImpl` 继续负责业务文档召回
- `AiModelFacadeRoutingConfig` 的主实现切到 `SpringAiModelFacade`
- `DefaultAiModelFacade` 退化为 fallback / legacy 兜底实现

---

## 7. 当前仓库里已经准备好的切换点

- `AiAssistantFacadeController`
- `AiChatEnhancedServiceImpl`
- `AiKnowledgeEnhancedServiceImpl`
- `AiModelFacade`
- `DefaultAiModelFacade`
- `SpringAiModelFacade`
- `AiModelFacadeRoutingConfig`
- `AiProperties`
- `AiSpringConfig`

也就是说，当前真正缺的只是：

- Maven 依赖
- Spring AI Bean 装配
- `SpringAiModelFacade` 正式实现
