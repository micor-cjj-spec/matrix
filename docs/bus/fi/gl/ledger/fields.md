# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| ledgerCode | 账簿编码 | string | 是 | 账簿唯一标识 |
| ledgerName | 账簿名称 | string | 是 | |
| companyId | 核算主体 | string | 是 | 对应组织或公司 |
| accountingStandard | 会计准则 | string | 是 | 如企业会计准则 |
| baseCurrency | 本位币 | string | 是 | 默认记账币别 |
| startPeriod | 启用期间 | string | 是 | 例如 2026-01 |
| status | 状态 | string | 是 | 草稿、已启用、已停用 |
| enableFlag | 启用标识 | boolean | 是 | |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

账簿通常以主表为主，若后续存在账簿-组织、账簿-币别、账簿-核算维度扩展关系，可按明细表扩展。
