# 流程设计

## 查询流程
1. 前端调用 `GET /internal-notice/counterparty`。
2. 后端从内部通知单表读取 `COUNTERPARTY` 类型通知。
3. 按往来类型、状态、紧急程度、统计日期过滤。
4. 返回通知列表、金额汇总和提示信息。

## 生成流程
1. 前端调用 `POST /internal-notice/counterparty/generate`。
2. 后端先执行最新账龄风险扫描，生成候选问题项。
3. 将候选项与已有通知按 `referenceKey` 对齐。
4. 对新问题插入通知，对仍存在的问题刷新通知，对已解除问题自动置为 `RESOLVED`。
5. 返回最新通知列表和生成统计结果。
