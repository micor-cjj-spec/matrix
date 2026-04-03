# 业务规则

## 查询规则
- `period` 必须是 `yyyy-MM`，无效时返回 warning。
- 该页面不接受明细过滤条件，重点是整期补录与复核视图。

## 任务规则
当前任务项至少覆盖：
- 现金类科目设置
- 现金流项目主数据
- 直接标记覆盖率
- 未知编码复核
- 多编码复核
- 现金划转复核
- 组织隔离提示

## 待处理凭证规则
- `pendingVouchers` 只包含非 `DIRECT` 的记录。
- 优先级顺序：`UNKNOWN_ITEM > MIXED_ITEM > HEURISTIC > CASH_TRANSFER`。
- 每条待处理凭证都会生成 `issue` 和 `suggestion`。

## 说明规则
- `CASH_TRANSFER` 代表纯现金类科目内部划转，主表不计入。
- `UNKNOWN_ITEM` 和 `MIXED_ITEM` 都应优先处理后再出具正式现金流主表。
