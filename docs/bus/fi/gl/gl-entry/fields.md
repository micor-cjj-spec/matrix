# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | |
| fvoucherId | 凭证ID | Long | 是 | 来源凭证 |
| fvoucherLineId | 凭证行ID | Long | 是 | 来源凭证明细 |
| fvoucherNumber | 凭证号 | string | 是 | |
| fvoucherDate | 凭证日期 | date | 是 | |
| faccountCode | 科目编码 | string | 是 | |
| fsummary | 摘要 | string | 否 | 分录摘要 |
| fdebitAmount | 借方金额 | decimal | 是 | |
| fcreditAmount | 贷方金额 | decimal | 是 | |
| fcashflowItem | 现金流量项目 | string | 否 | |
| fpostedTime | 过账时间 | datetime | 否 | |
| fpostedBy | 过账人 | string | 否 | |

## 明细字段

当前代码模型为单表结果对象，不再拆分明细表。fileciteturn223file0L1-L29
