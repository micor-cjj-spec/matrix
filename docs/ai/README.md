# AI 文档目录（matrix）

本目录用于沉淀 AI 助手相关文档，与 `docs/business` 分开维护。

## 目录约定

- `prompts/`：系统提示词、回答策略、身份说明、降级策略
- `design/`：AI 架构设计、RAG 策略、模型接入方案

## 为什么不把提示词放进 `docs/business`

`docs/business` 当前用于沉淀业务分析文档，并且会被后端启动导入为 AI 知识库分片，作为后续 RAG 检索来源。

因此：

- `docs/business/**`：只放业务知识，给 RAG 用
- `docs/ai/prompts/**`：只放提示词与行为约束，给 system prompt 用

这样可以避免：

- 提示词被误导入知识库
- system instruction 与业务事实混在一起
- 后续检索召回被 prompt 污染
