# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| asOfDate | 统计日期 | 可空，默认当天 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `counterpartyCount`
- `warningCount`
- `totalOpenAmount`
- `rows`
- `warnings`

## 行字段
| 字段 | 含义 |
|---|---|
| counterparty | 往来方 |
| docCount | 单据数 |
| openAmount | 未核销余额 |
| writtenOffAmount | 已核销金额 |
| bucket0_30 | 0-30 天余额 |
| bucket31_60 | 31-60 天余额 |
| bucket61_90 | 61-90 天余额 |
| bucket91Plus | 90+ 天余额 |
| maxAgeDays | 最大账龄 |
| creditLimit | 信用额度 |
| riskFlag | 是否预警 |
| riskReason | 风险原因 |
