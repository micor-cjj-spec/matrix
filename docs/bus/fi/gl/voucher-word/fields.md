# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| wordCode | 凭证字编码 | string | 是 | 如 J、S、F、Z |
| wordName | 凭证字名称 | string | 是 | 记、收、付、转 |
| serialRule | 编号规则 | string | 否 | 关联编号规则 |
| status | 状态 | string | 是 | 草稿、已启用、已停用 |
| defaultFlag | 默认标识 | boolean | 否 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

如后续需按账簿、组织或凭证类型配置适用范围，可扩展明细表。
