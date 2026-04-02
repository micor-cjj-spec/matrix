# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| billNo | 单据编号 | string | 是 | 系统生成 |
| applicantId | 报销人 | string | 是 | |
| deptId | 报销部门 | string | 是 | |
| applyDate | 申请日期 | date | 是 | |
| expenseType | 报销类型 | string | 是 | |
| totalAmount | 报销总金额 | decimal | 是 | |
| currency | 币别 | string | 否 | 默认本位币 |
| status | 单据状态 | string | 是 | 草稿、审批中、已通过、已驳回、已支付 |
| invoiceAmount | 发票金额合计 | decimal | 否 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| expenseItem | 费用项目 | string | 是 | |
| businessDate | 业务日期 | date | 是 | |
| amount | 明细金额 | decimal | 是 | |
| taxAmount | 税额 | decimal | 否 | |
| invoiceNo | 发票号 | string | 否 | |
| attachmentRef | 附件引用 | string | 否 | 关联影像或附件 |
