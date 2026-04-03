# 字段设计

## 查询字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| forg | 组织ID | Long | 否 | |
| period | 期间 | string | 是 | yyyy-MM |
| accountStart | 开始科目 | string | 否 | |
| accountEnd | 结束科目 | string | 否 | |
| showZero | 是否显示零余额 | boolean | 否 | 默认否 |
| summaryLevel | 汇总级次 | int | 否 | |

## 结果字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| accountCode | 科目编码 | string | |
| accountName | 科目名称 | string | |
| beginDebit | 期初借方余额 | decimal | |
| beginCredit | 期初贷方余额 | decimal | |
| debitAmount | 本期借方发生额 | decimal | |
| creditAmount | 本期贷方发生额 | decimal | |
| endDebit | 期末借方余额 | decimal | |
| endCredit | 期末贷方余额 | decimal | |
| levelNo | 科目级次 | int | |
| leafFlag | 是否末级 | boolean | |
