# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| taskNo | 任务单号 | string | 是 | 期末处理任务唯一标识 |
| ledgerCode | 账簿编码 | string | 是 | |
| periodCode | 会计期间 | string | 是 | |
| closeType | 处理类型 | string | 是 | 结账、反结账、损益结转等 |
| status | 状态 | string | 是 | 草稿、处理中、已完成、已失败 |
| executeTime | 执行时间 | datetime | 否 | |
| executeResult | 执行结果 | string | 否 | 结果说明 |
| operator | 执行人 | string | 否 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

如后续需要记录检查项、步骤日志、异常明细，可扩展任务步骤和日志明细表。
