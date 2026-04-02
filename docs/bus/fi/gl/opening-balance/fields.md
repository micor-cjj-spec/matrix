# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| initBatchNo | 导入批次号 | string | 是 | 初始化或导入批次 |
| ledgerCode | 账簿编码 | string | 是 | |
| periodCode | 启用期间 | string | 是 | |
| status | 状态 | string | 是 | 草稿、已提交、已确认 |
| totalDebit | 借方合计 | decimal | 否 | 汇总字段 |
| totalCredit | 贷方合计 | decimal | 否 | 汇总字段 |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| accountCode | 科目编码 | string | 是 | |
| assistInfo | 辅助核算信息 | string | 否 | |
| debitAmount | 期初借方余额 | decimal | 否 | |
| creditAmount | 期初贷方余额 | decimal | 否 | |
| qty | 数量余额 | decimal | 否 | |
| foreignAmount | 外币余额 | decimal | 否 | |
