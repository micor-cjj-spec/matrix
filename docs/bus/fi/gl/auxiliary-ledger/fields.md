# 字段设计

## 查询字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| forg | 组织ID | Long | 否 | |
| period | 期间 | string | 是 | yyyy-MM |
| accountCode | 科目编码 | string | 否 | |
| assistType | 辅助类型 | string | 否 | 客户/供应商/项目/部门等 |
| assistCode | 辅助对象编码 | string | 否 | |
| showDetail | 是否显示明细 | boolean | 否 | |

## 结果字段

| 字段 | 含义 | 类型 | 备注 |
|---|---|---|---|
| accountCode | 科目编码 | string | |
| accountName | 科目名称 | string | |
| assistType | 辅助类型 | string | |
| assistCode | 辅助对象编码 | string | |
| assistName | 辅助对象名称 | string | |
| beginBalance | 期初余额 | decimal | |
| debitAmount | 借方发生额 | decimal | |
| creditAmount | 贷方发生额 | decimal | |
| endBalance | 期末余额 | decimal | |
