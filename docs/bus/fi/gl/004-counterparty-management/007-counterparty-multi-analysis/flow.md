# 流程设计

## 查询流程
1. 前端调用 `GET /arap-manage/multi-analysis`。
2. 后端读取指定日期前的往来单据，并扣减已落库核销链接金额。
3. 按 `groupDimension` 计算分组键和分组名称。
4. 聚合每组的原额、已核销金额、未核销金额、平均账龄和最近日期。
5. 前端展示汇总卡片、告警和分组结果表。

## 支持维度
- `COUNTERPARTY`：按往来方
- `DOCTYPE`：按单据类型
- `STATUS`：按单据状态
- `COUNTERPARTY_DOCTYPE`：按往来方 + 单据类型
- `ROLE`：按角色
