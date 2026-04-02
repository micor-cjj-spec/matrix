# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fiscalYear | 会计年度 | string | 是 | 例如 2026 |
| periodNo | 会计期间 | string | 是 | 例如 01 |
| periodCode | 期间编码 | string | 是 | 例如 2026-01 |
| startDate | 开始日期 | date | 是 | |
| endDate | 结束日期 | date | 是 | |
| status | 期间状态 | string | 是 | 未启用、开放、已结账 |
| closeFlag | 结账标识 | boolean | 是 | |
| ledgerCode | 账簿编码 | string | 是 | 所属账簿 |
| remark | 备注 | string | 否 | |
| createdBy | 创建人 | string | 否 | 审计字段 |
| createdTime | 创建时间 | datetime | 否 | 审计字段 |
| updatedBy | 更新人 | string | 否 | 审计字段 |
| updatedTime | 更新时间 | datetime | 否 | 审计字段 |

## 明细字段

会计期间通常以主表控制为主，若后续存在期间任务、期间检查项等扩展，可独立设计明细表。
