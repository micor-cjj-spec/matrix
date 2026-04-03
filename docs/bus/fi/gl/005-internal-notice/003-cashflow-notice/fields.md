# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | `yyyy-MM`，可空时后端默认当前期间 |
| status | 通知状态 | `ALL / OPEN / RESOLVED` |
| severity | 紧急程度 | `HIGH / MEDIUM / LOW`，可空 |
| sourceCode | 问题来源 | `UNKNOWN_ITEM / MIXED_ITEM / HEURISTIC / CASH_TRANSFER`，可空 |
| currency | 币种 | 默认 `CNY` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `noticeType`
- `orgId`
- `period`
- `currency`
- `noticeCount`
- `openCount`
- `resolvedCount`
- `highCount`
- `amount`
- `rows`
- `warnings`

## 通知行字段
| 字段 | 含义 |
|---|---|
| fid | 通知ID |
| status | `OPEN / RESOLVED` |
| severity | `HIGH / MEDIUM / LOW` |
| orgId | 业务单元 |
| period | 期间 |
| sourceCode | 问题来源 |
| categoryCode | 当前活动分类 |
| voucherId | 凭证ID |
| voucherNumber | 凭证号 |
| title | 标题 |
| message | 通知说明 |
| suggestion | 处理建议 |
| amount | 问题金额 |
| sourceDate | 来源日期 |
| dueDate | 期望处理日期 |
| noticeTime | 通知生成时间 |
| updateTime | 最近更新时间 |
| resolvedTime | 解决时间 |
| resolveNote | 解决说明 |
