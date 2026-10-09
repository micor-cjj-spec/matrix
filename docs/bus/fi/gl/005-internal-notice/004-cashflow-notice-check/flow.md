# 流程设计

## 勾稽流程
1. 前端调用 `GET /internal-notice/cashflow/reconcile`。
2. 后端读取历史现金流通知单。
3. 同时执行最新现金流候选扫描。
4. 按 `referenceKey` 关联历史通知与当前候选。
5. 如果当前仍存在对应候选，则标记 `ONGOING`；否则标记 `RESOLVED`。
6. 计算快照金额、当前金额并返回当前来源、分类和说明。
