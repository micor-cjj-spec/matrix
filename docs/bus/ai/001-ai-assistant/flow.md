# 流程设计

## 会话流程
1. 进入 `/ai/assistant` 页面。
2. 前端先读取 `/ai/config/status` 获取模型配置状态。
3. 若当前无会话，则调用 `POST /ai/conversations` 自动创建新会话。
4. 选择历史会话时，调用 `GET /ai/conversations/{conversationId}/messages` 加载消息。

## 发送流程
1. 用户输入问题。
2. 前端组装 `conversationId`、`userMessage`、可选知识库参数。
3. 调用 `POST /ai/chat` 发送问题。
4. 页面将返回内容追加到当前会话消息区。

## 反馈流程
1. 用户可对回复提交反馈。
2. 前端调用 `POST /ai/feedback`。
