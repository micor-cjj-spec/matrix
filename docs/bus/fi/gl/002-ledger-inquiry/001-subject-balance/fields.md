# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| accountCode | 科目编码 | 可空，空时查询全部科目 |

## 返回结构
统一返回 `LedgerQueryResultVO`：
- `records`
- `warnings`
- `summary`

## 记录行（LedgerBalanceRowVO）
| 字段 | 含义 |
|---|---|
| accountCode | 科目编码 |
| accountName | 科目名称 |
| normalDirection | 正常方向 |
| openingBalance | 期初余额 |
| openingDirection | 期初方向 |
| periodDebit | 本期借方 |
| periodCredit | 本期贷方 |
| closingBalance | 期末余额 |
| closingDirection | 期末方向 |
| entryCount | 本期分录数 |

## 汇总区（summary）
| 字段 | 含义 |
|---|---|
| recordCount | 科目行数 |
| totalDebit | 本期借方合计 |
| totalCredit | 本期贷方合计 |
| startDate | 实际查询开始日期 |
| endDate | 实际查询结束日期 |
