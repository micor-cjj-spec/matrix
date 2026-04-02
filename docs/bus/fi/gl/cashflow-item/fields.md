# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| itemCode | 项目编码 | string | 是 | 唯一标识 |
| itemName | 项目名称 | string | 是 | |
| itemType | 项目类型 | string | 是 | 经营、投资、筹资 |
| parentCode | 上级项目 | string | 否 | 支持层级结构 |
| leafFlag | 是否末级 | boolean | 是 | |
| enableFlag | 启用标识 | boolean | 是 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

如后续需要配置与科目、分录规则或报表项的映射关系，可扩展明细表。
