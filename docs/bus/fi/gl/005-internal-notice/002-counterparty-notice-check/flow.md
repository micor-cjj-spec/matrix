# 流程设计

## 勾稽流程
1. 前端调用 `GET /internal-notice/counterparty/reconcile`。
2. 后端读取历史往来通知单。
3. 同时执行最新的往来风险候选扫描。
4. 按 `referenceKey` 关联历史通知与当前候选。
5. 如果当前仍存在对应候选，则标记 `ONGOING`；否则标记 `RESOLVED`。
6. 计算快照未清、当前未清和改善金额并返回。
