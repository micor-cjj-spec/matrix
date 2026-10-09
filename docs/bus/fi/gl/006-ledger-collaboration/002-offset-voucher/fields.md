# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| matchStatus | 配对状态 | `ALL / PAIRED / ORPHAN` |
| keyword | 关键字 | 匹配凭证号、摘要、备注 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `startDate`
- `endDate`
- `pairCount`
- `orphanCount`
- `originalAmount`
- `reverseAmount`
- `rows`
- `warnings`

## 行字段
| 字段 | 含义 |
|---|---|
| matchStatus | `PAIRED / ORPHAN` |
| originalId | 原凭证ID |
| originalNumber | 原凭证号 |
| originalDate | 原凭证日期 |
| originalStatus | 原凭证状态 |
| originalAmount | 原凭证金额 |
| reverseId | 对冲凭证ID |
| reverseNumber | 对冲凭证号 |
| reverseDate | 对冲凭证日期 |
| reverseStatus | 对冲凭证状态 |
| reverseAmount | 对冲金额 |
| amountDiff | 差额 |
| remark | 备注 |
| message | 说明 |
