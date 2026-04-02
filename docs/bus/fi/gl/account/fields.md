# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| accountCode | 科目编码 | string | 是 | 科目唯一标识 |
| accountName | 科目名称 | string | 是 | |
| parentCode | 上级科目 | string | 否 | 顶级科目可为空 |
| levelNo | 科目级次 | int | 是 | |
| direction | 余额方向 | string | 是 | 借或贷 |
| leafFlag | 是否末级 | boolean | 是 | |
| enableFlag | 启用标识 | boolean | 是 | |
| assistConfig | 辅助核算配置 | string | 否 | 可序列化存储 |
| cashflowFlag | 现金流量控制 | boolean | 否 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

若后续支持科目与辅助核算、币别、账簿维度的独立配置，可扩展明细表维护。
