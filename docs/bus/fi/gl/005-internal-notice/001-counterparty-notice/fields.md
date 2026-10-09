# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| status | 通知状态 | `ALL / OPEN / RESOLVED` |
| severity | 紧急程度 | `HIGH / MEDIUM / LOW`，可空 |
| asOfDate | 统计日期 | 可空，默认当天 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `noticeType`
- `docTypeRoot`
- `asOfDate`
- `noticeCount`
- `openCount`
- `resolvedCount`
- `highCount`
- `amount`
- `openAmount`
- `rows`
- `warnings`

## 通知行字段
| 字段 | 含义 |
|---|---|
| fid | 通知ID |
| status | `OPEN / RESOLVED` |
| severity | `HIGH / MEDIUM / LOW` |
| docTypeRoot | 往来类型 |
| sourceCode | 异常来源：`OVER_LIMIT / OVERDUE / OPEN_AGING` |
| counterparty | 往来方 |
| title | 标题 |
| message | 通知说明 |
| suggestion | 处理建议 |
| amount | 通知金额 |
| openAmount | 通知快照未清金额 |
| sourceDate | 来源日期 |
| dueDate | 期望处理日期 |
| noticeTime | 通知生成时间 |
| updateTime | 最近更新时间 |
| resolvedTime | 解决时间 |
| resolveNote | 解决说明 |
