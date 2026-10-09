# 业务规则

## 查询规则
- `docTypeRoot` 只支持 `AR / AP`。
- `status=ALL` 表示不过滤状态。
- `severity` 为空时不过滤紧急程度。

## 生成规则
- 风险来源仅包含：`OVER_LIMIT / OVERDUE / OPEN_AGING`。
- `OVER_LIMIT` 一律高优先级。
- `OVERDUE` 会按账龄高低分配 `HIGH / MEDIUM`。
- `OPEN_AGING` 作为长期未清风险，默认中优先级。
- 自动生成时，当前扫描已不存在的问题会自动更新为 `RESOLVED`。

## 处理规则
- 通知单本身不直接回写往来单据，只记录处理跟踪。
- 页面通过“对账单”跳转到往来对账单做进一步核查。
