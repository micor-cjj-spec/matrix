# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| counterparty | 往来方 | 可空 |
| planCode | 方案号 | 可空；传入后返回明细链接 |
| startDate | 开始日期 | 可空 |
| endDate | 结束日期 | 可空 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `logCount`
- `linkCount`
- `totalAmount`
- `records`
- `warnings`
- `linkDetails`（仅 planCode 传值时返回）

## 批次行字段
| 字段 | 含义 |
|---|---|
| fid | 日志ID |
| planCode | 方案号 |
| docTypeRoot | 往来类型 |
| counterparty | 往来方 |
| mode | 模式 |
| sourceDocCount | 源单据数 |
| targetDocCount | 结算单据数 |
| linkCount | 链接数 |
| totalAmount | 总金额 |
| status | 状态 |
| message | 说明 |
| operator | 操作人 |
| operateTime | 操作时间 |

## 明细行字段
| 字段 | 含义 |
|---|---|
| fid | 链接ID |
| planCode | 方案号 |
| counterparty | 往来方 |
| sourceNumber | 源单据号 |
| sourceType | 源单据类型 |
| targetNumber | 结算单据号 |
| targetType | 结算单据类型 |
| amount | 核销金额 |
| operator | 操作人 |
| operateTime | 操作时间 |
| remark | 备注 |
