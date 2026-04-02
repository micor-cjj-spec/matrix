# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| voucherNo | 凭证号 | string | 是 | 系统生成 |
| voucherDate | 凭证日期 | date | 是 | |
| status | 凭证状态 | string | 是 | |

## 分录字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| accountCode | 科目编码 | string | 是 | |
| debitAmount | 借方金额 | decimal | 否 | |
| creditAmount | 贷方金额 | decimal | 否 | |
