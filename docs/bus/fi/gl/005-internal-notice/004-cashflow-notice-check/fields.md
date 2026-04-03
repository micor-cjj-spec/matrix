# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| orgId | 业务单元 | 可空 |
| period | 期间 | `yyyy-MM`，为空时后端回退当前期间 |
| status | 通知状态 | `ALL / OPEN / RESOLVED` |
| currency | 币种 | 默认 `CNY` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `noticeType`
- `orgId`
- `period`
- `currency`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotAmount`
- `currentAmount`
- `rows`
- `warnings`

## 勾稽行字段
| 字段 | 含义 |
|---|---|
| fid | 通知ID |
| matchStatus | `ONGOING / RESOLVED` |
| status | 历史通知状态 |
| severity | 历史紧急程度 |
| sourceCode | 历史问题来源 |
| currentSourceCode | 当前问题来源 |
| voucherId | 凭证ID |
| voucherNumber | 凭证号 |
| snapshotAmount | 通知快照金额 |
| currentAmount | 当前问题金额 |
| currentCategoryCode | 当前活动分类 |
| currentMessage | 当前说明 |
| currentSuggestion | 当前建议 |
