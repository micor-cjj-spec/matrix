# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docType | 单据类型 | 固定 `AR` |
| page | 页码 | 默认 1 |
| size | 每页数量 | 默认 10 |
| number | 单据号 | 模糊匹配 |
| counterparty | 往来方 | 模糊匹配 |
| status | 状态 | 精确匹配 |
| startDate | 开始日期 | 可空 |
| endDate | 结束日期 | 可空 |
| minAmount | 最小金额 | 可空 |
| maxAmount | 最大金额 | 可空 |

## 主体字段（BizfiFiArapDoc）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fdoctype | 单据类型 |
| fnumber | 单据号 |
| fdate | 单据日期 |
| fcounterparty | 往来方 |
| famount | 金额 |
| fstatus | 状态：`DRAFT / SUBMITTED / AUDITED / REJECTED` |
| fremark | 备注 |
| fvoucherId | 关联凭证ID |
| fvoucherNumber | 关联凭证号 |
| fauditedBy | 审核人 |
| fauditedTime | 审核时间 |
