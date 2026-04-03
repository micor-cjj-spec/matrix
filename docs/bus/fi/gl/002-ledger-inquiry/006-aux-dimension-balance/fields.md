# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| accountCode | 科目编码 | 可空 |
| dimensionCode | 维度编码 | 当前实际为现金流项目编码 |

## 返回结构
统一返回 `LedgerQueryResultVO`。

## 记录行（LedgerDimensionBalanceRowVO）
| 字段 | 含义 |
|---|---|
| dimensionCode | 维度编码 |
| dimensionName | 维度名称 |
| accountCode | 科目编码 |
| accountName | 科目名称 |
| openingBalance | 期初余额 |
| openingDirection | 期初方向 |
| periodDebit | 本期借方 |
| periodCredit | 本期贷方 |
| closingBalance | 期末余额 |
| closingDirection | 期末方向 |
| entryCount | 分录数 |
