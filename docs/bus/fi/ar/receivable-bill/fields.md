# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| billNo | 单据编号 | string | 是 | 系统生成 |
| customerId | 客户 | string | 是 | |
| sourceType | 来源类型 | string | 否 | 销售、服务等 |
| sourceNo | 来源单号 | string | 否 | |
| billDate | 单据日期 | date | 是 | |
| dueDate | 到期日 | date | 否 | |
| amount | 应收金额 | decimal | 是 | |
| taxAmount | 税额 | decimal | 否 | |
| unreceivedAmount | 未收金额 | decimal | 否 | |
| status | 状态 | string | 是 | 草稿、已确认、部分收款、已结清 |
| remark | 备注 | string | 否 | |

## 明细字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| itemType | 明细类型 | string | 否 | |
| itemDesc | 明细说明 | string | 否 | |
| qty | 数量 | decimal | 否 | |
| unitPrice | 单价 | decimal | 否 | |
| lineAmount | 行金额 | decimal | 是 | |
