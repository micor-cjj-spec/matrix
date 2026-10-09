# 字段设计

## 执行参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| counterparty | 往来方 | 可空 |
| asOfDate | 统计日期 | 可空，后端默认当天 |
| operator | 操作人 | 可空，后端默认 `system` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `planCode`
- `logId`
- `message`
- `sourceDocCount`
- `targetDocCount`
- `linkCount`
- `totalAmount`
- `records`
- `warnings`

## 结果行字段
`records` 与核销方案页的建议行结构一致，表示本次实际执行落库的匹配明细。
