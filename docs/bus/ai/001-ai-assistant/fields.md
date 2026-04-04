# 字段设计

## 会话字段
| 字段 | 含义 |
|---|---|
| id | 会话ID |
| title | 会话标题 |
| scene | 场景 |

## 消息字段
| 字段 | 含义 |
|---|---|
| role | 角色：`user / assistant` |
| text | 文本内容 |

## 配置状态字段
| 字段 | 含义 |
|---|---|
| configured | 是否已配置模型 |
| model | 模型名称 |
| mode | 模式：真实模型 / 降级模式 |

## 发送字段
| 字段 | 含义 |
|---|---|
| conversationId | 会话ID |
| userMessage | 用户输入 |
| kbIds | 知识库ID列表 |
| stream | 是否流式 |
