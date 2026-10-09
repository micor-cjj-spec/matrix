# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| issueCode | 问题类型 | `ALL` 或具体异常编码 |
| severity | 问题等级 | `ALL / HIGH / MEDIUM / LOW` |
| status | 凭证状态 | `ALL / DRAFT / SUBMITTED / AUDITED / POSTED / REJECTED / REVERSED` |
| onlyIssue | 仅看异常 | `true / false` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `startDate`
- `endDate`
- `voucherCount`
- `issueCount`
- `issueVoucherCount`
- `highCount`
- `healthyCount`
- `rows`
- `warnings`

## 检查结果行字段
| 字段 | 含义 |
|---|---|
| voucherId | 凭证ID |
| voucherNumber | 凭证号 |
| voucherDate | 凭证日期 |
| status | 凭证状态 |
| summary | 摘要 |
| issueCode | 问题编码 |
| severity | 严重级别 |
| headerAmount | 表头金额 |
| lineCount | 分录条数 |
| entryCount | 总账分录条数 |
| lineDebit | 分录借方合计 |
| lineCredit | 分录贷方合计 |
| glDebit | 总账借方合计 |
| glCredit | 总账贷方合计 |
| message | 问题说明 |
| suggestion | 处理建议 |
| remark | 备注 |
