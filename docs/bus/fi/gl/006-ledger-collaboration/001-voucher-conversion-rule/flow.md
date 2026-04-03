# 流程设计

## 查询流程
1. 前端调用 `GET /ledger-collaboration/voucher-rules`。
2. 后端根据 `docTypeRoot` 过滤内置 `RuleSpec`。
3. 读取 AR/AP 单据实际数据，按单据类型分组统计。
4. 结合单据状态和是否已关联凭证，计算已审核、已生成和待生成数量。
5. 前端展示规则映射、覆盖率和最近单据日期。

## 统计逻辑
- 已审核：状态达到 `AUDITED / POSTED / REVERSED`
- 已生成凭证：存在 `voucherId` 或 `voucherNumber`
- 待生成：已审核 - 已生成
