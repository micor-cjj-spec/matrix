# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| templateCode | 模板编码 | string | 是 | 唯一标识 |
| templateName | 模板名称 | string | 是 | |
| templateType | 模板类型 | string | 是 | 损益结转、本年利润等 |
| voucherWord | 凭证字 | string | 否 | 生成结转凭证使用 |
| summaryRule | 摘要规则 | string | 否 | |
| status | 状态 | string | 是 | 草稿、已启用、已停用 |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| sourceAccount | 来源科目 | string | 是 | |
| targetAccount | 目标科目 | string | 是 | |
| amountRule | 取数规则 | string | 是 | 如余额、发生额 |
| directionRule | 方向规则 | string | 否 | |
| sortNo | 排序号 | int | 否 | |
