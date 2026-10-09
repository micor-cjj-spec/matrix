# 业务规则

## 查询规则
- `period` 使用 `yyyy-MM`，为空时后端回退当前期间。
- `status=ALL` 表示不过滤状态。
- `severity`、`sourceCode` 为空时不过滤。
- `currency` 为空时默认 `CNY`。

## 生成规则
- 风险来源仅包含：`UNKNOWN_ITEM / MIXED_ITEM / HEURISTIC / CASH_TRANSFER`。
- `UNKNOWN_ITEM` 和 `MIXED_ITEM` 一律高优先级。
- `HEURISTIC` 默认中优先级。
- `CASH_TRANSFER` 默认低优先级。
- 自动生成时，当前扫描已不存在的问题会自动更新为 `RESOLVED`。

## 处理规则
- 通知单本身不直接修改凭证，只记录处理跟踪。
- 页面通过“查看凭证”跳转到凭证页做进一步复核。
