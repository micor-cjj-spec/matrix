# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `AR / AP` |
| status | 通知状态 | `ALL / OPEN / RESOLVED` |
| asOfDate | 统计日期 | 可空，默认当天 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `noticeType`
- `docTypeRoot`
- `asOfDate`
- `noticeCount`
- `ongoingCount`
- `resolvedCount`
- `snapshotOpenAmount`
- `currentOpenAmount`
- `rows`
- `warnings`

## 勾稽行字段
| 字段 | 含义 |
|---|---|
| fid | 通知ID |
| matchStatus | `ONGOING / RESOLVED` |
| status | 历史通知状态 |
| severity | 历史紧急程度 |
| sourceCode | 历史异常来源 |
| counterparty | 往来方 |
| title | 通知标题 |
| snapshotOpenAmount | 通知快照未清金额 |
| currentOpenAmount | 当前扫描未清金额 |
| improvementAmount | 改善金额 |
| currentSeverity | 当前紧急程度 |
| currentMessage | 当前说明 |
| currentSuggestion | 当前建议 |
