# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| accountCode | 科目编码 | 可空 |

## 返回结构
统一返回 `LedgerQueryResultVO`。

## 记录行（LedgerDailyRowVO）
| 字段 | 含义 |
|---|---|
| bizDate | 业务日期 |
| voucherCount | 当日凭证数 |
| openingBalance | 当日期初余额 |
| openingDirection | 当日期初方向 |
| debitAmount | 当日借方发生 |
| creditAmount | 当日贷方发生 |
| closingBalance | 当日期末余额 |
| closingDirection | 当日期末方向 |

## 汇总区（summary）
| 字段 | 含义 |
|---|---|
| recordCount | 日记录数 |
| totalDebit | 期间借方合计 |
| totalCredit | 期间贷方合计 |
| voucherCount | 期间凭证数 |
| startDate | 实际查询开始日期 |
| endDate | 实际查询结束日期 |
