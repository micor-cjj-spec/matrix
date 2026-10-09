# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| counterparty | 往来方 | 可空 |
| asOfDate | 统计日期 | 可空，后端默认当天 |
| auditedOnly | 仅已审核 | 默认 `false`，前端方案页当前默认传 `true` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `sourceDocCount`
- `targetDocCount`
- `counterpartyCount`
- `sourceOpenAmount`
- `targetOpenAmount`
- `suggestedAmount`
- `planCount`
- `remainingSourceAmount`
- `remainingTargetAmount`
- `warnings`
- `records`

## 方案行字段
| 字段 | 含义 |
|---|---|
| counterparty | 往来方 |
| sourceDocId | 源单据ID |
| sourceNumber | 源单据号 |
| sourceType | 源单据类型 |
| sourceDate | 源单据日期 |
| sourceOpenAmount | 源单据剩余未核销金额 |
| sourceAgeDays | 源单据账龄 |
| targetDocId | 结算单据ID |
| targetNumber | 结算单据号 |
| targetType | 结算单据类型 |
| targetDate | 结算单据日期 |
| targetOpenAmount | 结算单据剩余可分配金额 |
| targetAgeDays | 结算单据账龄 |
| suggestedAmount | 建议核销金额 |
